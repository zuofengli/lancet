package edu.uwm.jiaoduan.i2b2.knowtatorparser;

import generalutils.FileOperations;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gets the text associated with the given spans
 * @author shashank
 */
public class AssociatedTextGetter {
    private String fileText;

    /**
     * Creates an instance of the AssociatedTextGetter
     * @param fileName the article file to be loaded
     */
    public AssociatedTextGetter(String fileName) {
        try {
            ArrayList<String> fileTexts = FileOperations.readFile(fileName);
            fileText = "";
            for (String ft : fileTexts) {
                fileText += ft + "\n";
            }
        } catch (Exception ex) {
            Logger.getLogger(AssociatedTextGetter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Gets the text associated for this article beginning at the spanStart and
     * ending at spanEnd
     * @param spanStart the start of span
     * @param spanEnd the end of span
     * @return the text associated with these span parameters. If span start or
     * span end are less than 0, then "No Text Associated" is returned. "No text
     * associated" is also returned if spanStart is greater than spanEnd
     */
    public String getAssociatedText(int spanStart, int spanEnd) {
        if ((spanStart < 0) || (spanEnd < 0) || (spanStart > spanEnd)) {
            return "No Text Associated";
        }
        return fileText.substring(spanStart, spanEnd);
    }

}
