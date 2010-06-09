package edu.uwm.jiaoduan.i2b2;

import java.io.File;
import java.util.ArrayList;

import edu.uwm.jiaoduan.i2b2.knowtatorparser.Parser;
import edu.uwm.jiaoduan.i2b2.utils.ListedMedication;
import edu.uwm.jiaoduan.i2b2.utils.RawInput;

public class GenerateKnowtatorannotation {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String filePath = "C:/zuofeng/eclipse/workspace/Jduan/i2b2Data/i2b2-2009-knowatorXML";
		RawInput rin = new RawInput();
		
		rin.GetFileNameList(filePath);
		ArrayList<String> knowtatorxmllist = rin.GetFileNameList(filePath);
		
		for(String knowxml: knowtatorxmllist){
			File fknowxml = new File(knowxml);
			String id = fknowxml.getName().replaceFirst(".knowtator.xml", "");
			String outfile = "./i2b2Data/147GroundTruths/" + id + ".i2b2.entries";
			rin.CreateFile(outfile);
			
			Parser p = new Parser(knowxml);
			ArrayList<String> listmeds = p.GetListedMedications();
			for(String lm: listmeds){
				rin.WriteFile(outfile, lm);
			}
			
			rin.CloseFile(outfile);
			
		}
		
		try {
			ListedMedication lm = new ListedMedication(filePath);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
