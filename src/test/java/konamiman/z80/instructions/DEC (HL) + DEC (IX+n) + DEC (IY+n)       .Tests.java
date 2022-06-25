package konamiman.z80.instructions;

import java.util.stream.Stream;

import konamiman.z80.utils.Bit;
import konamiman.z80.utils.NumberUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.dec;
import static konamiman.z80.utils.NumberUtils.withBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class DEC_aHL_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> DEC_Source() {
        return Stream.of(
                arguments("HL", (byte) 0x35, null),
                arguments("IX", (byte) 0x35, (byte) 0xDD),
                arguments("IY", (byte) 0x35, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_increases_value_appropriately(String reg, byte opcode, Byte prefix) {
        var oldValue = fixture.create(Byte.TYPE);
        var offset = reg.equals("HL") ? (byte) 0 : fixture.create(Byte.TYPE);
        var address = setup(reg, oldValue, offset);

        if (reg.equals("HL"))
            execute(opcode, prefix);
        else
            execute(opcode, prefix, offset);

        assertMemoryContents(address, dec(oldValue));
    }

    private short setup(String reg, byte value, byte offset/* = 0*/) {
        // TODO got error when 1 at (IX|IY)
        var address = fixture.create().inRange(Short.TYPE, (short) 10, Short.MAX_VALUE);
        var actualAddress = NumberUtils.add(address, offset);
        processorAgent.writeToMemory(actualAddress, value);
        setReg(reg, address);
//Debug.printf("reg: %s, value: %d, offset: %d, address: %d", reg, value, offset, address);
        return actualAddress;
    }

    private void assertMemoryContents(short address, byte expected) {
        assertEquals(expected, processorAgent.readFromMemory(address));
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_sets_SF_appropriately(String reg, byte opcode, Byte prefix) {
        setup(reg, (byte) 0x02, (byte) 0);

        execute(opcode, prefix);
        assertEquals(0, registers.getSF().intValue());

        execute(opcode, prefix);
        assertEquals(0, registers.getSF().intValue());

        execute(opcode, prefix);
        assertEquals(1, registers.getSF().intValue());

        execute(opcode, prefix);
        assertEquals(1, registers.getSF().intValue());
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_sets_ZF_appropriately(String reg, byte opcode, Byte prefix) {
        setup(reg, (byte) 0x03, (byte) 0);

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
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_sets_HF_appropriately(String reg, byte opcode, Byte prefix) {
        for (byte b : new byte[] {0x11, (byte) 0x81, (byte) 0xF1}) {
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
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_sets_PF_appropriately(String reg, byte opcode, Byte prefix) {
        setup(reg, (byte) 0x81, (byte) 0);

        execute(opcode, prefix);
        assertEquals(0, registers.getPF().intValue());

        execute(opcode, prefix);
        assertEquals(1, registers.getPF().intValue());

        execute(opcode, prefix);
        assertEquals(0, registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_sets_NF(String reg, byte opcode, Byte prefix) {
        assertSetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_does_not_change_CF(String reg, byte opcode, Byte prefix) {
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
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_sets_bits_3_and_5_from_result(String reg, byte opcode, Byte prefix) {
        setup(reg, withBit(withBit((byte) 1, 3, 1), 5, 0), (byte) 0);
        execute(opcode, prefix);
        assertEquals(1, registers.getFlag3().intValue());
        assertEquals(0, registers.getFlag5().intValue());

        setup(reg, withBit(withBit((byte) 1, 3, 0), 5, 1), (byte) 0);
        execute(opcode, prefix);
        assertEquals(0, registers.getFlag3().intValue());
        assertEquals(1, registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(reg.equals("HL") ? 11 : 23, states);
    }
}

