package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.Dec;
import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static konamiman.DataTypesAndUtils.NumberUtils.ToSignedByte;
import static konamiman.DataTypesAndUtils.NumberUtils.WithBit;
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
        var oldValue = Fixture.create(Byte.TYPE);
        var offset = reg.equals("HL") ? (byte) 0 : Fixture.create(Byte.TYPE);
        var address = Setup(reg, oldValue, offset);

        if (reg.equals("HL"))
            Execute(opcode, prefix);
        else
            Execute(opcode, prefix, offset);

        AssertMemoryContents(address, Dec(oldValue));
    }

    private short Setup(String reg, byte value, byte offset/* = 0*/) {
        var address = Fixture.create(Short.TYPE);
        var actualAddress = Add(address, ToSignedByte(offset));
        ProcessorAgent.getMemory()[actualAddress] = value;
        SetReg(reg, ToShort(address));
        return actualAddress;
    }

    private void AssertMemoryContents(short address, byte expected) {
        assertEquals(expected, ProcessorAgent.getMemory()[address]);
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_sets_SF_appropriately(String reg, byte opcode, Byte prefix) {
        Setup(reg, (byte) 0x02, (byte) 0);

        Execute(opcode, prefix);
        assertEquals(0, Registers.getSF().intValue());

        Execute(opcode, prefix);
        assertEquals(0, Registers.getSF().intValue());

        Execute(opcode, prefix);
        assertEquals(1, Registers.getSF().intValue());

        Execute(opcode, prefix);
        assertEquals(1, Registers.getSF().intValue());
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_sets_ZF_appropriately(String reg, byte opcode, Byte prefix) {
        Setup(reg, (byte) 0x03, (byte) 0);

        Execute(opcode, prefix);
        assertEquals(0, Registers.getZF().intValue());

        Execute(opcode, prefix);
        assertEquals(0, Registers.getZF().intValue());

        Execute(opcode, prefix);
        assertEquals(1, Registers.getZF().intValue());

        Execute(opcode, prefix);
        assertEquals(0, Registers.getZF().intValue());
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_sets_HF_appropriately(String reg, byte opcode, Byte prefix) {
        for (byte b : new byte[] {0x11, (byte) 0x81, (byte) 0xF1}) {
            Setup(reg, b, (byte) 0);

            Execute(opcode, prefix);
            assertEquals(0, Registers.getHF().intValue());

            Execute(opcode, prefix);
            assertEquals(1, Registers.getHF().intValue());

            Execute(opcode, prefix);
            assertEquals(0, Registers.getHF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_sets_PF_appropriately(String reg, byte opcode, Byte prefix) {
        Setup(reg, (byte) 0x81, (byte) 0);

        Execute(opcode, prefix);
        assertEquals(0, Registers.getPF().intValue());

        Execute(opcode, prefix);
        assertEquals(1, Registers.getPF().intValue());

        Execute(opcode, prefix);
        assertEquals(0, Registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_sets_NF(String reg, byte opcode, Byte prefix) {
        AssertSetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_does_not_change_CF(String reg, byte opcode, Byte prefix) {
        var randomValues = Fixture.create(byte[].class);

        for (var value : randomValues) {
            Setup(reg, value, (byte) 0);

            Registers.setCF(Bit.OFF);
            Execute(opcode, prefix);
            assertEquals(0, Registers.getCF().intValue());

            Registers.setCF(Bit.ON);
            Execute(opcode, prefix);
            assertEquals(1, Registers.getCF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_sets_bits_3_and_5_from_result(String reg, byte opcode, Byte prefix) {
        Setup(reg, WithBit(WithBit((byte) 1, 3, 1), 5, 0), (byte) 0);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getFlag3().intValue());
        assertEquals(0, Registers.getFlag5().intValue());

        Setup(reg, WithBit(WithBit((byte) 1, 3, 0), 5, 1), (byte) 0);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getFlag3().intValue());
        assertEquals(1, Registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = Execute(opcode, prefix);
        assertEquals(reg.equals("HL") ? 11 : 23, states);
    }
}

