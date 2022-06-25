package konamiman.z80.instructions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class EXX_tests extends InstructionsExecutionTestsBase {
    private static final byte EXX_opcode = (byte) 0xD9;

    @Test
    public void EXX_exchanges_registers_correctly() {
        var BC = fixture.create(Short.TYPE);
        var DE = fixture.create(Short.TYPE);
        var HL = fixture.create(Short.TYPE);
        var altBC = fixture.create(Short.TYPE);
        var altDE = fixture.create(Short.TYPE);
        var altHL = fixture.create(Short.TYPE);

        registers.setBC(BC);
        registers.setDE(DE);
        registers.setHL(HL);
        registers.getAlternate().setBC(altBC);
        registers.getAlternate().setDE(altDE);
        registers.getAlternate().setHL(altHL);

        execute(EXX_opcode, null);

        assertEquals(altBC, registers.getBC());
        assertEquals(altDE, registers.getDE());
        assertEquals(altHL, registers.getHL());
        assertEquals(BC, registers.getAlternate().getBC());
        assertEquals(DE, registers.getAlternate().getDE());
        assertEquals(HL, registers.getAlternate().getHL());
    }

    @Test
    public void EXX_does_not_change_flags() {
        assertNoFlagsAreModified(EXX_opcode, null);
    }

    @Test
    public void EXX_returns_proper_T_states() {
        var states = execute(EXX_opcode, null);
        assertEquals(4, states);
    }
}