package edu.uwm.jiaoduan.i2b2.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.Math;

import edu.uwm.jiaoduan.Messages;
import edu.uwm.jiaoduan.i2b2.utils.*;

/**
 * @author zuofeng Li
 * 
 * Version 1.0 Fix bug for tagging tag problem list of the document
 *
 */
public class JMerki {
	
	public static void main(String[] args){
		JMerki jm = new JMerki();
		try {
			jm.initializeParser();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while(true){
			String content = " prn pain allergy Aspirin 10 mg for heart\n pain and\n Edema of leg \nEdema of leg    pain.\n pain Aspirin p.r.n. heart\n    pain and\n    Edema of\nleg     \npain.\n";
			content = RawInput.getInput();
//			String content = "aspirin 10 mg";
		
			ArrayList<String> listedMeds = jm.getListedMedication(content);
			for (String lm : listedMeds){
				System.out.println(lm);
			}
		}
	}
	/**
	 * @param content
	 * @return Only the nearest context
	 * @keyword1 clue.put("SECTIONTITLE", normSectName);
	 * @keyword1 clue.put("STARTTOKEN", Integer.toString(contextToken));
	 */
	public HashMap<String, String> getContextClue(int referToken, String content) {
		HashMap<String,String> clue = null;
		
		content = content.replaceAll("\n", " ");
		int contextToken  = -1;
		for(String normSectName: dxSectionPatterns.keySet()){
			String pStr = dxSectionPatterns.get(normSectName);

			Pattern dscSection = Pattern.compile(pStr);
			Matcher m = dscSection.matcher(content);
			if(m.find())
			{
				String parTokenOffset = Messages.getString("i2b2.parameters.discharge.section.title.maximal.line.token.offset");
				if(parTokenOffset == null){
					System.err.println("Could not find i2b2.parameters.discharge.section.title.maximal.line.token.offset parameter");
					parTokenOffset = "0";
				}
				int iOffset = Integer.parseInt(parTokenOffset);


                
				int iStartToken = lm.getStartToken(m.start(),m.end());
				if(iStartToken > iOffset)//begin with
					continue;
				
				if(iStartToken <= referToken){
					if(contextToken <0){
						contextToken = iStartToken;
					}else if(contextToken > 0 && contextToken < iStartToken){
						contextToken = iStartToken;
					}else
						continue;
				}else
					continue;
				clue = new HashMap<String,String>();
				clue.put("SECTIONTITLE", normSectName.toUpperCase());
				clue.put("STARTTOKEN", Integer.toString(contextToken));
			}
		}
		return clue;
	}
	/**
	 * @param residue
	 * @param type: "DRUG", "PROBLEMLIST" 
	 * @return
	 */
	public boolean isBeginWithAnEntryOfLexicon(String residue, String type) {
		residue = residue.toLowerCase().trim();
		
		Map<String, String> entryList = null;
		if(type.equals("DRUG"))
			entryList = drugLookup_hashBased(residue.trim());
		else if(type.equals("PROBLEMLIST"))
			entryList = this.termLookup(residue);
		else
			System.err.println("Error: Jmerki: type do not match each type" );
		
		if(entryList != null)
			return true;
		else
			return false;
	}
	/*
	 * Definition: look up for problem list in the document and returned
	 * 	i2b2 format
	 * m="singleton" -1:-1 -1:-1||do="nm" ||mo="nm" ||f="nm" ||du="nm" ||r="pain" 10:2 10:2 ||ln="narrative"
	 */
	public ArrayList<String> getListedReason(String text) {
		
		ArrayList<String> listedMed = new ArrayList<String>();
//		reset the artifical id to zero;
		resetArtificialId();
		
//		LancetParser pLancet = null;
//		try {
//			pLancet = new LancetParser(text);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		
//		check if there is "enter" symbol
		Pattern pEnter = Pattern.compile("\n");
		Matcher pEnterMatcher = pEnter.matcher(text);
		HashMap <Integer, Integer> enterList = new HashMap<Integer, Integer>();
		while(pEnterMatcher.find()){
			int start = pEnterMatcher.start();
			int end = pEnterMatcher.end();
			enterList.put(start, end);
		}

		//replace enter into space.		
		text = text.replace("\n", " ");

		Pattern pword = Pattern.compile("\\b\\w");

		Matcher ptnMatcher = pword.matcher(text);
		
		while(ptnMatcher.find())
		{
			int start = ptnMatcher.start();
			String residue = text.substring(start);

			HashMap<String, String> term = null;
			term = termLookup(residue);
			if(term == null)
				continue;
			
			int length = term.get("termName").length();
			//				int end = start + length -1; //Perl version
			int end = start + length; //JiaoDuan version
			
			String strTokens = text.substring(start, end);
			
			System.out.println(start + "-" + end);
			String strOffset = lm.getTokenOffset(start, end);
			String singleton = "m=\"singleton\" -1:-1 -1:-1||do=\"nm\" ||mo=\"nm\" ||f=\"nm\" ||du=\"nm\" ||r=\"" + strTokens + "\" " +  strOffset + "||";
			
//			jmerki is designed for listed medication
			String strLN = "list";			
			if(strLN.equals("narrative")){	
				singleton += "ln=\"narrative\"";
			}else if(strLN.equals("list")){
				singleton += "ln=\"list\"";
			}
			
			System.out.println(singleton);
			listedMed.add(singleton);
		}		
		
		return listedMed ;
	}
	/*
	 * Definition: This function is an interface for i2b2 2009 NLP challenge task;
	 * 				The output format according to the competition definition.
	 * For example: m="acetylsalicylic acid" 16:0 16:1||do="325 mg" 16:2 16:3||mo="po" 16:4 16:4||f="qd" 16:5 16:5||du="nm"||r="nm"||ln="list"
	 * 
	 */
	public ArrayList<String> getListedMedication(String dsumContent) {
		String[] topLevel = {"drug","drugClasse"};
		String[] secondLevel = {"dose", "route", "freq", "prn", "date", "howLong", "reason"};
		List<HashMap<String, String>> drugs = null;
		try {
			drugs = this.twoLevelParse(dsumContent, topLevel, secondLevel);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		try {
			return drugsToi2b2(drugs, dsumContent);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * Definition: This function used to load discharge summary or target articles in a two-level folder
	 * 				Example: CompetitionData
	 * 								       |--- gses_test290
	 * 													    |---GSE18
	 * 														|---...
	 * Input: 
	 */
	public void LoadDischargeSummary(String dischargfolder, HashMap<String, String> dischgs) throws IOException {
		ArrayList<String> fileList = new ArrayList<String>();
		RawInput.getDirectoryFile(dischargfolder, fileList);
		
		Pattern fName = Pattern.compile("/(.*?)/(\\w+?\\d+)$");
		
		for(String file: fileList)
		{
			Matcher m = fName.matcher(file);
			String filekey = new String();
			if(m.find())
				filekey = m.group(1) + "-" + m.group(2);
			else
				System.err.println("File name erro!");
			
			BufferedReader fFile = null;
			String line = new String();
			String content =new String();
			try {
				fFile = new BufferedReader(new FileReader(file));
				while ((line = fFile.readLine()) != null) {
				content += line +"\n";
				}
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			dischgs.put(filekey, content);	
		}
	}
	ListedMedication lm = null;
//	LancetParser lancet = null;
//private static final RawInput rin = new RawInput();
	
	private ArrayList<String> drugsToi2b2(List<HashMap<String, String>> drugs, String text) throws Exception {
		ArrayList<String> ListedMedictions = new ArrayList<String>();
		String FieldSeparator = Messages.getString("i2b2.field.separator");
		
		for(int i=0; i< drugs.size(); i++)
		{
			String listm = new String();
			
			HashMap<String,String> drug = drugs.get(i);
			
			if(!drug.containsKey("drugNameStart")){
				System.err.println("This list for JMerki do not contain drug name!");
				for(String key: drug.keySet())
				{
					System.err.println("---" + key + ":" + drug.get(key));
				}
				continue;
			}
			
			
			int start = Integer.parseInt(drug.get("start"));
			int end = Integer.parseInt(drug.get("end"));
			
//			System.out.println(lancet.GetTokenContent(start, end));
			
			HashMap<String, Integer> tokens = null;
			HashMap<String, String> medication = new HashMap<String, String>();
				
			int iDrugBegin =Integer.parseInt(drug.get("drugNameStart"));
		    int iDrugEnd = Integer.parseInt(drug.get("drugNameEnd"));
			
			if(drug.get("type").equals("drug"))
				tokens = lm.convertArticleRegion2TokenPosition(iDrugBegin, iDrugEnd);
			else
				tokens = lm.convertArticleRegion2TokenPosition(start, end);
			
			String drugName = drug.get("drugName");
			String NormalizedDrugName = lm.GetNormalizedTokenContent(iDrugBegin, iDrugEnd);
			
			if(NormalizedDrugName.equals(drugName)){
				listm += "m=\"" + drug.get("drugName") + "\" " + tokens.get("StartLine") + ":" + tokens.get("StartTokenPosition")+ " " + tokens.get("EndLine") + ":" + tokens.get("EndTokenPosition") + FieldSeparator;
			}else{
//				System.out.println("drug name normalized");
				listm += "m=\"" + NormalizedDrugName + "\" " + tokens.get("StartLine") + ":" + tokens.get("StartTokenPosition")+ " " + tokens.get("EndLine") + ":" + tokens.get("EndTokenPosition") + FieldSeparator;
			}
//			iMedStartLine = Integer.parseInt(medication.get("StartLine"));
//			iMedStartToken = Integer.parseInt(medication.get("StartToken"));
//			iMedEndLine = Integer.parseInt(medication.get("EndLine"));
//			iMedEndToken = Integer.parseInt(medication.get("EndToken"));
			medication.put("StartLine", Integer.toString(tokens.get("StartLine")));
			medication.put("StartToken", Integer.toString(tokens.get("StartTokenPosition")));
			medication.put("EndLine", Integer.toString(tokens.get("EndLine")));
			medication.put("EndToken", Integer.toString(tokens.get("EndTokenPosition")));
			
			String[] parts = new String[]{"dose","route","freq","howLong","prn"};
			String[] brief = new String[]{"do","mo","f","du","r"};
			
			for(int j=0 ; j< parts.length; j++)
			{
				if(!drug.containsKey(parts[j])){
					String fieldType = brief[j];
					String fieldOffset = ApplyMedicationFieldRelationModel(fieldType, medication);
					if(fieldOffset == null)
						listm += brief[j] + "=\"nm\" " + FieldSeparator;
					else
						listm += fieldOffset + FieldSeparator;
				}
				else
				{
					start = Integer.parseInt(drug.get(parts[j] + "Start"));
					end = Integer.parseInt(drug.get(parts[j] + "End"));
					tokens = lm.convertArticleRegion2TokenPosition(start, end);
					
					String Field = drug.get(parts[j]);
					String NormalizedField = lm.GetNormalizedTokenContent(start, end);
					
					if(NormalizedField.equals(Field)){
						listm += brief[j] + "=\"" + drug.get(parts[j]) + "\" " + tokens.get("StartLine") + ":" + tokens.get("StartTokenPosition")+ " " + tokens.get("EndLine") + ":" + tokens.get("EndTokenPosition") + FieldSeparator;
					}else{
//						System.out.println(parts[j] + " has been normalized");
						listm += brief[j] + "=\"" + NormalizedField + "\" " + tokens.get("StartLine") + ":" + tokens.get("StartTokenPosition")+ " " + tokens.get("EndLine") + ":" + tokens.get("EndTokenPosition") + FieldSeparator;
					}
				}
			}
			
//			listm += "ln=\"" + lm.GetListNarrative(drug.get("context")) + "\"";
			listm += "ln=\"" + drug.get("context") + "\"";
			ListedMedictions.add(listm);
			
		}
		
		return PostProcessing(ListedMedictions);		
	}
	private String ApplyMedicationFieldRelationModel(String fieldType,
			HashMap<String, String> medication) {
		// TODO Auto-generated method stub
		fieldType = fieldType.toUpperCase();
		String offset = null;
		for(HashMap<String,String> field : fieldMatched){
			String type = field.get("type");
			if(!type.equals(fieldType))
				continue;
			boolean isRelated = lm.disambiguateFieldsAroundDrugName(medication, field);
			if(isRelated)
				offset = field.get(fieldType + "offset"); 
		}
		
		return offset;
	}
	private ArrayList<String> PostProcessing(ArrayList<String> listedMedications) throws Exception {
		// filter with common words
		ArrayList<String> pp = new ArrayList<String>();
//		GenNegEx ng = new GenNegEx();
		
		LISTMEDICATION:
		for(String lm: listedMedications){
			String drugFieldValue = ListedMedication.getFieldValue("M", lm);
			Scanner scanner = new Scanner(new StringReader(drugFieldValue));
			int number = 0;
			while (scanner.hasNext()) {
				Pattern pAlphabet = Pattern.compile("\\w");
				Matcher ma = pAlphabet.matcher(scanner.next());
				if(ma.find()){
					number ++;
				}	
			}
//			will not match with more than one word
			if(number == 1){
				for(String word: linuxWords.keySet()){
					//             NO.
					Pattern pm = Pattern.compile("^" + word + "\\b");
					Matcher mm = pm.matcher(drugFieldValue);
					if(mm.find()){
						System.out.println("Filtered in PostProcessing: <filter>" + drugFieldValue + "</filter> By matching " + mm.group(0));
						continue LISTMEDICATION;
					}
				}
			}
			//filteration with negEx
//			String RawSentence = lancet.GetSentenceByListedMedication(lm);
//			String phrase = drugFieldValue;
//			if(ng.IsNegativeEvent(RawSentence, phrase)){
//				System.out.println("Filtered in PostProcessing: <filter>" + drugFieldValue + "</filter> By matching negative event" + "\n" + RawSentence);
//				continue LISTMEDICATION;
//			}
			
			
			pp.add(lm);		
		}
		
//		NegtiveAllergy filteration
		for(String lm: listedMedications){
			String drugName = ListedMedication.getFieldValue("M", lm);
			Scanner scanner = new Scanner(new StringReader(drugName));
		}
		
		return pp;
	}
	private void drugsToXML(final List<HashMap<String, String>> drugs) {
		// TODO Auto-generated method stub
		
	}
	/**
	 * input1: dsum String: lower cased article
	 *  @param String[] topLevel = {"drug","drugClasse"};
	 *  @param String[] secondLevel = {"dose", "route", "freq", "prn", "date", "howLong", "reason"};
	 * 
	 * 	@return drugs: a list of drugs. each drug has many attributes. For example,
	 */
	private List<HashMap<String, String>> twoLevelParse(String dsum,
			String[] topLevel, String[] secondLevel) throws Exception {
	    String text = dsum;
	    
//	    initial ListedMedication
	    lm = new ListedMedication(text);
//	    lancet = new LancetParser(text);
	    
//	    AppendDrugDictionaryByCRF();
	    
	    text = PreProcessing(text);

//	    the parser extracts three types of objects: drugs,possible drugs and context clues
	    boolean isTopLevel = true;    
	    
	    
	    List<HashMap<String,String>> drugs = parse(text, topLevel, isTopLevel);
	    
	    this.removeTrumpedTokens(drugs);
	    
//	    attachContextClues(drugs, "");
	    AddContextInfo(drugs);	    
	    
//	    for( HashMap<String, String> drug : drugs)
	    for(int i= 0; i< drugs.size(); i++)
	    {
	    	HashMap<String,String> drug = drugs.get(i);
	    	String drugWindowText = drug.get("text");
	    	if(drugWindowText.length() < 1)
	    		continue;
//	    	System.out.println(drug.get("text"));
	    	
	    	List<HashMap<String, String>> parts = parse(drug.get("text"), secondLevel, false);
	    	
	    	removePartialParts(parts);
	    	AddFiedsToDrug(parts, drug);

	    	if(parts != null && parts.size()>0)
	    	{
//	    		drug.put("parts", parts.toString());//big potential erro!!!
	    		drug.put("parts", parts.get(0).get("start"));
	    	}
	    	
	    	if(drug.get("type").equals("possibleDrug"))
	    	{
	    		drug.put("drugName", drug.get("text"));
	    		if(drug.containsKey("parts"))
	    		{
	    			int bgnIndex = 0;
	    			int endIndex = Integer.parseInt(drug.get("parts"));
	    			String value = drug.get("text").substring(bgnIndex, endIndex);
	    			drug.put("drugName", value );
	    		}
	    	}
	    	String NormalizedContext = normalizeContext(drug.get("context"));
	    	drug.put("context", NormalizedContext);
//	    	guessDates(drug);
	    	
	    	drugs.set(i, drug);
	    }
	   
	    drugFilter(drugs);
//	    System.out.println("drugs size:" + drugs.size());
//	    ExtendToSentence(drugs, secondLevel);
	    
//	    ExtendToMinusPlusTwoLine(drugs);
	    
	    SetMatchedFields(drugs);
	    
	    return drugs;
	}
	private String PreProcessing(String text) {
//	    the new line symbol is not needed anymore. It is replaced with a sapce.
	    text = text.replace("\n", " ");
	    
	    text = text.toLowerCase();
	    
//	    special tag word from medical record system
//	    diuresed <drug>w/ lasix </drug>40mg on day of d/c about net -500cc
//	    this cause significant reduce system performance
//	    text = text.replaceAll(" w/ ", "    ");//4 spaces
	    
	    return text;
	}
	private void AddFiedsToDrug(List<HashMap<String, String>> fields,
			HashMap<String, String> drug) {
		// TODO Auto-generated method stub
    	for(HashMap<String, String> p: fields)
    	{
    		String sType = p.get("type");
    		String sText = p.get("text");
    		drug.put(sType, sText);
    		
    		int start = Integer.parseInt(drug.get("start")) + Integer.parseInt(p.get("start"));
    		int end = Integer.parseInt(drug.get("start")) + Integer.parseInt(p.get("end"));
    		drug.put(sType + "Start", Integer.toString(start));
    		drug.put(sType + "End", Integer.toString(end));
    	}
		
	}
	private void ExtendToSentence(List<HashMap<String, String>> rawdrugs, String[] secondLevel) {
		// TODO Auto-generated method stub
		for (int i= 0; i< rawdrugs.size(); i++){
			HashMap<String,String> drug = rawdrugs.get(i);
			
			String remainLevelList = null;
			for(String level: secondLevel){
				if(rawdrugs.contains(level)){
		
					continue;
				}
				if(remainLevelList == null)
					remainLevelList = level;
				else
					remainLevelList += "\t" + level;
			}
			String[] remainLevels = remainLevelList.split("\t");
			if(remainLevels.length < 1)
				return;
			
//			exten drug.text to sentence
			int drugNameStart = Integer.parseInt(drug.get("drugNameStart"));
//			HashMap<String, String> sentence = lancet.GetSentenceByArticlePosition(drugNameStart);
			
			
			
			List<HashMap<String, String>> remainFields = parse(drug.get("text"), remainLevels, false);
			
			AddFiedsToDrug(remainFields, drug);
			
			
			rawdrugs.set(i, drug);
		}
	}
	/** append drug name based on machine learning results, especially drug names from list region
	 * No input
	 * No output
	 * the druglist variable is altered during analysis
	 */
	private void AppendDrugDictionaryByCRF() {
		// TODO Auto-generated method stub
		//potassium chloride slow release
		String sd = "\\b";
//		String[] drugNames = lancet.GetDrugNamesByCRF();
		ArrayList<String> drugNames = lm.GetDrugList();
		
		drugNames = NormalizeCRFMedications(drugNames);
		
		DRUGNAMES:
		for(String drugName: drugNames)
		{
//			String drugName = " potassium chloride slow release ";
//			HandleComplexDrugName
			drugName = drugName.toLowerCase();

			String[] fields = drugName.split(sd);
			if(fields.length < 2)
				continue;
			
			String firstWord = fields[1];
			
//			boolean isExisting = false;
//			HashMap <String, ArrayList<HashMap<String, String>>> druglist
			ArrayList<HashMap<String,String>> drugs = new ArrayList<HashMap<String,String>>();
			HashMap<String,String> newDrug = new HashMap<String, String>();
			if(druglist.containsKey(firstWord)){

				for(HashMap<String,String> drug: druglist.get(firstWord)){
					if(drug.get("drugName").equals(drugName)){
//						isExisting = true;
						continue DRUGNAMES;
					}
				}

//				if(isExisting)
				//					continue;
				//				else{
				drugs = druglist.get(firstWord);
				newDrug.put("drugName", drugName);
				drugs.add(newDrug);
				//					druglist.get(firstWord).add(newDrug);
//				}
				
			}else{
				newDrug.put("drugName", drugName);
				drugs.add(newDrug);
				
			}
//			reverese sort the drugs
			Collections.sort(drugs, new CompareByIndexNameLength("drugName"));
			druglist.put(firstWord, drugs);
		}
	}
	/*
	 * Definition: This function is used to tag Dx summary with medical term (SNOMED-CT core problem list).
	 * Input: text; Type: String.
	 * 		The input string could be single line or multiple lines.
	 * Output: String
	 */
	public String tagMedicalTerms(String text) {
//		reset the artifical id to zero;
		resetArtificialId();
		
//		check if there is "enter" symbol
		Pattern pEnter = Pattern.compile("\n");
		Matcher pEnterMatcher = pEnter.matcher(text);
		HashMap <Integer, Integer> enterList = new HashMap<Integer, Integer>();
		while(pEnterMatcher.find()){
			int start = pEnterMatcher.start();
			int end = pEnterMatcher.end();
			enterList.put(start, end);
		}

		//replace enter into space.		
		text = text.replace("\n", " ");

		Pattern pword = Pattern.compile("\\b\\w");

		Matcher ptnMatcher = pword.matcher(text);

		while(ptnMatcher.find())
		{
			int start = ptnMatcher.start();
			String residue = text.substring(start);

			HashMap<String, String> term = null;
			term = termLookup(residue);
			if(term == null)
				continue;
			
			int length = term.get("termName").length();
			//				int end = start + length -1; //Perl version
			int end = start + length; //JiaoDuan version
			String termId = getArtificialId(term.get("termName"), "T", false);
			
			//				replace the problem term with drugId
			text = text.substring(0, start) + termId + text.substring(end);
		}
		
		if(enterList.size() > 0){
			for(int start: enterList.keySet()){
				text = text.substring(0, start) + "\n" + text.substring(enterList.get(start));
			}
		}
		
		
		return text;
	}
	/*
	 * 
	 */
	private HashMap<String, String> termLookup(String residue) {
		    String text = residue;

		    String sd = "\\b";

		    Pattern	pname = Pattern.compile("^(.+?)" + sd, Pattern.CASE_INSENSITIVE);

			Matcher ptnMatcher = pname.matcher(text);

		    if(ptnMatcher.find())
			{
		    	String token = ptnMatcher.group(1);
//		    	all of data in lexicon are in lower case
		    	token = token.toLowerCase();
		    	
				int iEnd = text.length() > 30? 30: text.length();
				
				if(medicalTermlist.containsKey(token))
				{
//					System.out.println(token);
					ArrayList<HashMap<String, String>> termList = medicalTermlist.get(token);

//					sort ArrayList with decreasing order to match the maximum name
					String indexName = "termName";
					Collections.sort(termList, new CompareByIndexNameLength(indexName));
					
					for(HashMap<String,String> term: termList)
					{
						String termName_rx = "^" + term.get("termName") + sd;
						
//						Pattern pDrug = null;
						Pattern pDrug = Pattern.compile(termName_rx, Pattern.CASE_INSENSITIVE);

						ptnMatcher = pDrug.matcher(text);
						
						if(ptnMatcher.find())
							return term;
					}	 
				}
			}else
			{
				System.err.println("-----0750pmhow can text<" + text + "> be missing a right word boundary after the first word?");
			}
			
			return null;
	}
	private void AddContextInfo(List<HashMap<String, String>> drugs) throws Exception {
		// TODO Auto-generated method stub
		
		for(int i=0; i< drugs.size();i++)
		{
			HashMap<String,String> drug = drugs.get(i);
			
			int start = Integer.parseInt(drug.get("start"));
			String cntInfo = lm.getListNarrative(start);
			drug.put("context", cntInfo);
			
			drugs.set(i, drug);
		}
		
		
	}
	private void removePartialParts(List<HashMap<String, String>> parts) {
		// TODO Auto-generated method stub
		
		
		if(parts.size() < 0)
			return;
		
		for(int i=0; i< parts.size(); i++)
		{
			for (int j=0; j < parts.size(); j++)
			{
				
				if(i == j)
					continue;
				
				int pStart = Integer.parseInt(parts.get(i).get("start"));
				int pEnd = Integer.parseInt(parts.get(i).get("end"));
				
				int tpStart = Integer.parseInt(parts.get(j).get("start"));
				int tpEnd = Integer.parseInt(parts.get(j).get("end"));
				
				if(pStart >= tpStart && pEnd <= tpEnd)
				{
					HashMap<String, String> part = parts.get(i);
					part.put("subsumed", "Yes");
//					parts.get(i).put("subsumed", "1");
					parts.set(i, part);
				}
			}
		}
		
		for(int m=0; m < parts.size(); m++){
			HashMap<String,String> part = parts.get(m);
			if(part.containsKey("subsumed"))
				parts.remove(m);
		}
		
		
	}
	private String normalizeContext(String ctx) {
		// TODO Auto-generated method stub
		String context = new String();
		ctx = ctx.toLowerCase();
		if(ctx == null || ctx.equals(""))
		{
			context= "unkown";
			return context;
		}
		
		if(ctx.matches("lab")){
			context = "lab results";
		}else if (ctx.matches("in (the )?(e(r|d)|emergency)")){
			context = "Emergency room";
		}else if( ctx.matches("(history|hpi|cc|pmh)" )){
			context = "History";
		}else if( ctx.matches("at home")){
			context = "At home";
		}else if( ctx.matches("discharge medications")) {
			context = "Discharge meds";
		}else if( ctx.matches("(discontinued|dc)")) {
			context = "DC'd";
		}else if( ctx.matches("titrate")) {
			context = "Titrate off";
		}else if( ctx.matches("(hold|held)")) {
			context = "Held";
		}else if( ctx.matches("standing")) {
			context = "Standing";
		}else if( ctx.matches("HOSPITAL COURSE")) {
			context = "Hosp Course";
		}else{
			context = ctx;
		}
		return context;
	}
	private void guessDates(HashMap<String, String> drug) {
		// TODO Auto-generated method stub
		if(drug.containsKey("context"))
		{
			System.err.println("No context information");
			return;
		}
		
		String when = new String();
		String ctx = drug.get("context");
		if(ctx == null)
		{
			when = "unkown";
		}else
		{
			if(ctx.matches("(out|at home)")){
				when = "before admission";
			}else if (ctx.matches("history")){
				when = "before admission";
			}else if (ctx.matches("(hopital course|medications|emergency|hc|hosp course)")){
				when = "during hospital stay";
			}else if (ctx.matches("(discharge)")){
				when = "after discharge";
			}else {
				when = "unkown";
			}
		}
		
		drug.put("when", when);
	}
	private void drugFilter(List<HashMap<String, String>> drugs) {
		// TODO Auto-generated method stub
		for(int i =0; i< drugs.size(); i++)
		{
			boolean bFiltered = false;
			HashMap<String, String> d = drugs.get(i);
//			System.out.println(d.get("text"));
//			System.out.println(patterns.get("drugClasse"));
			String drugName = null;
//			System.out.println(patterns.get("drugClasse"));
			if(d.containsKey("drugName"))
				drugName = d.get("drugName").toLowerCase();
			else
				continue;
			
			int start = Integer.parseInt(d.get("drugNameStart"));
			
			
			int end = Integer.parseInt(d.get("drugNameEnd"));
			drugName = lm.GetTokenContent(start, end);
		
//			prepare for filter 1
			boolean isaPossibleDrug = false;
			if(d.containsKey("type")){
				if(d.get("type").equals("possibleDrug"))
					isaPossibleDrug = true;
			}
			boolean existsRoute = d.containsKey("route");
			
			boolean RouteEqualOr = false;
			if(existsRoute)
			{
				String rt = d.get("rout");
				if(rt != null)
					RouteEqualOr = rt.equals("or");
			}
			
			boolean legalFreq = false;
			if (d.containsKey("freq")){
				if(d.get("freq").length() > 0){
					legalFreq = true;
				}
			}
			
			boolean legalPrn = false;
			if(d.containsKey("prn")){
				if(d.get("prn").length() > 0){
					legalPrn = true;
				}
			}
//			end of filter 1
			
			if( isaPossibleDrug && existsRoute && RouteEqualOr && !(legalFreq || legalPrn) ){
				
			}else if (d.containsKey("context"))
			{
				String context = d.get("context");
				if(context != null)
					bFiltered = context.equals("lab results");
			}else if (d.get("followingText").matches("\\s*panel")){
				bFiltered = true;
			}else if (d.get("followingText").matches("\\s*deficiency")){
				bFiltered = true;
			}else if (drugName.matches("^iron$") && d.get("followingText").matches("\\s*of")){
				bFiltered = true;
			}
			
			if(bFiltered)
			{
				drugs.remove(i);
				drugs.remove(i);
				System.out.println(drugName + "is filted by default filteration");
				continue;
			}		
			
//			term filteration
			Pattern nonWord = Pattern.compile("\\W", Pattern.CASE_INSENSITIVE);
			Matcher nwMatcher = nonWord.matcher(drugName);
			if(!nwMatcher.find())
				continue;
			
			boolean isTerm = false;
			for(String firstWord: medicalTermlist.keySet()){
//				HashMap <String, ArrayList<HashMap<String, String>>>
				for(HashMap<String,String> termList: medicalTermlist.get(firstWord)){
					if(!termList.containsKey("termName"))
						System.err.print("erro in drugFilter of Jmerki: 1131 AM");
					String term = termList.get("termName");
					
					if(term.contains(drugName)){
						isTerm = true;
						break;
					}
				}
				
				if(isTerm)
					break;
			}
			if(isTerm)
			{
				drugs.remove(i);
				System.out.println(drugName + "is filted by medical term filteration");
				continue;
			}	
//			end of term filteration
			
			
//			System.out.println();
			
			
		}
	}
	private void attachContextClues(List<HashMap<String, String>> tokens, String outsideContextClue) {
		// TODO Auto-generated method stub
		
//		my $outsideContextClue = shift || '';

//		$tokens = $self->tokenSort($tokens);
		ArrayList<Integer> clueIndexes = new ArrayList<Integer>();
		
	    String context = outsideContextClue;
	    for (int i =0; i< tokens.size(); i++)
	    {
	    	if(tokens.get(i).get("type").equals("context"))
	    	{
	    		context = outsideContextClue;
//	    		context = lm.GetContextCue(Integer.parseInt(tokens.get(i).get("start")));
	    		String clue = tokens.get(i).get("text");
	    		if(context.trim().toUpperCase().equals(clue.trim().toUpperCase()))
	    		{
	    			context = "";
	    		}
	    		context += (context.length()> 0) ? ";" : "";
	    		context +=clue;
	    		if(context.matches("\\blab(oratory)?"))
	    		{
	    			context = "lab results";
	    		}
	    		clueIndexes.add(i);
	    		continue;
	    	}
	    	tokens.get(i).put("context", context);
	    }
	    
//	    remove contextClues from the token list, we're done with them
	    Collections.sort(clueIndexes, Collections.reverseOrder());
	    for(int i: clueIndexes)
	    {
	    	tokens.remove(i);
	    }
		
	}
	private void removeTrumpedTokens(List<HashMap<String, String>> toks) {
		// TODO Auto-generated method stub
//	     remove trumped tokens, if there's an overlap at any point, remove trumpee
		
//	    my $trumps = $self->{trumps};
		ArrayList<Integer> trumped = new ArrayList<Integer>();
	    
	    for(HashMap<String,String> t: trumps)
	    {
	    	String trumper = t.get("trumper");
	    	String trumpee = t.get("trumpee");
//	    	System.out.println(trumper + " trumping " + trumpee);
	    	
	    	TRUMPEE:
	    	for(int i = 0; i < toks.size(); i++)
	    	{
	    		HashMap<String, String> testForTrumpee  = toks.get(i);
	    		
	    		if(testForTrumpee.get("type").equals(trumpee))
	    		{
	    			for(HashMap<String,String> testForTrumper : toks)
	    			{
	    				if(testForTrumper.get("type").equals(trumper))
	    				{
	    					int trumperStart = Integer.parseInt(testForTrumper.get("start"));
	    					int trumperEnd = Integer.parseInt(testForTrumper.get("end"));
	    					int trumpeeStart = Integer.parseInt(testForTrumpee.get("start"));
	    					int trumpeeEnd = Integer.parseInt(testForTrumpee.get("end"));
	    					System.err.printf("does [%d-%d, %s, %s] trump tok %d: [%d-%d, %s, %s]?  ",
	    							trumperStart, 
	    							trumperEnd, 
	    							testForTrumper.get("type"),
	    							testForTrumper.get("text"),
	    	                        i,
	    	                        trumpeeStart,
	    	                        trumpeeEnd,
	    	                        testForTrumpee.get("type"),
	    	                        testForTrumpee.get("text")
	    							);
	                        if((trumpeeStart >= trumperStart  &&  trumpeeStart <= trumperEnd)  ||
		                            ( trumpeeEnd >= trumperStart && trumpeeEnd <= trumperEnd ))
	                        {
//	                        	System.out.println("Yes");
	                        	trumped.add(i);
	                        	continue TRUMPEE;	                   
	                        }
	                        else
	                        {
//	                        	System.out.println("No \n");
	                        }	    					
	    				}
	    			}
	    		}
	    	}
	    	
	    }
//	    System.out.println("got " + toks.size() + " coming in,");
	    Collections.sort(trumped,Collections.reverseOrder());
	    for(int index: trumped)
	    {
	    	toks.remove(index);
	    }
//	    map { splice @$toks, $_, 1 } reverse @trumped;  # have to do it in reverse order so don't clobber wrong indexes
//	    System.out.println(toks.size() + " going out");
	}
	/**
	 * 
	 * @param InputText
	 * @param parseLevel {"dose", "route", "freq", "prn", "date", "howLong", "reason"};
	 * @param isTopLevel
	 * @return
	 */
	public List<HashMap<String, String>> parse(String InputText, String[] parseLevel, boolean isTopLevel) {
		ArrayList<String> nonTerminalsToParse_Ex = new ArrayList<String>(); 
		if (parseLevel.length < 0)
		{
			nonTerminalsToParse_Ex = (ArrayList<String>)this.nonTerminalsToParse;
		}else{
			for(int i=0; i< parseLevel.length; i++)
			{
				nonTerminalsToParse_Ex.add(parseLevel[i]);
			}
		}
		
	    String lctext = InputText.toLowerCase();
	    
	    String strippedlctext = lctext.trim();
	    String strippedtext = InputText.trim();
	    
//	    System.out.println("parsing: " + text);
	    
//		
	    
		resetArtificialId();
		

		HashMap<String, HashMap<String,String>> drugsMatched = new HashMap<String, HashMap<String,String>>();	
		strippedlctext = tagDrugNames(strippedlctext, drugsMatched);
		
//	    tag medical term
		resetArtificialId();
		strippedlctext = tagMedicalTerms(strippedlctext);
		
	    ArrayList<HashMap<String, String>> nonTerminalsMatched = new ArrayList<HashMap<String,String>>();
	    
//	    print "drugs matched: ", Dumper($drugsMatched);
//	    parse with drug rules
//	    PatternMatcher ptnMatcher = new Perl5Matcher();
//	    PatternCompiler patternCompiler = new Perl5Compiler();
		
//		String strRe = "(D\\d+D+)";
		String strDrugNameRe = "(" + patterns.get("drugname") + ")";
		Pattern ptnConvterdDrugName = Pattern.compile(strDrugNameRe);

		// try {
		// ptext = patternCompiler.compile(strRe);
		// } catch (MalformedPatternException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// parse with drug rules
		for (String nonTerm : nonTerminalsToParse_Ex) {

			String pattern = patterns.get(nonTerm);
//			System.out.println(patterns.get("Medications"));
			// System.out.println(nonTerm + ":" + pattern);
			// String strpTerm = "(" + patterns.get("medicalTerm") + ")";
			// if(strippedlctext.matches(strpTerm))
			// System.out.println();

			ArrayList<HashMap<String, String>> matches = new ArrayList<HashMap<String, String>>();
			matchPattern(strippedlctext, pattern, nonTerm, matches);// ????
			// System.out.println(pattern);
			// System.out.println();
			// String strDrugNamePattern = patterns.get("drugname");

			for (HashMap<String, String> m : matches) {
				// put drugname back in matched text # what about non-drug
				// matches? what is this doing?
				m.put("type", nonTerm);

				String mText = m.get("text");
				mText = mText.replace("\n", " ");
				Matcher pDrugId = ptnConvterdDrugName.matcher(mText);
				boolean found = false;
				while (pDrugId.find()) {
					String sDrugid = pDrugId.group(1);
					// mText = mText.replaceFirst("D\\d+D+",
					// drugsMatched.get(sDrugid).get("drugName"));
					int mStart = pDrugId.start();
					int textStart = Integer.parseInt(m.get("start"));

					String startPosition = Integer.toString(textStart + mStart);

					sDrugid += "-" + startPosition;
					if (drugsMatched.get(sDrugid) == null) {
						System.err.println(sDrugid + " not found");
						continue;
					}
					mText = mText.replaceFirst(strDrugNameRe, drugsMatched.get(
							sDrugid).get("drugName"));
					m.put("text", mText);

					String sType = m.get("type");
					if (sType.equals("drug")) {
						HashMap<String, String> dMatch = drugsMatched
								.get(sDrugid);
						int bgnIndex = Integer.parseInt(dMatch.get("start"));
						int endIndex = Integer.parseInt(dMatch.get("end"));
						endIndex = Math.min(endIndex, strippedtext.length());
						m.put("drugName", strippedtext.substring(bgnIndex,
								endIndex));
						m.put("drugNameStart", Integer.toString(bgnIndex));
						m.put("drugNameEnd", Integer.toString(endIndex));
					}
					found = true;
					break;
				}
				if (!found && isTopLevel) {
					m.put("drugNameStart", m.get("start"));
					m.put("drugNameEnd", m.get("end"));
				}
				// m.put("text", mText);
				int iBegin = Integer.parseInt(m.get("start"));
				int iEnd = Integer.parseInt(m.get("end"));
				iEnd = Math.min(InputText.length(), iEnd);
				m.put("text", InputText.substring(iBegin, iEnd));

				if (isTopLevel)
					AddMedicationContex(iBegin, iEnd, m);

				// Pattern pTerm = Pattern.compile(patterns.get("medicalTerm"));
				// Matcher mTerm = pTerm.matcher(mText);
				// while(mText.matches(regex))

				nonTerminalsMatched.add(m);
			}
		}

		for (HashMap<String, String> tok : nonTerminalsMatched) {
			tok.put("untrimmedText", tok.get("text"));
			tok.put("text", tok.get("text").trim());
			if (tok.get("untrimmedText").equals(tok.get("text"))) {
				tok.remove("untrimmedText");
			}
		}

		return tokenSort(nonTerminalsMatched);

	}
	private void AddMedicationContex(int begin, int end, HashMap<String, String> m) {
		// TODO Auto-generated method stub
		HashMap<String,String> cnt = lm.getUpContext(begin, end);
		m.put("UpContext", cnt.get("UpContext"));
		m.put("DownContext", cnt.get("DownContext"));
		int contexBegin = Math.max(0,begin - cnt.get("UpContext").length());
		int contexEnd = begin + cnt.get("DownContext").length();
		
		m.put("UpContextBegin", Integer.toString(contexBegin));
		m.put("DownContexEnd", Integer.toString(contexEnd));
	}
	@SuppressWarnings("unchecked")
	private List<HashMap<String, String>> tokenSort(
			ArrayList<HashMap<String, String>> nonTerminalsMatched) {
		// TODO Auto-generated method stub
		ArrayList<HashMap<String,String>> tokens = nonTerminalsMatched;
		Collections.sort(tokens, new CompareByStartLocation());
		return tokens;
	}
	private void matchPattern(String strippedlctext, String pattern, String nonTerm, ArrayList<HashMap<String, String>> matches) {
		// TODO Auto-generated method stub
	String text = strippedlctext;
		
//	    PatternMatcher ptnMatcher = new Perl5Matcher();
//	    PatternCompiler patternCompiler = new Perl5Compiler();
		
		String strRe = "(" + pattern + ")";
//		if(nonTerm.equals("freq"))
//			System.out.println("freq");
		
//		System.out.println("pattern:" +nonTerm + ":\t" + strRe);
		Pattern pword = Pattern.compile(strRe);
//		try {
//			pword = patternCompiler.compile(strRe);
//			
//		} catch (MalformedPatternException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		PatternMatcherInput ptnMatcherInput;
//		ptnMatcherInput = new PatternMatcherInput(text);
		
		Matcher ptnMatcher = pword.matcher(text); 
//		System.out.println("Begin looking for .." + nonTerm);
		
		
		while (ptnMatcher.find()) {
//			MatchResult rlt = ptnMatcher.getMatch();
			String matchedText = ptnMatcher.group(1);
//			System.out.println(matchedText);
//			int matchStart = ptnMatcherInput.getMatchBeginOffset();
//			int matchEnd = ptnMatcherInput.getMatchEndOffset();
			int matchStart = ptnMatcher.start();
			int matchEnd = ptnMatcher.end();
			
//			System.out.println(ptnMatcher.group(1));
//			System.out.println(ptnMatcher.group());
//			Pattern spBegin = null;
//			Pattern spEnd = null;
//			try {
			Pattern spBegin = Pattern.compile("^(\\s+)");
			Pattern spEnd = Pattern.compile("(\\s+)$");
//			} catch (MalformedPatternException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			Matcher ptnEx = spBegin.matcher(matchedText);
//			if(ptnMatcher.matches(matchedText, spBegin))
			if(ptnEx.find())
			{
				
				matchStart += ptnEx.group(1).length();
				matchedText = matchedText.replaceFirst(ptnEx.group(1), "");
			}
			
			ptnEx = spEnd.matcher(matchedText);
//			if(ptnMatcher.matches(matchedText, spEnd))
			if(ptnEx.find())
			{
				matchedText = matchedText.replace("(\\s+)$", "");
//				rlt = ptnMatcher.getMatch();
//				matchEnd -= rlt.group(1).length();
				matchEnd -= ptnEx.group(1).length();
			}
			HashMap <String, String> match = new HashMap<String, String>();
			
//			matchEnd = GetMinimumDrugWindowRightSpan(text, matchEnd);
//			matchedText = text.substring(matchStart, matchEnd);
//			System.out.println(matchedText);
			
			
			match.put("text", matchedText);
			match.put("start", Integer.toString(matchStart));
			match.put("end", Integer.toString(matchEnd));
			match.put("length", Integer.toString(matchEnd - matchStart + 1));
			
			
			
			int bgnIndex = Math.max(0, matchStart-30);
			int endIndex = bgnIndex + Math.min(30, matchStart) - 1;
			endIndex = Math.max(0, endIndex);
			
			match.put("precedingText", text.substring(bgnIndex , endIndex));
			bgnIndex = (matchEnd + 1);
			endIndex = (bgnIndex + 30 -1);
			
//			endIndex = GetMinimumDrugWindowRightSpan(text, endIndex);
			Pattern pp = Pattern.compile("^D\\d+D+(.*)one month");
			Matcher mm = pp.matcher(text);
			
			if(mm.find())
				System.out.println();
			
			if(bgnIndex >= text.length())
			{
				match.put("followingText", "");
			}
			else
			{
				endIndex = Math.min(endIndex, text.length());
				match.put("followingText", text.substring(bgnIndex, endIndex));
//				System.out.println(text.substring(bgnIndex, endIndex));
			}
			

			
			matches.add(match);
		}
	}
	
//	private int GetMinimumDrugWindowRightSpan(String text, int startPosition) {
//		// TODO Auto-generated method stub
//		for(int i= startPosition; i< text.length(); i++){
//			String posStr = text.substring(i, i+1);
//			if(posStr.equals(".")){
//				return i;
//			}
//		}
//		return startPosition;
//	}
	private String tagDrugNames(
			String strippedlctext, HashMap<String, HashMap<String, String>> drugsMatched) {
		// TODO Auto-generated method stub
//		 finds drugnames in text, creates a drug object (?)
//		 replaces occurrence with drugId which is a key into
//		 the drugsMatched hash and is also the same length as
//		 the drugname (so that subsequent offsets will be correct)
		
		String text = strippedlctext;            // lower case already
		
//	    PatternMatcher ptnMatcher = new Perl5Matcher();
//	    PatternCompiler patternCompiler = new Perl5Compiler();
//		Pattern pword = null;
//		try {
//			the beginning of a word
		Pattern pword = Pattern.compile("\\b\\w");
		
//		} catch (MalformedPatternException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		PatternMatcherInput ptnMatcherInput;
//		ptnMatcherInput = new PatternMatcherInput(text);
		Matcher ptnMatcher = pword.matcher(text);
		
//		while (ptnMatcher.contains(ptnMatcherInput, pword))
		while(ptnMatcher.find())
		{
//			int start = ptnMatcherInput.getMatchBeginOffset();
			int start = ptnMatcher.start();
			String residue = text.substring(start);
//			System.out.println(start + "\t:" + residue);
			
			HashMap<String, String>drug = null;
			Pattern p = Pattern.compile("^potassium", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(residue);
			if(m.find())
				System.out.println();
			drug = drugLookup(residue);
			if(drug == null)
				continue;
			
			int length = drug.get("drugName").length();
//			int end = start + length -1; //Perl version
			int end = start + length; //JiaoDuan version
			String drugId = getArtificialId(drug.get("drugName"), "D", false);
			
			HashMap<String, String> Matcheddrug = new HashMap<String, String>();
			Matcheddrug.put("type", "drug");
			Matcheddrug.put("drugName", drug.get("drugName"));
			Matcheddrug.put("start", Integer.toString(start));
			Matcheddrug.put("end", Integer.toString(end));
			
			String startPosition = Integer.toString(start);
			
			drugsMatched.put(drugId + "-" + startPosition, Matcheddrug);
//			replace the drug name with drugId
			text = text.substring(0, start) + drugId + text.substring(end);
		}
//		System.out.println("old:" + strippedlctext);
//		System.out.println("new:" + text);
//		System.out.println("---!!!Tag Drug names end \n" );
		
//		Tag compound drug name
		Pattern pComDrug = Pattern.compile(patterns.get("compoundDrugName"));
		Matcher mComDrug = pComDrug.matcher(text);
		while(mComDrug.find()){
			int start = mComDrug.start();
			int end = mComDrug.end(); //JiaoDuan version
			String drugName = text.substring(start, end);
			String drugId = getArtificialId(drugName, "D", false);
			
			HashMap<String, String> Matcheddrug = new HashMap<String, String>();
			Matcheddrug.put("type", "drug");
			Matcheddrug.put("drugName", drugName);
			Matcheddrug.put("start", Integer.toString(start));
			Matcheddrug.put("end", Integer.toString(end));
			
			String startPosition = Integer.toString(start);
//			drugId += "-" + startPosition;
//			we could not change drugId length. This is ued to replace the article position
			drugsMatched.put(drugId + "-" + startPosition , Matcheddrug);
//			replace the drug name with drugId
			text = text.substring(0, start) + drugId + text.substring(end);
		}
		
		
		
		return text;
	}
	
	private String getArtificalIdWithoutDrugId(String drugname, String singleWord){
		String sId = "";
		for(int i = 0; i < drugname.length(); i++){
			sId += singleWord.toUpperCase();
		}
		return sId;
	}
	/**
	 * 
	 * @param strTerm
	 * @param singleWord D: drugName; T: medical term
	 * @param bWithDrugId if possible, add drug id in the name like DDD10D
	 * @return  DDDD or DDD10D
	 */
	private String getArtificialId(String strTerm, String singleWord, boolean bWithDrugId) {
        String sId = singleWord + Integer.toString(index);
        
        if(bWithDrugId){
        if(sId.length() >= strTerm.length()){
        	sId = getArtificalIdWithoutDrugId(strTerm, singleWord);
        }else{
        	int repeatNum = strTerm.length() -1 -Integer.toString(index).length();
        	for(int i=0; i< repeatNum; i++){
        		sId += singleWord;
        	}
        	if(sId.length() != strTerm.length()){
        		System.err.println(" id isn't same length as drugname:[" + strTerm + " " + sId + "]");
        	}
        }
        }else
        	sId = getArtificalIdWithoutDrugId(strTerm, singleWord);
        
        index++;
        return sId;
	}
	private HashMap<String, String> drugLookup(String substring) {
		// TODO Auto-generated method stub
		Map<String, String> drug = null;
		if(drugSearchMethod.equals("treeBased"))
		{
			drug = drugLookup_treeBased(substring); 
		}else if(drugSearchMethod.equals("hashBased"))
		{
			drug = drugLookup_hashBased(substring);
		}else if(drugSearchMethod.equals("binarySearchBased"))
		{
			drug = drugLookup_binarySearchBased(substring);
		}
		
		return (HashMap<String, String>) drug;
	}
	private Map<String, String> drugLookup_binarySearchBased(String substring) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, String> drugLookup_hashBased(String residue) {
		
	    String text = residue;

	    String sd = "\\b";
//	    String sd = "\\s+";
//	    String[] words = text.split(sd);

//	    PatternMatcher ptnMatcher = new Perl5Matcher();
//	    PatternCompiler patternCompiler = new Perl5Compiler();
//		Pattern pname = null;
//		try {
	    Pattern	pname = Pattern.compile("^(.+?)" + sd);
//		} catch (MalformedPatternException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	    
	    ArrayList<HashMap<String,String>>  MedicationList = new ArrayList<HashMap<String,String>>();
	    
	    
		Matcher ptnMatcher = pname.matcher(text);
//		if(ptnMatcher.contains(text, pname))
	    if(ptnMatcher.find())
		{
//			MatchResult rlt = ptnMatcher.getMatch();
			
//	    	Get first word. May be a drug name			
//			String token = rlt.group(1);
	    	String token = ptnMatcher.group(1);
			int iEnd = text.length() > 30? 30: text.length();
//			if(token.equals("azithromycin"))
//				System.out.println("First word of  '" + text.substring(0, iEnd) + "' is '" + token + "'");
			
			if(druglist.containsKey(token))
			{
//				if(token.equals("potassium"))
//					System.out.println();
//				System.out.println(token);
				ArrayList<HashMap<String, String>> synDrugList = druglist.get(token);

//				sort ArrayList with decreasing order to match the maximum name
				String indexName = "drugName";
				Collections.sort(synDrugList, new CompareByIndexNameLength(indexName));
				int index = 0;
				for(HashMap<String,String> drug: synDrugList)
				{
//					this bug is found by different name of same drug: warfarin and warfarin sodium (2-832876).
//					with out "^", warfarin sodium will be recognized first. This will cause wrong drug name length and make an exception.
					drug.put("DrugLookupIndex", Integer.toString(index));
					index++;
//					String dname_rx = drug.get("drugName") + sd;
					String drugName = drug.get("drugName");
					
////					potassium chloride slow rel.
//
//					String dname_rx = "^" + drugName;
//					if(!drugName.contains(" "))
//						dname_rx += sd;

//                  Heme C					
//					�this was confirmed with a Heme Consult. D39D recommended rechecking
//					in this case without sd will make system corrupt.
					drugName = RawInput.normalizeWord(drugName);
					String dname_rx = "^" + drugName + sd;
					
//					Pattern pDrug = null;
//					\^potassium chloride 10meq in plastic container\b
//					this line cause system greate erro!
//					because I add "^" in the normalization code.
//					dname_rx = lancet.NormalizeWord(dname_rx);
					Pattern pDrug = Pattern.compile(dname_rx);

					ptnMatcher = pDrug.matcher(text);
					
					if(ptnMatcher.find()){
						boolean bAccept = DisambiguateDrugName(drug, residue, ptnMatcher.end());
						if(bAccept)
							return drug;
						else
							System.out.println("<filter>" + drugName + " is filtered in druglookup" +"</filter>");
					}
					
				}	 
			}
		}else
		{
			System.err.println("how can text <" + residue + "> missing a right word boundary after the first word?");
			
		}
	    
		return null;
	}

	private boolean DisambiguateDrugName(
			HashMap<String, String> drug, String residue, int iEnd) {
		// TODO Auto-generated method stub
		//disambiguation with parts
		//Imdur 60 mg
//		advair 250/50 bid
		
		String drugName = drug.get("drugName");
		
		boolean bRlt = true;
//		String[] parts = new String[]{"dose","route","freq","howLong","reason"};
//		for case: atrovent hfa inhaler
		String[] parts = new String[]{"dose","freq","howLong","reason"};
		for(String part: parts){
			Pattern pPart = Pattern.compile(patterns.get(part));

			Matcher pm = pPart.matcher(residue);
			if(pm.find()){
				int iStart = pm.start();
				if(iStart < iEnd){
					bRlt = false;
					bRlt = RescueSpecialDrugNameCase(drugName);
				}
			}
		}
		return bRlt;
	}
	private boolean RescueSpecialDrugNameCase(String drugName) {
		// TODO Auto-generated method stub
//		case1: rescue //advair diskus 250/50 ( fluticasone propionate/... ) 1 puff inh bid
		Pattern scPattern = Pattern.compile("(\\w+\\s*)\\d+/\\d+");
		Matcher scMatcher = scPattern.matcher(drugName);
		if(scMatcher.find()){
			return true;
		}
//		case2: >vit. b-3
		Pattern hyphenPattern = Pattern.compile("-(\\s*)\\d+");
		scMatcher = hyphenPattern.matcher(drugName);
		if(scMatcher.find())
			return true;
		
		System.out.println("rescueSpecialDrugNameCase <filter>" + drugName + "</filter> Is filtered.");
		return false;
	}
	private Map<String, String> drugLookup_treeBased(String substring) {
		// TODO Auto-generated method stub
		return null;
	}
	/*
	 * Definition: Reset the artificial index 
	 * 	For example, T3181TTTTTTTTTT, the 3181 is the index for taggged string.
	 * 
	 */
	private void resetArtificialId() {
		index = 0;		
	}
	
	String drugSearchMethod = "hashBased";
	// treeBased, binarySearchBased, or hashBased, binary not working right
	
	int SHORTEST_DRUG_NAME = 3;
	String RESOURCE_DIR = "./merki/";
	static String trainDataFolder = "./i2b2Data/i2b2 090601/trainingdata/";
	
//	public void JMerki() throws IOException
//	{
////		InitializeParser();
//	}

	HashMap<String, LinkedList<String>> terminals = new HashMap<String, LinkedList<String>>();
	List<Object> nonTerminals = new LinkedList<Object>();
	List<String> nonTerminalsToParse = new LinkedList<String>();
	HashMap<String, Object> convenienceRules = new HashMap<String, Object>();
	LinkedList<HashMap<String, String>> trumps = new LinkedList<HashMap<String,String>>();
	List<String> drugnameStoplist = new LinkedList<String>();
	
	String splitDelim = new String(); //Merki:# split on word boundaries, between number and something else and non-word chars, I THINK
	
	int index = 0;
	
	////
	HashMap <String, ArrayList<HashMap<String, String>>> druglist = new HashMap<String, ArrayList<HashMap<String, String>>>();
	HashMap <String, ArrayList<HashMap<String, String>>> medicalTermlist = new HashMap<String, ArrayList<HashMap<String, String>>>();
	Map<String, String> patterns = new HashMap<String, String>();
	
//	
	ArrayList<HashMap<String,String>> fieldMatched = new ArrayList<HashMap<String,String>>();

	private int DEBUG = 0;
//	private StanfordParserWrapper stanfordParser = null;
	private static HashMap<String, String> dxSectionPatterns =  new HashMap<String,String>();;
	private static HashMap<String, Integer> linuxWords = new HashMap<String, Integer>();

	public void initializeParser() throws IOException {
		//	    String yamlfile = RESOURCE_DIR + "drugParseRules.yaml";
		String yamlfile = Messages.getString("jMerki.YAML.FilePath");
	    Object ob = RawInput.loadYAMLfile(yamlfile);
	    HashMap<String, Object> data = (HashMap<String, Object>)ob;
//	    terminals
	    terminals = (HashMap<String,LinkedList<String>>)data.get("terminals");
	    
//	    nonterminals
	    nonTerminals = (List<Object>)data.get("nonTerminals");
	    
//	    nonTerminalsToParse
	    nonTerminalsToParse = (List<String>)data.get("nonTerminalsToParse");
	    
//	    convenienceRules:
	    convenienceRules = (HashMap<String, Object>)data.get("convenienceRules");
	    
//	    System.out.println(terminals.get("drugname"));
//	    trumps

	    trumps = (LinkedList<HashMap<String,String>>)data.get("trumps");
	    
	    drugnameStoplist = (List<String>)data.get("drugnameStoplist");
	    
//	    '$' is a special character for Java regex engine. You should escape it as "\\$" in a Java string.
//	    \\$ does not work for dot at the end
	    splitDelim = "(\\b|(?=\\d)|(?<=\\d)|(?=\\W)|$)";
//	    splitDelim = "((\\b)|$)";
	    
	    
	    applyConvenienceRules();
	    makeTerminalPatterns();
	    makeNonTerminalPatterns();
	    
//	    adding RxNorm Lexicon
	    String dictFileName = Messages.getString("jMerki.drug.dictionary");
	    getDruglist(dictFileName);
//	    
//	    add drugBank Lexicon
	    dictFileName = Messages.getString("jiaoduan.drugbank.lexicon");
	    AppendDrugListFromTSVFile(dictFileName);
	    
//	    add medical terms
	    getMedicalTermList();
	    
//	    get linux words
	    GetCommonEnglishWords();
	    
	    
	    
//	    for(String pName: patterns.keySet()){
////	    String pName = "prn";
//	    	System.out.println(pName);
//	    	System.out.println(patterns.get(pName));
//	    }
//	    load patterns for dx Section recognition

		HashMap<String,LinkedList<String>> dcSumSections = (HashMap<String,LinkedList<String>>)data.get("dcSumSections");


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

			dxSectionPatterns.put(sectionName, pStr);
		}
	    
	    
//	    System.out.println();

	}

	public static HashMap<String, Integer> GetCommonEnglishWords() {
		// TODO Auto-generated method stub
		if(linuxWords.size()>0)
			return linuxWords;
		String linuxWordfile = Messages.getString("JiaoDuan.linux.words.file");
		RawInput rin = new RawInput();
		
		BufferedReader fFile = null;
		//			fFile = new BufferedReader(new FileReader(linuxWordfile));
		String[] lines = RawInput.getTxtResourceContent(linuxWordfile).split("\n");
		//				while ((line = fFile.readLine()) != null) {
		for (String line: lines){
			line = line.trim();
			if(line.length() > 1)
				linuxWords.put(line, 1);
		}
		
		System.out.println();
		return linuxWords;
	}
	private void AppendDrugListFromTSVFile(String dictFileName) throws IOException {
		// TODO Auto-generated method stub
		String mainField = "drugName";
		HashMap <String, ArrayList<HashMap<String, String>>> extendDruglist = getList_hashBased_fromTSV(dictFileName, mainField);
		
//		delete stop word
		for(int i=0; i< drugnameStoplist.size(); i++)
		{
			String name = drugnameStoplist.get(i);
//			if(extendDruglist.containsKey(name))
//			{
//				extendDruglist.remove(name);
//			}
			for(String fwd: extendDruglist.keySet()){
				ArrayList<HashMap<String, String>> drugs = new ArrayList<HashMap<String, String>>();
				for(HashMap<String, String> drug: extendDruglist.get(fwd)){
					if(!drug.get("drugName").equals(name)){
						drugs.add(drug);
					}
				}
				extendDruglist.put(fwd, drugs);
			}
		}
		for(String fstWord: extendDruglist.keySet()){
			if(druglist.containsKey(fstWord)){
				ArrayList<HashMap<String, String>> drugs = druglist.get(fstWord);
				ArrayList<HashMap<String, String>> extendDrugs = extendDruglist.get(fstWord);
				for(HashMap<String, String> drug: drugs){
					String dName = drug.get(mainField);
					for(int i = 0; i < extendDrugs.size(); i++){
						String exName = extendDrugs.get(i).get(mainField);
						if(exName.equals(dName)){
							extendDrugs.remove(i);
							break;
						}		
					}
				}
				for(HashMap<String, String> extendDrug: extendDrugs){
					drugs.add(extendDrug);
				}
				druglist.put(fstWord, drugs);
			}else{
				druglist.put(fstWord, extendDruglist.get(fstWord));
			}
		}
		
	}
	
	public HashMap<String, ArrayList<HashMap<String,String>>> GetMediccalTermDictionary(){
		return this.medicalTermlist;
	}
	
	private void getMedicalTermList() {
		// TODO Auto-generated method stub
//		String fname = RESOURCE_DIR + "medterm.tsv";
		String fname = Messages.getString("i2b2.medical.term.tsv.file");
		
		try {
			String mainField = "termName";
			medicalTermlist = getList_hashBased_fromTSV(fname, mainField);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private void makeNonTerminalPatterns() {
		// TODO Auto-generated method stub
//	    my $self = shift;
//	    $self->{DEBUG} = 1;


		DEBUG  = 1;
		
		for (int i=0; i<nonTerminals.size(); i++)
		{
			Map<String, List> nt = (HashMap<String, List>)nonTerminals.get(i);
			List<String> patterns = nt.get("patterns");
	
//			get pattern name
			Map<String, String> nt2 = (HashMap<String, String>)nonTerminals.get(i);
			String name = nt2.get("name") ;
			
			ArrayList<String> expandedPatterns = new ArrayList<String>();
			for(String pattern: patterns)
			{
//				# any alpha chars we find in a pattern should be part of the name of another pattern, EXCEPT
//				#	chars following \ ! or =, which are parts of regexes.  get rid of these temporarily
//				#	and for every other alpha string, replace it with the pattern it refers to
				String regExp_ExeptChar = "((\\\\|=|!)[a-zA-Z])";
				
				Pattern repart = Pattern.compile(regExp_ExeptChar);//((=|!|\\|)[a-zA-Z])
				Matcher matcher = repart.matcher(pattern);
				
				ArrayList<String> escs = new ArrayList<String>();
//				if(matcher.find())
//				{
//					System.out.println("Special Pattern:" +pattern);
//				}
//				else
//				{
//					System.out.println("No Special Pattern:" + pattern);
//				}
//				System.out.println(pattern + " " + repart.getPattern());
				while(matcher.find())
				{
					pattern = pattern.replaceFirst(regExp_ExeptChar, "====");
					escs.add(matcher.group(1));
//					System.out.println(matcher.group(1));
				}
//				System.out.println(pattern);
				
				DEBUG = 0;
				Pattern pPatternTerm = Pattern.compile("([a-zA-Z]+)");				
				
				if(DEBUG > 0)
				{
					int count =0;
					matcher = pPatternTerm.matcher(pattern);
//					ptnMatInput = new PatternMatcherInput(pattern);
					while(matcher.find())
					{
						count ++;
//						MatchResult rlt = matcher.getMatch();
//						System.out.println("dd:" + matcher.group(1));
						if(!this.patterns.containsKey(matcher.group(1)))
						{
							System.err.println("Fatal Erro: Could not find pattern for term " + matcher.group(1));
						}
					}
				}
				
				int count =0;
				
				matcher = pPatternTerm.matcher(pattern);
				while(matcher.find())
				{
					count ++;
//					System.out.println("----------------\t" + count);
//					MatchResult rlt = matcher.getMatch();
					String terPatName = matcher.group(1);
					if(!this.patterns.containsKey(terPatName) || this.patterns.get(terPatName) == null){
						System.out.println(terPatName + " in " + pattern);
						System.out.println(this.patterns.get(terPatName));
						
				}
					
					String rStr = "((?=\\s*)" + this.patterns.get(matcher.group(1)) + "(?=\\s*))";
					String term = matcher.group(1);
//					pattern = pattern.replace(term, rStr);
//					rStr = rStr.replace("\\$", "\\\\$");
					pattern = pattern.replace(term, rStr);
//					System.out.println(pattern);
				}
				
				Pattern pRecover = Pattern.compile("====");
	            if( escs.size() > 0 ) {
	            	// \b just doesn't work right for this app
//	            	System.out.println("Contained Special Pattern: " + escs);
	            	if(escs.get(0).equals("\\b")){
//					escs.set(0, this.splitDelim);
//	            	escs.set(0, "");
	            	}
//	            	ptnMatInput = new PatternMatcherInput(pattern);
	            	matcher = pRecover.matcher(pattern);
	            	count = 0;
	            	while(matcher.find())
	            	{
	            		pattern = pattern.replaceFirst("====", "\\" + escs.get(0));
						count ++;
//						System.out.println("----------------\t" + count);
	            		escs.remove(0);
	            	}
//	            	System.out.println(pattern);
	            }
	            expandedPatterns.add(pattern);				
			}
	    	String joinp = new String();
	    	int pNumber = expandedPatterns.size();
	    	for(int j=0; j< pNumber; j++){
	    		if(j< pNumber-1){
	    			joinp += expandedPatterns.get(j) + "|";
	    		}else{
	    			joinp += expandedPatterns.get(j);
	    		}
	    	}
//			System.out.println("MakeNonTerminal: " + joinp + "\n");
	    	String combinedPatterns = "(" + joinp + ")";
	    	this.patterns.put(name, combinedPatterns);			
		}
//		System.out.println("Patterns size is " + this.patterns.size());
//		System.out.println("MakeNonTerminal Pattern End\n");
//		System.out.println("reason");
//		System.out.println(patterns.get("reason"));
//		String pName = "freq";
//		System.out.println(pName);
//		System.out.println(patterns.get(pName));
	}

	private void makeTerminalPatterns() {
		// TODO Auto-generated method stub

		String sd = splitDelim;
	    for(String terminalName: terminals.keySet())
	    {
	    	List<String>terminalPatterns = (LinkedList<String>)terminals.get(terminalName);
	    	String joinp = new String();
	    	int pNumber = terminalPatterns.size();
	    	for(int i=0; i< pNumber; i++){
	    		if(i< pNumber-1){
	    			joinp += terminalPatterns.get(i) + "|";
	    		}else{
	    			joinp += terminalPatterns.get(i);
	    		}
	    	}
	    	String pattern = sd + "(" + joinp + ")" + sd;
//	    	System.out.println("MakeTerminalPatterns: "+ terminalName +"\t\t\t\t\t"+ pattern);
	    	this.patterns.put(terminalName, pattern);	
	    }
//	    System.out.println("Patterns size is " + this.patterns.size());
//	    System.out.println("MakeTerminalPatterns end\n");		
	}

	@SuppressWarnings("unchecked")
	private void applyConvenienceRules()  {
		// TODO Auto-generated method stub
//	    my $self = shift;
//	    my $terminals = $self->{terminals};
//	    my $convenienceRules = $self->{convenienceRules};
		
		Pattern dotsAfterEachLetter = Pattern.compile("(.)");
		
//		try{
//			dotsAfterEachLetter = compiler.compile("(.)");
//		}catch (MalformedPatternException e){
//			e.printStackTrace();
//		}
		
		Map<String, ArrayList<String>> expansions = new HashMap<String, ArrayList<String>>();
		for(String ruleName:convenienceRules.keySet())
		{
			List<String> thingsToExpand = (LinkedList<String>)convenienceRules.get(ruleName);
			for(String original : thingsToExpand)
			{
				String expanded = new String();
				if( ruleName.equals("dotsAfterLtrOk")){
//					q pm variant q p.m.,
//					String[] parts = original.split("\\s+");
					
					Matcher matcher =dotsAfterEachLetter.matcher(original);
					expanded = original;
					while(matcher.find())
					{
						expanded = expanded.replaceFirst(matcher.group(1), matcher.group(1)+"\\\\.");
					}
//					expanded = original.repla~ s/(.)/$1\\./g;
				}else if( ruleName.equals("dotsAtEndOk")) {
					expanded = original.replaceAll("$", "\\\\.");//influence matchpattern
//					System.out.println(expanded);
				}else if( ruleName.equals("canBePlural") ) {
					expanded = original.replaceAll("$", "s");
//					expanded =~ s/$/s/;
				}else if( ruleName.equals("plurDotAtEnd")) {
					expanded = original.replaceAll("$", "s\\\\.");
//					expanded =~ s/$/s\\./;
				}else if( ruleName.equals("pastState")){
					Pattern pEnd = Pattern.compile("(e$)");
					Matcher m = pEnd.matcher(original);
					if(m.find())
						expanded = original.replaceAll(m.group(1), "ed");
					else
						expanded = original.replaceAll("$", "ed");					
				}else{
					System.err.println("don't know how to handle convenience rule "+original + "xxxx"+ ruleName);
				}
//				System.out.println("Apply Convenient Rules :" + ruleName + "\t" + original + "---" + expanded );
				
//				try {
//					Pattern p= Compiler.compile(expanded);
//				} catch (MalformedPatternException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				if(expansions.containsKey(original))
				{
					expansions.get(original).add(expanded);
				}
				else
				{
					ArrayList<String> expList = new ArrayList<String>();
					expList.add(expanded);
					expansions.put(original, expList);
				}
			}
		}

//		add expanded pattern to terminals
		for (String terminalName : terminals.keySet())
		{
			LinkedList<String> terminal = terminals.get(terminalName);
			ArrayList<String> newExpressions = new ArrayList<String>();
//			get the new expression for each one
			for(String expression : terminal)
			{
				if(expansions.containsKey(expression))
				{
					for(String expand: expansions.get(expression))
					{
//						terminals.get(terminalName).add(expand);
						newExpressions.add(expand);
					}
				}
			}
			
//			add new expression to the list
			for(String expand: newExpressions)
			{
				terminals.get(terminalName).add(expand);
				Collections.sort(terminals.get(terminalName), Collections.reverseOrder());
//				System.out.println(terminals.get(terminalName));
			}
		}
		
//		System.out.println("Apply Convenient Rules end\n");
	}

	private void getDruglist(String drugDictionary) {
		// TODO Auto-generated method stub
		
		if(drugSearchMethod.equals("treeBased"))
		{
			getDruglist_treeBased();
		}else if(drugSearchMethod.equals("hashBased"))
		{
			try {
				getDruglist_hashBased(drugDictionary);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if(drugSearchMethod.equals("binarySearchBased"))
		{
			getDruglist_binarySearchBased();
		}
	}

	private void getDruglist_binarySearchBased() {
		// TODO Auto-generated method stub
		
	}

	private void getDruglist_hashBased(String drugDictFileName) throws IOException {
		// TODO Auto-generated method stub
//		String fname = RESOURCE_DIR + "druglist.tsv";
		
		
		String mainField = "drugName";
		druglist = getList_hashBased_fromTSV(drugDictFileName, mainField);
		
//		delete stop word
		FilterDruglistWithStopWordList();
	}

	private void FilterDruglistWithStopWordList() {
		// TODO Auto-generated method stub
		String sd = "\\b";
		for(int i=0; i< drugnameStoplist.size(); i++)
		{
			String stopName = drugnameStoplist.get(i);
			String[] words = stopName.split(sd);
			String firstWord = words[1];
			
			if(druglist.containsKey(firstWord)){
				for(String fswd: druglist.keySet()){
					ArrayList<HashMap<String,String>> drugs = druglist.get(fswd);
					for(int j = 0; j< drugs.size(); j++){
						String dName = drugs.get(j).get("drugName");
						if(dName.equals(stopName))
							drugs.remove(j);
					}
					
					druglist.put(fswd, drugs);				
				}
				
			}
		}
		
	}
	private HashMap<String, ArrayList<HashMap<String, String>>> getList_hashBased_fromTSV(
			String fname, String indexField) throws IOException {
		HashMap<String, ArrayList<HashMap<String, String>>> termlist = new HashMap<String, ArrayList<HashMap<String, String>>>();
		
		BufferedReader in;
		System.out.println(fname);
		String[] lines = RawInput.getTxtResourceContent(fname).split("\n");
//			in = new BufferedReader(new FileReader(fname));
		
//			String firstLine =  in.readLine();
		String firstLine = lines[0];
		String[] fieldNames = firstLine.split("\\t");
		
//			String sd = splitDelim;
		String sd = "\\b";
		
//			while ((line = in.readLine()) != null) {
		for (int j = 1; j < lines.length; j++){
//				#s/\s*\(obs.*$//;    # throw away "(obsolete)" label on drug names (sometimes cut off)
			String line = lines[j];
			if(!line.contains("\t"))
				continue;
			
			line = line.toLowerCase();
//				if(line.contains("Potassium"))
//					System.out.println(line);
			
			String[] fields = line.split("\\t");
			HashMap<String,String> drug = new HashMap<String, String>();
			for(int i=0; i< fieldNames.length; i++)
			{
				drug.put(fieldNames[i], fields[i].toLowerCase());
			}
			
			
//				String[] words = drug.get("drugName").split(sd);
			
//				System.out.println(drug.get(indexField));
			String[] words = drug.get(indexField).split(sd);
			String firstWord = words[1];
			
			if(termlist.containsKey(firstWord))
			{
				termlist.get(firstWord).add(drug);
			}
			else
			{	
				ArrayList<HashMap<String,String>> tmp = new ArrayList<HashMap<String,String>>();
				tmp.add(drug);					
				termlist.put(firstWord, tmp);
			}
//				System.out.println(""firstWord + drug);
		}
		return termlist;
	}
	private void getDruglist_treeBased() {
		// TODO Auto-generated method stub
		
	}
//	public ArrayList<String> drugsToi2b2ByCRF() throws Exception {
//		// TODO Auto-generated method stub
//		return lancet.drugsToi2b2();
//	}
	
	public void Reset() throws IOException {
		// TODO Auto-generated method stub
		
		lm = null;
		
		initializeParser();
	}
	public void SetMatchedFields(List<HashMap<String, String>> drugs) {
		// TODO Auto-generated method stub
//		iMedStartLine = Integer.parseInt(medication.get("StartLine"));
//		iMedStartToken = Integer.parseInt(medication.get("StartToken"));
//		iMedEndLine = Integer.parseInt(medication.get("EndLine"));
//		iMedEndToken = Integer.parseInt(medication.get("EndToken"));
		
		for(HashMap<String,String> m: drugs){
			if(!m.containsKey("drugNameStart"))
				continue;
			if(!m.containsKey("drugNameEnd"))
				continue;
			
			int bgnIndex = Integer.parseInt(m.get("drugNameStart"));
			int endIndex = Integer.parseInt(m.get("drugNameEnd"));
			//		collect drug field information
			
			HashMap<String, Integer> medToken = new HashMap<String, Integer>();
			HashMap<String, String> medication = new HashMap<String, String>();
			
			String type = "M";
			medication.put("type", type);
			medication.put(type, m.get("drugName"));
			medication.put(type+"offset", lm.getTokenOffset(bgnIndex, endIndex));
			medToken = lm.convertArticleRegion2TokenPosition(bgnIndex, endIndex);
			
			medication.put("StartLine", Integer.toString(medToken.get("StartLine")));
			medication.put("StartToken", Integer.toString(medToken.get("StartTokenPosition")));
			medication.put("EndLine", Integer.toString(medToken.get("EndLine")));
			medication.put("EndToken", Integer.toString(medToken.get("EndTokenPosition")));
			
			fieldMatched.add(medication);
		}
		
		String[] parts = new String[]{"dose","route","freq","howLong","reason"};
		String[] brief = new String[]{"do","mo","f","du","r"};
		for(int i =0 ; i < parts.length; i++){
			String strContent = lm.GetContent();
			String strPattern = patterns.get(parts[i]);
			Pattern pField = Pattern.compile(strPattern, Pattern.CASE_INSENSITIVE);
			Matcher mField = pField.matcher(strContent);
			while(mField.find()){
				int start = mField.start();
				int end = mField.end();
				
				HashMap<String, String> field = new HashMap<String,String>();
				String type = brief[i];
				field.put("type", type);
				field.put(type, mField.group(1));
				field.put(type+"offset", lm.getTokenOffset(start, end));
				
				HashMap<String, Integer> fieldToken = new HashMap<String, Integer>();
				
				fieldToken = lm.convertArticleRegion2TokenPosition(start, end);
//				HashMap<String, String> field = new HashMap<String, String>();
				field.put("StartLine", Integer.toString(fieldToken.get("StartLine")));
				field.put("StartToken", Integer.toString(fieldToken.get("StartTokenPosition")));
				field.put("EndLine", Integer.toString(fieldToken.get("EndLine")));
				field.put("EndToken", Integer.toString(fieldToken.get("EndTokenPosition")));
		
				fieldMatched.add(field);
			}
		}
		
//		for(HashMap<String,String> field: fieldMatched){
//			String type = field.get("type");
//			System.out.println(type + "\t" + field.get(type) + "\t" + field.get(type + "offset") + "\t" +field.get("EndToken"));
//		}
		
	}
	public String i2b2EvaluateMetics(
			ArrayList<String> sysListedMeds, String trainDataFile,
			String evType) {
		String evRlt = "";
		File ftrain = new File(trainDataFile);
		String filename = ftrain.getName();
		String gFolder = Messages.getString("i2b2.Gold.Entries.folder");

		RawInput rinput = new RawInput();
		//		String goldStandardFile = trainDataFile.replace("trainingdata", "training.ground.truth");
		String goldStandardFile = gFolder + "2/" + filename;
		goldStandardFile += ".i2b2.entries";

		if(!rinput.IsFileExist(goldStandardFile)){
			System.err.println("There is no gold standard file");
			evRlt = "There is no gold standard file";
			
			return evRlt;
		}


		ArrayList<String> goldListedMeds = rinput.getListByEachLine(goldStandardFile, false);

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
			
			String[] fields_g = goldlm.split(ListedMedication.lmSeparator_ex);
			String sys_lm = new String();


			S = lm.GetNonNMnumber(goldlm, entries_g);

			String m_ex = "^" + rin.normalizeWord(fields_g[0]);

			Pattern pMentry = Pattern.compile(m_ex);			

			int sysIndex = -1;
			for(int j =0; j < sysListedMeds.size(); j++)
			{
				String lm = sysListedMeds.get(j);
				lm = lm.toLowerCase();
				Matcher mm = pMentry.matcher(lm);
//				if(mm.find())
				if(lm.contains(fields_g[0]))
				{
					N = lm.GetNonNMnumber(lm, entries_sys);
					//					System.out.println(m_ex);
					sys_lm = lm;

					sysIndex = j;
					break;
				}
			}

			String[] fields_sys = goldlm.split(ListedMedication.lmSeparator_ex);
			//compute
			double precision = 0;
			double recall = 0;
			if(N > 0)
			{
				//				removed matched list medication
				sysListedMeds.remove(sysIndex);

				D = lm.GetExactMatchNumber(entries_g, entries_sys);

				precision = D/N;
				recall = D/S;
				if(!(recall == 1 && precision == 1)){
					System.out.println(lm.GetMarkedRegionByListedMedication(sys_lm));

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
			System.out.println(lm.GetMarkedRegionByListedMedication(lm));
			
			String drugName = lm.getFieldValue("m", lm);
			HashMap<String, String> drug = drugLookup(drugName);
			if(drug == null)
				System.out.println("There is no " + drugName + " in drug dicitionary");
			else{
				System.out.println(drugName + " is existing in the dictionary");
				System.out.println(drug.get("cui"));
			}
			System.out.println();
			
			
			System.out.println();
		}

		System.out.println("\nFalse negtive record!");
		for(String fn: FalseNegative){
			System.out.println(fn);
			System.out.print("FN Record---");
			System.out.println(lm.GetMarkedRegionByListedMedication(fn));
			
			String drugName = lm.getFieldValue("m", fn);
			HashMap<String, String> drug = drugLookup(drugName);
			if(drug == null)
				System.out.println("There is no " + drugName + " in drug dicitionary");
			else{
				System.out.println(drugName + " is existing in the dictionary");
				System.out.println(drug.get("cui"));
			}
			System.out.println();
		}

		System.out.println("Micro-average Recall is:" + pSum/PrecisionList.size() + "; Precision is" + rSum/RecallList.size());
		System.out.println("--------------------------------------------------------------------------------------------------------------------------------------------");
		evRlt = "Micro-average Recall is:" + pSum/PrecisionList.size() + "; Precision is" + rSum/RecallList.size();
		return evRlt;
	}
//	public void SetSentenceParser(StanfordParserWrapper wrapper) {
//		// TODO Auto-generated method stub
//		stanfordParser = wrapper;
//	}
//	this function is moved to lancet
//	public void exportTaggedTextToFile(ArrayList<String> listedMedsJmerki, String filePath) throws IOException {
//		String taggedArticle = lancet.getFieldTaggedText(listedMedsJmerki);
//		
//		BufferedWriter out = new BufferedWriter(new FileWriter(filePath));
//		out.write("<html><head>");
//		out.write("<link rel='stylesheet' type='text/css' href='i2b2.css' /><body><pre>");
//		out.write(taggedArticle);
//		out.write("</body></html>");
//		out.close();
//	}
	/**
	 * @param originWord
	 * @return
	 */
	public boolean isWithinLinuxWords(String originWord) {
			if(originWord != null )
				return linuxWords.containsKey(originWord.toLowerCase());
			else
				return false;
	}



}
