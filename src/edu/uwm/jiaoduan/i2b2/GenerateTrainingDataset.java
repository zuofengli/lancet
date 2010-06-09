package edu.uwm.jiaoduan.i2b2;

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

public class GenerateTrainingDataset {

	/**
	 * @param args
	 */
	private static String KnowtatorXMLDirectory = Messages.getString("i2b2.KnowtaorXMLFolder");
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		ParseSNOMEDCore();
		
		
		GenerateTrainingDataset train = new GenerateTrainingDataset();
		train.TrainI2B2CRFmodel();
	
//		medication field relation model
		String relationshipTrainfile = Messages.getString("i2b2.Medication.Relationship.trainFilePathName");
		GenerateMedicationRelationshipTrainingData(relationshipTrainfile);
		
//		ShaShank NGram ln CLassfiction model
		String listFile = Messages.getString("i2b2.list.sentence.trainingFilePahtName");
		String narrativeFile = Messages.getString("i2b2.narrative.sentence.trainingFilePahtName");
//		
		GenerateListNarrativeSentenceTrainingData(listFile, narrativeFile);
		
//		ln document classlifcication
//		GenerateLNTrainingFileForMalletDocumentClassification();
		GenerateLNTrainingFileForMalletDocumentClassificationBySentence();
		
		File lnRoot = new File(Messages.getString("i2b2.mallet.document.train.file.folder"));
		String lntrain = Messages.getString("i2b2.mallet.ln.classification.arff.file");
		
		MalletWekaInterface mwInterface = new MalletWekaInterface();
		mwInterface.GenerateTrainingARFFfile(lnRoot, lntrain);
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

	private void TrainI2B2CRFmodel() throws Exception {
		// TODO Auto-generated method stub
		String crfTrainingFile = Messages.getString("i2b2.CRFTrainFilePathName");
		GenerateCRFi2b2MedicationTrain(crfTrainingFile);
		
		String[] tags = Messages.getString("i2b2.CRF.TAGs").split(",");
		Trainer t = new Trainer();

		String modelFile = Messages.getString("i2b2.CRFModelFilePathName");
		
//		t.train(crfTrainingFile, modelFile);
		t.train(crfTrainingFile, modelFile, tags);
	}

	private static void GenerateLNTrainingFileForMalletDocumentClassificationBySentence() throws Exception {
		// TODO Auto-generated method stub
		String baseDirectory = KnowtatorXMLDirectory;

		RawInput rinput = new RawInput();
		ArrayList<String> kxmlfileList = new ArrayList<String>();
		rinput.GetDirectoryFile(baseDirectory, kxmlfileList);


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
        rinput.GetDirectoryFile(baseDirectory, kxmlfileList);
        

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

	private static void GenerateListNarrativeSentenceTrainingData(
			String listFile, String narrativeFile) throws Exception {
		// TODO Auto-generated method stub
		String baseDirectory = KnowtatorXMLDirectory;
		
        RawInput rinput = new RawInput();
        ArrayList<String> kxmlfileList = new ArrayList<String>();
        rinput.GetDirectoryFile(baseDirectory, kxmlfileList);
        
        BufferedWriter fList = null;
        BufferedWriter fNarrative = null;
        try {
        	fList = new BufferedWriter(new FileWriter(listFile));
        	fNarrative = new BufferedWriter(new FileWriter(narrativeFile));

        	for(String knowtatorXmlFile: kxmlfileList)
        	{  		
        		Parser p = new Parser(knowtatorXmlFile);
        		
        		ArrayList<String> narratives = p.GetListNarrativeSentences("narrative");
        		ArrayList<String> lists = p.GetListNarrativeSentences("list");
        		
        		for(String sent: narratives){
        			fNarrative.write(knowtatorXmlFile + "\t" + sent + "\n");
        		}
        		for(String sent: lists){
        			fList.write(knowtatorXmlFile + "\t" + sent + "\n");
        		}
        	}
        	
        	fList.close();
        	fNarrative.close();
        } catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }
        
        System.out.println(kxmlfileList.size());
		
	}

	private static void GenerateMedicationRelationshipTrainingData(
			String relationshipTrainfile) throws Exception {
		String baseDirectory = KnowtatorXMLDirectory;
		
        RawInput rinput = new RawInput();
        ArrayList<String> kxmlfileList = new ArrayList<String>();
        rinput.GetDirectoryFile(baseDirectory, kxmlfileList);
        
        String head = "";
        String dataSet = "";
        for(String knowtatorXmlFile: kxmlfileList)
        {  		
        	Parser p = new Parser(knowtatorXmlFile);
        	HashMap<String,String> data = p.GetARFFDataset();
        	if(head.isEmpty())
        		head = data.get("head");
        	
        	dataSet += data.get("data");        	
        }
        BufferedWriter fTrain;
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

	private static void GenerateCRFi2b2MedicationTrain(String crfTrainingFile) throws Exception {
		// TODO Auto-generated method stub
		String baseDirectory = KnowtatorXMLDirectory;
		
        RawInput rinput = new RawInput();
        ArrayList<String> kxmlfileList = new ArrayList<String>();
        rinput.GetDirectoryFile(baseDirectory, kxmlfileList);
        
        BufferedWriter fTrain;
        try {
        	fTrain = new BufferedWriter(new FileWriter(crfTrainingFile));

        	for(String knowtatorXmlFile: kxmlfileList)
        	{  		
        		Parser p = new Parser(knowtatorXmlFile);
            	String taggedArticle = p.GetTaggedArticleForCRFModel();
            	
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
