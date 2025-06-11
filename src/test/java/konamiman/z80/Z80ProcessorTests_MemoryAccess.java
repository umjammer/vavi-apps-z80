package konamiman.z80;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import com.flextrade.jfixture.JFixture;
import konamiman.z80.enums.MemoryAccessEventType;
import konamiman.z80.enums.MemoryAccessMode;
import konamiman.z80.interfaces.Memory;
import konamiman.z80.utils.NumberUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class Z80ProcessorTests_MemoryAccess {

    Z80ProcessorForTests sut;
    JFixture fixture;
    Memory memory;
    Memory ports;

    @BeforeEach
    public void Setup() {
        fixture = new JFixture();

        sut = new Z80ProcessorForTests();
        sut.setInstructionExecutionContextToNonNull();

        memory = mock(Memory.class);
        sut.setMemory(memory);
        ports = mock(Memory.class);
        when(ports.getSize()).thenReturn(256);
        sut.setPortsSpace(ports);
    }

    @Test
    public void Can_create_instances() {
        assertNotNull(sut);
    }

//#region ReadFromMemory and ReadFromPort

    static Stream<Arguments> source1() {
        return Stream.of(
                arguments(MemoryAccessMode.ReadAndWrite, false),
                arguments(MemoryAccessMode.ReadOnly, false),
                arguments(MemoryAccessMode.ReadAndWrite, true),
                arguments(MemoryAccessMode.ReadOnly, true)
        );
    }

    @ParameterizedTest
    @MethodSource("source1")
    public void ReadFromMemory_accesses_memory_if_memory_mode_is_ReadAndWrite_or_ReadOnly(MemoryAccessMode accessMode, boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var memory = isPort ? ports : this.memory;

        when(memory.get(address & 0xffff)).thenReturn(value);
        setAccessMode(address, accessMode, isPort);

        var actual = read(address, isPort);

        assertEquals(value, actual);
        verify(memory).get(address & 0xffff);
    }

    private void setAccessMode(byte address, MemoryAccessMode accessMode, boolean isPort) {
        if (isPort)
            sut.setPortsSpaceAccessMode(address, 1, accessMode);
        else
            sut.setMemoryAccessMode(address, 1, accessMode);
    }

    static Stream<Arguments> source2() {
        return Stream.of(
                arguments(MemoryAccessMode.NotConnected, false),
                arguments(MemoryAccessMode.WriteOnly, false),
                arguments(MemoryAccessMode.NotConnected, true),
                arguments(MemoryAccessMode.WriteOnly, true)
        );
    }

    @ParameterizedTest
    @MethodSource("source2")
    public void ReadFromMemory_does_not_access_memory_if_memory_mode_is_NotConnected_or_WriteOnly(MemoryAccessMode accessMode, boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var memory = isPort ? ports : this.memory;

        when(memory.get(address & 0xffff)).thenThrow(IndexOutOfBoundsException.class);
        setAccessMode(address, accessMode, isPort);

        read(address, isPort);
    }

    byte read(byte address, boolean isPort) {
        if (isPort)
            return sut.readFromPort(address);
        else
            return sut.readFromMemory(address);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void ReadFromMemory_fires_Before_event_with_appropriate_address_and_value(boolean isPort) {
        var address = fixture.create(Byte.TYPE);

        var eventFired = new AtomicBoolean(false);

        sut.memoryAccess().addListener(args -> {
            if (args.getEventType() == beforeReadEventType(isPort)) {
                eventFired.set(true);
                assertEquals(sut, args.getSource());
                assertEquals((byte) address, args.getAddress());
                assertEquals((byte) 0xFF, args.getValue());
            }
        });

        read(address, isPort);

        assertTrue(eventFired.get());
    }

    static MemoryAccessEventType beforeReadEventType(boolean isPort) {
        if (isPort)
            return MemoryAccessEventType.BeforePortRead;
        else
            return MemoryAccessEventType.BeforeMemoryRead;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void ReadFromMemory_fires_After_event_with_appropriate_address_and_value_if_memory_is_accessed(boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var memory = isPort ? ports : this.memory;

        var eventFired = new AtomicBoolean(false);

        when(memory.get(address & 0xffff)).thenReturn(value);

        sut.memoryAccess().addListener(args -> {
            if (args.getEventType() == afterReadEventType(isPort)) {
                eventFired.set(true);
                assertEquals(sut, args.getSource());
                assertEquals((byte) address, args.getAddress());
                assertEquals(value, args.getValue());
            }
        });

        var actual = read(address, isPort);

        assertTrue(eventFired.get());
        assertEquals(value, actual);
    }

    static MemoryAccessEventType afterReadEventType(boolean isPort) {
        if (isPort)
            return MemoryAccessEventType.AfterPortRead;
        else
            return MemoryAccessEventType.AfterMemoryRead;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void ReadFromMemory_fires_After_event_with_same_value_as_Before_event_if_memory_is_not_accessed(boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var memory = isPort ? ports : this.memory;

        var beforeEventFired = new AtomicBoolean(false);
        var afterEventFired = new AtomicBoolean(false);

        when(memory.get(address & 0xffff)).thenThrow(IndexOutOfBoundsException.class);
        setAccessMode(address, MemoryAccessMode.NotConnected, isPort);

        sut.memoryAccess().addListener(args -> {
            if (args.getEventType() == beforeReadEventType(isPort)) {
                beforeEventFired.set(true);
                assertEquals((byte) address, args.getAddress());
                args.setValue(value);
            }
            if (args.getEventType() == afterReadEventType(isPort)) {
                afterEventFired.set(true);
                assertEquals((byte) address, args.getAddress());
                assertEquals(value, args.getValue());
            }
        });

        var actual = read(address, isPort);

        assertTrue(beforeEventFired.get());
        assertTrue(afterEventFired.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void ReadFromMemory_returns_value_set_in_After_event(boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var valueFromMemory = fixture.create(Byte.TYPE);
        var valueFromEvent = fixture.create(Byte.TYPE);
        var memory = isPort ? ports : this.memory;

        var eventFired = new AtomicBoolean(false);

        when(memory.get(address & 0xffff)).thenReturn(valueFromMemory);

        sut.memoryAccess().addListener(args -> {
            if (args.getEventType() == afterReadEventType(isPort)) {
                eventFired.set(true);
                args.setValue(valueFromEvent);
            }
        });

        var actual = read(address, isPort);

        assertTrue(eventFired.get());
        assertEquals(valueFromEvent, actual);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void ReadFromMemory_does_not_access_memory_if_Cancel_is_set_from_Before_event(boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var memory = isPort ? ports : this.memory;

        when(memory.get(address & 0xffff)).thenThrow(IndexOutOfBoundsException.class);

        sut.memoryAccess().addListener(args -> {
            if (args.getEventType() == beforeReadEventType(isPort)) {
                args.setCancelMemoryAccess(true);
                args.setValue(value);
            }
        });

        var actual = read(address, isPort);

        assertEquals(value, actual);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void ReadFromMemory_propagates_Cancel_from_Before_to_after_event(boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var memory = isPort ? ports : this.memory;

        when(memory.get(address & 0xffff)).thenThrow(IndexOutOfBoundsException.class);

        var eventFired = new AtomicBoolean(false);

        sut.memoryAccess().addListener(args -> {
            if (args.getEventType() == beforeReadEventType(isPort)) {
                args.setCancelMemoryAccess(true);
            } else if (args.getEventType() == afterReadEventType(isPort)) {
                eventFired.set(true);
                assertTrue(args.getCancelMemoryAccess());
            }
        });

        read(address, isPort);

        assertTrue(eventFired.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void ReadFromMemory_enters_with_null_LocalState(boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var memory = isPort ? ports : this.memory;

        var eventFired = new AtomicBoolean(false);

        sut.memoryAccess().addListener(args -> {
            if (args.getEventType() == beforeReadEventType(isPort)) {
                eventFired.set(true);
                assertNull(args.getLocalUserState());
            }
        });

        read(address, isPort);

        assertTrue(eventFired.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void ReadFromMemory_passes_LocalState_from_Before_to_After(boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var localUserState = fixture.create(Object.class);
        var memory = isPort ? ports : this.memory;

        var eventFired = new AtomicBoolean(false);

        sut.memoryAccess().addListener(args -> {
            if (args.getEventType() == beforeReadEventType(isPort)) {
                args.setLocalUserState(localUserState);
            } else if (args.getEventType() == afterReadEventType(isPort)) {
                eventFired.set(true);
                assertEquals(localUserState, args.getLocalUserState());
            }
        });

        read(address, isPort);

        assertTrue(eventFired.get());
    }

//#endregion

//#region FetchNextOpcode

    @Test
    public void FetchNextOpcode_reads_from_address_pointed_by_PC() {
        var address = fixture.create(Short.TYPE);
        var value = fixture.create(Byte.TYPE);

        when(memory.get(address & 0xffff)).thenReturn(value);
        sut.getRegisters().setPC(address);

        var actual = sut.fetchNextOpcode();

        assertEquals(value, actual);
        verify(memory).get(address & 0xffff);
    }

    @Test
    public void FetchNextOpcode_increases_PC_by_one() {
        var address = fixture.create(Short.TYPE);
        sut.getRegisters().setPC(address);

        sut.fetchNextOpcode();

        assertEquals(NumberUtils.inc(address), sut.getRegisters().getPC());
    }

//#endregion

//#region WriteToMemory and WriteToPort

    static Stream<Arguments> source3() {
        return Stream.of(
                arguments(MemoryAccessMode.ReadAndWrite, false),
                arguments(MemoryAccessMode.WriteOnly, false),
                arguments(MemoryAccessMode.ReadAndWrite, true),
                arguments(MemoryAccessMode.WriteOnly, true)
        );
    }

    @ParameterizedTest
    @MethodSource("source3")
    public void WriteToMemory_accesses_memory_if_memory_mode_is_ReadAndWrite_or_WriteOnly(MemoryAccessMode accessMode, boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var memory = isPort ? ports : this.memory;

        setAccessMode(address, accessMode, isPort);

        write(address, value, isPort);

        verify(memory).set(address & 0xffff, value);
    }

    void write(byte address, byte value, boolean isPort) {
        if (isPort)
            sut.writeToPort(address, value);
        else
            sut.writeToMemory(address, value);
    }

    static Stream<Arguments> source4() {
        return Stream.of(
                arguments(MemoryAccessMode.NotConnected, false),
                arguments(MemoryAccessMode.ReadOnly, false),
                arguments(MemoryAccessMode.NotConnected, true),
                arguments(MemoryAccessMode.ReadOnly, true)
        );
    }

    @ParameterizedTest
    @MethodSource("source4")
    public void WriteToMemory_does_not_access_memory_if_memory_mode_is_NotConnected_or_ReadOnly(MemoryAccessMode accessMode, boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var memory = isPort ? ports : this.memory;

        doThrow(IndexOutOfBoundsException.class).when(memory).set(address & 0xffff, value);
        setAccessMode(address, accessMode, isPort);

        write(address, value, isPort);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void WriteMemory_fires_Before_event_with_appropriate_address_and_value(boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);

        var eventFired = new AtomicBoolean(false);

        sut.memoryAccess().addListener(args ->
        {
            if (args.getEventType() == BeforeWriteEventType(isPort)) {
                eventFired.set(true);
                assertEquals(sut, args.getSource());
                assertEquals((byte) address, args.getAddress());
                assertEquals(value, args.getValue());
            }
        });

        write(address, value, isPort);

        assertTrue(eventFired.get());
    }

    static MemoryAccessEventType BeforeWriteEventType(boolean isPort) {
        if (isPort)
            return MemoryAccessEventType.BeforePortWrite;
        else
            return MemoryAccessEventType.BeforeMemoryWrite;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void WriteToMemory_fires_After_event_with_appropriate_address_and_value_if_memory_is_accessed(boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var memory = isPort ? ports : this.memory;

        var eventFired = new AtomicBoolean(false);

        sut.memoryAccess().addListener(args ->
        {
            if (args.getEventType() == AfterWriteEventType(isPort)) {
                eventFired.set(true);
                assertEquals(sut, args.getSource());
                assertEquals((byte) address, args.getAddress());
                assertEquals(value, args.getValue());
            }
        });

        write(address, value, isPort);

        assertTrue(eventFired.get());
        verify(memory).set(address & 0xffff, value);
    }

    static MemoryAccessEventType AfterWriteEventType(boolean isPort) {
        if (isPort)
            return MemoryAccessEventType.AfterPortWrite;
        else
            return MemoryAccessEventType.AfterMemoryWrite;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void WriteToMemory_fires_After_event_with_same_value_as_Before_event_if_memory_is_not_accessed(boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var originalValue = fixture.create(Byte.TYPE);
        var modifiedValue = fixture.create(Byte.TYPE);
        var memory = isPort ? ports : this.memory;

        var beforeEventFired = new AtomicBoolean(false);
        var afterEventFired = new AtomicBoolean(false);

        doThrow(IndexOutOfBoundsException.class).when(memory).set(address & 0xffff, modifiedValue);
        setAccessMode(address, MemoryAccessMode.NotConnected, isPort);

        sut.memoryAccess().addListener(args -> {
            if (args.getEventType() == BeforeWriteEventType(isPort)) {
                beforeEventFired.set(true);
                assertEquals((byte) address, args.getAddress());
                args.setValue(modifiedValue);
            }
            if (args.getEventType() == AfterWriteEventType(isPort)) {
                afterEventFired.set(true);
                assertEquals((byte) address, args.getAddress());
                assertEquals(modifiedValue, args.getValue());
            }
        });

        write(address, originalValue, isPort);

        assertTrue(beforeEventFired.get());
        assertTrue(afterEventFired.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void WriteToMemory_writes_value_set_in_Before_event(boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var originalValue = fixture.create(Byte.TYPE);
        var modifiedValue = fixture.create(Byte.TYPE);
        var memory = isPort ? ports : this.memory;

        var eventFired = new AtomicBoolean(false);

        sut.memoryAccess().addListener(args -> {
            if (args.getEventType() == BeforeWriteEventType(isPort)) {
                eventFired.set(true);
                args.setValue(modifiedValue);
            }
        });

        write(address, originalValue, isPort);

        assertTrue(eventFired.get());
        verify(memory).set(address & 0xffff, modifiedValue);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void WriteToMemory_does_not_access_memory_if_Cancel_is_set_from_Before_event(boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var memory = isPort ? ports : this.memory;

        var eventFired = new AtomicBoolean(false);

        doThrow(IndexOutOfBoundsException.class).when(memory).set(address & 0xffff, value);

        sut.memoryAccess().addListener(args -> {
            if (args.getEventType() == BeforeWriteEventType(isPort)) {
                eventFired.set(true);
                args.setCancelMemoryAccess(true);
                args.setValue(value);
            }
        });

        write(address, value, isPort);

        assertTrue(eventFired.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void WriteToMemory_propagates_Cancel_from_Before_to_after_event(boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var memory = isPort ? ports : this.memory;

        doThrow(IndexOutOfBoundsException.class).when(memory).set(address & 0xffff, value);

        var eventFired = new AtomicBoolean(false);

        sut.memoryAccess().addListener(args -> {
            if (args.getEventType() == BeforeWriteEventType(isPort)) {
                args.setCancelMemoryAccess(true);
            } else if (args.getEventType() == AfterWriteEventType(isPort)) {
                eventFired.set(true);
                assertTrue(args.getCancelMemoryAccess());
            }
        });

        write(address, value, isPort);

        assertTrue(eventFired.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void WriteToMemory_enters_with_null_LocalState(boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var memory = isPort ? ports : this.memory;

        var eventFired = new AtomicBoolean(false);

        sut.memoryAccess().addListener(args -> {
            if (args.getEventType() == BeforeWriteEventType(isPort)) {
                eventFired.set(true);
                assertNull(args.getLocalUserState());
            }
        });

        write(address, value, isPort);

        assertTrue(eventFired.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void WriteToMemory_passes_LocalState_from_Before_to_After(boolean isPort) {
        var address = fixture.create(Byte.TYPE);
        var localUserState = fixture.create(Object.class);
        var value = fixture.create(Byte.TYPE);
        var memory = isPort ? ports : this.memory;

        var eventFired = new AtomicBoolean(false);

        sut.memoryAccess().addListener(args ->
        {
            if (args.getEventType() == BeforeWriteEventType(isPort)) {
                args.setLocalUserState(localUserState);
            } else if (args.getEventType() == AfterWriteEventType(isPort)) {
                eventFired.set(true);
                assertEquals(localUserState, args.getLocalUserState());
            }
        });

        write(address, value, isPort);

        assertTrue(eventFired.get());
    }

//#endregion

//#region Other

    @Test
    public void ReadAndWrite_operations_fail_if_not_execution_an_instruction() {
        var address = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);

        sut.setInstructionExecutionContextToNull();

        assertThrows(IllegalStateException.class, () -> sut.readFromMemory(address));
        assertThrows(IllegalStateException.class, () -> sut.readFromPort(address));
        assertThrows(IllegalStateException.class, () -> sut.fetchNextOpcode());
        assertThrows(IllegalStateException.class, () -> sut.writeToMemory(address, value));
        assertThrows(IllegalStateException.class, () -> sut.writeToPort(address, value));
    }

//#endregion
}
