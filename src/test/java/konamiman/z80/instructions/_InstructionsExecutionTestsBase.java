package konamiman.z80.instructions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.flextrade.jfixture.JFixture;
import dotnet4j.util.compat.CollectionUtilities;
import konamiman.z80.impls.Z80RegistersImpl;
import konamiman.z80.instructions.core.Z80InstructionExecutorImpl;
import konamiman.z80.interfaces.Z80ProcessorAgent;
import konamiman.z80.interfaces.Z80ProcessorAgentExtendedPorts;
import konamiman.z80.interfaces.Z80Registers;
import konamiman.z80.utils.Bit;
import konamiman.z80.utils.NumberUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.provider.Arguments;

import static konamiman.z80.utils.NumberUtils.add;
import static konamiman.z80.utils.NumberUtils.createShort;
import static konamiman.z80.utils.NumberUtils.getHighByte;
import static konamiman.z80.utils.NumberUtils.getLowByte;
import static konamiman.z80.utils.NumberUtils.inc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


abstract class InstructionsExecutionTestsBase {

    protected Z80InstructionExecutorImpl sut;
    protected FakeProcessorAgent processorAgent;
    protected Z80Registers registers;
    protected JFixture fixture;

    @BeforeEach
    protected void setup() {
        sut = new Z80InstructionExecutorImpl();
        sut.setProcessorAgent(processorAgent = new FakeProcessorAgent());
        sut.setProcessorAgentExtendedPorts(processorAgent);
        registers = processorAgent.registers;
        sut.instructionFetchFinished().addListener(e -> {
        });

        fixture = new JFixture();
    }

//#region Auxiliary methods

    protected int nextFetchesAddress;

    protected void setPortValue(short portNumber, byte value) {
        processorAgent.ports[portNumber & 0xffff] = value;
    }

    protected byte getPortValue(short portNumber) {
        return processorAgent.ports[portNumber & 0xffff];
    }

    protected void setMemoryContents(byte... opcodes) {
        setMemoryContentsAt((byte) 0, opcodes);
    }

    protected void setMemoryContentsAt(short address, byte... opcodes) {
        System.arraycopy(opcodes, 0, processorAgent.memory, address & 0xffff, opcodes.length);
        nextFetchesAddress = (address & 0xffff) + opcodes.length;
    }

    protected void continueSettingMemoryContents(byte... opcodes) {
        System.arraycopy(opcodes, 0, processorAgent.memory, nextFetchesAddress, opcodes.length);
        nextFetchesAddress += opcodes.length;
    }

    protected FakeInstructionExecutor newFakeInstructionExecutor() {
        var sut = new FakeInstructionExecutor();
        sut.setProcessorAgent(processorAgent = new FakeProcessorAgent());
        sut.setProcessorAgentExtendedPorts(processorAgent);
        registers = processorAgent.registers;
        sut.instructionFetchFinished().addListener(e -> {});
        return sut;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getReg(String name) {
        try {
            Method method = registers.getClass().getMethod("get" + name);
            return (T) method.invoke(registers);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Bit getFlag(String name) {
        if (name.length() == 1)
            name += "F";

        try {
            Method method = registers.getClass().getMethod("get" + name);
            return (Bit) method.invoke(registers);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void setReg(String regName, byte value) {
        try {
            regProperty("set" + regName, Byte.TYPE).invoke(registers, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void setReg(String regName, short value) {
        try {
            regProperty("set" + regName, Short.TYPE).invoke(registers, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void setFlag(String flagName, Bit value) {
        if (flagName.length() == 1)
            flagName += "F";

        try {
            regProperty("set" + flagName, Bit.class).invoke(registers, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Method regProperty(String name, Class<?> arg) {
        try {
            return registers.getClass().getMethod(name, arg);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    protected int execute(byte opcode, Byte prefix /* = null */, byte... nextFetches) {
        return executeAt((short) 0, opcode, prefix, nextFetches);
    }

    protected int executeAt(short address, byte opcode, Byte prefix /* = null */, byte... nextFetches) {
        registers.setPC(inc(address)); // Inc needed to simulate the first fetch made by the enclosing Z80Processor
        if (prefix == null) {
            setMemoryContentsAt(inc(address), nextFetches);
            return sut.execute(opcode);
        } else {
            setMemoryContentsAt(inc(address), opcode);
            continueSettingMemoryContents(nextFetches);

            return sut.execute(prefix);
        }
    }

    protected static Object ifIndexRegister(String regName, Object value, Object else_) {
        return regName.startsWith("IX") || regName.startsWith("IY") ? value : else_;
    }

    protected void assertNoFlagsAreModified(byte opcode, Byte prefix /* = null */) {
        var value = fixture.create(Byte.TYPE);
        registers.setF(value);
        execute(opcode, prefix);

        assertEquals(value, registers.getF());
    }

    protected void assertSetsFlags(byte opcode, Byte prefix /* = null */, String... flagNames) {
        assertSetsFlags(null, opcode, prefix, flagNames);
    }

    protected void assertSetsFlags(Runnable executor, byte opcode, Byte prefix /* = null */, String... flagNames) {
        assertSetsOrResetsFlags(opcode, Bit.ON, prefix, executor, flagNames);
    }

    protected void assertResetsFlags(byte opcode, Byte prefix /* = null */, String... flagNames) {
        assertResetsFlags(null, opcode, prefix, flagNames);
    }

    protected void assertResetsFlags(Runnable executor, byte opcode, Byte prefix /* = null */, String... flagNames) {
        assertSetsOrResetsFlags(opcode, Bit.OFF, prefix, executor, flagNames);
    }

    protected void assertSetsOrResetsFlags(byte opcode, Bit expected, Byte prefix /* = null */, Runnable executor /* = null */, String... flagNames) {
        if (executor == null)
            executor = () -> execute(opcode, prefix);

        var randomValues = fixture.collections().createCollection(Byte.TYPE, 3);

        for (var value : randomValues) {
            for (var flag : flagNames)
                setFlag(flag, expected.operatorNOT());

            registers.setA(value);

            executor.run();

            for (var flag : flagNames)
                assertEquals(expected, getFlag(flag));
        }
    }

    protected void assertDoesNotChangeFlags(byte opcode, Byte prefix /* = null */, String... flagNames) {
        assertDoesNotChangeFlags(null, opcode, prefix, flagNames);
    }

    protected void assertDoesNotChangeFlags(Runnable executor, byte opcode, Byte prefix /* = null */, String... flagNames) {
        if (executor == null)
            executor = () -> execute(opcode, prefix);

        if (flagNames.length == 0)
            flagNames = new String[] {"C", "H", "S", "Z", "P", "N", "Flag3", "Flag5"};

        var randomFlags = CollectionUtilities.toMap(flagNames, x -> x, x -> fixture.create(Bit.class));

        for (var flag : flagNames)
            setFlag(flag, randomFlags.get(flag));

        for (var i = 0; i <= fixture.create(Byte.TYPE); i++) {
            executor.run();

            for (var flag : flagNames)
                assertEquals(randomFlags.get(flag), getFlag(flag));
        }
    }

    protected void writeShortToMemory(short address, short value) {
        processorAgent.writeToMemory(address, getLowByte(value));
        processorAgent.writeToMemory(inc(address), getHighByte(value));
    }

    protected short readShortFromMemory(short address) {
        return createShort(processorAgent.readFromMemory(address), processorAgent.readFromMemory(inc(address)));
    }

    protected void setupRegOrMem(String reg, byte value, byte offset /* = 0 */) {
        if (reg.equals("(HL)")) {
            var address = createAddressFixture();
            processorAgent.writeToMemory(address, value);
            registers.setHL(address);
        } else if (reg.startsWith(("(I"))) {
            var regName = reg.substring(1, 1 + 2);
            var address = createAddressFixture((short) 1, (short) 2, (short) 3);
            var realAddress = add(address, offset);
//Debug.printf("%04x, %d, %04x", address, offset, realAddress);
            processorAgent.writeToMemory(realAddress, value);
            setReg(regName, address);
        } else {
            setReg(reg, value);
        }
    }

    protected byte valueOfRegOrMem(String reg, byte offset /* = 0 */) {
        if (reg.equals("(HL)")) {
            return processorAgent.readFromMemory(registers.getHL());
        } else if (reg.startsWith(("(I"))) {
            var regName = reg.substring(1, 1 + 2);
            var address = add(this.<Short>getReg(regName), offset);
            return processorAgent.readFromMemory(address);
        } else {
            return this.<Byte>getReg(reg);
        }
    }

    protected static List<Arguments> getBitInstructionsSource(byte baseOpcode, boolean includeLoadReg /* = true */, boolean loopSevenBits /* = false */) {
        var bases = new Object[][] {
                new Object[] {"A", 7},
                new Object[] {"B", 0},
                new Object[] {"C", 1},
                new Object[] {"D", 2},
                new Object[] {"E", 3},
                new Object[] {"H", 4},
                new Object[] {"L", 5},
                new Object[] {"(HL)", 6}
        };

        var sources = new ArrayList<Arguments>();
        var bitsCount = loopSevenBits ? 7 : 0;
        for (var bit = 0; bit <= bitsCount; bit++) {
            for (var instr : bases) {
                var reg = (String) instr[0];
                var regCode = (int) instr[1];
                var opcode = baseOpcode | (bit << 3) | regCode;
                //srcReg, dest, opcode, prefix, bit
                sources.add(arguments(reg, null, (byte) opcode, null, bit));
            }

            for (var instr : bases) {
                var destReg = (String) instr[0];
                if (destReg.equals("(HL)")) destReg = "";
                if (!destReg.isEmpty() && !includeLoadReg) continue;
                var regCode = baseOpcode | (bit << 3) | (int) instr[1];
                for (var reg : new String[] {"(IX+n)", "(IY+n)"}) {
                    //srcReg, dest, opcode, prefix, bit
                    sources.add(arguments(
                            reg, destReg, (byte) regCode,
                            reg.charAt(2) == 'X' ? (byte) 0xDD :(byte) 0xFD,
                            bit));
                }
            }
        }

        return sources;
    }

    /**
     * @param regNamesArrayIndex OUT
     * @param prefix OUT
     */
    protected static void modifyTestCaseCreationForIndexRegs(String regName, /* ref */ int[] regNamesArrayIndex, /* out */ Byte[] prefix) {
//            prefix = null;

        switch (regName) {
        case "IXH":
            regNamesArrayIndex[0] = 4;
            prefix[0] = (byte) 0xDD;
            break;
        case "IXL":
            regNamesArrayIndex[0] = 5;
            prefix[0] = (byte) 0xDD;
            break;
        case "IYH":
            regNamesArrayIndex[0] = 4;
            prefix[0] = (byte) 0xFD;
            break;
        case "IYL":
            regNamesArrayIndex[0] = 5;
            prefix[0] = (byte) 0xFD;
            break;
        case "(IX+n)":
            regNamesArrayIndex[0] = 6;
            prefix[0] = (byte) 0xDD;
            break;
        case "(IY+n)":
            regNamesArrayIndex[0] = 6;
            prefix[0] = (byte) 0xFD;
            break;
        }
    }

    protected int executeBit(byte opcode, Byte prefix /* = null */, Byte offset /* = null */) {
        if (prefix == null)
            return execute(opcode, (byte) 0xCB);
        else
            return execute((byte) 0xCB, prefix, offset, opcode);
    }

//#endregion

//#region ParityTable

    protected static final byte[] parity = new byte[] {
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
            0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
            0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
            0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
            0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
            0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
            0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
            0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
            0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1};

//#endregion

//#region Fake classes

    protected static class FakeInstructionExecutor extends Z80InstructionExecutorImpl {
        public final List<Byte> unsupportedExecuted = new ArrayList<>();

        protected @Override int executeUnsupported_ED_Instruction(byte secondOpcodeByte) {
            unsupportedExecuted.add(secondOpcodeByte);
            return super.executeUnsupported_ED_Instruction(secondOpcodeByte);
        }
    }

    protected static class FakeProcessorAgent implements Z80ProcessorAgent, Z80ProcessorAgentExtendedPorts {
        public FakeProcessorAgent() {
            registers = new Z80RegistersImpl();
            memory = new byte[65536];
            ports = new byte[65536];
        }

        private final byte[] memory;

        private final byte[] ports;

        private short memoryPointer;

        private byte currentInterruptMode;

        public byte getCurrentInterruptMode() {
            return currentInterruptMode;
        }

        @Override
        public byte fetchNextOpcode() {
            return readFromMemory(registers.incPC());
        }

        @Override
        public byte peekNextOpcode() {
            return readFromMemory(registers.getPC());
        }

        @Override
        public byte readFromMemory(short address) {
            return memory[address & 0xffff];
        }

        @Override
        public void writeToMemory(short address, byte value) {
            memory[address & 0xffff] = value;
        }

        @Override
        public byte readFromPort(byte portNumber) {
            return ports[portNumber & 0xff];
        }

        @Override
        public void writeToPort(byte portNumber, byte value) {
            ports[portNumber & 0xff] = value;
        }

        private Z80Registers registers;

        @Override
        public Z80Registers getRegisters() {
            return registers;
        }

        public void setRegisters(Z80Registers value) {
            registers = value;
        }

        @Override
        public void setInterruptMode2(byte interruptMode) {
            currentInterruptMode = interruptMode;
        }

        @Override
        public void stop(boolean isPause /* = false */) {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte readFromPort(byte portNumberLow, byte portNumberHigh) {
            return ports[NumberUtils.createShort(portNumberLow, portNumberHigh) & 0xffff];
        }

        @Override
        public void writeToPort(byte portNumberLow, byte portNumberHigh, byte value) {
            ports[NumberUtils.createShort(portNumberLow, portNumberHigh) & 0xffff] = value;
        }
    }

//#endregion

    protected short createAddressFixture() {
        return createAddressFixture((short) 1);
    }

    /**
     * TODO some tests doesn't allow address 1
     * @param excepts should be sorted (binary search is used inside)
     */
    protected short createAddressFixture(short... excepts) {
        short s;
        do {
            s = fixture.create(Short.TYPE);
        } while (Arrays.binarySearch(excepts, s) >= 0);
//Debug.println("address fixture: " + s);
        return s;
    }
}
