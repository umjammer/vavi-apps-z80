package zexalltest;

import konamiman.z80.Z80Processor;
import konamiman.z80.Z80ProcessorImpl;
import konamiman.z80.events.BeforeInstructionFetchEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static konamiman.z80.utils.NumberUtils.*;

class ConsoleTest {

    private static final byte DOLLAR = '$';

    public static void main(String[] args) throws Exception {
        Path comFile = args.length == 0
                ? Path.of("src/test/resources/console/console.com")
                : Path.of(args[0]);

        byte[] program = Files.readAllBytes(comFile);

        Z80Processor z80 = new Z80ProcessorImpl();
        z80.setClockSynchronizer(null);
        z80.setAutoStopOnRetWithStackEmpty(true);

        // Load COM program into memory starting at 0x100
        z80.getMemory().setContents(0x100, program, 0, null);

        z80.beforeInstructionFetch().addListener(ConsoleTest::handleBdosCall);

        System.err.println("Running COM program... Press Ctrl+C to quit.\n");

        z80.reset();
        z80.getRegisters().setPC((short) 0x100);
        z80.continue_();
    }

    private static void handleBdosCall(BeforeInstructionFetchEvent args) {
        var z80 = (Z80Processor) args.getSource();
        int pc = z80.getRegisters().getPC() & 0xFFFF;

        if (pc == 0x0000) {
            args.getExecutionStopper().stop(false);
            return;
        }

        if (pc != 0x0005) return;

        int c = z80.getRegisters().getC() & 0xFF;

        switch (c) {
            case 1 -> { // CONIN
                try {
                    int input = System.in.read();
                    z80.getRegisters().setA((byte) input);
                } catch (IOException e) {
                    z80.getRegisters().setA((byte) 0);
                }
            }
            case 2 -> { // CONOUT
                int e = z80.getRegisters().getE() & 0xFF;
                System.out.print((char) e);
            }
            case 9 -> { // PRINT $
                int addr = z80.getRegisters().getDE() & 0xFFFF;
                var out = new ArrayList<Byte>();
                byte b;
                while ((b = z80.getMemory().get(addr)) != DOLLAR) {
                    out.add(b);
                    addr++;
                }
                System.out.print(new String(toByteArray(out), StandardCharsets.US_ASCII));
            }
            default -> {
                System.err.printf("Unhandled BDOS function: C=0x%02X at PC=0x%04X\n", c, pc);
            }
        }

        z80.executeRet();
    }
}

