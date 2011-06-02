package edu.uwm.jiaoduan.acl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFTrainerByLabelLikelihood;
import cc.mallet.types.Instance;
import cc.mallet.types.Sequence;
import cc.mallet.util.FileUtils;
import edu.uwm.jiaoduan.i2b2.utils.RawInput;

public class TaggerCRF {

	public TaggerCRF(String infile) {
		//load model
		CRF crf = null;
		File f = new File("C:/Users/Zuofeng/workspace/lancet/model.crf");
		if(!f.exists())
			System.exit(0);
		
		try {
			ObjectInputStream ois = new ObjectInputStream(
					new FileInputStream(f));
			crf = (CRF) ois.readObject();
			ois.close();
		} catch (IOException e) {
			System.err.println("Exception reading file: " + e);
		} catch (ClassNotFoundException cnfe) {
			System.err.println("Cound not find class reading in object: "
					+ cnfe);
		}
		CRFTrainerByLabelLikelihood crft = new CRFTrainerByLabelLikelihood(crf);
		crft.setUseSparseWeights(false);
		
		System.err.println("Read in CRF.");
		System.err.println("CRF parameters. hyperbolicPriorSlope: "
				+ crft.getUseHyperbolicPriorSlope()
				+ ". hyperbolicPriorSharpness: "
				+ crft.getUseHyperbolicPriorSharpness()
				+ ". gaussianPriorVariance: "
				+ crft.getGaussianPriorVariance());
		//load infile
		
		String testString = "John NNP B-NP O\nDoe NNP I-NP O\nsaid VBZ B-VP O\nhi NN B-NP O\n";
		String testFile = "C:/Users/Zuofeng/workspace/lancet/test.txt";
		testString = RawInput.getFullText( testFile );
		ArrayList<String> lines = RawInput.getLines( testFile, false);
		testString = RawInput.join(lines, "\n");
		
		
		System.out.println(testString);
//		testString = "John O\nDoe O\nis O\nKate's O\nfather O";
//		testString = "\nzuofeng O\nLI O\nis O\nben's O\nfather O";
//		testString = "zuofeng \nLI \nis \nben's \nfather";
//		testString = "Thus, she was transitioned over to a ciprofloxacin 700 mg p.o. b.i.d. regime for a total of 12 days for a presumed urinary tract infection";
//		String[] tokens = testString.split("\\s+");
//		testString = RawInput.join(tokens, " O\n");
		
		Instance inst = crf.getInputPipe().instanceFrom(
				new Instance(testString, null, null, null));

		Sequence output = crf.transduce((Sequence) inst.getData());
		
		String[][] tokens = new String[2][];
//		tokens[0] = ((String) inst.getSource().toString()).split("[ \t]+");
		System.out.println("---");
		System.out.println( inst.getSource());
		
		System.out.println("---");
		
			
//		crf.print();
//		System.out.println(crf.numStates());
		String std = output.toString();
		String[] tags = std.split("\\s+");
		String[] iobs = std.trim().split(" ");
		assert(iobs.length == lines.size());
		System.out.println(std);
		
		System.out.println(lines.size());
		System.out.println(iobs.length);
//		for (String line : lines)
//			System.out.println(line);
		
		for (int i = 0; i < lines.size(); i++){
			System.out.print(lines.get(i));
			System.out.println( " " + iobs[i]);
		}

		
		
		
		//tag the infile
		//output the results
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TaggerCRF tagger = new TaggerCRF(args[0]);

	}

}
