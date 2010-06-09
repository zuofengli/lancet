package edu.uwm.jiaoduan.i2b2.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import spiaotools.SentParDetector;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

import edu.uwm.jiaoduan.Messages;
import edu.uwm.jiaoduan.i2b2.classifier.I2B2Classifier;
import edu.uwm.jiaoduan.i2b2.knowtatorparser.KnowtatorXmlBuilder;
import edu.uwm.jiaoduan.tools.abner.Tagger;
import edu.uwm.jiaoduan.tools.negex.GenNegEx;

public class LancetParser extends ListedMedication {

	public LancetParser(String contentOrFilepath) throws Exception {
		super(contentOrFilepath);

		//		for model lower or uppercase would be useful to detect the type

		int iManner = Integer.parseInt(Messages.getString("JiaoDuan.i2b2.lancet.parser.manner.tolowercase"));

		String content = "";
		if(iManner > 0){
			content = super.GetContent();
//			System.out.println("Convert to lower case");
		}else{
			content = super.getOriginContent();
//			System.out.println("Keep original upper and lower case");
		}
		
		
		
		if(Messages.
				getString("JioaDuan.i2b2.lancet.parser.manner.bySentence").equals("1"))
		bTagArticleBySentence = true;
		if(!content.isEmpty())
			initialParser(content);
		System.out.println("Finish initial lancet parser");
	}


	private static final int I2B22009 = 2;
//	private static Tagger crfTagger  = new Tagger( new File(Messages.getString("i2b2.CRF.Lancet.Parser.CRFModel.FilePath")));
	private static Tagger crfTagger  = new Tagger( I2B22009);
	
	private String article;
	private ArrayList<HashMap<String, String>> taglist = null;
	private ArrayList<HashMap<String, String>> taglistBySentence = null;
	private static Classifier m_relentClassifier = null;
	private Instances m_OutputFormat;
	private boolean bTagArticleBySentence = false;
	private RawInput rin = new RawInput();
	private static Instances  training = null;
	private static Jsplitta jsp = null;
	private ArrayList<HashMap<String, String>> SentenceSpans = new ArrayList<HashMap<String, String>>();
	private ArrayList<HashMap<String, String>> mPhrases = new ArrayList<HashMap<String, String>>();
	private HashMap<String, Integer> LNClassifiers = new HashMap<String, Integer>();
	private I2B2Classifier NgramLNClassifier = null;
//	LNMalletClassifier malletDoclassifier;
	private boolean bParsMannerSingleLine = false;
	private ArrayList<String> tmpfiles = new ArrayList<String>();



	/**
	 * Create i2b2 entries with whole article as an input.
	 * @param manner: Parser Manner: integer;
	 * @param manner=0 whole article;
	 * @param manner=1 by sentence;
	 *	@throws Exception
	 */
	public ArrayList<String> drugsToi2b2() throws Exception {
		
		ArrayList<String> drugList = new ArrayList<String>();
		
		int manner = Integer.parseInt(Messages.
				getString("JioaDuan.i2b2.lancet.parser.manner.bySentence"));

		ArrayList<HashMap<String, String>> i2b2Tags =null;
		if(manner == 0){
			i2b2Tags = this.taglist;
			System.out.println("Parsing the whole document!");
		}else if(manner == 1){
			i2b2Tags = this.taglist;
			System.out.println("Parsing at sentence level");
		}
		
//		appendReasonBySingletonModel(i2b2Tags);
//		appendFieldsFromOtherModel("lancetSingleton147.model", "R", i2b2Tags);
//		appendFieldsFromOtherModel("Lancet147Half.crf", "M", i2b2Tags);
//		appendFieldsFromOtherModel("LancetDC147.model", "DO", i2b2Tags);
//		appendFieldsFromOtherModel("LancetDC147.model", "MO", i2b2Tags);
//		appendFieldsFromOtherModel("LancetDC147.model", "F", i2b2Tags);
//		appendFieldsFromOtherModel("LancetDC147.model", "DU", i2b2Tags);
//		i2b2Tags = filterDrugNameBySwissnifeModel(i2b2Tags);
		
		i2b2Tags = sortI2b2Tags(i2b2Tags);
		
		boolean bByMedication = false;
		if(bByMedication){
			for (int i = 0; i < i2b2Tags .size(); i++) {
				String type = i2b2Tags.get(i).get("type");
				if (type.equals("M")) {
					HashMap<String, String> medication = i2b2Tags.get(i);

					ArrayList<HashMap<String, String>> fields = getTagsMinusPlusTwoLineWindow(medication);
					for (HashMap<String, String> field : fields) {
						boolean isRelated = disambiguateFieldsAroundDrugName(medication, field);

						if (isRelated) {
							String fieldType = field.get("type");
							medication.put(fieldType, field.get(fieldType));
							medication.put(fieldType + "Offset", field
									.get(fieldType + "Offset"));
						}
					}

					String lm = compileListedMedication(medication);
					drugList.add(lm);
				}
			}
		}else{
			//by medication fields
			HashMap<String, ArrayList<ArrayList<HashMap<String, String>>>> drugInfo = new HashMap<String, ArrayList<ArrayList<HashMap<String,String>>>>();

			for (int i = 0; i < i2b2Tags .size(); i++) {
				String type = i2b2Tags.get(i).get("type");
				
				if (!type.equals("M")) {
					HashMap<String, String> field = i2b2Tags.get(i);					
					appendTopOneMedication( field, i2b2Tags);
				}
			}
			
			//arrange i2b2 medication
			for (int i = 0; i < i2b2Tags .size(); i++) {
				String type = i2b2Tags.get(i).get("type");
				if (type.equals("M")) {
					HashMap<String, String> medication = i2b2Tags.get(i);
					int size = 0;

					clearMedicationFields(medication);
					
					if(medication.containsKey("subMedNumber")){
//						System.out.println(medication.get("M"));
						size = Integer.parseInt(medication.get("subMedNumber"));
						String[] tags = {"DO", "MO", "F", "DU", "R"};
//						System.out.println(size);
//						begin from 1
						for(int j=1; j<= size; j++){
							for(String tag : tags){
								String key = tag + "-" + j;
								String offsetKey = tag + "Offset" + "-" + j;
								if(medication.containsKey(key)){
									medication.put(tag, medication.get(key));
									medication.put(tag + "Offset", medication.get(offsetKey));
								}
							}
							String lm = compileListedMedication(medication);
							drugList.add(lm);
							clearMedicationFields(medication);
						}
					}else{
						String lm = compileListedMedication(medication);
						drugList.add(lm);
					}
				}
			}
			
		}
		String negEx = Messages.getString("i2b2.filter.Negative.Medication.With.NegEx");
		if(negEx.equals("1"))
			drugList = filterListMedsWithNegExByLine(drugList);
		
		return drugList;
	}

	/**
	 * @param tags
	 * @return 
	 */
	private ArrayList<HashMap<String, String>> sortI2b2Tags(ArrayList<HashMap<String, String>> tags) {
		HashMap<Integer, HashMap<String,String>> posIndexedTags = new HashMap<Integer, HashMap<String,String>>();
		for(HashMap<String,String> tag: tags){
			String type = tag.get("type");
			ArrayList<HashMap<String, Integer>> offsets = this.parseOffset(tag.get(type + "Offset"));
			HashMap<String, Integer> offset = offsets.get(0);
			int imLineStart = offset.get("StartLine");
			int imTokenStart = offset.get("StartToken");
			int position = this.GetArticlePositionByStartToken(imLineStart, imTokenStart);
			posIndexedTags.put(position, tag);
		}
		
		ArrayList<Integer> sortedPos = new ArrayList<Integer>();
		for(int pos: posIndexedTags.keySet()){
			sortedPos.add(pos);
		}
		Collections.sort(sortedPos);
		ArrayList<HashMap<String, String>> sortedTags = new ArrayList<HashMap<String, String>>();
		for(int pos: sortedPos){
			sortedTags.add(posIndexedTags.get(pos));
		}	
		
		return sortedTags;
	}

	/**
	 * @param medication
	 */
	private void clearMedicationFields(HashMap<String, String> medication) {
		String[] tags = {"DO", "MO", "F", "DU", "R"};
		for(String tag : tags){
			if(medication.containsKey(tag))
				medication.remove(tag);
		}
		
	}

	/**
	 * @param drugInfo 
	 * @definition this function collect drug names in the +/-2 lines around the 
	 * 				medication field, decide the top one drug name and assign the field to
	 * 				the drug name; When there are multiple entries for the same filed type, 
	 * 				the medication would ....
	 * @param field
	 * @param tags
	 */
	private void appendTopOneMedication( HashMap<String, String> field,
			ArrayList<HashMap<String, String>> tags) {
		
		
		double minRelativeValue = 1;
		int minRelativeID = -1;
		for (int i = 0; i < tags .size(); i++) {
			String type = tags.get(i).get("type");
			if (type.equals("M")) {
				
				if(isInMniusPlusTwoLineWindow(field, tags.get(i))){
					
					double[] distpro = null;
					distpro = getMedicationFiledRelationshipValue(tags.get(i), field);
//					System.out.println(tags.get(i).get("M") + distpro[0] + "" + distpro[1]);
					if(distpro[0] < minRelativeValue){
						minRelativeValue = distpro[0];
						minRelativeID = i;
					}
				}
			}
		}
		
		if (minRelativeID >= 0) {
			String fieldType = field.get("type");
			tags.get(minRelativeID).put(fieldType, field.get(fieldType));
//			String fieldType = fieldType + "Offset";
			if(tags.get(minRelativeID).containsKey("subMedNumber")){
//				System.err.println("This field have been existing. " + tags.get(maxRelativeID).get("M") + " " +field.get(fieldType) + tags.get(maxRelativeID).get(sKey));
				int subMedNumber = Integer.parseInt(tags.get(minRelativeID).get("subMedNumber"));
				
				String oldKey = fieldType + "-" + subMedNumber;
				
				if(tags.get(minRelativeID).containsKey(oldKey)){
					subMedNumber ++;
					tags.get(minRelativeID).put("subMedNumber", Integer.toString(subMedNumber));
				}
				
				
				String subIndex = Integer.toString(subMedNumber);
				
				tags.get(minRelativeID).put(fieldType + "Offset" +"-" +subIndex, field
						.get(fieldType + "Offset"));
				tags.get(minRelativeID).put(fieldType + "-" +subIndex, field
						.get(fieldType));
				
			}else{
				tags.get(minRelativeID).put("subMedNumber", "1");
				
				tags.get(minRelativeID).put(fieldType + "-" + "1", field
					.get(fieldType));
				tags.get(minRelativeID).put(fieldType + "Offset" + "-" + "1", field
						.get(fieldType + "Offset"));
			}
		}
		
		
	}

	/**
	 * @param field
	 * @param hashMap
	 * @return
	 */
	private boolean isInMniusPlusTwoLineWindow(HashMap<String, String> aField,
			HashMap<String, String> bField) {
	 	
		Pattern pOffset = Pattern.compile("(\\d+):(\\d+)\\s+(\\d+):(\\d+)");
		
		String aType = aField.get("type");
		String bType = bField.get("type");
		

		Matcher am = pOffset.matcher(aField.get(aType +"Offset"));
		
		
		int aStartLine = 0;
		int aEndLine = 0;
		if(am.find()){
			aStartLine = Integer.parseInt(am.group(1));
			aEndLine = Integer.parseInt(am.group(1));
		}else
			System.out.println("LancetParser: Error in offset matching. 201001040443pm");
		
		Matcher bm = pOffset.matcher(bField.get(bType + "Offset"));
		int bStartLine = 0;
		int bEndLine = 0;
		if(bm.find()){
			bStartLine = Integer.parseInt(bm.group(1));
			bEndLine = Integer.parseInt(bm.group(1));
		}else
			System.out.println("LancetParser: Error in offset matching. 201001040444pm");
		
		boolean bSameWindow = false;
		int iDif =-1;
		if(aStartLine == bStartLine)
			iDif = 0;
		else if (aStartLine > bStartLine){
			iDif = aEndLine - bStartLine;
		}else if(aStartLine < bStartLine)
			iDif = bEndLine - aStartLine;
		if(iDif >=0 && iDif <=2)
			bSameWindow = true;
		
		return bSameWindow;
	}

	/**
	 * @param medication
	 * @return
	 */
	private String compileListedMedication(HashMap<String, String> medication) {
		String FieldSeparator = Messages.getString("i2b2.field.separator");
		
		try {
			setListNarrativeForMedication(medication);
		} catch (Exception e) {
			e.printStackTrace();
		}

		String[] tags = Messages.getString("i2b2.CRF.TAGs")
		.toUpperCase().split(",");
		String lm = "";
		for (String entry : tags) {

			if (entry.toLowerCase().equals("ln")) {
				lm += entry.toLowerCase() + "=\""
				+ medication.get(entry) + "\"";
			} else {						
				if (medication.get(entry) != null){
					String offset = medication.get(entry + "Offset");
//					System.out.print(offset + "\t");
					if(bParsMannerSingleLine)
						offset = convert2i2b2Offset(offset);
//					System.out.println(offset);	
					lm += entry.toLowerCase() + "=\""
					+ medication.get(entry) + "\" "
					+ offset;
				}else
					lm += entry.toLowerCase() + "=\"nm\"";

				lm += FieldSeparator;
			}

		}
		return lm;
	}

	/**
	 * @param tags
	 * @return 
	 */
	private ArrayList<HashMap<String, String>> filterDrugNameBySwissnifeModel(
			ArrayList<HashMap<String, String>> tags) {
		ArrayList<HashMap<String,String>> filteredTags = new ArrayList<HashMap<String,String>>();
		
		String swKnifeModel = "./CRFmodel/afterFixBug/SwissKnife147.crf";
		File file = new File(swKnifeModel);
		File fmodel = null;
		try {
			fmodel = new File(file.getCanonicalPath());
		} catch (IOException e) {
			System.out.println("could not find sigleton model file");
			e.printStackTrace();
		}
		Tagger t = new Tagger(fmodel);
		t.setTokenization(true, "i2b2");
		ArrayList<HashMap<String, String>> tsgs = t.tagI2B2(article);
		
		ArrayList<Integer> negative = new ArrayList<Integer>();
		
		//all system drugs
		for(int i =0; i<tags.size(); i++){

		boolean  bIsNegative = false;
		for(HashMap<String,String> tag: tsgs){
//			System.out.println( "negative drug" +tag.get("NEGM"));
			if(tag.get("type").equals("NEGM")){
//				System.out.println("find a negative drug name");
				
				if(tag.get("NEGMOffset").equals(tags.get(i).get("MOffset"))){
					bIsNegative = true;
				}
				
				
				continue;
			}
			
			
		}	
			if(!bIsNegative)
				filteredTags.add(tags.get(i));

			
		}
		
//		for(int  i = 0; i < negative.size(); i++){
//			System.out.println("find a negatvie drug name");
//			int index = negative.get(i);
//			tags.remove(index);
//		}
		System.out.println();
		return filteredTags;
	}

	/**
	 * 
	 */
	private void appendFieldsFromOtherModel(String modelName, String tagName,
			ArrayList<HashMap<String, String>> tags) {
		String reasonSigletonModel = "./CRFmodel/afterFixBug/" + modelName;
		File file = new File(reasonSigletonModel);
		File fmodel = null;
		try {
			fmodel = new File(file.getCanonicalPath());
		} catch (IOException e) {
			System.out.println("could not find sigleton model file");
			e.printStackTrace();
		}
		Tagger t = new Tagger(fmodel);
		t.setTokenization(true, "i2b2");
		ArrayList<HashMap<String, String>> tsgs = t.tagI2B2(article);
		for (HashMap<String,String> tag: tsgs){
			if(tag.get("type").equals(tagName)){
//				System.out.println("Find a " + tagName);
				boolean bExisting = false;
				for(HashMap<String,String> exTag: tags){
					if(exTag.get("type").equals(tagName)){
						if(exTag.get(tagName + "Offset").equals(tag.get(tagName + "Offset"))){
							bExisting = true;
//							System.out.println("this " + tagName + " is existing.");
							break;
						}
							
					}
				}
				if(!bExisting)
					tags.add(tag);
			}
				
		}

		
	}

	/**
	 * @param tags
	 */
	private void appendReasonBySingletonModel(
			ArrayList<HashMap<String, String>> tags) {
		String reasonSigletonModel = "./CRFmodel/afterFixBug/lancetSingleton147.model";
		File file = new File(reasonSigletonModel);
		File fmodel = null;
		try {
			fmodel = new File(file.getCanonicalPath());
		} catch (IOException e) {
			System.out.println("could not find sigleton model file");
			e.printStackTrace();
		}
		Tagger t = new Tagger(fmodel);
		t.setTokenization(true, "i2b2");
		ArrayList<HashMap<String, String>> tsgs = t.tagI2B2(article);
		for (HashMap<String,String> tag: tsgs){
			if(tag.get("type").equals("R")){
				System.out.println("Find a reason");
				tags.add(tag);
			}
				
		}

		
	}

	public void setListNarrativeForMedication(
			HashMap<String, String> medication) throws Exception {
		String offset = medication.get("MOffset");
		Pattern pOffset = Pattern.compile("(\\d+):(\\d+)\\s+(\\d+):(\\d+)");

		Matcher mm = pOffset.matcher(medication.get("MOffset"));
		int imLineStart = 0;
		int imTokenStart = 0;
		if (mm.find()) {
			imLineStart = Integer.parseInt(mm.group(1));
			imTokenStart = Integer.parseInt(mm.group(2));
		} else {
			System.err.println("Error in lancet parser 0333pm");
		}

		int start = super.GetArticlePositionByStartToken(imLineStart,
				imTokenStart);
		String lnrlt = this.getListNarrative(start);

		medication.put("LN", lnrlt);
	}
	/*
	 * Get +/-2 lines of the drug name;
	 * 
	 */
	public  ArrayList<HashMap<String, String>> getTagsMinusPlusTwoLineWindow(
			HashMap<String, String> medication) {
		ArrayList<HashMap<String, String>> fieldList = new ArrayList<HashMap<String, String>>();
		Pattern pOffset = Pattern.compile("(\\d+):(\\d+)\\s+(\\d+):(\\d+)");

		Matcher mm = pOffset.matcher(medication.get("MOffset"));
		int imLineStart = 0;
		int imLineEnd = 0;
		if (mm.find()) {
			imLineStart = Integer.parseInt(mm.group(1));
			imLineEnd = Integer.parseInt(mm.group(3));
		} else {
			System.err.println("Error in lancet parser 0506pm");
		}

		for (HashMap<String, String> tag : taglist) {
			String type = tag.get("type");
			if (type.equals("M"))
				continue;

			Matcher fm = pOffset.matcher(tag.get(type + "Offset"));
			if (fm.find()) {
				int ifLineStart = Integer.parseInt(fm.group(1));
				int ifLineEnd = Integer.parseInt(fm.group(3));

				if ((ifLineEnd < (imLineEnd + 3) && ifLineEnd >= imLineEnd || (ifLineStart <= imLineStart && ifLineStart > (imLineStart - 3)))) {
					fieldList.add(tag);
				}

			} else {
				System.err.println("Error in lancet parser 0506pm");
			}

		}

		return fieldList;
	}

	public ArrayList<String> GetDrugList() {
		// TODO Auto-generated method stub
		String[] medications = crfTagger.getEntities(article, "M");
		ArrayList<String> medlist = new ArrayList<String>();
		HashMap<String, Integer> meds = new HashMap<String, Integer>();
		// ((.*)+)(\s*)?\((.*)(\s*)?(\(\s*(\w+)\s*\))?\)
		// Pattern pcm =
		// Pattern.compile("((.*)+)(\\s*)?\\((.*)(\\s*)?(\\((.*)\\))?\\)");
		for (String m : medications) {

			// Matcher cmm = pcm.matcher(m);
			String[] names = m.split("(\\(|\\))");
			String name = m;
			if (names.length > 1) {
				// m = this.NormalizeWord(m);
				// medlist.add(m.trim());

				for (int i = 0; i < 2; i++) {
					// medlist.add(names[i]);
					// System.out.println(names[i]);

					name = names[i];

					name = name.replaceAll("\\W$", " ");
					name = name.replaceAll("\\s+", " ");
					name = name.trim();
					if (!StringUtil.isStopWord(name)
							|| StringUtil.hasCaps(name))
						meds.put(name, 1);
					// medlist.add(name);
				}
				// System.out.println();

			} else {
				// String name = m;

				name = name.replaceAll("\\W$", " ");
				name = name.replaceAll("\\s+", " ");
				name = name.trim();

				meds.put(name, 1);
				// medlist.add(name);
				// System.err.println("Erro in lancetParser GetDrugList: pattern exception");
			}

		}
		for (String med : meds.keySet()) {
			medlist.add(med.toLowerCase());
			System.out.println("crf medication:<crfMed>" + med + "</crfMed>");
		}

		return medlist;
	}
	/*
	 * 
	 */
	public void initialParser(String s) {
		
		try {
			setSentenceSpans(true);
			InitialLNClassifier();
		} catch (Exception e) {
			e.printStackTrace();
		}

		article = s.replaceAll("\\|", "\\\\|");
		article = crfTagger.i2b2Tokenize(article);
		crfTagger.setTokenization(true, "i2b2");

		String strSingeLineConvert = Messages.
			getString("JiaoDuan.i2b2.lancet.parser.manner.tosingleline");
		
		if(strSingeLineConvert.equals("1")){
			bParsMannerSingleLine = true;
			taglist = crfTagger.tagI2B2(article.
					replaceAll("\n", " "));
		}else if(bTagArticleBySentence ){
			ArrayList<HashMap<String, String>> span = getSentenceSpan();
			taglist = crfTagger.tagI2B2BySentence(span, super.getContentWithoutEnter());
			for(int i =0; i< this.taglist.size(); i++){
				HashMap<String, String> tag = this.taglist.get(i);
				int start = Integer.parseInt(tag.get("StartPosition"));
				int end = Integer.parseInt(tag.get("EndPosition"));
				String type = tag.get("type");
				tag.put(type + "Offset", super.getTokenOffset(start, end));

				this.taglist.set(i, tag);
			}
		}else{
//			String tokenizedArticle = crfTagger.i2b2Tokenize(article);
			taglist = crfTagger.tagI2B2(article);
		}
		//		load medication field relationship model
		
		String modelName = Messages
		.getString("i2b2.Relate.Medication.Field.Model");
		
		URL rModel = LancetParser.class.getResource(modelName);
		InputStream file =null;
		try {
			file = rModel.openStream();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		System.out.println(file);
		loadTagRelationModel(file);
		try {
			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		


		
		

	}

	private void loadTagRelationModel(InputStream file) {
		// deserialize model

		try {
			m_relentClassifier = (Classifier)weka.core.SerializationHelper.read(file);
			
//			m_relentClassifier = (Classifier) weka.core.SerializationHelper
//			.read(file);
			System.out.println();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void SetTaglist(ArrayList<HashMap<String, String>> taglist2) {
		// TODO Auto-generated method stub
		taglist  = taglist2;
	}

	public int getMedicationNumberBetween(HashMap<String, String> medication,
			HashMap<String, String> field) {
		String medOffset = medication.get("MOffset");
		String fType = field.get("type");
		String fOffset = field.get(fType + "Offset");

		int medNumber = -1;
		for(HashMap<String, String> tag: taglist){
			String type = tag.get("type");
			String offset = tag.get(type + "Offset");

			if(medNumber < 0){
				if((type.equals("M") && offset.equals(medOffset)) ||
						(type.equals(fType) || offset.equals(fOffset))){
					medNumber++;
				}

			}else{
				if((type.equals("M") && offset.equals(medOffset)) ||
						(type.equals(fType) || offset.equals(fOffset))){
					break;
				}else if(type.equals("M")){
					medNumber++;
				}				
			}

		}
		return medNumber;
	}
	public HashMap<String, String> getMedicationFieldsMutalInformation(HashMap<String, String> ltmed) {
		String[] tags = Messages.getString("i2b2.CRF.TAGs").split(",");
		String dataset = "";
		HashMap<String, String> medication = new HashMap<String,String>();

		medication.put("type", "M");
		medication.put("M", ltmed.get("m"));
		medication.put("MOffset", ltmed.get("mTokenPosition"));

		String arffHead = "";
		for(String tag: tags){
			if(tag.matches("(m|ln)"))
				continue;

			if(ltmed.get(tag).equals("nm"))
				continue;

			HashMap<String, String> field = new HashMap<String,String>();

			String type = tag.toUpperCase();
			field.put("type", type);
			field.put(type, ltmed.get(tag));
			field.put(type + "Offset", ltmed.get(tag + "TokenPosition"));


			FeatureChoice ftc = new FeatureChoice(medication, field);
			String data = ftc.GetDatasetLine(this);
			if(!data.isEmpty())
				dataset += data + "1\n";

			if(arffHead.isEmpty())
				arffHead += ftc.GetArffHead("training");
		}
		//		System.out.println(dataset);
		HashMap<String,String> data = new HashMap<String,String>();
		data.put("head", arffHead);
		data.put("data", dataset);
		return data;
	}
	/*
	 * Definition: get mututal information for drug name and other fields in +/-2 lines
	 */
	public HashMap<String, String> GetMinusPlusMutalInformation(
			HashMap<String, String> listMed, ArrayList<String> listedMedications) {
		HashMap<String, String> medication = new HashMap<String,String>();
		medication.put("type", "M");
		medication.put("M", listMed.get("m"));
		medication.put("MOffset", listMed.get("mTokenPosition"));




		ArrayList<HashMap<String,String>> fields = GetMinusPlusFieldsByMedication(listMed, listedMedications);
		String dataset = "";
		String arffHead = "";
		for(HashMap<String,String> field: fields){
			FeatureChoice ftc = new FeatureChoice(medication, field);
			String data = ftc.GetDatasetLine(this);
			if(!data.isEmpty())
				dataset += data + "0\n";

			if(arffHead.isEmpty())
				arffHead += ftc.GetArffHead("training");

			String type = field.get("type");
			System.out.println(medication.get("M") + "\t" + medication.get("MOffset") + "\t" +  field.get(type) + "\t" + field.get(type + "Offset") + "\t" + type);
			System.out.println(data);
			System.out.println(GetTwoLineContextByOffset(medication.get("MOffset")));

			System.out.println();
		}
		HashMap<String,String> data = new HashMap<String,String>();
		data.put("head", arffHead);
		data.put("data", dataset);
		return data;
	}
	/*
	 * Input 1 : medication {EndLine=36, StartToken=0, MOffset=36:0 36:0, StartLine=36, EndToken=0, M=pravachol, type=M}
	 * Input 2 : field HashMap {F=q.day, FOffset=36:10 36:10, type=F} 
	 * 
	 * Output: true/false  == related/not related
	 */	
	public boolean disambiguateFieldsAroundDrugName(HashMap<String, String> medication,
			HashMap<String, String> field) {
		
		double[] distpro = null;
		distpro = getMedicationFiledRelationshipValue(medication, field);
	
		//		try {
		//			double cla = m_relentClassifier.classifyInstance(newInst);
		//		} catch (Exception e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
		// System.out.print("ï¿½ï¿½ï¿½ï¿½ï¿½Ô£ï¿½ ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½\n");
		// for (int i=0; i< distpro.length;i++)
		// {
		// System.out.print(distpro[i]+" ");
		// }
		// filter with 1
		//		double threshold = 0.75;
		//		return distpro[1] > threshold ? true : false;
		//		testing the model for duration and reason
		String fieldType = field.get("type");



		double threshold = Double.parseDouble(
				Messages.getString("JiaoDuan.i2b2.stitching.model.threshold"));

//				if( fieldType.equals("R")){
//					threshold = 0.01;
//					System.out.println(field.get(fieldType) + "\t" + field.get(fieldType+ "Offset") + "\t" + distpro[1]);
//					return distpro[1] > threshold ? true : false;
//				}else if( fieldType.equals("DU")){
//					threshold = 0.01;
//					System.out.println(field.get(fieldType) + "\t" + field.get(fieldType+ "Offset") + "\t" + distpro[1]);
//					return distpro[1] > threshold ? true : false;
//				}else
//					return distpro[0] > threshold ? false : true;

		return distpro[0] > threshold ? false : true;
	}
	/**
	 * @param medication
	 * @param field
	 * @return double[]: 1, relative;
	 * :0, not relative
	 */
	private double[] getMedicationFiledRelationshipValue(
			HashMap<String, String> medication, HashMap<String, String> field) {
		// get data set
		FeatureChoice ftc = new FeatureChoice(medication, field);
		// String head = ftc.GetArffHead("test");
		String dataset = ftc.GetDatasetLine(this);
		String[] data = dataset.split("\t");
		double[] featureValues = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			featureValues[i] = Double.parseDouble(data[i]);
		}
		Instance newInst = new Instance(1.0, featureValues);

		// add head information
		
		URL TRAIN = LancetParser.class.getResource(Messages
		.getString("i2b2.Medication.Relationship.trainFilePathName"));
		
		String trainFileName=null;
		try {
			trainFileName = RawInput.getTemporaryFilePath(TRAIN.openStream());
			tmpfiles.add(trainFileName);
			
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		if(training  == null){
			try {
				training = new Instances(new BufferedReader(new FileReader(
						trainFileName)));
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		training.setClassIndex(training.numAttributes() - 1);
		m_OutputFormat = new Instances(training, -1);

		newInst.setDataset(m_OutputFormat);

		double[] distpro = null;
		try {
//			System.out.println(newInst);
			distpro = m_relentClassifier.distributionForInstance(newInst);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(distpro == null)
			System.out.println();
		return distpro;
		
	}

	/*
	 * DEFINITION: Basing on i2b2 token offset (2:4 2:5), get original token contents
	 * 
	 * INPUT: String: token offset:
	 * 					2:4 2:5, 7:8 7:9
	 * 
	 * OUTPUT: String: original words. For separated tokens, use "..." separat it
	 */
	public String GetTokenContent(String offset) {
		ArrayList<HashMap<String, Integer>> lineTokens = this.parseOffset(offset);
		String tokenContents = "";
		for(HashMap<String, Integer> lineToken: lineTokens){
			int start = this.GetStartPositionOfToken(lineToken.get("StartLine"), lineToken.get("StartToken"));
			int end = this.GetEndPositionOfToken(lineToken.get("EndLine"), lineToken.get("EndToken"));

			String content = this.GetTokenContent(start, end);
			if(tokenContents.isEmpty())
				tokenContents = content;
			else
				tokenContents += "..." + content;

		}

		return tokenContents;
	}
	/*
	 * DEFINITION: Get adjective and noun phrases in +/- 2 lines by the drug name
	 * INPUT: i2b2Med: TYPE: HashMap<String, String>
	 * We could call it as i2b2 medication: type, M, MOffset. M, for drug name
	 * 				Example: 		HashMap<String, String> medication = new HashMap<String,String>();
									medication.put("type", "M");
									medication.put("M", listMed.get("m"));
									medication.put("MOffset", listMed.get("mTokenPosition"));
	   OUTPUT: 	all of the adjective and noun pharses in the +/-2 lines by the i2b2 drug name. Each phrase
	            is stored in a HashMap
	   				Example: [type="ADJECTIVE"; ADJECTIVE="dizzy"; ADJECTIVEOffset="1:1 1:1"]
	   					     [type="NOUN"; NOUN="pain in back"; NOUNOffset="1:3 1:5"]
	 */	
	public ArrayList<HashMap<String, String>> GetPhrasesInMinusPlusTwoLinesByMedication(
			HashMap<String, String> i2b2Med) {
		ArrayList<HashMap<String,String>> phrases = this.GetPhrases();

		String type = i2b2Med.get("type");
		String mOffset = i2b2Med.get(type + "Offset");

		for(HashMap<String, String>p: phrases){
			type = p.get("type");
//			System.out.println(p.get(type) + " " + p.get(type + "Offset"));
			//			decide p is in the +/-2 line of i2b2Med
			//			if true

			int dist = this.GetLineDistance(i2b2Med, p);
			if(dist <= 2)
				phrases.add(p);

		}
		return phrases;
	}

	public void exportTaggedTextToFile(ArrayList<String> listedMedsJmerki, String filePath) throws IOException {
		String taggedArticle = this.getFieldTaggedText(listedMedsJmerki);
		BufferedWriter out = new BufferedWriter(new FileWriter(filePath));
		out.write("<html><head>");
		out.write("<link rel='stylesheet' type='text/css' href='i2b2.css' /><body><pre>");
		out.write(taggedArticle);
		out.write("</body></html>");
		out.close();
	}

//	public void parse(String dxContent) {
//		InitialParser(content);
//	}

	public void summary() {
		
		System.out.println(Messages.getString("i2b2.ln.NGram.classify.tarinfile"));
		System.out.println(Messages.getString("i2b2.ln.NGram.classify.model"));		
		
		System.out.println(Messages.getString("i2b2.CRF.Lancet.Parser.CRFModel.FilePath"));
		System.out.println(Messages.getString("i2b2.Relate.Medication.Field.Model"));
		System.out.println(Messages.getString("i2b2.Medication.Relationship.trainFilePathName"));
		System.out.println(Messages.getString("JiaoDuan.i2b2.stitching.model.threshold"));
		System.out.println(Messages.getString("JiaoDuan.i2b2.lancet.parser.manner.tolowercase"));


	}

	/**
	 * input: bConverIntoSingleLine
	 * output: none
	 * process:
	 * Use Jsplitta to detect sentence boundary, and get 
	 * the start and end absolute position of the sentence in the 
	 * article. These information is set to member variable- span
	 * 			  span.put("BeginIndex", Integer.toString(begin));
				  span.put("EndIndex", Integer.toString(index));
				  span.put("Sentence", sent); 
	 */
	private void setSentenceSpans(boolean bConverIntoSingleLine) throws Exception {
		if(content == null){
			System.err.println("The function need content to be initialed");
			return;
		}
		String text = null;
		if(bConverIntoSingleLine)
			text = content.replaceAll("\n", " ");
		else
			text = content;
		String[] sents = null;

		String SentenceSplitter = Messages.
					getString("JiaoDuan.i2b2.lancet.sentence.boundary.detector");
		if(SentenceSplitter.equals("splitta")){
			if(jsp == null)
//				0 for nb
//				1 for svm
				jsp = new Jsplitta(0);
			boolean token = true;
			sents = jsp.RunSplitta(text, true);
		}else if(SentenceSplitter.equals("sptookit")){
			SentParDetector spd = new SentParDetector();
			String markedText = spd.markupRawText(2, text);
			sents = markedText.split("\n");
		}


		int index = 0;
		for(String sent: sents){
			//				  System.out.println(index + " :" + sent);
			HashMap<String, String> span = new HashMap<String, String>();
			Scanner scanner = new Scanner(new StringReader(sent));

			String word;
			int begin = 0;
			boolean bBegin = false;
			while (scanner.hasNext()) {
				word = scanner.next();
//				if(word.equals("OKConsults:"))
//					System.out.println(word);

				//					  bug matching a "\n";				
				word.replaceAll("\n", " ");
				word.trim();
				//					  System.out.println("word is :" + word);
				if(word.isEmpty() || word.equals("\\"))
					continue;

				word = rin .normalizeWord(word);

				//					  System.out.println("pattern: " + word);
				Pattern pword = Pattern.compile(word);

				Matcher m = pword.matcher(text);
				if(m.find()){
					if(!bBegin){
						begin = index + m.start();
						bBegin = true;
					} 
					index += m.end();
					int iEnd = m.end();
					text = text.substring(iEnd);
				}else{
					//						  error example: GSE2430 : cDNA Assay)”,
					System.err.println("Error In ListedMedication: SetSentenceSpans: 0430PM could not find the word" + word + "-\t-" + text);
				}
			}
			//				  System.out.println("Sentence Span: Begin: " + begin +", end: " + index);
			span.put("BeginIndex", Integer.toString(begin));
			span.put("EndIndex", Integer.toString(index));

			//				  convert sent to raw text
			sent = content.substring(begin, index);
			sent = sent.replace("\n", " ");

			span.put("Sentence", sent);

			SentenceSpans.add(span);
		}
	}
	public String getSentenceByTokenPosition(int beginLine, int beginToken, String subString) {
		// TODO Auto-generated method stub
		String sentence = "";
		int index = 0;
		for(int i =0; i < SentenceSpans.size(); i++){
			HashMap<String, String> span = SentenceSpans.get(i);
			int iBegin = Integer.parseInt(span.get("BeginIndex"));
			int iEnd = Integer.parseInt(span.get("EndIndex"));

			HashMap<String, Integer> offset = convertArticleRegion2TokenPosition(iBegin, iEnd);

			int iSentBeginLine = offset.get("StartLine");
			int iSentBeginToken = offset.get("StartTokenPosition");
			int iSentEndLine = offset.get("EndLine");
			int iSentEndToken = offset.get("EndTokenPosition");

			if(iSentBeginLine  == iSentEndLine){
				if(beginLine == iSentBeginLine){
					if(beginToken >= iSentBeginToken && beginToken <= iSentEndToken)
						sentence =  span.get("Sentence");
				}

			}else{
				if(beginLine == iSentBeginLine){
					if(beginToken >= iSentBeginToken )
						sentence =  span.get("Sentence");

				}else if(beginLine == iSentEndLine){
					if(beginToken <= iSentEndToken)
						sentence =  span.get("Sentence");
				}else{
					if(beginLine > iSentBeginLine && beginLine < iSentEndLine)
						sentence =  span.get("Sentence");
				}
			}

			if(!sentence.isEmpty()){
				index = i;
				break;
			}

		}
		if(subString.isEmpty())
			return sentence;

		String subStr = rin.normalizeWord(subString);
		Pattern pPart = Pattern.compile(subStr);
		Matcher mPart = pPart.matcher(sentence);

		if(!mPart.find()){
			System.err.println("Error in listedmedication 0748PM!");
			sentence += " " + SentenceSpans.get(index + 1).get("sentence");
		}
		return sentence;
	}

	public String getSentenceByListedMedication(String listMedication) {

		Pattern pOffset = Pattern.compile("(\\d+):(\\d+)\\s+(\\d+):(\\d+)");
		Matcher mm = pOffset.matcher(listMedication);
		int mStartLine = 0;
		int mStartToken = 0;
		if(mm.find()){
			mStartLine = Integer.parseInt(mm.group(1));
			mStartToken = Integer.parseInt(mm.group(2));
		}else{
			System.err.println("Erro in medication offset Listmedicaiton 0347 pm");
		}

		return getSentenceByTokenPosition(mStartLine, mStartToken, "");
	}
	/**
	 * @definition This function will split the discharg summary into phrases using montilinga.
	 * 				This function must be executed after SetSentenceSpans or InitialLstMedcine()
	 * 				function.
	 * The results is mPhrases Type: ArrayList<HashMap<String, String>>
	 * 				Example: [type="ADJNOUN"; ADJNOUN="pain in back"; ADJNOUNOffset="1:3 1:5"]
	 */
	private void parseDxSummaryIntoPhrases() {
		SentenceParser sp = new SentenceParser(); 
		for(HashMap<String,String> sentenceSpan : this.SentenceSpans){
			String sentence = sentenceSpan.get("Sentence");
			int sentBegin = Integer.parseInt(sentenceSpan.get("BeginIndex"));
			int sentEnd = Integer.parseInt(sentenceSpan.get("EndIndex"));

			for (Position p : sp.getPoses(sentence)){
				HashMap<String, String> phrase = new HashMap<String, String>();
				phrase.put("type", "ADJNOUN");

				String tContent = this.GetTokenContent(sentBegin + p.start, sentBegin + p.end);
				phrase.put("ADJNOUN", tContent);

				String offset = this.getTokenOffset(sentBegin + p.start, sentBegin + p.end);
				phrase.put("ADJNOUNOffset", offset);				

				mPhrases.add(phrase);
			}
		}

	}

	/**
	 * @definition To get member variable: mPhrases
	 * @return ArrayList<HashMap<String, String>> * Please refer to <ParseDischargeIntoPhrases> function (above)
	 */
	public ArrayList<HashMap<String, String>> GetPhrases() {
		return this.mPhrases;
	}

	private String getSentenceByArticlePosition(int start) {
		// TODO Auto-generated method stub
		HashMap<String, Integer> offset = convertArticleRegion2TokenPosition(start, start);

		String sentence = getSentenceByTokenPosition(offset.get("StartLine"), offset.get("StartTokenPosition"), "");

		return sentence;
	}

	/**
	 * This function is used to get list or narrative attribute of the sentence
	 * or phrase where the position is.
	 * input: start: int: article position
	 * output: String: ["list","narrative"]
	 */
	public String getListNarrative(int start) throws Exception {
		/**
		 * start: the String position of the article
		 */

		double listScore = 0;
		double narrativeScore = 0;

		String sentence = getSentenceByArticlePosition(start);

		//			base on discharge summary section
		String contextCue = null;
		for(int i=0; i < sctList.size(); i++)
		{
			if(sctList.get(i) > start)
			{
				int index = sctList.get(i-1);
				contextCue = dscSections.get(index);
				break;
			}
		}
		String ln = "";
		if(contextCue.equals("Medications"))
			listScore += 1;
		else
			narrativeScore += 1;

		//		   Shashank ln NGram term feature classification model
		if(LNClassifiers.containsKey("Ngram")){
			String rlt = NgramLNClassifier.classify(sentence, false);
			if(rlt.equals("l"))
				listScore +=1;
			else
				narrativeScore +=1;
		}

		//			Mallet document classification
		//			if(LNClassifiers.containsKey("Mallet")){
		//				String rlt = malletDoclassifier.Classify(sentence);
		//				if(rlt.equals("list"))
		//					listScore += 1;
		//				else
		//					narrativeScore += 1;
		//			}

		//			score system
		if(listScore > narrativeScore)
			ln = "list";
		else
			ln = "narrative";	

		return ln;
	}

	private void InitialLNClassifier() throws Exception {
		//			   Shashank'S ln NGram term feature classification model

		LNClassifiers.put("Ngram", 1);
		
		String tarfile = Messages.getString("i2b2.ln.NGram.classify.tarinfile");
		InputStream is = this.getClass().getResourceAsStream(tarfile);
		
		String tmpArff = RawInput.getTemporaryFilePath(is);
		tmpfiles.add(tmpArff);
		
		String arffFile = tmpArff;
		String model = Messages.getString("i2b2.ln.NGram.classify.model");
		
		NgramLNClassifier = new I2B2Classifier(model, arffFile);

//		LNClassifiers.put("Mallet", 1);
//		String trainfile = Messages.getString("i2b2.mallet.ln.classification.arff.file");
//		String modelfile = Messages.getString("i2b2.mallet.ln.classification.weka.model.file");
//		malletDoclassifier = new LNMalletClassifier(trainfile, modelfile);
	}

	public int getNumberOfSentence() {
		return this.SentenceSpans.size();
	}
	/**
	 * input: none
	 * output: ArrayList<HashMap<String, String>>
	 * Suppose each element is span. 
	 * @keyword       span.put("BeginIndex", Integer.toString(begin));
	   @keyword       span.put("EndIndex", Integer.toString(index));
	   @keyword  	  span.put("Sentence", sent);
			refer to SetSentenceSpans() function
	 */
	public ArrayList<HashMap<String, String>> getSentenceSpan() {
		return this.SentenceSpans;
	}

	private HashMap<String, String> getSentenceSpanByTokenPosition(
			int beginLine, int beginToken, String string) {
		int index = 0;
		HashMap<String, String> sentenceSpan = new HashMap<String,String>();
		for(int i =0; i < SentenceSpans.size(); i++){
			HashMap<String, String> span = SentenceSpans.get(i);
			int iBegin = Integer.parseInt(span.get("BeginIndex"));
			int iEnd = Integer.parseInt(span.get("EndIndex"));

			HashMap<String, Integer> offset = convertArticleRegion2TokenPosition(iBegin, iEnd);

			int iSentBeginLine = offset.get("StartLine");
			int iSentBeginToken = offset.get("StartTokenPosition");
			int iSentEndLine = offset.get("EndLine");
			int iSentEndToken = offset.get("EndTokenPosition");

			if(iSentBeginLine  == iSentEndLine){
				if(beginLine == iSentBeginLine){
					if(beginToken >= iSentBeginToken && beginToken <= iSentEndToken)
						sentenceSpan = span;
				}

			}else{
				if(beginLine == iSentBeginLine){
					if(beginToken >= iSentBeginToken )
						sentenceSpan = span;

				}else if(beginLine == iSentEndLine){
					if(beginToken <= iSentEndToken)
						sentenceSpan = span;
				}else{
					if(beginLine > iSentBeginLine && beginLine < iSentEndLine)
						sentenceSpan = span;
				}
			}

			if(!sentenceSpan.isEmpty()){
				index = i;
				break;
			}

		}

		return sentenceSpan;
	}

	public HashMap<String, String> getSentenceSpanByListedMedication(String listMedication, String fieldType) {
		fieldType = fieldType.toLowerCase();
		fieldType += "\\s*=\\s*\".*?\"\\s*";
		fieldType += "(\\d+):(\\d+)\\s+(\\d+):(\\d+)";

		Pattern pOffset = Pattern.compile(fieldType);
		Matcher mm = pOffset.matcher(listMedication);
		int mStartLine = 0;
		int mStartToken = 0;
		if(mm.find()){
			mStartLine = Integer.parseInt(mm.group(1));
			mStartToken = Integer.parseInt(mm.group(2));
		}else{
			System.err.println("Erro in medication offset Listmedicaiton 0347 pm");
			return null;
		}

		return getSentenceSpanByTokenPosition(mStartLine, mStartToken, "");
	}

	/**
	 * @param listedMeds
	 * @param makeLowerCase
	 * @param labelProblemList
	 * @return
	 */
	public ArrayList<String> tagLabelForCRFModelBySentence(
			ArrayList<String> listedMeds, boolean makeLowerCase,
			boolean labelProblemList) {
		
		ArrayList<String> labelledSentences = new ArrayList<String>();
		
		ArrayList<HashMap<String, ArrayList<String>>> goldStd = getGoldStdTokens(listedMeds);
		
		ArrayList<HashMap<String, String>> sentences = getSentenceSpan();
		for(HashMap<String,String> data: sentences){
			int iStart = Integer.parseInt(data.get("BeginIndex"));
			int iEnd = Integer.parseInt(data.get("EndIndex"));
			String sent = data.get("Sentence");
			
			String taggedSent = "";
			String oldTag = "";
			Scanner sc = new Scanner(sent);
			
			int index = -1;
			while(sc.hasNext()){
				String token = sc.next();
				Pattern pToken = Pattern.compile(rin.normalizeWord(token), Pattern.CASE_INSENSITIVE);
				Matcher mToken = pToken.matcher(sent);
				while(mToken.find()){
					if(mToken.start() > index){
						index = mToken.start();
						break;
					}
				}
				HashMap<String, Integer> offset = super.
					convertArticleRegion2TokenPosition(iStart + index, 
								iStart + index + token.length());
				String[] tags = Messages.getString("i2b2.CRF.TAGs").split(",");
				
				int lineIndex = offset.get("StartLine");
				int tokenIndex = offset.get("StartTokenPosition");
				String sTag = getTokenTagType(goldStd, tags, lineIndex, tokenIndex);
//				System.out.println(token + "|" + tt);
				
				if(!sTag.equals(oldTag) || sTag.equals("O"))
				{
					if(!sTag.equals("O")){
						taggedSent += token +  "|B-" + sTag + " ";
					}
					else
						taggedSent += token +  "|" + sTag + " ";
				}else
					taggedSent += token + "|I-" + sTag + " ";

				oldTag = sTag;
			}
			labelledSentences.add(taggedSent);
			
		}
		
		
		return labelledSentences;
	}

	/**
	 * @param listMeds
	 * @param string
	 * @throws IOException 
	 */
	public void exportTaggedTextToFileByEntity(ArrayList<String> listMeds,
			String filePath) throws IOException {
		String[] fields = Messages.getString("i2b2.competition.2009.fields").split(",");
		ArrayList<HashMap<String, Integer>> tokenList = new ArrayList<HashMap<String, Integer>>();
		
		for(HashMap<String,String> tag: taglist){
			
			Pattern pOffset = Pattern.compile("(\\d+):(\\d+)\\s+(\\d+):(\\d+)");

			for(int i =0; i < fields.length; i ++){
				String fieldType = fields[i];

				String offset = tag.get(fieldType.toUpperCase() + "Offset");
				if(offset == null)
					continue;
				Matcher mOffset = pOffset.matcher(offset);
				if(mOffset.find()){
					HashMap<String, Integer> token = new HashMap<String, Integer>();
					token.put("StartLine", Integer.parseInt(mOffset.group(1)));
					token.put("StartToken", Integer.parseInt(mOffset.group(2)));
					token.put("EndLine", Integer.parseInt(mOffset.group(3)));
					token.put("EndToken", Integer.parseInt(mOffset.group(4)));
					token.put("type", i);

					tokenList.add(token);
				}
			}
		}
		String highLightedText = highlighTokens(tokenList, false);
		
		BufferedWriter out = new BufferedWriter(new FileWriter(filePath));
		out.write("<html><head>");
		out.write("<link rel='stylesheet' type='text/css' href='i2b2.css' /><body><pre>");
		out.write(highLightedText);
		out.write("</body></html>");
		out.close();
		
	}

	/**
	 * @param listMeds
	 * @return
	 * @throws Exception 
	 */
	public ArrayList<String> filterListMedsWithNegExByLine(ArrayList<String> listMeds) throws Exception {
		GenNegEx ng = new GenNegEx();
		ArrayList<String> lmsAfterFilteration = new ArrayList<String>();
		for(String lm: listMeds){
			String drugName = ListedMedication.getFieldValue("M", lm);
			drugName = RawInput.normalizeWord(drugName);
			String line = getLineByListedMedication(lm);
			boolean isAllergy = ng.IsNegativeEvent(line, drugName);
			if (!isAllergy)
				lmsAfterFilteration.add(lm);
			else{
				String sShowStd = Messages.getString("i2b2.filter.Negative.Medication.With.NegEx.Output.STD");
				if(sShowStd.equals("1"))
					System.out.println("FILTERED:\t" + lm);
			}
		}
		return lmsAfterFilteration;
	}

	public void clean() {
		RawInput.delAllFiles(tmpfiles);
	}
}
