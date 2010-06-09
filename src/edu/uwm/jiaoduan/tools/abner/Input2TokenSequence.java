/* ****************************************************************

   Copyright (C) 2004 Burr Settles, University of Wisconsin-Madison,
   Dept. of Computer Sciences and Dept. of Biostatistics and Medical
   Informatics.

   This file is part of the "ABNER (A Biomedical Named Entity
   Recognizer)" system. It requires Java 1.4. This software is
   provided "as is," and the author makes no representations or
   warranties, express or implied. For details, see the "README" file
   included in this distribution.

   This software is provided under the terms of the Common Public
   License, v1.0, as published by http://www.opensource.org. For more
   information, see the "LICENSE" file included in this distribution.

 **************************************************************** */

package edu.uwm.jiaoduan.tools.abner;

import edu.umass.cs.mallet.base.pipe.*;
import edu.umass.cs.mallet.base.types.*;
import edu.uwm.jiaoduan.Messages;
import edu.uwm.jiaoduan.i2b2.crf.FeatureGenerator;
import edu.uwm.jiaoduan.i2b2.utils.FeatureBuilder;
import edu.uwm.jiaoduan.i2b2.utils.RawInput;

import java.util.ArrayList;
import java.util.regex.*;

/**
 * <p>
 * Input2TokenSequence is a text processing Pipe for the MALLET framework. It
 * converts a tokenized sentence string into an input training/tagging sequence
 * for the conditional random fields. Input one sentence per line. for training,
 * '|' separates word|tag:
 * 
 * <pre>
 * IL-2|B-DNA gene|I-DNA expression|O and|O NF-kappa|B-PROTEIN B|I-PROTEIN activation|O ...
 * </pre>
 * 
 * <p>
 * For tagging new sequences, word tokens only will suffice:
 * 
 * <pre>
 * IL-2 gene expression and NF-kappa B activation ...
 * </pre>
 * 
 * @author Burr Settles <a
 *         href="http://www.cs.wisc.edu/~bsettles">bsettles&#64;&
 *         #99;s&#46;&#119;i&#115;&#99;&#46;&#101;d&#117;</a>
 * @version 1.5 (March 2005)
 */

public class Input2TokenSequence extends Pipe {
	
	int lineIndex = 0;
	String oldContext = "BEGIN";
	
	boolean saveSource = true;
	

	boolean doDowncasing = true;
	boolean doWordClass = true;
	boolean doBriefWordClass = true;
	
//	#################################
	
	transient boolean doDigitCollapses = false;
	transient boolean doLexiconLookup = false;
	transient boolean doProblemLexiconLookup = false;
	transient boolean doDrugNameStem = false;
	transient boolean doDxContext = false;
	transient boolean dojMerkiMatch = false;
	transient boolean doStopWordCheck = false; //linux word
	transient boolean dolengthfilter = false;
//	###########################################################
	
	
	transient int lineNum = 0;
	transient boolean doSem = false;
	transient boolean doRule = true;
	private transient static final long serialVersionUID = 2474521989590763426L;
//	transient FeatureGenerator gen;
	transient FeatureBuilder fb = new FeatureBuilder();

	public Input2TokenSequence(boolean cls) {
		super(null, LabelAlphabet.class);
		doWordClass = cls;
		doBriefWordClass = cls;
	}

	public Input2TokenSequence() {
		super(null, LabelAlphabet.class);

	}

	static Pattern recordpat = Pattern.compile("record.*? #(\\d+).*?");

	public Instance pipe(Instance carrier) {
//		for loaded model
		if(fb == null){
			fb = new FeatureBuilder();
			lineIndex = 0;
			oldContext = "BEGIN";
		}
		
//		System.out.println(lineIndex++);
//		RawInput rin = new RawInput();
//		rin.GetInput();
		String sentenceLines = (String) carrier.getData();
		String[] tokens = sentenceLines.trim().split("[\t ]+");
		StringBuffer sentence = new StringBuffer();
		for (String token : tokens) {
			String[] features = token.split("\\|");
			if (features != null && features.length > 0) {

				sentence.append(features[0]).append(" ");
			}
		}

		TokenSequence data = new TokenSequence(tokens.length);
		LabelSequence target = new LabelSequence(
				(LabelAlphabet) getTargetAlphabet(), tokens.length);
		StringBuffer source = saveSource ? new StringBuffer() : null;

		String prevLabel = "NOLABEL";
		String word, tag, label, wc, bwc;
		String originWord = null;
		String[] features;
		lineNum++;//start from 1
		String record = null;
		boolean useCache=false;
//		if (gen == null) {// for serialized pipe, which won't use constructor.
//			String goldFile = Messages.getString("ruleGoldStandardfile");
//			if (!goldFile.startsWith("!")) {
//				gen = new FeatureGenerator(goldFile);
//				doRule = Boolean.parseBoolean(Messages.getString("useRule"));
//				doSem = Boolean.parseBoolean(Messages
//						.getString("useSemanticType"));
//				useCache = Boolean.parseBoolean(Messages
//						.getString("useCace"));
//				
//			}
//		}
//		if (gen != null) {
//			Matcher mat = recordpat.matcher(sentenceLines);
//			if (mat.find()) {
//				gen.init(mat.group(1));
//
//			}
//		}
		for (int i = 0; i < tokens.length; i++) {
			int tokenIndex = i;
			
			
			
			String residue = getResidues(i, tokens);

			if (tokens[i].length() > 0) {
				features = tokens[i].split("\\|");
				if (features.length > 2)
					throw new IllegalStateException("Line \"" + tokens[i]
							+ "\" is formatted badly!");
				word = features[0];
				wc = word;
				bwc = word;
				originWord = word;
				
//				System.out.println(originWord + "\t" + sentence);
//				RawInput rin = new RawInput();
//				rin.GetInput();
				
				if (features.length == 2)
					label = features[1];
				else
					label = "O";
			} else {
				word = "";
				wc = "";
				bwc = "";
				label = "";
			}

			// Transformations
			if (doDigitCollapses) {
				if (word.matches("19\\d\\d"))
					word = "<YEAR>";
				else if (word.matches("19\\d\\ds"))
					word = "<YEARDECADE>";
				else if (word.matches("19\\d\\d-\\d+"))
					word = "<YEARSPAN>";
				else if (word.matches("\\d+\\\\/\\d"))
					word = "<FRACTION>";
				else if (word.matches("\\d[\\d,\\.]*"))
					word = "<DIGITS>";
				else if (word.matches("19\\d\\d-\\d\\d-\\d--d"))
					word = "<DATELINEDATE>";
				else if (word.matches("19\\d\\d-\\d\\d-\\d\\d"))
					word = "<DATELINEDATE>";
				else if (word.matches(".*-led"))
					word = "<LED>";
				else if (word.matches(".*-sponsored"))
					word = "<LED>";
			}

			// do the word class business
			if (doWordClass) {
				wc = wc.replaceAll("[A-Z]", "A");
				wc = wc.replaceAll("[a-z]", "a");
				wc = wc.replaceAll("[0-9]", "0");
				wc = wc.replaceAll("[^A-Za-z0-9]", "x");
			}
			if (doBriefWordClass) {
				bwc = bwc.replaceAll("[A-Z]+", "A");
				bwc = bwc.replaceAll("[a-z]+", "a");
//				System.out.println(bwc);
				bwc = bwc.replaceAll("[0-9]+", "0");
//				System.out.println(bwc);
				bwc = bwc.replaceAll("[^A-Za-z0-9]+", "x");
//				System.out.println(originWord + "-" +bwc);
			}

			Token token = new Token(word);
			if (doDowncasing)
				word = word.toLowerCase();
			token.setFeatureValue("W=" + word, 1);

			if (doWordClass)
				token.setFeatureValue("WC=" + wc, 1);
//			if (gen != null) {
//				if (doRule)
//					token.setFeatureValue("RULE=" + gen.getRuleTag(lineNum, i),
//							1);
//				if (doSem)
//					token.setFeatureValue("SEM="
//							+ gen.getSemType(useCache?null:sentence.toString(), lineNum, word, i),
//							1);
//			}
			if (doBriefWordClass)
				
				token.setFeatureValue("BWC=" + bwc, 1);
			
			if(doProblemLexiconLookup){
				if(fb.isAnEntryOfProblemListLexicon(residue))
					token.setFeatureValue("PROBLEMLISTLEXICON=", 1.0);
			}
			
//			zuofeng code
			if (doLexiconLookup){
				if(fb == null)
					fb = new FeatureBuilder();
				
				boolean bMembership = fb.lookupDrugNameLexicon(residue);
				if(bMembership)
					token.setFeatureValue("DRUGNAMELEXICON=", 1.0);
			}
//			match dosage using jmerki expression
			if(dojMerkiMatch){

			
				String i2b2MedicationField = "DO";
				ArrayList<String> jmerkiTypes = fb.matchI2b2MedicationField(residue);
				for(String type: jmerkiTypes){
					token.setFeatureValue(type + "=", 1);
				}
				
			}
			
			
			if (doDrugNameStem){
				if(fb == null)
					fb = new FeatureBuilder();
//				ArrayList<String> featureKeys = fb.getDrugNameStemFeature(originWord);
				ArrayList<String> featureKeys = fb.getDrugNamePreSuffixFeature(originWord);
				
				if(featureKeys.size()>0){
					for(String featureKey: featureKeys){
						token.setFeatureValue(featureKey, 1);
					}
				}
			}
			
			if(doStopWordCheck){
				if(fb.isACommoEnglishWord(originWord))
					token.setFeatureValue("COMENGWORD=", 1);
			}

			
//			add the discharge summary context
			if(doDxContext){
				if(fb == null)
					fb = new FeatureBuilder();
				String context = fb.getContextBySentence( tokenIndex, oldContext, sentence);
				if(!context.isEmpty())
					token.setFeatureValue("CONTEXT=" + context, 1);
			}
			
			if(dolengthfilter){
				if(originWord != null){
					if(originWord.length() > 2)
						token.setFeatureValue("TOKENLENGTH=", 1);
				}
			}
				
//			end of zuofeng code

			// Append
//			System.out.println(token.getText());
			data.add(token);
			target.add(label);
//			 System.out.print (label + ' ');
			if (saveSource) {
				source.append(token.getText());
				source.append(" ");
				// source.append (bigramLabel); source.append ("\n");
				// source.append (label); source.append ("\n");
			}

		}
		// System.out.println ("");
		carrier.setData(data);
		carrier.setTarget(target);
		if (saveSource)
			carrier.setSource(source);
		return carrier;
	}

	/**
	 * @param tokens of the line
	 * @return connected tokens of the residues of the line
	 */
	private String getResidues(int index, String[] tokens) {
		String residue = "";
		for(int i = index; i < tokens.length; i++){
			String[] features = tokens[i].split("\\|");
			if (features != null && features.length > 0) {

				residue +=features[0] + " ";
			}
		}
		
		return residue.trim();
	}

	/**
	 * 
	 */
	public void resetLineIndex() {
		lineIndex = 0;
		oldContext = "BEGIN";
	}
}
