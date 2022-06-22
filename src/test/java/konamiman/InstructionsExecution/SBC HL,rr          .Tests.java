package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import konamiman.DataTypesAndUtils.Bit;
import konamiman.DataTypesAndUtils.NumberUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Sub;
import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static konamiman.DataTypesAndUtils.NumberUtils.WithBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class SBC_HL_rr_tests extends InstructionsExecutionTestsBase
{
    private static final byte prefix = (byte) 0xED;

    static Stream<Arguments> SBC_HL_rr_Source() {
        return Stream.of(
                arguments("BC", (byte)0x42),
                arguments("DE", (byte)0x52),
                arguments("SP", (byte)0x72)
        );
    }

    static Stream<Arguments> SBC_HL_HL_Source() {
        return Stream.of(
                arguments("HL", (byte)0x62)
        );
    }

    @ParameterizedTest
    @MethodSource({"SBC_HL_rr_Source", "SBC_HL_HL_Source"})
    public void SBC_HL_rr_substracts_both_registers_with_and_without_carry(String src, byte opcode)
    {
        for(var cf = 0; cf <= 1; cf++)
        {
            var value1 = Fixture.create(Short.TYPE);
            var value2 = (src.equals("HL")) ? value1 : Fixture.create(Short.TYPE);

            Registers.setHL(value1);
            Registers.setCF(Bit.of(cf));
            if (!src.equals("HL"))
                SetReg(src, value2);

            Execute(opcode, prefix);

            assertEquals(Sub(Sub(value1, value2), (short) cf), Registers.getHL());
            if (!src.equals("HL"))
                assertEquals(value2, this.<Short>GetReg(src));
        }
    }

    @ParameterizedTest
    @MethodSource("SBC_HL_rr_Source")
    public void SUB_HL_rr_sets_SF_appropriately(String src, byte opcode)
    {
        Setup(src, (short) 0x02, (short) 1);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getSF().intValue());

        Setup(src, (short) 0x01, (short) 1);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getSF().intValue());

        Setup(src, (short) 0x00, (short) 1);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getSF().intValue());

        Setup(src, ToShort(0xFFFF), (short) 1);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getSF().intValue());
    }

    private void Setup(String src, short oldValue, short valueToSubstract)
    {
        Registers.setHL(oldValue);
        Registers.setCF(Bit.OFF);

        if(!src.equals("HL"))
        {
            SetReg(src, valueToSubstract);
        }
    }

    @ParameterizedTest
    @MethodSource("SBC_HL_rr_Source")
    public void SUB_HL_rr_sets_ZF_appropriately(String src, byte opcode)
    {
        Setup(src, (short) 0x03, (short) 1);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getZF().intValue());

        Setup(src, (short) 0x02, (short) 1);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getZF().intValue());

        Setup(src, (short) 0x01, (short) 1);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getZF().intValue());

        Setup(src, (short) 0x00, (short) 1);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getZF().intValue());
    }

    @ParameterizedTest
    @MethodSource("SBC_HL_rr_Source")
    public void SUB_HL_rr_sets_HF_appropriately(String src, byte opcode)
    {
        for (int i : new int[] { 0x1001, 0x8001, 0xF001 })
        {
            short b = ToShort(i);

            Setup(src, b, (short) 1);
            Execute(opcode, prefix);
            assertEquals(0, Registers.getHF().intValue());

            Setup(src, (byte)(b-1), (short) 1);
            Execute(opcode, prefix);
            assertEquals(1, Registers.getHF().intValue());

            Setup(src, (byte)(b-2), (short) 1);
            Execute(opcode, prefix);
            assertEquals(0, Registers.getHF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("SBC_HL_rr_Source")
    public void SUB_HL_rr_sets_CF_appropriately(String src, byte opcode)
    {
        Setup(src, (short) 0x01, (short) 1);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getCF().intValue());

        Setup(src, (short) 0x00, (short) 1);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getCF().intValue());

        Setup(src, (short) 0xFF, (short) 1);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getCF().intValue());
    }

    @ParameterizedTest
    @MethodSource("SBC_HL_rr_Source")
    public void SBC_HL_rr_sets_PF_appropriately(String src, byte opcode)
    {
        //http://stackoverflow.com/a/8037485/4574

        TestPF(src, opcode, 127, 0, 0);
        TestPF(src, opcode, 127, 1, 0);
        TestPF(src, opcode, 127, 127, 0);
        TestPF(src, opcode, 127, 128, 1);
        TestPF(src, opcode, 127, 129, 1);
        TestPF(src, opcode, 127, 255, 1);
        TestPF(src, opcode, 128, 0, 0);
        TestPF(src, opcode, 128, 1, 1);
        TestPF(src, opcode, 128, 127, 1);
        TestPF(src, opcode, 128, 128, 0);
        TestPF(src, opcode, 128, 129, 0);
        TestPF(src, opcode, 128, 255, 0);
        TestPF(src, opcode, 129, 0, 0);
        TestPF(src, opcode, 129, 1, 0);
        TestPF(src, opcode, 129, 127, 1);
        TestPF(src, opcode, 129, 128, 0);
        TestPF(src, opcode, 129, 129, 0);
        TestPF(src, opcode, 129, 255, 0);
    }

    void TestPF(String src, byte opcode, int oldValue, int substractedValue, int expectedPF)
    {
        Setup(src, NumberUtils.createShort((byte) 0, (byte)oldValue), NumberUtils.createShort((byte) 0, (byte)substractedValue));

        Execute(opcode, prefix);
        assertEquals(expectedPF, Registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource("SBC_HL_rr_Source")
    public void SBC_HL_rr_sets_NF(String src, byte opcode)
    {
        AssertSetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("SBC_HL_rr_Source")
    public void SBC_HL_rr_sets_bits_3_and_5_from_high_byte_of_result(String src, byte opcode)
    {
        Registers.setHL(NumberUtils.createShort((byte) 0,  WithBit(WithBit((byte)0, 3, 1), 5, 0)));
        SetReg(src, (byte) 0);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getFlag3().intValue());
        assertEquals(0, Registers.getFlag5().intValue());

        Registers.setHL(NumberUtils.createShort((byte) 0,  WithBit(WithBit((byte)0, 3, 0), 5, 1)));
        Execute(opcode, prefix);
        assertEquals(0, Registers.getFlag3().intValue());
        assertEquals(1, Registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource("SBC_HL_rr_Source")
    public void SBC_HL_rr_returns_proper_T_states(String src, byte opcode)
    {
        var states = Execute(opcode, prefix);
        assertEquals(15, states);
    }
}

