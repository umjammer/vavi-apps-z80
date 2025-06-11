/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package konamiman.z80;

import java.util.concurrent.atomic.AtomicInteger;

import com.flextrade.jfixture.JFixture;
import konamiman.z80.impls.PlainMemory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class Z80ProcessorTests_PortsAccess {

    Z80ProcessorForTests sut;
    JFixture fixture;

    @BeforeEach
    void setup() {
        fixture = new JFixture();

        sut = new Z80ProcessorForTests();
        sut.setPortsSpace(new PlainMemory(65536));
    }

    @Test
    void Test_IN_A_without_extended_port_access() {
        sut.getPortsSpace().set(0x34, (byte) 0xaa);
        sut.getPortsSpace().set(0x1234, (byte) 0xbb);

        sut.getRegisters().setA((byte) 0x12);
        execute((byte) 0xdb, (byte) 0x34); // IN A,(0x34)

        assertEquals((byte) 0xaa, sut.getRegisters().getA());
    }

    @Test
    void Test_IN_A_with_extended_port_access() {
        sut.setUseExtendedPortsSpace(true);

        sut.getPortsSpace().set(0x34, (byte) 0xaa);
        sut.getPortsSpace().set(0x1234, (byte) 0xbb);

        sut.getRegisters().setA((byte) 0x12);
        execute((byte) 0xdb, (byte) 0x34); // IN A,(0x34)

        assertEquals((byte) 0xbb, sut.getRegisters().getA());
    }

    @Test
    void Test_IN_r_without_extended_port_access() {
        sut.getPortsSpace().set(0x34, (byte) 0xaa);
        sut.getPortsSpace().set(0x1234, (byte) 0xbb);

        sut.getRegisters().setB((byte) 0x12);
        sut.getRegisters().setC((byte) 0x34);
        execute((byte) 0xed, (byte) 0x50); // IN D,(C)

        assertEquals((byte) 0xaa, sut.getRegisters().getD());
    }

    @Test
    void Test_IN_r_with_extended_port_access() {
        sut.setUseExtendedPortsSpace(true);

        sut.getPortsSpace().set(0x34, (byte) 0xaa);
        sut.getPortsSpace().set(0x1234, (byte) 0xbb);

        sut.getRegisters().setB((byte) 0x12);
        sut.getRegisters().setC((byte) 0x34);
        execute((byte) 0xed, (byte) 0x50); // IN D,(C)

        assertEquals((byte) 0xbb, sut.getRegisters().getD());
    }

    @Test
    void Test_INI_without_extended_port_access() {
        sut.getPortsSpace().set(0x34, (byte) 0xaa);
        sut.getPortsSpace().set(0x1234, (byte) 0xbb);

        sut.getRegisters().setB((byte) 0x12);
        sut.getRegisters().setC((byte) 0x34);
        sut.getRegisters().setHL((short) 0x1000);
        execute((byte) 0xed, (byte) 0xa2); // INI

        assertEquals((byte) 0xaa, sut.getMemory().get(0x1000));
    }

    @Test
    void Test_INI_with_extended_port_access() {
        sut.setUseExtendedPortsSpace(true);

        sut.getPortsSpace().set(0x34, (byte) 0xaa);
        sut.getPortsSpace().set(0x1234, (byte) 0xbb);

        sut.getRegisters().setB((byte) 0x12);
        sut.getRegisters().setC((byte) 0x34);
        sut.getRegisters().setHL((short) 0x1000);
        execute((byte) 0xed, (byte) 0xa2); // INI

        assertEquals((byte) 0xbb, sut.getMemory().get(0x1000));
    }

    @Test
    void Test_IND_without_extended_port_access() {
        sut.getPortsSpace().set(0x34, (byte) 0xaa);
        sut.getPortsSpace().set(0x1234, (byte) 0xbb);

        sut.getRegisters().setB((byte) 0x12);
        sut.getRegisters().setC((byte) 0x34);
        sut.getRegisters().setHL((short) 0x1000);
        execute((byte) 0xed, (byte) 0xaa); // IND

        assertEquals((byte) 0xaa, sut.getMemory().get(0x1000));
    }

    @Test
    void Test_IND_with_extended_port_access() {
        sut.setUseExtendedPortsSpace(true);

        sut.getPortsSpace().set(0x34, (byte) 0xaa);
        sut.getPortsSpace().set(0x1234, (byte) 0xbb);

        sut.getRegisters().setB((byte) 0x12);
        sut.getRegisters().setC((byte) 0x34);
        sut.getRegisters().setHL((short) 0x1000);
        execute((byte) 0xed, (byte) 0xaa); // INI

        assertEquals((byte) 0xbb, sut.getMemory().get(0x1000));
    }

    @Test
    void Test_INIR_without_extended_port_access() {
        sut.getPortsSpace().set(0x34, (byte) 0xaa);

        sut.getRegisters().setB((byte) 0x03);
        sut.getRegisters().setC((byte) 0x34);
        sut.getRegisters().setHL((short) 0x1000);
        execute((byte) 0xed, (byte) 0xb2); // INIR
        execute((byte) 0xed, (byte) 0xb2); // INIR
        execute((byte) 0xed, (byte) 0xb2); // INIR

        assertEquals((byte) 0xaa, sut.getMemory().get(0x1000));
        assertEquals((byte) 0xaa, sut.getMemory().get(0x1001));
        assertEquals((byte) 0xaa, sut.getMemory().get(0x1002));
    }

    @Test
    void Test_INIR_with_extended_port_access() {
        sut.setUseExtendedPortsSpace(true);

        sut.getPortsSpace().set(0x0334, (byte) 0xaa);
        sut.getPortsSpace().set(0x0234, (byte) 0xbb);
        sut.getPortsSpace().set(0x0134, (byte) 0xcc);

        sut.getRegisters().setB((byte) 0x03);
        sut.getRegisters().setC((byte) 0x34);
        sut.getRegisters().setHL((short) 0x1000);
        execute((byte) 0xed, (byte) 0xb2); // INIR
        execute((byte) 0xed, (byte) 0xb2); // INIR
        execute((byte) 0xed, (byte) 0xb2); // INIR

        assertEquals((byte) 0xaa, sut.getMemory().get(0x1000));
        assertEquals((byte) 0xbb, sut.getMemory().get(0x1001));
        assertEquals((byte) 0xcc, sut.getMemory().get(0x1002));
    }

    @Test
    void Test_INDR_without_extended_port_access() {
        sut.getPortsSpace().set(0x34, (byte) 0xaa);

        sut.getRegisters().setB((byte) 0x03);
        sut.getRegisters().setC((byte) 0x34);
        sut.getRegisters().setHL((short) 0x1000);
        execute((byte) 0xed, (byte) 0xba); // INDR
        execute((byte) 0xed, (byte) 0xba); // INDR
        execute((byte) 0xed, (byte) 0xba); // INDR

        assertEquals((byte) 0xaa, sut.getMemory().get(0x1000));
        assertEquals((byte) 0xaa, sut.getMemory().get(0x0fff));
        assertEquals((byte) 0xaa, sut.getMemory().get(0x0ffe));
    }

    @Test
    void Test_INDR_with_extended_port_access() {
        sut.setUseExtendedPortsSpace(true);

        sut.getPortsSpace().set(0x0334, (byte) 0xaa);
        sut.getPortsSpace().set(0x0234, (byte) 0xbb);
        sut.getPortsSpace().set(0x0134, (byte) 0xcc);

        sut.getRegisters().setB((byte) 0x03);
        sut.getRegisters().setC((byte) 0x34);
        sut.getRegisters().setHL((short) 0x1000);
        execute((byte) 0xed, (byte) 0xba); // INDR
        execute((byte) 0xed, (byte) 0xba); // INDR
        execute((byte) 0xed, (byte) 0xba); // INDR

        assertEquals((byte) 0xaa, sut.getMemory().get(0x1000));
        assertEquals((byte) 0xbb, sut.getMemory().get(0x0fff));
        assertEquals((byte) 0xcc, sut.getMemory().get(0x0ffe));
    }

    @Test
    void Test_OUT_A_without_extended_port_access() {
        sut.getRegisters().setA((byte) 0x12);
        execute((byte) 0xd3, (byte) 0x34); // OUT (0x34),A

        assertEquals((byte) 0x12, sut.getPortsSpace().get(0x34));
    }

    @Test
    void Test_OUT_A_with_extended_port_access() {
        sut.setUseExtendedPortsSpace(true);

        sut.getRegisters().setA((byte) 0x12);
        execute((byte) 0xd3, (byte) 0x34); // OUT (0x34),A

        assertEquals((byte) 0x12, sut.getPortsSpace().get(0x1234));
    }

    @Test
    void Test_OUT_r_without_extended_port_access() {
        sut.getRegisters().setB((byte) 0x12);
        sut.getRegisters().setC((byte) 0x34);
        sut.getRegisters().setD((byte) 0xaa);
        execute((byte) 0xed, (byte) 0x51); // OUT (C),D

        assertEquals((byte) 0xaa, sut.getPortsSpace().get(0x34));
    }

    @Test
    void Test_OUT_r_with_extended_port_access() {
        sut.setUseExtendedPortsSpace(true);

        sut.getRegisters().setB((byte) 0x12);
        sut.getRegisters().setC((byte) 0x34);
        sut.getRegisters().setD((byte) 0xaa);
        execute((byte) 0xed, (byte) 0x51); // OUT (C),D

        assertEquals((byte) 0xaa, sut.getPortsSpace().get(0x1234));
    }

    @Test
    void Test_OUTI_without_extended_port_access() {
        sut.getRegisters().setB((byte) 0x12);
        sut.getRegisters().setC((byte) 0x34);
        sut.getRegisters().setHL((short) 0x1000);
        sut.getMemory().set(0x1000, (byte) 0xaa);
        execute((byte) 0xed, (byte) 0xa3); // OUTI

        assertEquals((byte) 0xaa, sut.getPortsSpace().get(0x34));
    }

    @Test
    void Test_OUTI_with_extended_port_access() {
        sut.setUseExtendedPortsSpace(true);

        sut.getRegisters().setB((byte) 0x12);
        sut.getRegisters().setC((byte) 0x34);
        sut.getRegisters().setHL((short) 0x1000);
        sut.getMemory().set(0x1000, (byte) 0xaa);
        execute((byte) 0xed, (byte) 0xa3); // OUTI

        assertEquals((byte) 0xaa, sut.getPortsSpace().get(0x1234));
    }

    @Test
    void Test_OUTD_without_extended_port_access() {
        sut.getRegisters().setB((byte) 0x12);
        sut.getRegisters().setC((byte) 0x34);
        sut.getRegisters().setHL((short) 0x1000);
        sut.getMemory().set(0x1000, (byte) 0xaa);
        execute((byte) 0xed, (byte) 0xab); // OUTD

        assertEquals((byte) 0xaa, sut.getPortsSpace().get(0x34));
    }

    @Test
    void Test_OUTD_with_extended_port_access() {
        sut.setUseExtendedPortsSpace(true);

        sut.getRegisters().setB((byte) 0x12);
        sut.getRegisters().setC((byte) 0x34);
        sut.getRegisters().setHL((short) 0x1000);
        sut.getMemory().set(0x1000, (byte) 0xaa);
        execute((byte) 0xed, (byte) 0xab); // OUTD

        assertEquals((byte) 0xaa, sut.getPortsSpace().get(0x1234));
    }

    @Test
    void Test_OTIR_without_extended_port_access() {
        sut.getRegisters().setB((byte) 0x03);
        sut.getRegisters().setC((byte) 0x34);
        sut.getRegisters().setHL((short) 0x1000);
        sut.getMemory().set(0x1000, (byte) 0xaa);
        sut.getMemory().set(0x1001, (byte) 0xbb);
        sut.getMemory().set(0x1002, (byte) 0xcc);
        execute((byte) 0xed, (byte) 0xb3); // OTIR
        execute((byte) 0xed, (byte) 0xb3); // OTIR
        execute((byte) 0xed, (byte) 0xb3); // OTIR

        assertEquals((byte) 0xcc, sut.getPortsSpace().get(0x34));
    }

    @Test
    void Test_OTIR_with_extended_port_access() {
        sut.setUseExtendedPortsSpace(true);

        sut.getRegisters().setB((byte) 0x03);
        sut.getRegisters().setC((byte) 0x34);
        sut.getRegisters().setHL((short) 0x1000);
        sut.getMemory().set(0x1000, (byte) 0xaa);
        sut.getMemory().set(0x1001, (byte) 0xbb);
        sut.getMemory().set(0x1002, (byte) 0xcc);
        execute((byte) 0xed, (byte) 0xb3); // OTIR
        execute((byte) 0xed, (byte) 0xb3); // OTIR
        execute((byte) 0xed, (byte) 0xb3); // OTIR

        assertEquals((byte) 0xaa, sut.getPortsSpace().get(0x0334));
        assertEquals((byte) 0xbb, sut.getPortsSpace().get(0x0234));
        assertEquals((byte) 0xcc, sut.getPortsSpace().get(0x0134));
    }

    @Test
    void Test_OTDR_without_extended_port_access() {
        sut.getRegisters().setB((byte) 0x03);
        sut.getRegisters().setC((byte) 0x34);
        sut.getRegisters().setHL((short) 0x1000);
        sut.getMemory().set(0x1000, (byte) 0xaa);
        sut.getMemory().set(0x0fff, (byte) 0xbb);
        sut.getMemory().set(0x0ffe, (byte) 0xcc);
        execute((byte) 0xed, (byte) 0xbb); // OTDR
        execute((byte) 0xed, (byte) 0xbb); // OTDR
        execute((byte) 0xed, (byte) 0xbb); // OTDR

        assertEquals((byte) 0xcc, sut.getPortsSpace().get(0x34));
    }

    @Test
    void Test_OTDR_with_extended_port_access() {
        sut.setUseExtendedPortsSpace(true);

        sut.getRegisters().setB((byte) 0x03);
        sut.getRegisters().setC((byte) 0x34);
        sut.getRegisters().setHL((short) 0x1000);
        sut.getMemory().set(0x1000, (byte) 0xaa);
        sut.getMemory().set(0x0fff, (byte) 0xbb);
        sut.getMemory().set(0x0ffe, (byte) 0xcc);
        execute((byte) 0xed, (byte) 0xbb); // OTDR
        execute((byte) 0xed, (byte) 0xbb); // OTDR
        execute((byte) 0xed, (byte) 0xbb); // OTDR

        assertEquals((byte) 0xaa, sut.getPortsSpace().get(0x0334));
        assertEquals((byte) 0xbb, sut.getPortsSpace().get(0x0234));
        assertEquals((byte) 0xcc, sut.getPortsSpace().get(0x0134));
    }

    @Test
    void Test_memory_event_without_extended_port_access() {
        AtomicInteger address = new AtomicInteger();

        sut.memoryAccess().addListener(e -> {
            address.set(e.getAddress() & 0xffff);
        });

        sut.getRegisters().setA((byte) 0x12);
        execute((byte) 0xdb, (byte) 0x34); // IN A,(0x34)

        assertEquals(0x34, address.get());
    }

    @Test
    void Test_memory_event_with_extended_port_access() {
        sut.setUseExtendedPortsSpace(true);

        AtomicInteger address = new AtomicInteger();

        sut.memoryAccess().addListener(e -> {
            address.set(e.getAddress() & 0xffff);
        });

        sut.getRegisters().setA((byte) 0x12);
        execute((byte) 0xdb, (byte) 0x34); // IN A,(0x34)

        assertEquals(0x1234, address.get());
    }

    void execute(byte... opcodes) {
        sut.getMemory().setContents(0, opcodes, 0, null);
        sut.getRegisters().setPC((short) 0);
        sut.executeNextInstruction();
    }
}
