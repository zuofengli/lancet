package edu.uwm.jiaoduan.i2b2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import edu.uwm.jiaoduan.Messages;
import edu.uwm.jiaoduan.i2b2.knowtatorparser.Parser;
import edu.uwm.jiaoduan.i2b2.utils.RawInput;

public class ParseKnowtatorXmlToI2b2 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String KnowtatorXMLDirectory = Messages.getString("i2b2.KnowtaorXMLFolder");
		String baseDirectory = KnowtatorXMLDirectory;
		
        ArrayList<String> kxmlfileList = new ArrayList<String>();
        RawInput.getDirectoryFile(baseDirectory, kxmlfileList);
        
        String outFolderName = Messages.getString("JiaoDuan.i2b2.Manual.Annotation.i2b2.format.folder.name");

        	for(String knowtatorXmlFile: kxmlfileList)
        	{  	
        		System.out.println("Parsing " + knowtatorXmlFile);
        		File fKnowtator = new File(knowtatorXmlFile);
         		
        		//get outfile pathname
        		if(!fKnowtator.exists())
        			System.err.println("In ParseKnowtatorToI2b2" + knowtatorXmlFile + " do not exists!");
        		String basePath = null;
				try {
					basePath = fKnowtator.getParentFile().getCanonicalPath();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
							
        		String OutfileFolder = basePath.replaceFirst("i2b2-2009-knowatorXML", outFolderName);       		
        		File fOutFolder = new File(OutfileFolder);
				if(!fOutFolder.exists())
					System.err.println("In ParseKnowtatorToI2b2" + fOutFolder + " do not exists!");
        		
				ConvertKnowtatorXmlToi2b2(knowtatorXmlFile, OutfileFolder);
        	}
        
        System.out.println(kxmlfileList.size());

	}
	static void ConvertKnowtatorXmlToi2b2(String knowtatorXmlFile, String OutfileFolder) {
		Parser p = null;
		try {
			p = new Parser(knowtatorXmlFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(!p.AnnotationToI2b2(OutfileFolder))
			System.out.println("File exists!");
		
	}

	void ConvertKnowtatorXmlToi2b2(String rawfile, String knowtatorXmlFile, String OutfileFolder) {
		Parser p = null;
		try {
			p = new Parser(rawfile, knowtatorXmlFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(!p.AnnotationToI2b2(OutfileFolder))
			System.out.println("File exists!");
		
	}

}
