package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.GetBit;
import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static konamiman.StringExtensions.AsBinaryByte;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class RRD_RLD_tests extends InstructionsExecutionTestsBase {

    private static final byte prefix = (byte) 0xED;

    static Stream<Arguments> RRD_RLD_Source() {
        return Stream.of(
                arguments((byte) 0x67, "R"),
                arguments((byte) 0x6F, "L")
        );
    }

    @ParameterizedTest
    @MethodSource("RRD_RLD_Source")
    public void RRD_RLD_moves_data_appropriately(byte opcode, String direction) {
        var oldHLContents = AsBinaryByte("1001 0110");
        var oldAValue = AsBinaryByte("0011 1010");
        var address = Setup(oldHLContents, oldAValue);

        Execute(opcode, prefix);

        byte expectedHLContents, expectedAValue;
        if (direction.equals("R")) {
            expectedHLContents = AsBinaryByte("1010 1001");
            expectedAValue = AsBinaryByte("0011 0110");
        } else {
            expectedHLContents = AsBinaryByte("0110 1010");
            expectedAValue = AsBinaryByte("0011 1001");
        }

        AssertMemoryContents(address, expectedHLContents);
        assertEquals(expectedAValue, Registers.getA());
    }

    private short Setup(byte HLcontents, byte Avalue) {
        Registers.setA(Avalue);
        var address = Fixture.create(Short.TYPE);
        ProcessorAgent.getMemory()[address] = HLcontents;
        Registers.setHL(ToShort(address));
        return address;
    }

    private void AssertMemoryContents(short address, byte expected) {
        assertEquals(expected, ProcessorAgent.getMemory()[address]);
    }

    @ParameterizedTest
    @MethodSource("RRD_RLD_Source")
    public void RRD_RLD_sets_SF_appropriately(byte opcode, String direction) {
        for (int i = 0; i <= 255; i++) {
            var b = (byte) i;
            Registers.setA(b);
            Execute(opcode, prefix);
            assertEquals((b & 0xff) >= 128, Registers.getSF().booleanValue());
        }
    }

    @ParameterizedTest
    @MethodSource("RRD_RLD_Source")
    public void RRD_RLD_sets_ZF_appropriately(byte opcode, String direction) {
        for (int i = 0; i <= 255; i++) {
            var b = (byte) i;
            Setup(b, (byte) 0);
            Execute(opcode, prefix);
            assertEquals(Registers.getA() == 0, Registers.getZF().booleanValue());
        }
    }

    @ParameterizedTest
    @MethodSource("RRD_RLD_Source")
    public void RRD_RLD_resets_HF(byte opcode, String direction) {
        AssertResetsFlags(opcode, prefix, "H");
    }

    @ParameterizedTest
    @MethodSource("RRD_RLD_Source")
    public void RRD_RLD_sets_PF_as_parity(byte opcode, String direction) {
        for (int i = 0; i <= 255; i++) {
            var b = (byte) i;
            Setup(Fixture.create(Byte.TYPE), b);
            Execute(opcode, prefix);
            assertEquals(Parity[Registers.getA()], Registers.getPF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("RRD_RLD_Source")
    public void RRD_RLD_resets_NF(byte opcode, String direction) {
        AssertResetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("RRD_RLD_Source")
    public void RRD_RLD_does_not_chance_CF(byte opcode, String direction) {
        AssertDoesNotChangeFlags(opcode, prefix, "C");
    }

    @ParameterizedTest
    @MethodSource("RRD_RLD_Source")
    public void RRD_RLD_sets_bits_3_and_5_from_result(byte opcode, String direction) {
        for (int i = 0; i <= 255; i++) {
            var b = (byte) i;
            Setup(Fixture.create(Byte.TYPE), b);
            Execute(opcode, prefix);
            assertEquals(GetBit(Registers.getA(), 3), Registers.getFlag3());
            assertEquals(GetBit(Registers.getA(), 5), Registers.getFlag5());
        }
    }

    @ParameterizedTest
    @MethodSource("RRD_RLD_Source")
    public void RRD_RLD_returns_proper_T_states(byte opcode, String direction) {
        var states = Execute(opcode, prefix);
        assertEquals(18, states);
    }
}

