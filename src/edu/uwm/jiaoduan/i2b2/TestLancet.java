package edu.uwm.jiaoduan.i2b2;

import java.util.ArrayList;

import edu.uwm.jiaoduan.i2b2.utils.LancetParser;

public class TestLancet {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		 try {
			LancetParser l = new LancetParser ("He was continue on Lantus with sliding scale insulin.");
			ArrayList<String> listMeds = l.drugsToi2b2();
			for(String lm: listMeds)
				System.out.println(lm);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

}
