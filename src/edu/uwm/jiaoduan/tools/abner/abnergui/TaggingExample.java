package edu.uwm.jiaoduan.tools.abner.abnergui;

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

/*
  Very simple example of how to use ABNER's API to tag text.
 */

import edu.uwm.jiaoduan.Messages;
import edu.uwm.jiaoduan.i2b2.utils.JMerki;
import edu.uwm.jiaoduan.i2b2.utils.RawInput;
import edu.uwm.jiaoduan.tools.abner.*;

import java.io.*;
import java.lang.*;
import java.util.*;

public class TaggingExample {

    private static final int I2B22009 = 2;

	public static void main(String[] args) throws Exception {
//	Tagger t = new Tagger(I2B22009);//for i2b2 model
//	File file = new File(Messages.getString("i2b2.CRFModelFilePathName"));
//	String baseLineModel = "./CRFmodel/SwissKnife147.crf";
	String modelFile = Messages.getString("jd.models.abner.BioNLP.NLPBA2004");
	File file = new File(modelFile);
	File fmodel = new File(file.getCanonicalPath());
	Tagger t = new Tagger(fmodel);
	
//	get input
	String s = "";
	String infile = "./resources/mutation/20361015";
	s = RawInput.getFullText(infile);

	

	String i2b2 = t.tagABNER(s);
	System.out.println(i2b2);
	}
}
