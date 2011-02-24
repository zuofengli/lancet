/**
 * 
 */
package edu.uwm.jiaoduan.i2b2.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.uwm.jiaoduan.i2b2.crf.Span;

/**
 * @author yonggang
 * @date Aug 18, 2009
 */
public class I2b2Evaluator {
	public class Score {
		public double tp = 0.0d;
		public double fp = 0.0d;
		public double fn = 0.0d;

		public double precision() {
			return (tp + fp == 0) ? 1 : tp / (tp + fp);
		}

		public double recall() {
			return (tp + fn == 0) ? 0 : tp / (tp + fn);
		}

		public double f1() {
			double p = precision();
			double r = recall();
			return (p + r == 0) ? 0 : 2 * p * r / (p + r);

		}

		public void add(Score sc) {
			tp += sc.tp;
			fp += sc.fn;
			fn += sc.fn;
		}

		@Override
		public String toString() {
			return "Score [fn=" + fn + ", fp=" + fp + ", tp=" + tp + ", f1()="
					+ f1() + ", precision()=" + precision() + ", recall()="
					+ recall() + "]";
		}

	}

	Map<String, Score> sysscores = new HashMap<String, Score>();
	Map<String, Double> patientscore = new HashMap<String, Double>();
	public static Pattern pOffset = Pattern
			.compile("(\\d+):(\\d+)\\s+(\\d+):(\\d+)");

	public void test(String goldDir, String resDir, PrintStream out) {
		sysscores.clear();
		patientscore.clear();
		int filecnt = processDir(goldDir, resDir);
		for (String tag : sysscores.keySet()) {
			out.println("System Level Scores for " + tag + ":"
					+ sysscores.get(tag).f1());
			out.println("Patient Level Scores for " + tag + ":"
					+ (patientscore.get(tag) / filecnt));
		}

	}

	private int processDir(String goldFolder, String resDir) {
		int filecnt = 0;
		try {

			File tf = new File(resDir);
			for (File f : tf.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					// TODO Auto-generated method stub
					return name.endsWith(".i2b2.entries");
				}
			})) {
				filecnt++;
//				 System.out.println("processing:" + f.getName());
				getScore(goldFolder + "/" + f.getName(), resDir + "/"
						+ f.getName());
				// if(filecnt>1)
				// break;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return filecnt;

	}

	/**
	 * @param string
	 * @param string2
	 */
	private void getScore(String goldFile, String resFile) {
		Map<Span, String> pos2tag1 = new HashMap<Span, String>();
		ArrayList<Span> spans1 = new ArrayList<Span>();
		parseSpans(resFile, pos2tag1, spans1);
		Map<Span, String> pos2tag2 = new HashMap<Span, String>();
		ArrayList<Span> spans2 = new ArrayList<Span>();
		parseSpans(goldFile, pos2tag2, spans2);
		Map<String, Score> patscores = new HashMap<String, Score>();
		int goldpos = 0;
		for (Span span : spans1) {
			String tag = pos2tag1.get(span);
			// if(tag.equals("do"))
			// System.out.println(tag + ":" + span);
			Score patscore = getScoreByTag(patscores, tag);
			if (tag.equals(pos2tag2.get(span))) {
				patscore.tp++;
			} else
				patscore.fp++;
			for (; goldpos < spans2.size(); goldpos++) {
				Span sp2 = spans2.get(goldpos);
				String tg = pos2tag2.get(sp2);
				// if(tg.equals("do"))
				// System.out.println("Gold:"+tg + ":" + sp2);
				Score fnscore = getScoreByTag(patscores, tg);
				int cmp = sp2.compareTo(span);
				if (cmp > 0) {
					break;
				} else if (cmp == 0) {
					if (!pos2tag2.get(span).equals(tag)) {
						fnscore.fn++;

					}
					continue;
				}

				fnscore.fn++;
			}

		}
		for (; goldpos < spans2.size(); goldpos++) {
			Span sp2 = spans2.get(goldpos);
			String tg = pos2tag2.get(sp2);
			Score fnscore = getScoreByTag(patscores, tg);
			fnscore.fn++;
		}

		for (String tag : patscores.keySet()) {
			Score sc = getScoreByTag(sysscores, tag);
			Score patsc = patscores.get(tag);
			double patientsc = 0;
			if (patientscore.containsKey(tag)) {
				patientsc = patientscore.get(tag) + patsc.f1();
			} else {
				patientsc = patsc.f1();

			}
			patientscore.put(tag, patientsc);
			sc.add(patsc);
			// System.out.println(tag + ":" + patsc);
		}

	}

	private Score getScoreByTag(Map<String, Score> patscores, String tag) {
		Score patscore = null;
		if (patscores.containsKey(tag)) {
			patscore = patscores.get(tag);
		} else {
			patscore = new Score();
			patscores.put(tag, patscore);
		}
		return patscore;
	}

	public static Map<String, Span> parseLine(String ln) {
		Map<String, Span> res = new HashMap<String, Span>();
		String[] tags = ln.split("\\|\\|");
		for (String tagS : tags) {
			String tag = tagS.substring(0, tagS.indexOf("=")).trim();
			Matcher mOff = pOffset.matcher(tagS);
			if (mOff.find()) {
				int startLine = Integer.parseInt(mOff.group(1));
				int startTOken = Integer.parseInt(mOff.group(2));
				int endLine = Integer.parseInt(mOff.group(3));
				int endTOken = Integer.parseInt(mOff.group(4));
				Span s = new Span(startLine, startTOken, endLine, endTOken);
				s.text = tagS.substring(tagS.indexOf("\"") + 1,
						tagS.lastIndexOf("\"")).trim();
				res.put(tag, s);
			}
		}
		return res;
	}

	public static void parseSpans(String file, Map<Span, String> pos2tag,
			ArrayList<Span> spans) {
		String[] lns = FileUtil.readLines(file);
		pos2tag.clear();
		spans.clear();
		for (String ln : lns) {
			String[] tags = ln.split("\\|\\|");
			for (String tagS : tags) {
				if(tagS.trim().length()==0)
					continue;
				String tag = tagS.substring(0, tagS.indexOf("=")).trim();
				Matcher mOff = pOffset.matcher(tagS);
				if (mOff.find()) {
					int startLine = Integer.parseInt(mOff.group(1));
					int startTOken = Integer.parseInt(mOff.group(2));
					int endLine = Integer.parseInt(mOff.group(3));
					int endTOken = Integer.parseInt(mOff.group(4));
					Span s = new Span(startLine, startTOken, endLine, endTOken);
					spans.add(s);
					if (!pos2tag.containsKey(s)) {
						pos2tag.put(s, tag);
					}
				}
			}
		}
		Collections.sort(spans);
	}

	public void combineResult(String baseline, String lancet, String outdir) {
		if (!baseline.endsWith("/") || !baseline.endsWith("\\"))
			baseline += "/";
		if (!lancet.endsWith("/") || !lancet.endsWith("\\"))
			lancet += "/";
		if (!outdir.endsWith("/") || !outdir.endsWith("\\"))
			outdir += "/";
		
		File tf = new File(baseline);
		for (File f : tf.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				// TODO Auto-generated method stub
				return name.endsWith(".i2b2.entries") || name.endsWith(".con");
			}
		})) {

			// System.out.println("processing:" + f.getName());
			String fn = f.getName();
			
			String fromFile = baseline + fn;
			String destFile = lancet + fn;
			String outFile = outdir + fn;
			
			ArrayList<String> fromLines = RawInput.getListByEachLine(fromFile, false);
			
			if (fn.endsWith(".con")){
				fn = fn.substring(0, fn.length()-4) + ".i2b2.entries";
				destFile = lancet + fn;
				outFile = outdir + fn;
				
				fromLines = ListedMedication.transform(fromLines);
				
			}
			
			ArrayList<String> destLines = RawInput.getListByEachLine(destFile, false);
			mergeFiles(fromLines, 
					destLines,
					outFile
					);
			// break;
		}
	}
/*
 * Definition: If the two results are the same in the drug name offset, outCombined 
 * 				function would be used to merge other medication fields and context attribute.
 * firstSpan: Map<String, Span>: contain a single i2b2 entry information.
 * secondSpan:  Map<String, Span>: contain a single i2b2 entry information.
 * 
 * The function would scan all the six i2b2 fields (do,mo,f,du,r,ln). In case one result is not available, the other one result would be accepted. 
 * In case both results are available, the first result is preferred.
 */
	public void outCombined(Map<String, Span> baselineSpan,
			Map<String, Span> lancetSpan, PrintStream out) {
		String[] tags = new String[] { "m", "do", "mo", "f", "du", "r", "ln" };
		int i = 0;
		for (String tag : tags) {
			Span s1 = baselineSpan.get(tag);
			Span s2 = lancetSpan.get(tag);
			if (i > 0)
				out.print("||");
			i++;
			if (s1 == null && s2 != null)
				out.print(tag + "=" + s2.toI2b2());
			else if (s2 == null && s1 != null)
				out.print(tag + "=" + s1.toI2b2());
			else if (s1 == null && s2 == null)
				out.print(tag + "=\"nm\"");
			else {
//				Experiment 1: prefer the baseline (jMerki: rule-based)
				out.print(tag + "=" + s1.toI2b2());
//				Experiment 2: prefer the second  (lancet: supervised machine learning )
//				 out.print(tag+"="+s2.toI2b2());
//				Experiment 3: prefer the longer one.
//				 out.print(tag+"="+((s1.compareTo(s2)<0)?s1.toI2b2():s2.toI2b2()));
			}

		}
		out.println();
	}

	/**
	 * @param string
	 * @param string2
	 * @param string3
	 */
	public void mergeFiles(ArrayList<String> fromLines, ArrayList<String> destLines, String outputdir) {
		try {
//			String[] lns1 = FileUtil.readLines(fromdir);
//			String[] lns2 = FileUtil.readLines(destdir);
			// Arrays.sort(lns1);
			// Arrays.sort(lns2);
			PrintStream out = new PrintStream(new FileOutputStream(outputdir));
			int goldpos = 0;
			for (String ln : fromLines) {
				Map<String, Span> firstspan = parseLine(ln);
				Span span = firstspan.get("m");
				boolean proccessed = false;

				for (; goldpos < destLines.size(); goldpos++) {
					String ln2 = destLines.get(goldpos);
					if (ln2.equals(ln)) {
						out.println(ln);
						proccessed = true;
						continue;
					}
					Map<String, Span> secondspan = parseLine(ln2);
					Span sp2 = secondspan.get("m");

					int cmp = sp2.compareTo(span);
					if (cmp > 0) {
						break;
					} else if (cmp == 0) {
//						the same in drug name token offset
//						The first one is preferred.
						outCombined(firstspan, secondspan, out);
						proccessed = true;
						continue;
					}

					out.println(ln2);
				}
//				if 
				if (!proccessed)
					out.println(ln);

			}
			for (; goldpos < destLines.size(); goldpos++) {
				out.println(destLines.get(goldpos));
			}
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

	public void filterResult(String res1dir, String outdir) {
		try {
			File tf = new File(res1dir);
			HashMap<String, Integer> dic = JMerki.GetCommonEnglishWords();
			for (File f : tf.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					// TODO Auto-generated method stub
					return name.endsWith(".i2b2.entries");
				}
			})) {
				System.out.println("processing:"+f.getName());
				PrintStream out = new PrintStream(new FileOutputStream(outdir
						+ "/" + f.getName()));
				String[] lns = FileUtil.readLines(f.getCanonicalPath());
				for (String ln : lns) {
					Map<String, Span> tag2span = parseLine(ln);
					Span medicationSpan = tag2span.get("m");
					String medication = medicationSpan.text;
					if (medication.indexOf(" ") < 0) {
						if (!dic.containsKey(medication.toLowerCase())) {
							out.println(ln);
						}else
							System.out.println(medication+" is filtered!");
					}else
						out.println(ln);

				}
				out.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		I2b2Evaluator eval = new I2b2Evaluator();
		/*
		 * Three parameters: to combine the two folder results and output the third folder
		 * Ask for yonggang : which is merged to which
		 * baseline output is merged into lancet output.
		 */
		if (args.length == 3) {
			if (args[0].equals("filter")) {
				eval.filterResult(args[1], args[2]);
			} else{
				String baseLine = args[0];
				String lancet = args[1];
				String hybrid = args[2];
				eval.combineResult(baseLine, lancet, hybrid);
			}
			// eval.combineResult("./i2b2Data/Challenge2009/crfOutput147/2",
			// "./i2b2Data/Challenge2009/jmOutput147/2","i2b2Data/Challenge2009/combineOutput147/");
		} else if (args.length == 2) {
			/*
			 * two parameter would so i2b2 evaluation
			 * 
			 */
			String goldfolder = args[0];
			String sysfolder = args[1];
			eval.test(goldfolder, sysfolder, System.out);
		} else {
			// eval.test("./i2b2Data/090601/training.ground.truth/2",
			// "./i2b2Data/090601/training.ground.truth/2", System.out);
			// System.out.println("jmOutput2");
			// eval.test("./i2b2Data/090601/training.ground.truth/2",
			// "./i2b2Data/Challenge2009/jmOutput2/2", System.out);
			// System.out.println("crfOutput2");
			// eval.test("./i2b2Data/090601/training.ground.truth/2",
			// "./i2b2Data/Challenge2009/crfOutput2/2", System.out);
			System.out.println("crfOutput147");
			eval.test("./i2b2Data/090601/training.ground.truth/2",
					"./i2b2Data/Challenge2009/crfOutput147/2", System.out);
			System.out.println("crfOutput147filter");
			eval.test("./i2b2Data/090601/training.ground.truth/2",
					"./i2b2Data/Challenge2009/crfOutput147filter", System.out);
//			System.out.println("jmOutput147");
//			eval.test("./i2b2Data/090601/training.ground.truth/2",
//					"./i2b2Data/Challenge2009/jmOutput147/2", System.out);
			System.out.println("combineOutput147");
			eval.test("./i2b2Data/090601/training.ground.truth/2",
					"./i2b2Data/Challenge2009/combineOutput147", System.out);
//			System.out.println("combineOutput147jm");
//			eval.test("./i2b2Data/090601/training.ground.truth/2",
//					"./i2b2Data/Challenge2009/combineOutput147jm", System.out);
			// System.out.println("crfOutputsw");
			// eval.test("./i2b2Data/090601/training.ground.truth/2",
			// "./i2b2Data/Challenge2009/crfOutputsw/2", System.out);
			// System.out.println("crfOutput147sw");
			// eval.test("./i2b2Data/090601/training.ground.truth/2",
			// "./i2b2Data/Challenge2009/crfOutput147sw/2", System.out);
		}
	}

}
