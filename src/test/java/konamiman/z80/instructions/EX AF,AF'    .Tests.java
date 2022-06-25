package konamiman.z80.instructions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class EX_AF_AF_tests extends InstructionsExecutionTestsBase {

    private static final byte EX_AF_AF_opcode = 0x08;

    @Test
    public void EX_AF_AF_exchanges_the_AF_registers() {
        var mainValue = fixture.create(Short.TYPE);
        var alternateValue = fixture.create(Short.TYPE);

        registers.setAF(mainValue);
        registers.getAlternate().setAF(alternateValue);

        execute(EX_AF_AF_opcode, null);

        assertEquals(alternateValue, registers.getAF());
        assertEquals(mainValue, registers.getAlternate().getAF());
    }

    @Test
    public void EX_AF_AF_returns_proper_T_states() {
        var states = execute(EX_AF_AF_opcode, null);
        assertEquals(4, states);
    }
}
