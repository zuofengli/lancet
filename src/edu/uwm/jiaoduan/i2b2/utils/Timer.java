package edu.uwm.jiaoduan.i2b2.utils;

public class Timer {
	long start = 0;
	String name = null;

	public Timer(String name) {
		start = System.currentTimeMillis();
		this.name = name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void start() {
		start = System.currentTimeMillis();
	}

	final public long getTimeSpent() {
		return System.currentTimeMillis() - start;
	}

	public void outTime(java.io.PrintStream out) {
		out.println("Time spent for " + name + ":" + getTimeSpent() + "ms");
	}

}
