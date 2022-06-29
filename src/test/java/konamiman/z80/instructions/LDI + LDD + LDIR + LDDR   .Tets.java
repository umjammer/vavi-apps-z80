package konamiman.z80.instructions;

import java.util.stream.Stream;

import konamiman.z80.utils.NumberUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.add;
import static konamiman.z80.utils.NumberUtils.dec;
import static konamiman.z80.utils.NumberUtils.getBit;
import static konamiman.z80.utils.NumberUtils.getLowByte;
import static konamiman.z80.utils.NumberUtils.inc;
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
        var oldValue = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        // TODO got error when 1 at (LDI|LDD)
        var srcAddress = createAddressFixture();
        var destAddress = fixture.create(Short.TYPE);

        registers.setHL(srcAddress);
        registers.setDE(destAddress);

        processorAgent.writeToMemory(srcAddress, value);
        processorAgent.writeToMemory(destAddress, oldValue);

        execute(opcode, prefix);

        var newValue = processorAgent.readFromMemory(destAddress);
        assertEquals(value, newValue);
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDIR_Source"})
    public void LDI_LDIR_increase_DE_and_HL(String instr, byte opcode) {
        var srcAddress = fixture.create(Short.TYPE);
        var destAddress = fixture.create(Short.TYPE);

        registers.setHL(srcAddress);
        registers.setDE(destAddress);

        execute(opcode, prefix);

        assertEquals(inc(srcAddress), registers.getHL());
        assertEquals(inc(destAddress), registers.getDE());
    }

    @ParameterizedTest
    @MethodSource({"LDD_Source", "LDDR_Source"})
    public void LDD_LDDR_decreases_DE_and_HL(String instr, byte opcode) {
        var srcAddress = fixture.create(Short.TYPE);
        var destAddress = fixture.create(Short.TYPE);

        registers.setHL(srcAddress);
        registers.setDE(destAddress);

        execute(opcode, prefix);

        assertEquals(dec(srcAddress), registers.getHL());
        assertEquals(dec(destAddress), registers.getDE());
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source", "LDIR_Source", "LDDR_Source"})
    public void LDI_LDD_LDIR_LDDR_decrease_BC(String instr, byte opcode) {
        var counter = fixture.create(Short.TYPE);
        registers.setBC(counter);

        execute(opcode, prefix);

        assertEquals(dec(counter), registers.getBC());
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source", "LDIR_Source", "LDDR_Source"})
    public void LDI_LDD_LDIR_LDDR_do_not_change_S_Z_C(String instr, byte opcode) {
        assertDoesNotChangeFlags(opcode, prefix, "S", "Z", "C");
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source", "LDIR_Source", "LDDR_Source"})
    public void LDI_LDD_LDIR_LDDR_reset_H_N(String instr, byte opcode) {
        assertResetsFlags(opcode, prefix, "H", "N");
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source", "LDIR_Source", "LDDR_Source"})
    public void LDI_LDD_LDIR_LDDR_resets_PF_if_BC_reaches_zero(String instr, byte opcode) {
        registers.setBC((short) 128);
        for (int i = 0; i <= 256; i++) {
            var oldBC = registers.getBC();
            execute(opcode, prefix);
            assertEquals(dec(oldBC), registers.getBC());
            assertEquals(registers.getBC() != 0, registers.getPF().booleanValue());
        }
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source", "LDIR_Source", "LDDR_Source"})
    public void LDI_LDD_LDIR_LDDR_set_Flag3_from_bit_3_of_value_plus_A_and_Flag5_from_bit_1(String instr, byte opcode) {
        var value = fixture.create(Byte.TYPE);
        var srcAddress = fixture.create(Short.TYPE);

        for (int i = 0; i < 256; i++) {
            var valueOfA = (byte) i;
            registers.setA(valueOfA);
            registers.setHL(srcAddress);
            processorAgent.writeToMemory(srcAddress, value);

            execute(opcode, prefix);

            var valuePlusA = getLowByte(add(value, valueOfA));
            assertEquals(getBit(valuePlusA, 3), registers.getFlag3());
            assertEquals(getBit(valuePlusA, 1), registers.getFlag5());
        }
    }

    @ParameterizedTest
    @MethodSource({"LDIR_Source", "LDDR_Source"})
    public void LDIR_LDDR_decrease_PC_by_two_if_counter_does_not_reach_zero(String instr, byte opcode) {
        registers.setBC((short) 128);
        for (int i = 0; i <= 256; i++) {
            var address = fixture.create(Short.TYPE);
            var oldPC = registers.getPC();
            var oldBC = registers.getBC();
            executeAt(address, opcode, prefix);
            assertEquals(dec(oldBC), registers.getBC());
            assertEquals(registers.getBC() == 0 ? add(address, 2) : address, registers.getPC());
        }
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source"})
    public void LDI_LDD_return_proper_T_states(String instr, byte opcode) {
        var states = execute(opcode, prefix);
        assertEquals(16, states);
    }

    @ParameterizedTest
    @MethodSource({"LDIR_Source", "LDDR_Source"})
    public void LDIR_LDDR_return_proper_T_states_depending_of_value_of_BC(String instr, byte opcode) {
        registers.setBC((short) 128);
        for (int i = 0; i <= 256; i++) {
            var oldBC = registers.getBC();
            var states = execute(opcode, prefix);
            assertEquals(dec(oldBC), registers.getBC());
            assertEquals(registers.getBC() == 0 ? 16 : 21, states);
        }
    }
}