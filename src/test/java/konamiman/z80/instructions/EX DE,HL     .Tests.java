package konamiman.z80.instructions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class EX_DE_HL_tests extends InstructionsExecutionTestsBase {

    private static final byte EX_DE_HL_opcode = (byte) 0xEB;

    @Test
    public void EX_DE_HL_exchanges_the_AF_registers() {
        var DE = fixture.create(Short.TYPE);
        var HL = fixture.create(Short.TYPE);

        registers.setDE(DE);
        registers.setHL(HL);

        execute(EX_DE_HL_opcode, null);

        assertEquals(HL, registers.getDE());
        assertEquals(DE, registers.getHL());
    }

    @Test
    public void EX_DE_HL_does_not_change_flags() {
        assertNoFlagsAreModified(EX_DE_HL_opcode, null);
    }

    @Test
    public void EX_DE_HL_returns_proper_T_states() {
        var states = execute(EX_DE_HL_opcode, null);
        assertEquals(4, states);
    }
}