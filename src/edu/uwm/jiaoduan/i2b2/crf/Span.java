package edu.uwm.jiaoduan.i2b2.crf;


/**
 * 
 */
public class Span implements Comparable<Span> {
	public int startLine;
	public int startToken;
	public int endLine;
	public int endToken;
	public String text;

	/**
	 * 
	 */
	public Span(int startLine, int startTOken, int endLine, int endTOken) {
		this.startLine = startLine;
		this.startToken = startTOken;
		this.endLine = endLine;
		this.endToken = endTOken;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Span o) {
		if (startLine != o.startLine)
			return (startLine < o.startLine) ? -1 : 1;
		else if (startToken != o.startToken)
			return (startToken < o.startToken) ? -1 : 1;
		else if (endLine != o.endLine)
			return (endLine < o.endLine) ? -1 : 1;
		else if (endToken != o.endToken)
			return (endToken < o.endToken) ? -1 : 1;
		else
			return 0;
	}

	public boolean contains(int startLine, int startTOken) {
		if (this.startLine > startLine)
			return false;
		if (this.startToken > startTOken)
			return false;
		if (this.endLine < startLine)
			return false;
		if (this.endToken < startTOken)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Span [endLine=" + endLine + ", endToken=" + endToken
				+ ", startLine=" + startLine + ", startToken=" + startToken
				+ "]";
	}

	public String toI2b2(){
		return "\""+text+"\" "+startLine+":"+startToken+" "+endLine+":"+endToken;
	}
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Span))
			return false;
		return compareTo((Span) obj) == 0;
	}

}