package konamiman.z80.instructions;

import java.util.stream.Stream;

import konamiman.z80.utils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.add;
import static konamiman.z80.utils.NumberUtils.createShort;
import static konamiman.z80.utils.NumberUtils.withBit;
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
        var value1 = fixture.create(Short.TYPE);
        var value2 = src.equals(dest) ? value1 : fixture.create(Short.TYPE);

        setReg(dest, value1);
        if (!src.equals(dest))
            setReg(src, value2);

        execute(opcode, prefix);

        assertEquals(add(value1, value2), this.<Short>getReg(dest));
        if (!src.equals(dest))
            assertEquals(value2, this.<Short>getReg(src));
    }

    @ParameterizedTest
    @MethodSource("ADD_rr_rr_Source")
    public void ADD_rr_rr_sets_CF_properly(String dest, String src, byte opcode, Byte prefix) {
        registers.setCF(Bit.ON);
        setReg(dest, (short) 0xFFFE);
        setReg(src, (short) 1);

        execute(opcode, prefix);
        assertEquals(0, registers.getCF().intValue());

        execute(opcode, prefix);
        assertEquals(1, registers.getCF().intValue());
    }

    @ParameterizedTest
    @MethodSource({"ADD_rr_rr_Source", "ADD_rr_rr_Source_same_src_and_dest"})
    public void ADD_rr_rr_resets_N(String dest, String src, byte opcode, Byte prefix) {
        assertResetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("ADD_rr_rr_Source")
    public void ADD_rr_rr_sets_HF_appropriately(String dest, String src, byte opcode, Byte prefix) {
        setReg(src, (short) 0x10);
        for (byte b : new byte[] {0x0F, 0x7F, (byte) 0xFF}) {
            setReg(dest, createShort((byte) 0xFF, b));

            execute(opcode, prefix);
            assertEquals(1, registers.getHF().intValue());

            execute(opcode, prefix);
            assertEquals(0, registers.getHF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource({"ADD_rr_rr_Source", "ADD_rr_rr_Source_same_src_and_dest"})
    public void ADD_rr_rr_does_not_change_SF_ZF_PF(String dest, String src, byte opcode, Byte prefix) {
        var randomValues = fixture.collections().createCollection(Short.class);
        var randomSF = fixture.create(Bit.class);
        var randomZF = fixture.create(Bit.class);
        var randomPF = fixture.create(Bit.class);

        registers.setSF(randomSF);
        registers.setZF(randomZF);
        registers.setPF(randomPF);

        setReg(src, fixture.create(Short.TYPE));
        for (var value : randomValues) {
            setReg(src, value);
            execute(opcode, prefix);

            assertEquals(randomSF, registers.getSF());
            assertEquals(randomZF, registers.getZF());
            assertEquals(randomPF, registers.getPF());
        }
    }

    @ParameterizedTest
    @MethodSource("ADD_rr_rr_Source")
    public void ADD_rr_rr_sets_bits_3_and_5_from_high_byte_of_result(String dest, String src, byte opcode, Byte prefix) {
        setReg(dest, createShort((byte) 0, withBit(withBit(((byte) 0), 3, 1), 5, 0)));
        setReg(src, (short) 0);
        execute(opcode, prefix);
        assertEquals(1, registers.getFlag3().intValue());
        assertEquals(0, registers.getFlag5().intValue());

        setReg(dest, createShort((byte) 0, withBit(withBit(((byte) 0), 3, 0), 5, 1)));
        execute(opcode, prefix);
        assertEquals(0, registers.getFlag3().intValue());
        assertEquals(1, registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource({"ADD_rr_rr_Source", "ADD_rr_rr_Source_same_src_and_dest"})
    public void ADD_rr_rr_returns_proper_T_states(String dest, String src, byte opcode, Byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(ifIndexRegister(dest, 15, 11), states);
    }
}
