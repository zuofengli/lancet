package edu.uwm.jiaoduan.i2b2.utils;

/**
 * 
 */


import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author qing
 *
 * Aug 17, 2009
 */
public class SequenceCheckEntry {
	
	private float highestScore = 0 ;
	private boolean flag = true;
	private HashMap highestMap = null;
	private SequenceChecker checker = new SequenceChecker();
	
	public SequenceCheckEntry()
	{
		
	}
	
	public HashMap doCheck(String word, HashMap<String, ArrayList<HashMap<String, String>>> dic, String indexName)
	{
		ArrayList<HashMap<String, String>> list = dic.get(word);
		String target = null;
		float score;
		if(list == null)
			return null;
		else
		{
			for(HashMap map : list)
			{
				target = (String)map.get(indexName);
				if(target!=null)
				{
					score = checker.computeScore(word, target);
					if(flag)
					{
						highestScore = score;
						highestMap = map;
					}
					else
					{
						if(highestScore < score)
						{
							highestScore = score;
							highestMap = map;
						}
					}
				}
				else
					continue;
			}
			return highestMap;
		}
	}

}
