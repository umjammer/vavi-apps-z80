package konamiman.z80.utils;


/**
 * Represents a single bit that can be implicitly cast to/from and compared with booleans and integers.
 *
 * <remarks>
 * <para>
 * An instance with a value of one is equal to any non-zero integer and is true, an instance
 * with a value of zero is equal to the integer zero and is false.
 * </para>
 * <para>
 * Arithmetic and logical AND, OR and NOT, as well as arithmetic XOR, are supported.
 * </para>
 */
public class Bit {

    /**
     * Creates a new instance with the specified value.
     *
     * @param value The value for the bit. Anything other than 0 will be interpreted as 1.
     */
    private Bit(int value) {
        this.value = (value != 0) ? 1 : 0;
    }

    /**
     * Gets the value of the bit, 0 or 1.
     */
    private int value;

    public int intValue() {
        return value;
    }

    public boolean booleanValue() {
        return value != 0;
    }

    public @Override String toString() {
        return String.valueOf(value);
    }

//#region Implicit conversions

    public static Bit of(int value) {
        return new Bit(value);
    }

    public static Bit of(Bit bit) {
        return of(bit.value);
    }

    public static Bit of(boolean value) {
        return of(value ? 1 : 0);
    }

//#endregion

//#region Arithmetic operators

    /** @return new instance TODO */
    public Bit operatorOR(Bit value2) {
        return Bit.of(value | value2.value);
    }

    /** @return new instance TODO */
    public Bit operatorAND(Bit value2) {
        return Bit.of(value & value2.value);
    }

    /** @return new instance TODO */
    public Bit operatorXOR(Bit value2) {
        return Bit.of(value ^ value2.value);
    }

    /** @return new instance TODO */
    public Bit operatorNEG() {
        return Bit.of(value ^ 1);
    }

    /** @return new instance TODO */
    public Bit operatorNOT() {
        return operatorNEG();
    }

//#endregion

//#region The true and false operators

    public static final Bit ON = Bit.of(1);

    public static final Bit OFF = Bit.of(0);

//#endregion

//#region Comparison operators

    public boolean operatorEquals(int intValue) {
        return (value == 0 && intValue == 0) || (value == 1 && intValue != 0);
    }

    public boolean operatorNotEqual(int intValue) {
        return !operatorEquals(intValue);
    }

    /**
     * Indicates whether this instance and a specified object are equal.
     *
     * @param obj The object to compare with the current instance.
     * @return <b>true</b> if obj and this instance are the same type and represent the same value; otherwise, <b>false</b>.
     */
    public @Override boolean equals(Object obj) {
        if (obj instanceof Integer)
            return operatorEquals((int) obj);
        else if (obj instanceof Bit)
            return operatorEquals(((Bit) obj).value);
        else
            return super.equals(obj);
    }

//#endregion
}

