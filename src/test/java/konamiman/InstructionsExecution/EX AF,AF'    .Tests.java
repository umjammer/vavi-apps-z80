package konamiman.InstructionsExecution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class EX_AF_AF_tests extends InstructionsExecutionTestsBase {

    private static final byte EX_AF_AF_opcode = 0x08;

    @Test
    public void EX_AF_AF_exchanges_the_AF_registers() {
        var mainValue = Fixture.create(Short.TYPE);
        var alternateValue = Fixture.create(Short.TYPE);

        Registers.setAF(mainValue);
        Registers.getAlternate().setAF(alternateValue);

        Execute(EX_AF_AF_opcode, null);

        assertEquals(alternateValue, Registers.getAF());
        assertEquals(mainValue, Registers.getAlternate().getAF());
    }

    @Test
    public void EX_AF_AF_returns_proper_T_states() {
        var states = Execute(EX_AF_AF_opcode, null);
        assertEquals(4, states);
    }
}
