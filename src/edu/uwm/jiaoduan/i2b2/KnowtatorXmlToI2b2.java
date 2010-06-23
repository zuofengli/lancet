package edu.uwm.jiaoduan.i2b2;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.uwm.jiaoduan.i2b2.knowtatorparser.Parser;
import edu.uwm.jiaoduan.i2b2.utils.RawInput;

public class KnowtatorXmlToI2b2 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Options knowtatorOptions = initLancetOption();
		CommandLineParser cmdParser = new GnuParser();
		CommandLine cmd = null;
		try {
			cmd = cmdParser.parse(knowtatorOptions, args);
		} catch (ParseException e) {
			//			e.printStackTrace();
			System.out.println(e.getMessage());
			usage();
			System.exit(-1);
		}

		if (args.length != 8){
			System.out.println("incomplete options.");
			usage();
			System.exit(-1);
		}

		//		@example: ./i2b2Data/147.lst
		String lstfile = null;
		//		@example ./i2b2Data/147train/
		String rawRoot = null;
		//		@example ./i2b2Data/147knowtatorxml/
		String KnowtatorXMLRoot = null;
		//		@example ./i2b2Data/147GroundTruths/
		String gtRoot = null;

		for(Option p : cmd.getOptions()){
			String opt = p.getOpt();
			if(opt.equals("l")) {
				lstfile = cmd.getOptionValue("l");
			}else if (opt.equals("r")){
				rawRoot = RawInput.
				normalizeFolderPath(cmd.getOptionValue("r"));
			}else if (opt.equals("x")){
				KnowtatorXMLRoot = RawInput.
				normalizeFolderPath(cmd.getOptionValue("x"));
			}else if (opt.equals("o")){
				gtRoot = RawInput.
				normalizeFolderPath(cmd.getOptionValue("o"));
			}
		}

		ArrayList<String> ids = RawInput.getListByEachLine(lstfile , false);
		for(String id: ids){
			String rawfile = rawRoot + id;
			File fraw = new File(rawfile);
			if(!fraw.exists()){
				System.out.println(rawfile + ": do not exist!");
				return;
			}

			String xmlfile = KnowtatorXMLRoot + id + ".knowtator.xml";
			File fxml = new File(xmlfile);
			if(!fxml.exists()){
				System.out.println(xmlfile + ": do not exist");
				return;
			}


			String i2b2file = gtRoot + id + ".i2b2.entries";


			HashMap<String, String>lntypes = new HashMap<String, String>();
			lntypes.put("narrative", "");
			convertKnowtatorXmlToi2b2(rawfile, xmlfile, i2b2file, lntypes);

			System.out.println(RawInput.getFullText(i2b2file));
			//			rin.GetInput();
		}
	}

	private static void usage() {
		System.out.println("KnowtatorXmlToI2b2 -l file_4_raw_file_name -r raw_file_root -x knowtator_xml_output_root -o ground_truth_root");
	}

	private static Options initLancetOption() {
		Options options = new Options();
		options.addOption("l", true, "raw file name list. Without knowtator extension");
		options.addOption("r", true, "root directory for raw files");
		options.addOption("x", true, "Knowtator annotation output in XML format");
		options.addOption("o", true, "root directory for ground truth in i2b2 format");
		return options;
	}
	static void convertKnowtatorXmlToi2b2(String rawfile, String knowtatorXmlFile, String OutfileFolder, HashMap<String,String> lnType) {
		Parser p = null;
		try {
			p = new Parser(rawfile, knowtatorXmlFile);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if(!p.AnnotationToI2b2(OutfileFolder, lnType))
			System.out.println("File exists!");
		
	}

}
