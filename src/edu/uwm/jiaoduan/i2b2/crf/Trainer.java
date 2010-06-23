package edu.uwm.jiaoduan.i2b2.crf;

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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.regex.*;
import java.io.*;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFOptimizableByLabelLikelihood;
import cc.mallet.fst.CRFTrainerByLabelLikelihood;
import cc.mallet.fst.CRFTrainerByValueGradients;
import cc.mallet.fst.CRFWriter;
import cc.mallet.fst.MultiSegmentationEvaluator;
import cc.mallet.fst.TransducerEvaluator;
import cc.mallet.fst.TransducerTrainer;
import cc.mallet.optimize.Optimizable;
import cc.mallet.optimize.OptimizationException;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.PrintInputAndTarget;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.iterator.LineIterator;
import cc.mallet.pipe.iterator.SimpleFileLineIterator;
import cc.mallet.pipe.iterator.StringArrayIterator;
import cc.mallet.pipe.tsf.*;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Sequence;
import edu.uwm.jiaoduan.Messages;

/**
   <p>The Trainer class will train a CRF to extract entities from a
   customized dataset. The input file must be tokenized with one
   sentence per line, with a "|" (vertical pipe) separating a
   word/token from its label. The first token of an entity name should
   have a label beginning with "B-", all other entity token labels
   should begin with "I-", and non-entity tokens should be labeled
   with "O":

   <pre>
   IL-2|B-DNA gene|I-DNA expression|O and|O NF-kappa|B-PROTEIN B|I-PROTEIN activation|O ...
   </pre>

   @author Burr Settles <a href="http://www.cs.wisc.edu/~bsettles">bsettles&#64;&#99;s&#46;&#119;i&#115;&#99;&#46;&#101;d&#117;</a> 
   @version 1.5 (March 2005)
 */
public class Trainer {
	int numEvaluations = 0;
	static int iterationsBetweenEvals = 16;

	private static String CAPS = "[A-Z¡…Õ”⁄¿»Ã“Ÿ«—œ‹]";
	private static String LOW = "[a-z‡ËÏÚ˘·ÈÌÛ˙ÁÒÔ¸]";
	private static String CAPSNUM = "[A-Z¡…Õ”⁄¿»Ã“Ÿ«—œ‹0-9]";
	private static String ALPHA = "[A-Z¡…Õ”⁄¿»Ã“Ÿ«—œ‹a-z‡ËÏÚ˘·ÈÌÛ˙ÁÒÔ¸]";
	private static String ALPHANUM = "[A-Z¡…Õ”⁄¿»Ã“Ÿ«—œ‹a-z‡ËÏÚ˘·ÈÌÛ˙ÁÒÔ¸0-9]";
	private static String PUNCTUATION = "[,\\.;:?!()]";
	private static String QUOTE = "[\"`']";
	private static String GREEK = "(alpha|beta|gamma|delta|epsilon|zeta|eta|theta|iota|kappa|lambda|mu|nu|xi|omicron|pi|rho|sigma|tau|upsilon|phi|chi|psi|omega)";


	/**
       <p>Takes input <tt>trainFile</tt> (format described above), and
       saves a trained linear-chain CRF on the data using ABNER's
       default feature set in the corresponding output
       <tt>modelFile</tt>.

       <p><i>Warning: training will take several hours, perhaps even
       days to complete depending on corpus size and number of entity
       tags.</i>
	 */
	public void train (String trainFile, String modelFile) {
		train(trainFile, null, modelFile, null, 1.0);
	}

	/**
       <p>Identical to the other train routine, but the set of tags
       (e.g. "PROTEIN", "DNA", etc.) allows the model to periodically
       output progress in terms of precision/recall/f1 during
       training. <i>Note: do not use "B-" or "I-" prefixes.</i>
	 */

	public static void main(String[] args) throws Exception {
		String[] tags = new String[]{"M","DO","F","MO","DU","R"};
		Trainer trainer = new Trainer();
//		trainer.train("CRFTrainExample.txt", null, "crf.model", tags);
		
//		Train CRF model for i2b2 task with upgraded trainer.
		String trainFile = args[0];
		String modelFile = args[1];
		double[] varGaussianPrior = new double[]{0, 0.1, 0.5, 1 , 10,100};
		for (double var : varGaussianPrior){
			modelFile = args[1] + "." + Double.toString(var) + ".crf";
			trainer.train(trainFile, null, modelFile, tags, var);
		}
	}

	public void train (String trainFile, String testFile,String modelFile, String[] tags, double var) {

		try {
			SerialPipes p = getPipes();
			p.setTargetProcessing(true);

			CRF crf = null;
			// read in the traing set files
			System.out.println("Reading '"+trainFile+"' file...");

			//	    trainingData could be used for training or testing.
			InstanceList tnData = new InstanceList (p);

			

			//	    Untill now, tnData is null.
			//	    The following code would add training data by line one by one.
			//	    the line would be treated by Input2TokenSequence and split them into tokens.
			SimpleFileLineIterator lines = new SimpleFileLineIterator (new File (trainFile));

			tnData.addThruPipe(lines);
			TransducerEvaluator evaluator = null;
			
			if (crf == null){
				// init model w/info
				crf = new CRF (tnData.getPipe(), (Pipe)null);
				crf.addFullyConnectedStatesForLabels();
				crf.setWeightsDimensionAsIn(tnData, false);

				CRFOptimizableByLabelLikelihood optLabel =
					new CRFOptimizableByLabelLikelihood(crf, tnData);
				// CRF trainer
				Optimizable.ByGradientValue[] opts =
					new Optimizable.ByGradientValue[]{optLabel};

				// by default, use L-BFGS as the optimizer
				CRFTrainerByValueGradients trainer =
					new CRFTrainerByValueGradients(crf, opts);


				if (tags != null) {
					String[] bTags = new String[tags.length];
					String[] iTags = new String[tags.length];
					for (int i=0; i<tags.length; i++) {
						bTags[i] = "B-"+tags[i];
						iTags[i] = "I-"+tags[i];
					}
					InstanceList ttData = new InstanceList(p);
					if (testFile != null){
					
						lines = new SimpleFileLineIterator(new File(testFile));
						ttData.addThruPipe(lines);
					}

					evaluator = new MultiSegmentationEvaluator(
							new InstanceList[]{tnData, ttData},
							new String[]{"train", "test"}, bTags, iTags){
						@Override
						public boolean precondition(TransducerTrainer tt) {
							// evaluate model every 5 training iterations
							return tt.getIteration() % 5 == 0;
						}
					};

					trainer.addEvaluator(evaluator);
					CRFWriter crfWriter = new CRFWriter("ner_crf.model") {
						@Override
						public boolean precondition(TransducerTrainer tt) {
							// save the trained model after training finishes
							//ystem.out.println();
							return tt.getIteration() % Integer.MAX_VALUE == 0;
						}
					};

					trainer.addEvaluator(crfWriter);

					trainer.setMaxResets(0);
//					try{
//						trainer.train(tnData);
//					}catch(OptimizationException e){
//						System.out.println(e);
//						System.exit(1);
//					}
//					trainer.train(tnData, 10, new double[] {.2, .5, .8});
					crf = SimpleTagger.train2(tnData, evaluator, var);
					// now save the model
					crf.write(new File(modelFile));
				}

			}

			

		} catch (Exception e) {
			System.err.println(e);
		}

	}
	/**
	 * Build mallet pipe
	 * @return
	 */
	public SerialPipes getPipes() {
		ArrayList<Pipe> pipes = new ArrayList<Pipe>();
		try{		
			pipes.add(new Input2TokenSequence ());

			pipes.add(new RegexMatches ("INITCAPS", Pattern.compile ("[A-Z].*")));
			pipes.add(new RegexMatches ("INITCAPSALPHA", Pattern.compile ("[A-Z][a-z].*")));
			pipes.add(new RegexMatches ("ALLCAPS", Pattern.compile ("[A-Z]+")));
			//		pipes.add(new RegexMatches ("CAPSMIX", Pattern.compile ("[A-Za-z]+")));
			//		pipes.add(new RegexMatches ("HASDIGIT", Pattern.compile (".*[0-9].*")));
			//		pipes.add(new RegexMatches ("SINGLEDIGIT", Pattern.compile ("[0-9]")));
			//		pipes.add(new RegexMatches ("DOUBLEDIGIT", Pattern.compile ("[0-9][0-9]")));
			//		pipes.add(new RegexMatches ("NATURALNUMBER", Pattern.compile ("[0-9]+")));
			//		pipes.add(new RegexMatches ("REALNUMBER", Pattern.compile ("[-0-9]+[.,]+[0-9.,]+")));
			//		pipes.add(new RegexMatches ("HASDASH", Pattern.compile (".*-.*")));
			//		pipes.add(new RegexMatches ("INITDASH", Pattern.compile ("-.*")));
			//		pipes.add(new RegexMatches ("ENDDASH", Pattern.compile (".*-")));
			//		pipes.add(new TokenTextCharPrefix ("PREFIX=", 3));
			//		pipes.add(new TokenTextCharPrefix ("PREFIX=", 4));
			//		pipes.add(new TokenTextCharSuffix ("SUFFIX=", 3));
			//		pipes.add(new TokenTextCharSuffix ("SUFFIX=", 4));
			//		pipes.add(new OffsetConjunctions (new int[][] {{-1}, {1}}));
			//		pipes.add(new RegexMatches ("ALPHANUMERIC", Pattern.compile (".*[A-Za-z].*[0-9].*")));
			//		pipes.add(new RegexMatches ("ALPHANUMERIC", Pattern.compile (".*[0-9].*[A-Za-z].*")));
			//		pipes.add(new RegexMatches ("ROMAN", Pattern.compile ("[IVXDLCM]+")));
			//		pipes.add(new RegexMatches ("HASROMAN", Pattern.compile (".*\\b[IVXDLCM]+\\b.*")));
			//		pipes.add(new RegexMatches ("GREEK", Pattern.compile (GREEK)));
			//		pipes.add(new RegexMatches ("HASGREEK", Pattern.compile (".*\\b"+GREEK+"\\b.*")));
			//		pipes.add(new RegexMatches ("PUNCTUATION", Pattern.compile ("[,.;:?!-+]")));

			//		pallet: Page 3
			//		Function: Converts the Token Sequence to Feature Sequence. It indexes each token.
			//		Therefore the output of this serial pipe is Feature Sequence instance list.
			pipes.add(new TokenSequence2FeatureVectorSequence ());

			pipes.add(new PrintInputAndTarget());
		}catch(Exception e){
			System.err.println("Error in build CRF pipe.");
		}
		return new SerialPipes(pipes);
	}
}
