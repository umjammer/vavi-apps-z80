package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.Dec;
import static konamiman.DataTypesAndUtils.NumberUtils.GetBit;
import static konamiman.DataTypesAndUtils.NumberUtils.GetLowByte;
import static konamiman.DataTypesAndUtils.NumberUtils.Inc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class LDI_tests extends InstructionsExecutionTestsBase {

    private static final byte prefix = (byte) 0xED;

    static Stream<Arguments> LDI_Source() {
        return Stream.of(arguments("LDI", (byte) 0xA0));
    }

    static Stream<Arguments> LDD_Source() {
        return Stream.of(arguments("LDD", (byte) 0xA8));
    }

    static Stream<Arguments> LDIR_Source() {
        return Stream.of(arguments("LDI", (byte) 0xB0));
    }

    static Stream<Arguments> LDDR_Source() {
        return Stream.of(arguments("LDD", (byte) 0xB8));
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source", "LDIR_Source", "LDDR_Source"})
    public void LDI_LDD_LDIR_LDDR_copy_value_correctly(String instr, byte opcode) {
        var oldValue = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);
        var srcAddress = Fixture.create(Short.TYPE);
        var destAddress = Fixture.create(Short.TYPE);

        Registers.setHL(srcAddress);
        Registers.setDE(destAddress);

        ProcessorAgent.getMemory()[srcAddress] = value;
        ProcessorAgent.getMemory()[destAddress] = oldValue;

        Execute(opcode, prefix);

        var newValue = ProcessorAgent.getMemory()[destAddress];
        assertEquals(value, newValue);
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDIR_Source"})
    public void LDI_LDIR_increase_DE_and_HL(String instr, byte opcode) {
        var srcAddress = Fixture.create(Short.TYPE);
        var destAddress = Fixture.create(Short.TYPE);

        Registers.setHL(srcAddress);
        Registers.setDE(destAddress);

        Execute(opcode, prefix);

        assertEquals(Inc(srcAddress), Registers.getHL());
        assertEquals(Inc(destAddress), Registers.getDE());
    }

    @ParameterizedTest
    @MethodSource({"LDD_Source", "LDDR_Source"})
    public void LDD_LDDR_decreases_DE_and_HL(String instr, byte opcode) {
        var srcAddress = Fixture.create(Short.TYPE);
        var destAddress = Fixture.create(Short.TYPE);

        Registers.setHL(srcAddress);
        Registers.setDE(destAddress);

        Execute(opcode, prefix);

        assertEquals(Dec(srcAddress), Registers.getHL());
        assertEquals(Dec(destAddress), Registers.getDE());
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source", "LDIR_Source", "LDDR_Source"})
    public void LDI_LDD_LDIR_LDDR_decrease_BC(String instr, byte opcode) {
        var counter = Fixture.create(Short.TYPE);
        Registers.setBC(counter);

        Execute(opcode, prefix);

        assertEquals(Dec(counter), Registers.getBC());
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source", "LDIR_Source", "LDDR_Source"})
    public void LDI_LDD_LDIR_LDDR_do_not_change_S_Z_C(String instr, byte opcode) {
        AssertDoesNotChangeFlags(opcode, prefix, "S", "Z", "C");
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source", "LDIR_Source", "LDDR_Source"})
    public void LDI_LDD_LDIR_LDDR_reset_H_N(String instr, byte opcode) {
        AssertResetsFlags(opcode, prefix, "H", "N");
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source", "LDIR_Source", "LDDR_Source"})
    public void LDI_LDD_LDIR_LDDR_resets_PF_if_BC_reaches_zero(String instr, byte opcode) {
        Registers.setBC((short) 128);
        for (int i = 0; i <= 256; i++) {
            var oldBC = Registers.getBC();
            Execute(opcode, prefix);
            assertEquals(Dec(oldBC), Registers.getBC());
            assertEquals(Registers.getBC() != 0, Registers.getPF().booleanValue());
        }
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source", "LDIR_Source", "LDDR_Source"})
    public void LDI_LDD_LDIR_LDDR_set_Flag3_from_bit_3_of_value_plus_A_and_Flag5_from_bit_1(String instr, byte opcode) {
        var value = Fixture.create(Byte.TYPE);
        var srcAddress = Fixture.create(Short.TYPE);

        for (int i = 0; i < 256; i++) {
            var valueOfA = (byte) i;
            Registers.setA(valueOfA);
            Registers.setHL(srcAddress);
            ProcessorAgent.getMemory()[srcAddress] = value;

            Execute(opcode, prefix);

            var valuePlusA = GetLowByte(Add(value, valueOfA));
            assertEquals(GetBit(valuePlusA, 3), Registers.getFlag3());
            assertEquals(GetBit(valuePlusA, 1), Registers.getFlag5());
        }
    }

    @ParameterizedTest
    @MethodSource({"LDIR_Source", "LDDR_Source"})
    public void LDIR_LDDR_decrease_PC_by_two_if_counter_does_not_reach_zero(String instr, byte opcode) {
        Registers.setBC((short) 128);
        for (int i = 0; i <= 256; i++) {
            var address = Fixture.create(Short.TYPE);
            var oldPC = Registers.getPC();
            var oldBC = Registers.getBC();
            ExecuteAt(address, opcode, prefix);
            assertEquals(Dec(oldBC), Registers.getBC());
            assertEquals(Registers.getBC() == 0 ? Add(address, (short) 2) : address, Registers.getPC());
        }
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source"})
    public void LDI_LDD_return_proper_T_states(String instr, byte opcode) {
        var states = Execute(opcode, prefix);
        assertEquals(16, states);
    }

    @ParameterizedTest
    @MethodSource({"LDIR_Source", "LDDR_Source"})
    public void LDIR_LDDR_return_proper_T_states_depending_of_value_of_BC(String instr, byte opcode) {
        Registers.setBC((short) 128);
        for (int i = 0; i <= 256; i++) {
            var oldBC = Registers.getBC();
            var states = Execute(opcode, prefix);
            assertEquals(Dec(oldBC), Registers.getBC());
            assertEquals(Registers.getBC() == 0 ? 16 : 21, states);
        }
    }
}