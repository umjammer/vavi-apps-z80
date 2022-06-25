package konamiman.z80.utils;


import java.util.List;
import java.util.stream.IntStream;


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
    public static byte getHighByte(short value) {
        return (byte) (value >> 8);
    }

    /**
     * Returns a modified version of an ushort number that has
     * the specified value in the high byte.
     *
     * @param value Original number
     * @param highByte New high byte
     *
     * @return Number with the original low byte and the new high byte
     */
    public static short setHighByte(short value, byte highByte) {
        return (short) ((value & 0x00FF) | (highByte << 8));
    }

    /**
     * Gets the low byte of a short value.
     *
     * @param value Number to get the low byte from
     * @return Loq byte of the number
     */
    public static byte getLowByte(short value) {
        return (byte) (value & 0xFF);
    }

    /**
     * Returns a modified version of an ushort number that has
     * the specified value in the low byte.
     *
     * @param value Original number
     * @param lowByte New low byte
     * @return Number with the original high byte and the new low byte
     */
    public static short setLowByte(short value, byte lowByte) {
        return (short) ((value & 0xFF00) | (lowByte & 0xff));
    }

    /**
     * Generates a short number from two bytes.
     *
     * @param lowByte Low byte of the new number
     * @param highByte High byte of the new number
     * @return  Generated number
     */
    public static short createShort(byte lowByte, byte highByte) {
        return (short) ((highByte << 8) | (lowByte & 0xff));
    }

    /**
     * Gets the value of a certain bit in a byte.
     * The rightmost bit has position 0, the leftmost bit has position 7.
     *
     * @param value Number to get the bit from
     * @param bitPosition the bit position to retrieve
     * @return Retrieved bit value
     */
    public static Bit getBit(byte value, int bitPosition) {
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
    public static byte withBit(byte number, int bitPosition, Bit value) {
        if (bitPosition < 0 || bitPosition > 7)
            throw new IllegalArgumentException("bit position must be between 0 and 7");

        if (value.booleanValue()) {
            return (byte) (number | (1 << bitPosition));
        } else {
            return (byte) (number & ~(1 << bitPosition));
        }
    }

    public static byte withBit(byte number, int bitPosition, int value) {
        return withBit(number, bitPosition, Bit.of(value));
    }

    /**
     * Increases a number and turns it into zero if it has its maximum value.
     *
     * @param value Number to increase
     * @return Increased number, or zero
     */
    public static short inc(short value) {
        return (short) (((value & 0xffff) + 1) & 0xffff);
    }

    /**
     * Decreases a number and turns it into max value if it passes under its minimum value.
     *
     * @param value Number to decrease
     * @return Increased number, or zero
     */
    public static short dec(short value) {
        return (short) (((value & 0xffff) - 1) & 0xffff);
    }

    /**
     * Adds a value to a number and overlaps it from zero if it passes its maximum value.
     *
     * @param value Number to increase (unsigned)
     * @param offset (signed)
     * @return Increased number, or zero
     */
    public static short add(short value, int offset) {
        return (short) (((value & 0xffff) + offset) & 0xffff);
    }

    /**
     * Substract a value to a number and overlaps it from its max value if it passes below its minimum value.
     *
     * @param value Number to decrease (unsigned)
     * @param amount signed integer
     * @return Increased number, or zero
     */
    public static short sub(short value, int amount) {
        return (short) (((value & 0xffff) - amount) & 0xffff);
    }

    /**
     * Increases a number and turns it into zero if it has its maximum value.
     *
     * @param value Number to increase
     * @return Increased number, or zero
     */
    public static byte inc(byte value) {
        return (byte) (((value & 0xff) + 1) & 0xff);
    }

    /**
     * Decreases a number and turns it into max value if it passes under its minimum value.
     *
     * @param value Number to decrease
     * @return Increased number, or zero
     */
    public static byte dec(byte value) {
        return (byte) (((value & 0xff) - 1) & 0xff);
    }

    /**
     * Adds a value to the number and overlaps it from zero if it passes its maximum value.
     *
     * @param value Number to increase
     * @param amount Amount to add to the number
     * @return Increased number, or zero
     */
    public static byte add(byte value, int amount) {
        return (byte) (value + (byte) amount);
    }

    /**
     * Substract a value to the number and overlaps it from its max value if it passes below its minimum value.
     *
     * @param value Number to decrease
     * @param amount Amount to substract to the number
     * @return Increased number, or zero
     */
    public static byte sub(byte value, int amount) {
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
    public static boolean between(byte value, byte fromInclusive, byte toInclusive) {
        return (fromInclusive & 0xff) <= (value & 0xff) && (value & 0xff) <= (toInclusive & 0xff);
    }

    /**
     * Generates a byte array from the low and high byte of the value.
     *
     * @param value Original value
     * @return Generated byte array
     */
    public static byte[] toByteArray(short value) {
        return new byte[] {
                getLowByte(value), getHighByte(value)
        };
    }

    /**
     * bit shift right
     * @return int not overflown
     */
    public static int shiftRight(byte b) {
        return ((b & 0xff) >> 1) & 0xff;
    }

    /**
     * bit shift left
     * @return int not overflown
     */
    public static int siftLeft(byte b) {
        return ((b & 0xff) << 1) & 0xff;
    }

    /**
     * bit shift 7 right
     * @return int not overflown
     */
    public static int shift7Right(byte b) {
        return ((b & 0xff) >> 7) & 0xff;
    }

    /**
     * bit shift right
     * @return int not overflown
     */
    public static int shift7Left(byte b) {
        return ((b & 0xff) << 7) & 0xff;
    }

    /** @return int overflown */
    public static int addAsInt(short value, short amount) {
        return (value & 0xffff) + (amount & 0xffff);
    }

    /** @return int overflown */
    public static int addAsInt(byte value, byte amount) {
        return (value & 0xff) + (amount & 0xff);
    }

    /** @return int underflown */
    public static int subAsInt(short value, short amount) {
        return (value & 0xffff) - (amount & 0xffff);
    }

    /** @return int underflown */
    public static int subAsInt(byte value, byte amount) {
        return (value & 0xff) - (amount & 0xff);
    }

    /**
     * Generates a byte array from the byte list.
     *
     * @return Generated byte array
     */
    public static byte[] toByteArray(List<Byte> o) {
        byte[] a = new byte[o.size()];
        IntStream.range(0, o.size()).forEach(i -> a[i] = o.get(i));
        return a;
    }
}
