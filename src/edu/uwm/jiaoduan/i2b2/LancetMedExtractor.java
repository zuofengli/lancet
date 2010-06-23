package edu.uwm.jiaoduan.i2b2;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import edu.uwm.jiaoduan.Messages;
import edu.uwm.jiaoduan.i2b2.utils.I2b2Evaluator;
import edu.uwm.jiaoduan.i2b2.utils.JMerki;
import edu.uwm.jiaoduan.i2b2.utils.LancetParser;
import edu.uwm.jiaoduan.i2b2.utils.RawInput;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * 
 * @author Zuofeng Li
 *
 * @date Jan 12, 2010
 */

public class LancetMedExtractor {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		checkingEnvironment();
		// create Options object		
		Options lancetOptions = initLancetOption();

		String dxfolder = "";
		String outputfolder = "";
		String dxfile = "";
		String goldfolder = "";
		boolean toShowDemo = false;
		boolean toShowVersion = false;
		String markedfile = "";
		String program = "lancet";
		String conceptfile = null;

		if(args.length == 3){
			goldfolder = args[2];
			if(!goldfolder.endsWith("/"))
				goldfolder += "/";
		}

		CommandLineParser parser = new GnuParser();
		CommandLine cmd = parser.parse(lancetOptions, args);
		for(Option p :  cmd.getOptions()){
			String opt = p.getOpt();
			if(opt.equals("i")) {
				dxfile = cmd.getOptionValue("i");
				System.out.println(dxfile);
			}else if(opt.equals("d")){
				dxfolder = cmd.getOptionValue("d");
				dxfolder = normalizeFolderPathname(dxfolder);
			}else if(opt.equals("o")){
				outputfolder = cmd.getOptionValue("o");
				outputfolder = normalizeFolderPathname(outputfolder);
			}else if(opt.equals("g")){
				goldfolder = cmd.getOptionValue("g");
				goldfolder = normalizeFolderPathname(goldfolder);
			}else if(opt.equals("demo")){
				toShowDemo = true;
			}else if(opt.equals("v")){
				toShowVersion = true;
			}else if(opt.equals("m")){
				markedfile = cmd.getOptionValue("m");
			}else if(opt.equals("c")){
				conceptfile = cmd.getOptionValue("c");
			}else if (opt.equals("p")){
				program = cmd.getOptionValue("p");
			}else{
				usage();
			}
		}
		
		String demoExcerpt = "The patient was placed on heparin instead of Coumadin for Chronicle device lead thrombus with a PPT goal of 60-80.";
		
		if (toShowVersion){
			System.out.println(Messages.getString("jd.lancetMedExtractor.lastest.version"));
		}

		if (toShowDemo){
			demo(demoExcerpt);
			System.out.println(demoExcerpt);
			System.exit(0);
		}

		if(dxfolder.isEmpty() && dxfile.isEmpty())
			usage();



		//begin extraction
		File sysFolder = new File(outputfolder);
		if(!sysFolder.exists())
			sysFolder.mkdir();		

		RawInput rin = new RawInput();

		ArrayList<String> fileList = new ArrayList<String>();
		
		if (dxfolder.isEmpty())
			fileList.add(dxfile);
		else
			RawInput.getDirectoryFile(dxfolder, fileList);

		int count =0;
		boolean isNormalExist = false;
		
		
		for(String file: fileList){
			isNormalExist = false;
			
			File fin = new File(file);
			
			System.out.println("PARSING:\t" + file);
			String dxContent = RawInput.getFullText(file);

			String outfile = outputfolder + fin.getName() + ".i2b2.entries";
			LancetParser lancet  = new LancetParser(dxContent);
			lancet.summary();

			ArrayList<String> listMeds = null;
			try {
				listMeds = lancet.drugsToi2b2();
				//					listMeds = sk.drugsToi2b2();
			} catch (Exception e) {
				e.printStackTrace();
			}

			rin .createFile(outfile);
			System.out.println("I2B2 ENTRIES:");
			for(String lm: listMeds){
				rin.writeLine(outfile, lm);
				System.out.println(lm);
			}
			rin.CloseFile(outfile);
			
			if (!markedfile.isEmpty() && !dxfile.isEmpty()){
				String[] maskFields ={"do","mo","f","du"};
				String marskedContent = lancet.getMaskedContent(listMeds, maskFields );
				JMerki jm = new JMerki();
				try {
					JMerki.setLoadDrugNameLexicon(false);
					jm.initializeParser();
				} catch (IOException e) {
					e.printStackTrace();
				}
				ArrayList<String> problemList = new ArrayList<String>();
				marskedContent = jm.markMedicationFieldsInArticle(marskedContent, problemList );
				rin.createFile(markedfile);
				rin.writeFile(markedfile, marskedContent);
				rin.CloseFile(markedfile);
				
				if (conceptfile != null){
					rin.createFile(conceptfile);
					for(String concept: problemList){
						rin.writeLine(conceptfile, concept);
					}
					rin.CloseFile(conceptfile);
				}else{
					for(String concept: problemList){
						System.out.println(concept);
					}
				}
				
			}

			System.out.println("FINISH DISX: " + ++count);
			
			isNormalExist = true;
			lancet.clean();
		}

		//	
		if(!goldfolder.isEmpty()){
			if(isNormalExist){
				System.out.println("EVALUATION:");
				I2b2Evaluator eval = new I2b2Evaluator();
				eval.test(goldfolder, outputfolder, System.out);
			}
		}
		

		
	}

	private static void checkingEnvironment() {

		String jython_path = System.getenv("JYTHON_HOME");//JYTHON_HOME
		if(jython_path == null){
			System.out.println("Please install jython and set an enviroment variable named JYTHON_HOME.");
		}

	}

	private static void demo(String excerpt) throws Exception {
		LancetParser lancet  = new LancetParser(excerpt);
		lancet.summary();

		ArrayList<String> listMeds = null;
		try {
			listMeds = lancet.drugsToi2b2();
			//					listMeds = sk.drugsToi2b2();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("I2B2 ENTRIES:");
		for(String lm: listMeds){
			System.out.println(lm);
		}

	}
	/**
	 * add '/' at the end of the folder path name.
	 */
	private static String normalizeFolderPathname(String folderPathname) {
		if(!folderPathname.endsWith("/"))
			folderPathname += "/";
		return folderPathname;
	}

	private static void usage() {
		System.out.println("java -Xmx250m  -jar lancetMedExtractor.jar  --demo");
		System.out.println("lancetMedExtractor.jar -d dx_folder -o output_folder [-g gold_folder] ");
		System.out.println("lancetMedExtractor.jar -i dx_file -o output_folder [-m maskedfile]");
		System.exit(0);		
	}

	private static Options initLancetOption() {
		Options options = new Options();

		options.addOption("d", true, "Discharege summary folder");
		options.addOption("dxsm", true, "Discharege summary folder");
		
		options.addOption("i", true, "Single dx summary file");
		options.addOption("infile", true, "Single dx summary file");
		
		options.addOption("o", true, "Output folder");
		options.addOption("outfolder", true, "Output folder");
		
		options.addOption("g", true, "Folder for gold standard files");		
		options.addOption("gold", true, "Folder for gold standard files");
		
		options.addOption("p", true, "[lancet|jmerki|hybrid]Program: lancet core, jMerki and hybrid");		
		options.addOption("program", true, "[lancet|jmerki|hybrid] Program: lancet core, jMerki and hybrid");
		
		options.addOption("demo", false, "Folder for gold standard files");
		options.addOption("v", false, "Version information");
		options.addOption("m", true, "Mark i2b2 field with M. This option should be used with infile option");
		options.addOption("c", true, "c: concept: Problem concept file. This option should be used with M");
		return options;
	}

}
