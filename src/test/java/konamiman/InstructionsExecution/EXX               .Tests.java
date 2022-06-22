package konamiman.InstructionsExecution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class EXX_tests extends InstructionsExecutionTestsBase {
    private static final byte EXX_opcode = (byte) 0xD9;

    @Test
    public void EXX_exchanges_registers_correctly() {
        var BC = Fixture.create(Short.TYPE);
        var DE = Fixture.create(Short.TYPE);
        var HL = Fixture.create(Short.TYPE);
        var altBC = Fixture.create(Short.TYPE);
        var altDE = Fixture.create(Short.TYPE);
        var altHL = Fixture.create(Short.TYPE);

        Registers.setBC(BC);
        Registers.setDE(DE);
        Registers.setHL(HL);
        Registers.getAlternate().setBC(altBC);
        Registers.getAlternate().setDE(altDE);
        Registers.getAlternate().setHL(altHL);

        Execute(EXX_opcode, null);

        assertEquals(altBC, Registers.getBC());
        assertEquals(altDE, Registers.getDE());
        assertEquals(altHL, Registers.getHL());
        assertEquals(BC, Registers.getAlternate().getBC());
        assertEquals(DE, Registers.getAlternate().getDE());
        assertEquals(HL, Registers.getAlternate().getHL());
    }

    @Test
    public void EXX_does_not_change_flags() {
        AssertNoFlagsAreModified(EXX_opcode, null);
    }

    @Test
    public void EXX_returns_proper_T_states() {
        var states = Execute(EXX_opcode, null);
        assertEquals(4, states);
    }
}