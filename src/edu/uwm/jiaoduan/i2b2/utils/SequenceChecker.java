package edu.uwm.jiaoduan.i2b2.utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * @author qing
 *	Global Alignment Algorithm
 *	Usage:-pair [word1] [word2]
 *		  -dictionary [dictionary-path] [word]
 * Apr 29, 2009
 */
public class SequenceChecker {
	
	private int g = -2;
						//linear gap penalty
	private int m = 1;
						// match score
	private int mis = -1;
						// mismatch score
	private String sStr;
	private String tStr;
	private int[][] alignTable = null;
						// alignment score for i,j
	private char[] salign = null;
						// alignment result of string s
	private char[] talign = null;
						// alignment result of string t
	private ArrayList<Pair> topTen = new ArrayList<Pair>();
	
	
	public SequenceChecker()
	{
		
	}
	/**
	 * compute alignment score of two string
	 * @param s one string
	 * @param t the other string
	 * @return score
	 */
	public int computeScore(String s, String t)
	{
		int row = s.length()+1;
		int column = t.length()+1;
		int[][] table = new int[row][column];
		int i=0;
		int j=0;
		for(i=0; i<row; i++)
			table[i][0]=i*g;
		for(j=0; j<column; j++)
			table[0][j]=j*g;
		for(i=1; i<row; i++)
			for(j=1; j<column; j++)
			{
				table[i][j] = getMaximumn(table[i-1][j]+g, table[i-1][j-1]+checkCharacter(s.charAt(i-1), t.charAt(j-1)), table[i][j-1]+g);
			}
		sStr = s;
		tStr = t;
		salign = new char[row];
		talign = new char[column];
		alignTable = table;
		return table[i-1][j-1];
	}
	/**
	 * Traceback Algorithm - get the alignment sequence
	 * @param i start row
	 * @param j start column
	 * @param length of the result array
	 * @return available length for the next character
	 */
	public int tracebackSequence(int i, int j, int length)
	{
		if(i==0 && j==0)
			length =0;
		else if(i>0 && alignTable[i][j]==alignTable[i-1][j]+g)
		{
			length = tracebackSequence(i-1, j, length);
			
			salign[length] = sStr.charAt(i-1);
			talign[length] = '-';
			length +=1;
		}
		else if(i>0 && j>0 && alignTable[i][j] == alignTable[i-1][j-1]+checkCharacter(sStr.charAt(i-1), tStr.charAt(j-1)))
		{
			length = tracebackSequence(i-1, j-1, length);
			
			salign[length] = sStr.charAt(i-1);
			talign[length] = tStr.charAt(j-1);
			length+=1;
		}
		else
		{
			length = tracebackSequence(i, j-1, length);
			
			salign[length] = '-';
			talign[length] = tStr.charAt(j-1);
			length+=1;
		}
		return length;
	}
	
	public void checkTopTen(Pair pair)
	{
		int index = 0;
		if(topTen.size()==0)topTen.add(pair);
		else
		for(Pair p: topTen)
		{
			if(pair.score>p.score)
			{
				topTen.add(index, pair);
				if(topTen.size()==11)
					topTen.remove(10);
				break;
			}
			index++;
		}
		
	}
	
	public void lookupDict(String sgl, String path)
	{
		FileInputStream fstream;
		try {
			fstream = new FileInputStream(path);
			DataInputStream in = new DataInputStream(fstream);
	        BufferedReader br = new BufferedReader(new InputStreamReader(in));
	        String strLine;
	        while ((strLine = br.readLine()) != null)
	        {
	        	
	        	Pair pair = new Pair();
	        	pair.score = computeScore(sgl, strLine);
	        	pair.word = strLine;
	        	checkTopTen(pair);
	        }
	        printTopTen(sgl);
	        
	        // best scoring alignment
	        computeScore(sgl, topTen.get(0).word);
	        doTraceback(sgl.length(), topTen.get(0).word.length(), 0);
	        					
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void lookupDict(String sgl, ArrayList<String> list)
	{
		for(String ele : list)
		{
			Pair pair = new Pair();
        	pair.score = computeScore(sgl, ele);
        	pair.word = ele;
        	checkTopTen(pair);
		}
		computeScore(sgl, topTen.get(0).word);
	}
	
	public void doTraceback(int i, int j, int length)
	{
		tracebackSequence(i, j, length);
		System.out.println("\nalignment\n"+(new String(salign)).trim()+"\n"+(new String(talign)).trim());
	}
	
	private class Pair{
		String word;
		int score;
		public String toString()
		{
			String result = new String();
			result+=word+" ("+score+") ";
			return result;
		}
	}
	
	private void printTopTen(String source)
	{
		System.out.println("\nalignment of "+source+":");
		for(Pair p: topTen)
			System.out.println(p.toString());
	}
	
	/**
	 * instead of using a substitution matrix, just compare two characters, get the match score 
	 * @param a
	 * @param b
	 * @return match score
	 * m: match
	 * mis: mismatch
	 */
	public int checkCharacter(int a, int b)
	{
		return (a==b || Math.abs(a-b)==32)? m:mis;
	}
	
	public int getMaximumn(int arg0, int arg1, int arg2)
	{
		return Math.max(Math.max(arg0, arg1),arg2);
	}
	
	public static void main(String[] args)
	{
		String s = "tonite";
		String t = "teh";
		String dict = "C:/courses/CS425/alphaWords.txt";
		SequenceChecker checker = new SequenceChecker();
//		int score = checker.computeScore(s, t);
//		System.out.println(s+"\t"+t+"\nscore="+score);
//		checker.doRecover(s.length(), t.length(), 0);
//		checker.lookupDict(s, dict);
		if(args.length!=3)
			System.out.println("Usage:\n-pair [word1] [word2]\n-dictionary [dictionary-path] [word]");
		else if(args[0].equals("-pair"))
		{
			int score = checker.computeScore(args[1], args[2]);
			checker.doTraceback(args[1].length(), args[2].length(), 0);
			System.out.println(args[1]+"\t"+args[2]+"\nscore="+score);
		}
		else if(args[0].equals("-dictionary"))
		{
			checker.lookupDict(args[2], args[1]);
		}
		
		
	}
}
