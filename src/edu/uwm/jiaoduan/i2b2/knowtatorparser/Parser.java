package edu.uwm.jiaoduan.i2b2.knowtatorparser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.uwm.jiaoduan.Messages;
import edu.uwm.jiaoduan.i2b2.utils.*;

/**
 *
 * @author shashank
 * modified by zuofeng
 */
public class Parser {
	
    static LancetParser lancet = null;
	private String baseDirectory = "";
	private String knowtatorXmlFile = "";
	private String articleFile = "";
	
	private ArrayList<String> listedMedications = null;

    
public Parser(String knowtatorAnnoationXmlFile) throws Exception {
		// TODO Auto-generated constructor stub
	
	knowtatorXmlFile = knowtatorAnnoationXmlFile;
	
	String extName = ".knowtator.xml";
	articleFile = knowtatorXmlFile.substring(0, (knowtatorXmlFile.length() - extName.length()));
	articleFile = articleFile.replace("i2b2-2009-knowatorXML", "090601/trainingdata");
	
	lancet = new LancetParser(articleFile);
	
	GetListedMedicationFromKnowtatorXMLfile();
	
	
	

	
	}
public Parser(String rawfilePathName,String knowtatorAnnoationXmlFile){
	knowtatorXmlFile = knowtatorAnnoationXmlFile;
	articleFile = rawfilePathName;
	try {
		lancet = new LancetParser(rawfilePathName);
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	GetListedMedicationFromKnowtatorXMLfile();
}

	//    private static final double MAX_TOKEN_SIZE_EACH_LINE = 100;
	private static Map<String, Annotation> getAnnotations(String baseDirectory, String knowtatorXmlFile, String articleFile) {
        KnowtatorXMLReader kxr = new KnowtatorXMLReader(baseDirectory + knowtatorXmlFile, baseDirectory + articleFile);
        kxr.parse();
        return kxr.getAnnotations();
    }

    /**
     * Prints narrative, list and medication with their slots and their values.
     * @param baseDirectory the directory containing knowtator exported xml file
     * and the article file
     * @param knowtatorXmlFile the name of the xml file exported by knowtator
     * @param articleFile the name of the article text file
     */
    public static void hierarchyPrint(String baseDirectory, String knowtatorXmlFile, String articleFile) {
        Map<String, Annotation> annotations = getAnnotations(baseDirectory, knowtatorXmlFile, articleFile);
        for (String annotationId : annotations.keySet()) {
            Annotation annotation = annotations.get(annotationId);
            String annotationName = annotation.getName();
            if (annotationName.equalsIgnoreCase("medication") ||
                    annotationName.equalsIgnoreCase("narrative") ||
                    annotationName.equalsIgnoreCase("list")) {
//                System.out.println("Found " + annotationName + " Annotation: " + annotation.getAssociatedText() + ", spanning from " + annotation.getSpanStart() + " to " + annotation.getSpanEnd());
                Map<String, Slot> annotationSlots = annotation.getSlots();
                for (String slotId : annotationSlots.keySet()) {
                    Slot slot = annotationSlots.get(slotId);
//                    System.out.println("  Found Slot: " + slot.getSlotName());
//                    System.out.println("    Slot value class: " + slot.getValueType());
                    String slotValueClass = slot.getValueType();
                    if (slotValueClass.equalsIgnoreCase("i2b2knowtatorparser.I2B2String")) {
                        I2B2String slotValue = (I2B2String) slot.getValue();
                        System.out.println("    Value: " + slotValue.getI2b2String());
                    } else if (slotValueClass.equalsIgnoreCase("i2b2knowtatorparser.Annotation")) {
                        Annotation slotValue = (Annotation) slot.getValue();
                        System.out.println("    Value: " + slotValue.getAssociatedText() + " spanning from " + slotValue.getSpanStart() + " to " + slotValue.getSpanEnd());
//                        System.out.println("    Id: " + slotValue.getId());
                    }
                }
            }
        }
    }

    /**
     * Prints the extracted annotations in i2b2 format
     * @param baseDirectory the directory containing knowtator exported xml file
     * and the article file
     * @param knowtatorXmlFile the name of the xml file exported by knowtator
     * @param articleFile the name of the article text file
     */

//    public  void i2b2Print(String baseDirectory, String knowtatorXmlFile, String articleFile) {
//    	
//    	ArrayList<String> listmeds = GetListedMedicationFromKnowtatorXMLfile();
//    	for(String lmed: listmeds)
//    		System.out.println(lmed);
//    }

    public  void GetListedMedicationFromKnowtatorXMLfile() {
    	listedMedications = new ArrayList<String>();
    	
        Map<String, Annotation> annotations = getAnnotations(baseDirectory, knowtatorXmlFile, articleFile);
//        lm = new ListedMedication(articleFile);
        Map<String, String> slotPrintVal = new HashMap<String, String>();
        for (String annotationId : annotations.keySet()) {
            Annotation annotation = annotations.get(annotationId);
            String annotationName = annotation.getName();
            if (annotationName.equalsIgnoreCase("narrative") || annotationName.equalsIgnoreCase("list") || annotationName.equalsIgnoreCase("negative")) {
                for (String mnSlotId : annotation.getSlots().keySet()) {
                    for (I2B2Knowtator med : annotation.getSlots().get(mnSlotId).getValues()) {
                        Annotation medication = (Annotation) med;
                        String medicationName = medication.getAssociatedText();
                        int medicationNameStart = medication.getSpanStart(); //TODO: Zuofeng - change to line and token
                        int medicationNameEnd = medication.getSpanEnd(); //TODO: Zuofeng - change to line and token
                        String offset = lancet.getTokenOffset(medicationNameStart, medicationNameEnd);
//                        String printString = "m=\"" + medicationName + "\" " + medicationNameStart + " " + medicationNameEnd + "|| ";
                        medicationName = lancet.GetTokenContent(medicationNameStart, medicationNameEnd);
                        String printString = "m=\"" + medicationName + "\" " + offset + "||";


                        Map<String, Slot> slots = medication.getSlots();
                        slotPrintVal.clear();
                        for (String slotId : slots.keySet()) {
                            Slot slot = slots.get(slotId);
                            slotPrintVal.put(slot.getSlotName(), getSlotNameAndValue(slot));
                        }
                        
                        if (slotPrintVal.containsKey("do")) {
                            printString += slotPrintVal.get("do") + "||";
                        } else {
                            printString += "do=\"nm\"||";
                        }
                        if (slotPrintVal.containsKey("mo")) {
                            printString += slotPrintVal.get("mo") + "||";
                        } else {
                            printString += "mo=\"nm\"|| ";
                        }
                        if (slotPrintVal.containsKey("f")) {
                            printString += slotPrintVal.get("f") + "||";
                        } else {
                            printString += "f=\"nm\"||";
                        }
                        if (slotPrintVal.containsKey("du")) {
                            printString += slotPrintVal.get("du") + "||";
                        } else {
                            printString += "du=\"nm\"|| ";
                        }
                        if (slotPrintVal.containsKey("r")) {
                            printString += slotPrintVal.get("r") + "||";
                        } else {
                            printString += "r=\"nm\"||";
                        }
                        if (slotPrintVal.containsKey("e")) {
                            printString += slotPrintVal.get("e") + "||";
                        } else {
                            printString += "e=\"nm\"||";
                        }
                        if (slotPrintVal.containsKey("t")) {
                            printString += slotPrintVal.get("t") + "||";
                        } else {
                            printString += "t=\"nm\"||";
                        }
                        if (slotPrintVal.containsKey("c")) {
                            printString += slotPrintVal.get("c") + "||";
                        } else {
                            printString += "c=\"nm\"||";
                        }

                        printString += "ln=\"" + annotationName + "\"";
                        printString = printString.replaceAll("\n", " ");
//                        System.out.println(printString);
                        listedMedications.add(printString);
                    }
                }
            }

        }
//        ListedMedications = SortByTokenPosition(ListedMedications);
	}

	private static ArrayList<String> SortByTokenPosition(
			ArrayList<String> listmeds) {
		// TODO Auto-generated method stub
		HashMap<Double,Integer> map = new HashMap<Double, Integer>();
		ArrayList<String> sortedlm = new ArrayList<String>();
		ArrayList<Double> indexs = new ArrayList<Double>();
		
        for(int i=0; i < listmeds.size(); i++)
        {
        	HashMap<String, String> parseRlt = new HashMap<String,String>();
        	lancet.GetFeatures(listmeds.get(i) , parseRlt);
        	String mOffset = parseRlt.get("mTokenPosition");
        	
        	String[] fields = mOffset.split("[,|:|\\s]");
        	if(fields[0] == null)
        		System.out.println();
        	
        	double startLine = Double.parseDouble(fields[0]);
        	double startToken = Double.parseDouble(fields[1]);
        	
        	double lineTokenSize = lancet.GetLineTokenNumber(startLine);
        	
//        	Double index = startLine + startToken/MAX_TOKEN_SIZE_EACH_LINE;
        	if(startToken > lineTokenSize)
        		System.err.println("Erro in list medication: getLineTokenNumber");
        	
        	Double index = startLine + startToken/lineTokenSize;
        	
        	map.put(index, i);
        	indexs.add(index);
        }
        Collections.sort(indexs);
        for(double index: indexs)
        {
        	int originalId = map.get(index);
        	sortedlm.add(listmeds.get(originalId));
        }
 
		return sortedlm;
	}

	private static String getSlotNameAndValue(Slot slot) {
        String slotName = slot.getSlotName();
        if (slot.getValueType().endsWith("Annotation")) {
            String returnString = "";
            List<String> values = new ArrayList<String>();
            List<String> spans = new ArrayList<String>();

            List<Annotation> annotations = new ArrayList<Annotation>(slot.getValues().size());
            for (I2B2Knowtator val : slot.getValues()) {
            	Annotation slotValue = (Annotation) val;
            	annotations.add(slotValue);
            }
            Collections.sort(annotations);
            
            for (Annotation slotValue : annotations) {
                if (slotValue.getSpanStart() <= 0 || slotValue.getSpanEnd() < 0) {
                    return slotName + "=\"nm\"";
                } else {
                    values.add(slotValue.getAssociatedText());
//                    spans.add(slotValue.getSpanStart() + " " + slotValue.getSpanEnd()); //TODO: Zuofeng - change to line and token
                    String offset = lancet.getTokenOffset(slotValue.getSpanStart(), slotValue.getSpanEnd());
                    spans.add(offset);
                }
            }
            returnString = slotName + "=\"" + joinMultiple(values, "...") + "\" " + joinMultiple(spans, ",");
            return returnString;
        } else {
            I2B2String i2b2SlotValue = (I2B2String) slot.getValue();
            String slotValue = i2b2SlotValue.getI2b2String();
            return slotName + "=\"" + slotValue + "\"";
        }
    }

    private static String joinMultiple(List<String> values, String connector) {
        if (values.size() == 0) {
            return "";
        }
        String joinStr = values.get(0);
        for (int i = 1; i < values.size(); ++i) {
            joinStr += connector + values.get(i);
        }
        return joinStr;
    }

    /**
     * @param args the command line arguments
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
////        String baseDirectory = "./i2b2Data/i2b2-2009-knowatorXML/";
//    	String baseDirectory = Messages.getString("i2b2.KnowtaorXMLFolder");
//        RawInput rinput = new RawInput();
//        ArrayList<String> kxmlfileList = new ArrayList<String>();
//        rinput.GetDirectoryFile(baseDirectory, kxmlfileList);
//        
//        System.out.println(kxmlfileList.size());
//        for(String knowtatorXmlFile: kxmlfileList)
//        {
////        	String knowtatorXmlFile = kxmlfileList.get(0);
//
//        	//        [./i2b2Data/i2b2-2009-knowatorXML/2/11995.knowtator.xml
//        	String extName = ".knowtator.xml";
//        	String articleFilePathName = knowtatorXmlFile.substring(0, (knowtatorXmlFile.length() - extName.length()));
//        	articleFilePathName = articleFilePathName.replace("i2b2-2009-knowatorXML", "annotation/trainingdata");
//
//        	//        hierarchyPrint(baseDirectory, knowtatorXmlFile, articleFile);
////        	System.out.println(articleFilePathName);
//        	//        i2b2Print("", knowtatorXmlFile, articleFile);
//        	Parser p = new Parser(knowtatorXmlFile);
//
//        	ArrayList<String> listMeds =p.GetListedMedications();
//        	for(int i=0; i < listMeds.size(); i++)
//        	{
////        		System.out.println(listMeds.get(i));
//        	}
//        	String[] tags = Messages.getString("i2b2.CRF.TAGs").split(",");
//        	ListedMedication listm = new ListedMedication(articleFilePathName);
////        	String taggedArticle = listm.TagLabelForCRFModel(tags, articleFilePathName, listMeds);
////        	String taggedArticle = p.GetTaggedArticleForCRFModel();
////        	make tagged string clear for AbNer which do not recognize ||O
//        	taggedArticle = taggedArticle.replaceAll("\\|\\|O", "");
//        	
//        	System.out.print(taggedArticle);
//        }
    }

	public ArrayList<String> getListedMedications() {
		// TODO Auto-generated method stub
		return listedMedications;
	}
/*
 * Return the tagged content for CRF model;
 * bMakeLowerCase: Whether to convert all of character into lower case; If not, original morphology
 * 				would be kept.
 */
	public String getTaggedArticleForCRFModel(boolean bMakeLowerCase, boolean bLabelProblemList, boolean bAddReasonSingleton) {
		
//		bAddSingleton
		ArrayList<String> listedMeds = this.getListedMedications();
		if(bAddReasonSingleton){
			JMerki jm = new JMerki();
			try {
				jm.initializeParser();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			ArrayList<String> listedReason = jm.getListedReason(lancet.GetContent());
			for(String lr: listedReason){
				listedMeds.add(lr);
			}
			
		}
		
		return lancet.tagLabelForCRFModel(listedMeds, bMakeLowerCase, bLabelProblemList);
	}
	
	public ListedMedication GetListedMedication(){
		return lancet;
	}
	
	public String GetTaggedArticleForSwissKnifeCRFModel(){
		SwissKnifeParser swKnife = null;
		try {
			swKnife = new SwissKnifeParser(articleFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return swKnife.tagSwissKnifeLabelForCRFModel(listedMedications);
	}

	public HashMap<String, String> getARFFDataset(ArrayList<String> listedReason) {
		// TODO Auto-generated method stub
		String head = "";
		String dataset = "";
		HashMap<String,String> ArffDataSet = new HashMap<String, String>();

		for(int i = 0; i < listedMedications.size(); i++){
//			dataset += knowtatorXmlFile + "\n";
			
			System.out.println(i + "/" + listedMedications.size());
			
			String listMed = listedMedications.get(i);
			
			if(listMed.contains("singleton"))
					System.out.println(listMed);
			HashMap<String, String> medication = new HashMap<String,String>();
			
			lancet.GetFeatures(listMed, medication);
			HashMap<String,String> PositiveData = lancet.getMedicationFieldsMutalInformation(medication);
			dataset += PositiveData.get("data");
			
			if(head.isEmpty())
				head = PositiveData.get("head");
			
			ArrayList<String> otherListMeds =new ArrayList<String>();
			for(String lm: getListedMedications())
				otherListMeds.add(lm);
			
			otherListMeds.remove(i);
			if(listedReason.size() >0){
				
				
				for(String lr: listedReason){
					otherListMeds.add(lr);
				}
			}
			
			HashMap<String,String> NegativeData = lancet.GetMinusPlusMutalInformation(medication, otherListMeds);
			dataset += NegativeData.get("data");
		}
//		make full arff data
		ArffDataSet.put("head", head);
		ArffDataSet.put("data", dataset);
		return ArffDataSet;
	}

	public ArrayList<String> GetListNarrativeSentences(String type) {
		// TODO Auto-generated method stub
		/*
		 * input type: list or narrative
		 */
		
		ArrayList<String> sentences = new ArrayList<String>();
		
    	for(String listmed: listedMedications)
    	{
    		HashMap<String,String> medication = new HashMap<String,String>();
    		lancet.GetFeatures(listmed, medication);
    		String offset = medication.get("mTokenPosition");
    		Pattern pOffset = Pattern.compile("(\\d+):(\\d+)\\s+(\\d+):(\\d+)");
    		Matcher mm = pOffset.matcher(offset);
    		String sentence = "";
    		if(mm.find()){
    			int iBeginLine = Integer.parseInt(mm.group(1));
    			int iBeginToken = Integer.parseInt(mm.group(2));
    			
    			String drugName = medication.get("m");
    			sentence = lancet.getSentenceByTokenPosition(iBeginLine, iBeginToken, drugName);
    			
    			sentence = drugName + "\t" + sentence;
    			
    		}else{
    			System.err.println("Error 0623PM in knowtator parsor could not match medicaiton offset!!!");
    		}
    		String ln = medication.get("ln");
    		if(ln.equals(type))
    			sentences.add(sentence);
    	}
		
		
		return sentences;
	}

//	public ArrayList<String> GetListSentenceWithMedication() {
//		// TODO Auto-generated method stub
//		ArrayList<String> lists = new ArrayList<String>();
//		return lists;
//	}

//	public String GetListedMedicationsFromKnow(String articleFilePathName, String knowtatorXmlFile, String[] tags) {
//		// TODO Auto-generated method stub
//
//    	ArrayList<String> listMeds =GetListedMedicationFromKnowtatorXMLfile("", knowtatorXmlFile, articleFilePathName);
//    	
//    	ListedMedication listm = new ListedMedication(articleFilePathName);
//    	String taggedArticle = listm.TagLabelForCRFModel(tags, articleFilePathName, listMeds);
//    	
////    	make tagged string clear for AbNer which do not recognize ||O
//    	taggedArticle = taggedArticle.replaceAll("\\|\\|O", "");
//		return taggedArticle;
//	}
	
	
	public boolean AnnotationToI2b2(String baseFolder){
		
		File folder = new File(baseFolder);
		String outfile = null;
		if(folder.isDirectory()){
			if(!baseFolder.endsWith("/")){
				baseFolder += "/";
			}
			
			File fArticle = new File(articleFile);
			String fileName = fArticle.getName();
			outfile = baseFolder + fileName + ".i2b2.entries";
		}else if(folder.isFile()){
			outfile = baseFolder;
		}else{
//			System.err.println("Something rong with the input for annotationToI2b2 in knowator parser. It is not a folder or file");
			System.out.println("Create a new file in annotationToI2b2 in knowator parser");
			outfile = baseFolder;
//			return false;
		}
				
		File fout = new File(outfile);
//		if(fout.exists())
//			return false;
		
		BufferedWriter fList;
		
		try {
			fList = new BufferedWriter(new FileWriter(outfile));

			for(String lstMed: listedMedications){
				String value =lancet.GetFieldValue("ln", lstMed);
				if(value.equals("negative"))
					continue;

				fList.write(lstMed.toLowerCase());
				fList.write("\n");
			}
			fList.close();				

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
	/*
	 * get original content of the discharge summary
	 */
	public String getRawContent() {
		return lancet.getOriginContent();
	}
	/**
	 * @return
	 */
	public ArrayList<String> getTaggedSentences() {
		ArrayList<String> listedMeds = this.getListedMedications();
		boolean bMakeLowerCase = true;
		boolean bLabelProblemList = false;
		return lancet.tagLabelForCRFModelBySentence(listedMeds, bMakeLowerCase, bLabelProblemList);
	}

}
