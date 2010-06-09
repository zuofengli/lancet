/**
 * 
 */
package edu.uwm.jiaoduan.i2b2.crf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import edu.uwm.jiaoduan.Messages;
import edu.uwm.jiaoduan.i2b2.utils.FileUtil;
import edu.uwm.jiaoduan.i2b2.utils.I2b2Evaluator;
import edu.uwm.jiaoduan.i2b2.utils.MMTXWrapper;

/**
 * @author yonggang
 * @date Aug 10, 2009
 */
public class FeatureGenerator {
	Map<Span, String> pos2tag = new HashMap<Span, String>();
	ArrayList<Span> spans = new ArrayList<Span>();
	String goldStandardFile;

	/**
	 * 
	 */
	public FeatureGenerator(String goldStandardFile) {
		this.goldStandardFile = goldStandardFile;

	}

	String[] lns = null;

	public void init(String record) {
		String semcacheDir = Messages.getString("semcacheDir");

		if (!semcacheDir.startsWith("!")) {
			boolean useCache = Boolean.parseBoolean(Messages
					.getString("useCache"));
			if (useCache)
				lns = FileUtil.readLines(semcacheDir + "/" + record);
		}
		if (goldStandardFile != null) {
			I2b2Evaluator.parseSpans(goldStandardFile + "/" + record
					+ ".i2b2.entries",pos2tag,spans);
		}
	}

	static MMTXWrapper wrapper = new MMTXWrapper();
	String curSen = null;
	String[] curSemWords = null;
	int curPos = 0;
	int curIdx = 0;

	public String getSemType(String sentence, int line, String token,
			int tokenPos) {
		if (sentence == null)
			return "nm";
		sentence = sentence.trim();
		if (sentence.length() == 0)
			return "nm";
		token = token.trim();
		if (sentence == null) {
			sentence = lns[line - 1];
		}
		if (!sentence.equals(curSen)) {
			curSen = sentence;
			curSemWords = wrapper.getSemCodes(sentence).split("\n");
			curPos = 0;
		}
		if (curPos >= curSemWords.length)
			curPos = 0;
		for (int i = curPos; i < curSemWords.length; i++) {
			String[] wordSems = curSemWords[i].split("\r");
			if (wordSems.length < 2) {
				continue;
			}
			if (token.equals(wordSems[0])) {
				curPos = i + 1;
				curIdx = 0;
				return wordSems[1];
			} else if (wordSems[0].contains(token)) {
				int idx = wordSems[0].indexOf(token);
				if (idx >= curIdx) {
					curPos = i;
					curIdx = idx;
					return wordSems[1];
				}
			}
		}
		return "nm";
	}

	public String getRuleTag(int line, int tokenPos) {
		try {
			Span curS = new Span(line, tokenPos, line, tokenPos);
			int i = Collections.binarySearch(spans, curS);
			// System.out.println(spans.indexOf(curS));
			if (i >= 0)
				return pos2tag.get(curS);
			else {
				int cnt = Math.min(-(i + 1), spans.size() - 1);
				for (int j = cnt; j >= 0; j--) {
					Span s = spans.get(j);
					if (s.contains(line, tokenPos)) {
						return pos2tag.get(s);
					}
					if (s.compareTo(curS) > 0) {
						// System.out.println("Error:not found");
						return "nm";
					}
				}
			}
			return "nm";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "nm";

	}

	public void generateCache() {

		String srcFiles = Messages
				.getString("i2b2.challenge.competition.data.folder");
		String target = Messages.getString("");
		processDir(srcFiles, target);

	}

	/**
	 * @param canonicalPath
	 * @param target
	 */
	private void processDir(String srcFiles, String target) {
		try {
			File dir = new File(srcFiles);
			for (File f : dir.listFiles()) {
				if (f.isDirectory())
					processDir(f.getCanonicalPath(), target);
				else {
					String fn = f.getName();
					if (fn.startsWith("."))
						continue;
					String[] lns = FileUtil.readLines(f.getCanonicalPath());
					PrintStream out = new PrintStream(new FileOutputStream(
							target + "/" + fn));

					for (String l : lns) {
						out.println(wrapper.getSemCodes(l));
					}
					out.close();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		FeatureGenerator gen = new FeatureGenerator(
				"C:/Documents and Settings/yonggang/workspace/jiaoduan/i2b2Data/090601/training.ground.truth/2");
		gen.init("11995");// .i2b2.entries"some", 74);
		System.out.println(gen.getRuleTag(74, 3));
		System.out
				.println(gen
						.getSemType(
								"reports an allergy to codeine and Iodine.  "
										+ "admission examination: remarkable for the following findings:                       "
										+ "she was afebrile , heart rate 72 , blood                                            "
										+ "pressure 140/70. her right lower extremity was characterized by a                   "
										+ "well-healed transmetatarsal amputation site. ",
								74, "examination", 3));
		System.out
				.println(gen
						.getSemType(
								"reports an allergy to codeine and iodine.  "
										+ "admission examination: remarkable for the following findings:                       "
										+ "she was afebrile , heart rate 72 , blood                                            "
										+ "pressure 140/70. her right lower extremity was characterized by a                   "
										+ "well-healed transmetatarsal amputation site. ",
								74, "amputation", 3));
	}

}
