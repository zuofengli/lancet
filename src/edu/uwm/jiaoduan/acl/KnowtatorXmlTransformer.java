package edu.uwm.jiaoduan.acl;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.uwm.jiaoduan.i2b2.utils.RawInput;

public class KnowtatorXmlTransformer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String kx_dir = "I:/uwm/Papers/aers/Annotations/Zuofeng51/xmlAnnts/";
		String tg_dir = "I:/uwm/Papers/aers/Annotations/Zuofeng51/Zuofeng51XmlAnntsTransformed/";
		String annotatorName = "Zuofeng";
		String biasName = "51";
		
		RawInput rin = new RawInput();
		
		//String type = "NULL_CLASS";
		String type = "transform";
		ArrayList<String> fnList = RawInput.GetFileNameList(kx_dir);
		for (String fn : fnList){
			ArrayList<String> lines = RawInput.getListByEachLine(kx_dir + fn, true);
			String OUT = tg_dir + fn;
			rin.createFile(OUT);
			if (type == "NULL_CLASS"){
				
				String nc = RawInput.join(lines, "");
				
				
				//delete the class definition
				nc = nc.replaceAll("<classMention.*?NULL CLASS.*?</classMention>", "");
				
				nc = nc.replaceAll("<complexSlotMention.*?<mentionSlot id=\"(adverse|MedDRA code)\".*?complexSlotMention>", "");
				
				Pattern pMentionId = Pattern.compile("<mention id=\"(.*?)\"");
				
				Matcher ms = pMentionId.matcher(nc);
				while(ms.find()){
					String id = ms.group(1);
					System.out.println(fn);
					//System.out.println(ms.group(1));
				}
				
				rin.writeFile(OUT, nc);
				
				//
				
				
				
			}else{
				for (String line : lines){
					if (annotatorName.equals("Zuofeng")){
						if (line.contains("Nadya Frid, UWM") || line.contains("Zuofeng Li, UW-Milwaukee") ){
							line = "<annotator id=\"aers51_Instance_40000\">Zuofeng Li, uwm</annotator>";
						}
					}
					
					if(annotatorName.equals("Nadya")){
						if (line.contains("Nadya Frid, uwm")){
							line = "<annotator id=\"aers51_Instance_40001\">Nadya Frid, UWM</annotator>";
						}
					}
					
					
					if (!line.contains("<annotator id=")){
						
						line = line.replace("aers51_Instance_", "aers51_" + annotatorName +"_" + biasName + "_" + "_Instance_");
					}
					System.out.println(line);
					rin.writeLine(OUT, line);
				}
			}
			rin.CloseFile(OUT);
		}
		
		
	}

}
