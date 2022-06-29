package zexalltest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import konamiman.z80.events.BeforeInstructionFetchEvent;
import konamiman.z80.Z80Processor;
import konamiman.z80.Z80ProcessorImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static konamiman.z80.utils.NumberUtils.add;
import static konamiman.z80.utils.NumberUtils.getHighByte;
import static konamiman.z80.utils.NumberUtils.getLowByte;
import static konamiman.z80.utils.NumberUtils.toByteArray;


/**
 * ZEXALL and ZEXDOC tests executor
 * Usage:
 * ZexallTest - run all ZEXALL tests
 * ZexallTest zexdoc.com - run all ZEXDOC tests
 * ZexallTests zexall.com|zexdoc.com n - run all tests after skipping the first n
 */
class Program {

    private static final byte DollarCode = '$';

    public static void main(String[] args) throws IOException {
        var fileName = args.length == 0 ? "src/test/resources/zexall.com" : args[0];
        var testsToSkip = args.length >= 1 ? Integer.parseInt(args[1]) : 0;

        var program = Files.readAllBytes(Paths.get(fileName));
        exec(program, testsToSkip);
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "zexall")
    void testZExeAll() throws Exception {
        var program = Files.readAllBytes(Paths.get("src/test/resources/zexall.com"));
        exec(program, 0);
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "zexdoc")
    void testZExeDoc() throws Exception {
        var program = Files.readAllBytes(Paths.get("src/test/resources/zexdoc.com"));
        exec(program, 0);
    }

    public static void exec(byte[] program, int testsToSkip) {
        var z80 = new Z80ProcessorImpl();
        z80.setClockSynchronizer(null);
        z80.setAutoStopOnRetWithStackEmpty(true);

        z80.getMemory().setContents(0x100, program, 0, null);

        z80.getMemory().set(6, (byte) 0xFF);
        z80.getMemory().set(7, (byte) 0xFF);

        z80.beforeInstructionFetch().addListener(Program::z80OnBeforeInstructionFetch);

        skipTests(z80, testsToSkip);

        long sw = System.currentTimeMillis();

        z80.reset();
        z80.getRegisters().setPC((short) 0x100);
        z80.continue_();

        System.err.println("\nElapsed time: " + (System.currentTimeMillis() - sw));
    }

    private static void skipTests(Z80Processor z80, int testsToSkipCount) {
        short loadTestsAddress = 0x120;
        short originalAddress = 0x13A;
        short newTestAddress = add(originalAddress, testsToSkipCount * 2);
        z80.getMemory().set(loadTestsAddress, getLowByte(newTestAddress));
        z80.getMemory().set(loadTestsAddress + 1, getHighByte(newTestAddress));
    }

    private static void z80OnBeforeInstructionFetch(BeforeInstructionFetchEvent args) {

        // Absolutely minimum implementation of CP/M for ZEXALL and ZEXDOC to work

        var z80 = (Z80Processor) args.getSource();

        if (z80.getRegisters().getPC() == 0) {
            args.getExecutionStopper().stop(false);
            return;
        } else if (z80.getRegisters().getPC() != 5)
            return;

        var function = z80.getRegisters().getC();

        if (function == 9) {
            var messageAddress = z80.getRegisters().getDE();
            var bytesToPrint = new ArrayList<Byte>();
            byte byteToPrint;
            while ((byteToPrint = z80.getMemory().get(messageAddress & 0xffff)) != DollarCode) {
                bytesToPrint.add(byteToPrint);
                messageAddress++;
            }

            var StringToPrint = new String(toByteArray(bytesToPrint), StandardCharsets.US_ASCII);
            System.err.print(StringToPrint);
        } else if (function == 2) {
            var byteToPrint = z80.getRegisters().getE();
            var charToPrint = (char) (byteToPrint & 0xff);
            System.err.print(charToPrint);
        }

        z80.executeRet();
    }
}

