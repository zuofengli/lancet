package edu.uwm.jiaoduan.i2b2.utils;

import java.util.Comparator;
import java.util.HashMap;

public class CompareByStartLocation implements Comparator {

	@SuppressWarnings("unchecked")
	@Override
	public int compare(Object o1, Object o2) {
		// TODO Auto-generated method stub
		HashMap<String, String> a = (HashMap<String,String>) o1;
		HashMap<String, String> b = (HashMap<String,String>) o2;
		int aStart = Integer.parseInt(a.get("start"));
		int bStart = Integer.parseInt(b.get("start"));
		
		if(aStart > bStart)
		{
			return 1;
		}else if(aStart < bStart)
		{
			return -1;
		}else
		{
			return 0;
		}
	}

}
