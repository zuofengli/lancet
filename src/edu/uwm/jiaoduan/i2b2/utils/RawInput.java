package edu.uwm.jiaoduan.i2b2.utils;

import java.io.BufferedInputStream;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.Yaml;

import edu.uwm.jiaoduan.Messages;


public class RawInput {

	/** 
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		RawInput.getInput();
		System.out.println("hello");
		RawInput in = new RawInput();
		//		System.out.println(in.getStartTokenIndex(1,2, " i eat apple\n i eat apple again"));
		//		
		String folder = "C:/zuofeng/eclipse/workspace/Jduan/i2b2Data/090601/trainingdata";
		ArrayList<String> filelist = new ArrayList<String>() ;
		//		String folder = "C:/zuofeng/eclipse/workspace/Jduan/lsdb";
		in.getDirectoryFile(folder, filelist );

		String strTree = in.getFullText("1249.tre");

		for(String path: filelist){

		}


		for(String path: filelist){
			File f = new File(path);
			String name = f.getName();
			String folderName = f.getParentFile().getName();
			Pattern pNode = Pattern.compile("([\\(,])" + name + "(:)");
			Matcher mNode = pNode.matcher(strTree);
			while(mNode.find()){
				strTree = strTree.substring(0, mNode.start()) 
				+ mNode.group(1) 
				+ folderName 
				+ mNode.group(2) 
				+ 
				strTree.substring(mNode.end());
			}

		}
		String outfile = "1249New.tre";
		in.createFile(outfile);
		in.writeFile(outfile, strTree);
		in.CloseFile(outfile);
	}
	/**
	 * @param infilePath
	 * @return
	 */
	public static String getFullText(String infilePath ){

		BufferedReader fFile = null;
		String line = new String();
		String content =new String();
		int count = 0;
		try {
			fFile = new BufferedReader(new FileReader(infilePath));
			try {
				while ((line = fFile.readLine()) != null) {
					content += line +"\n";
					count ++;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				fFile.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("In RawInput.ReadFullText Could not open " + infilePath);
		}

		return content;
	}
	public static ArrayList<String> getLines(String infilePath, boolean keepBlankLine ){
		return getListByEachLine(infilePath, keepBlankLine, "");		
	}
	/**
	 * @param infilePath
	 * @param keepBlankLine boolean
	 * @return
	 * 
	 * file without comments.
	 */
	public static ArrayList<String> getListByEachLine(String infilePath, boolean keepBlankLine ){
		return getListByEachLine(infilePath, keepBlankLine, "");		
	}

	public ArrayList<String> getLinesByIngnoringAnnotation(String infilePath, String symbol, boolean keepBlankLine ){
		// TODO Auto-generated method stub
		BufferedReader fFile = null;
		String line = new String();
		//		String content =new String();

		Pattern pAnnotation = Pattern.compile("^" + symbol);


		ArrayList<String> cntList = new ArrayList<String>();
		try {
			fFile = new BufferedReader(new FileReader(infilePath));
			try {
				while ((line = fFile.readLine()) != null) {
					//				content += line +"\n";
					line.trim();
					if(line.length() == 0 && (!keepBlankLine))
						continue;

					Matcher mAnnotation = pAnnotation.matcher(line);
					if(mAnnotation.find())
						continue;

					cntList.add(line);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return cntList;
	}

	/**
	 * @definition get the input from user
	 * 
	 */
	public static String getInput() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		String input = null;

		// read the username from the command-line; need to use try/catch with
		// the
		// readLine() method
		try {
			System.out.print("Please type in here >>");
			input = br.readLine();
		} catch (IOException ioe) {
			System.out.println("IO error trying to get your typing in!"); //$NON-NLS-1$
			System.exit(1);
		}

		return input;
	}
	/**
	 * 
	 * @param dirPath
	 * @param fileList
	 * @return
	 * @reference http://www.exampledepot.com/egs/java.io/GetFiles.html
	 */

	public static Boolean getDirectoryFile(String dirPath, ArrayList<String> fileList) {
		if(!dirPath.endsWith("/"))
		{
			dirPath += "/";
		}

		File dir = new File(dirPath);
		String[] flist = dir.list();

		if(flist == null)
			System.err.println("Could not open the folder or there is no file under it:" + dirPath);

		if(dir == null)
		{
			System.err.println("can not open" + dirPath);
			return false;
		}

		for(int i =0; i<flist.length; i++)
		{
			String name = flist[i]; 
			//			filter .svn/ ./ ../
			if(name.startsWith(".")){
				continue;
			}else{
				String tempname = dirPath + name;
				File subdir = new File(tempname);
				if(subdir.isDirectory() && subdir != null){
					if(!getDirectoryFile(tempname , fileList))
						System.err.println("Read erro!" + tempname );
				}else{
					fileList.add(dirPath + name);
				}
			}
		}		

		return true;
	}

	public boolean IsFileExist(String fileName) {
		// TODO Auto-generated method stub
		File f = new File(fileName);

		if ( f.exists() )
		{
			return true;
		}
		return false;
	}

	public static Object loadYAMLfile(String yamlfile) {
		Yaml yaml = new Yaml();
		String str = new String();

		String bgnAnnot = "^#(.*)";
		String spbgnAnnot = "\\s+#(.*)";
		String blank = "^$";

		Pattern midAnnot = Pattern.compile("(.*)#(.*)");

		String[] lines = RawInput.getTxtResourceContent(yamlfile).split("\n");
		
		for (String line: lines) {

			Matcher ptnMatcher = midAnnot.matcher(line);

			if (line.matches(bgnAnnot))
			{
				continue;
			}else if(line.matches(spbgnAnnot))
			{
				continue;
			}else if(line.matches(blank))
			{
				continue;
			}else if(ptnMatcher.find())
			{

				line = line.replace("#" + ptnMatcher.group(2), "");
			}
			str+=line + "\n";
		}		
		return yaml.load(str);
	}

	//	http://www.blogjava.net/mrcmd/archive/2007/10/12/139003.html
	public static boolean delAllFile(String path) {
		boolean flag = false;
		File file = new File(path);
		if (!file.exists()) {
			return flag;
		}
		if (!file.isDirectory()) {
			return flag;
		}
		String[] tempList = file.list();
		File temp = null;
		for (int i = 0; i < tempList.length; i++) {
			if (path.endsWith(File.separator)) {
				temp = new File(path + tempList[i]);
			} else {
				temp = new File(path + File.separator + tempList[i]);
			}
			if (temp.isFile()) {
				temp.delete();
			}
			if (temp.isDirectory()) {
				delAllFile(path + "/" + tempList[i]);
				delFolder(path + "/" + tempList[i]);
				flag = true;
			}
		}
		return flag;
	}
	//	   http://www.blogjava.net/mrcmd/archive/2007/10/12/139003.html   
	public static void delFolder(String folderPath) {
		try {
			delAllFile(folderPath); 
			String filePath = folderPath;
			filePath = filePath.toString();
			java.io.File myFilePath = new java.io.File(filePath);
			myFilePath.delete(); 
		} catch (Exception e) {
			e.printStackTrace(); 
		}
	}

	public void copyfile(File src, File dst) throws IOException {
		// TODO Auto-generated method stub
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();

	}
	public static String writeTemporaryFile(String content, String prefix, String suffix){
		String outfile = getTemporaryFilePath(prefix, suffix);
		BufferedWriter fout = null;
		try {
			fout = new BufferedWriter(new FileWriter(outfile));
			fout.write(content);
			fout.close();
		} catch (IOException e) {
			System.out.println("Erro 0549pm: in RawInput : create file erro");
			e.printStackTrace();
		}
		return outfile;
	}

	public static String getTemporaryFilePath(String prefix, String suffix){
		File tempFile =  null;
		String tempFilePath = null;
		try {
			tempFile = File.createTempFile(prefix, suffix);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		tempFilePath = tempFile.getAbsolutePath();
		return tempFilePath;
	}
	HashMap<String, BufferedWriter> fileList = new HashMap<String, BufferedWriter>();

	public void createFile(String outfile) {
		BufferedWriter fout = null;
		try {
			fout = new BufferedWriter(new FileWriter(outfile));
		} catch (IOException e) {
			System.out.println("Erro 0549pm: in RawInput : create file erro");
			e.printStackTrace();
		}
		fileList.put(outfile, fout);
	}

	public void writeFile(String outfile, String content){
		if(fileList.containsKey(outfile)){
			try {
				fileList.get(outfile).write(content);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void CloseFile(String outfile){
		if(fileList.containsKey(outfile)){
			try {
				fileList.get(outfile).close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	/**
	 * @definition print out user's prompt firstly.
	 */
	public static String getInput(String prompt) {
		System.out.println(prompt);
		return getInput();
	}
	/*
	 * Definition: Get all of file names under one folder. There many be repeat names
	 * 				in the returned ArrayList
	 * 
	 * Input: String: folder name
	 * 
	 * Output: ArrayList<String>: file name with extension name: 
	 */
	public static ArrayList<String> GetFileNameList(String folder) {
		ArrayList<String> filePathNameList = new ArrayList<String>();
		RawInput.getDirectoryFile(folder, filePathNameList );

		ArrayList<String> nameList = new ArrayList<String>();
		for(String filePathName: filePathNameList){
			File f = new File(filePathName);
			if(!f.exists())
				System.err.println("RawInput: The input file do not exist: Error: 1325PM");
			nameList.add(f.getName());
		}
		return nameList;
	}
	/*
	 * write a line. An extenstion of writeFile function.
	 */
	public void writeLine(String outfile, String lm) {
		writeFile(outfile, lm);
		writeFile(outfile, "\n");
	}
	/*
	 * normalize word for expression
	 */
	public static String normalizeWord(String input) {
		// cDNA Assay)”,
		String word = input;
		Pattern pRex = Pattern.compile("([\\(\\)\\*\\+\\-\\?\\{\\}\\[\\]\\.\\:\\;\\^])");
		Matcher mex = pRex.matcher(word);
		if(mex.find()){
			String[] words = word.split("");
			word = "";
			for(int i =0; i< words.length; i++){
				mex = pRex.matcher(words[i]);
				if(mex.find())
					words[i] = "\\" + words[i];
				word += words[i];
			}
		}
		return word;
	}
	/**
	 * @definition
	 * @source http://www.merriampark.com/ldjava.htm
	 * @param strA
	 * @param strB
	 * @return
	 */
	public static int getLevenshteinDistance (String strA, String strB) {
		if (strA == null || strB == null) {
			throw new IllegalArgumentException("Strings must not be null");
		}

		/*
		    The difference between this impl. and the previous is that, rather 
		     than creating and retaining a matrix of size s.length()+1 by t.length()+1, 
		     we maintain two single-dimensional arrays of length s.length()+1.  The first, d,
		     is the 'current working' distance array that maintains the newest distance cost
		     counts as we iterate through the characters of String s.  Each time we increment
		     the index of String t we are comparing, d is copied to p, the second int[].  Doing so
		     allows us to retain the previous cost counts as required by the algorithm (taking 
		     the minimum of the cost count to the left, up one, and diagonally up and to the left
		     of the current cost count being calculated).  (Note that the arrays aren't really 
		     copied anymore, just switched...this is clearly much better than cloning an array 
		     or doing a System.arraycopy() each time  through the outer loop.)

		     Effectively, the difference between the two implementations is this one does not 
		     cause an out of memory condition when calculating the LD over two very large strings.  		
		 */		

		int n = strA.length(); // length of s
		int m = strB.length(); // length of t

		if (n == 0) {
			return m;
		} else if (m == 0) {
			return n;
		}

		int p[] = new int[n+1]; //'previous' cost array, horizontally
		int d[] = new int[n+1]; // cost array, horizontally
		int _d[]; //placeholder to assist in swapping p and d

		// indexes into strings s and t
		int i; // iterates through s
		int j; // iterates through t

		char t_j; // jth character of t

		int cost; // cost

		for (i = 0; i<=n; i++) {
			p[i] = i;
		}

		for (j = 1; j<=m; j++) {
			t_j = strB.charAt(j-1);
			d[0] = j;

			for (i=1; i<=n; i++) {
				cost = strA.charAt(i-1)==t_j ? 0 : 1;
				// minimum of cell to the left+1, to the top+1, diagonally left and up +cost				
				d[i] = Math.min(Math.min(d[i-1]+1, p[i]+1),  p[i-1]+cost);  
			}

			// copy current distance counts to 'previous row' distance counts
			_d = p;
			p = d;
			d = _d;
		} 

		// our last action in the above loop was to switch d and p, so p now 
		// actually has the most recent cost counts
		return p[n];
	}

	/**
	 * @param start
	 * @param end
	 * @param content 
	 * @return
	 */
	//	public int getStartTokenIndex(int start, int end, String content) {
	//		
	//		int index = -1;
	//		int startTokenIndex = -1;
	//		for (Position p : getPoses(content)){
	//			index++;
	//			if(start == p.start){
	//				startTokenIndex = index;
	//				break;
	//			}
	////			System.out.println(index + "\t" + p.start  + "\t" + p.end);
	//		}
	//		if(startTokenIndex < 0)
	//			System.err.println("rawInput:getStartTokenIndex: no match");
	//		return startTokenIndex;
	//	}

	public static Position[] getPoses(String[] nes, String sen) {
		int curPos = 0;
		ArrayList<Position> pos = new ArrayList<Position>();
		Scanner sc = new Scanner(sen);
		for (String token : nes) {
			//			while(sc.hasNext()){
			//				token = sc.next();
			if (curPos >= sen.length()) {
				curPos = 0;
			}

			String subs = sen.substring(curPos);
			int start = subs.indexOf(token);
			if (start >= 0) {
				start+=curPos;
				int end = start + token.length();
				pos.add(new Position(start, end));
				curPos = end;
			} else{
				// else ignore
				System.err.println("Erro in SentenceParser: ignore:" + token);
			}
			//			}
		}
		return (Position[]) pos.toArray(new Position[pos.size()]);
	}

	/**
	 * @param twoLevelFolder
	 * @return
	 */
	public static ArrayList<String> getSubFolderList(String twoLevelFolder) {
		ArrayList<String> folderList = new ArrayList<String>();

		if(!twoLevelFolder.endsWith("/"))
			twoLevelFolder += "/";

		File dir = new File(twoLevelFolder);
		String[] flist = dir.list();

		if(flist == null  || dir == null){
			System.err.println("Error in RawInput: It is not a two level folder."
					+ twoLevelFolder);
			return folderList;
		}

		for(int i =0; i<flist.length; i++)
		{
			String name = flist[i]; 
			//			filter .svn/ ./ ../
			if(name.startsWith(".")){
				continue;
			}else{
				String tempname = twoLevelFolder + name;
				File subdir = new File(tempname);
				if(subdir.isDirectory() && subdir != null)
					folderList.add(subdir.getAbsolutePath());
			}
		}		

		return folderList;
	}
	/**
	 * @param pieces
	 * @param delimiter
	 * @from http://www.manticmoo.com/articles/jeff/programming/java/join-strings-in-java.php
	 */
	public static String join(String[] pieces, char delimiter){
		return join(pieces, String.valueOf(delimiter));
	}
	public static String join(ArrayList<String> pieces, String delimiter){
		if(pieces==null || pieces.size() == 0 || delimiter == null) {
			return "";
		}

		StringBuffer buf = new StringBuffer();

		for(int i=0; i<pieces.size(); i++)  {
			if(i>0) {
				buf.append(delimiter);
			}
			buf.append(pieces.get(i));
		}

		return buf.toString();
	}

	public static String join(String[] pieces, String delimiter){
		if(pieces==null || pieces.length == 0 || delimiter == null) {
			return "";
		}

		StringBuffer buf = new StringBuffer();

		for(int i=0; i<pieces.length; i++)  {
			if(i>0) {
				buf.append(delimiter);
			}
			buf.append(pieces[i]);
		}

		return buf.toString();
	}
	/**
	 * get source code of the url
	 * @param url
	 * @return
	 * http://java.sun.com/docs/books/tutorial/networking/urls/readingURL.html
	 */
	public static String getWebPage(String url) {

		URL u = null;

		Pattern urlPattern = Pattern.compile("(http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?");
		Matcher urlMatcher = urlPattern.matcher(url);
		if(!urlMatcher.find())
			return null;

		String content = null;
		try {
			u = new URL(url);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedReader in = null;
		InputStream url_st;
		try {
			url_st = u.openStream();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}

		in = new BufferedReader(
				new InputStreamReader(
						url_st)); 

		String inputLine = null;

		try {
			while ((inputLine = in.readLine()) != null)
				content +=inputLine;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return content;
	}
	public static ArrayList<String> getListByEachLine(String infilePath, boolean keepBlankLine, String commentChar) {
		BufferedReader fFile = null;
		String line = new String();
		//		String content =new String();
		Pattern pCmt = null;
		if (!commentChar.isEmpty())
			pCmt = Pattern.compile("^" + commentChar);

		ArrayList<String> cntList = new ArrayList<String>();
		try {
			fFile = new BufferedReader(new FileReader(infilePath));
			try {
				while ((line = fFile.readLine()) != null) {
					//				content += line +"\n";
					line.trim();
					if(line.length() == 0 && (!keepBlankLine))
						continue;

					if (pCmt != null){
						Matcher mCmt = pCmt.matcher(line);
						if (mCmt.find())
							continue;
					}

					cntList.add(line);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return cntList;

	}
	public static String getTemporaryFilePath(InputStream is) {
		return getTemporaryFilePath(is, ".tmp");
	}
	public static void delFile(String filePath){
		File temp = null;
		temp = new File(filePath);
		if (temp.isFile()){
			//			System.out.println("Cleaning: " + filelist.get(i));
			temp.delete();
		}else
			System.out.println("Error in RawInput: delAllFiles");
	}
	public static void delAllFiles(ArrayList<String> filelist) {

		for (int i = 0; i < filelist.size(); i++) {
			delFile(filelist.get(i));
		}

	}
	public static String getSourceCode(InputStream stream) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String script = "";                          
		String line = null;
		try {
			line = reader.readLine();
			while (line != null){ 
				script += line + "\n";
				line = reader.readLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();}

		//		System.out.println(script);
		return script;
	}
	public static String getTxtResourceContent(String rsfilepath){

		URL fileUrl = RawInput.class.getResource(rsfilepath);
		InputStream is = null;
		try {
			is = fileUrl.openStream();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String content = "";
		byte[] buf = new byte[1024];  
		int i;  
		try {
			while((i=is.read(buf))!=-1){   
				content += new String(buf, 0, i);
			}
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return content;
	}
	public static String getTemporaryFilePath(InputStream is,
			String suffix) {
		String tmpfile = RawInput.getTemporaryFilePath("lancet", "." + suffix);

		OutputStream fos = null;
		try {
			fos = new FileOutputStream(tmpfile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}  
		byte[] buf = new byte[1024];  
		int i;  
		try {
			while((i=is.read(buf))!=-1){  
				fos.write(buf, 0, i);  
			}
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//		System.out.println("RawInput: " + tmpfile);
		return tmpfile;
	}
	public static String normalizeFolderPath(String folderPath) {
		if (folderPath.contains("\\")){
			if (!folderPath.endsWith("\\"))
			folderPath += "\\";
		}else if (folderPath.contains("/")){
			if (!folderPath.endsWith("/"))
				folderPath += "/";
		}
		return folderPath;
	}
	
	public  static int countCharInString(String toCount, String inString){
		int lastIndex = 0;
		int count = 0;

		while(lastIndex != -1){

		       lastIndex = inString.indexOf(toCount,lastIndex);

		       if( lastIndex != -1){
		             count ++;
		             lastIndex++;
		      }
		       
		}
		return count;
	}

}
