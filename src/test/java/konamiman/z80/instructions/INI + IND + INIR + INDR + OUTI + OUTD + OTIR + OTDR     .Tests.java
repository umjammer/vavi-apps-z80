package konamiman.z80.instructions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vavi.util.Debug;

import static konamiman.z80.utils.NumberUtils.add;
import static konamiman.z80.utils.NumberUtils.dec;
import static konamiman.z80.utils.NumberUtils.inc;
import static konamiman.z80.utils.NumberUtils.withBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class INI_IND_INIR_INDR_tests extends InstructionsExecutionTestsBase {

    private static final byte prefix = (byte) 0xED;

    public static final byte INI_Source = (byte) 0xA2;
    public static final byte IND_Source = (byte) 0xAA;
    public static final byte INIR_Source = (byte) 0xB2;
    public static final byte INDR_Source = (byte) 0xBA;
    public static final byte OUTI_Source = (byte) 0xA3;
    public static final byte OUTD_Source = (byte) 0xAB;
    public static final byte OTIR_Source = (byte) 0xB3;
    public static final byte OTDR_Source = (byte) 0xBB;

    @BeforeEach
    protected void setup() {
        super.setup();
    }

    @ParameterizedTest
    @ValueSource(bytes = {INI_Source, IND_Source, INIR_Source, INDR_Source})
    public void INI_IND_INIR_INDR_read_value_from_port_aC_into_aHL(byte opcode) {
        var portNumber = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var oldValue = fixture.create(Byte.TYPE);
        var address = fixture.create(Short.TYPE);

        registers.setHL(address);
        processorAgent.writeToMemory(address, oldValue);

        executeWithPortSetup(opcode, portNumber, value);

        var actual = processorAgent.readFromMemory(address);
        assertEquals(value, actual);
    }

    @ParameterizedTest
    @ValueSource(bytes = {OUTI_Source, OUTD_Source, OTIR_Source, OTDR_Source})
    public void OUTI_OUTD_OTIR_OTDR_write_value_from_aHL_into_port_aC(byte opcode) {
        var portNumber = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var oldValue = fixture.create(Byte.TYPE);
        var address = createAddressFixture();

        registers.setHL(address);
        processorAgent.writeToMemory(address, value);

        executeWithPortSetup(opcode, portNumber, oldValue);

        var actual = processorAgent.readFromPort(portNumber);
        assertEquals(value, actual);
    }

    @ParameterizedTest
    @ValueSource(bytes = {INI_Source, INIR_Source, OUTI_Source, OTIR_Source})
    public void INI_INIR_OUTI_OTIR_increase_HL(byte opcode) {
        var address = fixture.create(Short.TYPE);

        registers.setHL(address);

        executeWithPortSetup(opcode, (byte) 0, (byte) 0);

        var expected = inc(address);
        assertEquals(expected, registers.getHL());
    }

    @ParameterizedTest
    @ValueSource(bytes = {IND_Source, INDR_Source, OUTD_Source, OTDR_Source})
    public void IND_INDR_OUTD_OTDR_decrease_HL(byte opcode) {
        var address = fixture.create(Short.TYPE);

        registers.setHL(address);

        executeWithPortSetup(opcode, (byte) 0, (byte) 0);

        var expected = dec(address);
        assertEquals(expected, registers.getHL());
    }

    @ParameterizedTest
    @ValueSource(bytes = {INI_Source, IND_Source, INIR_Source, INDR_Source, OUTI_Source, OUTD_Source, OTIR_Source, OTDR_Source})
    public void INI_IND_INIR_INDR_OUTI_OUTD_OTIR_OTDR_decrease_B(byte opcode) {
        var counter = fixture.create(Byte.TYPE);

        registers.setB(counter);

        executeWithPortSetup(opcode, (byte) 0, (byte) 0);

        var expected = dec(counter);
        assertEquals(expected, registers.getB());
    }

    @ParameterizedTest
    @ValueSource(bytes = {INI_Source, IND_Source, INIR_Source, INDR_Source, OUTI_Source, OUTD_Source, OTIR_Source, OTDR_Source})
    public void INI_IND_INIR_INDR_OUTI_OUTD_OTIR_OTDR_set_Z_if_B_reaches_zero(byte opcode) {
        for (int i = 0; i < 256; i++) {
            byte b = (byte) i;
            registers.setB(b);

            executeWithPortSetup(opcode, (byte) 0, (byte) 0);

            assertEquals(b - 1 == 0, registers.getZF().booleanValue());
        }
    }

    @ParameterizedTest
    @ValueSource(bytes = {INI_Source, IND_Source, INIR_Source, INDR_Source, OUTI_Source, OUTD_Source, OTIR_Source, OTDR_Source})
    public void INI_IND_INIR_INDR_OUTI_OUTD_OTIR_OTDR_set_NF(byte opcode) {
        assertSetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @ValueSource(bytes = {INI_Source, IND_Source, INIR_Source, INDR_Source, OUTI_Source, OUTD_Source, OTIR_Source, OTDR_Source})
    public void INI_IND_INIR_INDR_OUTI_OUTD_OTIR_OTDR_do_not_change_CF(byte opcode) {
        assertDoesNotChangeFlags(opcode, prefix, "C");
    }

    @ParameterizedTest
    @ValueSource(bytes = {INI_Source, IND_Source, INIR_Source, INDR_Source, OUTI_Source, OUTD_Source, OTIR_Source, OTDR_Source})
    public void INI_IND_INIR_INDR_OUTI_OUTD_OTIR_OTDR_set_bits_3_and_5_from_result_of_decrementing_B(byte opcode) {
        registers.setB(withBit(withBit((byte) 1, 3, 1), 5, 0));
        execute(opcode, prefix);
        assertEquals(1, registers.getFlag3().intValue());
        assertEquals(0, registers.getFlag5().intValue());

        registers.setB(withBit(withBit((byte) 1, 3, 0), 5, 1));
        execute(opcode, prefix);
        assertEquals(0, registers.getFlag3().intValue());
        assertEquals(1, registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @ValueSource(bytes = {INI_Source, IND_Source, INIR_Source, INDR_Source, OUTI_Source, OUTD_Source, OTIR_Source, OTDR_Source})
    public void INI_IND_INIR_INDR_OUTI_OUTD_OTIR_OTDR_set_SF_appropriately(byte opcode) {
        registers.setB((byte) 0x02);

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
    @ValueSource(bytes = {INI_Source, IND_Source, OUTI_Source, OUTD_Source})
    public void INI_IND_OUTI_OUTD_return_proper_T_states(byte opcode) {
        var states = execute(opcode, prefix);
        assertEquals(16, states);
    }

    @ParameterizedTest
    @ValueSource(bytes = {INIR_Source, INDR_Source, OTIR_Source, OTDR_Source})
    public void INIR_INDR_OTIR_OTDR_decrease_PC_by_two_if_counter_does_not_reach_zero(byte opcode) {
        var dataAddress = fixture.create(Short.TYPE);
        var runAddress = fixture.create(Short.TYPE);
        var portNumber = fixture.create(Byte.TYPE);
        var oldData = fixture.create(Byte.TYPE);
        var data = fixture.create(Byte.TYPE);

        processorAgent.writeToMemory(dataAddress, oldData);
        setPortValue(portNumber, data);
        registers.setHL(dataAddress);
        registers.setB(fixture.create(Byte.TYPE));

        executeAt(runAddress, opcode, prefix);

try {
        assertEquals(runAddress, registers.getPC());
} catch (AssertionError e) {
 Debug.printf("fixture: %d, %d, %d, %d, %d", dataAddress, runAddress, portNumber, oldData, data);
}
    }

    @ParameterizedTest
    @ValueSource(bytes = {INIR_Source, INDR_Source, OTIR_Source, OTDR_Source})
    public void INIR_INDR_OTIR_OTDR_do_not_decrease_PC_by_two_if_counter_reaches_zero(byte opcode) {
        var dataAddress = fixture.create(Short.TYPE);
        var runAddress = fixture.create(Short.TYPE);
        var portNumber = fixture.create(Byte.TYPE);
        var oldData = fixture.create(Byte.TYPE);
        var data = fixture.create(Byte.TYPE);

        processorAgent.writeToMemory(dataAddress, oldData);
        setPortValue(portNumber, data);
        registers.setHL(dataAddress);
        registers.setB((byte) 1);

        executeAt(runAddress, opcode, prefix);

        assertEquals(add(runAddress, 2), registers.getPC());
    }

    @ParameterizedTest
    @ValueSource(bytes = {INIR_Source, INDR_Source, OTIR_Source, OTDR_Source})
    public void INIR_INDR_OTIR_OTDR_return_proper_T_states_depending_of_value_of_B(byte opcode) {
        registers.setB((byte) 128);
        for (int i = 0; i <= 256; i++) {
            var oldB = registers.getB();
            var states = execute(opcode, prefix);
            assertEquals(dec(oldB), registers.getB());
            assertEquals(registers.getB() == 0 ? 16 : 21, states);
        }
    }

    private int executeWithPortSetup(byte opcode, byte portNumber/* = 0*/, byte portValue/* = 0*/) {
        registers.setC(portNumber);
        setPortValue(portNumber, portValue);
        return execute(opcode, prefix);
    }
}
