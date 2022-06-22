package zexalltest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import konamiman.EventArgs.BeforeInstructionFetchEventArgs;
import konamiman.IZ80Processor;
import konamiman.Z80Processor;

import static konamiman.DataTypesAndUtils.NumberUtils.GetHighByte;
import static konamiman.DataTypesAndUtils.NumberUtils.GetLowByte;
import static konamiman.Z80Processor.toByteArray;


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

        var z80 = new Z80Processor();
        z80.setClockSynchronizer(null);
        z80.setAutoStopOnRetWithStackEmpty(true);

        var program = Files.readAllBytes(Paths.get(fileName));
        z80.getMemory().SetContents(0x100, program, 0, null);

        z80.getMemory().set(6, (byte) 0xFF);
        z80.getMemory().set(7, (byte) 0xFF);

        z80.BeforeInstructionFetch().addListener(Program::Z80OnBeforeInstructionFetch);

        SkipTests(z80, testsToSkip);

        long sw = System.currentTimeMillis();

        z80.Reset();
        z80.getRegisters().setPC((short) 0x100);
        z80.Continue();

        System.err.println("\r\nElapsed time: " + (System.currentTimeMillis() - sw));
    }

    private static void SkipTests(Z80Processor z80, int testsToSkipCount) {
        short loadTestsAddress = 0x120;
        short originalAddress = 0x13A;
        short newTestAddress = (short) (originalAddress + testsToSkipCount * 2);
        z80.getMemory().set(loadTestsAddress, GetLowByte(newTestAddress));
        z80.getMemory().set(loadTestsAddress + 1, GetHighByte(newTestAddress));
    }

    private static void Z80OnBeforeInstructionFetch(BeforeInstructionFetchEventArgs args) {

        // Absolutely minimum implementation of CP/M for ZEXALL and ZEXDOC to work

        var z80 = (IZ80Processor) args.getSource();

        if (z80.getRegisters().getPC() == 0) {
            args.getExecutionStopper().Stop(false);
            return;
        } else if (z80.getRegisters().getPC() != 5)
            return;

        var function = z80.getRegisters().getC();

        if (function == 9) {
            var messageAddress = z80.getRegisters().getDE();
            var bytesToPrint = new ArrayList<Byte>();
            byte byteToPrint;
            while ((byteToPrint = z80.getMemory().get(messageAddress)) != DollarCode) {
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

        z80.ExecuteRet();
    }
}

