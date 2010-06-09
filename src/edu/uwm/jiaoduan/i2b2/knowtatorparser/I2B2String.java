package edu.uwm.jiaoduan.i2b2.knowtatorparser;

/**
 * Just a glorified string
 * @author shashank
 */
public class I2B2String extends I2B2Knowtator {
    private String i2b2String;

    /**
     * Creates an instance of this class
     * @param i2b2String the string value associated with this class
     */
    public I2B2String(String i2b2String) {
        super("0"); // This is set by default, since knowtator does not
                    // supply a value here.
        this.i2b2String = i2b2String;
    }

    /**
     * Sets the value of the string represented by this object
     * @param i2b2String the string value to set
     */
    public void setI2b2String(String i2b2String) {
        this.i2b2String = i2b2String;
    }

    /**
     * Gets the value of the string represented by this object
     * @return the string value associated with this object
     */
    public String getI2b2String() {
        return i2b2String;
    }
}
