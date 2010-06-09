package edu.uwm.jiaoduan.i2b2.train;

import edu.uwm.jiaoduan.Messages;

public class NegativeMedicationTrain {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
//		ShaShank NGram ln CLassfiction model
		String listFile = Messages.getString("i2b2.list.sentence.trainingFilePahtName");
		String narrativeFile = Messages.getString("i2b2.narrative.sentence.trainingFilePahtName");
		String negativeFile = Messages.getString("i2b2.negative.medication.sentence.trainingFilePahtName");
//		
		GenerateTrainingDataset train = new GenerateTrainingDataset();
		train.GenerateListNarrativeSentenceTrainingData(listFile, narrativeFile, negativeFile);
		System.out.println("negative medication training data is completed");

	}
	
	

}
