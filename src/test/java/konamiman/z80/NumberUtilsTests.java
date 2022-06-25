package konamiman.z80;

import konamiman.z80.utils.NumberUtils;
import org.junit.jupiter.api.Test;

import static konamiman.z80.utils.NumberUtils.add;
import static konamiman.z80.utils.NumberUtils.between;
import static konamiman.z80.utils.NumberUtils.createShort;
import static konamiman.z80.utils.NumberUtils.dec;
import static konamiman.z80.utils.NumberUtils.getBit;
import static konamiman.z80.utils.NumberUtils.getHighByte;
import static konamiman.z80.utils.NumberUtils.getLowByte;
import static konamiman.z80.utils.NumberUtils.inc;
import static konamiman.z80.utils.NumberUtils.inc7Bits;
import static konamiman.z80.utils.NumberUtils.setHighByte;
import static konamiman.z80.utils.NumberUtils.setLowByte;
import static konamiman.z80.utils.NumberUtils.sub;
import static konamiman.z80.utils.NumberUtils.toByteArray;
import static konamiman.z80.utils.NumberUtils.withBit;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class NumberUtilsTests {

    @Test
    public void GetHighByte_works_for_short_values_over_8000h() {
        byte expected = (byte) 0xDE;
        var actual = getHighByte((short) 0xDE12);
        assertEquals(expected, actual);
    }

    @Test
    public void GetHighByte_works_for_short_values_under_8000h() {
        byte expected = 0x12;
        var actual = getHighByte((short) 0x12DE);
        assertEquals(expected, actual);
    }

    @Test
    public void SetHighByte_works_for_short_values_over_8000h() {
        var expected = (short) 0xDE12;
        var actual = setHighByte((short) 0xFF12, (byte) 0xDE);
        assertEquals(expected, actual);
    }

    @Test
    public void SetHighByte_works_for_short_values_under_8000() {
        short expected = 0x12DE;
        var actual = setHighByte((short) 0x34DE, (byte) 0x12);
        assertEquals(expected, actual);
    }

    @Test
    public void SetHighByte_works_for_short_values_over_8000() {
        short expected = 0xDE12 - 65536;
        var actual = setHighByte((short) 0x3412, (byte) 0xDE);
        assertEquals(expected, actual);
    }

    @Test
    public void GetLowByte_works_for_shorts() {
        byte expected = (byte) 0xDE;
        var actual = getLowByte((short) 0x12DE);
        assertEquals(expected, actual);
    }

    @Test
    public void GetLowByte_works_for_shorts2() {
        byte expected = (byte) 0xDE;
        var actual = getLowByte((short) 0xFFDE);
        assertEquals(expected, actual);
    }

    @Test
    public void SetLowByte_works_for_shorts() {
        var expected = (short) 0xDE12;
        var actual = setLowByte((short) 0xDEFF, (byte) 0x12);
        assertEquals(expected, actual);
    }

    @Test
    public void SetLowByte_works_for_shorts2() {
        var expected = (short) 0x12DE;
        var actual = setLowByte(((short) 0x12FF), (byte) 0xDE);
        assertEquals(expected, actual);
    }

    @Test
    public void createShort_works_for_high_bytes_under_80h() {
        short expected = (short) 0x12DE;
        var actual = createShort((byte) 0xDE, (byte) 0x12);
        assertEquals(expected, actual);
    }

    @Test
    public void createShort_works_for_high_bytes_over_80h() {
        short expected = 0xDE12 - 65536;
        var actual = createShort((byte) 0x12, (byte) 0xDE);
        assertEquals(expected, actual);
    }

    @Test
    public void GetBit_works_for_lsb() {
        byte lsb0 = (byte) 0xFE;
        assertEquals(0, getBit(lsb0, 0).intValue());

        byte lsb1 = 0x01;
        assertEquals(1, getBit(lsb1, 0).intValue());
    }

    @Test
    public void GetBit_works_for_msb() {
        byte msb0 = (byte) 0x7F;
        assertEquals(0, getBit(msb0, 7).intValue());

        byte msb1 = (byte) 0x80;
        assertEquals(1, getBit(msb1, 7).intValue());
    }

    @Test
    public void GetBit_works_for_middle_bit() {
        byte bit4reset = (byte) (0x10 ^ 0xFF);
        assertEquals(0, getBit(bit4reset, 4).intValue());

        byte bit4set = 0x10;
        assertEquals(1, getBit(bit4set, 4).intValue());
    }

    @Test
    public void GetBit_fails_for_negative_bit_number() {
        assertThrows(IllegalArgumentException.class, () -> getBit((byte) 0, -1));
    }

    @Test
    public void GetBit_fails_for_bit_number_over_7() {
        assertThrows(IllegalArgumentException.class, () -> getBit((byte) 0, 8));
    }

    @Test
    public void SetBit_works_for_lsb() {
        byte lsb0 = (byte) 0xFF;
        assertEquals((byte) 0xFE, withBit(lsb0, 0, 0));

        byte lsb1 = 0x00;
        assertEquals((byte) 0x01, withBit(lsb1, 0, 1));
    }

    @Test
    public void SetBit_works_for_msb() {
        byte msb0 = (byte) 0xFF;
        assertEquals((byte) 0x7F, withBit(msb0, 7, 0));

        byte msb1 = 0x00;
        assertEquals((byte) 0x80, withBit(msb1, 7, 1));
    }

    @Test
    public void SetBit_works_for_middle_bit() {
        byte bit4reset = (byte) 0xFF;
        assertEquals((byte) 0xEF, withBit(bit4reset, 4, 0));

        byte bit4set = 0x00;
        assertEquals((byte) 0x10, withBit(bit4set, 4, 1));
    }

    @Test
    public void SetBit_fails_for_negative_bit_number() {
        assertThrows(IllegalArgumentException.class, () -> withBit((byte) 0, -1, 0));
    }

    @Test
    public void SetBit_fails_for_bit_number_over_7() {
        assertThrows(IllegalArgumentException.class, () -> withBit((byte) 0, 8, 0));
    }

    @Test
    public void ToShort_works_for_numbers_below_8000h() {
        assertEquals((short) 0x1234, (short) 0x1234);
    }

    @Test
    public void ToShort_works_for_numbers_above_8000h() {
        assertEquals((short) -1, (short) 0xFFFF);
    }

    @Test
    public void ToShort_works_for_8000h() {
        assertEquals((short) -32768, (short) 0x8000);
    }

    @Test
    public void ToUshort_works_for_number_below_zero() { // TODO
        assertEquals((short) 0xFFFF, (short) -1);
    }

    @Test
    public void ToUshort_works_for_number_above_zero() { // TODO
        assertEquals((short) 1, (short) 1);
    }

    @Test
    public void ToUshort_works_for_zero() { // TODO
        assertEquals((short) 0, 0);
    }

    @Test
    public void Inc_short_works_for_non_boundary_values() {
        assertEquals(2, inc((short) 1));
    }

    @Test
    public void Inc_short_works_for_boundary_values2() {
        assertEquals(0, inc((short) 0xFFFF));
    }

    @Test
    public void Inc_short_works_for_non_boundary_values2() {
        assertEquals(2, inc((short) 1));
    }

    @Test
    public void Inc_short_works_for_boundary_values() {
        assertEquals(0, inc((short) 0xFFFF));
    }

    @Test
    public void Dec_short_works_for_non_boundary_values() {
        assertEquals(1, dec((short) 2));
    }

    @Test
    public void Dec_short_works_for_boundary_values() {
        assertEquals((short) 0xFFFF, dec((short) 0));
    }

    @Test
    public void Add_short_works_for_non_boundary_values() {
        assertEquals(5, add((short) 2, 3));
    }

    @Test
    public void Add_short_works_for_boundary_values() {
        assertEquals(1, add((short) 0xFFFE, 3));
    }

    @Test
    public void Add_short_works_for_non_boundary_values2() {
        assertEquals(5, add((short) 2, 3));
    }

    @Test
    public void Add_short_works_for_boundary_values2() {
        assertEquals(1, add((short) 0xFFFE, 3));
    }

    @Test
    public void Sub_short_works_for_non_boundary_value2s() {
        assertEquals(2, NumberUtils.sub((short) 5, 3));
    }

    @Test
    public void Sub_short_works_for_boundary_values2() {
        assertEquals((short) 0xFFFE, NumberUtils.sub((short) 1, 3));
    }

    @Test
    public void Sub_short_works_for_non_boundary_values() {
        assertEquals(2, NumberUtils.sub((short) 5, 3));
    }

    @Test
    public void Sub_short_works_for_boundary_values() {
        assertEquals((short) 0xFFFE, NumberUtils.sub((short) 1, 3));
    }

    @Test
    public void Inc_byte_works_for_non_boundary_values() {
        assertEquals(2, inc((byte) 1));
    }

    @Test
    public void Inc_byte_works_for_boundary_values() {
        assertEquals(0, inc((byte) 0xFF));
    }

    @Test
    public void Dec_byte_works_for_non_boundary_values() {
        assertEquals(1, dec((byte) 2));
    }

    @Test
    public void Dec_byte_works_for_boundary_values() {
        assertEquals((short) (byte) 0xFF, dec((byte) 0));
    }

    @Test
    public void Add_byte_works_for_non_boundary_values() {
        assertEquals(5, add((byte) 2, 3));
    }

    @Test
    public void Add_byte_works_for_boundary_values() {
        assertEquals(1, add((byte) 0xFE, 3));
    }

    @Test
    public void Sub_byte_works_for_non_boundary_values() {
        assertEquals(2, sub((byte) 5, 3));
    }

    @Test
    public void Sub_byte_works_for_boundary_values() {
        assertEquals((byte) 0xFE, sub((byte) 1, 3));
    }

    @Test
    public void Test7Bits_works_as_expected() {
        assertEquals(1, inc7Bits((byte) 0));
        assertEquals(0, inc7Bits((byte) 0x7F));
        assertEquals((byte) 0x81, inc7Bits((byte) 0x80));
        assertEquals((byte) 0x80, inc7Bits((byte) 0xFF));
    }

    @Test
    public void Between_works_as_expected() {
        byte value = (byte) 0x80;

        assertFalse(between(value, (byte) 0, (byte) 0x7F));
        assertTrue(between(value, (byte) 0x7F, (byte) 0xFF));
        assertTrue(between(value, (byte) 0x80, (byte) 0xFF));
        assertFalse(between(value, (byte) 0x81, (byte) 0xFF));
    }

    @Test
    public void AddSignedByte_works_for_positive_values() {
        assertEquals((short) 0x8010, add((short) 0x8000, (byte) 0x10));
        assertEquals((short) 0x807F, add((short) 0x8000, (byte) 0x7F));
    }

    @Test
    public void AddSignedByte_works_for_negative_values2() {
        assertEquals((short) 0x7FF0, add(((short) 0x8000), (byte) 0xF0));
        assertEquals((short) 0x7F80, add(((short) 0x8000), (byte) 0x80));
    }

    @Test
    public void ToByteArray_works_for_shorts() {
        short value = 0x1234;

        var actual = toByteArray(value);

        assertArrayEquals(new byte[] {0x34, 0x12}, actual);
    }

    @Test
    public void ToByteArray_works_for_shorts2() {
        short value = 0x1234;

        var actual = toByteArray(value);

        assertArrayEquals(new byte[] {0x34, 0x12}, actual);
    }
}
