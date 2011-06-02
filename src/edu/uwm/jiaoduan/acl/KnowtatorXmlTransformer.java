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
		String kx_dir = "C:/Users/Zuofeng/Desktop/AIMA-IAA/annts-4classes/";
		String tg_dir = "C:/Users/Zuofeng/Desktop/AIMA-IAA/transformed/";
		
		RawInput rin = new RawInput();
		
		String type = "NULL_CLASS";
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
					
					System.out.println(ms.group(1));
				}
				
				rin.writeFile(OUT, nc);
				
				//
				System.out.println(fn);
				
				
			}else{
				for (String line : lines){
					if (line.contains("<annotator id=\"aers51_Instance_140121\"")){
						line = line.replace("aers51_Instance_140121", "aers51_Instance_170000");
					}else{
						line = line.replace("aers51_Instance_", "aers51_zuofeng_Instance_");
					}
					System.out.println(line);
					rin.writeLine(OUT, line);
				}
			}
			rin.CloseFile(OUT);
		}
		
		
	}

}
