package edu.uwm.jiaoduan.i2b2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import edu.uwm.jiaoduan.i2b2.utils.JMerki;
import edu.uwm.jiaoduan.i2b2.utils.ListedMedication;
import edu.uwm.jiaoduan.i2b2.utils.RawInput;

public class ErrorAnalysis {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		HashMap<String,String> drugbankCards = loadDrugBankCards();
//		System.exit(1);
		
		String gldFolder = "C:\\zuofeng\\data\\i2b2\\251\\gtruth251\\";
		ArrayList<String> fileList = new ArrayList<String>();
		RawInput.getDirectoryFile(gldFolder, fileList );
		JMerki jm = new JMerki();
		try {
			jm.initializeParser();
		} catch (IOException e) {
			e.printStackTrace();
		}
		double totalDrugname = 0;
		double missingDrugs = 0;
		HashMap<String, Integer> categories = new HashMap<String, Integer>();
		for(String gfile: fileList){
			ArrayList<String> lines = RawInput.getListByEachLine(gfile, false);
			totalDrugname += lines.size();
			for (String e: lines){
				HashMap<String, String> fields = new HashMap<String, String>();
				ListedMedication.getFeatures(e, fields );
				String medName = fields.get("m");
				
				HashMap<String, String> drugs = jm.drugLookup(medName);
				if (drugs == null){
//					System.out.println(medName);
					missingDrugs ++;
				}else{
					System.out.println(medName);
//					drugbank00727
					String cui = drugs.get("cui");
					if(drugbankCards.containsKey(cui + "Category")){
						String cats = drugbankCards.get(cui + "Category");
						String[] fsp = cats.split("\t");
						for (String cat: fsp){
							if (!categories.containsKey(cat)){
								categories.put(cat, 1);
							}
						}
					}

				}
			}
		}
		System.out.println("Total: " + totalDrugname);
		System.out.println("Missing: " + missingDrugs);
		System.out.println("Ratio: " + missingDrugs/totalDrugname);
		
		HashMap<String, Integer> allCategories = new HashMap<String, Integer>();
		for(String key : drugbankCards.keySet()){
			if (!key.contains("Category"))
				continue;
			String cats = drugbankCards.get(key);
			String[] fsp = cats.split("\t");
			for (String cat: fsp){
				if (!allCategories.containsKey(cat)){
					allCategories.put(cat, 1);
				}
			}
		}
		
		System.out.println("From " + categories.keySet().size()+ " drug categories");
		System.out.println("There is " + allCategories.keySet().size() +" drug categories"); 
	}

	private static HashMap<String, String> loadDrugBankCards() {
		String drugbank = "C:\\zuofeng\\workspace\\lancetMedExtractor\\resources\\drugBankCardsJun20_2010.txt";
		ArrayList<String> lines = RawInput.getListByEachLine(drugbank, true);
		Pattern pBegin = Pattern.compile("^#BEGIN_DRUGCARD DB([0-9]+)");
		Pattern pCategory = Pattern.compile("^# Drug_Category:");
		System.out.println("load drug bank file");
 
		HashMap<String,String> drugs = new HashMap<String,String>();
		String cui = "";
		for (int i =0; i< lines.size(); i++){
			Matcher mBegin = pBegin.matcher(lines.get(i));
			
			if (mBegin.find()){
				cui = "drugbank" + mBegin.group(1);
				drugs.put(cui, "cui");
				continue;
			}
			Matcher mCategory = pCategory.matcher(lines.get(i));
			if (mCategory.find()){
				for(int j = i + 1; j < lines.size(); j++){
					if (lines.get(j).isEmpty()){
						i = j;
						break;
					}
					if (!drugs.containsKey(cui + "Category"))
						drugs.put(cui + "Category", lines.get(j));
					drugs.put(cui + "Category", drugs.get(cui + "Category") + "\t" + lines.get(j).trim());
//					RawInput.getInput(lines.get(j));
				}
			}
			
			
			
		}
		return drugs;
	}

}
