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

package edu.uwm.jiaoduan.i2b2.crf;

import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.regex.*;

import cc.mallet.fst.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.types.*;
import edu.umass.cs.mallet.base.pipe.iterator.LineGroupIterator;
import edu.uwm.jiaoduan.Messages;
import edu.uwm.jiaoduan.i2b2.utils.RawInput;
import edu.uwm.jiaoduan.tools.abner.Tagger;

/**
   <p>This is the interface to the CRF that does named entity
   tagging. It contains methods for taking input text and returning
   tagged results in a variety of formats.

   <p>By default, the all methods in the Tagger class use ABNER's
   built-in tokenization. A single newline, e.g. <tt>'\n'</tt>, is
   treated as a space, but two or more will conserve a paragraph
   break. You may also disable it and use your own pre-tokenized text
   if you prefer, though tokens must be whitespace-delimited, with
   newlines separating sentences.

   @author Burr Settles <a href="http://www.cs.wisc.edu/~bsettles">bsettles&#64;&#99;s&#46;&#119;i&#115;&#99;&#46;&#101;d&#117;</a> 
   @version 1.5 (March 2005)
*/
public class CRFTagger {

    // constants

    /** The tagger trained on the NLPBA corpus. */
    public static final int NLPBA = 0;
    /** The tagger trained on the BioCreative corpus. */
    public static final int BIOCREATIVE = 1;
    /** Indicates a tagger for some externally-trained model. */
    public static final int EXTERNAL = 2;

    // very important: the CRF itself and its feature pipes!!
    private CRF myCRF;
    private Pipe myPipe;
    private boolean doTokenization = true;
    private int myMode;
	private String tokenizer = "default";
	private int I2B22009 = 2;
	private int I2B22010 = 3;
	public static void main(String[] args) throws Exception {
		double[] varGaussianPrior = new double[]{0, 0.1, 0.5, 1 , 10,100};
		
//		Tagger t = new Tagger(new File("./crf.model" + "." + Double.toString(var) + ".crf"));
		String modelFile = Messages.getString("adr.crf.file");
		CRFTagger t = new CRFTagger(new File(modelFile));
		t.setTokenization(true, "i2b2");
		String rlt = t.tagSGML("otrin 225 mg q d , vancomycin 1250 mg q 24");
		RawInput.getInput(rlt);
	}

    ////////////////////////////////////////////////////////////////
    private  void initialize(ObjectInputStream ois) throws Exception {
	// load the CRF into memory and get ready to go...
	myCRF = (CRF) ois.readObject();
	myPipe = myCRF.getInputPipe();
    }

    /**
       Basic Constructor: Loads the "NLPBA" model by default.
    */
    public CRFTagger() {
	this(NLPBA);
    }

    /**
       Advanced constructor: Specify either "NLPBA" or "BioCreative" model.
       new File(Messages.getString("i2b2.CRF.Lancet.Parser.CRFModel.Mallet2x.FilePath"))
    */
    public CRFTagger(int mode) {
    	try {
    	    myMode = mode;
    	    URL model = null;//Tagger.class.getResource("resources/nlpba.crf");
    	    if (mode == BIOCREATIVE) {
    		System.err.println("Loading BioCreative tagging module...");
    		model = Tagger.class.getResource("models/biocreative.crf");
    	    }else if(mode == I2B22009)
    	    {
    	    	String mfile = Messages.getString("i2b2.CRF.Lancet.Parser.CRFModel.FilePath");
    	    	
    	    	System.out.println(Messages.getString("i2b2.CRF.Lancet.Parser.CRFModel.FilePath"));
    	    	model =  Tagger.class.getResource(mfile);
    	    	System.err.println("Loading i2b2 2009 medication tagging model!");
    	    	
    	    }else if(mode == I2B22010 )
    	    {
    	    	String mfile = Messages.getString("i2b2.CRF.Lancet.Parser.CRFModel.Mallet2x.FilePath");
    	    	
    	    	model =  CRFTagger.class.getResource(mfile);
    	    	System.err.println("Loading i2b2 2010 medication tagging model: " + mfile);
    	    	
    	    }else {
    		System.err.println("Loading default NLPBA tagging module...");
    		model = Tagger.class.getResource("models/nlpba.crf");
    	    }
    	    ObjectInputStream ois = new ObjectInputStream(model.openStream());
//    	    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
//    	    		modelPath));
    	    initialize(ois);
    	    ois.close();
    	} catch (Exception e) {
    	    System.err.println(e);
    	}
    }

    /**
       External constructor: Load a trained CRF specified by the
       external model file.
    */
    public CRFTagger(File f) {
	try {
	    System.err.println("Loading external tagging module from '"+f.getPath()+"'...");
	    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
	    initialize(ois);
	    ois.close();
	    myMode = EXTERNAL;
	} catch (Exception e) {
	    System.err.println(e);
	}
    }

    

    ////////////////////////////////////////////////////////////////
    /**
       <p>Turn on/off ABNER's built-in tokenization (default is
       <tt>true</tt>).
     */
    public  void setTokenization(boolean t) {
	doTokenization = t;
    }
    /**
       <p>Return the tagger's current tokenization setting.
     */
    public  boolean getTokenization() {
	return doTokenization;
    }
    /**
       <p>Return the tagger's mode (NLPBA, BIOCREATIVE, or EXTERNAL)
     */
    public int getMode() {
	return myMode;
    }


    ////////////////////////////////////////////////////////////////
    /**
       <p>Take raw text apply ABNER's built-in tokenization on it.
     */
    public  String tokenize(String s) {
	StringBuffer sb = new StringBuffer();
	try {
	    Scanner scanner = new Scanner(new StringReader(s));;
	    String t;
	    while ((t = scanner.next()) != null) {
		sb.append(t+" ");
		if (t.toString().matches("[?!\\.]"))
		    sb.append("\n");
	    }
	    return sb.toString();
	} catch (Exception e) {
	    System.err.println(e);
	}
	return sb.toString();
    }

    ////////////////////////////////////////////////////////////////
    /**
       <p>Take an input string (if tokenization is turned on, this
       string will be tokenized as well) and return a {@link Vector}
       of 2D {@link String} arrays, where sentence tokens are
       <i>words</i> stored in <tt>result[0][...]</tt> and tags are
       stored in <tt>result[1][...]</tt>.
     */
    public  Vector getWords(String text) {
	Vector myList = new Vector();
	Vector tagged = doTheTagging(text);
	for (int i=0; i<tagged.size(); i++) {
	    myList.add((String[][])tagged.get(i));
	}
	return myList;
    }

    ////////////////////////////////////////////////////////////////
    /**
       <p>Take an input string (if tokenization is turned on, this
       string will be tokenized as well) and return a {@link Vector}
       of 2D {@link String} arrays, where sentence tokens are
       <i>segments</i> (not individual words). In other words, words
       comprising protein names are grouped together, words in
       non-entity phrases are grouped together, as well, etc.
     */
    public  Vector getSegments(String text) {
	Vector myList = new Vector();
	Vector tagged = doTheTagging(text);
	// cycle through all the sentences
	for (int i=0; i<tagged.size(); i++) {
	    String sent[][] = (String[][])tagged.get(i);
	    // we need to be sure that this isn't a blank line.
	    if (sent[0].length > 0) {
		Vector tmpSegs = new Vector();
		Vector tmpTags = new Vector();
		// cycle through words and build the segments
		StringBuffer tmpSeg = new StringBuffer(sent[0][0]);
		String tmpTag = sent[1][0].replaceAll("[BI]-","");
		for (int j=1; j<sent[0].length; j++) {
		    // if we're starting a new segment, store the
		    // seg-in-progress and start the new one...
		    if (!sent[1][j].replaceAll("[BI]-","").equals(tmpTag)) {
			tmpSegs.add(tmpSeg.toString());
			tmpTags.add(tmpTag);
			tmpSeg = new StringBuffer(sent[0][j]);
			tmpTag = sent[1][j].replaceAll("[BI]-","");
		    }
		    // if same segment, just tack on this word...
		    else tmpSeg.append(" "+sent[0][j]);
		}
		tmpSegs.add(tmpSeg.toString());
		tmpTags.add(tmpTag);
		// done. load it up!
		String[][] val = new String[2][tmpSegs.size()];
		for (int j=0; j<val[0].length; j++) {
		    val[0][j] = (String)tmpSegs.get(j);
		    val[1][j] = (String)tmpTags.get(j);
		}
		myList.add(val);
	    }
	    // if it's a blank line... move along...
	    else {
		myList.add(new String[2][0]);
	    }
	}
	return myList;
    }

    ////////////////////////////////////////////////////////////////
    /**
       <p>Similar to getSegments, but returns all segments in the
       entire document that correspond to entities (e.g. "DNA,"
       "protein," etc.). Segment <i>text</i> is stored in
       <tt>result[0][...]</tt> and entity tags (minus "B-" and "I-"
       prefixes) are stored in <tt>result[1][...]</tt>.
     */
    public String[][] getEntities(String text) {
	String[][] result;
	Vector tmpSegs = new Vector();
	Vector tmpTags = new Vector();
	Vector tagged = doTheTagging(text);
	// cycle through all the sentences
	for (int i=0; i<tagged.size(); i++) {
	    String sent[][] = (String[][])tagged.get(i);
	    // we need to be sure that this isn't a blank line.
	    if (sent[0].length > 0) {
		// cycle through words and build the segments
		StringBuffer tmpSeg = new StringBuffer(sent[0][0]);
		String tmpTag = sent[1][0].replaceAll("[BI]-","");
		for (int j=1; j<sent[0].length; j++) {
		    // if we're starting a new segment, store the
		    // seg-in-progress and start the new one...
		    if (!sent[1][j].replaceAll("[BI]-","").equals(tmpTag)) { //
			if (!tmpTag.equals("O")) {
			    tmpSegs.add(tmpSeg.toString());
			    tmpTags.add(tmpTag);
			}
			tmpSeg = new StringBuffer(sent[0][j]);
			tmpTag = sent[1][j].replaceAll("[BI]-","");
		    }
		    // if same segment, just tack on this word...
		    else tmpSeg.append(" "+sent[0][j]);
		}
		if (!tmpTag.equals("O")) {
		    tmpSegs.add(tmpSeg.toString());
		    tmpTags.add(tmpTag);
		}
	    }
	}
	// done. load it up!
	result = new String[2][tmpSegs.size()];
	for (int j=0; j<result[0].length; j++) {
	    result[0][j] = (String)tmpSegs.get(j);
	    result[1][j] = (String)tmpTags.get(j);
	}
	return result;
    }

    ////////////////////////////////////////////////////////////////
    /**
       <p>Returns only segments corresponding to the entity provided
       in the <tt>tag</tt> argument (do not us "B-" or "I-" prefixes).
     */
    public  String[] getEntities(String text, String tag) {
	String[] result;
	Vector tmpSegs = new Vector();
	Vector tmpTags = new Vector();
	Vector tagged = doTheTagging(text);
	// cycle through all the sentences
	for (int i=0; i<tagged.size(); i++) {
	    String sent[][] = (String[][])tagged.get(i);
	    // we need to be sure that this isn't a blank line.
	    if (sent[0].length > 0) {
		// cycle through words and build the segments
		StringBuffer tmpSeg = new StringBuffer(sent[0][0]);
		String tmpTag = sent[1][0].replaceAll("[BI]-","");
		for (int j=1; j<sent[0].length; j++) {
		    // if we're starting a new segment, store the
		    // seg-in-progress and start the new one...
		    if (!sent[1][j].replaceAll("[BI]-","").equals(tmpTag)) {
			if (tmpTag.equals(tag)) {
			    tmpSegs.add(tmpSeg.toString());
			    tmpTags.add(tmpTag);
			}
			tmpSeg = new StringBuffer(sent[0][j]);
			tmpTag = sent[1][j].replaceAll("[BI]-","");
		    }
		    // if same segment, just tack on this word...
		    else tmpSeg.append(" "+sent[0][j]);
		}
		if (tmpTag.equals(tag)) {
		    tmpSegs.add(tmpSeg.toString());
		    tmpTags.add(tmpTag);
		}
	    }
	}
	// done. load it up!
	result = new String[tmpSegs.size()];
	for (int j=0; j<result.length; j++) {
	    result[j] = (String)tmpSegs.get(j);
	}
	return result;
    }

    ////////////////////////////////////////////////////////////////
    /**
       <p>Takes input text and returns a string of annotated text in
       the ABNER training format:

       <pre>
       IL-2|B-DNA  gene|I-DNA  expression|O  and|O  NF-kappa|B-PROTEIN  B|I-PROTEIN  activation|O  ...
       </pre>

       Words and tags are "|" (vertical pipe) delimited, and sentences
       are separated with newlines.
     */
    public  String tagABNER(String text) {	
	StringBuffer tmp = new StringBuffer();
	// first, do the annotations
	Vector tagged = doTheTagging(text);
	for (int i=0; i<tagged.size(); i++) {
	    String sent[][] = (String[][])tagged.get(i);
	    for (int j=0; j<sent[0].length; j++) {
		tmp.append(sent[0][j]+"|");
		tmp.append(sent[1][j]+"  ");
	    }
	    if (sent[0].length > 0)
		tmp.append("\n");
	}
	return tmp.toString();
    }

    ////////////////////////////////////////////////////////////////
    /**
       <p>Takes input text and returns a string of annotated text in
       CoNLL-style "IOB" format:

       <pre>
       IL-2    B-DNA
       gene    I-DNA
       expression      O
       and     O
       NF-kappa        B-PROTEIN
       B       I-PROTEIN
       activation      O
       ...
       </pre>

       Words and tags are tab-delimited, and sentences are separated
       by blank lines.
     */
    public  String tagIOB(String text) {
    	
	StringBuffer tmp = new StringBuffer();
	// first, do the annotations
	Vector tagged = doTheTagging(text);
	String tag = "";
	for (int i=0; i<tagged.size(); i++) {
	    String sent[][] = (String[][])tagged.get(i);
	    for (int j=0; j<sent[0].length; j++) {
		tmp.append(sent[0][j]+"\t");
		tmp.append(sent[1][j]+"\n");
	    }
	    if (sent[0].length > 0)
		tmp.append("\n");
	}
	return tmp.toString();
    }

    ////////////////////////////////////////////////////////////////
    /**
       <p>Takes input text and returns a string of annotated text in
       a generic SGML-style format:

       <pre>
       &lt;DNA&gt; IL-2 gene &lt;/DNA&gt; expression and &lt;PROTEIN&gt; NF-kappa B &lt;/PROTEIN&gt; activation...
       </pre>

       Words remain tokenized, and sentences are separated by
       newlines.
     */
    public  String tagSGML(String text) {
	StringBuffer tmp = new StringBuffer();
	Vector segs = getSegments(text);
	for (int i=0; i<segs.size(); i++) {
	    //	    Sentence s = (Sentence)segs.get(i);
	    String[][] s = (String[][])segs.get(i);
	    for (int j=0; j<s[0].length; j++) {
		if (s[1][j].equals("O"))
		    tmp.append(s[0][j]+" ");
		else 
		    tmp.append("<"+s[1][j]+"> "+s[0][j]+" </"+s[1][j]+"> ");
	    }
	    tmp.append("\n");
	}
	return tmp.toString();
    }


    ////////////////////////////////////////////////////////////////
    // THIS FUNCTION ACTUALLY DOES THE TAGGING ITSELF
    private  Vector doTheTagging(String text) {
    	File f = new File(text);
    	String inputFile = null;
    	if (!f.exists())
    	 inputFile = RawInput.writeTemporaryFile(text, "crftagger", "input");
    	else
    		inputFile = text;
    	
    	
    	
	Vector result = new Vector();
	//	try {
	// define the instance feature pipe...
	InstanceList data = new InstanceList (myPipe);
	// tokenize if appropriate, otherwise don't...
	
	SimpleFileLineIterator lines = new SimpleFileLineIterator (new File (inputFile));
	data.addThruPipe(lines);
	// cycle through sentences, tag each one, store up the 
	for (int i=0; i<data.size(); i++) {
	    // nab the sentence, set up the input, and 
	    Instance instance = data.get(i);
	    Sequence input = (Sequence) instance.getData();

	    // get the predicted labeling...
//	    myCRF.viterbiPath(input).output();
	    Sequence predOutput = myCRF.transduce(input);
	    assert (input.size() == predOutput.size());

	    String[][] tokens = new String[2][];
	    tokens[0] = ((String) instance.getSource().toString()).split("[ \t]+");
	    tokens[1] = new String[tokens[0].length];
	    if (tokens[0].length > 0) {
		for (int j=0; j<predOutput.size(); j++)
		    tokens[1][j] = predOutput.get(j).toString();
	    }
	    result.add(tokens);
	}
	// done return the results.
	return result;
    }

	/*
 * Definition: tokenize the article just with space to meet i2b2
 */
	public String i2b2Tokenize(String s) {
		String lines[] = s.split("\n");
		
		StringBuffer sb = new StringBuffer();
		for(String line: lines)
		{
			try {
				Scanner scanner = new Scanner(new StringReader(line));
				String t;
				while (scanner.hasNext()) {
					t= scanner.next();
					sb.append(t+" ");
					//			if (t.toString().matches("[?!\\.]"))
					//			    sb.append("\n");//replace with a space will not influence the position of other word
				}
			} catch (Exception e) {
				System.err.println(e);
			}
			
			sb.append("\n");
		}
		return sb.toString();
	}

	    /**
       <p>Turn on/off ABNER's built-in tokenization (default is
       <tt>true</tt>).
     * @param string 
     */
    public  void setTokenization(boolean t, String type) {
	doTokenization = t;
	tokenizer = type;
    }
    
	public ArrayList<HashMap<String, String>> tagI2B2(String text) {
//		StringBuffer tmp = new StringBuffer();
		Vector<String[][]> segs = getSegments(text);
		
		ArrayList<HashMap<String, String>> taglist = new ArrayList<HashMap<String, String>>();
		for (int i=0; i<segs.size(); i++) {
			int lineIndex = i + 1;

			//		[[aspirin, 325 mg, daily, ,, metformin, 500 mg, b.i.d., , and, Humalog insulin, 12 units, q.i.d.], [M, DO, F, O, M, DO, F, O, M, DO, F]]
			String[][] line = (String[][])segs.get(i);
			int startOffset = 0;
			int endOffset = 0;
//			System.out.print("---:");
//			for(String a: s[0])
//				System.out.print(a + " ");
//			
//			System.out.println();
			for (int j=0; j<line[0].length; j++) {
				
				String taggedText = line[0][j].toLowerCase();
				String type = line[1][j].toUpperCase();


				endOffset =  startOffset + GetI2B2TokenSize(taggedText) - 1;

				String lIndex_ex = Integer.toString(lineIndex);
				String offset_ex = lIndex_ex + ":" + Integer.toString(startOffset) + " " + lIndex_ex + ":" + Integer.toString(endOffset);

				if(!type.equals("O")){
					HashMap<String, String> tag = new HashMap<String, String>();
					tag.put("type", type);
					tag.put(type, taggedText);
					tag.put(type + "Offset", offset_ex);
					
					taglist.add(tag);
				}
				
				startOffset = endOffset + 1;
			}
		}
		return taglist;
	}
	private int GetI2B2TokenSize(String taggedText) {
		// TODO Auto-generated method stub
//		StringBuffer sb = new StringBuffer();
		int size =0;
		try {
			Scanner scanner = new Scanner(new StringReader(taggedText));;
//			String t;
			while (scanner.hasNext()) {
				String t = scanner.next();
				size++;
			}
		} catch (Exception e) {
			System.err.println(e);
		}
		return size;
	}
	
	/*
	 * Try to tag i2b2 in sentence level
	 */
		public ArrayList<HashMap<String, String>> tagI2B2BySentence(
				ArrayList<HashMap<String, String>> span, String article) {
			
			ArrayList<HashMap<String, String>> taglist = new ArrayList<HashMap<String, String>>();

			article = article.replaceAll("\\|", " ");
			
			for (HashMap<String,String> sentence: span){
				int iBeginIndex = Integer.parseInt(sentence.get("BeginIndex"));
				int iEndIndex = Integer.parseInt(sentence.get("EndIndex")); 
				String originText = article.substring(iBeginIndex, iEndIndex);
				
				System.out.print(sentence.get("Sentence") + "\t");
				System.out.println();
				String sent = sentence.get("Sentence").toLowerCase();
				sent = sent.replaceAll("\\|", " ");
				
				if(sent.length() < 1)
					continue;
				
				Vector<String[][]> segs = getSegments(sent);
				
				for (int i=0; i<segs.size(); i++) {
					int lineIndex = i + 1;

					//		[[aspirin, 325 mg, daily, ,, metformin, 500 mg, b.i.d., , and, Humalog insulin, 12 units, q.i.d.], [M, DO, F, O, M, DO, F, O, M, DO, F]]
					String[][] line = (String[][])segs.get(i);				
					int startPosition = 0;				
					
					for (int j=0; j<line[0].length; j++) {
						
						String taggedText = line[0][j].toLowerCase();
						String type = line[1][j].toUpperCase();
						
						HashMap<String, Integer> pos = getPositionForText(taggedText, originText, startPosition);
						startPosition = pos.get("EndPosition");
						
						if(!type.equals("O")){
							if(type.equals("R"))
								System.out.println(taggedText);
							HashMap<String, String> tag = new HashMap<String, String>();
							tag.put("type", type);
							tag.put(type, taggedText);
							tag.put(type + "Offset", "");
							
							tag.put("StartPosition", Integer.toString(iBeginIndex + pos.get("StartPosition")));
							tag.put("EndPosition", Integer.toString(iBeginIndex + pos.get("EndPosition")));
							
							taglist.add(tag);
						}
					}
				}	
			}
			return taglist;
		}
		
		/*
		 * Get the start and end position of tagged text in original
		 * text.
		 * Considering the multiple entity wit same name in same sentence, we
		 * only match the first one. Therefore, the original text must adopted
		 * for this rule.
		 */
			private HashMap<String, Integer> getPositionForText(String taggedText,
					String originText, int start) {
				HashMap<String, Integer> pos = new HashMap<String, Integer>();
				 RawInput rin = new RawInput();
				 String tagged = rin.normalizeWord(taggedText);
				 String tagRex = tagged.replaceAll("\\s+", "\\\\s+");
				 Pattern tagPattern = Pattern.compile("(" + tagRex + ")");
				 
				 originText = originText.substring(start);
				 Matcher tagMatcher = tagPattern.matcher(originText);

				 if(tagMatcher.find()){
					 pos.put("StartPosition", start + tagMatcher.start());
					 pos.put("EndPosition", start + tagMatcher.end());		 
				 }else{
					 System.err.println("Abner:Tagger:getPositionForText: " +
					 		                       "could not match the text");
				 }
				return pos;
			}
}
