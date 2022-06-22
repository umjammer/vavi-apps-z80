package konamiman;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import com.flextrade.jfixture.JFixture;
import konamiman.DependenciesInterfaces.IMemory;
import konamiman.Enums.MemoryAccessEventType;
import konamiman.Enums.MemoryAccessMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Inc;
import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
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

    Z80ProcessorForTests Sut; public Z80ProcessorForTests getSut() { return Sut; } public void setSut(Z80ProcessorForTests value) { Sut = value; }
    JFixture Fixture; public JFixture getFixture() { return Fixture; } public void setFixture(JFixture value) { Fixture = value; }
    IMemory Memory; public IMemory getMemory() { return Memory; } public void setMemory(IMemory value) { Memory = value; }
    IMemory Ports; public IMemory getPorts() { return Ports; } public void setPorts(IMemory value) { Ports = value; }

    @BeforeEach
    public void Setup() {
        Fixture = new JFixture();

        Sut = new Z80ProcessorForTests();
        Sut.SetInstructionExecutionContextToNonNull();

        Memory = mock(IMemory.class);
        Sut.setMemory(Memory);
        Ports = mock(IMemory.class);
        Sut.setPortsSpace(Ports);
    }

    @Test
    public void Can_create_instances() {
        assertNotNull(Sut);
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
        var address = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);
        var memory = isPort ? Ports : Memory;

        when(memory.get(address)).thenReturn(value);
        SetAccessMode(address, accessMode, isPort);

        var actual = Read(address, isPort);

        assertEquals(value, actual);
        verify(memory.get(address));
    }

    private void SetAccessMode(byte address, MemoryAccessMode accessMode, boolean isPort) {
        if (isPort)
            Sut.SetPortsSpaceAccessMode(address, 1, accessMode);
        else
            Sut.SetMemoryAccessMode(address, 1, accessMode);
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
        var address = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);
        var memory = isPort ? Ports : Memory;

        when(memory.get(address)).thenThrow(IndexOutOfBoundsException.class);
        SetAccessMode(address, accessMode, isPort);

        Read(address, isPort);
    }

    byte Read(byte address, boolean isPort) {
        if (isPort)
            return Sut.ReadFromPort(address);
        else
            return Sut.readFromMemory(address);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void ReadFromMemory_fires_Before_event_with_appropriate_address_and_value(boolean isPort) {
        var address = Fixture.create(Byte.TYPE);

        var eventFired = new AtomicBoolean(false);

        Sut.MemoryAccess().addListener(args -> {
            if (args.getEventType() == BeforeReadEventType(isPort)) {
                eventFired.set(true);
                assertEquals(Sut, args.getSource());
                assertEquals((byte) address, args.getAddress());
                assertEquals(0xFF, args.getValue());
            }
        });

        Read(address, isPort);

        assertTrue(eventFired.get());
    }

    MemoryAccessEventType BeforeReadEventType(boolean isPort) {
        if (isPort)
            return MemoryAccessEventType.BeforePortRead;
        else
            return MemoryAccessEventType.BeforeMemoryRead;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void ReadFromMemory_fires_After_event_with_appropriate_address_and_value_if_memory_is_accessed(boolean isPort) {
        var address = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);
        var memory = isPort ? Ports : Memory;

        var eventFired = new AtomicBoolean(false);

        when(memory.get(address)).thenReturn(value);

        Sut.MemoryAccess().addListener(args -> {
            if (args.getEventType() == AfterReadEventType(isPort)) {
                eventFired.set(true);
                assertEquals(Sut, args.getSource());
                assertEquals((byte) address, args.getAddress());
                assertEquals(value, args.getValue());
            }
        });

        var actual = Read(address, isPort);

        assertTrue(eventFired.get());
        assertEquals(value, actual);
    }

    MemoryAccessEventType AfterReadEventType(boolean isPort) {
        if (isPort)
            return MemoryAccessEventType.AfterPortRead;
        else
            return MemoryAccessEventType.AfterMemoryRead;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void ReadFromMemory_fires_After_event_with_same_value_as_Before_event_if_memory_is_not_accessed(boolean isPort) {
        var address = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);
        var memory = isPort ? Ports : Memory;

        var beforeEventFired = new AtomicBoolean(false);
        var afterEventFired = new AtomicBoolean(false);

        when(memory.get(address)).thenThrow(IndexOutOfBoundsException.class);
        SetAccessMode(address, MemoryAccessMode.NotConnected, isPort);

        Sut.MemoryAccess().addListener(args -> {
            if (args.getEventType() == BeforeReadEventType(isPort)) {
                beforeEventFired.set(true);
                assertEquals((byte) address, args.getAddress());
                args.setValue(value);
            }
            if (args.getEventType() == AfterReadEventType(isPort)) {
                afterEventFired.set(true);
                assertEquals((byte) address, args.getAddress());
                assertEquals(value, args.getValue());
            }
        });

        var actual = Read(address, isPort);

        assertTrue(beforeEventFired.get());
        assertTrue(afterEventFired.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void ReadFromMemory_returns_value_set_in_After_event(boolean isPort) {
        var address = Fixture.create(Byte.TYPE);
        var valueFromMemory = Fixture.create(Byte.TYPE);
        var valueFromEvent = Fixture.create(Byte.TYPE);
        var memory = isPort ? Ports : Memory;

        var eventFired = new AtomicBoolean(false);

        when(memory.get(address)).thenReturn(valueFromMemory);

        Sut.MemoryAccess().addListener(args -> {
            if (args.getEventType() == AfterReadEventType(isPort)) {
                eventFired.set(true);
                args.setValue(valueFromEvent);
            }
        });

        var actual = Read(address, isPort);

        assertTrue(eventFired.get());
        assertEquals(valueFromEvent, actual);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void ReadFromMemory_does_not_access_memory_if_Cancel_is_set_from_Before_event(boolean isPort) {
        var address = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);
        var memory = isPort ? Ports : Memory;

        when(memory.get(address)).thenThrow(IndexOutOfBoundsException.class);

        Sut.MemoryAccess().addListener(args -> {
            if (args.getEventType() == BeforeReadEventType(isPort)) {
                args.setCancelMemoryAccess(true);
                args.setValue(value);
            }
        });

        var actual = Read(address, isPort);

        assertEquals(value, actual);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void ReadFromMemory_propagates_Cancel_from_Before_to_after_event(boolean isPort) {
        var address = Fixture.create(Byte.TYPE);
        var memory = isPort ? Ports : Memory;

        when(memory.get(address)).thenThrow(IndexOutOfBoundsException.class);

        var eventFired = new AtomicBoolean(false);

        Sut.MemoryAccess().addListener(args -> {
            if (args.getEventType() == BeforeReadEventType(isPort)) {
                args.setCancelMemoryAccess(true);
            } else if (args.getEventType() == AfterReadEventType(isPort)) {
                eventFired.set(true);
                assertTrue(args.getCancelMemoryAccess());
            }
        });

        Read(address, isPort);

        assertTrue(eventFired.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void ReadFromMemory_enters_with_null_LocalState(boolean isPort) {
        var address = Fixture.create(Byte.TYPE);
        var memory = isPort ? Ports : Memory;

        var eventFired = new AtomicBoolean(false);

        Sut.MemoryAccess().addListener(args -> {
            if (args.getEventType() == BeforeReadEventType(isPort)) {
                eventFired.set(true);
                assertNull(args.getLocalUserState());
            }
        });

        Read(address, isPort);

        assertTrue(eventFired.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void ReadFromMemory_passes_LocalState_from_Before_to_After(boolean isPort) {
        var address = Fixture.create(Byte.TYPE);
        var localUserState = Fixture.create(Object.class);
        var memory = isPort ? Ports : Memory;

        var eventFired = new AtomicBoolean(false);

        Sut.MemoryAccess().addListener(args -> {
            if (args.getEventType() == BeforeReadEventType(isPort)) {
                args.setLocalUserState(localUserState);
            } else if (args.getEventType() == AfterReadEventType(isPort)) {
                eventFired.set(true);
                assertEquals(localUserState, args.getLocalUserState());
            }
        });

        Read(address, isPort);

        assertTrue(eventFired.get());
    }

//#endregion

//#region FetchNextOpcode

    @Test
    public void FetchNextOpcode_reads_from_address_pointed_by_PC() {
        var address = Fixture.create(Short.TYPE);
        var value = Fixture.create(Byte.TYPE);

        when(Memory.get(address)).thenReturn(value);
        Sut.getRegisters().setPC(address);

        var actual = Sut.fetchNextOpcode();

        assertEquals(value, actual);
        verify(Memory.get(address));
    }

    @Test
    public void FetchNextOpcode_increases_PC_by_one() {
        var address = Fixture.create(Short.TYPE);
        Sut.getRegisters().setPC(address);

        Sut.fetchNextOpcode();

        assertEquals(Inc(ToShort(address)), Sut.getRegisters().getPC());
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
        var address = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);
        var memory = isPort ? Ports : Memory;

        SetAccessMode(address, accessMode, isPort);

        Write(address, value, isPort);

        verify(memory).set(address, value);
    }

    void Write(byte address, byte value, boolean isPort) {
        if (isPort)
            Sut.WriteToPort(address, value);
        else
            Sut.WriteToMemory(address, value);
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
        var address = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);
        var memory = isPort ? Ports : Memory;

        doThrow(IndexOutOfBoundsException.class).when(memory).set(address, value);
        SetAccessMode(address, accessMode, isPort);

        Write(address, value, isPort);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void WriteMemory_fires_Before_event_with_appropriate_address_and_value(boolean isPort) {
        var address = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);

        var eventFired = new AtomicBoolean(false);

        Sut.MemoryAccess().addListener(args ->
        {
            if (args.getEventType() == BeforeWriteEventType(isPort)) {
                eventFired.set(true);
                assertEquals(Sut, args.getSource());
                assertEquals((byte) address, args.getAddress());
                assertEquals(value, args.getValue());
            }
        });

        Write(address, value, isPort);

        assertTrue(eventFired.get());
    }

    MemoryAccessEventType BeforeWriteEventType(boolean isPort) {
        if (isPort)
            return MemoryAccessEventType.BeforePortWrite;
        else
            return MemoryAccessEventType.BeforeMemoryWrite;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void WriteToMemory_fires_After_event_with_appropriate_address_and_value_if_memory_is_accessed(boolean isPort) {
        var address = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);
        var memory = isPort ? Ports : Memory;

        var eventFired = new AtomicBoolean(false);

        Sut.MemoryAccess().addListener(args ->
        {
            if (args.getEventType() == AfterWriteEventType(isPort)) {
                eventFired.set(true);
                assertEquals(Sut, args.getSource());
                assertEquals((byte) address, args.getAddress());
                assertEquals(value, args.getValue());
            }
        });

        Write(address, value, isPort);

        assertTrue(eventFired.get());
        verify(memory).set(address, value);
    }

    MemoryAccessEventType AfterWriteEventType(boolean isPort) {
        if (isPort)
            return MemoryAccessEventType.AfterPortWrite;
        else
            return MemoryAccessEventType.AfterMemoryWrite;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void WriteToMemory_fires_After_event_with_same_value_as_Before_event_if_memory_is_not_accessed(boolean isPort) {
        var address = Fixture.create(Byte.TYPE);
        var originalValue = Fixture.create(Byte.TYPE);
        var modifiedValue = Fixture.create(Byte.TYPE);
        var memory = isPort ? Ports : Memory;

        var beforeEventFired = new AtomicBoolean(false);
        var afterEventFired = new AtomicBoolean(false);

        doThrow(IndexOutOfBoundsException.class).when(memory).set(address, modifiedValue);
        SetAccessMode(address, MemoryAccessMode.NotConnected, isPort);

        Sut.MemoryAccess().addListener(args -> {
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

        Write(address, originalValue, isPort);

        assertTrue(beforeEventFired.get());
        assertTrue(afterEventFired.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void WriteToMemory_writes_value_set_in_Before_event(boolean isPort) {
        var address = Fixture.create(Byte.TYPE);
        var originalValue = Fixture.create(Byte.TYPE);
        var modifiedValue = Fixture.create(Byte.TYPE);
        var memory = isPort ? Ports : Memory;

        var eventFired = new AtomicBoolean(false);

        Sut.MemoryAccess().addListener(args -> {
            if (args.getEventType() == BeforeWriteEventType(isPort)) {
                eventFired.set(true);
                args.setValue(modifiedValue);
            }
        });

        Write(address, originalValue, isPort);

        assertTrue(eventFired.get());
        verify(memory).set(address, modifiedValue);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void WriteToMemory_does_not_access_memory_if_Cancel_is_set_from_Before_event(boolean isPort) {
        var address = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);
        var memory = isPort ? Ports : Memory;

        var eventFired = new AtomicBoolean(false);

        doThrow(IndexOutOfBoundsException.class).when(memory).set(address, value);

        Sut.MemoryAccess().addListener(args -> {
            if (args.getEventType() == BeforeWriteEventType(isPort)) {
                eventFired.set(true);
                args.setCancelMemoryAccess(true);
                args.setValue(value);
            }
        });

        Write(address, value, isPort);

        assertTrue(eventFired.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void WriteToMemory_propagates_Cancel_from_Before_to_after_event(boolean isPort) {
        var address = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);
        var memory = isPort ? Ports : Memory;

        doThrow(IndexOutOfBoundsException.class).when(memory).set(address, value);

        var eventFired = new AtomicBoolean(false);

        Sut.MemoryAccess().addListener(args -> {
            if (args.getEventType() == BeforeWriteEventType(isPort)) {
                args.setCancelMemoryAccess(true);
            } else if (args.getEventType() == AfterWriteEventType(isPort)) {
                eventFired.set(true);
                assertTrue(args.getCancelMemoryAccess());
            }
        });

        Write(address, value, isPort);

        assertTrue(eventFired.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void WriteToMemory_enters_with_null_LocalState(boolean isPort) {
        var address = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);
        var memory = isPort ? Ports : Memory;

        var eventFired = new AtomicBoolean(false);

        Sut.MemoryAccess().addListener(args -> {
            if (args.getEventType() == BeforeWriteEventType(isPort)) {
                eventFired.set(true);
                assertNull(args.getLocalUserState());
            }
        });

        Write(address, value, isPort);

        assertTrue(eventFired.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void WriteToMemory_passes_LocalState_from_Before_to_After(boolean isPort) {
        var address = Fixture.create(Byte.TYPE);
        var localUserState = Fixture.create(Object.class);
        var value = Fixture.create(Byte.TYPE);
        var memory = isPort ? Ports : Memory;

        var eventFired = new AtomicBoolean(false);

        Sut.MemoryAccess().addListener(args ->
        {
            if (args.getEventType() == BeforeWriteEventType(isPort)) {
                args.setLocalUserState(localUserState);
            } else if (args.getEventType() == AfterWriteEventType(isPort)) {
                eventFired.set(true);
                assertEquals(localUserState, args.getLocalUserState());
            }
        });

        Write(address, value, isPort);

        assertTrue(eventFired.get());
    }

//#endregion

//#region Other

    @Test
    public void ReadAndWrite_operations_fail_if_not_execution_an_instruction() {
        var address = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);

        Sut.SetInstructionExecutionContextToNull();

        assertThrows(UnsupportedOperationException.class, () -> Sut.readFromMemory(address));
        assertThrows(UnsupportedOperationException.class, () -> Sut.ReadFromPort(address));
        assertThrows(UnsupportedOperationException.class, () -> Sut.fetchNextOpcode());
        assertThrows(UnsupportedOperationException.class, () -> Sut.WriteToMemory(address, value));
        assertThrows(UnsupportedOperationException.class, () -> Sut.WriteToPort(address, value));
    }

//#endregion
}
