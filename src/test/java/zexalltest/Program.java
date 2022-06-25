package zexalltest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import konamiman.z80.events.BeforeInstructionFetchEvent;
import konamiman.z80.Z80Processor;
import konamiman.z80.Z80ProcessorImpl;

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

    private static byte DollarCode;

    public static void main(String[] args) throws IOException {

        var fileName = args.length == 0 ? "zexall.com" : args[0];
        var testsToSkip = args.length >= 2 ? Integer.parseInt(args[1]) : 0;

        DollarCode = '$';

        var z80 = new Z80ProcessorImpl();
        z80.setClockSynchronizer(null);
        z80.setAutoStopOnRetWithStackEmpty(true);

        var program = Files.readAllBytes(Paths.get(fileName));
        z80.getMemory().setContents(0x100, program, 0, null);

        z80.getMemory().set(6, (byte) 0xFF);
        z80.getMemory().set(7, (byte) 0xFF);

        z80.beforeInstructionFetch().addListener(Program::Z80OnBeforeInstructionFetch);

        SkipTests(z80, testsToSkip);

        long sw = System.currentTimeMillis();

        z80.reset();
        z80.getRegisters().setPC((short) 0x100);
        z80.continue_();

        System.err.println("\r\nElapsed time: " + (System.currentTimeMillis() - sw));
    }

    private static void SkipTests(Z80Processor z80, int testsToSkipCount) {
        short loadTestsAddress = 0x120;
        short originalAddress = 0x13A;
        short newTestAddress = (short) (originalAddress + testsToSkipCount * 2);
        z80.getMemory().set(loadTestsAddress, getLowByte(newTestAddress));
        z80.getMemory().set(loadTestsAddress + 1, getHighByte(newTestAddress));
    }

    private static void Z80OnBeforeInstructionFetch(BeforeInstructionFetchEvent args) {

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
            var charToPrint = new String(new byte[] {byteToPrint}, StandardCharsets.US_ASCII).charAt(0);
            System.err.print(charToPrint);
        }

        z80.executeRet();
    }
}

