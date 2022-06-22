package konamiman.InstructionsExecution;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.flextrade.jfixture.JFixture;
import konamiman.DataTypesAndUtils.Bit;
import konamiman.DataTypesAndUtils.NumberUtils;
import konamiman.DependenciesImplementations.Z80Registers;
import konamiman.DependenciesInterfaces.IZ80ProcessorAgent;
import konamiman.DependenciesInterfaces.IZ80Registers;
import konamiman.InstructionsExecution.Core.Z80InstructionExecutor;
import org.junit.jupiter.api.BeforeEach;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.GetHighByte;
import static konamiman.DataTypesAndUtils.NumberUtils.GetLowByte;
import static konamiman.DataTypesAndUtils.NumberUtils.Inc;
import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static konamiman.DataTypesAndUtils.NumberUtils.ToSignedByte;
import static org.junit.jupiter.api.Assertions.assertEquals;


abstract class InstructionsExecutionTestsBase {

    protected Z80InstructionExecutor Sut; public Z80InstructionExecutor getSut() { return Sut; }
    protected FakeProcessorAgent ProcessorAgent; public FakeProcessorAgent getProcessorAgent() { return ProcessorAgent; }
    protected IZ80Registers Registers; public IZ80Registers getRegisters() { return Registers; }
    protected JFixture Fixture; public JFixture getFixture() { return Fixture; }

    @BeforeEach
    public void Setup() {
        Sut = new Z80InstructionExecutor();
        Sut.setProcessorAgent(ProcessorAgent = new FakeProcessorAgent());
        Registers = ProcessorAgent.Registers;
        Sut.InstructionFetchFinished().addListener(e -> {
        });

        Fixture = new JFixture();
    }

//#region Auxiliary methods

    protected int nextFetchesAddress;

    protected void SetPortValue(byte portNumber, byte value) {
        ProcessorAgent.Ports[portNumber] = value;
    }

    protected byte GetPortValue(byte portNumber) {
        return ProcessorAgent.Ports[portNumber];
    }

    protected void SetMemoryContents(byte... opcodes) {
        SetMemoryContentsAt((byte) 0, opcodes);
    }

    protected void SetMemoryContentsAt(short address, byte... opcodes) {
        System.arraycopy(opcodes, 0, ProcessorAgent.Memory, address, opcodes.length);
        nextFetchesAddress = address + opcodes.length;
    }

    protected void ContinueSettingMemoryContents(byte... opcodes) {
        System.arraycopy(opcodes, 0, ProcessorAgent.Memory, nextFetchesAddress, opcodes.length);
        nextFetchesAddress += opcodes.length;
    }

    protected FakeInstructionExecutor NewFakeInstructionExecutor() {
        var sut = new FakeInstructionExecutor();
        sut.setProcessorAgent(ProcessorAgent = new FakeProcessorAgent());
        Registers = ProcessorAgent.Registers;
        sut.InstructionFetchFinished().addListener(e -> {
        });
        return sut;
    }

    protected <T> T GetReg(String name) {
        try {
            Method method = Registers.getClass().getDeclaredMethod("get" + name);
            return (T) method.invoke(Registers);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Bit GetFlag(String name) {
        if (name.length() == 1)
            name += "F";

        try {
            Method method = Registers.getClass().getDeclaredMethod("get" + name);
            return (Bit) method.invoke(Registers);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void SetReg(String regName, byte value) {
        try {
            RegProperty("set" + regName, Byte.TYPE).invoke(Registers, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void SetReg(String regName, short value) {
        try {
            RegProperty("set" + regName, Short.TYPE).invoke(Registers, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void SetFlag(String flagName, Bit value) {
        if (flagName.length() == 1)
            flagName += "F";

        try {
            RegProperty("set" + flagName, Bit.class).invoke(Registers, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Method RegProperty(String name, Class<?> arg) {
        try {
            return Registers.getClass().getDeclaredMethod(name, arg);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    protected int Execute(byte opcode, Byte prefix/*= null*/, byte... nextFetches) {
        return ExecuteAt((short) 0, opcode, prefix, nextFetches);
    }

    protected int ExecuteAt(short address, byte opcode, Byte prefix/*= null*/, byte... nextFetches) {
        Registers.setPC(Inc(address)); // Inc needed to simulate the first fetch made by the enclosing IZ80Processor
        if (prefix == null) {
            SetMemoryContentsAt(Inc(address), nextFetches);
            return Sut.execute(opcode);
        } else {
            SetMemoryContentsAt(Inc(address), opcode);
            ContinueSettingMemoryContents(nextFetches);

            return Sut.execute(prefix);
        }

    }

    protected Object IfIndexRegister(String regName, Object value, Object else_) {
        return regName.startsWith("IX") || regName.startsWith("IY") ? value : else_;
    }

    protected void AssertNoFlagsAreModified(byte opcode, Byte prefix/*= null*/) {
        var value = Fixture.create(Byte.TYPE);
        Registers.setF(value);
        Execute(opcode, prefix);

        assertEquals(value, Registers.getF());
    }

    protected void AssertSetsFlags(byte opcode, Byte prefix/*= null*/, String... flagNames) {
        AssertSetsFlags(null, opcode, prefix, flagNames);
    }

    protected void AssertSetsFlags(Runnable executor, byte opcode, Byte prefix/*= null*/, String... flagNames) {
        AssertSetsOrResetsFlags(opcode, Bit.ON, prefix, executor, flagNames);
    }

    protected void AssertResetsFlags(byte opcode, Byte prefix/*= null*/, String... flagNames) {
        AssertResetsFlags(null, opcode, prefix, flagNames);
    }

    protected void AssertResetsFlags(Runnable executor, byte opcode, Byte prefix/*= null*/, String... flagNames) {
        AssertSetsOrResetsFlags(opcode, Bit.OFF, prefix, executor, flagNames);
    }

    protected void AssertSetsOrResetsFlags(byte opcode, Bit expected, Byte prefix/*= null*/, Runnable executor/*= null*/, String... flagNames) {
        if (executor == null)
            executor = () -> Execute(opcode, prefix);

        var randomValues = Fixture.create(byte[].class);

        for (var value : randomValues) {
            for (var flag : flagNames)
                SetFlag(flag, expected.operatorNOT());

            Registers.setA(value);

            executor.run();

            for (var flag : flagNames)
                assertEquals(expected, GetFlag(flag));
        }
    }

    protected void AssertDoesNotChangeFlags(byte opcode, Byte prefix/*= null*/, String... flagNames) {
        AssertDoesNotChangeFlags(null, opcode, prefix, flagNames);
    }

    static <K, V> Map<K, V> toMap(String[] ss, Function<String, K> k, Function<String, V> v) {
        Map<K, V> map = new HashMap<>();
        for (String s : ss) {
            map.put(k.apply(s), v.apply(s));
        }
        return map;
    }

    protected void AssertDoesNotChangeFlags(Runnable executor, byte opcode, Byte prefix/*= null*/, String... flagNames) {
        if (executor == null)
            executor = () -> Execute(opcode, prefix);

        if (flagNames.length == 0)
            flagNames = new String[] {"C", "H", "S", "Z", "P", "N", "Flag3", "Flag5"};

        var randomFlags = toMap(flagNames, x -> x, x -> Fixture.create(Bit.class));

        for (var flag : flagNames)
            SetFlag(flag, randomFlags.get(flag));

        for (var i = 0; i <= Fixture.create(Byte.TYPE); i++) {
            executor.run();

            for (var flag : flagNames)
                assertEquals(randomFlags.get(flag), GetFlag(flag));
        }
    }

    protected void WriteShortToMemory(short address, short value) {
        ProcessorAgent.getMemory()[address] = GetLowByte(value);
        ProcessorAgent.getMemory()[address + 1] = GetHighByte(value);
    }

    protected short ReadShortFromMemory(short address) {
        return NumberUtils.createShort(ProcessorAgent.Memory[address], ProcessorAgent.Memory[address + 1]);
    }

    protected void SetupRegOrMem(String reg, byte value, byte offset/* = 0*/) {
        if (reg.equals("(HL)")) {
            var address = Fixture.create(Short.TYPE);
            if (address < 10) address = (short) (address + 10);
            ProcessorAgent.getMemory()[address] = value;
            Registers.setHL(ToShort(address));
        } else if (reg.startsWith(("(I"))) {
            var regName = reg.substring(1, 1 + 2);
            var address = Fixture.create(Short.TYPE);
            if (address < 1000) address = (short) (address + 1000);
            var realAddress = Add(address, ToSignedByte(offset));
            ProcessorAgent.getMemory()[realAddress] = value;
            SetReg(regName, ToShort(address));
        } else {
            SetReg(reg, value);
        }
    }

    protected byte ValueOfRegOrMem(String reg, byte offset/* = 0*/) {
        if (reg.equals("(HL)")) {
            return ProcessorAgent.Memory[Registers.getHL()];
        } else if (reg.startsWith(("(I"))) {
            var regName = reg.substring(1, 1 + 2);
            var address = Add(this.<Short>GetReg(regName), ToSignedByte(offset));
            return ProcessorAgent.Memory[address];
        } else {
            return this.<Byte>GetReg(reg);
        }
    }

    protected static Object[] GetBitInstructionsSource(byte baseOpcode, boolean includeLoadReg/* = true*/, boolean loopSevenBits/* = false*/) {
        final var bases = new Object[][] {
                new Object[] {"A", 7},
                new Object[] {"B", 0},
                new Object[] {"C", 1},
                new Object[] {"D", 2},
                new Object[] {"E", 3},
                new Object[] {"H", 4},
                new Object[] {"L", 5},
                new Object[] {"(HL)", 6}
        };

        var sources = new ArrayList<Object[]>();
        var bitsCount = loopSevenBits ? 7 : 0;
        for (var bit = 0; bit <= bitsCount; bit++) {
            for (var instr : bases) {
                var reg = (String) instr[0];
                var regCode = (int) instr[1];
                var opcode = baseOpcode | (bit << 3) | regCode;
                //srcReg, dest, opcode, prefix, bit
                sources.add(new Object[] {reg, null, (byte) opcode, null, bit});
            }

            for (var instr : bases) {
                var destReg = (String) instr[0];
                if (destReg.equals("(HL)")) destReg = "";
                if (!destReg.isEmpty() && !includeLoadReg) continue;
                var regCode = baseOpcode | (bit << 3) | (int) instr[1];
                for (var reg : new String[] {"(IX+n)", "(IY+n)"}) {
                    //srcReg, dest, opcode, prefix, bit
                    sources.add(new Object[] {
                            reg, destReg, regCode,
                            reg.charAt(2) == 'X' ? 0xDD : 0xFD,
                            bit});
                }
            }
        }

        return sources.toArray();
    }

    public static void ModifyTestCaseCreationForIndexRegs(String regName, /* ref */int[] regNamesArrayIndex, /* out */Byte[] prefix) {
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

    protected int ExecuteBit(byte opcode, Byte prefix/*= null*/, Byte offset/*= null*/) {
        if (prefix == null)
            return Execute(opcode, (byte) 0xCB);
        else
            return Execute((byte) 0xCB, prefix, offset, opcode);
    }

//#endregion

//#region ParityTable

    protected static final byte[] Parity = new byte[] {
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

    protected static class FakeInstructionExecutor extends Z80InstructionExecutor {
        public List<Byte> UnsupportedExecuted = new ArrayList<>();

        protected @Override int executeUnsopported_ED_Instruction(byte secondOpcodeByte) {
            UnsupportedExecuted.add(secondOpcodeByte);
            return super.executeUnsopported_ED_Instruction(secondOpcodeByte);
        }
    }

    protected static class FakeProcessorAgent implements IZ80ProcessorAgent {
        public FakeProcessorAgent() {
            Registers = new Z80Registers();
            Memory = new byte[65536];
            Ports = new byte[256];
        }

        private byte[] Memory;

        public byte[] getMemory() {
            return Memory;
        }

        public void setMemory(byte[] value) {
            Memory = value;
        }

        private byte[] Ports;

        public byte[] getPorts() {
            return Ports;
        }

        public void setPorts(byte[] value) {
            Ports = value;
        }

        private short MemoryPointer;

        public short getMemoryPointer() {
            return MemoryPointer;
        }

        public void setMemoryPointer(short value) {
            MemoryPointer = value;
        }

        private byte CurrentInterruptMode;

        public byte getCurrentInterruptMode() {
            return CurrentInterruptMode;
        }

        public byte fetchNextOpcode() {
            return Memory[Registers.incPC()];
        }

        public byte PeekNextOpcode() {
            return Memory[Registers.getPC()];
        }

        public byte readFromMemory(short address) {
            return Memory[address];
        }

        public void WriteToMemory(short address, byte value) {
            Memory[address] = value;
        }

        public byte ReadFromPort(byte portNumber) {
            return Ports[portNumber];
        }

        public void WriteToPort(byte portNumber, byte value) {
            Ports[portNumber] = value;
        }

        private IZ80Registers Registers;

        public IZ80Registers getRegisters() {
            return Registers;
        }

        public void setRegisters(IZ80Registers value) {
            Registers = value;
        }

        public void SetInterruptMode(byte interruptMode) {
            CurrentInterruptMode = interruptMode;
        }

        public void Stop(boolean isPause/* = false*/) {
            throw new UnsupportedOperationException();
        }
    }

//#endregion
}
