package edu.uwm.jiaoduan.i2b2.classifier;

import cs425termproject.crossvalidator.ARFFWriter;
import cs425termproject.crossvalidator.MutualInformationWriter;
import cs425termproject.crossvalidator.SentenceDataReader;
import fulltextclassifier.data.SentenceData;
import java.util.List;

/**
 * Writes the mutual information and arff file
 * @author agarwal
 */
public class ArffWriter {
    public static void main(String[] args) {
        String base = "D:/Users/shashank/Documents/projects/biocreative/i2b2/data/";
        List<SentenceData> sentenceDataList = SentenceDataReader.readSentenceDate(base + "training_data.csv");
        MutualInformationWriter miw = new MutualInformationWriter(new int[] {1, 2, 3});
        miw.writeMI(base + "training_data.csv", base + "i2b2_mi.csv", base + "categories.txt");
        ARFFWriter aw = new ARFFWriter();
        System.out.println("Writing arff");
        aw.writeARFF(base + "i2b2_clasifier.arff", sentenceDataList, base + "i2b2_mi.csv", 2000, false, false, "l,n");
    }
}
