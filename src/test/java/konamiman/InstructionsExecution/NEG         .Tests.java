package konamiman.InstructionsExecution;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.api.Test;

import static konamiman.DataTypesAndUtils.NumberUtils.Sub;
import static org.junit.jupiter.api.Assertions.assertEquals;


class NEG_tests extends InstructionsExecutionTestsBase {

    private static final byte opcode = 0x44;
    private static final byte prefix = (byte) 0xED;

    @Test
    public void NEG_substracts_A_from_zero() {
        var oldValue = Fixture.create(Byte.TYPE);

        Registers.setA(oldValue);
        Execute();

        var expected = (byte) Sub((byte) 0, oldValue);
        assertEquals(expected, Registers.getA());
    }

    @Test
    public void NEG_sets_SF_appropriately() {
        Registers.setA((byte) 2);
        Execute();
        assertEquals(1, Registers.getSF().intValue());

        Registers.setA((byte) 1);
        Execute();
        assertEquals(1, Registers.getSF().intValue());

        Registers.setA((byte) 0);
        Execute();
        assertEquals(0, Registers.getSF().intValue());

        Registers.setA((byte) 0xFF);
        Execute();
        assertEquals(0, Registers.getSF().intValue());

        Registers.setA((byte) 0x80);
        Execute();
        assertEquals(1, Registers.getSF().intValue());
    }

    @Test
    public void NEG_sets_ZF_appropriately() {
        Registers.setA((byte) 2);
        Execute();
        assertEquals(0, Registers.getZF().intValue());

        Registers.setA((byte) 1);
        Execute();
        assertEquals(0, Registers.getZF().intValue());

        Registers.setA((byte) 0);
        Execute();
        assertEquals(1, Registers.getZF().intValue());

        Registers.setA((byte) 0xFF);
        Execute();
        assertEquals(0, Registers.getZF().intValue());

        Registers.setA((byte) 0x80);
        Execute();
        assertEquals(0, Registers.getZF().intValue());
    }

    @Test
    public void NEG_sets_HF_appropriately() {
        for (int i = 0; i <= 255; i++) {
            var b = (byte) i;
            Registers.setA(b);
            Execute();
            var expected = Bit.of((b ^ Registers.getA()) & 0x10);
            assertEquals(expected, Registers.getHF().intValue());
        }
    }

    @Test
    public void NEG_sets_PF_appropriately() {
        for (int i = 0; i <= 255; i++) {
            var b = (byte) i;
            Registers.setA(b);
            Execute();
            var expected = (b & 0xff) == 0x80 ? 1 : 0;
            assertEquals(expected, Registers.getPF().intValue());
        }
    }

    @Test
    public void NEG_sets_NF() {
        AssertSetsFlags(opcode, prefix, "N");
    }

    @Test
    public void NEG_sets_CF_appropriately() {
        for (int i = 0; i <= 255; i++) {
            var b = (byte) i;
            Registers.setA(b);
            Execute();
            var expected = b == 0 ? 0 : 1;
            assertEquals(expected, Registers.getCF().intValue());
        }
    }

    @Test
    public void NEG_sets_bits_3_and_5_from_result() {
        Registers.setA((byte) 0x0F);
        Execute();
        assertEquals(0, Registers.getFlag3().intValue());
        assertEquals(1, Registers.getFlag5().intValue());

        Registers.setA((byte) 0xF1);
        Execute();
        assertEquals(1, Registers.getFlag3().intValue());
        assertEquals(0, Registers.getFlag5().intValue());
    }

    @Test
    public void NEG_returns_proper_T_states() {
        var states = Execute();
        assertEquals(8, states);
    }

    private int Execute() {
        return Execute(opcode, prefix);
    }
}
