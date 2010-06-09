package edu.uwm.jiaoduan.i2b2.train;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import edu.uwm.jiaoduan.Messages;
import edu.uwm.jiaoduan.i2b2.knowtatorparser.Parser;
import edu.uwm.jiaoduan.i2b2.utils.RawInput;
import edu.uwm.jiaoduan.i2b2.utils.SwissKnifeParser;
import edu.uwm.jiaoduan.tools.abner.Tagger;
import edu.uwm.jiaoduan.tools.abner.Trainer;

public class GenerateSwissKnifeCRFModel {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String crfTrainFile = Messages.getString("i2b2.CRF.SwissKnife.Train.FilePath");
		try {
			GenerateCRFi2b2MedicationTrain(crfTrainFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String[] tags = Messages.getString("i2b2.CRF.Swiss.Knife.TAGs").toUpperCase().split(",");
		Trainer trainer = new Trainer();

		String modelFile = Messages.getString("i2b2.CRF.SwissKnife.Parser.CRFModel.FilePath");
		
		trainer.train(crfTrainFile, modelFile, tags);
		
		////tagging testing
		File file = new File(Messages.getString("i2b2.CRF.SwissKnife.Parser.CRFModel.FilePath"));
		File fmodel = null;
		try {
			fmodel = new File(file.getCanonicalPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Tagger t = new Tagger(fmodel);
		
		String s = " He is to continue diuresis" + 
"with Lasix 40 mg p.o. daily for five days and then discontinue" + 
"until his re-evaluation at a follow-up appointment with his PCP" + 
"or cardiologist and 1-2 weeks. Of note , this patient was" + 
"diagnosed for the first time as a type II diabetic during this" + 
"admission with a hemoglobin A1c of 7.2 prior to admission.";
//		String s = "DISCHARGE MEDICATIONS: Medications at the time of discharge\n" +
//		"include aspirin 325 mg p.o. daily , atorvastatin 40 mg p.o. daily ,\n" +
//		"diltiazem 30 mg p.o. t.i.d. , Colace 100 mg p.o. t.i.d. as needed\n" +
//		"for constipation , Zetia 10 mg p.o. daily , fenofibrate 145 mg p.o.\n" +
//		"daily , Lasix 40 mg p.o. daily x5 days , K-Dur 20 mEq p.o. daily x5\n" +
//		"days , metformin 500 mg p.o. daily , Toprol-XL 25 mg p.o. daily ,\n" +
//		"and oxycodone 5 mg p.o. q.4 h. as needed for pain.";
		
		s=s.replaceAll("\\|", "\\\\|");
		
		System.out.println(t.i2b2Tokenize(s));
		s=t.i2b2Tokenize(s);
		t.setTokenization(false, "i2b2");

		String[][] ents = t.getEntities(s);
		for (int i=0; i<ents[0].length; i++) {
		    System.out.println(ents[1][i]+"\t["+ents[0][i]+"]");
		}
		System.out.println();

		System.out.println("################################################################");
		System.out.println("[M]");
		String[] prots = t.getEntities(s,"M");
		for (int i=0; i<prots.length; i++) {
		    System.out.println(prots[i]);
		}
		
	}
	private static void GenerateCRFi2b2MedicationTrain(String crfTrainingFile) throws Exception {
		// TODO Auto-generated method stub
		String KnowtatorXMLDirectory = Messages.getString("i2b2.KnowtaorXMLFolder");
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
//        		SwissKnifeParser swKnife = new SwissKnifeParser(knowtatorXmlFile);
//        		p.GetListedMedications();
//            	String taggedArticle = p.GetTaggedArticleForCRFModel();
        		String taggedArticle = p.GetTaggedArticleForSwissKnifeCRFModel();
            	
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
