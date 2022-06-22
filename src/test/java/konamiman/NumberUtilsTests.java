package konamiman;

import konamiman.DataTypesAndUtils.NumberUtils;
import org.junit.jupiter.api.Test;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.AddSignedByte;
import static konamiman.DataTypesAndUtils.NumberUtils.Between;
import static konamiman.DataTypesAndUtils.NumberUtils.Dec;
import static konamiman.DataTypesAndUtils.NumberUtils.GetBit;
import static konamiman.DataTypesAndUtils.NumberUtils.GetHighByte;
import static konamiman.DataTypesAndUtils.NumberUtils.GetLowByte;
import static konamiman.DataTypesAndUtils.NumberUtils.Inc;
import static konamiman.DataTypesAndUtils.NumberUtils.SetHighByte;
import static konamiman.DataTypesAndUtils.NumberUtils.SetLowByte;
import static konamiman.DataTypesAndUtils.NumberUtils.Sub;
import static konamiman.DataTypesAndUtils.NumberUtils.ToByteArray;
import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static konamiman.DataTypesAndUtils.NumberUtils.ToSignedByte;
import static konamiman.DataTypesAndUtils.NumberUtils.ToUShort;
import static konamiman.DataTypesAndUtils.NumberUtils.WithBit;
import static konamiman.DataTypesAndUtils.NumberUtils.inc7Bits;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class NumberUtilsTests {

    @Test
    public void GetHighByte_works_for_short_values_over_8000h() {
        byte expected = (byte) 0xDE;
        var actual = GetHighByte((short) 0xDE12);
        assertEquals(expected, actual);
    }

    @Test
    public void GetHighByte_works_for_short_values_under_8000h() {
        byte expected = 0x12;
        var actual = GetHighByte((short) 0x12DE);
        assertEquals(expected, actual);
    }

    @Test
    public void SetHighByte_works_for_short_values_over_8000h() {
        var expected = (short) 0xDE12;
        var actual = SetHighByte((short) 0xFF12, (byte) 0xDE);
        assertEquals(expected, actual);
    }

    @Test
    public void SetHighByte_works_for_short_values_under_8000() {
        short expected = 0x12DE;
        var actual = SetHighByte((short) 0x34DE, (byte) 0x12);
        assertEquals(expected, actual);
    }

    @Test
    public void SetHighByte_works_for_short_values_over_8000() {
        short expected = 0xDE12 - 65536;
        var actual = SetHighByte((short) 0x3412, (byte) 0xDE);
        assertEquals(expected, actual);
    }

    @Test
    public void GetLowByte_works_for_shorts() {
        byte expected = (byte) 0xDE;
        var actual = GetLowByte((short) 0x12DE);
        assertEquals(expected, actual);
    }

    @Test
    public void GetLowByte_works_for_shorts2() {
        byte expected = (byte) 0xDE;
        var actual = GetLowByte((short) 0xFFDE);
        assertEquals(expected, actual);
    }

    @Test
    public void SetLowByte_works_for_shorts() {
        var expected = (short) 0xDE12;
        var actual = SetLowByte((short) 0xDEFF, (byte) 0x12);
        assertEquals(expected, actual);
    }

    @Test
    public void SetLowByte_works_for_shorts2() {
        var expected = (short) 0x12DE;
        var actual = SetLowByte(((short) 0x12FF), (byte) 0xDE);
        assertEquals(expected, actual);
    }

    @Test
    public void createShort_works_for_high_bytes_under_80h() {
        short expected = 0x12DE;
        var actual = NumberUtils.createShort((byte) 0xDE, (byte) 0x12);
        assertEquals(expected, actual);
    }

    @Test
    public void createShort_works_for_high_bytes_over_80h() {
        short expected = 0xDE12 - 65536;
        var actual = NumberUtils.createShort((byte) 0x12, (byte) 0xDE);
        assertEquals(expected, actual);
    }

    @Test
    public void GetBit_works_for_lsb() {
        byte lsb0 = (byte) 0xFE;
        assertEquals(0, GetBit(lsb0, 0).intValue());

        byte lsb1 = 0x01;
        assertEquals(1, GetBit(lsb1, 0).intValue());
    }

    @Test
    public void GetBit_works_for_msb() {
        byte msb0 = (byte) 0x7F;
        assertEquals(0, GetBit(msb0, 7).intValue());

        byte msb1 = (byte) 0x80;
        assertEquals(1, GetBit(msb1, 7).intValue());
    }

    @Test
    public void GetBit_works_for_middle_bit() {
        byte bit4reset = (byte) (0x10 ^ 0xFF);
        assertEquals(0, GetBit(bit4reset, 4).intValue());

        byte bit4set = 0x10;
        assertEquals(1, GetBit(bit4set, 4).intValue());
    }

    @Test
    public void GetBit_fails_for_negative_bit_number() {
        assertThrows(IllegalArgumentException.class, () -> GetBit((byte) 0, -1));
    }

    @Test
    public void GetBit_fails_for_bit_number_over_7() {
        assertThrows(IllegalArgumentException.class, () -> GetBit((byte) 0, 8));
    }

    @Test
    public void SetBit_works_for_lsb() {
        byte lsb0 = (byte) 0xFF;
        assertEquals(0xFE, WithBit(lsb0, 0, 0));

        byte lsb1 = 0x00;
        assertEquals(0x01, WithBit(lsb1, 0, 1));
    }

    @Test
    public void SetBit_works_for_msb() {
        byte msb0 = (byte) 0xFF;
        assertEquals(0x7F, WithBit(msb0, 7, 0));

        byte msb1 = 0x00;
        assertEquals(0x80, WithBit(msb1, 7, 1));
    }

    @Test
    public void SetBit_works_for_middle_bit() {
        byte bit4reset = (byte) 0xFF;
        assertEquals(0xEF, WithBit(bit4reset, 4, 0));

        byte bit4set = 0x00;
        assertEquals(0x10, WithBit(bit4set, 4, 1));
    }

    @Test
    public void SetBit_fails_for_negative_bit_number() {
        assertThrows(IllegalArgumentException.class, () -> WithBit((byte) 0, -1, 0));
    }

    @Test
    public void SetBit_fails_for_bit_number_over_7() {
        assertThrows(IllegalArgumentException.class, () -> WithBit((byte) 0, 8, 0));
    }

    @Test
    public void ToShort_works_for_numbers_below_8000h() {
        assertEquals((short) 0x1234, ToShort(0x1234));
    }

    @Test
    public void ToShort_works_for_numbers_above_8000h() {
        assertEquals((short) -1, ToShort(0xFFFF));
    }

    @Test
    public void ToShort_works_for_8000h() {
        assertEquals((short) -32768, ToShort(0x8000));
    }

    @Test
    public void ToUshort_works_for_number_below_zero() {
        assertEquals((short) 0xFFFF, ToUShort(-1));
    }

    @Test
    public void ToUshort_works_for_number_above_zero() {
        assertEquals((short) 1, ToUShort(1));
    }

    @Test
    public void ToUshort_works_for_zero() {
        assertEquals((short) 0, ToUShort(0));
    }

    @Test
    public void ToSignedByte_works_for_numbers_below_80h() {
        assertEquals((byte) 0x12, ToSignedByte(0x12));
    }

    @Test
    public void ToSignedByte_works_for_80h() {
        assertEquals((byte) -128, ToSignedByte(0x80));
    }

    @Test
    public void ToSignedByte_works_for_numbers_above_80h() {
        assertEquals((byte) -1, ToSignedByte(0xFF));
    }

    @Test
    public void Inc_short_works_for_non_boundary_values() {
        assertEquals(2, Inc((short) 1));
    }

    @Test
    public void Inc_short_works_for_boundary_values2() {
        assertEquals(0, Inc(ToShort(0xFFFF)));
    }

    @Test
    public void Inc_short_works_for_non_boundary_values2() {
        assertEquals(2, Inc((short) 1));
    }

    @Test
    public void Inc_short_works_for_boundary_values() {
        assertEquals(0, Inc(ToUShort(0xFFFF)));
    }

    @Test
    public void Dec_short_works_for_non_boundary_values() {
        assertEquals(1, Dec((short) 2));
    }

    @Test
    public void Dec_short_works_for_boundary_values() {
        assertEquals(ToShort(0xFFFF), Dec((short) 0));
    }

    @Test
    public void Add_short_works_for_non_boundary_values() {
        assertEquals(5, Add((short) 2, (short) 3));
    }

    @Test
    public void Add_short_works_for_boundary_values() {
        assertEquals(1, Add(ToUShort(0xFFFE), (short) 3));
    }

    @Test
    public void Add_short_works_for_non_boundary_values2() {
        assertEquals(5, Add((short) 2, (short) 3));
    }

    @Test
    public void Add_short_works_for_boundary_values2() {
        assertEquals(1, Add(ToShort(0xFFFE), (short) 3));
    }

    @Test
    public void Sub_short_works_for_non_boundary_value2s() {
        assertEquals(2, Sub((short) 5, (short) 3));
    }

    @Test
    public void Sub_short_works_for_boundary_values2() {
        assertEquals(ToShort(0xFFFE), Sub((short) 1, (short) 3));
    }

    @Test
    public void Sub_short_works_for_non_boundary_values() {
        assertEquals(2, Sub((short) 5, (short) 3));
    }

    @Test
    public void Sub_short_works_for_boundary_values() {
        assertEquals(ToUShort(0xFFFE), Sub((short) 1, (short) 3));
    }

    @Test
    public void Inc_byte_works_for_non_boundary_values() {
        assertEquals(2, Inc((byte) 1));
    }

    @Test
    public void Inc_byte_works_for_boundary_values() {
        assertEquals(0, Inc((byte) 0xFF));
    }

    @Test
    public void Dec_byte_works_for_non_boundary_values() {
        assertEquals(1, Dec((byte) 2));
    }

    @Test
    public void Dec_byte_works_for_boundary_values() {
        assertEquals(ToShort((short) 0xFF), Dec((byte) 0));
    }

    @Test
    public void Add_byte_works_for_non_boundary_values() {
        assertEquals(5, Add((byte) 2, 3));
    }

    @Test
    public void Add_byte_works_for_boundary_values() {
        assertEquals(1, Add((byte) 0xFE, 3));
    }

    @Test
    public void Sub_byte_works_for_non_boundary_values() {
        assertEquals(2, Sub((byte) 5, 3));
    }

    @Test
    public void Sub_byte_works_for_boundary_values() {
        assertEquals(ToShort((short) 0xFE), Sub((byte) 1, 3));
    }

    @Test
    public void Test7Bits_works_as_expected() {
        assertEquals(1, inc7Bits((byte) 0));
        assertEquals(0, inc7Bits((byte) 0x7F));
        assertEquals(0x81, inc7Bits((byte) 0x80));
        assertEquals(0x80, inc7Bits((byte) 0xFF));
    }

    @Test
    public void Between_works_as_expected() {
        byte value = (byte) 0x80;

        assertFalse(Between(value, (byte) 0, (byte) 0x7F));
        assertTrue(Between(value, (byte) 0x7F, (byte) 0xFF));
        assertTrue(Between(value, (byte) 0x80, (byte) 0xFF));
        assertFalse(Between(value, (byte) 0x81, (byte) 0xFF));
    }

    @Test
    public void AddSignedByte_works_for_positive_values() {
        assertEquals(0x8010, AddSignedByte((short) 0x8000, (byte) 0x10));
        assertEquals(0x807F, AddSignedByte((short) 0x8000, (byte) 0x7F));
    }

    @Test
    public void AddSignedByte_works_for_negative_values2() {
        assertEquals(0x7FF0, AddSignedByte(((short) 0x8000), (byte) 0xF0));
        assertEquals(0x7F80, AddSignedByte(((short) 0x8000), (byte) 0x80));
    }

    @Test
    public void ToByteArray_works_for_shorts() {
        short value = 0x1234;

        var actual = ToByteArray(value);

        assertArrayEquals(new byte[] {0x34, 0x12}, actual);
    }

    @Test
    public void ToByteArray_works_for_shorts2() {
        short value = 0x1234;

        var actual = ToByteArray(value);

        assertArrayEquals(new byte[] {0x34, 0x12}, actual);
    }
}
