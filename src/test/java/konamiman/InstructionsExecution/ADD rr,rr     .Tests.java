package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import konamiman.DataTypesAndUtils.Bit;
import konamiman.DataTypesAndUtils.NumberUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static konamiman.DataTypesAndUtils.NumberUtils.WithBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class ADD_rr_rr_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> ADD_rr_rr_Source() {
        return Stream.of(
                arguments("HL", "BC", (byte) 0x09, null),
                arguments("HL", "DE", (byte) 0x19, null),
                arguments("HL", "SP", (byte) 0x39, null),
                arguments("IX", "BC", (byte) 0x09, (byte) 0xDD),
                arguments("IX", "DE", (byte) 0x19, (byte) 0xDD),
                arguments("IX", "SP", (byte) 0x39, (byte) 0xDD),
                arguments("IY", "BC", (byte) 0x09, (byte) 0xFD),
                arguments("IY", "DE", (byte) 0x19, (byte) 0xFD),
                arguments("IY", "SP", (byte) 0x39, (byte) 0xFD)
        );
    }

    static Stream<Arguments> ADD_rr_rr_Source_same_src_and_dest() {
        return Stream.of(
                arguments("HL", "HL", (byte) 0x29, null),
                arguments("IX", "IX", (byte) 0x29, (byte) 0xDD),
                arguments("IY", "IY", (byte) 0x29, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource({"ADD_rr_rr_Source", "ADD_rr_rr_Source_same_src_and_dest"})
    public void ADD_rr_rr_adds_register_values(String dest, String src, byte opcode, Byte prefix) {
        var value1 = Fixture.create(Short.TYPE);
        var value2 = (src.equals(dest)) ? value1 : Fixture.create(Short.TYPE);

        SetReg(dest, value1);
        if (!src.equals(dest))
            SetReg(src, value2);

        Execute(opcode, prefix);

        assertEquals(Add(value1, value2), this.<Short>GetReg(dest));
        if (!src.equals(dest))
            assertEquals(value2, this.<Short>GetReg(src));
    }

    @ParameterizedTest
    @MethodSource("ADD_rr_rr_Source")
    public void ADD_rr_rr_sets_CF_properly(String dest, String src, byte opcode, Byte prefix) {
        Registers.setCF(Bit.ON);
        SetReg(dest, ToShort(0xFFFE));
        SetReg(src, (byte) 1);

        Execute(opcode, prefix);
        assertEquals(0, Registers.getCF().intValue());

        Execute(opcode, prefix);
        assertEquals(1, Registers.getCF().intValue());
    }

    @ParameterizedTest
    @MethodSource({"ADD_rr_rr_Source", "ADD_rr_rr_Source_same_src_and_dest"})
    public void ADD_rr_rr_resets_N(String dest, String src, byte opcode, Byte prefix) {
        AssertResetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("ADD_rr_rr_Source")
    public void ADD_rr_rr_sets_HF_appropriately(String dest, String src, byte opcode, Byte prefix) {
        SetReg(src, (byte) 0x10);
        for (byte b : new byte[] {0x0F, 0x7F, (byte) 0xFF}) {
            SetReg(dest, NumberUtils.createShort((byte) 0xFF, b));

            Execute(opcode, prefix);
            assertEquals(1, Registers.getHF().intValue());

            Execute(opcode, prefix);
            assertEquals(0, Registers.getHF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource({"ADD_rr_rr_Source", "ADD_rr_rr_Source_same_src_and_dest"})
    public void ADD_rr_rr_does_not_change_SF_ZF_PF(String dest, String src, byte opcode, Byte prefix) {
        var randomValues = Fixture.create(byte[].class);
        var randomSF = Fixture.create(Bit.class);
        var randomZF = Fixture.create(Bit.class);
        var randomPF = Fixture.create(Bit.class);

        Registers.setSF(randomSF);
        Registers.setZF(randomZF);
        Registers.setPF(randomPF);

        SetReg(src, Fixture.create(Byte.TYPE));
        for (var value : randomValues) {
            SetReg(src, value);
            Execute(opcode, prefix);

            assertEquals(randomSF, Registers.getSF());
            assertEquals(randomZF, Registers.getZF());
            assertEquals(randomPF, Registers.getPF());
        }
    }

    @ParameterizedTest
    @MethodSource("ADD_rr_rr_Source")
    public void ADD_rr_rr_sets_bits_3_and_5_from_high_byte_of_result(String dest, String src, byte opcode, Byte prefix) {
        SetReg(dest, NumberUtils.createShort((byte) 0, WithBit(WithBit(((byte) 0), 3, 1), 5, 0)));
        SetReg(src, (byte) 0);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getFlag3().intValue());
        assertEquals(0, Registers.getFlag5().intValue());

        SetReg(dest, NumberUtils.createShort((byte) 0, WithBit(WithBit(((byte) 0), 3, 0), 5, 1)));
        Execute(opcode, prefix);
        assertEquals(0, Registers.getFlag3().intValue());
        assertEquals(1, Registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource({"ADD_rr_rr_Source", "ADD_rr_rr_Source_same_src_and_dest"})
    public void ADD_rr_rr_returns_proper_T_states(String dest, String src, byte opcode, Byte prefix) {
        var states = Execute(opcode, prefix);
        assertEquals(IfIndexRegister(dest, 15, /*@else:*/ 11), states);
    }
}
