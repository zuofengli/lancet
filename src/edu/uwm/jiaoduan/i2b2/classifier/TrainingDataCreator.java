package edu.uwm.jiaoduan.i2b2.classifier;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import generalutils.Normalizer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates the training data for I2B2 classifier
 * @author shashank
 */
public class TrainingDataCreator {
    private static Normalizer normalizer = new Normalizer(true, true, false, false, true, true);

    public static void main(String[] args) {
        try {
            String dataDir = "D:/Users/shashank/Documents/projects/biocreative/i2b2/data/";
            String list = dataDir + "listSentence.txt";
            String narrative = dataDir + "narrativeSentence.txt";
            CsvWriter testWriter = new CsvWriter(dataDir + "trainingData.csv", ',', Charset.forName("UTF-8"));
            CsvReader listReader = new CsvReader(list, '\t', Charset.forName("UTF-8"));
            CsvReader narrativeReader = new CsvReader(narrative, '\t', Charset.forName("UTF-8"));
            write(narrativeReader, testWriter, "n");
            write(listReader, testWriter, "l");
            listReader.close();
            narrativeReader.close();
            testWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(TrainingDataCreator.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }

    private static void write(CsvReader reader, CsvWriter writer, String label) throws IOException {
        Set<String> seenSentences = new HashSet<String>();
        while (reader.readRecord()) {
            String file = reader.get(0);
            String sentence = reader.get(2);
            if (file.contains("5/163896")) {
                continue;
            }
            if (seenSentences.contains(sentence)) {
                continue;
            }
            seenSentences.add(sentence);
            writer.writeRecord(new String[] {normalizer.normalize(sentence), label});
        }
    }
}
