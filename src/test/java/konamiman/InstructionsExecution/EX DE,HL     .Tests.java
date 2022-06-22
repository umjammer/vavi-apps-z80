package konamiman.InstructionsExecution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class EX_DE_HL_tests extends InstructionsExecutionTestsBase {

    private static final byte EX_DE_HL_opcode = (byte) 0xEB;

    @Test
    public void EX_DE_HL_exchanges_the_AF_registers() {
        var DE = Fixture.create(Short.TYPE);
        var HL = Fixture.create(Short.TYPE);

        Registers.setDE(DE);
        Registers.setHL(HL);

        Execute(EX_DE_HL_opcode, null);

        assertEquals(HL, Registers.getDE());
        assertEquals(DE, Registers.getHL());
    }

    @Test
    public void EX_DE_HL_does_not_change_flags() {
        AssertNoFlagsAreModified(EX_DE_HL_opcode, null);
    }

    @Test
    public void EX_DE_HL_returns_proper_T_states() {
        var states = Execute(EX_DE_HL_opcode, null);
        assertEquals(4, states);
    }
}