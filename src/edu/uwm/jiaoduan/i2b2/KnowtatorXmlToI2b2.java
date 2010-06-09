package edu.uwm.jiaoduan.i2b2;

import java.io.File;
import java.util.ArrayList;

import edu.uwm.jiaoduan.i2b2.utils.RawInput;

public class KnowtatorXmlToI2b2 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String lstfile = "./i2b2Data/147.lst";
		ArrayList<String> ids = RawInput.getListByEachLine(lstfile , false);
		
		String rawRoot = "./i2b2Data/147train/";
		String UWMYuRoot = "./i2b2Data/147GroundTruths/";
		String KnowtatorXMLRoot = "./i2b2Data/147knowtatorxml/";
		
		for(String id: ids){
			ParseKnowtatorXmlToI2b2 parser = new ParseKnowtatorXmlToI2b2();
//			RawInput rin = new RawInput();

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


			String i2b2file = UWMYuRoot + id + ".i2b2.entries";



			parser.ConvertKnowtatorXmlToi2b2(rawfile, xmlfile, i2b2file);

			System.out.println(RawInput.getFullText(i2b2file));
//			rin.GetInput();
		}
	}

}
