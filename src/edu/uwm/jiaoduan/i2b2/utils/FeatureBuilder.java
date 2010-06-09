package edu.uwm.jiaoduan.i2b2.utils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.uwm.jiaoduan.Messages;

public class FeatureBuilder {

	/**
	 * Zuofeng li
	 */
	
	public static void main(String[] args) {
		
		FeatureBuilder fb = new FeatureBuilder();
//		ArrayList<String> features = fb.getDrugNameStemFeature("omeprazole");
		while(true){
		String input = RawInput.getInput();
		ArrayList<String> features = fb.matchI2b2MedicationField(input);
		for(String feature: features)
			System.out.println(feature);
		}

	}

	private ArrayList<HashMap<String,String>> drugNameStemList = new ArrayList<HashMap<String,String>>() ;
	private static final RawInput rin = new RawInput();
	private JMerki jm = null;

	
	/*
	 * look up a drug name dictionary to 
	 */
	public ArrayList<String> getDrugNameStemFeature(String originWord) {
		ArrayList<String> stemFeatures = new ArrayList<String>();
		
		if(originWord == null)
			return stemFeatures;
	
		Pattern pSub = Pattern.compile("([A-Za-z0-9]*)");
		Matcher mSub = pSub.matcher(originWord);
		String subOriginWord = null;
		if(mSub.find())
			subOriginWord  = mSub.group(1);
		else
			return stemFeatures;
		
		//EndWith-abine
		String key = "";
		String rexp = "";
		if(drugNameStemList.size() ==0)
			loadWHOINNStems();
		
		for(HashMap<String,String> stem: drugNameStemList){
			String type = stem.get("TYPE");
			String affix = stem.get("STEM");
			if(type.equals("prefix")){
				rexp = "^" + affix + "[A-Za-z0-9]*";
			}else if(type.equals("suffix")){
				rexp = "[A-Za-z0-9]*" + affix + "$";
			}else if(type.equals("infix")){
				rexp = "[A-Za-z0-9]+" + affix + "[A-Za-z0-9]+";
			}else if(type.equals("any"))
				rexp = "[A-Za-z0-9]*" + affix + "[A-Za-z0-9]*";
			else
				System.err.println("i2b2.utils.FeatureBuilder: There is an error in stem type!");
			
			Pattern pAffix = Pattern.compile(rexp, Pattern.CASE_INSENSITIVE);
			Matcher mAffix = pAffix.matcher(subOriginWord);
			if(mAffix.find()){
				key = "WHOINN=" + type.toUpperCase() + "-" + affix;
				stemFeatures.add(key);
			}
		}
		
//		for cases like: Penicillins
		if(stemFeatures.size() <1){
		
			Pattern pPlural = Pattern.compile("([A-Za-z]+)s$");
			Matcher mPlural = pPlural.matcher(subOriginWord);
			if(mPlural.find())
				stemFeatures = getDrugNameStemFeature(mPlural.group(1)); 
		}
		return stemFeatures;
		
//		should return  a list of feature.
	}
	/*
	 * Read a file to load the affixes for drug name.
	 */
	private void loadWHOINNStems() {
		RawInput rin = new RawInput();
		
		ArrayList<String> lines = rin.getLinesByIngnoringAnnotation(Messages.getString("JiaoDuan.i2b2.crf.whoinn.affixes.file"),
				"#",
				false);
		
//		-fenamic acid: special case :not be considered
		Pattern pPrefix = Pattern.compile("^([A-Za-z]+)-$", Pattern.CASE_INSENSITIVE);
		Pattern pSuffix = Pattern.compile("^-([A-Za-z]+)$", Pattern.CASE_INSENSITIVE);
		Pattern pInfix = Pattern.compile("^-([A-Za-z]+)-$", Pattern.CASE_INSENSITIVE);
		Pattern pAny = Pattern.compile("^([A-Za-z]+)$", Pattern.CASE_INSENSITIVE);
//		othters any
		
		
		for(String line: lines){
			HashMap<String, String> affix = new HashMap<String,String>();
			
			String[] fields = line.split("\\t");
			if(fields.length != 4){
//				System.out.println(line + fields.length);
//				System.err.println("LoadWhoINN error!!!");
				
//				if(fields.length == 2)
//					System.out.println(line);
				continue;
			}
			
			Matcher mPrefix = pPrefix.matcher(fields[2]);
			Matcher mSuffix = pSuffix.matcher(fields[2]);
			Matcher mInfix = pInfix.matcher(fields[2]);
			Matcher mAny = pAny.matcher(fields[2]);
			if(mPrefix.find()){
				affix.put("TYPE", "prefix");
				affix.put("STEM", mPrefix.group(1));
			}else if(mSuffix.find()){
				affix.put("TYPE", "suffix");
				affix.put("STEM", mSuffix.group(1));
			}else if(mInfix.find()){
				affix.put("TYPE", "infix");
				affix.put("STEM", mInfix.group(1));
			}else if(mAny.find()){
				affix.put("TYPE", "any");
				affix.put("STEM", mAny.group(1));
			}else{
//				-dronic acid
//				-fenamic acid
//				System.out.println("not matched:" + fields[2]);
				continue;
			}
			affix.put("ID", fields[0]);
			drugNameStemList.add(affix);		
		}
	}
	/**
	 * @param residue
	 * @return
	 */
	public boolean lookupDrugNameLexicon(String residue) {
		if(jm == null){
			jm = new JMerki();
			try {
				jm.initializeParser();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return jm.isBeginWithAnEntryOfLexicon(residue, "DRUG");
	}
	/**
	 * @param originWord
	 * @return
	 */
	public ArrayList<String> getDrugNamePreSuffixFeature(String originWord) {
		
		ArrayList<String> stemFeatures = new ArrayList<String>();
		
		if(originWord == null)
			return stemFeatures;
	
		Pattern pSub = Pattern.compile("([A-Za-z0-9]*)");
		Matcher mSub = pSub.matcher(originWord);
		String subOriginWord = null;
		if(mSub.find())
			subOriginWord  = mSub.group(1);
		else
			return stemFeatures;
		
		//EndWith-abine
		String key = "";
		String rexp = "";
		if(drugNameStemList.size() ==0)
			loadWHOINNStems();
		
		for(HashMap<String,String> stem: drugNameStemList){
			String type = stem.get("TYPE");
			String affix = stem.get("STEM");
			if(type.equals("prefix")){
				rexp = "^" + affix + "[A-Za-z0-9]*";
			}else if(type.equals("suffix")){
				rexp = "[A-Za-z0-9]*" + affix + "$";
			}else if(type.equals("infix")){
				rexp = "[A-Za-z0-9]+" + affix + "[A-Za-z0-9]+";
				continue;
			}else if(type.equals("any")){
				rexp = "[A-Za-z0-9]*" + affix + "[A-Za-z0-9]*";
				continue;
			}else
				System.err.println("i2b2.utils.FeatureBuilder: There is an error in stem type!");
			
			Pattern pAffix = Pattern.compile(rexp, Pattern.CASE_INSENSITIVE);
			Matcher mAffix = pAffix.matcher(subOriginWord);
			if(mAffix.find()){
				key = "WHOINN=" + type.toUpperCase() + "-" + affix;
				stemFeatures.add(key);
			}
		}
		
//		for cases like: Penicillins
		if(stemFeatures.size() <1){
		
			Pattern pPlural = Pattern.compile("([A-Za-z]+)s$");
			Matcher mPlural = pPlural.matcher(subOriginWord);
			if(mPlural.find())
				stemFeatures = getDrugNameStemFeature(mPlural.group(1)); 
		}
		return stemFeatures;
	}
	/**
	 * @param medicationField
	 * @param residue
	 * @return For example "10 mg";
	 */
	public ArrayList<String> matchI2b2MedicationField(String residue) {
		residue = residue.trim();
		
		ArrayList<String> types = new ArrayList<String>();
		String[] parseLevel = {"dose", "route", "freq", "prn", "date", "howLong", "reason"};
		
		if(jm == null){
			jm = new JMerki();
			try {
				jm.initializeParser();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		List<HashMap<String, String>> rlt = jm.parse(residue, parseLevel, false);

		for(HashMap<String,String> match: rlt){
			if(match.get("start").equals("0"))
				types.add("JMERKI" + match.get("type").toUpperCase());
		}
		
		
		return types;
	}
	/**
	 * @param tokenIndex
	 * @param oldContext
	 * @param setence
	 * @return section type. For example ALLERGY
	 */
	public String getContextBySentence(int referToken, String oldContext, StringBuffer sentence) {
		if(jm == null){
			jm = new JMerki();
			try {
				jm.initializeParser();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		 
		HashMap<String, String> contextClue = jm.getContextClue(referToken, sentence.toString());
		if(contextClue == null)
			return oldContext.toUpperCase();
		else
			return contextClue.get("SECTIONTITLE");
	}
	/**
	 * @param residue
	 * @return
	 */
	public boolean isAnEntryOfProblemListLexicon(String residue) {
		if(jm == null){
			jm = new JMerki();
			try {
				jm.initializeParser();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return jm.isBeginWithAnEntryOfLexicon(residue, "PROBLEMLIST");
	}
	/**
	 * @param originWord
	 * @return
	 */
	public boolean isACommoEnglishWord(String originWord) {
		checkJMerki();
		
		return jm.isWithinLinuxWords(originWord);
	}
	/**
	 * 
	 */
	private void checkJMerki() {
		if(jm == null){
			jm = new JMerki();
			try {
				jm.initializeParser();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	/**
	 * @param articleName
	 * @param i 
	 * @param i
	 * @return
	 */
	public String getPartOfSpeech(String articleName, int lineIndex, int index) {
			String folder = "./pos/";
			String posFile = folder + articleName + ".pos";
			
			ArrayList<String> lines = RawInput.getListByEachLine(posFile, true);
			
			int tokenIndex =0;
			String token = "";
//			try {
				int javaLineIndex = lineIndex -1;
				String line = lines.get(javaLineIndex);
				Scanner scanner = new Scanner(new StringReader(line));;
				while (scanner.hasNext()) {
					token = scanner.next();
					
					
					if(tokenIndex  == index )
						break;
					tokenIndex++;
				}	
			Pattern pPos = Pattern.compile("/(.*?)$");
			Matcher mPos = pPos.matcher(token);
			String tokenPos = "";
			if(mPos.find())
				tokenPos  = mPos.group(1);
			else
				System.err.println("wrong in matching pos tag");
			return tokenPos.toUpperCase();
		}
}
