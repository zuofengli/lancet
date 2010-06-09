package edu.uwm.jiaoduan.i2b2.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
/*
 * util class for file operation
 * @author Yong-gang Cao
 */
public class FileUtil {

	public static void writeFile(String fileName, String msg)
			throws FileNotFoundException, IOException {
		FileOutputStream fout=new FileOutputStream(fileName);
		fout.write(msg.getBytes());
		fout.close();
	}
	
	public static void appendFile(String fileName, String msg)
	throws FileNotFoundException, IOException {
		FileOutputStream fout=new FileOutputStream(fileName,true);
		fout.write(msg.getBytes());
		fout.close();
	}

	/**
	 * @param args
	 */
	public static String[] readLines(String filename){
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					filename));
			String line = null;
			ArrayList<String> lines=new ArrayList<String>();
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
			reader.close();
			String[] res=new String[lines.size()];
			return lines.toArray(res);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static void createIfNotExist(String dirname) {
		File f=new File(dirname);
		if(!f.exists()){
			f.mkdirs();
		}
		
	}
	
	

}
