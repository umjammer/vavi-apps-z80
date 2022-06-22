package konamiman;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class BitTests {

    @Test
    public void Default_constructor_creates_instance_with_value_zero() {
        assertEquals(0, Bit.of(0).intValue());
    }

    @Test
    public void Can_implicitly_convert_to_int() {
        int theInt = Bit.of(0).intValue();
        assertEquals(0, theInt);

        theInt = Bit.of(1).intValue();
        assertEquals(1, theInt);
    }

    @Test
    public void Can_implicitly_convert_from_zero_int_to_zero_bit() {
        Bit theBit = Bit.of(0);
        assertEquals(theBit.intValue(), 0);
    }

    @Test
    public void Can_implicitly_convert_from_any_nonzero_int_to_one_bit() {
        Bit theBit = Bit.of(34);
        assertEquals(theBit.intValue(), 1);
    }

    @Test
    public void Can_implicity_convert_to_and_from_boolean() {
        assertTrue(Bit.of(1).booleanValue());
        assertFalse(Bit.of(0).booleanValue());

        Bit theTrue = Bit.of(true);
        Bit theFalse = Bit.of(false);
        assertEquals(1, theTrue.intValue());
        assertEquals(0, theFalse.intValue());
    }

    @Test
    public void Can_create_one_instance_from_another() {
        Bit zero = Bit.of(0);
        Bit one = Bit.of(1);

        assertEquals(zero, Bit.of(zero));
        assertEquals(one, Bit.of(one));
    }

    @Test
    public void Can_compare_to_other_bit_with_equals_sign() {
        assertNotSame(Bit.of(0), Bit.of(0));
        assertNotSame(Bit.of(1), Bit.of(1));
        assertNotSame(Bit.of(0), Bit.of(1));
    }

    @Test
    public void Can_compare_to_other_bit_with_equals_method() {
        assertEquals(Bit.of(0), Bit.of(0));
        assertEquals(Bit.of(1), Bit.of(1));
        assertNotEquals(Bit.of(0), Bit.of(1));
    }

    @Test
    public void Can_compare_for_equality_to_zero_int() {
        int zero = 0;
        int nonZero = 34;

        assertTrue(Bit.of(0).operatorEquals(zero));
        assertTrue(Bit.of(0).equals(zero));

        assertTrue(Bit.of(zero).equals(Bit.of(0)));
//        assertTrue(Bit.of(zero).eqials(Bit.of(0)));

        assertFalse(Bit.of(0).operatorEquals(nonZero));
        assertFalse(Bit.of(0).equals(nonZero));

        assertFalse(Bit.of(nonZero).equals(Bit.of(0)));
//        assertFalse(nonZero.equals(Bit.of(0)));
    }

    @Test
    public void Can_compare_for_equality_to_non_zero_int() {
        int zero = 0;
        int nonZero = 34;

        assertTrue(Bit.of(1).operatorEquals(nonZero));
        assertTrue(Bit.of(1).equals(nonZero));

        assertTrue(Bit.of(nonZero).equals(Bit.of(1)));
        //assertTrue(nonZero.equals(Bit.of(1)));

        assertFalse(Bit.of(1).operatorEquals(zero));
        assertFalse(Bit.of(1).equals(zero));

        assertFalse(Bit.of(zero).equals(Bit.of(1)));
        //assertFalse(zero.equals(Bit.of(1)));
    }

    @Test
    public void Can_compare_for_inequality_to_zero_int() {
        int zero = 0;
        int nonZero = 34;

        assertTrue(Bit.of(1).operatorNotEqual(zero));
        assertTrue(!Bit.of(zero).equals(Bit.of(1).intValue()));

        assertFalse(Bit.of(1).operatorNotEqual(nonZero));
        assertFalse(!Bit.of(nonZero).equals(Bit.of(1)));
    }

    @Test
    public void Can_convert_to_boolean() {
        assertFalse(Bit.of(0).booleanValue());
        assertTrue(Bit.of(1).booleanValue());
    }

    @Test
    public void Can_do_arithmetic_OR() {
        Bit zero = Bit.OFF;
        Bit one = Bit.ON;

        assertFalse(zero.operatorOR(zero).booleanValue());
        assertTrue(zero.operatorOR(one).booleanValue());
        assertTrue(one.operatorOR(zero).booleanValue());
        assertTrue(one.operatorOR(one).booleanValue());
    }

    @Test
    public void Can_do_logical_OR() {
        Bit zero = Bit.OFF;
        Bit one = Bit.ON;

        assertFalse(zero.booleanValue() || zero.booleanValue());
        assertTrue(zero.booleanValue() || one.booleanValue());
        assertTrue(one.booleanValue() || zero.booleanValue());
        assertTrue(one.booleanValue() || one.booleanValue());
    }

    @Test
    public void Can_do_arithmetic_AND() {
        Bit zero = Bit.OFF;
        Bit one = Bit.ON;

        assertFalse(zero.operatorAND(zero).booleanValue());
        assertFalse(zero.operatorAND(one).booleanValue());
        assertFalse(one.operatorAND(zero).booleanValue());
        assertTrue(one.operatorAND(one).booleanValue());
    }

    @Test
    public void Can_do_logical_AND() {
        Bit zero = Bit.OFF;
        Bit one = Bit.ON;

        assertFalse(zero.booleanValue() && zero.booleanValue());
        assertFalse(zero.booleanValue() && one.booleanValue());
        assertFalse(one.booleanValue() && zero.booleanValue());
        assertTrue(one.booleanValue() && one.booleanValue());
    }

    @Test
    public void Can_do_arithmetic_XOR() {
        Bit zero = Bit.OFF;
        Bit one = Bit.ON;

        assertFalse(zero.operatorXOR(zero).booleanValue());
        assertTrue(zero.operatorXOR(one).booleanValue());
        assertTrue(one.operatorXOR(zero).booleanValue());
        assertFalse(one.operatorXOR(one).booleanValue());
    }

    @Test
    public void Can_do_arithmetic_NOT() {
        Bit zero = Bit.OFF;
        Bit one = Bit.ON;

        assertEquals(zero, one.operatorNEG());
        assertEquals(one, zero.operatorNEG());
    }

    @Test
    public void Can_do_logical_NOT() {
        Bit zero = Bit.OFF;
        Bit one = Bit.ON;

        assertEquals(one, zero.operatorNOT());
        assertEquals(zero, one.operatorNOT());
    }
}

