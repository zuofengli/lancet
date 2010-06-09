package edu.uwm.jiaoduan.i2b2.train;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umass.cs.mallet.base.types.InstanceList;
import edu.uwm.jiaoduan.Messages;
import edu.uwm.jiaoduan.i2b2.knowtatorparser.Parser;
import edu.uwm.jiaoduan.i2b2.utils.*;
import edu.uwm.jiaoduan.tools.abner.Trainer;
import edu.uwm.jiaoduan.tools.biocreative.MalletImportPipe;
import edu.uwm.jiaoduan.tools.biocreative.MalletWekaInterface;

public class TrainingDatasetBuilder {

	/**
	 * @param args
	 */
	private static String KnowtatorXMLDirectory = Messages.getString("i2b2.KnowtaorXMLFolder");
	
	public static void main(String[] args) throws Exception {
		boolean bMakeLowerCase = false;//convert training article into lower case
		boolean bLabelProblemList = false;
		boolean bAddReasonSingleton = false;
		
		TrainingDatasetBuilder builder = new TrainingDatasetBuilder();
		
//		in the messages.properties file
		String tagListName = "JiaoDuan.i2b2.CRFModel.Annotation.Tags";
//		String tagListName = "JiaoDuan.i2b2.CRFModel.Annotation.Negative.Medication.Tags";//add negm
		String trainFile = "./CRFmodel/Lancet147.train";

		
		String modelFile = "./CRFmodel/afterFixBug/Lancet147Dosage.crf";
		
		String singleTagTrainFile = "./CRFmodel/Lancet147Reason";
		String[] tags = {"DO"};
		
		
//		String stitchingModelTrainfile = "./CRFmodel/afterFixBug/stitching147ReasonSingleTon.arff";
//		builder.buildCRFTrainBySentence(trainFile, bMakeLowerCase, bLabelProblemList, bAddReasonSingleton );
//		builder.buildCRFi2b2MedicationTrain(trainFile, bMakeLowerCase, bLabelProblemList, bAddReasonSingleton );
//		String[] tags = Messages.getString(tagListName).split(",");
		builder.trainAnI2B2CRFmodel(trainFile, modelFile, tags );
//		builder.buildMedicationRelationshipTrainingData(stitchingModelTrainfile, bAddReasonSingleton);
//		
////		ShaShank NGram ln CLassfiction model
//		String listFile = Messages.getString("i2b2.list.sentence.trainingFilePahtName");
//		String narrativeFile = Messages.getString("i2b2.narrative.sentence.trainingFilePahtName");
//		String negativeFile = Messages.getString("i2b2.negative.medication.sentence.trainingFilePahtName");
////		
//		GenerateListNarrativeSentenceTrainingData(listFile, narrativeFile, negativeFile);
//		
////		ln document classlifcication
////		GenerateLNTrainingFileForMalletDocumentClassification();
//		GenerateLNTrainingFileForMalletDocumentClassificationBySentence();
//		
//		File lnRoot = new File(Messages.getString("i2b2.mallet.document.train.file.folder"));
//		String lntrain = Messages.getString("i2b2.mallet.ln.classification.arff.file");
//		
//		MalletWekaInterface mwInterface = new MalletWekaInterface();
//		mwInterface.GenerateTrainingARFFfile(lnRoot, lntrain);
	}

	/**
	 * @param trainFile
	 * @param makeLowerCase
	 * @param labelProblemList
	 * @param addReasonSingleton
	 * @throws Exception 
	 */
	private void buildCRFTrainBySentence(String trainFile,
			boolean makeLowerCase, boolean labelProblemList,
			boolean addReasonSingleton) throws Exception {
		String baseDirectory = KnowtatorXMLDirectory;
		
        RawInput rinput = new RawInput();
        ArrayList<String> kxmlfileList = new ArrayList<String>();
        rinput.getDirectoryFile(baseDirectory, kxmlfileList);
        
        BufferedWriter fTrain;
        try {
        	fTrain = new BufferedWriter(new FileWriter(trainFile));

        	for(String knowtatorXmlFile: kxmlfileList)
        	{  		
        		System.out.println(knowtatorXmlFile);
//        		if(!knowtatorXmlFile.contains("11995"))
//        			continue;
        		
        		Parser p = new Parser(knowtatorXmlFile);
//            	String taggedArticle = p.getTaggedArticleForCRFModel(makeLowerCase, labelProblemList, addReasonSingleton);
            	
        		ArrayList<String> taggedSentences = p.getTaggedSentences();
        		
        		for(String sentence: taggedSentences){
        			sentence = sentence.replaceAll("\\|\\|O", "");
        			//        	System.out.print(taggedArticle);
        			fTrain.write(sentence + "\n\n");
        		}
        	}
        	
        	fTrain.close();
        } catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }
        
        System.out.println(kxmlfileList.size());
		
	}

	private static void ParseSNOMEDCore() throws IOException {
		// TODO Auto-generated method stub
		String SnomedCoreFile = Messages.getString("snomed.core.terminology.file");
		String TermFile = Messages.getString("i2b2.medical.term.tsv.file");
		
		Pattern midAnnot = Pattern.compile("^(\\d+)\\|(.*)\\s+\\((.*)\\)\\|\\d\\|(\\w+)\\|(.*)");
		
		String line = new String();

		Object rlt = null;
	 
			BufferedReader in = new BufferedReader(new FileReader(SnomedCoreFile));
			BufferedWriter out = new BufferedWriter(new FileWriter(TermFile));

			out.write("termName" + "\t" + "cui" + "\t" + "tty" + "\n");
			int i = 0;
			while ((line = in.readLine()) != null) {
				
				Matcher ptnMatcher = midAnnot.matcher(line);
				
				if(ptnMatcher.find()){
					i++;
					out.write(ptnMatcher.group(2) + "\t" + ptnMatcher.group(4) + "\t" +ptnMatcher.group(3));
					out.write("\n");
				}
			}
			in.close();
			out.close();
		
	}
/*
 * Definition: Genereate CRF model for i2b2 2009
 */
	void trainAnI2B2CRFmodel(String trainFile, String modelFile, String[] strTagList) throws Exception {
		
//		String crfTrainingFile = Messages.getString("i2b2.CRFTrainFilePathName");
//		buildCRFi2b2MedicationTrain(trainFile);
		
		
		Trainer t = new Trainer();

//		String modelFile = Messages.getString("i2b2.CRFModelFilePathName");
		
		t.train(trainFile, modelFile, strTagList);
	}

	private static void GenerateLNTrainingFileForMalletDocumentClassificationBySentence() throws Exception {
		// TODO Auto-generated method stub
		String baseDirectory = KnowtatorXMLDirectory;

		RawInput rinput = new RawInput();
		ArrayList<String> kxmlfileList = new ArrayList<String>();
		RawInput.getDirectoryFile(baseDirectory, kxmlfileList);


		try {

			for(String knowtatorXmlFile: kxmlfileList)
			{  
				File fxml = new File(knowtatorXmlFile);
				String fileName = fxml.getName();

				Parser p = new Parser(knowtatorXmlFile);

				ArrayList<String> narratives = p.GetListNarrativeSentences("narrative");
				ArrayList<String> lists = p.GetListNarrativeSentences("list");

				for(int i =0; i < narratives.size(); i++){
					//        			fNarrative.write(knowtatorXmlFile + "\t" + sent + "\n");
					String narrativeFile = Messages.getString("i2b2.mallet.document.train.file.folder.narrative") + fileName;
					narrativeFile += "." + Integer.toString(i) + ".txt";
					File fn = new File(narrativeFile);
					if(fn.exists())
						System.err.println("error in generate traing data for mallet document classification: 0618 pm File exist!");
					
					BufferedWriter fNarrative = null;
					fNarrative = new BufferedWriter(new FileWriter(narrativeFile));
										
					fNarrative.write(narratives.get(i) + "\n");
					
					fNarrative.close();

				}
				for(int j=0; j < lists.size(); j++){
					//        			fList.write(knowtatorXmlFile + "\t" + sent + "\n");
					String listFile = Messages.getString("i2b2.mallet.document.train.file.folder.list") + fileName;
					listFile += "." + Integer.toString(j) + ".txt";
					File fl = new File(listFile);
					if(fl.exists())
						System.err.println("error in generate traing data for mallet document classification: 0618 pm File exist!");
					
					BufferedWriter fList = null;
					fList = new BufferedWriter(new FileWriter(listFile));
					
					fList.write(lists.get(j) + "\n");
					
					fList.close();
				}

				
			}


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(kxmlfileList.size());

		//generate mallet file
		File root = new File(Messages.getString("i2b2.mallet.document.train.file.folder"));

		String arffFile = Messages.getString("i2b2.mallet.ln.classification.arff.file");

		MalletWekaInterface mwInterface = new MalletWekaInterface();

		mwInterface.GenerateTrainingARFFfile(root, arffFile);

	}

	private static void GenerateLNTrainingFileForMalletDocumentClassification() throws Exception {
		// TODO Auto-generated method stub
		String baseDirectory = KnowtatorXMLDirectory;
		
        RawInput rinput = new RawInput();
        ArrayList<String> kxmlfileList = new ArrayList<String>();
        RawInput.getDirectoryFile(baseDirectory, kxmlfileList);
        

        try {

        	for(String knowtatorXmlFile: kxmlfileList)
        	{  
        		File fxml = new File(knowtatorXmlFile);
        		String fileName = fxml.getName();
        		
        		String listFile = Messages.getString("i2b2.mallet.document.train.file.folder.list") + fileName + ".txt";
        		String narrativeFile = Messages.getString("i2b2.mallet.document.train.file.folder.narrative") + fileName + ".txt";
        		
        		File fl = new File(listFile);
        		File fn = new File(narrativeFile);
        		
        		if(fl.exists() || fn.exists())
        			System.err.println("error in generate traing data for mallet document classification: 0618 pm File exist!");
        		
        		BufferedWriter fList = null;
        		BufferedWriter fNarrative = null;	

        		fList = new BufferedWriter(new FileWriter(listFile));
        		fNarrative = new BufferedWriter(new FileWriter(narrativeFile));


        		Parser p = new Parser(knowtatorXmlFile);

        		ArrayList<String> narratives = p.GetListNarrativeSentences("narrative");
        		ArrayList<String> lists = p.GetListNarrativeSentences("list");

        		for(String sent: narratives){
//        			fNarrative.write(knowtatorXmlFile + "\t" + sent + "\n");
        			fNarrative.write(sent + "\n");
        			
        		}
        		for(String sent: lists){
//        			fList.write(knowtatorXmlFile + "\t" + sent + "\n");
        			fList.write(sent + "\n");
        		}

        		fList.close();
        		fNarrative.close();
        	}
        	

        } catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }
        
        System.out.println(kxmlfileList.size());
        
        //generate mallet file
    	File root = new File(Messages.getString("i2b2.mallet.document.train.file.folder"));
    	
    	String arffFile = Messages.getString("i2b2.mallet.ln.classification.arff.file");
    	
    	MalletWekaInterface mwInterface = new MalletWekaInterface();
    	
    	mwInterface.GenerateTrainingARFFfile(root, arffFile);
		
	}

	public static void GenerateListNarrativeSentenceTrainingData(
			String listFile, String narrativeFile, String negativeFile) throws Exception {
		// TODO Auto-generated method stub
		String baseDirectory = KnowtatorXMLDirectory;
		
        RawInput rinput = new RawInput();
        ArrayList<String> kxmlfileList = new ArrayList<String>();
        rinput.getDirectoryFile(baseDirectory, kxmlfileList);
        
        BufferedWriter fList = null;
        BufferedWriter fNarrative = null;
        BufferedWriter fNegative = null;
        try {
        	fList = new BufferedWriter(new FileWriter(listFile));
        	fNarrative = new BufferedWriter(new FileWriter(narrativeFile));
        	fNegative = new BufferedWriter(new FileWriter(negativeFile));

        	for(String knowtatorXmlFile: kxmlfileList)
        	{  	
        		System.out.println(knowtatorXmlFile);
        		Parser p = new Parser(knowtatorXmlFile);
        		
        		ArrayList<String> narratives = p.GetListNarrativeSentences("narrative");
        		ArrayList<String> lists = p.GetListNarrativeSentences("list");
        		ArrayList<String> negatives = p.GetListNarrativeSentences("negative");
        		
        		for(String sent: narratives){
        			fNarrative.write(knowtatorXmlFile + "\t" + sent + "\n");
        		}
        		for(String sent: lists){
        			fList.write(knowtatorXmlFile + "\t" + sent + "\n");
        		}
        		for(String sent: negatives){
        			fNegative.write(knowtatorXmlFile + "\t" + sent + "\n");
        		}
        	}
        	
        	fList.close();
        	fNarrative.close();
        	fNegative.close();
        } catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }
        
        System.out.println(kxmlfileList.size());
		 
	}
	/*
	 * Definition: used to build ARFF training file for medication relationship
	 * 				model.
	 */
	static void buildMedicationRelationshipTrainingData(
		String relationshipTrainfile, boolean addReasonSingleton) throws Exception {
		String baseDirectory = KnowtatorXMLDirectory;
		
        RawInput rinput = new RawInput();
        ArrayList<String> kxmlfileList = new ArrayList<String>();
        rinput.getDirectoryFile(baseDirectory, kxmlfileList);
        
        String head = "";
        String dataSet = "";BufferedWriter fTrain;
    	JMerki jm = new JMerki();
    	
    		jm.initializeParser();
    	
        for(String knowtatorXmlFile: kxmlfileList)
        {  		
        	Parser p = new Parser(knowtatorXmlFile);
        	String rawContent = p.getRawContent();
        	ArrayList<String> listedReason = new ArrayList<String>(); 
        	listedReason = jm.getListedReason(rawContent);
        	HashMap<String,String> data = p.getARFFDataset(listedReason);
        	if(head.isEmpty())
        		head = data.get("head");
        	System.out.println(data.get("data"));
        
        	dataSet += data.get("data");        	
        }
        
        try {
        	fTrain = new BufferedWriter(new FileWriter(relationshipTrainfile));
        	fTrain.write(head + "@data\n" + dataSet);
        	
        	fTrain.close();
        } catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }
        
        System.out.println(kxmlfileList.size());
	}
	/*
	 * Definition: parse the xml files in the Knowtator annotation output folder to build 
	 * 				CRF training file.
	 */
	public void buildCRFi2b2MedicationTrain(String crfTrainingFile, boolean bMakeLowerCase, 
			boolean bLabelProblemList, boolean bAddReasonSingleton) throws Exception {
		String baseDirectory = KnowtatorXMLDirectory;
		
        RawInput rinput = new RawInput();
        ArrayList<String> kxmlfileList = new ArrayList<String>();
        rinput.getDirectoryFile(baseDirectory, kxmlfileList);
        
        BufferedWriter fTrain;
        try {
        	fTrain = new BufferedWriter(new FileWriter(crfTrainingFile));

        	for(String knowtatorXmlFile: kxmlfileList)
        	{  		
        		System.out.println(knowtatorXmlFile);
        		Parser p = new Parser(knowtatorXmlFile);
            	String taggedArticle = p.getTaggedArticleForCRFModel(bMakeLowerCase, bLabelProblemList, bAddReasonSingleton);
            	
            	taggedArticle = taggedArticle.replaceAll("\\|\\|O", "");
        		//        	System.out.print(taggedArticle);
        		fTrain.write(taggedArticle + "\n");
        	}
        	
        	fTrain.close();
        } catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }
        
        System.out.println(kxmlfileList.size());
	}

}
