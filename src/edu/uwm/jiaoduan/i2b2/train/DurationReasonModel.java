package edu.uwm.jiaoduan.i2b2.train;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;

import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;


import edu.uwm.jiaoduan.Messages;
import edu.uwm.jiaoduan.i2b2.knowtatorparser.Parser;
import edu.uwm.jiaoduan.i2b2.utils.CompareByIndexNameLength;
import edu.uwm.jiaoduan.i2b2.utils.JMerki;
import edu.uwm.jiaoduan.i2b2.utils.ListedMedication;
import edu.uwm.jiaoduan.i2b2.utils.Position;
import edu.uwm.jiaoduan.i2b2.utils.RawInput;
import edu.uwm.jiaoduan.i2b2.utils.SentenceParser;
import edu.uwm.jiaoduan.i2b2.utils.SequenceCheckEntry;

public class DurationReasonModel {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		DurationReasonModel drmodel = new DurationReasonModel();
		try {
			drmodel.GenerateDurationReasonTrain("123.txt");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private static void GenerateDurationReasonTrain(String crfTrainingFile) throws Exception {
		// TODO Auto-generated method stub
		
		JMerki jm = new JMerki();
		try {
			jm.InitializeParser();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		HashMap<String, ArrayList<HashMap<String,String>>>  medDict = jm.GetMediccalTermDictionary();
		
		

		
		
		
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
        		Parser kxmlParser = new Parser(knowtatorXmlFile);
        		ArrayList<String> listMeds = kxmlParser.GetListedMedications();
        		ListedMedication lm = kxmlParser.GetListedMedication();
        		
        		
        		HashMap<String, String> features = new HashMap<String, String>();
        		for(String med: listMeds){
        			lm.GetFeatures(med, features);
        			
        			String sentence = lm.GetSentenceByListedMedication(med);
        			sentence = sentence.replaceAll("\\.", " ");
        			String ln = lm.GetFieldValue("ln", med);
        			if(ln.equals("list"))
        				continue;
        			
        			SentenceParser sp = new SentenceParser();
        			JaroWinkler jw = new JaroWinkler();
        			
        			for (Position p : sp.getPoses(sentence)){
        				SequenceCheckEntry checker = new SequenceCheckEntry();
        				TreeMap<Double, String> scores = new TreeMap<Double, String>();
        				String word = sentence.substring(p.start, p.end);
        				for(String fsw: medDict.keySet()){
        					for(HashMap<String,String> term: medDict.get(fsw)){
        						String termStr = term.get("termName");
        						double score = jw.getSimilarity(word, termStr);
        						if(score > 0.8)
        							System.out.println(termStr + "\t" + score);
        					}
        				}
//        				HashMap<String, String> map = checker.doCheck(word, medDict, "termName");
        			
        				System.out.println(p + "\t" + sentence.substring(p.start, p.end));
        			}
        			
        			String duration = features.get("du");
        			if(duration.equals("nm")){
        				
        			}else{
        				System.out.println("duration :" + duration);
        			}
        			
        			String reason = features.get("r");
        			if(reason.equals("nm")){
        				
        			}else{
        				System.out.println("reason :" + reason);
        			}
        			
        			RawInput rin = new RawInput();
        			rin.GetInput();
        		}
        	
        		//        	System.out.print(taggedArticle);
//        		fTrain.write(taggedArticle + "\n");
        	}
        	
        	fTrain.close();
        } catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }
        
        System.out.println(kxmlfileList.size());
	}


}
