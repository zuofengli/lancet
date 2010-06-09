package edu.uwm.jiaoduan.i2b2.knowtatorparser;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an I2B2 annotation
 * @author shashank
 */
public class Annotation extends I2B2Knowtator implements Comparable<Annotation> {
    private int spanStart;
    private int spanEnd;
    private String name;
    private String associatedText;
    private Map<String, Slot> slots;

    /**
     * Creates an Annotation object
     * @param id the knowtator id of the annotation
     * @param spanStart start index of annotation span
     * @param spanEnd end index of annotation span
     */
    public Annotation(String id, int spanStart, int spanEnd) {
        super(id);
        slots = new HashMap<String, Slot>();
        this.spanStart = spanStart;
        this.spanEnd = spanEnd;
    }

    /**
     * Gets the name of the annotation
     * @return the name of the annotation
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the annotation
     * @param name the name of the annotation
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the text associated with this annotation
     * @param associatedText the text associated with this annotation
     */
    public void setAssociatedText(String associatedText) {
        this.associatedText = associatedText;
    }

    /**
     * Gets the text associated with this annotation
     * @return the text associated with this annotation
     */
    public String getAssociatedText() {
        return associatedText;
    }

    /**
     * Gets the span end index
     * @return the span end index
     */
    public int getSpanEnd() {
        return spanEnd;
    }

    /**
     * Gets the span start index
     * @return the span start index
     */
    public int getSpanStart() {
        return spanStart;
    }

    /**
     * Gets the slot by the given id
     * @param slotId the id of the slot which will be returned
     * @return the slot represented by the given slot id
     */
    public Slot getSlot(String slotId) {
        return slots.get(slotId);
    }

    /**
     * Puts the given slot into the slots associated with this annotation
     * @param slotId the id of the slot being added
     * @param slot the slot to add
     */
    public void putSlot(String slotId, Slot slot) {
        slots.put(slotId, slot);
    }

    /**
     * Gets a map of slot id (key) and slots (values)
     * @return the map of slots associated with this annotation
     */
    public Map<String, Slot> getSlots() {
        return slots;
    }

    /**
     * Compares this annotation with other annotation for sorting purposes. 
     */
    public int compareTo(Annotation otherAnnotation) {
    	return Double.compare(spanStart, otherAnnotation.spanStart);
    }
}
