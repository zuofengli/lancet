package edu.uwm.jiaoduan.i2b2.knowtatorparser;

/**
 * Represents a class in knowtator
 * @author shashank
 */
public class I2B2Knowtator {
    private String id;

    /**
     * Creates an instance of I2B2Knowtator with the given id
     * @param id the id of the knowtator instance
     */
    public I2B2Knowtator(String id) {
        this.id = id;
    }

    /**
     * Gets the id of this knowtator instance
     * @return the id of the knowtator instance
     */
    public String getId() {
        return id;
    }
}
