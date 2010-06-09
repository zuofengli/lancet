package edu.uwm.jiaoduan.i2b2.utils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class CompareByIndexNameLength implements Comparator {

	String index = "";
	public CompareByIndexNameLength(String indexName) {
		// TODO Auto-generated constructor stub
		index = indexName;
	}

	@Override
	public int compare(Object o1, Object o2) {
		// TODO Auto-generated method stub
		HashMap<String, String> a = (HashMap<String,String>) o1;
		HashMap<String, String> b = (HashMap<String,String>) o2;
		int aLength = a.get(index).length();
		int bLength = b.get(index).length();
		
		if(aLength > bLength)
		{
			return -1;
		}else if(aLength < bLength)
		{
			return 1;
		}else
		{
			return 0;
		}
	}

}
