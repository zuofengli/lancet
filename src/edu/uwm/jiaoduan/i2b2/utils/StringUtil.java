package edu.uwm.jiaoduan.i2b2.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/*
 * string manipulate helper class
 * @author Yong-gang Cao
 */
public class StringUtil {
	public static String quote(String key2) {
		key2 = trimSymbol(key2);
		if (key2.indexOf(' ') > 0)
			return "\"" + key2 + "\"";
		else
			return key2;// donothing
	}

	public static boolean hasSymbol(String tok) {
		for (int i = 0; i < tok.length(); i++) {
			if (!Character.isLetterOrDigit(tok.charAt(i)))
				return true;
			else if (tok.charAt(i) == '-')
				return true;
		}
		return false;
	}

	public static String trimLowerLetter(String phrase) {
		if (phrase != null)
			return phrase.trim().replaceAll("^[a-z]+", "").replaceAll(
					"[a-z]+$", "");
		return null;
	}

	public static boolean onlyContains(String vocabulary, String str) {
		if (str == null || str.length() == 0)
			return false;
		for (int i = 0; i < str.length(); i++) {
			if (vocabulary.indexOf(str.charAt(i)) < 0)
				return false;
		}
		return true;
	}

	

	public static String trimSymbol2(String txt) {

		txt = txt.trim();
		int start = 0, end = txt.length();
		for (int i = 0; i < txt.length(); i++) {
			if (Character.isLetter(txt.charAt(i))
					|| Character.isDigit(txt.charAt(i))) {
				start = i;
				break;
			}
		}
		for (int i = txt.length(); i > 0; i--) {
			if (Character.isLetter(txt.charAt(i - 1))
					|| Character.isDigit(txt.charAt(i - 1))) {
				end = i;
				break;
			}
		}
		return txt.substring(start, end);
	}

	public static int wordCount(String str) {
		if (str == null || str.trim().length() == 0)
			return 0;
		return str.split(" ").length;
	}

	public static String trimNonChar(String str) {
		return Pattern.compile("(^\\W+)|(\\W+$)").matcher(str).replaceAll("");
	}

	public static String quoteList(Collection<String> candidateList) {
		StringBuffer buf = new StringBuffer();
		for (String str : candidateList) {
			buf.append(quote(str)).append(" ");
		}
		return buf.toString();
	}

	public static String connectStrs(String[] strs, String connector) {

		return connectStrs(strs, connector, false);
	}

	public static String connectStrs(String[] strs, String connector,
			boolean usequote) {
		StringBuffer buf = new StringBuffer();
		for (String str : strs) {
			buf.append(usequote ? quote(str) : str).append(connector);
		}
		if (buf.length() > 0)
			buf.delete(buf.length() - connector.length(), buf.length());
		return buf.toString();
	}

	public static boolean isAllCaps(String tok) {
		tok = tok.trim();
		for (int i = 0; i < tok.length(); i++) {
			if (!Character.isUpperCase(tok.charAt(i)))
				return false;
		}
		return true;
	}

	public static boolean isMixed(String tok) {
		tok = tok.trim();
		int letter = 0, digit = 0;
		for (int i = 0; i < tok.length(); i++) {
			if (Character.isDigit(tok.charAt(i)))
				digit++;
			if (Character.isLetter(tok.charAt(i)))
				letter++;
		}
		return letter > 0 && digit > 0;
	}

	public static boolean isDigit(String tok) {
		tok = tok.trim();
		if (tok.length() == 0)
			return false;
		for (int i = 0; i < tok.length(); i++)
			if (!Character.isDigit(tok.charAt(i)))
				return false;
		return true;
	}

	public static boolean hasDigit(String tok) {
		tok = tok.trim();

		for (int i = 0; i < tok.length(); i++)
			if (Character.isDigit(tok.charAt(i)))
				return true;
		return false;
	}

	public static boolean isMutiCaps(String tok) {
		tok = tok.trim();
		int caps = 0;
		for (int i = 0; i < tok.length(); i++) {
			if (Character.isUpperCase(tok.charAt(i)))
				caps++;
		}
		return caps > 1;
	}

	public static boolean hasCaps(String tok) {
		tok = tok.trim();

		for (int i = 0; i < tok.length(); i++) {
			if (Character.isUpperCase(tok.charAt(i)))
				return true;
		}
		return false;
	}

	public static boolean isLexicalEntity(String tok) {
		// multicap or mixed
		tok = tok.trim();
		int caps = 0, letter = 0, digit = 0;
		for (int i = 0; i < tok.length(); i++) {
			if (Character.isUpperCase(tok.charAt(i)))
				caps++;
			if (Character.isDigit(tok.charAt(i)))
				digit++;
			if (Character.isLetter(tok.charAt(i)))
				letter++;
		}
		return caps > 1 || (letter > 0 && digit > 0);
	}

	public static String trimSymbol(String phrase) {

		if (phrase != null)
			return phrase.trim().replaceAll("^[\\W\\-]+", "").replaceAll(
					"[\\W\\-]+$", "");
		return null;
	}

	public static Pattern senSep = Pattern
			.compile("([\\?!]+(\"[?!\\.]{0,1}){0,1})|(\\.[\\r\\n\\t\\f])|(\\s[a-z\\-_]+\\.)|([a-z]{3}\\. )|([\\.?!]\"[?!\\.]{0,1})|(\\w[\\)\\]\"][?!\\.])|(\\w/ )");

	public static String[] getSentences(String cnt) {
		ArrayList<String> sens = new ArrayList<String>();
		Matcher mat = senSep.matcher(cnt);
		int pos = 0;
		while (mat.find()) {
			String str = cnt.substring(pos, mat.end()).trim();
			if (str.length() > 0)
				sens.add(str);
			pos = mat.end();
		}
		if (pos < cnt.length()) {
			String str = cnt.substring(pos).trim();
			if (str.length() > 0)
				sens.add(str);
		}
		String[] res = new String[sens.size()];
		return sens.toArray(res);

	}

	final static String[] months = new String[] { "Jan", "Feb", "Mar", "Apr",
			"May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
	static ArrayList<String> list = new ArrayList<String>(Arrays.asList(months));

	public static int getMonthIndex(String month) {

		int m = list.indexOf(month);
		return (m >= 0) ? m + 1 : m;

	}

	public final static String[] stops = new String[] { "an", "and", "are",
			"as", "at", "be", "but", "by", "for", "if", "in", "into", "is",
			"it", "no", "not", "of", "on", "or", "s", "such", "t", "that",
			"the", "their", "then", "there", "these", "those", "they", "this",
			"to", "was", "will", "with", "i", "you", "he", "she", "do", "does",
			"did", "done", "how", "what", "where", "when", "which", "who",
			"whom", "whose", "some", "any", "somebody", "anything",
			"something", "someone", "anybody", "anyone", "his", "her", "have",
			"has", "a", "should", "would", "can", "could", "else", "about",
			"don", "had", "from", "another" };
	static HashSet<String> stopSet = new HashSet<String>();

	public static boolean isStopWord(String word) {

		initStopSet();
		return stopSet.contains(word);

	}

	private static void initStopSet() {
		synchronized (stopSet) {
			if (stopSet.size() == 0) {
				for (String stop : stops) {
					stopSet.add(stop);
				}
				for(char c='a';c<='z';c++){
					stopSet.add(String.valueOf(c));
				}
			}
		}

	}

	public static boolean isSymbol(String t) {

		for (int i = 0; i < t.length(); i++)
			if (Character.isLetter(t.charAt(i))
					|| Character.isDigit(t.charAt(i)))
				return false;
		return true;
	}

	// static Pattern wdsep = Pattern.compile("[,\\.?!; $]");
	public static Pattern wdpat = Pattern
			.compile("(\\d+,)*\\d+.\\d+%?|([\\p{L}\\p{N}_-]*(\\d,)*[\\p{L}\\p{N}_-]+)");


	

	public static String[] getWordsByPattern(String sen) {

		Matcher mat = StringUtil.wdpat.matcher(sen);
		ArrayList<String> arr1 = new ArrayList<String>();
		while (mat.find()) {
			arr1.add(mat.group().toLowerCase());

		}
		String[] s1 = new String[arr1.size()];

		return arr1.toArray(s1);

	}

	public static String concat(String[] strs) {
		return connectStrs(strs, " ");
	}

	public static String stripHtml(String org) {
		return org.replaceAll(
				"(<script.+?</script>)|(<style.+?</style>)|(<.*?>)", "");
	}

	public static String getSpan(int pos, String[] lines, String line,
			String prefix, String endStr) {
		int start = line.indexOf(prefix);
		StringBuffer strb = new StringBuffer();
		if (start >= 0) {
			int end = line.indexOf(endStr, start + prefix.length());
			if (end < 0) {
				strb.append(line.substring(start + prefix.length()));
				while (pos < lines.length - 1 && end < 0) {
					pos++;
					line = lines[pos];
					end = line.indexOf(endStr);
					if (end < 0)
						strb.append(line);
					else
						strb.append(line.substring(0, end));
				}
			} else {
				strb.append(line.substring(start + prefix.length(), end));
			}
			if (strb.length() > 0)
				return strb.toString();
			else
				return line.substring(start + prefix.length(), end);
		} else
			return null;
	}

	public static String getSpan(BufferedReader reader, String line,
			String prefix, String endStr) throws IOException {

		int start = line.indexOf(prefix);
		StringBuffer strb = new StringBuffer();
		while (start < 0 && line != null) {
			line = reader.readLine();
			start = line.indexOf(prefix);
		}
		if (start >= 0 && line != null) {
			int end = line.indexOf(endStr, start + prefix.length());
			if (end < 0) {
				strb.append(line.substring(start + prefix.length()));
				while ((line = reader.readLine()) != null && end < 0) {

					end = line.indexOf(endStr);
					if (end < 0)
						strb.append(line);
					else
						strb.append(line.substring(0, end));
				}
			} else {
				strb.append(line.substring(start + prefix.length(), end));
			}
			if (strb.length() > 0)
				return strb.toString();
			else if (line != null)
				return line.substring(start + prefix.length(), end);
		}

		return null;
	}

	/**
	 * gets the similarity of the two strings using CosineSimilarity.
	 * 
	 * @param string1
	 * @param string2
	 * @return a value between 0-1 of the similarity
	 */
	public static float getSimilarity(final String string1, final String string2) {
		final List<String> str1Tokens = Arrays
				.asList(getWordsByPattern(string1));
		final List<String> str2Tokens = Arrays
				.asList(getWordsByPattern(string2));

		final Set<String> allTokens = new HashSet<String>();
		allTokens.addAll(str1Tokens);
		final int termsInString1 = allTokens.size();
		final Set<String> secondStringTokens = new HashSet<String>();
		secondStringTokens.addAll(str2Tokens);
		final int termsInString2 = secondStringTokens.size();

		// now combine the sets
		allTokens.addAll(secondStringTokens);
		final int commonTerms = (termsInString1 + termsInString2)
				- allTokens.size();

		// return CosineSimilarity
		return (float) (commonTerms)
				/ (float) (Math.pow((float) termsInString1, 0.5f) * Math.pow(
						(float) termsInString2, 0.5f));
	}



	public static float getMatchingRatio(final String string1,
			final String string2) {
		if (string1.equals(string2))
			return 1;
		int m = string1.length();
		int n = string2.length();
		int minLen = Math.min(m, n);

		int[][] L = new int[m][n];
		float z = 0;

		for (int i = 0; i < m; i++)
			for (int j = 0; j < n; j++) {
				if (string1.charAt(i) == string2.charAt(j)) {
					if (i == 0 || j == 0)
						L[i][j] = 1;
					else
						L[i][j] = L[i - 1][j - 1] + 1;
					if (L[i][j] > z)
						z = L[i][j];

				}
			}
		return 2 * z / (m + n);

	}

	

	

	/**
	 * @param term
	 * @return
	 */
	public static Pattern brackedTerm = Pattern
			.compile("[\\{\\(\\[](.+?)[\\}\\)\\]]");

	//one-three letter and digits are ignored
	public static String[] getBracketedString(String text) {
		Matcher mat = brackedTerm.matcher(text);
		ArrayList<String> res = new ArrayList<String>();
		while (mat.find()) {
			if (mat.end() - mat.start() > 3&&!StringUtil.isDigit(mat.group(1)))
				res.add(mat.group(1));
		}
		return (String[]) res.toArray(new String[res.size()]);
	}

	public static String cleanseTerm(String term) {
		term = term.replaceAll("[ ,]+NOS\\b", "");

		HashSet<String> tmset = new HashSet<String>();

		appendtext(tmset, term);
		String[] bracked = getBracketedString(term);
		for (String brt : bracked) {
			appendtext(tmset, brt);
		}
		bracked = getCommaString(term);
		for (String brt : bracked) {
			appendtext(tmset, brt);
		}

		String tk = StringUtil.stripHtml(term);
		if (tk.length() < term.length()) {
			appendtext(tmset, tk);
		}
		getVariances(tmset, tk);
		String[] tokens = tk.split("&/or");
		if (tokens.length > 1) {
			for (String token : tokens) {
				appendtext(tmset, token);
				token = getVariances(tmset, token);

			}
		}
		StringBuffer tm = new StringBuffer();
		for (String t : tmset) {
			tm.append(t).append("\t");
		}
		return tm.toString();
	}

	/**
	 * @param term
	 * @return comma separated string except for 1,2,3-something
	 */
	public static String[] getCommaString(String term) {
		String[] parts = term.split(",");
		if (parts.length > 1) {
			StringBuffer left = new StringBuffer();
			ArrayList<String> res = new ArrayList<String>();
			for (String part : parts) {
				part = part.trim();
				if (part.length() == 0)
					continue;
				if (part.matches(".*[a-zA-Z]{2,20}$")) {
					res.add(left.toString() + part);
					left.setLength(0);
				} else
					left.append(part).append(",");
			}
			if (res.size() > 0 && left.length() > 0) {
				res.add(left.deleteCharAt(left.length() - 1).toString());
			}
			return (String[]) res.toArray(new String[res.size()]);
		} else
			return new String[0];

	}

	private static String getVariances(HashSet<String> tm, String token) {

		String tk = token.replaceAll("\\(([a-z ]{2,100}?)\\)", "");

		if (tk.length() < token.length()) {
			appendtext(tm, tk);

			token = tk;
		}
		tk = token.replaceAll("\\[([a-z ]{2,100}?)\\]", "");
		if (tk.length() < token.length()) {
			appendtext(tm, tk);
			token = tk;
		}
		tk = StringUtil.trimSymbol2(token);
		if (tk.length() < token.length()) {
			appendtext(tm, tk);
		}
		return tk;
	}

	/**
	 * @param tm
	 * @param tk
	 */
	private static void appendtext(HashSet<String> tm, String tk) {

		tk = tk.trim();
		while (tk.indexOf("  ") > 0) {
			tk = tk.replace("  ", " ");
		}
		if (tk.endsWith(" nos") || tk.endsWith(",nos")) {
			tk = tk.substring(0, tk.length() - 4);
		}
		if (tk.length() > 0)
			tm.add(tk);
	}

}
