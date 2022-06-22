package konamiman.InstructionsExecution;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static konamiman.DataTypesAndUtils.NumberUtils.ToSignedByte;
import static konamiman.DataTypesAndUtils.NumberUtils.WithBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class XOR_r_tests extends InstructionsExecutionTestsBase {

    public static List<Arguments> XOR_r;

    static {
        var combinations = new ArrayList<Arguments>();

        var registers = new String[] {"B", "C", "D", "E", "H", "L", "(HL)", "n", "IXH", "IXL", "IYH", "IYL", "(IX+n)", "(IY+n)"};
        for (var src = 0; src < registers.length; src++) {
            var reg = registers[src];
            var i = new int[] {src};
            Byte[] prefix = new Byte[1];

            ModifyTestCaseCreationForIndexRegs(reg, /* ref */i, /* out */prefix);

            var opcode = (byte) (i[0] == 7 ? 0xEE : (i[0] | 0xA8));
            combinations.add(arguments(reg, opcode, prefix[0]));
        }

        XOR_r = combinations;
    }

    static Stream<Arguments> XOR_r_Source() {
        return XOR_r.stream();
    }

    static Stream<Arguments> XOR_A_Source() {
        return Stream.of(
                arguments("A", (byte) 0xAF, null)
        );
    }

    @ParameterizedTest
    @MethodSource("XOR_r_Source")
    public void XOR_r_xors_both_registers(String src, byte opcode, Byte prefix)
    {
        var oldValue = Fixture.create(Byte.TYPE);
        var valueToXor = Fixture.create(Byte.TYPE);

        Setup(src, oldValue, valueToXor);
        Execute(opcode, prefix);

        assertEquals(oldValue ^ valueToXor, Registers.getA());
    }

    @ParameterizedTest
    @MethodSource("XOR_A_Source")
    public void XOR_A_resets_A(String src, byte opcode, Byte prefix)
    {
        var value = Fixture.create(Byte.TYPE);

        Registers.setA(value);
        Execute(opcode, prefix);

        assertEquals(0, Registers.getA());
    }

    private void Setup(String src, byte oldValue, byte valueToXor)
    {
        Registers.setA(oldValue);

        if(src.equals("n"))
        {
            SetMemoryContentsAt((short) 1, valueToXor);
        }
        else if(src.equals("(HL)"))
        {
            var address = Fixture.create(Short.TYPE);
            ProcessorAgent.getMemory()[address] = valueToXor;
            Registers.setHL(ToShort(address));
        }
        else if(src.startsWith("(I"))
        {
            var address = Fixture.create(Short.TYPE);
            var offset = Fixture.create(Byte.TYPE);
            var realAddress = Add(address, ToSignedByte(offset));
            ProcessorAgent.getMemory()[realAddress] = valueToXor;
            SetMemoryContentsAt((short) 2, offset);
            SetReg(src.substring(1,1 + 2), ToShort(address));
        }
        else if(!src.equals("A"))
        {
            SetReg(src, valueToXor);
        }
    }

    @ParameterizedTest
    @MethodSource("XOR_r_Source")
    public void XOR_r_sets_SF_appropriately(String src, byte opcode, Byte prefix)
    {
        ExecuteCase(src, opcode, 0xFF, 0x00, prefix);
        assertEquals(1, Registers.getSF().intValue());

        ExecuteCase(src, opcode, 0xFF, 0x7F, prefix);
        assertEquals(1, Registers.getSF().intValue());

        ExecuteCase(src, opcode, 0xFF, 0xF0, prefix);
        assertEquals(0, Registers.getSF().intValue());
    }

    private void ExecuteCase(String src, byte opcode, int oldValue, int valueToAnd, Byte prefix)
    {
        Setup(src, (byte)oldValue, (byte)valueToAnd);
        Execute(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("XOR_r_Source")
    public void XOR_r_sets_ZF_appropriately(String src, byte opcode, Byte prefix)
    {
        ExecuteCase(src, opcode, 0xFF, 0x00, prefix);
        assertEquals(0, Registers.getZF().intValue());

        ExecuteCase(src, opcode, 0xFF, 0x7F, prefix);
        assertEquals(0, Registers.getZF().intValue());

        ExecuteCase(src, opcode, 0xFF, 0xFF, prefix);
        assertEquals(1, Registers.getZF().intValue());
    }

    @ParameterizedTest
    @MethodSource("XOR_r_Source")
    public void XOR_r_sets_PF_appropriately(String src, byte opcode, Byte prefix)
    {
        ExecuteCase(src, opcode, 0xFF, 0x7E, prefix);
        assertEquals(1, Registers.getPF().intValue());

        ExecuteCase(src, opcode, 0xFF, 0x7F, prefix);
        assertEquals(0, Registers.getPF().intValue());

        ExecuteCase(src, opcode, 0xFF, 0x80, prefix);
        assertEquals(0, Registers.getPF().intValue());

        ExecuteCase(src, opcode, 0xFF, 0x81, prefix);
        assertEquals(1, Registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource({"XOR_r_Source", "XOR_A_Source"})
    public void XOR_r_resets_NF_CF_HF(String src, byte opcode, Byte prefix)
    {
        AssertResetsFlags(opcode, null, "N", "C", "H");
    }

    @ParameterizedTest
    @MethodSource("XOR_r_Source")
    public void XOR_r_sets_bits_3_and_5_from_result(String src, byte opcode, Byte prefix)
    {
        var value = WithBit(WithBit(((byte)0), 3, 1), 5, 0);
        Setup(src, value, (byte) 0);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getFlag3().intValue());
        assertEquals(0, Registers.getFlag5().intValue());

        value = WithBit(WithBit((byte)0, 3, 0), 5, 1);
        Setup(src, value, (byte) 0);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getFlag3().intValue());
        assertEquals(1, Registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource({"XOR_r_Source", "XOR_A_Source"})
    public void XOR_r_returns_proper_T_states(String src, byte opcode, Byte prefix)
    {
        var states = Execute(opcode, prefix);
        assertEquals(
            (src.equals("(HL)") || src.equals("n")) ? 7 :
            src.startsWith("I") ? 8 :
            src.startsWith(("(I")) ? 19 :
            4, states);
    }
}
