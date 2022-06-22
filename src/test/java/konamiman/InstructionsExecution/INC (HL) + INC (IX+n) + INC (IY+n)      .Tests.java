package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.Inc;
import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static konamiman.DataTypesAndUtils.NumberUtils.ToSignedByte;
import static konamiman.DataTypesAndUtils.NumberUtils.WithBit;
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
        var oldValue = Fixture.create(Byte.TYPE);
        var offset = reg.equals("HL") ? (byte) 0 : Fixture.create(Byte.TYPE);
        var address = Setup(reg, oldValue, offset);

        if (reg.equals("HL"))
            Execute(opcode, prefix);
        else
            Execute(opcode, prefix, offset);

        AssertMemoryContents(address, Inc(oldValue));
    }

    private short Setup(String reg, byte value, byte offset /*= 0*/) {
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
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_sets_SF_appropriately(String reg, byte opcode, Byte prefix) {
        Setup(reg, (byte) 0xFD, (byte) 0);

        Execute(opcode, prefix);
        assertEquals(1, Registers.getSF().intValue());

        Execute(opcode, prefix);
        assertEquals(1, Registers.getSF().intValue());

        Execute(opcode, prefix);
        assertEquals(0, Registers.getSF().intValue());

        Execute(opcode, prefix);
        assertEquals(0, Registers.getSF().intValue());
    }

    @ParameterizedTest
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_sets_ZF_appropriately(String reg, byte opcode, Byte prefix) {
        Setup(reg, (byte) 0xFD, (byte) 0);

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
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_sets_HF_appropriately(String reg, byte opcode, Byte prefix) {
        for (byte b : new byte[] {0x0E, 0x7E, (byte) 0xFE}) {
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
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_sets_PF_appropriately(String reg, byte opcode, Byte prefix) {
        Setup(reg, (byte) 0x7E, (byte) 0);

        Execute(opcode, prefix);
        assertEquals(0, Registers.getPF().intValue());

        Execute(opcode, prefix);
        assertEquals(1, Registers.getPF().intValue());

        Execute(opcode, prefix);
        assertEquals(0, Registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_resets_NF(String reg, byte opcode, Byte prefix) {
        AssertResetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_does_not_change_CF(String reg, byte opcode, Byte prefix) {
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
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_sets_bits_3_and_5_from_result(String reg, byte opcode, Byte prefix) {
        Setup(reg, WithBit(WithBit(((byte) 0), 3, 1), 5, 0), (byte) 0);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getFlag3().intValue());
        assertEquals(0, Registers.getFlag5().intValue());

        Setup(reg, WithBit(WithBit(((byte) 0), 3, 0), 5, 1), (byte) 0);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getFlag3().intValue());
        assertEquals(1, Registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = Execute(opcode, prefix);
        assertEquals(reg.equals("HL") ? 11 : 23, states);
    }
}
