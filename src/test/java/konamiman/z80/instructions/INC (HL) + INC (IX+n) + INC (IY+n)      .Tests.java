package konamiman.z80.instructions;

import java.util.stream.Stream;

import konamiman.z80.utils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.add;
import static konamiman.z80.utils.NumberUtils.inc;
import static konamiman.z80.utils.NumberUtils.withBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class INC_aHL_IX_IY_plus_n_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> INC_Source() {
        return Stream.of(
                arguments("HL", (byte) 0x34, null),
                arguments("IX", (byte) 0x34, (byte) 0xDD),
                arguments("IY", (byte) 0x34, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_increases_value_appropriately(String reg, byte opcode, Byte prefix) {
        var oldValue = fixture.create(Byte.TYPE);
        var offset = reg.equals("HL") ? (byte) 0 : fixture.create(Byte.TYPE);
        var address = setup(reg, oldValue, offset);

        if (reg.equals("HL"))
            execute(opcode, prefix);
        else
            execute(opcode, prefix, offset);

        AssertMemoryContents(address, inc(oldValue));
    }

    private short setup(String reg, byte value, byte offset /*= 0*/) {
        // TODO got error when 1 at "increases_value_appropriately" (IX|IY)
        // offset is always 0, then when address is 0, actualAddress becomes 1, so excepts 0 also
        // 2 ???
        var address = createAddressFixture((short) 0, (short) 1, (short) 2);
        var actualAddress = add(address, offset);
        processorAgent.writeToMemory(actualAddress, value);
        setReg(reg, address);
        return actualAddress;
    }

    private void AssertMemoryContents(short address, byte expected) {
        assertEquals(expected, processorAgent.readFromMemory(address));
    }

    @ParameterizedTest
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_sets_SF_appropriately(String reg, byte opcode, Byte prefix) {
        setup(reg, (byte) 0xFD, (byte) 0);

        execute(opcode, prefix);
        assertEquals(1, registers.getSF().intValue());

        execute(opcode, prefix);
        assertEquals(1, registers.getSF().intValue());

        execute(opcode, prefix);
        assertEquals(0, registers.getSF().intValue());

        execute(opcode, prefix);
        assertEquals(0, registers.getSF().intValue());
    }

    @ParameterizedTest
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_sets_ZF_appropriately(String reg, byte opcode, Byte prefix) {
        setup(reg, (byte) 0xFD, (byte) 0);

        execute(opcode, prefix);
        assertEquals(0, registers.getZF().intValue());

        execute(opcode, prefix);
        assertEquals(0, registers.getZF().intValue());

        execute(opcode, prefix);
        assertEquals(1, registers.getZF().intValue());

        execute(opcode, prefix);
        assertEquals(0, registers.getZF().intValue());
    }

    @ParameterizedTest
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_sets_HF_appropriately(String reg, byte opcode, Byte prefix) {
        for (byte b : new byte[] {0x0E, 0x7E, (byte) 0xFE}) {
            setup(reg, b, (byte) 0);

            execute(opcode, prefix);
            assertEquals(0, registers.getHF().intValue());

            execute(opcode, prefix);
            assertEquals(1, registers.getHF().intValue());

            execute(opcode, prefix);
            assertEquals(0, registers.getHF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_sets_PF_appropriately(String reg, byte opcode, Byte prefix) {
        setup(reg, (byte) 0x7E, (byte) 0);

        execute(opcode, prefix);
        assertEquals(0, registers.getPF().intValue());

        execute(opcode, prefix);
        assertEquals(1, registers.getPF().intValue());

        execute(opcode, prefix);
        assertEquals(0, registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_resets_NF(String reg, byte opcode, Byte prefix) {
        assertResetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_does_not_change_CF(String reg, byte opcode, Byte prefix) {
        var randomValues = fixture.create(byte[].class);

        for (var value : randomValues) {
            setup(reg, value, (byte) 0);

            registers.setCF(Bit.OFF);
            execute(opcode, prefix);
            assertEquals(0, registers.getCF().intValue());

            registers.setCF(Bit.ON);
            execute(opcode, prefix);
            assertEquals(1, registers.getCF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_sets_bits_3_and_5_from_result(String reg, byte opcode, Byte prefix) {
        setup(reg, withBit(withBit(((byte) 0), 3, 1), 5, 0), (byte) 0);
        execute(opcode, prefix);
        assertEquals(1, registers.getFlag3().intValue());
        assertEquals(0, registers.getFlag5().intValue());

        setup(reg, withBit(withBit(((byte) 0), 3, 0), 5, 1), (byte) 0);
        execute(opcode, prefix);
        assertEquals(0, registers.getFlag3().intValue());
        assertEquals(1, registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(reg.equals("HL") ? 11 : 23, states);
    }
}
