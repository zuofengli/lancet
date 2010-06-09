package edu.uwm.jiaoduan.i2b2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import edu.uwm.jiaoduan.Messages;
import edu.uwm.jiaoduan.i2b2.utils.JMerki;
import edu.uwm.jiaoduan.i2b2.utils.LancetParser;
import edu.uwm.jiaoduan.i2b2.utils.RawInput;
import edu.uwm.jiaoduan.i2b2.utils.Timer;

public class Challenge2009 {

	/**
	 * @param args
	 * @throws Exception 
	 * Definition: This application is used for i2b2 extracting medication challenge 2009
	 * 
	 */
	public static void main(String[] args) throws Exception {
		boolean useRule=args.length>0;
		if(useRule)			
			Messages.setUseRuleBundle(true);
		Timer tm=new Timer("generate data");
		long begin_time = System.currentTimeMillis();
		
		JMerki jm = new JMerki();
		jm.initializeParser(); 
		
		String TwoLevelsDataFolder = Messages.getString("i2b2.challenge.competition.data.folder");
//		String jmerkiOutputFolder =  Messages.getString("i2b2.challenge.system.output.jmerki.folder");
		String crfOutputFolder = Messages.getString("i2b2.challenge.system.output.crf.folder");
		
		System.out.println("Would you like to clear the ouptput folder?");
		String choice = RawInput.getInput();
		if (choice.matches("[y|Y]")){
//			ClearSystemOutputFolder(jmerkiOutputFolder);
			ClearSystemOutputFolder(crfOutputFolder);
			Thread.sleep(5000);
			System.out.println("End of cleaning");
		}
		
		
		HashMap<String, String> dsumfileList = new HashMap<String,String>();
		for(String dsumfile: dsumfileList.keySet())
		{
//			2-811572					
			System.out.println("Parsing " + dsumfile);
			
			String dsumContent = dsumfileList.get(dsumfile);

			System.out.println("==  Extracting drugs  ============================================");	

//			running JMerki
//			String[] topLevel = {"drug","drugClasse"};
//			String[] secondLevel = {"dose", "route", "freq", "prn", "date", "howLong", "reason"};
//			System.out.println("processing:"+dsumfile);
//			List<HashMap<String, String>> drugs = jm.twoLevelParse(dsumContent, topLevel, secondLevel);	
//			ArrayList<String> listedMeds = jm.drugsToi2b2(drugs, dsumContent);
//			//output results
//			String jmerkiFilePathName = GetSystemOutFilePathName(dsumfile, TwoLevelsDataFolder, (useRule)?"jmOutput2":"jmOutput");
//			WriteEntriesToFile(listedMeds, jmerkiFilePathName);
			
//			using CRF MODEL
			System.out.println("using CRF model");
			LancetParser lancet = new LancetParser(dsumfile);
			ArrayList<String> listedMedsCRF = lancet.drugsToi2b2();
			String crfFilePathName = GetSystemOutFilePathName(dsumfile, TwoLevelsDataFolder, (useRule)?"crfOutput2":"crfOutput");
			WriteEntriesToFile(listedMedsCRF, crfFilePathName);
			
//			using SwissKnife CRF MODEL
//			System.out.println("using SWissKnife CRF model");
//			SwissKnifeParser swKnife = new SwissKnifeParser(dsumContent);
//			ArrayList<String> listedMedsCRF = swKnife.drugsToi2b2();
//			//output results
//			String crfFilePathName = GetSystemOutFilePathName(dsumfile, TwoLevelsDataFolder, (useRule)?"crfOutputsw2":"crfOutputsw");
//			WriteEntriesToFile(listedMedsCRF, crfFilePathName);
			
			jm.Reset();
		}
		long end_time = System.currentTimeMillis();
		long time_total = end_time -begin_time;
		
		System.out.println("Analysis over!!!" + time_total/600);
		tm.outTime(System.out);
		
//		testing 
		
//		ChallengeEvaluator evaluator = new ChallengeEvaluator();
//		
//		evaluator.i2b2Challenge2009GoldStandardTest();
//		
//		rin.GetInput();
	}

	private static void WriteEntriesToFile(ArrayList<String> listedMeds,
			String jmerkiFilePathName) {
		BufferedWriter fout = null;

		try {
			fout = new BufferedWriter(new FileWriter(jmerkiFilePathName));
			for(String lm: listedMeds)
				fout.write(lm + "\n");
			fout.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static String GetSystemOutFilePathName(String dsumfile, String twoLevelsDataFolder, String outputfolderName) {
		// 2-811572
		File fdisum = new File(dsumfile);
		String dxFileName = fdisum.getName();
		String[] fields = dxFileName.split("-");
		String fileName = fields[1];
		String folder = twoLevelsDataFolder + fields[0];
//		dsumfile = dsumfile.replace("\\-", "\\");
//		File fdsm = new File(twoLevelsDataFolder + dsumfile);
//		String fileName = fdsm.getName();
//		String folder = fdsm.getPath();
		
		if(!folder.contains("competitionData"))
			System.err.println("Error in Challenge2009: 1205PM");
		
		folder = folder.replace("competitionData", outputfolderName);
		File entryFolder = new File(folder);
		
		if(!entryFolder.exists())
			entryFolder.mkdir();
		
		String entryFile = folder + "/" + fileName + ".i2b2.entries";
		return entryFile;
	}

	private static void ClearSystemOutputFolder(String systemOutputFolder) {
		// TODO Auto-generated method stub
		File folder = new File(systemOutputFolder);
		String fullPath = "";
		try {
			fullPath = folder.getCanonicalPath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		RawInput rin = new RawInput();
		RawInput.delFolder(fullPath);
		
		if(!folder.mkdir())
			System.err.println("Erro in Challenge2009: 1157 am: create folder error.");
		
	}

}
