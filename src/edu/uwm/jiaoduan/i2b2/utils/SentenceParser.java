/**
 * 
 */
package edu.uwm.jiaoduan.i2b2.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import edu.uwm.jiaoduan.Messages;
import edu.uwm.jiaoduan.tools.JdMontyLingua;

/**
 * @author yonggang
 * @date Aug 17, 2009
 */
public class SentenceParser {
	public static Position[] getPoses(String sen) {
		int curPos = 0;
		ArrayList<Position> pos = new ArrayList<Position>();
		ArrayList<String> nps = getNPs(sen);
		
		for (String token : nps) {
			if (curPos >= sen.length()) {
				curPos = 0;
			}
			String subs = sen.substring(curPos);
			int start = subs.indexOf(token);
			if (start >= 0) {
				start+=curPos;
				int end = start + token.length();
				pos.add(new Position(start, end));
				curPos = end;
			} else{
				// else ignore
				System.err.println("Erro in SentenceParser: ignore:" + token);
			}
		}
		return (Position[]) pos.toArray(new Position[pos.size()]);
	}

	static JdMontyLingua jdm = new JdMontyLingua();

	/**
	 * @param sen
	 * @return
	 */
	private static ArrayList<String> getNPs(String sen) {

		String[] phraseType = new String[] { "adj_phrases", "noun_phrases" };
		return jdm.GetPrasesInSentence(sen, phraseType);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		JMerki jm = new JMerki();
		try {
			jm.initializeParser();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		
		String TwoLevelsDataFolder = Messages.getString("i2b2.challenge.competition.data.folder");
		HashMap<String, String> dsumfileList = new HashMap<String,String>();
		try {
			jm.LoadDischargeSummary(TwoLevelsDataFolder, dsumfileList);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		boolean begin = false;
//		String sentence = "The patient has very serious anxiety, especially at night. The patient has been treated successfully with DDD4DDD, but DD3DDD resultsed in nightmares\n\n";
//		for (Position p : getPoses(sentence))
//			System.out.println(p + "\t" + sentence.substring(p.start, p.end));
		
		RawInput rin = new RawInput(); 
		for(String dsumfile: dsumfileList.keySet())
		{
			System.out.println("Parsing: " + dsumfile);
			String content  = dsumfileList.get(dsumfile);
			LancetParser lancet = null;
			try {
				lancet = new LancetParser(content);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ArrayList<HashMap<String, String>> sentenceSpan = lancet.getSentenceSpan();
			for(HashMap<String,String> span: sentenceSpan){
				String sentence = span.get("Sentence").replace(".", "_");
				int sentBegin = Integer.parseInt(span.get("BeginIndex"));
				int sentEnd = Integer.parseInt(span.get("EndIndex"));
				
				try {
					int midPos = (sentBegin + sentEnd)/2;
					if(lancet.getListNarrative(midPos).equals("list"))
						continue;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				for (Position p : getPoses(sentence)){
					
					String offset = lancet.getTokenOffset(sentBegin + p.start, sentBegin + p.end);
					System.out.println(p + "\t" + offset + "\t" + sentence.substring(p.start, p.end) + "\t" + lancet.GetTokenContent(sentBegin + p.start, sentBegin + p.end));
				}
				RawInput.getInput();
			}
		}


	}

}
