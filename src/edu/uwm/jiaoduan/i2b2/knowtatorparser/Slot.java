package edu.uwm.jiaoduan.i2b2.knowtatorparser;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the slot in knowtator. Slot can have multiple values.
 * @author shashank
 */
public class Slot extends I2B2Knowtator {
    private List<I2B2Knowtator> values;
    private String slotName;

    /**
     * Creates a Slot instance with the given id
     * @param id the id of the slot instance
     */
    public Slot(String id) {
        super(id);
        values = new ArrayList<I2B2Knowtator>();
    }

    /**
     * Gets the first value of this slot.
     * @return the first value of this slot
     */
    public I2B2Knowtator getValue() {
        return values.get(0);
    }

    /**
     * Gets the value specified by the given index
     * @param index the index of the slot value being sought
     * @return the value at the given index
     */
    public I2B2Knowtator getValue(int index) {
        return values.get(index);
    }

    /**
     * Adds a value to this slot
     * @param value the value to be added
     */
    public void addValue(I2B2Knowtator value) {
        values.add(value);
    }

    /**
     * Gets the (human readable) name of this slot
     * @return the name of this slot
     */
    public String getSlotName() {
        return slotName;
    }

    /**
     * Sets the name of this slot
     * @param slotName the name of this slot
     */
    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    /**
     * Returns the class of the value.
     * @return the class name of the value held by this slot
     */
    public String getValueType() {
        return values.get(0).getClass().getName();
    }

    /**
     * Returns a list of values associated with this slot
     * @return list of values associated with this slot
     */
    public List<I2B2Knowtator> getValues() {
        return values;
    }
}
