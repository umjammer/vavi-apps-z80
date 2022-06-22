package konamiman.DataTypesAndUtils;


/**
 * Class with utility static and extension methods for manipulating numbers.
 */
public class NumberUtils {
    /**
     * Gets the high byte of a short value.
     *
     * @param value Number to get the high byte from
     * @return High byte of the number
     */
    public static byte GetHighByte(short value) {
        return (byte) (value >> 8);
    }

    /**
     * Gets the high byte of an ushort value.
     *
     * @param value Number to get the high byte from
     * @return High byte of the number
     */
//    public static byte GetHighByte(short value) {
//        return (byte) (value >> 8);
//    }

    /**
     * Returns a modified version of an ushort number that has
     * the specified value in the high byte.
     *
     * @param value Original number
     * @param highByte New high byte
     *
     * @return Number with the original low byte and the new high byte
     */
    public static short SetHighByte(short value, byte highByte) {
        return (short) ((value & 0x00FF) | (highByte << 8));
    }

    /**
     * Returns a modified version of a short number that has
     * the specified value in the high byte.
     *
     * @param value Original number
     * @param highByte New high byte
     * @return Number with the original low byte and the new high byte
     */
//    public static short SetHighByte(short value, byte highByte) {
//        return ((value & 0x00FF) | (highByte << 8)).ToShort();
//    }

    /**
     * Gets the low byte of a short value.
     *
     * @param value Number to get the low byte from
     * @return Loq byte of the number
     */
    public static byte GetLowByte(short value) {
        return (byte) (value & 0xFF);
    }

    /**
     * Gets the low byte of an ushort value.
     *
     * @param value Number to get the low byte from
     * @return Loq byte of the number
     */
//    public static byte GetLowByte(short value) {
//        return (byte) (value & 0xFF);
//    }

    /**
     * Returns a modified version of an ushort number that has
     * the specified value in the low byte.
     *
     * @param value Original number
     * @param lowByte New low byte
     * @return Number with the original high byte and the new low byte
     */
    public static short SetLowByte(short value, byte lowByte) {
        return (short) ((value & 0xFF00) | lowByte);
    }

    /**
     * Returns a modified version of a short number that has
     * the specified value in the low byte.
     *
     * @param value Original number
     * @param lowByte New low byte
     * @return Number with the original high byte and the new low byte
     */
//    public static short SetLowByte(short value, byte lowByte) {
//        return (short) ((value & 0xFF00) | lowByte);
//    }

    /**
     * Generates a short number from two bytes.
     *
     * @param lowByte Low byte of the new number
     * @param highByte High byte of the new number
     * @return  Generated number
     */
    public static short createShort(byte lowByte, byte highByte) {
        return (short) ((highByte << 8) | lowByte);
    }

    /**
     * Generates a ushort number from two bytes.
     *
     * @param lowByte Low byte of the new number
     * @param highByte High byte of the new number
     * @return Generated number
     */
    public static short CreateUshort(byte lowByte, byte highByte) {
        return (short) ((highByte << 8) | lowByte);
    }

    /**
     * Gets the value of a certain bit in a byte.
     * The rightmost bit has position 0, the leftmost bit has position 7.
     *
     * @param value Number to get the bit from
     * @param bitPosition Bit position to retrieve
     * @return Retrieved bit value
     */
    public static Bit GetBit(byte value, int bitPosition) {
        if (bitPosition < 0 || bitPosition > 7)
            throw new IllegalArgumentException("bit position must be between 0 and 7");

        return Bit.of(value & (1 << bitPosition));
    }

    /**
     * Retuens a copy of the value that has a certain bit set or reset.
     * The rightmost bit has position 0, the leftmost bit has position 7.
     *
     * @param number The original number
     * @param bitPosition The bit position to modify
     * @param value The bit value
     * @return The original number with the bit appropriately modified
     */
    public static byte WithBit(byte number, int bitPosition, Bit value) {
        if (bitPosition < 0 || bitPosition > 7)
            throw new IllegalArgumentException("bit position must be between 0 and 7");

        if (value.booleanValue()) {
            return (byte) (number | (1 << bitPosition));
        } else {
            return (byte) (number & ~(1 << bitPosition));
        }
    }

    public static byte WithBit(byte number, int bitPosition, int value) {
        return WithBit(number, bitPosition, Bit.of(value));
    }

    /**
     * Converts a number to short by substracting 65536 when the number is 32768 or higher.
     *
     * @param value Number to convert
     * @return Converted number
     */
    public static short ToShort(int value) {
        return (short) (short) value;
    }

    /**
     * Converts a number to short by substracting 65536 when the number is 32768 or higher.
     *
     * @param value Number to convert
     * @return Converted number
     */
    public static short ToShort(short value) {
        return value;
    }

    /**
     * Converts a number to ushort by adding 65536 when the number is negative.
     *
     * @param value Number to convert
     * @return Converted number
     */
    public static short ToUShort(int value) {
        return (short) value;
    }

    /**
     * Converts a number to ushort by adding 65536 when the number is negative.
     *
     * @param value Number to convert
     * @return Converted number
     */
    public static short ToUShort(short value) {
        return value;
    }

    /**
     * Converts a number to signed byte by substracting 256 when the number is 128 or higher.
     *
     * @param value Number to convert
     * @return Converted number
     */
    public static byte ToSignedByte(int value) {
        return (byte) value;
    }

    /**
     * Converts a number to signed byte by substracting 256 when the number is 128 or higher.
     *
     * @param value Number to convert
     * @return Converted number
     */
    public static byte ToSignedByte(byte value) {
        return value;
    }

    /**
     * Increases a number and turns it into min value if it has its maximum value.
     *
     * @param value Number to increase
     * @return Increased number, or zero
     */
    public static short Inc(short value) {
        return (short) (value + 1);
    }

    /**
     * Increases a number and turns it into zero if it has its maximum value.
     *
     * @param value Number to increase
     * @return Increased number, or zero
     */
//    public static short Inc(short value) {
//        return (short) (value + 1);
//    }

    /**
     * Decreases a number and turns it into max value if it passes under its minimum value.
     *
     * @param value Number to increase
     * @return Increased number, or zero
     */
    public static short Dec(short value) {
        return (short) (value - 1);
    }

    /**
     * Decreases a number and turns it into max value if it passes under its minimum value.
     *
     * @param value Number to decrease
     * @return Increased number, or zero
     */
//    public static short Dec(short value) {
//        return (short) (value - 1);
//    }

    /**
     * Adds a value to a number and overlaps it from zero if it passes its maximum value.
     *
     * @param value Number to be added to
     * @param amount Number to add
     * @return Increased number, or zero
     */
    public static short Add(short value, short amount) {
        return (short) (value + amount);
    }

    /**
     * Adds a value to a number and overlaps it from zero if it passes its maximum value.
     *
     * @param value Number to increase
     * @return Increased number, or zero
     */
//    public static short Add(short value, short amount) {
//        return (short) (value + amount);
//    }

    /**
     * Adds a value to a number and overlaps it from zero if it passes its maximum value.
     *
     * @param value Number to increase
     * @return Increased number, or zero
 */
    public static short Add(short value, byte amount) {
        return (short) (value + amount);
    }

    /**
     * Substract a value to a number and overlaps it from its max value if it passes below its minimum value.
     *
     * @param value Number to decrease
     * @return Increased number, or zero
     */
    public static short Sub(short value, short amount) {
        return (short) (value - amount);
    }

    /**
     * Substract a value to a number and overlaps it from its max value if it passes below its minimum value.
     *
     * @param value Number to decrease
     * @return Increased number, or zero
     */
//    public static short Sub(short value, short amount) {
//        return (short) (value - amount);
//    }

    /**
     * Increases a number and turns it into zero if it has its maximum value.
     *
     * @param value Number to increase
     * @return Increased number, or zero
     */
    public static byte Inc(byte value) {
        return (byte) (value + 1);
    }

    /**
     * Decreases a number and turns it into max value if it passes under its minimum value.
     *
     * @param value Number to decrease
     * @return Increased number, or zero
     */
    public static byte Dec(byte value) {
        return (byte) (value - 1);
    }

    /**
     * Adds a value to the number and overlaps it from zero if it passes its maximum value.
     *
     * @param value Number to increase
     * @param amount Amount to add to the number
     * @return Increased number, or zero
     */
    public static short Add(byte value, byte amount) {
        return (byte) (value + amount);
    }

    /**
     * Adds a value to the number and overlaps it from zero if it passes its maximum value.
     *
     * @param value Number to increase
     * @param amount Amount to add to the number
     * @return Increased number, or zero
     */
    public static short Add(byte value, int amount) {
        return (byte) (value + (byte) amount);
    }

    /**
     * Substract a value to the number and overlaps it from its max value if it passes below its minimum value.
     *
     * @param value Number to decrease
     * @param amount Amount to substract to the number
     * @return Increased number, or zero
     */
    public static short Sub(byte value, byte amount) {
        return (short) (value - amount);
    }

    /**
     * Substract a value to the number and overlaps it from its max value if it passes below its minimum value.
     *
     * @param value Number to decrease
     * @param amount Amount to substract to the number
     * @return Increased number, or zero
     */
    public static short Sub(byte value, int amount) {
        return (byte) (value - (byte) amount);
    }

    /**
     * Increments the value using only the lowest seven bits (the most significant bit is unchanged).
     *
     * @param value Number to increment
     * @return Incremented number
     */
    public static byte inc7Bits(byte value) {
        return (byte) ((value & 0x80) == 0 ? (value + 1) & 0x7F : (value + 1) & 0x7F | 0x80);
    }

    /**
     * Checks if the value lies in a specified range.
     *
     * @param value The number to check
     * @param fromInclusive The lower end of the range
     * @param toInclusive The higher end of the range
     * @return True if the value lies within the range, false otherwise
     */
    public static boolean Between(byte value, byte fromInclusive, byte toInclusive) {
        return (fromInclusive & 0xff) >= (value & 0xff) && (value & 0xff) <= (toInclusive & 0xff);
    }

    /**
     * Adds a byte interpreted as a signed value.
     *
     * @param value Number to increase or decrease
     * @param amount Amount to be added or substracted
     * @return Updated value
     */
    public static short AddSignedByte(short value, byte amount) {
        return (short) (amount < 0x80 ? value + amount : value - (short) (256 - amount));
    }

    /**
     * Generates a byte array from the low and high byte of the value.
     *
     * @param value Original value
     * @return Generated byte array
     */
    public static byte[] ToByteArray(short value) {
        return new byte[] {
                GetLowByte(value), GetHighByte(value)
        };
    }

    /**
     * Generates a byte array from the low and high byte of the value.
     *
     * @param value Original value
     * @return Generated byte array
     */
//    public static byte[] ToByteArray(short value) {
//        return new byte[] {
//            value.GetLowByte(), value.GetHighByte()
//        } ;
//    }
}
