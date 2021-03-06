package edu.uwm.jiaoduan.acl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.xml.sax.SAXException;

import edu.uwm.jiaoduan.i2b2.knowtatorparser.KnowtatorXmlBuilder;
import edu.uwm.jiaoduan.i2b2.utils.FileUtil;
import edu.uwm.jiaoduan.i2b2.utils.ListedMedication;
import edu.uwm.jiaoduan.i2b2.utils.RawInput;

public class KnowtatorReverseEngineering {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO build Knowtator xml annotation file
		String raw_dir = "C:/Users/Zuofeng/data/adrs/corpus/pitts-dx50/";
		String gt_dir = "C:/Users/Zuofeng/data/auto/lancet1.13/pitts-dx50/";
		String annotator_id = "007";
		String annotator_instantId = "aers51_Instance_170000";
		String kx_dir = "C:/Users/Zuofeng/data/auto/Knowtator/pitts-dx50/";
		
		boolean isFromConceptExtractor = false;
//		//extractConcept
//		gt_dir = "C:/Users/Zuofeng/data/auto/extractConcept/pitts50/extractConcept/concept/";
//		annotator_id = "008";
//		kx_dir = "C:/Users/Zuofeng/data/auto/Knowtator/pitts-dx50-ec/";
//		isFromConceptExtractor = true;
//		////////////////////
		
		//aers51
//		raw_dir = "C:/Users/Zuofeng/data/adrs/corpus/aers51/";
//		gt_dir = "C:/Users/Zuofeng/data/auto/lancet1.13/aers51/";
//		annotator_id = "007";//lancet
//		annotator_instantId = "aers51_Instance_210000";
//		kx_dir = "C:/Users/Zuofeng/data/auto/lancet1.13/knowtator/aers51/";
		
//		//agree
//		raw_dir = "C:/Users/Zuofeng/data/adrs/corpus/aers51/";
//		gt_dir = "C:/Users/Zuofeng/data/auto/annotation/agree/";
//		annotator_instantId = "aers51_Instance_240000";
//		kx_dir = "C:/Users/Zuofeng/data/auto/Knowtator/amiaPoster/agree/";
//		String shortNameForOracle = "agree";
//		String longNameForOracle = "agree annotation between Zufeng and Nadya";
		
////		//diff
		raw_dir = "C:/Users/Zuofeng/data/adrs/corpus/aers51/";
		gt_dir = "C:/Users/Zuofeng/data/auto/annotation/diff/";
		annotator_instantId = "aers51_Instance_250001";
		kx_dir = "C:/Users/Zuofeng/data/auto/Knowtator/amiaPoster/diff/";
		String shortNameForOracle = "diff";
		String longNameForOracle = "diff annotation between Zufeng and Nadya";
		
		
		ArrayList<String> fileList = new ArrayList<String>();
		RawInput.getDirectoryFile(raw_dir, fileList );
		int instanceRoot = 0;
		for(String rawFilePath : fileList){
			File fraw = new File(rawFilePath);
			String sRawFileName = fraw.getName();
			
			String sgtFilePathName = gt_dir + sRawFileName + ".i2b2.entries";
			if (isFromConceptExtractor)
				sgtFilePathName = gt_dir + sRawFileName + ".con";
			
			String kxFilePathName = kx_dir + sRawFileName + ".knowtator.xml";
			
			System.out.println(sRawFileName);
			System.out.println(sgtFilePathName);
			System.out.println(kxFilePathName);
			
			String articleContent = RawInput.getFullText(rawFilePath);
			ArrayList<String> listedMeds = RawInput.getListByEachLine(sgtFilePathName, false);
			if (isFromConceptExtractor)
				listedMeds = ListedMedication.transform(listedMeds);
			
			
			try {
				KnowtatorXmlBuilder kxbuilder = new KnowtatorXmlBuilder(
						sRawFileName,
						articleContent,
						listedMeds,
						instanceRoot,
						shortNameForOracle,
						longNameForOracle,
						annotator_instantId
						);
				try {
					FileUtil.writeFile(kxFilePathName, kxbuilder.getXMLContent());
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				instanceRoot = kxbuilder.getLastInstanceId();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		

	}



}
