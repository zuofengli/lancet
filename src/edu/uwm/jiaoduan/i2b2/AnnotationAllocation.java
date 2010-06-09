package edu.uwm.jiaoduan.i2b2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.xml.sax.SAXException;

import edu.uwm.jiaoduan.Messages;
import edu.uwm.jiaoduan.i2b2.knowtatorparser.KnowtatorXmlBuilder;
import edu.uwm.jiaoduan.i2b2.utils.FileUtil;
import edu.uwm.jiaoduan.i2b2.utils.RawInput;

public class AnnotationAllocation {

	/**
	 * @param args
	 */
	ArrayList<String> idList = new ArrayList<String>();
	RawInput rin = new RawInput();
	
	public static void main(String[] args) {
		AnnotationAllocation aa = new AnnotationAllocation();
		ArrayList<String> ids = aa.getIdList();
		RawInput rin = new RawInput();
		int instanceRoot = 0;
		for(String id: ids){
			String srcRoot = "./i2b2Data/Challenge2009/train.test.released.8.17.09/";
			String dstRoot = "./i2b2Data/annotation/AllocatedAnnotation30/30articles/";
			File fScr = new File(srcRoot + id);
			File fDst = new File(dstRoot + id);
			try {
				rin.copyfile(fScr, fDst);
			} catch (IOException e) {
				System.err.println("could not file the file");
				e.printStackTrace();
			}
//		}
		String articleContent = RawInput.getFullText(srcRoot + id);
		//combine the three virtual annotation and output to 30VirtualAnnotation folder
		String vaFolder = "./i2b2Data/annotation/AllocatedAnnotation30/30VirtualAnnotation/";
		String vaPoolFolder = "./i2b2Data/annotation/AllocatedAnnotation30/virtualPool/";
//		for(String id: ids){
			
//			String afile = vaPoolFolder + id + ".v1";
//			String bfile = vaPoolFolder + id + ".v2";
//			String cfile = vaPoolFolder + id + ".v3";
//			
//			String tfile = rin.GetTemporaryFilePath();
//			
//			I2b2Evaluator eval = new I2b2Evaluator();
//			
//			eval.combineFiles(afile, bfile, tfile);
//			
////			combine c file with temporary to destinary file
//			String desfile = vaFolder + id + ".i2b2.entries";
//			eval.combineFiles(tfile, cfile, desfile);
		String sourceFile = vaPoolFolder + id + ".i2b2.entries";
			String desfile = vaFolder + id + ".i2b2.entries";
		try {
			FileUtil.writeFile(desfile, RawInput.getFullText(sourceFile));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
			
			
//			output knowtator xml file
			String vkxFolder = "./i2b2Data/annotation/AllocatedAnnotation30/30virtualKowtatorXML/";
			String desXmlFilePath = vkxFolder + id + ".virtual.knowtator.xml";
			
			ArrayList<String> listedMeds = RawInput.getListByEachLine(desfile, false);
			KnowtatorXmlBuilder kxmlBuilder = null;
			
			try {
				kxmlBuilder = new KnowtatorXmlBuilder(id, articleContent, listedMeds, instanceRoot);
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			System.out.println(kxmlBuilder.GetXMLContent());
			
			try {
				FileUtil.writeFile(desXmlFilePath, kxmlBuilder.getXMLContent());
				instanceRoot = kxmlBuilder.getLastInstanceId();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
	}

	public ArrayList<String> getIdList() {
		return idList;
	}

	public AnnotationAllocation() {
		String infilePath = Messages.getString("i2b2.annoation.allocation.idlist.filepath");
		idList = RawInput.getListByEachLine(infilePath, false);
	}

}
