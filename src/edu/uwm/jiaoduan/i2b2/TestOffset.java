package edu.uwm.jiaoduan.i2b2;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.uwm.jiaoduan.i2b2.utils.FileUtil;
import edu.uwm.jiaoduan.i2b2.utils.ListedMedication;

public class TestOffset {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String goldStandardFile = args[0];
		String txtFile = args[1];
		try {
			ListedMedication lm = new ListedMedication(txtFile);
			String[] lns = FileUtil.readLines(goldStandardFile);
			for (String ln : lns) {
				HashMap<String, String> parseRlt = new HashMap<String, String>();
				ListedMedication.getFeatures(ln, parseRlt);
				for (String k : parseRlt.keySet()) {
					if(!k.endsWith("TokenPosition"))
						continue;
					System.out.println(k);
					String mOffset = parseRlt.get(k);
					Pattern pOffset = Pattern
					.compile("(\\d+):(\\d+)\\s+(\\d+):(\\d+)");
					Matcher mOff = pOffset.matcher(mOffset);
					if (mOff.find()) {
						int startLine = Integer.parseInt(mOff.group(1));
						int startTOken = Integer.parseInt(mOff.group(2));
						int endLine = Integer.parseInt(mOff.group(3));
						int endTOken = Integer.parseInt(mOff.group(4));
						int startPos = lm.GetArticlePositionByStartToken(
								startLine, startTOken);
						int endPos = lm.GetArticlePositionByEndToken(endLine,
								endTOken);
						String offset = lm.getTokenOffset(startPos, endPos);
						if (!offset.equals(mOff.group(0)))
							System.out.println(offset + "\t" + mOff.group(0));
						else
							System.out.println("matched");

					}
				}

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
