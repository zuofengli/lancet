package edu.uwm.jiaoduan.i2b2.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import spiaotools.SentParDetector;
import edu.uwm.jiaoduan.Messages;
import edu.uwm.jiaoduan.i2b2.knowtatorparser.KnowtatorXmlBuilder;

/**
 * @author Zuofeng Li
 * @date Dec 16, 2009
 */
public class ListedMedication {

	private static final int TOKENBEGIN = 0;
	private static final int LINEBEGIN = 1;
	protected String content = null; 
	private String [] lines = null;
	private String[] words = null;

	HashMap<Integer, String> dscSections = new HashMap<Integer, String>();

	ArrayList<Integer> sctList = new ArrayList<Integer>();
	public static String lmSeparator_ex = "\\|+";
	String lmSeparator = "\\|\\|";

	private String contentWithoutEnter;
	public String getContentWithoutEnter() {
		return contentWithoutEnter;
	}
	private String[] orginalLines;
	private String originContent;
	private static final RawInput rin = new RawInput();;

	/*
	 * Set the raw text of the 
	 */
	public void setOriginContent(String originContent) {
		this.originContent = originContent;
	}


	/**
	 * @definition Construction function of listed medication
	 * @param contentOrFilepath: String: This could be a file path name or the content
	 * 				  of the file. The function will automatically determine which type
	 * 				  it is.
	 */
	public ListedMedication(String contentOrFilepath) throws Exception {

		boolean exists = (new File(contentOrFilepath)).exists();

		if(exists){
			String fullTexts = RawInput.getFullText(contentOrFilepath);
			InitialLstMedcine(fullTexts);
		}else{
			InitialLstMedcine(contentOrFilepath);
		}	

		parseDxIntoSections();

		//		ParseDischageIntoPhrases();
	}

	/**
	 * @definition 
	 */
	private void parseDxIntoSections() {
		dscSections.put(0, "Begin");

		//		RawInput rin = new RawInput();
		String yamlfile = Messages.
				getString("jMerki.YAML.FilePath");
		
		Object ob = RawInput.loadYAMLfile(yamlfile);
		HashMap<String, Object> data = (HashMap<String, Object>)ob;

		HashMap<String,LinkedList<String>> dcSumSections = (HashMap<String,LinkedList<String>>)data.get("dcSumSections");

		HashMap<String,String> sectionPatterns = new HashMap<String,String>();

		String sd = Messages.getString("jMerki.splitDelim");
		for(String sectionName: dcSumSections.keySet()){
			LinkedList<String> section = dcSumSections.get(sectionName);

			String pStr = "(";
			for(int i= 0; i< section.size(); i++){
				if(i < section.size()-1){
					pStr += sd + section.get(i) + sd + "|";
				}else{
					pStr += sd + section.get(i) + sd;
				}
			}
			pStr += ")";

			sectionPatterns.put(sectionName, pStr);
		}
		int size = sectionPatterns.keySet().size();
		String[] patterns = new String[size];
		int count =0;
		for(String normSectName: sectionPatterns.keySet()){
			String pStr = sectionPatterns.get(normSectName);
			patterns[count] =pStr;
			count ++;

			Pattern dscSection = Pattern.compile(pStr);
			Matcher m = dscSection.matcher(content);
			while(m.find())
			{
				HashMap<String, Integer> offset=  convertArticleRegion2TokenPosition(m.start(), m.end());
				String parTokenOffset = Messages.
								getString("i2b2.parameters.discharge.section.title.maximal.line.token.offset");
				if(parTokenOffset == null){
					System.err.println("Could not find i2b2.parameters.discharge.section.title.maximal.line.token.offset parameter");
					parTokenOffset = "0";
				}

				int iOffset = Integer.parseInt(parTokenOffset);

				if(offset.get("StartTokenPosition") > iOffset || iOffset < 0)//begin with
					continue;

				//			use article position to mark a section
				dscSections.put(m.start(), normSectName);
			}

		}
		
		//ystem.out.println(RawInput.join(patterns, '|'));

		dscSections.put(content.length(), "End");

		for(int p: dscSections.keySet())
			sctList.add(p);
		//	make order 
		Collections.sort(sctList);
	}
	
	public HashMap<Integer, String> getStructuredSections(){
		return dscSections;
	}

	//	public ListedMedication(String textContent, Map<String, String> patterns) {
	//		// TODO Auto-generated constructor stub
	//		
	//		InitialLstMedcine(textContent);
	//		
	//		
	//		
	////		Pattern dscSection = Pattern.compile("\n([A-Z]+(.*?)):\\s*\n");
	//		dscSections.put(0, "Begin");
	//		String pStr = "(" + patterns.get("context") + ")";
	//		Pattern dscSection = Pattern.compile(pStr);
	//		Matcher m = dscSection.matcher(content);
	//		while(m.find())
	//		{
	//			dscSections.put(m.start(), m.group(1));
	//		}
	//		dscSections.put(content.length(), "End");
	//		
	//		for(int p: dscSections.keySet())
	//			sctList.add(p);
	////		make order 
	//		Collections.sort(sctList);
	////		System.out.println(sctList);
	//	}



	/*
	 * Normalize the symbols for regular expression
	 */
	//	public String normalizeWord(String input) {
	//	// cDNA Assay)”,
	//		RawInput rin = new RawInput();
	//		return rin.normalizeWord(input);
	//     }

	private void InitialLstMedcine(String textContent) throws Exception {
		//		caption information is usefule for sentence boundary. 
		//		InitialLNClassifier(); moved to lancetParser

		content = textContent;
		//		SetSentenceSpans(); moved to LancetParser
		orginalLines = content.split("\n");
		originContent = textContent;

		content = textContent.toLowerCase();
		contentWithoutEnter = content.replaceAll("\n", " ");
		lines = content.split("\n");	
		words = content.split("");	

	}
	public String getOriginContent() {
		return originContent;
	}





	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		String a = " i eat apple\n and banana.";
		
		ListedMedication lm = new ListedMedication(a);
		
		String singleLineOffset = " 1:0 1:0 ";//single line offset
		
		String i2b2Offset = lm.convert2i2b2Offset(singleLineOffset);
		System.out.println("----");
		System.out.println(i2b2Offset);

	}
	/**
	 * @param singleLineOffset
	 * @return
	 */
	protected String convert2i2b2Offset(String singleLineOffset) {
		ArrayList<HashMap<String, Integer>> porlt = parseOffset(singleLineOffset);
		String offset = "";
	
		for(HashMap<String, Integer> po: porlt){

			int lineIndex = 0;

			int dxTokenIndex = -1;
			

			for(String line: lines){
//				System.out.println(dxTokenIndex);
				lineIndex ++;
				int lineTokenIndex = -1;
				Scanner sc = new Scanner(line);
				while(sc.hasNext()){
//					System.out.println(sc.next());
					sc.next();
					lineTokenIndex++;
					dxTokenIndex ++;
					
					if(dxTokenIndex == po.get("StartToken") || dxTokenIndex == po.get("EndToken")){
						offset += Integer.toString(lineIndex) 
									+ ":" 
									+ Integer.toString(lineTokenIndex);
						offset += " ";
						System.out.println(po.get("StartToken") + "\t" + po.get("EndToken"));
						if(po.get("StartToken").equals(po.get("EndToken"))){
							System.out.println(po.get("EndToken"));
							offset += Integer.toString(lineIndex) 
							+ ":" 
										+ Integer.toString(lineTokenIndex);
							offset += " ";
						}
					}
					
				}
			}
			offset += ",";
		}
		return offset.replaceFirst(",$", "").trim();
		}


	//	/*
	//	 * Another way to used listedMedication
	//	 */
	//	private void reset(String dxContent) {
	//		try {
	//			InitialLstMedcine(dxContent);
	//		} catch (Exception e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//		
	//		ParseDischargeIntoSections();
	//	}
	/**
	 * @param iBegin: the absolute position
	 */
	public HashMap<String, String> getUpContext(int iBegin, int iEnd) {	
		String sBefore = new String();
		String sAfter = new String();

		int lineIndex =LINEBEGIN;
		int tokenIndex =TOKENBEGIN;
		boolean isNewLineBegin = true;
		String word = new String();

		HashMap<String,Integer> tokenOffset = new HashMap<String, Integer>();
		String sLineBefore = new String();

		for(int i =0; i < words.length; i++)
		{
			word = words[i];

			if(word.equals("\n"))
			{
				lineIndex ++;
				tokenIndex = TOKENBEGIN;

				isNewLineBegin = true;
				sLineBefore = "";
			}

			if(word.equals(" "))
			{
				String next = words[i+1];
				if((!next.equals(" ")) && (!isNewLineBegin) )
				{
					tokenIndex ++;
				}
			}


			if(i == iBegin)
			{
				tokenOffset.put("StartLine", lineIndex);
				tokenOffset.put("StartTokenPosition", tokenIndex);
				int iUpOne = Math.max(0, lineIndex - 1);
				int iUpTwo = Math.max(0, lineIndex - 2);

				if(iUpOne ==iUpTwo)
					sBefore = lines[iUpTwo] + sLineBefore;
				else
					sBefore = lines[iUpTwo] + lines[iUpOne] + sLineBefore; 
			}

			if(i == iEnd)
			{
				tokenOffset.put("EndLine", lineIndex);
				tokenOffset.put("EndTokenPosition", tokenIndex);
				int iDownOne = Math.min(lines.length, lineIndex +1);
				int iDownTwo = Math.min(lines.length, lineIndex + 1);

				String sLineAfter = new String();
				for(int j=i+1; j < words.length; j++)
				{
					if(word.equals("\n"))
					{
						sAfter = sLineAfter + lines[iDownOne] + lines[iDownTwo];
						break;
					}
					else
						sLineAfter += word;
				}
				break;
			}

			sLineBefore +=word;
			isNewLineBegin = false;
		}



		HashMap<String, String> context = new HashMap<String,String>();
		context.put("UpContext", sBefore);
		context.put("DownContext", sAfter);

		return context;		
	}
	/**
	 * This is the main fuction used to convert position into i2b2 token
	 * offset.
	 * Input: absolute position value
	 * Output: HashMap<String, Integer>
	 * @keyword StartLine
	 * @keyword EndLine
	 * @keyword StartTokenPosition
	 * @keyword EndTokenPosition
	 * @Bug200912071122: If there is space at the beginning of a sentence,the offset is not correct  
	 * 	System	Input: "Aspirin for heart\n pain and pain."
	 * 	System	Output: r="pain" 2:1 2:1
	 *  Should be: r="pain" 2:0 2:0 
	 *  Status: Accept
	 */
	public HashMap<String, Integer> convertArticleRegion2TokenPosition(
			int start, int end) {
			HashMap<String,Integer> tokenOffset = new HashMap<String, Integer>();
	        
	        String preStartDocText = this.content.substring(0, start);
	        
	    	tokenOffset.put("StartLine", RawInput.countCharInString("\n", preStartDocText) + 1);
	    	
	    	
	    	String preEndDocText = this.content.substring(0, end);
	    	tokenOffset.put("EndLine", RawInput.countCharInString("\n", preEndDocText) + 1);
	    	
	    	int startLineBegin = preStartDocText.lastIndexOf("\n") + 1;
	    	int endLineBegin = preStartDocText.lastIndexOf("\n") + 1;
	    	
	    	String ahead = content.substring(startLineBegin, start).trim();
	    	if (ahead.isEmpty())
	    		tokenOffset.put("StartTokenPosition",0);
	    	else{
	    	String[] beginTokens = ahead.split("\\s+");
	    	tokenOffset.put("StartTokenPosition", beginTokens.length);
	    	}
	    	
	    	ahead = content.substring(endLineBegin, end).trim();
	    	if (ahead.isEmpty())
	    		tokenOffset.put("EndTokenPosition",0);
	    	else{
	    	String[] endTokens = ahead.split("\\s+");
	    	tokenOffset.put("EndTokenPosition", endTokens.length -1 );
	    	}
	    	

			
			
			

		return tokenOffset;
	}
	public HashMap<String, Integer> convertArticleRegion2TokenPosition_old(
			int start, int end) {
		String tokenContent = "";

		int lineIndex =LINEBEGIN;
		int tokenIndex =TOKENBEGIN;

		int lastTokenIndex = 0;
		boolean isNewLineBegin = true;
		String word = new String();

		int tokenNumber =0;

		HashMap<String,Integer> tokenOffset = new HashMap<String, Integer>();
		for(int i =0; i < words.length; i++)
		{
			word = words[i];

			//			Fix Bug200912071122 block
			if(word.isEmpty() && isNewLineBegin){
				isNewLineBegin = false;
				tokenIndex = TOKENBEGIN -1;
				//				continue;
			}
			//			Fix Bug200912071122 block	

			if(word.equals("\n"))
			{
				lineIndex ++;

				lastTokenIndex = tokenIndex;
				//				Fix Bug200912071122 block
				String next = words[i+1];
				if(next.equals(" "))
					tokenIndex = TOKENBEGIN -1;
				else
					tokenIndex = TOKENBEGIN;
				//				Fix Bug200912071122 block

				isNewLineBegin = true;
			}



			if(word.equals(" "))
			{
				String next = words[i+1];


				if((!next.equals(" ")) && (!isNewLineBegin) )
				{
					tokenIndex ++;
				}
			}


			if(i == start)
			{
				tokenOffset.put("StartLine", lineIndex);

				tokenOffset.put("StartTokenPosition", tokenIndex);

				tokenNumber = 0;
			}

			tokenNumber++;

			if(i == end)
			{
				if(word.equals("\n"))
				{
					tokenOffset.put("EndLine", lineIndex -1);
				}else
					tokenOffset.put("EndLine", lineIndex);

				//				it the word is end with space, the index point to the next token
				if(word.equals(" "))
					tokenOffset.put("EndTokenPosition", tokenIndex -1);
				else
				{
					if(word.equals("\n"))
					{
						tokenOffset.put("EndTokenPosition",lastTokenIndex);
					}else
					{
						tokenOffset.put("EndTokenPosition", tokenIndex);
					}
				}

				tokenOffset.put("TokenNumber", tokenNumber);

				break;
			}

			isNewLineBegin = false;



		}


		return tokenOffset;
	}
	/**
	 * 
	 * @param start
	 * @param end
	 * @return a string something like 10:2 10: 5
	 */
	public String getTokenOffset(int start, int end)
	{
		HashMap<String, Integer> offset=  convertArticleRegion2TokenPosition(start, end);
		String offset_ex = new String();
		offset_ex = offset.get("StartLine") + ":" +  offset.get("StartTokenPosition") + " " + offset.get("EndLine") + ":" + offset.get("EndTokenPosition");
		return offset_ex;
	}
	/*
	 * DEFINITION: parse i2b2 entry into HashTable
	 * INPUT: String: Singlei2b2List
	 * INPUT: new HashMap<String,String> parseRlt
	 * 		keysets: m|do|mo|f|du|r|e|t|c|ln\
	 * 		keysets: m|do|mo|f|du|r|e|t|c + "TokenPosition"
	 */
	public void GetMedicationFields(String Singlei2b2List, HashMap<String, String> parseRlt) {
		getFeatures(Singlei2b2List, parseRlt);
	}
	public static void getFeatures(String i2b2Entry, HashMap<String, String> parseRlt) {
		//		System.out.println(content);

		Pattern mentioned= Pattern.compile("^(m|do|mo|f|du|r|e|t|c|ln)=\"(.*?)\"\\s+(.*)$");
		Pattern nm = Pattern.compile("^(m|do|mo|f|du|r|e|t|c|ln)=\"nm\"$");
		Pattern ln = Pattern.compile("^(m|do|mo|f|du|r|e|t|c|ln)=\"(list|narrative|negative)\"$");		

		Matcher matcher=null;
		String rEx = "\\|+";
		String [] fields = i2b2Entry.split(lmSeparator_ex);
		for (int i=0; i< fields.length; i++)
		{
			String seg = fields[i].trim();
			//			System.out.println(seg);
			matcher = nm.matcher(seg);
			if (matcher.find()){
				parseRlt.put(matcher.group(1), "nm");
				continue;
			}

			matcher = mentioned.matcher(seg);
			if (matcher.find())
			{
				//				MatchResult result = matcher.getMatch();
				//				String [] offsets = seg.split(",");
				//				Pattern pOffSet = Pattern.compile("\\s*(\\d+)\\s*:\\s*(\\d+)\\s*(\\d+)\\s*:\\s*(\\d+)");
				parseRlt.put(matcher.group(1).toLowerCase(), matcher.group(2));
				String tpName = matcher.group(1).toLowerCase()+"TokenPosition";
				parseRlt.put(tpName, matcher.group(3));

				continue;
			}

			matcher = ln.matcher(seg);	
			if(matcher.find())
			{
				String skey = matcher.group(1);
				String rlt = matcher.group(2);
				parseRlt.put(skey, rlt);
				continue;
			}
			//			checking results
			//			System.out.println("no match" + seg +"----------------------no match");			
		}
	}

	//	public String GetListNarrative(String NormalizedContext) {
	//		// TODO Auto-generated method stub
	//		String nctx = NormalizedContext;
	//		Pattern pList = Pattern.compile("(Discharge meds|mediction lists|medication on admission)");
	//		Matcher lmatch = pList.matcher(nctx);
	//		if(lmatch.find())
	//			return "list";
	//		else
	//			return "narrative";
	//
	//	}
	public String GetContent(){
		return content;
	}



	/*
	 * Definition: Tag the origin article with i2b2 tags based i2b2 entries.
	 * INPUT: listMeds: Type:  ArrayList<String>
	 * 			Example: m="hydrogen peroxide," 4:8 4:9||do="nm"||mo="nm"||f="nm"||du="nm"||r="nm"||ln="narrative"
	 * 
	 * OUTPUT: Tagged article.
	 * 			Example: atenolol|B-M 12.5|B-DO mg|I-DO daily|B-F ,|O aspirin|B-M 325|B-DO mg|I-DO daily|B-F ,|O metformin|B-M 500|B-DO mg|I-DO 
	 */
	public String tagLabelForCRFModel(ArrayList<String> listMeds, boolean bMakeLowerCase, boolean bLabelProblemList) {		
		String[] tags = Messages.getString("i2b2.CRF.TAGs").split(",");

		ArrayList<HashMap<String, ArrayList<String>>> goldStdToken = getGoldStdTokens(listMeds);

		String labeledArticle = new String();

		String[] strLines = null;
		if(bMakeLowerCase){
			if(bLabelProblemList){
				JMerki jm = new JMerki();
				try {
					jm.initializeParser();
				} catch (IOException e) {
					e.printStackTrace();
				}
				ArrayList<String> conceptList = new ArrayList<String>();
				String labelledContent = jm.tagMedicalTerms(this.content, conceptList);
				strLines = labelledContent.split("\n");
			}else
				strLines = this.lines;
		}else{
			//			keep original lower case or upper case. No change.
			if(bLabelProblemList){
				JMerki jm = new JMerki();
				try {
					jm.initializeParser();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ArrayList<String> conceptList = new ArrayList<String>();
				String labelledOriginalContent = jm.tagMedicalTerms(this.originContent, conceptList);
				strLines = labelledOriginalContent.split("\n");
			}else
				strLines = this.orginalLines;
		}

		String oldTag = "";
		boolean bTagBegin = false;
		for(int i=0; i< strLines.length; i++)
		{
			int lineIndex = i + 1;

			String line = strLines[i];
			String timmedLine = line.trim();
			if(timmedLine.length() < 1)
				continue;

			String[] fields = line.split("");
			int tokenIndex = -1;

			//			Pattern pSpaceBegin = Pattern.compile("^\\s+");
			//			Matcher fmsb = pSpaceBegin.matcher(line);
			//			if(fmsb.find())
			//				line = line.trim();

			Scanner sc = new Scanner(line);

			Scanner scanner = new Scanner(new StringReader(line));
			Scanner scanner2 = new Scanner(new StringReader(line));
			String t;
			String tt = null;

			if(scanner2.hasNext()){
				String tmp = scanner2.next();
			}


			//		    we suppose that tokens 
			int nextLineIndex = 0;
			int nextTokenIndex = 0;
			int j = -1;
			while (scanner.hasNext()) {
				t= scanner.next();

				if(scanner2.hasNext())
					tt = scanner2.next();
				else{
					j = i + 1;
					if(j < strLines.length){
						String nextLine = strLines[j].trim();
						Scanner scanner3 = new Scanner(new StringReader(nextLine));
						if(scanner3.hasNext())
							tt = scanner3.next();
					}
				}


				tokenIndex ++;
				String sTag = getTokenTagType(goldStdToken, tags, lineIndex, tokenIndex);

				String sNextTag = null;
				if(j > -1){
					nextLineIndex = lineIndex + 1;
					nextTokenIndex = 0;
				}else{
					if(tt != null){
						nextLineIndex = lineIndex;
						nextTokenIndex = tokenIndex + 1;
					}
				}

				if(tt!= null){
					sNextTag = getTokenTagType(goldStdToken, tags, nextLineIndex, nextTokenIndex);
				}


				//		    	boolean = isOffsetEndToken();
				//		    	Edema|B-R of|I-R 
				//		    	leg|I-R 
				//		    	pain.|I-R 

				String label= null;
				if(!sTag.equals(oldTag) || sTag.equals("O"))
				{
					if(!sTag.equals("O")){
						if(sNextTag != null && sTag.equals(sNextTag)){
							label = t.toString() + "|B-" + sTag + " ";
						}else
							label = t.toString() + "|" + sTag + " ";

						bTagBegin = true;
					}
					else
						label = t.toString() + "|" + sTag + " ";
				}else
					label = t.toString() + "|I-" + sTag + " ";
				labeledArticle += label;

				oldTag = sTag;
			}
			labeledArticle += "\n";



			//			Pattern pToken = Pattern.compile("(.\\s+)");
			//		
			//			Matcher fm = pToken.matcher(line);
			//						
			//			int Start = 0;			
			//			while(fm.find())
			//			{
			//				tokenIndex++;
			//				String sMatch = fm.group(1);
			//				int spNumber = sMatch.length() - sMatch.trim().length();
			//				int end = fm.end() - spNumber;
			//				String label= null;
			//				String sTag = GetTagType(goldStdToken, tags, lineIndex, tokenIndex);
			//				if(!sTag.equals(oldTag) || sTag.equals("O"))
			//				{
			//					if(!sTag.equals("O"))
			//						label = line.substring(Start,end) + "|B-" + sTag + " ";
			//					else
			//						label = line.substring(Start,end) + "|" + sTag + " ";
			//				}else
			//					label = line.substring(Start,end) + "|I-" + sTag + " ";
			//				Start = fm.end();
			//				labeledArticle += label;
			//				
			//				oldTag = sTag;
			//			}

			//			labeledArticle += "\n";
			//			labeledArticle += " ";
		}		
		return labeledArticle;
	}
	/**
	 * @param listMeds
	 * @return
	 */
	protected ArrayList<HashMap<String, ArrayList<String>>> getGoldStdTokens(
			ArrayList<String> listMeds) {
		Pattern pKeyName = Pattern.compile("(\\w{1,2})TokenPosition");
		Pattern pOffset = Pattern.compile("(\\d+):(\\d+)\\s+(\\d+):(\\d+)");
		String TokenType = "";
		ArrayList<HashMap<String, ArrayList<String>>> goldStdTokenListByMedication = new ArrayList<HashMap<String, ArrayList<String>>>();

		for(String list: listMeds)
		{
			HashMap<String, ArrayList<String>> goldStdToken = new HashMap<String, ArrayList<String>>();

			HashMap<String, String> listMap = new HashMap<String,String>();
			getFeatures(list, listMap);

			ArrayList<String> lnInfo = new ArrayList<String>();
			lnInfo.add(listMap.get("ln"));
			goldStdToken.put("ln", lnInfo);

			for(String key: listMap.keySet())
			{
				Matcher m = pKeyName.matcher(key);
				if(m.find())
				{
					TokenType = m.group(1);
					Matcher mOfset= pOffset.matcher(listMap.get(key));
					while(mOfset.find())
					{
						int startLine = Integer.parseInt(mOfset.group(1));

						int startToken = Integer.parseInt(mOfset.group(2));

						int endLine = Integer.parseInt(mOfset.group(3));
						int endToken = Integer.parseInt(mOfset.group(4));


						if(startLine == endLine)
						{		
							addTokensInSameLine(goldStdToken, TokenType, startToken, endToken, startLine);

						}else
						{
							for(int j=startLine; j<=endLine; j++)
							{
								if(j == startLine)
								{
									int tokensize  = GetTokenSizeOfLine(startLine);
									addTokensInSameLine(goldStdToken, TokenType, startToken, tokensize, startLine);

								}else if (j == endLine)
								{
									addTokensInSameLine(goldStdToken, TokenType, 0, endToken, endLine);
								}else
								{
									int tokensize = GetTokenSizeOfLine(j);
									addTokensInSameLine(goldStdToken, TokenType, 0, tokensize, j);
								}
							}
						}

					}					
				}
			}
			goldStdTokenListByMedication.add(goldStdToken);
			//			System.out.println(listMap.keySet());
		}
		return goldStdTokenListByMedication;
	}

	public int GetTokenSizeOfLine(int startLine) {
		// TODO Auto-generated method stub
		String line = lines[startLine -1];
		line = line.trim();

		String[] fields = line.split("\\s+");
		return fields.length;
	}
	/**
	 * 
	 * @param goldStdToken
	 * @param tokenType
	 * @param startToken
	 * @param endToken
	 * @param startLine
	 */
	private void addTokensInSameLine(
			HashMap<String, ArrayList<String>> goldStdToken, String tokenType, int startToken,
			int endToken, int startLine) {
		String ofset = null;
		for(int j = startToken; j<=endToken; j++)
		{
			ofset = Integer.toString(startLine) + "." + j;
			if(goldStdToken.containsKey(tokenType))
			{
				goldStdToken.get(tokenType).add(ofset);
			}
			else
			{
				ArrayList<String> ltoken = new ArrayList<String>();
				ltoken.add(ofset);
				goldStdToken.put(tokenType, ltoken);
			}
		}

	}
	/**
	 * 
	 * @param stdTokens
	 * @param tags
	 * @param lineIndex
	 * @param tokenIndex
	 * @return choice in {M,DO,MO,F,DU,R}
	 */
	protected String getTokenTagType(ArrayList<HashMap<String, ArrayList<String>>> stdTokens, String[] tags, int lineIndex, int tokenIndex ) {
		String tag = "";
		String dLocation = lineIndex + "." + tokenIndex;

		for(HashMap<String, ArrayList<String>> medication: stdTokens){

			for(String field: medication.keySet()){
				for(String pos: medication.get(field)){
					if(dLocation.equals(pos)){
						tag = field;
						break;
					}
				}

				if(!tag.isEmpty())
					break;
			}

			if(!tag.isEmpty())
				break;
		}


		for(String t: tags)
		{
			if(t.equals(tag))
			{
				return tag.toUpperCase();
			}
		}
		return "O";
	}

	public HashMap<String, Double> i2b2EvaluateMetics(ArrayList<String> sysListedMeds,
			String trainDataFile, String string) {
		// TODO Auto-generated method stub

		HashMap<String, Double> evRlt = new HashMap<String, Double>();
		File ftrain = new File(trainDataFile);
		String filename = ftrain.getName();
		String gFolder = Messages.getString("i2b2.Gold.Entries.folder");

		//		RawInput rinput = new RawInput();
		//		String goldStandardFile = trainDataFile.replace("trainingdata", "training.ground.truth");
		String goldStandardFile = gFolder + "2/" + filename;
		goldStandardFile += ".i2b2.entries";

		if(!rin.IsFileExist(goldStandardFile)){
			System.err.println("There is no gold standard file");
			return evRlt;
		}


		ArrayList<String> goldListedMeds = RawInput.getListByEachLine(goldStandardFile, false);

		ArrayList<Double> PrecisionList = new ArrayList<Double>();
		ArrayList<Double> RecallList = new ArrayList<Double>();

		ArrayList<String> FalseNegative = new ArrayList<String>();

		for(String goldlm: goldListedMeds)
		{

			double S = 0; // total number of fields in the ground truth that are not nm;
			double N = 0; // total number of fields in the system output that are not nm;
			double D = 0; // 2 exact matches in terms of offset and field type

			HashMap<String, String> entries_g = new HashMap<String, String>();
			HashMap<String, String> entries_sys = new HashMap<String, String>();

			String[] fields_g = goldlm.split(lmSeparator_ex);
			String sys_lm = new String();


			S = GetNonNMnumber(goldlm, entries_g);

			String m_ex = "^" + rin.normalizeWord(fields_g[0]);

			Pattern pMentry = Pattern.compile(m_ex);			

			int sysIndex = -1;
			for(int j =0; j < sysListedMeds.size(); j++)
			{
				String lm = sysListedMeds.get(j);
				lm = lm.toLowerCase();
				Matcher mm = pMentry.matcher(lm);
				if(mm.find())
				{
					N = GetNonNMnumber(lm, entries_sys);
					//					System.out.println(m_ex);
					sys_lm = lm;

					sysIndex = j;
					break;
				}
			}

			String[] fields_sys = goldlm.split(lmSeparator_ex);
			//compute
			double precision = 0;
			double recall = 0;
			if(N > 0)
			{
				//				removed matched list medication
				sysListedMeds.remove(sysIndex);

				D = GetExactMatchNumber(entries_g, entries_sys);

				precision = D/N;
				recall = D/S;
				if(!(recall == 1 && precision == 1)){
					System.out.println(GetMarkedRegionByListedMedication(sys_lm));

					System.out.println("gold:\t" + goldlm + "\nSyst:\t" + sys_lm);
					System.out.println("D: " + D + ";N: " + N + ";S: " + S);
					System.out.println("precision: " + precision + "; recall: " + recall);
					System.out.println();
				}

			}
			else
			{
				//				System.err.println("System output do not match this medication:\n" + goldlm);
				FalseNegative.add(goldlm);
			}
			PrecisionList.add(precision);
			RecallList.add(recall);
		}
		double pSum = 0;
		double rSum = 0;
		for(int i= 0; i < PrecisionList.size(); i++)
		{
			pSum += PrecisionList.get(i);
			rSum += RecallList.get(i);
		}
		System.out.println("\nFalse positive record!");
		for(String lm: sysListedMeds){
			System.out.println(lm);
			System.out.print("FP Record---");
			System.out.println(GetMarkedRegionByListedMedication(lm));
			System.out.println();
		}

		System.out.println("\nFalse negtive record!");
		for(String fn: FalseNegative){
			System.out.println(fn);
			System.out.print("FN Record---");
			System.out.println(GetMarkedRegionByListedMedication(fn));
			System.out.println();
		}

		System.out.println("Micro-average Recall is:" + pSum/PrecisionList.size() + "; Precision is" + rSum/RecallList.size());
		System.out.println("--------------------------------------------------------------------------------------------------------------------------------------------");
		return evRlt;
	}

	public String GetMarkedRegionByListedMedication(String listMedication) {
		// TODO Auto-generated method stub
		int iSpan = 60;

		Pattern pOffset = Pattern.compile("m=\"(.*?)\"\\s+(\\d+):(\\d+)\\s+(\\d+):(\\d+)");
		Matcher mm = pOffset.matcher(listMedication);
		int mStartLine = 0;
		int mStartToken = 0;
		int mEndLine = 0;
		int mEndToken = 0;
		String drugName = "";
		if(mm.find()){
			drugName = mm.group(1);
			mStartLine = Integer.parseInt(mm.group(2));
			mStartToken = Integer.parseInt(mm.group(3));
			mEndLine = Integer.parseInt(mm.group(4));
			mEndToken = Integer.parseInt(mm.group(5));
		}else{
			System.err.println("Erro in medication offset Listmedicaiton 1006 am");
		}

		if(drugName.equals("pcp."))
			System.out.println();
		int start = GetArticlePositionByStartToken(mStartLine, mStartToken);
		int end  = GetArticlePositionByStartToken(mEndLine, mEndToken +1);

		int leftSpan = Math.max(0, start - iSpan);
		String markedRegion = content.substring(leftSpan, start);
		markedRegion += "<drug>" + content.substring(start, end) + "</drug>";

		int rightSpan = Math.min(content.length(), end + iSpan);
		markedRegion += content.substring(end, rightSpan);
		return markedRegion;
	}

	public double GetExactMatchNumber(HashMap<String, String> entries_g,
			HashMap<String, String> entries_sys) {
		// TODO Auto-generated method stub
		String[] fields = Messages.getString("i2b2.competition.2009.fields").split(",");
		double d_value = 0;
		for(String entry: entries_g.keySet())
		{
			//something like doTokenPosition is do not considered.
			boolean legalEntry = false;
			for(String fd: fields){
				if(entry.equals(fd)){
					legalEntry = true;
					break;
				}
			}
			if(!legalEntry)
				continue;

			//			nm is also not considered
			String g_value = entries_g.get(entry);
			if(g_value.equals("nm"))
				continue;

			String g = entries_g.get(entry);
			String sys = entries_sys.get(entry);

			if(!entry.equals("ln"))
			{

				String gOffset = entries_g.get(entry+"TokenPosition");
				String sysOffset = entries_sys.get(entry+"TokenPosition");

				if(g.equals(sys) && gOffset.equals(sysOffset))
					d_value++;
				else
					System.out.println("->" + entry + ": g:" + g + "; s:" + sys);
			}else
			{
				if(g.equals(sys))
					d_value++;
				else
					System.out.println("->" + entry + ": g:" + g + "; s:" + sys);
			}

		}

		return d_value;
	}

	private double IsTwoExactMatch(String entry, String[] fields_sys,
			String[] fields_g) {
		// TODO Auto-generated method stub
		double value = 0;
		String[] fields = Messages.getString("i2b2.competition.2009.fields").split(",");

		boolean bMatch = false;
		int i =0;
		for(i =0; i< fields.length; i++){
			if(entry.equals(fields[i])){
				bMatch = true;
				break;
			}
		}
		if(!bMatch)
			return 0;

		String gold = fields_g[i];
		String sys = fields_sys[i];
		if(gold.equals(sys))
			value = 1;
		else
			value = 0;

		return value;
	}

	public double GetNonNMnumber(String listMedication, HashMap<String, String> entries) {
		// TODO Auto-generated method stub

		double NOT_NM_NUMBER = 0;
		String[] fields = Messages.getString("i2b2.competition.2009.fields").split(",");

		getFeatures(listMedication, entries);
		if(!entries.containsKey("m"))
			System.out.println("not contain medication!");

		for(String fdName: fields)
		{
			if(!entries.get(fdName).equals("nm"))
				NOT_NM_NUMBER++;
		}

		return NOT_NM_NUMBER;
	}

	//	public String[] GetDrugNamesByCRF() {
	//		// TODO Auto-generated method stub
	//		
	//		String[] medications = lancet.GetDrugList();
	//		for (int i=0; i<medications.length; i++) {
	//		    System.out.println(medications[i]);
	//		}
	//		
	//		return medications;
	//	}
	/*
	 * Definition: Get number of tokens for each i2b2 line.
	 * 
	 * Input: startLine: double: i2b2 line index (1,2,3,4,5,...,n)
	 * 
	 * output: The token numbers.
	 */
	public double GetLineTokenNumber(double startLine) {
		int lineIndex = (int) (startLine - 1);
		String line = lines[lineIndex];
		Scanner scanner = new Scanner(new StringReader(line));
		double number = 0;
		String t;
		while (scanner.hasNext()) {
			t= scanner.next();
			number ++;
		}
		return number;
	}
	/*
	 * DEFINITION: Get medication fields(not include drug name or m) in +/- 2 lines
	 * INPUT: listMed: TYPE: HashMap<String, String>
	 * We could call it as i2b2 medication: type, M, MOffset. M, for drug name
	 * 				Example: 		HashMap<String, String> medication = new HashMap<String,String>();
									medication.put("type", "M");
									medication.put("M", listMed.get("m"));
									medication.put("MOffset", listMed.get("mTokenPosition"));
	 * INPUT: ArrayList<String> sListMedications: Array of i2b2 entries
	 * 				Example: m="antibiotics" 142:0 142:0||do="nm"||mo="iv" 141:11 141:11||f="nm"||du="nm"||r="his pneumonia" 143:3 143:4||ln="narrative"
							 m="ativan" 215:6 215:6||do="low-dose" 215:8 215:8||mo="nm"||f="p.r.n." 215:5 215:5||du="nm"||r="significant anxiety." 214:8 214:9||ln="narrative"
	   OUTPUT: 	all of the medciation fields (except m and ln) in the +/-2 lines. Each medication field
	            is stored in a Hash Table
	   				Example: [type="DO"; DO="Aspirin"; DOOffset="1:3 1:3"]
	 */	
	public ArrayList<HashMap<String, String>> GetMinusPlusFieldsByMedication(
			HashMap<String, String> listMed, ArrayList<String> sListMedications) {
		Pattern pOffset = Pattern.compile("(\\d+):(\\d+)\\s+(\\d+):(\\d+)");
		Matcher mm = pOffset.matcher(listMed.get("mTokenPosition"));
		int mBeginLine = 0;
		int mEndLine = 0;
		if(mm.find()){
			mBeginLine = Integer.parseInt(mm.group(1));
			mEndLine = Integer.parseInt(mm.group(3));
		}else{
			System.err.println("Erro in medication offset Listmedicaiton 0347 pm");
		}

		Pattern pKeyName = Pattern.compile("(\\w{1,2})TokenPosition");
		String TokenType = "";
		ArrayList<HashMap<String, String>> fieldList = new ArrayList<HashMap<String, String>>();

		for(String list: sListMedications)
		{
			HashMap<String, String> listMap = new HashMap<String,String>();

			this.getFeatures(list, listMap);
			for(String key: listMap.keySet())
			{
				//				doTokenPosition
				Matcher m = pKeyName.matcher(key);
				if(m.find())
				{
					TokenType = m.group(1);
					if(TokenType.matches("(m|ln)"))
						continue;
					if(listMap.get(TokenType).equals("nm"))
						continue;

					Matcher mOfset= pOffset.matcher(listMap.get(key));
					if(mOfset.find())
					{
						int startLine = Integer.parseInt(mOfset.group(1));

						int startToken = Integer.parseInt(mOfset.group(2));

						int endLine = Integer.parseInt(mOfset.group(3));
						int endToken = Integer.parseInt(mOfset.group(4));

						//						minus and plus two lines;
						if(startLine > (mBeginLine -3) && startLine < (mEndLine + 3)){
							HashMap<String, String> field = new HashMap<String, String>();
							String type = TokenType.toUpperCase();
							field.put("type", type);
							field.put(type, listMap.get(TokenType));
							field.put(type + "Offset", listMap.get(key));
							//							System.out.println("minus plus tow line window");
							fieldList.add(field);
						}

					}else{
						System.err.println("wrong in listmedication 0409pm");						
					}					
				}
			}

		}
		return fieldList;
	}

	public String GetNormalizedTokenContent(int start, int end){
		String tokenContent = getTokenContent(start, end);
		return tokenContent.toLowerCase();
	}
	/*
	 * DEFINITION: convert article position region into token offset and get token content
	 * 		for example:  will stop nitrates.
	 * 		Annotation: nitrates
	 * 		start: 10; end: 18
	 *  	token Content: nitrates.
	 * 
	 * input: start int : the article absolute position
	 * 
	 * input: end   int : the article absolute position   
	 * output: token content  
	 */
	public String getTokenContent(int start, int end){

		String token = "";
		if(end < start){
			System.err.println("Erro in GetTokenContent: end less than start");
			return token;
		}else if(start < 0){
			System.err.println("Erro in GetTokenContent: start out of boundary");
			return token;
		}else if(end > contentWithoutEnter.length()-1){
			System.err.println("Erro in GetTokenContent: end out of boundary");
			return token;
		}

		token = contentWithoutEnter.substring(start, end);

		for(int i = start-1; i >= 0; i--){
			String forward =contentWithoutEnter.substring(i,i+1);
			if(forward.matches("(\\s|\\n)")){
				break;
			}else{
				token = forward + token;
			}
		}

		for(int i= end ; i < contentWithoutEnter.length(); i++){
			String afterward = contentWithoutEnter.substring(i, i+1);
			if(afterward.matches("(\\s|\\n)")){
				break;
			}else{
				token += afterward;
			}
		}

		return token;
	}



	/*
	 * DEFINITION: Get Start position in the article for a token
	 * INPUT: tokenLine: integer: token line
	 * INPUT: tokenIndex: integer: token index in the line
	 * 
	 * OUTPUT: integer: article position of the begining of the token
	 */
	public int GetStartPositionOfToken(int tokenLine, int tokenIndex){
		return GetArticlePositionByStartToken(tokenLine, tokenIndex);
	}

	public int GetArticlePositionByStartToken(int imLineStart, int imTokenStart) {

		String line = GetI2B2Line(imLineStart);

		Scanner scanner = new Scanner(new StringReader(line));
		String word = "";
		int number = -1;
		while(scanner.hasNext()){
			word = scanner.next();
			number ++;
			if(number == imTokenStart){
				break;
			}
		}
		//		allergy: quinine , <drug>aspirin </drug>, sulfa , penicillins
		//		return the end position of line;
		if(number < imTokenStart){
			int position = 0;
			for(int i = 1; i <= imLineStart; i++){
				position += GetI2B2Line(i).length() + 1;
			}
			return position;
		}
		//		other situation

		word = rin.normalizeWord(word);

		Pattern pWord = Pattern.compile(word);
		Matcher mWord = pWord.matcher(line);
		int start = -1;
		while(mWord.find()){
			start = mWord.start();
			String sub = line.substring(0, start);
			Scanner scan = new Scanner(new StringReader(sub));
			String t = "";
			number = 0;
			while(scan.hasNext()){
				t = scan.next();
				number++;
			}

			if(number == imTokenStart)
				break;
		}

		if(start < 0)
			System.err.println("error in list medication: 0359 PM");

		int position = 0;
		for(int i=0; i < imLineStart-1; i ++){
			position += lines[i].length() + 1;
		}
		position += start;

		return position;
	}

	private String GetI2B2Line(int imLineStart) {
		// TODO Auto-generated method stub
		return lines[imLineStart -1];
	}
	/*
	 * input :fieldName: String : One of m, do, mo, f, du, r, ln
	 * * input : listMediation: String
	 * process: 
	 * output: value of the field
	 */
	public static String getFieldValue(String fieldName, String listMedication) {
		// TODO Auto-generated method stub
		fieldName = fieldName.toLowerCase();
		HashMap<String, String> rlt = new HashMap<String, String>();
		getFeatures(listMedication, rlt);
		return rlt.get(fieldName);
	}
	/*
	 * Definition: Get the i2b2 entity for a single i2b2 entry
	 * 		i2b2 entity is defined as a HashMap with something like: 
	 *                  [type="DO"; DO="Aspirin"; DOOffset="1:3 1:3"]
	 * Input: fieldType = [m,do,mo,  f, du, ]
	 * Input: String: listMedication
	 */
	public HashMap<String,String> Geti2b2Entity(String fieldName, String listMedication){
		fieldName = fieldName.toLowerCase();
		HashMap<String, String> rlt = new HashMap<String, String>();
		this.getFeatures(listMedication, rlt);
		HashMap<String, String> entity = new HashMap<String, String>();

		String type  = fieldName.toUpperCase();
		entity.put("type", type);
		entity.put(type, rlt.get(fieldName));
		entity.put(type + "Offset", rlt.get(fieldName + "TokenPosition"));
		return entity;
	}
	/*
	 * Definition: This function would return the tagged article with i2b2 tags.
	 * 	it would depend on a css to display it.
	 */
	public String getFieldTaggedText(ArrayList<String> listmeds){
		String highLightedText = "";
		ArrayList<HashMap<String, Integer>> tokenList = new ArrayList<HashMap<String, Integer>>();
		String[] fields = Messages.getString("i2b2.competition.2009.fields").split(",");

		for(String lm: listmeds){
			HashMap<String,String> parseRlt = new HashMap<String,String>();
			getFeatures(lm, parseRlt);
			//			m,do,mo,f,du,r,ln

			Pattern pOffset = Pattern.compile("(\\d+):(\\d+)\\s+(\\d+):(\\d+)");

			for(int fieldType =0; fieldType < fields.length; fieldType ++){
				String key = fields[fieldType] + "TokenPosition";
				if(!parseRlt.containsKey(key))
					continue;

				String offset = parseRlt.get(key);
				Matcher mOffset = pOffset.matcher(offset);
				if(mOffset.find()){
					HashMap<String, Integer> token = new HashMap<String, Integer>();
					token.put("StartLine", Integer.parseInt(mOffset.group(1)));
					token.put("StartToken", Integer.parseInt(mOffset.group(2)));
					token.put("EndLine", Integer.parseInt(mOffset.group(3)));
					token.put("EndToken", Integer.parseInt(mOffset.group(4)));
					token.put("type", fieldType);

					tokenList.add(token);
				}
			}

		}
		
		highLightedText = highlighTokens(tokenList, false);
		
		return highLightedText;
	}
	
	/**
	 * @param tokenList
	 * @param withoutTag 
	 */
	public String highlighTokens(ArrayList<HashMap<String, Integer>> tokenList, boolean withoutTag) {
		String highLightedText = "";
		
		String[] fields = Messages.getString("i2b2.competition.2009.fields").split(",");
		
		for(int i = 0; i < lines.length; i++){		

			////			HashMap<String, Integer> offset=  ConvertArticleRegion2TokenPosition(i, i);
			//			int LineIndex = offset.get("StartLine");
			//			int TokenIndex = offset.get("StartTokenPosition");

			int LineIndex = i + 1;
			Scanner scanner = new Scanner(new StringReader(lines[i]));
			String word =  null;
			int TokenIndex = -1;
			boolean isI2b2Field = false;
			while(scanner.hasNext()){
				word = scanner.next();
				TokenIndex ++;

				//				System.out.println(LineIndex + "-" + TokenIndex);
				
				for(HashMap<String, Integer> token: tokenList){
					Integer iType = token.get("type");
					String type = fields[iType];

					if(token.get("StartLine") == LineIndex && token.get("StartToken") == TokenIndex){
						isI2b2Field = true;
						if (!withoutTag)
							highLightedText = highLightedText + "<"  + type + ">";					
					}
				}
				if (isI2b2Field){
					String markword = "";
					for(int j =0; j< word.length(); j++)
						markword +="M";
					word = markword;
				}
				
				highLightedText += word;

				for(HashMap<String, Integer> token: tokenList){
					Integer iType = token.get("type");
					String type = fields[iType];

					if(token.get("EndLine") == LineIndex && token.get("EndToken") == TokenIndex){
						isI2b2Field = false;
						if (!withoutTag)
							highLightedText = highLightedText + "</" + type + ">";
					}
				}
				
					
					highLightedText += " ";

			}

			highLightedText += "\n";
		}

		return highLightedText;
		
	}


	/*
	 * DEFINITION: get the end position of a token by its line and token number. 
	 * 
	 * INPUT: INTEGER: tokenLine: line of the token
	 * INPUT: INTEGER: tokenIndex: token index in the line
	 * 
	 * OUTPUT: INTEGER: position in the article.
	 */
	public int GetEndPositionOfToken(int tokenLine, int tokenIndex){
		return GetArticlePositionByEndToken(tokenLine, tokenIndex);
	}

	public int GetArticlePositionByEndToken(int endLine, int endToken) {
		// TODO Auto-generated method stub
		int startPost = this.GetArticlePositionByStartToken(endLine, endToken);
		int tokenLength = 0;
		out:for(int i =0 ; i < lines.length; i ++){
			if(endLine == i + 1){
				int number = -1;
				Scanner scanner = new Scanner(new StringReader(lines[i]));
				String word = "";
				while (scanner.hasNext()) {
					word = scanner.next();
					number ++;
					if(number == endToken){
						tokenLength = word.length();
						break out;
					}
				}
			}
		}

		return startPost + tokenLength;
	}

	public int GetDischargeSection(int startLine, int startToken) {
		// TODO Auto-generated method stub

		int iStart = GetArticlePositionByStartToken(startLine, startToken);

		//		base on discharge summary section
		int sectionIndex = -1;
		for(int i=0; i < sctList.size(); i++)
		{
			if(sctList.get(i) > iStart)
			{
				sectionIndex = sctList.get(i-1);
				break;
			}
		}
		return sectionIndex;
	}


//	public String ConvertI2b2ListMedicationToKnowtatorXML(ArrayList<String> listMedications) throws SAXException{
//
//		String xmlContent = null;
//		String fileName = "test.xml";
//		FileOutputStream fos = null;
//		try {
//			fos = new FileOutputStream(fileName);
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		OutputFormat of = new OutputFormat("XML","UTF-8",true);
//		of.setIndent(1);
//		of.setIndenting(true);
//		//		of.setDoctype(null,"users.dtd");
//		XMLSerializer serializer = new XMLSerializer(fos,of);
//		// SAX2.0 ContentHandler.
//		ContentHandler hd = null;
//		try {
//			hd = serializer.asContentHandler();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		try {
//			hd.startDocument();
//		} catch (SAXException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		// Processing instruction sample.
//		//hd.processingInstruction("xml-stylesheet","type=\"text/xsl\" href=\"users.xsl\"");
//		// USER attributes.
//		AttributesImpl atts = new AttributesImpl();
//
//		// USERS tag.
//		try {
//			//			<annotations textSource="5\501104">
//			atts.addAttribute("", "textSource", "", "", "5" + "\\" + "501104");
//			hd.startElement("","","annotations",atts);
//		} catch (SAXException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		for(String lm: listMedications){
//			System.out.println(lm);
//			HashMap<String, String> fields = new HashMap<String, String>();
//			this.GetFeatures(lm, fields );
//			//			list/narrative
//			for(String fieldName : fields.keySet()){
//				atts.clear();
//				hd.startElement("", "", "annotation", atts);
//				hd.endElement("", "annotation", "");
//				if(fieldName.equals("ln")){
//					atts.clear();
//					atts.addAttribute("", "","" ,"", "i2b222009_Instance_990001");
//					hd.startElement("", "", "classMention", atts);
//					atts.clear();
//					atts.addAttribute("", "", "id", "", fields.get(fieldName));
//					hd.startElement("", "", "mentionClass", atts);
//					String lnValue = fields.get(fieldName);
//					hd.characters(lnValue.toCharArray(), 0, lnValue.length());
//					hd.endElement("","mentionClass" , "");
//					hd.endElement("", "classMention" , "");
//				}
//
//
//			}
//		}
//
//		return xmlContent;
//	}
	/**
	 * Parsing i2b2 offset
	 * INPUT: offset  TYPE: string EXAMPLE:  12:0 12:1, 11:9 11:9
	 * 
	 * OUTPUT: ArrayList<HashMap<String>>
	 * 		For each HasMap including keys:
	 * @keyword "StartLine" 
	 * @keyword "StartToken"
	 * @keyword "EndLine"
	 * @keyword "EndToken" 
	 */
	public ArrayList<HashMap<String, Integer>> parseOffset(String offset){
		ArrayList<HashMap<String, Integer>> parsRlt = new ArrayList<HashMap<String, Integer>>();
		Pattern pOffSet = Pattern.compile("(\\d+)\\s*:\\s*(\\d+)\\s+(\\d+)\\s*:\\s*(\\d+)");
		Matcher mOffset = null;
		try{
			mOffset= pOffSet.matcher(offset);
		}catch(Exception error){
			System.out.println(offset);
		}
		while(mOffset.find()){
			HashMap<String, Integer> ofset = new HashMap<String, Integer>();
			ofset.put("StartLine", Integer.parseInt(mOffset.group(1)));
			ofset.put("StartToken", Integer.parseInt(mOffset.group(2)));
			ofset.put("EndLine", Integer.parseInt(mOffset.group(3)));
			ofset.put("EndToken", Integer.parseInt(mOffset.group(4)));

			parsRlt.add(ofset);
		}
		if(parsRlt.size()<1)
			System.err.println("Error in lancet parser: Could not match listed medication offset. Somthing wrong with regular expression 1042am");

		return parsRlt;
	}



	public String GetTwoLineContextByOffset(String offset){
		Pattern pOffset = Pattern.compile("(\\d+):(\\d+)\\s+(\\d+):(\\d+)");

		Matcher mm = pOffset.matcher(offset);
		int imLineStart = 0;
		int imLineEnd = 0;
		int imTokenStart = 0;
		int imTokenEnd = 0;
		if (mm.find()) {
			imLineStart = Integer.parseInt(mm.group(1));
			imLineEnd = Integer.parseInt(mm.group(3));
			imTokenStart = Integer.parseInt(mm.group(2));
			imTokenEnd = Integer.parseInt(mm.group(4));
		} else {
			System.err.println("Error in lancet parser 0506pm");
		}
		//		get drug bondary
		int iStart = this.GetArticlePositionByStartToken(imLineStart, imTokenStart);
		int iEnd = this.GetArticlePositionByEndToken(imLineEnd, imTokenEnd);




		//		get boundary for tow lines context
		int sizeOfTokens = this.GetTokenSizeOfLine(imLineEnd + 2);

		imLineStart = Math.max(imLineStart-2, 1);
		imLineEnd = Math.min(this.lines.length, imLineEnd + 2);

		int start = this.GetArticlePositionByStartToken(imLineStart, 0);
		int end = this.GetArticlePositionByEndToken(imLineEnd, sizeOfTokens -1);

		//		get taged two line context
		String context = this.content.substring(start, iStart);
		context += "<drug>";
		context += this.content.substring(iStart, iEnd);
		context += "</drug>";
		context += this.content.substring(iEnd, end);
		return context;

	}

	/*
	 * Definition: Get the distance of two entity. Distance unit: line
	 * Input: i2b2EntityA and i2b2EntityB: type HashMap<String, String>
	 * 			Example: [type="NOUN"; NOUN="pain in back"; NOUNOffset="1:3 1:5"]
	 * 
	 *        other phrase
	 *                   Drug Name             other phrase
	 * A is the main entry; most cases it is drug name
	 */
	public int GetLineDistance(HashMap<String, String> i2b2EntityA,
			HashMap<String, String> i2b2EntityB) {
		int dist = 0;
		String type = i2b2EntityA.get("type");
		String offsetA = i2b2EntityA.get(type);

		type = i2b2EntityB.get("type");
		String offsetB = i2b2EntityB.get(type);

		//get line number by offset
		//		StartLine", "StartToken", "EndLine", "EndToken" 
		ArrayList<HashMap<String, Integer>> aParse = this.parseOffset(offsetA);
		int aSize = aParse.size();
		int aStartLine = aParse.get(0).get("StartLine");
		int aEndLine = aParse.get(aSize-1).get("EndLine");

		ArrayList<HashMap<String, Integer>> bParse = this.parseOffset(offsetB);
		int bSize = bParse.size();
		int bStartLine = bParse.get(0).get("StartLine");
		int bEndLine = bParse.get(aSize-1).get("EndLine");

		if(aStartLine >= bStartLine)
			dist = aEndLine - bStartLine;
		else
			dist = bEndLine - aStartLine;

		return Math.abs(dist);
	}
	/*
	 * Definition: Return i2b2 tokens for the article. Tokens are determined by white space.
	 * 
	 * No input;
	 * OutPut: ArrayList<String> : Tokens.
	 */
	public ArrayList<String> Geti2b2TokensOfWholeArticles(){
		ArrayList<String> tokens = new ArrayList<String>();
		Scanner scanner = new Scanner(new StringReader(this.content));

		while (scanner.hasNext()) {
			String t = scanner.next();
			tokens.add(t);
		}

		return tokens;
	}
	/**
	 * @param lm
	 * @return
	 */
	public String getLineByListedMedication(String lm) {
		Pattern pOffset = Pattern.compile("(\\d+):(\\d+)\\s+(\\d+):(\\d+)");
		Matcher mm = pOffset.matcher(lm);
		int mStartLine = 0;
		int mStartToken = 0;
		if(mm.find()){
			mStartLine = Integer.parseInt(mm.group(1));
		}else{
			System.err.println("Erro in medication offset Listmedicaiton 0347 pm");
		}
		String line = lines[mStartLine - 1];
		return line;
	}


	/**
	 * @param tag
	 * @param line
	 * @return
	 */
	public static String getFieldOffset(String fieldName, String listMedication) {
		fieldName = fieldName.toLowerCase();
		HashMap<String, String> rlt = new HashMap<String, String>();
		getFeatures(listMedication, rlt);
		return rlt.get(fieldName + "TokenPosition");
	}
	public int getStartToken(int start, int end) {
        String offset = getTokenOffset(start, end);
        return parseOffset(offset).get(0).get("StartToken");
	}
	public String getMaskedContent(ArrayList<String> listMeds,
			String[] maskFields) {
		
		String highLightedText = "";
		ArrayList<HashMap<String, Integer>> tokenList = new ArrayList<HashMap<String, Integer>>();
		String[] fields = Messages.getString("i2b2.competition.2009.fields").split(",");

		for(String lm: listMeds){
			HashMap<String,String> parseRlt = new HashMap<String,String>();
			getFeatures(lm, parseRlt);
			//			m,do,mo,f,du,r,ln

			Pattern pOffset = Pattern.compile("(\\d+):(\\d+)\\s+(\\d+):(\\d+)");

			for(int fieldType =0; fieldType < fields.length; fieldType ++){
				String key = fields[fieldType] + "TokenPosition";
				if(!parseRlt.containsKey(key))
					continue;

				String offset = parseRlt.get(key);
				Matcher mOffset = pOffset.matcher(offset);
				if(mOffset.find()){
					HashMap<String, Integer> token = new HashMap<String, Integer>();
					token.put("StartLine", Integer.parseInt(mOffset.group(1)));
					token.put("StartToken", Integer.parseInt(mOffset.group(2)));
					token.put("EndLine", Integer.parseInt(mOffset.group(3)));
					token.put("EndToken", Integer.parseInt(mOffset.group(4)));
					token.put("type", fieldType);
					
					
					
					for(String mf : maskFields){
						if(mf.equals(fields[fieldType])){
							tokenList.add(token);
						}
					}
					
				}
			}

		}
		boolean withoutTag = true;
		highLightedText = highlighTokens(tokenList, withoutTag);
		
		
		
		return highLightedText;
	}
	
	/**
	 * To match: 7/19/1992
	 * @return
	 * @reference: http://www.java2s.com/Code/Java/Development-Class/DateDiffcomputethedifferencebetweentwodates.htm
	 */
	public long getTimeSpan(){
		long days = 0;
		Pattern pAdmission = Pattern.compile("Admission Date:\\s+(\\d{1,2})\\/(\\d{1,2})\\/(\\d{4})", Pattern.CASE_INSENSITIVE);
		Pattern pDischarge = Pattern.compile("Discharge Date:\\s+(\\d{1,2})\\/(\\d{1,2})\\/(\\d{4})", Pattern.CASE_INSENSITIVE);
		
		Matcher admMatch = pAdmission.matcher(content);
		Matcher dsxMatch = pDischarge.matcher(content);
		int month= 0;
		int date=0;
		int year=0;
		Date admnDate = new Date();
		Date dsxDate = new Date();

		
		if (admMatch.find()){
			month = Integer.parseInt(admMatch.group(1));
			date = Integer.parseInt(admMatch.group(2));
			year = Integer.parseInt(admMatch.group(3));
			
			admnDate.setYear(year);
			admnDate.setMonth(month);
			admnDate.setDate(date);
			System.out.print(admMatch.group() + "\t");
		}else
			System.out.print("NO MATCH: Admission date"+ "\t");
		
		if (dsxMatch.find()){
			month = Integer.parseInt(dsxMatch.group(1));
			date = Integer.parseInt(dsxMatch.group(2));
			year = Integer.parseInt(dsxMatch.group(3));
			
			dsxDate.setYear(year);
			dsxDate.setMonth(month);
			dsxDate.setDate(date);
			System.out.print(dsxMatch.group()+ "\t");
		}else
			System.out.print("NO MATCH: Discharge date"+ "\t");
		
		
		
		days = (dsxDate.getTime() - admnDate.getTime())/(1000 * 60 * 60 * 24);
		return days;
	}
	
}
