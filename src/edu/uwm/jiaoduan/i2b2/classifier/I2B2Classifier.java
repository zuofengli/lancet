package edu.uwm.jiaoduan.i2b2.classifier;

import fulltextclassifier.classifier.WekaClassifier;
import generalutils.Normalizer;

/**
 * The classifier for classify list and narrative text in the I2B2 challenge
 * @author shashank
 */
public class I2B2Classifier {
    private WekaClassifier wc;
    private Normalizer normalizer;

    /**
     * Creates an instance of Classifier for I2B2.
     */
    public I2B2Classifier(String classifierName, String arffFile) {
        normalizer = new Normalizer(true, true, false, false, true, true);
        wc = new WekaClassifier(arffFile, false, "", classifierName);
    }

    /**
     * Classifies the given sentence
     * @param sentence the sentence to classify
     * @param normalized specify if the given sentence is already normalized
     * @return the category of the sentence, "l" (for list) or "n" (for narrative)
     */
    public String classify(String sentence, boolean normalized) {
        if (!normalized) {
            sentence = normalizer.normalize(sentence);
        }
        double category = wc.classify(sentence, sentence, 0, false);
        if (category == 0.0) {
            return "l";
        }
        if (category == 1.0) {
            return "n";
        }
        return "unknown";
    }

    public static void main(String[] args) {
        I2B2Classifier classifier = new I2B2Classifier("weka.classifiers.functions.SMO", 
                "D:/Users/shashank/Documents/projects/biocreative/i2b2/data/i2b2_clasifier.arff");
        System.out.println("Class: " + classifier.classify("ADMISSION MEDICATIONS: Colace 100 mg b.i.d. , insulin Lente 12 units subcu q p.m. supplemented by sliding scale regular insulin scale , Isordil 30 mg t.i.d. , Zestril 5 mg q d , Lopressor 50 mg b.i.d. , Axid 150 mg b.i.d. , Ofloxacin 200 mg p.o. q 12 , Ecotrin 225 mg q d , Vancomycin 1250 mg q 24.",
                false));
    }
}
