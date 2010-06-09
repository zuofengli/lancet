package edu.uwm.jiaoduan.i2b2.utils;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.uwm.jiaoduan.Messages;



public class FeatureChoice {
	
	HashMap<String, String> medication = null;
	HashMap<String, String> field = null;
	private int iMedStartLine = 0;
	private int iMedStartToken = 0;
	private int iMedEndLine = 0;
	private int iMedEndToken =0 ;
	private int iFieldStartLine =0 ;
	private int iFieldStartToken =0 ;
	private int iFieldEndLine =0 ;
	private int iFieldEndToken =0 ;
/**
 * 
 * @param med {MOffset=47:1 47:1, M=nitroglycerin., type=M}
 * @param entry {MO=sublingual, MOOffset=47:0 47:0, type=MO}
 */
	public FeatureChoice(HashMap<String, String> med,
			HashMap<String, String> entry) {
		
		medication = med;
		field = entry;
		
//		parse append the information from Offset entry
		SetFeatureSet();
		
		if(!medication.containsKey("EndToken"))
			System.err.println("Erro in featureChoice: FeatureChoice 0319 PM");
		
		iMedStartLine = Integer.parseInt(medication.get("StartLine"));
		iMedStartToken = Integer.parseInt(medication.get("StartToken"));
		iMedEndLine = Integer.parseInt(medication.get("EndLine"));
		iMedEndToken = Integer.parseInt(medication.get("EndToken"));
		
		if(!field.containsKey("EndToken"))
			System.err.println("Erro in featureChoice: FeatureChoice 1034 AM");
		
		iFieldStartLine = Integer.parseInt(field.get("StartLine"));
		iFieldStartToken = Integer.parseInt(field.get("StartToken"));
		iFieldEndLine = Integer.parseInt(field.get("EndLine"));
		iFieldEndToken = Integer.parseInt(field.get("EndToken"));
		
	}

	String arffHead = "@relation DATASET_NAME\n";
	String dataset = "";
	private int iNumberOfMedicationBetweenThem  = -1;
	
	public String GetArffHead(String datasetName){
		arffHead = arffHead.replace("DATASET_NAME", datasetName);
		
//		add class attribution
		arffHead += "@attribute Relationship {0,1}\n";
		return arffHead;
	}

	private void SetFeatureSet() {
		// TODO Auto-generated method stub
//		MOffset=2:0 2:0, M=aspirin, type=M
//		Pattern pKeyName = Pattern.compile("(\\w{1,2})TokenPosition");
		Pattern pOffset = Pattern.compile("(\\d+):(\\d+)\\s+(\\d+):(\\d+)");
//		System.out.println(medication.get("MOffset"));
		Matcher mm = pOffset.matcher(medication.get("MOffset"));
		if(mm.find()){
			medication.put("StartLine", mm.group(1));
			medication.put("StartToken", mm.group(2));
			medication.put("EndLine", mm.group(3));
			medication.put("EndToken", mm.group(4));	
			
		}else{
			System.err.println("erro in feature choice 0502pm");
		}
		
		String type = field.get("type");
		Matcher fm = pOffset.matcher(field.get(type + "Offset"));
		if(fm.find()){
			field.put("StartLine", fm.group(1));
			field.put("StartToken", fm.group(2));
			field.put("EndLine", fm.group(3));
			field.put("EndToken", fm.group(4));
		}else{
			System.err.println("erro in feature choice 0502pm");
		}
		
	}

	public String GetDatasetLine(LancetParser listmed) {
		String type = field.get("type");
		
//		String line = "--" + medication.get("StartLine") + " - " + field.get("StartLine");
//		String line = "";
//		line += medication.get("M") +"(" + medication.get("MOffset")+ ")";
//		line += "\t";
//		line += field.get(type) + "(" + field.get(type + "Offset") + ")";
//		line += "\t";
		arffHead += "@attribute IsInSameSentence {0,1}\n";
		dataset += IsInSameSentence(listmed);
		dataset += "\t";
		
		arffHead += "@attribute IsInSameDischargeSection {0,1}\n";
		dataset += IsInSameDischargeSection(listmed);
		dataset += "\t";
		
		arffHead += "@attribute IsThereNumberInFieldToken {0,1}\n";
		dataset += IsThereNumberInFieldToken();
		dataset += "\t";
		
		arffHead += "@attribute TokenBetween numeric\n";
		dataset += Math.abs(HowManyTokensBetween(listmed));
		dataset += "\t";
		
		arffHead += "@attribute IsFieldAfterMedication {0,1}\n";
		dataset += IsFieldAfterMedication();
		dataset += "\t";
		
		arffHead +="@attribute FieldType {1,2,3,4,5}\n";
		dataset += GetFieldTypeId();
		dataset += "\t";
		
		arffHead +="@attribute NumberOfMedicationBetween numeric\n";
		dataset += HowManyMedicationBetween(listmed);
		dataset += "\t";
		
		return dataset;
	}

	private int HowManyMedicationBetween(LancetParser lancet) {
			 iNumberOfMedicationBetweenThem = lancet.getMedicationNumberBetween(medication, field);
		
		return iNumberOfMedicationBetweenThem;
	}

	private int GetFieldTypeId() {
		// i2b2.CRF.TAGs=m,do,mo,f,du,r,ln
		int typeId = 0;
		String[] i2b2Fields = Messages.getString("i2b2.CRF.TAGs").split(",");
		for(int i =0; i < i2b2Fields.length; i++){
			String type = i2b2Fields[i].toUpperCase();
			if(field.get("type").equals(type)){
				typeId = i;
				break;
			}
		}
		
		if(typeId > 5 || typeId < 1)
			System.err.println("Error in FeaturChoice:typeid is not in range");
		
		return typeId;		
	}

	private int IsThereNumberInFieldToken() {
		// TODO Auto-generated method stub
		String type = field.get("type");
		Pattern pNumber = Pattern.compile("([0-9])");
		Matcher mNumber = pNumber.matcher(field.get(type));
		if(mNumber.find()){
			return 1;
		}else{
			return 0;
		}
	}

	private int IsFieldAfterMedication() {
		// TODO Auto-generated method stub
		int miStartline = Integer.parseInt(medication.get("StartLine"));
		int fiStartline = Integer.parseInt(field.get("StartLine"));
		
		if(miStartline == fiStartline){
			int imEnd = Integer.parseInt(medication.get("EndToken"));
			int ifBegin = Integer.parseInt(field.get("StartToken"));
			if(ifBegin > imEnd)
				return 1;
			else
				return 0;
		}else if(miStartline < fiStartline){
			return 1;
		}else{
			return 0;
		}
	}

	private int HowManyTokensBetween(LancetParser listmed) {
		// TODO Auto-generated method stub
		int number = 0;
		
		int miStartline = Integer.parseInt(medication.get("StartLine"));
		int fiStartline = Integer.parseInt(field.get("StartLine"));
		
		if(miStartline == fiStartline){
			int imEnd = Integer.parseInt(medication.get("EndToken"));
			int ifBegin = Integer.parseInt(field.get("StartToken"));
			number = ifBegin -imEnd;
			number = Math.abs(number);
		}else if(miStartline < fiStartline){
			for(int i = miStartline; i <= fiStartline; i++){
				int tokensize  = listmed.GetTokenSizeOfLine(i);
				if(i == miStartline){
					int miEndtoken = Integer.parseInt(medication.get("EndToken"));
					number += (tokensize - miEndtoken);					
				}else if (i == fiStartline){
					int fiBegintoken = Integer.parseInt(field.get("StartToken"));
					number += fiBegintoken;
				}else{
					number += tokensize;
				}
			}
		}else{
			for(int i = fiStartline; i <= miStartline ; i++){
				int tokensize  = listmed.GetTokenSizeOfLine(i);
				if(i == miStartline){
					int miBegintoken = Integer.parseInt(medication.get("StartToken"));
					number += miBegintoken;
				
				}else if (i == fiStartline){
					int fiEndtoken = Integer.parseInt(field.get("EndToken"));
					number += (tokensize - fiEndtoken);	
				}else{
					number += tokensize;
				}
			}
		}
		
		return number;
	}

	private int IsInSameDischargeSection(LancetParser listmed) {
		// TODO Auto-generated method stub
		int iMed = listmed.GetDischargeSection(iMedStartLine, iMedStartToken);
		int iField = listmed.GetDischargeSection(iFieldStartLine, iFieldStartToken);
		
		if(iMed == iField)
			return 1;
		else
			return 0;
	}

	private int IsInSameSentence(LancetParser listmed) {
		String msent = listmed.getSentenceByTokenPosition(iMedStartLine, iMedStartToken, "");
		String lsent = listmed.getSentenceByTokenPosition(iFieldStartLine, iFieldStartToken, "");
		
		if(msent.equals(lsent))
			return 1;
		else
			return 0;
	}

	public void SetNumberOfMedicationBetweenMedicationAndField(int iNumber){
		iNumberOfMedicationBetweenThem = iNumber;
	}
}
