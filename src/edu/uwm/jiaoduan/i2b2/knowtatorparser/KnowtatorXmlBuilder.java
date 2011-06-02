package edu.uwm.jiaoduan.i2b2.knowtatorparser;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import edu.uwm.jiaoduan.Messages;
import edu.uwm.jiaoduan.i2b2.utils.ListedMedication;
import edu.uwm.jiaoduan.i2b2.utils.RawInput;

public class KnowtatorXmlBuilder {

	private String XMLContent = "";
	private String tempFilepath = null;
	private FileOutputStream fos = null;
	XMLSerializer serializer = null;

	private AttributesImpl atts = new AttributesImpl();
	// SAX2.0 ContentHandler.
	ContentHandler hd = null;
	
	private String annotatorShortName = "ConptExtractor";

	int root_id = 300000;
	int lastArtificialId = 0;
	ListedMedication pListMedication = null;
	private String articleFileName;
	private String annotatorInstanceId;
	private String annotatorLongName;
	/**
	 * 
	 * @param uid
	 * @param articleContent
	 * @param listedMeds
	 * @param instanceIdRoot
	 * 
	 * @throws SAXException
	 */
	public KnowtatorXmlBuilder(String uid, String articleContent, ArrayList<String> listedMeds, int instanceIdRoot, String shortName, String longName , String projectInstanceIdForAnnotator) throws SAXException {
		if(root_id < instanceIdRoot)
			root_id = instanceIdRoot;
		
		annotatorInstanceId = projectInstanceIdForAnnotator;
		
//		if (annotatorId == "007"){
//			annotatorLongName = "Lancet Li, UWM";
//			annotatorShortName = "Lancet";
//			//annotatorInstanceId = "aers51_Instance_40002";
//		}
//		else if (annotatorId == "008"){
//			annotatorLongName = "ConceptExtractor Li, UWM";
//			annotatorShortName = "ConptExtractor";
//			//annotatorInstanceId = "aers51_Instance_40003";
//		}else
//			System.err.println("no such annotator");
		
		annotatorLongName = longName;
		annotatorShortName = shortName;

		articleFileName = uid;
		InitialBuilder();
		SetAnnotationsNode();

		try {
			pListMedication = new ListedMedication(articleContent);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//String[] shortFieldNameList = Messages.getString("i2b2.competition.2009.fields").split(",");
		String[] shortFieldNameList = Messages.getString("jiaoduan.semantic.labels.short").split(",");
		
//		String[] longFieldNameList = Messages.getString("i2b2.competition.2009.fields.full.expression").split(",");
//		String[] longFieldNameList = Messages.getString("i2b2.competition.2009.fields.full.expression.adr").split(",");
		String[] longFieldNameList = Messages.getString("jiaoduan.semantic.lables.long").split(",");
		

		for(String lm: listedMeds){
			System.out.println(lm);
			HashMap<String, String> fields = new HashMap<String, String>();
			ListedMedication.getFeatures(lm, fields);


			String medicationId = "";
			String longMedicationClassName =  "";
			HashMap<String, ArrayList<String>> fieldAnnotationList = new HashMap<String, ArrayList<String>>();
			HashMap<String,String> fieldTypeHasSlotIdMap = new HashMap<String,String>();
			
			boolean hasFakeDrugField = false;
			if ( lm.contains("0:0 0:0"))
				hasFakeDrugField = true;
			
			//			lookthrough all of the fields including m in the medication
			for(int i =0; i < shortFieldNameList.length; i ++){
				String shortFieldName = shortFieldNameList[i];
				String longFieldName = longFieldNameList[i];

				String fieldValue = fields.get(shortFieldName);
				if(fieldValue == null){
					System.out.println(shortFieldName);
					System.out.println(fields);
					continue;
				}
				if(fieldValue.equals("nm"))
					continue;
				if(shortFieldName.equals("ln")){
					//TODO Implement code for list/narrative
				}else{
					String offset = fields.get(shortFieldName + "TokenPosition");
					ArrayList<HashMap<String, Integer>> tokenOffsets = pListMedication.parseOffset(offset);

					for(HashMap<String, Integer> tokenOffset: tokenOffsets){
						
						
						//m="drug" 0:0 0:0
						//skip fake drug field
						if (tokenOffset.get("StartLine") == 0){
//							tokenOffset.put("StartLine", 1);
//							tokenOffset.put("EndLine", 1);
							continue;
						}
						String InstanceId = getKnowtatorArtificialId();
						SetAnnotationNode(InstanceId, tokenOffset);
						if(!shortFieldName.equals("m") && !hasFakeDrugField){
							if(fieldAnnotationList.containsKey(shortFieldName)){
								fieldAnnotationList.get(shortFieldName).add(InstanceId);
							}else{
								ArrayList<String> idList = new ArrayList<String>();
								idList.add(InstanceId);

								fieldAnnotationList.put(shortFieldName, idList);
							}
							SetFieldClassMentionNode(InstanceId, longFieldName);
						}else if(!shortFieldName.equals("m") && hasFakeDrugField){
							SetMedicationClassMentionNode(InstanceId, 
									longFieldName, fieldAnnotationList, fieldTypeHasSlotIdMap);
						}else{
							//TODO need code to cope with drug name with multiple offset
							//current ontology do not support multiple offset.
							medicationId = InstanceId;
							longMedicationClassName = longFieldName;
						}
					}
				}
			}//end of one sub field
			
			if (!hasFakeDrugField)
				SetMedicationClassMentionNode(medicationId, longMedicationClassName, fieldAnnotationList, fieldTypeHasSlotIdMap);
				SetComplexSlotMention(fieldAnnotationList, fieldTypeHasSlotIdMap);
		}//end of one list medication
		EndBuilder();
	}
	private void SetComplexSlotMention(
			HashMap<String, ArrayList<String>> fieldAnnotationList, HashMap<String, String> fieldTypeHasSlotIdMap) {
		for(String type: fieldAnnotationList.keySet()){
			atts.clear();
			atts.addAttribute("", "","id" ,"", fieldTypeHasSlotIdMap.get(type));//"i2b222009_Instance_990001"
			try {
				hd.startElement("", "", "complexSlotMention", atts);
				atts.clear();

				//				<mentionSlot id="m" />
				HashMap<String,String> maps = new HashMap<String,String>();
				maps.put("f", "fr");
				maps.put("do","do");
				maps.put("mo", "manner");
				maps.put("r", "reason");
				maps.put("du", "du");
				maps.put("ossd", "other S/S/D");
				maps.put("ad", "adverse");
				
				String nmType = maps.get(type);
				atts.addAttribute("", "","id" ,"", nmType);
				hd.startElement("", "", "mentionSlot", atts);
				hd.endElement("", "mentionSlotn" , "");
				atts.clear();
				//				
				SetComplexSlotMentionValue(fieldAnnotationList.get(type));


				hd.endElement("", "complexSlotMention" , "");
			} catch (SAXException e) {
				e.printStackTrace();
			}


		}

	}
	private void SetComplexSlotMentionValue(ArrayList<String> ids) {
		for(String id: ids){
			atts.addAttribute("", "","value" ,"", id);
			try {
				hd.startElement("", "", "complexSlotMentionValue", atts);
				atts.clear();
				hd.endElement("", "complexSlotMentionValue" , "");
			} catch (SAXException e) {
				e.printStackTrace();
			}
		}

	}
	/*
	 * @input medication annotation id
	 * @input field belonged to medicaiton and its annotation ids
	 */
	private void SetMedicationClassMentionNode(String medicationId,
			String longMedicationClassName,
			HashMap<String, ArrayList<String>> fieldAnnotationList, HashMap<String, String> fieldTypeHasSlotId) {
		try {
			atts.addAttribute("", "","id" ,"", medicationId);//"i2b222009_Instance_990001"
			hd.startElement("", "", "classMention", atts);
			atts.clear();

			atts.addAttribute("", "", "id", "", longMedicationClassName);
			hd.startElement("", "", "mentionClass", atts);
			hd.characters(longMedicationClassName.toCharArray(), 0, longMedicationClassName.length());
			hd.endElement("","mentionClass" , "");
			atts.clear();

			SetHashSlotMention(fieldAnnotationList, fieldTypeHasSlotId);


			hd.endElement("", "classMention" , "");
			atts.clear();
		} catch (SAXException e) {
			e.printStackTrace();
		}

	}

	private HashMap<String, String> SetHashSlotMention(
			HashMap<String, ArrayList<String>> fieldAnnotationList, HashMap<String, String> fieldTypeHasSlotId) {
		// look through annotation id
		HashMap<String, String> slotMap = new HashMap<String, String>();
		for(String type: fieldAnnotationList.keySet()){

			//			ArrayList<HashMap<String, String>> annotSlotPairList = fieldAnnotationList.get(type);
			//			for(int i = 0; i < annotSlotPairList.size(); i++ ){
			atts.clear();
			String hasSlotMentionId = getKnowtatorArtificialId();
			fieldTypeHasSlotId.put(type, hasSlotMentionId);

			//				add slot mention id to hashtable
			//				HashMap<String,String> AnnotationSlotPair = fieldAnnotationList.get(type).get(i);
			//				String annotionid = AnnotationSlotPair.keySet().toString();
			//				AnnotationSlotPair.put(annotionid, hasSlotMentionId);

			//				fieldAnnotationList.get(type).set(i, AnnotationSlotPair);
			//				end of add slot mention id

			atts.addAttribute("", "", "id", "", hasSlotMentionId);
			try {
				hd.startElement("", "", "hasSlotMention", atts);
				hd.endElement("","hasSlotMention" , "");
				atts.clear();
			} catch (SAXException e) {
				e.printStackTrace();
			}
		}


		return slotMap;
	}

	private String getKnowtatorArtificialId() {
		root_id ++;
		String artificialId = annotatorShortName + "_Instance_"; // "i2b222009_Instance_";
		artificialId += Integer.toString(root_id);

		return artificialId;
	}

	public int getLastInstanceId(){
		return root_id;
	}

	private void SetFieldClassMentionNode(String instanceId, String fieldClassName) {	
		try {
			atts.addAttribute("", "","id" ,"", instanceId);//"i2b222009_Instance_990001"
			hd.startElement("", "", "classMention", atts);
			atts.clear();

			atts.addAttribute("", "", "id", "", fieldClassName);
			hd.startElement("", "", "mentionClass", atts);
			hd.characters(fieldClassName.toCharArray(), 0, fieldClassName.length());
			hd.endElement("","mentionClass" , "");
			hd.endElement("", "classMention" , "");
			atts.clear();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}
	/*
	 * tokenOffset has following features
	 * 		StartLine
	 * 		StartToken
	 * 		EndLine
	 * 		EndToken
	 */
	private void SetAnnotationNode(String instanceId, HashMap<String, Integer> tokenOffset) {
		int iStartLine = tokenOffset.get("StartLine");
		int iStartToken = tokenOffset.get("StartToken");
		int iEndLine = tokenOffset.get("EndLine");
		int iEndToken = tokenOffset.get("EndToken");

		int start = pListMedication.GetArticlePositionByStartToken(iStartLine, iStartToken);
		int end = pListMedication.GetArticlePositionByEndToken(iEndLine, iEndToken);

		try {
			hd.startElement("", "", "annotation", atts);
			{
				SetSubMention(instanceId);
				SetSubAnnotator();

				SetSubSpan(start, end);

				String content = pListMedication.getTokenContent(start, end);
				SetSubSpannedText(content);
				SetSubCreationDate();
			}
			hd.endElement("", "annotation", "");
		} catch (SAXException e) {
			e.printStackTrace();
		}

	}

	private void SetSubCreationDate() {
		//		<creationDate>Thu Aug 20 13:19:55 CDT 2009</creationDate>
		try {
			hd.startElement("","","creationDate",atts);

			//			DateFormat dateFormat = new SimpleDateFormat ("yyyy/MM/dd HH:mm:ss");
			Date date = new java.util.Date ();
			//	        String dateStr = dateFormat.format (date);
			String dateStr = date.toString();

			hd.characters(dateStr.toCharArray(), 0, dateStr.length());
			hd.endElement("","","creationDate");
			atts.clear();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	private void SetSubSpannedText(String content) {
		//		<spannedText>RECORD #501104</spannedText>
		try {
			hd.startElement("","","spannedText",atts);
			hd.characters(content.toCharArray(), 0, content.length());
			hd.endElement("","","spannedText");
			atts.clear();
		} catch (SAXException e) {
			e.printStackTrace();
		}

	}

	private void SetSubSpan(int start, int end) {
		//		<span start="0" end="14" />
		atts.addAttribute("","","start","",Integer.toString(start));
		atts.addAttribute("","","end","",Integer.toString(end));

		try {
			hd.startElement("","","span",atts);
			hd.endElement("","","span");
			atts.clear();
		} catch (SAXException e) {
			e.printStackTrace();
		}

	}

	private void SetSubAnnotator() {
		//String annotatorInstanceId = "aers51_Instance_60000";
		//Todo: annotatorInstanceId should be changed for different projects. 
		//		<annotator id="i2b22009_Instance_90001">Zuofeng Li, UWM Natural Languaguage Processing Group</annotator>
		//      <annotator id="aers51_Instance_10000">Lancet Li, UWM</annotator>
//				<annotator id="aers51_Instance_60000">ConceptExtractor Li, UWM</annotator>
		atts.addAttribute("","","id","",annotatorInstanceId);
		
		//String annotator = "Lancet Li, UWM";
		
		
		try {
			hd.startElement("","","annotator",atts);
			
			hd.characters(annotatorLongName.toCharArray(), 0, annotatorLongName.length());
			hd.endElement("","","span");
			atts.clear();
		} catch (SAXException e) {
			e.printStackTrace();
		}

	}

	private void SetSubMention(String instanceId) {

		atts.addAttribute("","","id","",instanceId);
		try {
			hd.startElement("","","mention",atts);
			hd.endElement("","","mention");
			atts.clear();
		} catch (SAXException e) {
			e.printStackTrace();
		}

	}

	//	<annotations textSource="5\501104">
	private void SetAnnotationsNode() {
		try {
			//			atts.addAttribute("", "textSource", "", "", "5" + "\\" + "501104");
			atts.addAttribute("", "textSource", "", "", articleFileName);
			hd.startElement("","","annotations",atts);
		} catch (SAXException e) {
			e.printStackTrace();
		}
		atts.clear();
	}

	private void EndBuilder() {

		try {
			hd.endElement("","","annotations");
			hd.endDocument();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		try {
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(tempFilepath);
		XMLContent = RawInput.getFullText(tempFilepath);
	}

	private void InitialBuilder() {
		//		root_id = 990000;
		tempFilepath = RawInput.getTemporaryFilePath("KnowtatorXmlBuilder", "xml");

		try {
			fos = new FileOutputStream(tempFilepath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		OutputFormat of = new OutputFormat("XML","UTF-8",true);
		of.setIndent(1);
		of.setIndenting(true);

		serializer = new XMLSerializer(fos,of);

		try {
			hd = serializer.asContentHandler();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			hd.startDocument();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	public String getXMLContent() {
		return ReArrangeXMLContent();
		//		return XMLContent;
	}
	private String ReArrangeXMLContent() {
		String[] lines = XMLContent.split("\n");
		String mXMLContent = lines[0] + "\n";

		Pattern pClassMention = Pattern.compile("<classMention");
		Pattern pComplexSlotMention = Pattern.compile("<complexSlotMention");
		Pattern pEndOfAnnotations = Pattern.compile("</annotations>");
		Pattern pAnnotation = Pattern.compile("<annotation>");

		String ClassMentions = "";
		boolean bBegin = false;
		for(int i = 1; i < lines.length; i ++){
			Matcher mClass = pClassMention.matcher(lines[i]);
			Matcher mComplex = pComplexSlotMention.matcher(lines[i]);

			Matcher mAnnotation = pAnnotation.matcher(lines[i]);
			Matcher mEndOfAnnotations = pEndOfAnnotations.matcher(lines[i]);

			if(mClass.find() || mComplex.find()){
				bBegin = true;
			}

			if(mAnnotation.find()){
				bBegin = false;
			}

			if(bBegin){
				ClassMentions += lines[i] + "\n";
			}else
				mXMLContent += lines[i] + "\n";

			if(mEndOfAnnotations.find()){
				mXMLContent += ClassMentions;
				//				mXMLContent += lines[i] + "\n";
				break;
			}

		}

		return mXMLContent;
	}

}
