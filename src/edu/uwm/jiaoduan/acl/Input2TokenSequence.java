package edu.uwm.jiaoduan.acl;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;


public class Input2TokenSequence extends Pipe {
	public Input2TokenSequence() {
		super (new Alphabet(), new LabelAlphabet());
	}

	@Override
	public Instance pipe(Instance carrier) {

		String input = (String) carrier.getData();
		String[] tokenLines = input.split("\n");

		LabelAlphabet labels;
		LabelSequence target = null;
		if (isTargetProcessing())
		{
			labels = (LabelAlphabet)getTargetAlphabet();
			target = new LabelSequence (labels, tokenLines.length);
		}


		TokenSequence data = new TokenSequence(tokenLines.length);
		for(int i=0; i< tokenLines.length; i++){
			String line = tokenLines[i];
			System.out.println(line);

			String[] features = line.split("\\s+");
			
			int iFeatsLength = features.length;
			String word = features[0];
			String wc = word;
			String bwc = word;
			String originWord = word;

			String label = features[iFeatsLength -1];
			Token token = new Token(word);
			token.setFeatureValue("W=" + word, 1);
			
//			preprocessing
			for (int j =1; j< iFeatsLength -1; j++ ){
				String ftName = "PPF-" + Integer.toString(j) + ":" + features[j];
				token.setFeatureValue(ftName, 1);
			}

			//			transform
			word = doDigitalCollapse(word);
			token.setFeatureValue("DC=" + word, 1);

			wc = doWordClass(wc);
			token.setFeatureValue("WC=" + wc, 1);

			bwc = doBriefWordClass(bwc);
			token.setFeatureValue("BWC=" + bwc, 1);

			String ld = originWord.toLowerCase();
			token.setFeatureValue("LC=" + ld, 1);
			  

			//			finish one token line
			if (isTargetProcessing())
			{
				target.add(label);
			}

			data.add(token);

		}

		//		
		carrier.setData(data);
		if (isTargetProcessing())
			carrier.setTarget(target);
		else
			carrier.setTarget(new LabelSequence(getTargetAlphabet()));

		return carrier;
	}

	private String doBriefWordClass(String bwc) {
		bwc = bwc.replaceAll("[A-Z]+", "A");
		bwc = bwc.replaceAll("[a-z]+", "a");
		bwc = bwc.replaceAll("[0-9]+", "0");
		bwc = bwc.replaceAll("[^A-Za-z0-9]+", "x");
		return bwc;
	}

	private String doWordClass(String wc) {
		wc = wc.replaceAll("[A-Z]", "A");
		wc = wc.replaceAll("[a-z]", "a");
		wc = wc.replaceAll("[0-9]", "0");
		wc = wc.replaceAll("[^A-Za-z0-9]", "x");
		
		return wc;
	}

	private String doDigitalCollapse(String word) {
		
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
			return word;
			
	}



}
