package konamiman.z80.instructions;

import konamiman.z80.utils.Bit;
import org.junit.jupiter.api.Test;

import static konamiman.z80.utils.NumberUtils.sub;
import static org.junit.jupiter.api.Assertions.assertEquals;


class NEG_tests extends InstructionsExecutionTestsBase {

    private static final byte opcode = 0x44;
    private static final byte prefix = (byte) 0xED;

    @Test
    public void NEG_subtracts_A_from_zero() {
        var oldValue = fixture.create(Byte.TYPE);

        registers.setA(oldValue);
        execute();

        var expected = sub((byte) 0, oldValue);
        assertEquals(expected, registers.getA());
    }

    @Test
    public void NEG_sets_SF_appropriately() {
        registers.setA((byte) 2);
        execute();
        assertEquals(1, registers.getSF().intValue());

        registers.setA((byte) 1);
        execute();
        assertEquals(1, registers.getSF().intValue());

        registers.setA((byte) 0);
        execute();
        assertEquals(0, registers.getSF().intValue());

        registers.setA((byte) 0xFF);
        execute();
        assertEquals(0, registers.getSF().intValue());

        registers.setA((byte) 0x80);
        execute();
        assertEquals(1, registers.getSF().intValue());
    }

    @Test
    public void NEG_sets_ZF_appropriately() {
        registers.setA((byte) 2);
        execute();
        assertEquals(0, registers.getZF().intValue());

        registers.setA((byte) 1);
        execute();
        assertEquals(0, registers.getZF().intValue());

        registers.setA((byte) 0);
        execute();
        assertEquals(1, registers.getZF().intValue());

        registers.setA((byte) 0xFF);
        execute();
        assertEquals(0, registers.getZF().intValue());

        registers.setA((byte) 0x80);
        execute();
        assertEquals(0, registers.getZF().intValue());
    }

    @Test
    public void NEG_sets_HF_appropriately() {
        for (int i = 0; i <= 255; i++) {
            var b = (byte) i;
            registers.setA(b);
            execute();
            var expected = Bit.of((b ^ registers.getA()) & 0x10);
            assertEquals(expected, registers.getHF());
        }
    }

    @Test
    public void NEG_sets_PF_appropriately() {
        for (int i = 0; i <= 255; i++) {
            var b = (byte) i;
            registers.setA(b);
            execute();
            var expected = (b & 0xff) == 0x80 ? 1 : 0;
            assertEquals(expected, registers.getPF().intValue());
        }
    }

    @Test
    public void NEG_sets_NF() {
        assertSetsFlags(opcode, prefix, "N");
    }

    @Test
    public void NEG_sets_CF_appropriately() {
        for (int i = 0; i <= 255; i++) {
            var b = (byte) i;
            registers.setA(b);
            execute();
            var expected = b == 0 ? 0 : 1;
            assertEquals(expected, registers.getCF().intValue());
        }
    }

    @Test
    public void NEG_sets_bits_3_and_5_from_result() {
        registers.setA((byte) 0x0F);
        execute();
        assertEquals(0, registers.getFlag3().intValue());
        assertEquals(1, registers.getFlag5().intValue());

        registers.setA((byte) 0xF1);
        execute();
        assertEquals(1, registers.getFlag3().intValue());
        assertEquals(0, registers.getFlag5().intValue());
    }

    @Test
    public void NEG_returns_proper_T_states() {
        var states = execute();
        assertEquals(8, states);
    }

    private int execute() {
        return execute(opcode, prefix);
    }
}
