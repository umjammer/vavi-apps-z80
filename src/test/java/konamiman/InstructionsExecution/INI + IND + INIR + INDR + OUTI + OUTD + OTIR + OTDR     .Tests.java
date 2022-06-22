package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.Dec;
import static konamiman.DataTypesAndUtils.NumberUtils.Inc;
import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static konamiman.DataTypesAndUtils.NumberUtils.WithBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class INI_IND_INIR_INDR_tests extends InstructionsExecutionTestsBase {

    private static final byte prefix = (byte) 0xED;

    public static Object[] All_Source;

    public static Object[] INI_Source = {
            new Object[] {(byte) 0xA2}
    };

    public static Object[] IND_Source = {
            new Object[] {(byte) 0xAA}
    };

    public static Object[] INIR_Source = {
            new Object[] {(byte) 0xB2}
    };

    public static Object[] INDR_Source = {
            new Object[] {(byte) 0xBA}
    };

    public static Object[] OUTI_Source = {
            new Object[] {(byte) 0xA3}
    };

    public static Object[] OUTD_Source = {
            new Object[] {(byte) 0xAB}
    };

    public static Object[] OTIR_Source = {
            new Object[] {(byte) 0xB3}
    };

    public static Object[] OTDR_Source = {
            new Object[] {(byte) 0xBB}
    };

    static {
        All_Source = Stream.of(INI_Source,
                IND_Source,
                INIR_Source,
                INDR_Source,
                OUTI_Source,
                OUTD_Source,
                OTIR_Source,
                OTDR_Source).flatMap(Stream::of).toArray();
    }

    @ParameterizedTest
    @MethodSource({"INI_Source", "IND_Source", "INIR_Source", "INDR_Source"})
    public void INI_IND_INIR_INDR_read_value_from_port_aC_into_aHL(byte opcode) {
        var portNumber = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);
        var oldValue = Fixture.create(Byte.TYPE);
        var address = Fixture.create(Short.TYPE);

        Registers.setHL(ToShort(address));
        ProcessorAgent.getMemory()[address] = oldValue;

        ExecuteWithPortSetup(opcode, portNumber, value);

        var actual = ProcessorAgent.getMemory()[address];
        assertEquals(value, actual);
    }

    @ParameterizedTest
    @MethodSource({"OUTI_Source", "OUTD_Source", "OTIR_Source", "OTDR_Source"})
    public void OUTI_OUTD_OTIR_OTDR_write_value_from_aHL_into_port_aC(byte opcode) {
        var portNumber = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);
        var oldValue = Fixture.create(Byte.TYPE);
        var address = Fixture.create(Short.TYPE);

        Registers.setHL(ToShort(address));
        ProcessorAgent.getMemory()[address] = value;

        ExecuteWithPortSetup(opcode, portNumber, oldValue);

        var actual = ProcessorAgent.getPorts()[portNumber];
        assertEquals(value, actual);
    }

    @ParameterizedTest
    @MethodSource({"INI_Source", "INIR_Source", "OUTI_Source", "OTIR_Source"})
    public void INI_INIR_OUTI_OTIR_increase_HL(byte opcode) {
        var address = Fixture.create(Short.TYPE);

        Registers.setHL(ToShort(address));

        ExecuteWithPortSetup(opcode, (byte) 0, (byte) 0);

        var expected = Inc(address);
        assertEquals(expected, Registers.getHL());
    }

    @ParameterizedTest
    @MethodSource({"IND_Source", "INDR_Source", "OUTD_Source", "OTDR_Source"})
    public void IND_INDR_OUTD_OTDR_decrease_HL(byte opcode) {
        var address = Fixture.create(Short.TYPE);

        Registers.setHL(ToShort(address));

        ExecuteWithPortSetup(opcode, (byte) 0, (byte) 0);

        var expected = Dec(address);
        assertEquals(expected, Registers.getHL());
    }

    @ParameterizedTest
    @MethodSource("All_Source")
    public void INI_IND_INIR_INDR_OUTI_OUTD_OTIR_OTDR_decrease_B(byte opcode) {
        var counter = Fixture.create(Byte.TYPE);

        Registers.setB(counter);

        ExecuteWithPortSetup(opcode, (byte) 0, (byte) 0);

        var expected = Dec(counter);
        assertEquals(expected, Registers.getB());
    }

    @ParameterizedTest
    @MethodSource("All_Source")
    public void INI_IND_INIR_INDR_OUTI_OUTD_OTIR_OTDR_set_Z_if_B_reaches_zero(byte opcode) {
        for (int i = 0; i < 256; i++) {
            byte b = (byte) i;
            Registers.setB(b);

            ExecuteWithPortSetup(opcode, (byte) 0, (byte) 0);

            assertEquals(b - 1 == 0, Registers.getZF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("All_Source")
    public void INI_IND_INIR_INDR_OUTI_OUTD_OTIR_OTDR_set_NF(byte opcode) {
        AssertSetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("All_Source")
    public void INI_IND_INIR_INDR_OUTI_OUTD_OTIR_OTDR_do_not_change_CF(byte opcode) {
        AssertDoesNotChangeFlags(opcode, prefix, "C");
    }

    @ParameterizedTest
    @MethodSource("All_Source")
    public void INI_IND_INIR_INDR_OUTI_OUTD_OTIR_OTDR_set_bits_3_and_5_from_result_of_decrementing_B(byte opcode) {
        Registers.setB(WithBit(WithBit((byte) 1, 3, 1), 5, 0));
        Execute(opcode, prefix);
        assertEquals(1, Registers.getFlag3().intValue());
        assertEquals(0, Registers.getFlag5().intValue());

        Registers.setB(WithBit(WithBit((byte) 1, 3, 0), 5, 1));
        Execute(opcode, prefix);
        assertEquals(0, Registers.getFlag3().intValue());
        assertEquals(1, Registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource("All_Source")
    public void INI_IND_INIR_INDR_OUTI_OUTD_OTIR_OTDR_set_SF_appropriately(byte opcode) {
        Registers.setB((byte) 0x02);

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
    @MethodSource({"INI_Source", "IND_Source", "OUTI_Source", "OUTD_Source"})
    public void INI_IND_OUTI_OUTD_return_proper_T_states(byte opcode) {
        var states = Execute(opcode, prefix);
        assertEquals(16, states);
    }

    @ParameterizedTest
    @MethodSource({"INIR_Source", "INDR_Source", "OTIR_Source", "OTDR_Source"})
    public void INIR_INDR_OTIR_OTDR_decrease_PC_by_two_if_counter_does_not_reach_zero(byte opcode) {
        var dataAddress = Fixture.create(Short.TYPE);
        var runAddress = Fixture.create(Short.TYPE);
        var portNumber = Fixture.create(Byte.TYPE);
        var oldData = Fixture.create(Byte.TYPE);
        var data = Fixture.create(Byte.TYPE);

        ProcessorAgent.getMemory()[dataAddress] = oldData;
        SetPortValue(portNumber, data);
        Registers.setHL(ToShort(dataAddress));
        Registers.setB(Fixture.create(Byte.TYPE));

        ExecuteAt(runAddress, opcode, prefix);

        assertEquals(runAddress, Registers.getPC());
    }

    @ParameterizedTest
    @MethodSource({"INIR_Source", "INDR_Source", "OTIR_Source", "OTDR_Source"})
    public void INIR_INDR_OTIR_OTDR_do_not_decrease_PC_by_two_if_counter_reaches_zero(byte opcode) {
        var dataAddress = Fixture.create(Short.TYPE);
        var runAddress = Fixture.create(Short.TYPE);
        var portNumber = Fixture.create(Byte.TYPE);
        var oldData = Fixture.create(Byte.TYPE);
        var data = Fixture.create(Byte.TYPE);

        ProcessorAgent.getMemory()[dataAddress] = oldData;
        SetPortValue(portNumber, data);
        Registers.setHL(ToShort(dataAddress));
        Registers.setB((byte) 1);

        ExecuteAt(runAddress, opcode, prefix);

        assertEquals(Add(runAddress, (short) 2), Registers.getPC());
    }

    @ParameterizedTest
    @MethodSource({"INIR_Source", "INDR_Source", "OTIR_Source", "OTDR_Source"})
    public void INIR_INDR_OTIR_OTDR_return_proper_T_states_depending_of_value_of_B(byte opcode) {
        Registers.setB((byte) 128);
        for (int i = 0; i <= 256; i++) {
            var oldB = Registers.getB();
            var states = Execute(opcode, prefix);
            assertEquals(Dec(oldB), Registers.getB());
            assertEquals(Registers.getB() == 0 ? 16 : 21, states);
        }
    }

    private int ExecuteWithPortSetup(byte opcode, byte portNumber/* = 0*/, byte portValue/* = 0*/) {
        Registers.setC(portNumber);
        SetPortValue(portNumber, portValue);
        return Execute(opcode, prefix);
    }
}
