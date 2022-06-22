package konamiman.InstructionsExecution;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static org.junit.jupiter.api.Assertions.assertEquals;


class LD_arr_r_tests extends InstructionsExecutionTestsBase {

    static Object[] LD_rr_r_Source = {
            new Object[] {"BC", "A", (byte) 0x02},
            new Object[] {"DE", "A", (byte) 0x12},
            new Object[] {"HL", "A", (byte) 0x77},
            new Object[] {"HL", "B", (byte) 0x70},
            new Object[] {"HL", "C", (byte) 0x71},
            new Object[] {"HL", "D", (byte) 0x72},
            new Object[] {"HL", "E", (byte) 0x73},
            new Object[] {"HL", "H", (byte) 0x74},
            new Object[] {"HL", "L", (byte) 0x75}
    };

    @ParameterizedTest
    @MethodSource("LD_rr_r_Source")
    public void LD_arr_r_loads_value_in_memory(String destPointerReg, String srcReg, byte opcode) {
        var isHorL = (srcReg.equals("H") || srcReg.equals("L"));

        var address = Fixture.create(Short.TYPE);
        var oldValue = Fixture.create(Byte.TYPE);
        var newValue = Fixture.create(Byte.TYPE);

        SetReg(destPointerReg, ToShort(address));
        ProcessorAgent.getMemory()[address] = oldValue;
        if (!isHorL)
            SetReg(srcReg, newValue);

        Sut.execute(opcode);

        var expected = isHorL ? this.<Byte>GetReg(srcReg) : newValue;
        assertEquals(expected, ProcessorAgent.getMemory()[address]);
    }

    @ParameterizedTest
    @MethodSource("LD_rr_r_Source")
    public void LD_rr_r_do_not_modify_flags(String destPointerReg, String srcReg, byte opcode) {
        AssertNoFlagsAreModified(opcode, null);
    }

    @ParameterizedTest
    @MethodSource("LD_rr_r_Source")
    public void LD_rr_r_returns_proper_T_states(String destPointerReg, String srcReg, byte opcode) {
        var states = Execute(opcode, null);
        assertEquals(7, states);
    }
}
