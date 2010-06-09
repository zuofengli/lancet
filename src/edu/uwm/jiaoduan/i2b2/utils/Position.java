/**
 * 
 */
package edu.uwm.jiaoduan.i2b2.utils;

public class Position{
	
	public Position(int start, int end) {
		this.start = start;
		this.end = end;
	}
	public int start;
	public int end;
	@Override
	public String toString() {
		return "Position start=" + start + ",end=" + end + "]" ;
	}
	
}