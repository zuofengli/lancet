package edu.uwm.jiaoduan.i2b2.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.uwm.jiaoduan.Messages;
import edu.uwm.jiaoduan.tools.abner.Tagger;

public class SwissKnifeParser extends LancetParser {

	private Tagger crfTagger = null;
	private String article;
	private static ArrayList<HashMap<String, String>> taglist;

	public void InitialParser(String content) {
		String rModel = Messages
		.getString("i2b2.Relate.Medication.Field.Model");
		loadTagRelationModel(rModel);
		
		File file = new File(Messages.getString("i2b2.CRF.SwissKnife.Parser.CRFModel.FilePath"));
		File fmodel = null;
		try {
			fmodel = new File(file.getCanonicalPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		crfTagger = new Tagger(fmodel);

		article = content.replaceAll("\\|", "\\\\|");
		article = crfTagger.i2b2Tokenize(article);
		crfTagger.setTokenization(true, "i2b2");

		taglist = crfTagger.tagI2B2(article);
	}

	public SwissKnifeParser(String contentOrFilepath) throws Exception {
		super(contentOrFilepath);
		
		String content = super.GetContent();
		InitialParser(content);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RawInput rin = new RawInput();
		String folder251Dxs = Messages.getString("i2b2.2009.251.testing.dx.folder");

		ArrayList<String> fileList = new ArrayList<String>();
		rin.GetDirectoryFile(folder251Dxs, fileList );

		String outputFolder = "./i2b2Data/Challenge2009/251/swissknife251/";

		for(String dxFile: fileList){
			SwissKnifeParser swKnife = null;
			try {
				//			swKnife = new SwissKnifeParser("Aspirin 81mg b.i.d. for two days, xoxyton 100 mg t.i.d.");
				swKnife = new SwissKnifeParser(dxFile);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			File ftest = new File(dxFile);
			String sysoutfile = outputFolder + ftest.getName() + ".i2b2.entries";
			BufferedWriter fSysout;
			try {
				fSysout = new BufferedWriter(new FileWriter(sysoutfile));
				ArrayList<String> lms = swKnife.drugsToi2b2();
				for(String line: lms)
					fSysout.write(line + "\n");

				fSysout.close();
			}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
	
	public ArrayList<String> drugsToi2b2() {
		String FieldSeparator = Messages.getString("i2b2.field.separator");
		ArrayList<String> drugList = new ArrayList<String>();

		for (int i = 0; i < taglist.size(); i++) {
			String type = taglist.get(i).get("type");
			
			Pattern pMedType3 = Pattern.compile("(LISM|NARM|NEGM)");
			Pattern pMedType = Pattern.compile("(LISM|NARM)");
			Matcher mMedType = pMedType.matcher(type);
			
			if (mMedType.find()) {
				
				String swkType = mMedType.group(1);
				
				HashMap<String, String> medication = taglist.get(i);
				medication.put("type", "M");
				medication.put("M", medication.get(swkType));
				medication.put("MOffset", medication.get(swkType + "Offset"));
				

				ArrayList<HashMap<String, String>> fields = GetTagsMinusPlusTwoLineWindow(medication);

				for (HashMap<String, String> field : fields) {
					Matcher mfieldType = pMedType3.matcher(field.get("type"));
					if(mfieldType.find())
						continue;
					
					boolean isRelated = disambiguateFieldsAroundDrugName(medication, field);

					if (isRelated) {
						String fieldType = field.get("type");
						medication.put(fieldType, field.get(fieldType));
						medication.put(fieldType + "Offset", field
								.get(fieldType + "Offset"));
					}
				}
				// System.out.println("end");
				try {
					setListNarrativeForMedication(medication);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				

				String[] tags = Messages.getString("i2b2.CRF.TAGs")
						.toUpperCase().split(",");
				String lm = "";
				for (String entry : tags) {

					if (entry.toLowerCase().equals("ln")) {
						lm += entry.toLowerCase() + "=\""
								+ medication.get(entry) + "\"";
					} else {
						if (medication.get(entry) != null)
							lm += entry.toLowerCase() + "=\""
									+ medication.get(entry) + "\" "
									+ medication.get(entry + "Offset");
						else
							lm += entry.toLowerCase() + "=\"nm\"";

						lm += FieldSeparator;
					}

				}
				drugList.add(lm);
			}
		}

		return drugList;
	}
	

	@Override
	public void setListNarrativeForMedication(HashMap<String, String> medication)
			throws Exception {
		// TODO Auto-generated method stub
		String ln = "";
		for(String key: medication.keySet()){
			if(key.matches("^LISM")){
				ln = "list";
				break;
			}else if(key.matches("^NARM")){
				ln = "narrative";
			}else if(key.matches("^NEGM")){
				ln = "negative";
			}
		}
		
		if(ln.isEmpty())
			System.out.println("Something wrong in swissknifeparsor Set list narrtive");
		
		medication.put("LN", ln);
	}

	@Override
	public ArrayList<HashMap<String, String>> GetTagsMinusPlusTwoLineWindow(
			HashMap<String, String> medication) {
		// TODO Auto-generated method stub
		super.SetTaglist(taglist);
		return super.GetTagsMinusPlusTwoLineWindow(medication);
	}

	public String tagSwissKnifeLabelForCRFModel(ArrayList<String> listMeds) {
		// TODO Auto-generated method stub
		
		String[] tags = Messages.getString("i2b2.CRF.TAGs").split(",");
		String[] swKnifeTags = Messages.getString("i2b2.CRF.Swiss.Knife.TAGs").split(",");
		
		ArrayList<HashMap<String, ArrayList<String>>> goldStdToken = GetGoldStdTokens(listMeds);
		
		String labeledArticle = new String();
		
		String oldTag = "";
		for(int i=0; i< lines.length; i++)
		{
			int lineIndex = i + 1;
			
			String line = lines[i] + "\n";
			String timmedLine = line.trim();
			if(timmedLine.length() < 1)
				continue;
			
			String[] fields = line.split("");
			int tokenIndex = -1;
			Pattern pToken = Pattern.compile("(.\\s+)");
			Matcher fm = pToken.matcher(line);
			int Start = 0;
			
			while(fm.find())
			{
				tokenIndex++;
				String sMatch = fm.group(1);
				int spNumber = sMatch.length() - sMatch.trim().length();
				int end = fm.end() - spNumber;
				String label= null;
				String sTag = GetTagType(goldStdToken, tags, lineIndex, tokenIndex);
				if(!sTag.equals(oldTag) || sTag.equals("O"))
				{
					if(!sTag.equals("O"))
						label = line.substring(Start,end) + "|B-" + sTag + " ";
					else
						label = line.substring(Start,end) + "|" + sTag + " ";
				}else
					label = line.substring(Start,end) + "|I-" + sTag + " ";
				Start = fm.end();
				labeledArticle += label;
				
				oldTag = sTag;
			}

			labeledArticle += "\n";
//			labeledArticle += " ";
		}		
		return labeledArticle;
	}

	protected String GetTagType(ArrayList<HashMap<String, ArrayList<String>>> stdTokens,
			String[] tags, int lineIndex, int tokenIndex) {
		String tag = "";
		String dLocation = lineIndex + "." + tokenIndex;
		String lnType = null;
		for(HashMap<String, ArrayList<String>> medication: stdTokens){
			
			lnType = medication.get("ln").get(0);

			for(String field: medication.keySet()){
				for(String pos: medication.get(field)){
					if(dLocation.equals(pos)){
						tag = field;
						break;
					}
				}
				
				if(!tag.isEmpty())
					break;
			}
			
			if(!tag.isEmpty())
				break;
		}
	
		
		for(String t: tags)
		{
			if(t.equals(tag))
			{
				if(tag.equals("m"))
					tag = lnType.substring(0,3) + tag;
				
				return tag.toUpperCase();
			}
		}
		return "O";
	}
	

}
