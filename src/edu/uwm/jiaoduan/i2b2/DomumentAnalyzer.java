package edu.uwm.jiaoduan.i2b2;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import edu.uwm.jiaoduan.i2b2.utils.ListedMedication;
import edu.uwm.jiaoduan.i2b2.utils.RawInput;

public class DomumentAnalyzer {

	private static RawInput rin = new RawInput();
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String folder = "C://zuofeng//data//i2b2//251//disx251//";
		ArrayList<String> fileList = new ArrayList<String>();
		RawInput.getDirectoryFile(folder, fileList);
		
		HashMap<String, HashMap<String, Integer>> tagsList = new HashMap<String,HashMap<String, Integer>>();
		ArrayList<String> filenameList = new ArrayList<String>();
		rin.createFile("tagNumber.txt");
		
		for(String file : fileList){
			ListedMedication lm = null;
			try {
				lm = new ListedMedication(file);
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.print('.');
			
			File f = new File(file);			
//			Get time span
//			getTimeSpan(file, lm);
//			System.out.println(f.getName());
			filenameList.add(f.getName());
			
			HashMap<String, Integer> tags = getStructuredHeadings(f, lm);
			rin.writeLine("tagNumber.txt", f.getName() + "\t" + Integer.toString(tags.keySet().size()));
			tagsList.put(f.getName(), tags);
		}
		rin.CloseFile("tagNumber.txt");
		
		if (tagsList.keySet().size() > 0){
			showTagSimilarity(filenameList, tagsList);
		}

	}

	private static void showTagSimilarity(
			ArrayList<String> nameList, HashMap<String, HashMap<String, Integer>> tagsList) {
		System.out.println();
		rin  = new RawInput();
		rin.createFile("stSimilarity.tsv");
		rin.createFile("stSimilarity.txt");
		for (int i =0; i < nameList.size(); i++){
			System.out.print(nameList.get(i) + "\t");
			rin.writeFile("stSimilarity.tsv", nameList.get(i));
			
			for (int j = 0; j < i; j++){
				HashMap<String, Integer> iTags = tagsList.get(nameList.get(i));
				HashMap<String, Integer> jTags = tagsList.get(nameList.get(j));
				
				double value = getTagSimilarity(iTags, jTags);
				System.out.print("\t" + value);
				
				rin.writeFile("stSimilarity.tsv","\t" + value);
				rin.writeFile("stSimilarity.txt", nameList.get(i) + "\t" + nameList.get(j) + "\t" + value );
				
				double fi = getF1Value(nameList.get(i));
				double fj = getF1Value(nameList.get(j));
				
				double diff = Math.abs(fi - fj)/Math.max(fi, fj);
				
				rin.writeFile("stSimilarity.txt", "\t" + Double.toString(diff));
				rin.writeFile("stSimilarity.txt", "\n");
			}
			System.out.println();
			rin.writeFile("stSimilarity.tsv","\n");
		}
		rin.CloseFile("stSimilarity.tsv");
		
	}

	private static double getF1Value(String articleId) {
		String evluationResultsFolder = "C:\\zuofeng\\data\\i2b2\\251\\patientPlot\\reports-standard\\";
		String dxs = evluationResultsFolder + articleId + ".report";
		ArrayList<String> lines = RawInput.getListByEachLine(dxs, false);
	    String[] fields = lines.get(15).split("\t");
	    System.out.println(fields.toString());
		return Double.parseDouble(fields[4]);
	}

	private static double getTagSimilarity(HashMap<String, Integer> iTags,
			HashMap<String, Integer> jTags) {
		double fShared = 0;
		for (String tag: iTags.keySet()){
			if (jTags.containsKey(tag))
				fShared ++;
		}
		double iUnique = iTags.keySet().size() - fShared;
		double jUnique = jTags.keySet().size() - fShared;
		double similarity = fShared/(iUnique + fShared + jUnique);
		
		return similarity;
	}

	private static HashMap<String, Integer> getStructuredHeadings(File f, ListedMedication lm) {
//		System.out.print(f.getName() +  "\t");
		HashMap<Integer, String> sections = lm.getStructuredSections();
		HashMap<String, Integer> normalized = new HashMap<String, Integer>();
		
		for (int pos: sections.keySet()){
//			System.out.println(pos + "\t" + sections.get(pos));
			String sctName = sections.get(pos);
			if (normalized.containsKey(sctName)){
				Integer value = normalized.get(sctName);
				value += 1;
				normalized.put(sctName, value);
			}else
				normalized.put(sctName, 1);
		}
		
		return normalized;
	}
/**
 * @param file
 * @param lm
 * Display the admission and discharge date for each dsx summary and compute
 * the days between them.
 */
	private static void getTimeSpan(File f, ListedMedication lm) {
		
		System.out.print(f.getName() +  "\t");
		System.out.print(lm.getTimeSpan());
		System.out.println();
		
	}

}
