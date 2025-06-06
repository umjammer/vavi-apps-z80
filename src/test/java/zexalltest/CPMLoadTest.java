package zexalltest;

import konamiman.z80.Z80Processor;
import konamiman.z80.Z80ProcessorImpl;
import konamiman.z80.events.BeforeInstructionFetchEvent;
import konamiman.z80.interfaces.Memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class CPMLoadTest {

    private static final byte DOLLAR = '$';
    private static final Path CPM_APPS_DIR = Path.of("src/test/resources/cpm22/apps");
    private static int dmaAddress = 0x0080;
    private static int fileIndex = 1;
    private static String[] mockFiles;

    public static void main(String[] args) throws Exception {
        // Load COM files from src/test/resources/cpm22/apps -
        // these are the files we'll be able to list using DIR command
        mockFiles = Files.list(CPM_APPS_DIR)
                .filter(p -> p.toString().toUpperCase().endsWith(".COM"))
                .map(p -> p.getFileName().toString().toUpperCase())
                .sorted()
                .toArray(String[]::new);

        // Load CP/M Operating System
        Path comFile = args.length == 0
                ? Path.of("src/test/resources/cpm22/cpm.sys")
                : Path.of(args[0]);

        byte[] operatingSystem = Files.readAllBytes(comFile);

        Z80Processor z80 = new Z80ProcessorImpl();
        z80.setClockSynchronizer(null);
        z80.setAutoStopOnRetWithStackEmpty(true);

        // Load CPM.SYS at 0xDC00
        z80.getMemory().setContents(0xDC00, operatingSystem, 0, null);

        // WBOOT vector at 0x0000: JMP 0xD007
        z80.getMemory().set(0x0000, (byte) 0xC3); // JMP
        z80.getMemory().set(0x0001, (byte) 0x07); // low byte
        z80.getMemory().set(0x0002, (byte) 0xD0); // high byte

        z80.beforeInstructionFetch().addListener(CPMLoadTest::handleBdosCall);

        System.err.println("Running CP/M program... Press Ctrl+C to quit.\n");
        z80.reset();
        z80.getRegisters().setPC((short) 0xD007); // WBOOT
        z80.continue_();
    }

    private static void handleBdosCall(BeforeInstructionFetchEvent args) {
        var z80 = (Z80Processor) args.getSource();
        int pc = z80.getRegisters().getPC() & 0xFFFF;

        if (pc == 0x0000) {
            z80.getRegisters().setPC((short) 0xD007); // WBOOT vector
            return;
        }

        // System.out.println("BDOS Address: " + String.format("0b%8s", Integer.toBinaryString(pc & 0xFF)).replace(' ', '0'));
        // System.out.printf("BDOS Call Address: 0x%02X%n", (byte) pc & 0xFF);
        switch (pc) {
            case 0x0006 -> { // BIOS CONIN
                try {
                    int input = System.in.read();
                    z80.getRegisters().setA((byte) input);
                } catch (IOException e) {
                    z80.getRegisters().setA((byte) 0);
                }
                z80.executeRet();
            }

            case 0x0009 -> { // BIOS CONOUT
                int c = z80.getRegisters().getC() & 0xFF;
                System.out.print((char) c);
                System.out.flush(); // <== ADD THIS
                z80.executeRet();
            }

            case 0x0005 -> { // BDOS entry
                int c = z80.getRegisters().getC() & 0xFF;

                switch (c) {
                    /* System Reset (Cold Boot) */
                    case 0 -> {
                        System.err.println("System reset requested (ignored)");
                        z80.getRegisters().setA((byte) 0);
                    }
                    /* Console input */
                    case 1 -> {
                        try {
                            z80.getRegisters().setA((byte) System.in.read());
                        } catch (IOException e) {
                            z80.getRegisters().setA((byte) 0);
                        }
                    }
                    /* Console Output */
                    case 2 -> System.out.print((char) (z80.getRegisters().getE() & 0xFF));
                    /* Input/output from/to paper tape (legacy, unused) */
                    case 3, 4 -> z80.getRegisters().setA((byte) 0x1A); // EOF or NOP
                    /* Send character to list device (usually printer) */
                    case 5 -> System.out.print((char) (z80.getRegisters().getE() & 0xFF)); // list output
                    /* Low-level I/O: check status or input/output char */
                    case 6 -> {
                        int e = z80.getRegisters().getE() & 0xFF;
                        if (e == 0xFF) z80.getRegisters().setA((byte) 0); // no char ready
                        else {
                            try {
                                z80.getRegisters().setA((byte) System.in.read());
                            } catch (IOException ex) {
                                z80.getRegisters().setA((byte) 0);
                            }
                        }
                    }
                    /* Read the current I/O byte */
                    case 7 -> z80.getRegisters().setA((byte) 0); // IOBYTE (ignored)
                    /* Set the current I/O byte */
                    case 8 -> {}
                    /* Print string at DE, terminated by `$` */
                    case 9 -> {
                        int addr = z80.getRegisters().getDE() & 0xFFFF;
                        byte b;
                        StringBuilder sb = new StringBuilder();
                        while ((b = z80.getMemory().get(addr++)) != DOLLAR) {
                            sb.append((char) b);
                        }
                        String s = sb.toString();
                        System.out.println(s); // force newline
                        System.out.flush();
                    }
                    /* Read Console Buffer */
                    case 10 -> {
                        int addr = z80.getRegisters().getDE() & 0xFFFF;
                        try {
                            String input = new java.util.Scanner(System.in).nextLine();
                            byte maxLen = z80.getMemory().get(addr); // Byte 0 = max buffer size
                            byte actualLen = (byte) Math.min(input.length(), maxLen);

                            z80.getMemory().set(addr + 1, actualLen); // Byte 1 = actual length
                            for (int i = 0; i < actualLen; i++) {
                                z80.getMemory().set(addr + 2 + i, (byte) input.charAt(i));
                            }
                        } catch (Exception e) {
                            z80.getMemory().set(addr + 1, (byte) 0); // zero-length input
                        }
                    }
                    /* A = 0xFF if character ready, else 0 */
                    case 11 -> z80.getRegisters().setA((byte) 0);
                    /* Get Current Disk, returns drive number (A=0, B=1) */
                    case 12 -> z80.getRegisters().setA((byte) 0); // Drive A
                    /* Reset Disk System */
                    case 13 -> z80.getRegisters().setA((byte) 0); // success
                    /* Select Disk, C = drive (0=A), returns 0 if OK */
                    case 14 -> z80.getRegisters().setA((byte) 0);
                    /* Open File, DE points to FCB */
                    case 15 -> {
                        int fcbAddr = z80.getRegisters().getDE() & 0xFFFF;
                        String name = formatFcb(z80.getMemory(), fcbAddr).toUpperCase();

                        /* Attempt to load COM file */
                        if (tryLoadComProgram(name, z80.getMemory())) {
                            z80.getRegisters().setPC((short) 0x0100);
                            return;
                        }

                        if (!formatFcb(z80.getMemory(), fcbAddr).equals(".")) {
                            System.err.println("OPEN: " + formatFcb(z80.getMemory(), fcbAddr));
                        }
                        // Simulate successful open
                        z80.getMemory().set(fcbAddr + 0x0B, (byte) 0x00); // Extent = 0
                        z80.getRegisters().setA((byte) 0x00); // success
                    }
                    /* Close File, DE points to FCB */
                    case 16 -> z80.getRegisters().setA((byte) 0); // Always OK
                    /* Search First, DE points to FCB with filename mask */
                    case 17 -> {
                        fileIndex = 1;

                        int fcbAddr = z80.getRegisters().getDE() & 0xFFFF;
                        byte[] fcbMask = new byte[32];
                        for (int i = 0; i < 32; i++) {
                            fcbMask[i] = z80.getMemory().get(fcbAddr + i);
                        }

                        // System.err.println("SearchFirst mask: " + formatFcb(z80.getMemory(), fcbAddr));

                        String fname = mockFiles[0];
                        if (matchesMask(fname, fcbMask)) {
                            byte[] fcb = createFcbEntry(fname);
                            z80.getMemory().setContents(fcbAddr, fcb, 0, 32);           // Update FCB
                            z80.getMemory().setContents(dmaAddress, fcb, 0, 32);        // Populate DMA for DIR
                            z80.getRegisters().setA((byte) 0x00); // success
                        }
                    }

                    /* Search Next, Continue previous directory search */
                    case 18 -> {
                        int fcbAddr = z80.getRegisters().getDE() & 0xFFFF;

                        if (fileIndex <= mockFiles.length - 1) {
                            String fname = mockFiles[fileIndex];
                            byte[] fcb = createFcbEntry(fname);
                            z80.getMemory().setContents(fcbAddr, fcb, 0, 32);           // Update FCB
                            z80.getMemory().setContents(dmaAddress, fcb, 0, 32);        // Populate DMA for DIR
                            z80.getRegisters().setA((byte) 0x00); // match

                            fileIndex++;
                        } else {
                            z80.getRegisters().setA((byte) 0xFF); // no match
                        }
                    }
                    /* Delete File, DE points to FCB */
                    case 19 -> z80.getRegisters().setA((byte) 0);
                    /* Read Sequential, DE points to FCB, reads next record */
                    case 20 -> z80.getRegisters().setA((byte) 0x01); // EOF
                    /* Write Sequential, DE points to FCB, writes record */
                    case 21 -> z80.getRegisters().setA((byte) 0); // write OK
                    /* Make File, DE points to FCB, creates file */
                    case 22 -> z80.getRegisters().setA((byte) 0); // create OK
                    /* Rename File, DE points to FCB, use second FCB for new name */
                    case 23 -> z80.getRegisters().setA((byte) 0); // rename OK
                    /* Return Login Vector, HL = bitmap of logged-in drives */
                    case 24 -> z80.getRegisters().setHL((short) 0x0001); // only A logged in
                    /* Get Current DMA Address, HL = DMA address */
                    case 25 -> z80.getRegisters().setHL((short) dmaAddress);
                    /* Set DMA Address, DE = new DMA address */
                    case 26 -> dmaAddress = z80.getRegisters().getDE() & 0xFFFF;
                    /* Get ALV (Allocation Vector), HL = pointer to allocation vector (internal) */
                    case 27 -> z80.getRegisters().setHL((short) 0); // dummy
                    /* Get/Set User Number, C=0x28, E=user (0–15), if E=FF: get only */
                    case 28 -> {
                        int e = z80.getRegisters().getE() & 0xFF;
                        if (e == 0xFF) z80.getRegisters().setA((byte) 0);
                        else z80.getRegisters().setA((byte) e);
                    }
                    /* Read Random Record, DE = FCB, uses random record fields */
                    case 29 -> z80.getRegisters().setA((byte) 0); /* Dummy */
                    /* Write Random Record, Same as above */
                    case 30 -> z80.getRegisters().setA((byte) 0); // write OK
                    /* Compute File Size, Updates FCB with total number of records */
                    case 31 -> z80.getRegisters().setA((byte) 0x22); // CP/M 2.2
                    /* Set Random Record, Computes and stores random record number in FCB */
                    case 32 -> z80.getRegisters().setA((byte) 0); // Return 0 = success
                    /* Reset Drive (optional), Used in CP/M 3+; not meaningful in 2.2 */
                    case 33 -> z80.getRegisters().setA((byte) 0); // ignored
                    default ->
                        System.err.printf("Unhandled BDOS function: C=0x%02X at PC=0x%04X\n", c, pc);
                }
                z80.executeRet();
            }
        }
    }

    private static boolean tryLoadComProgram(String name, Memory mem) {
        // Normalize name: ensure it ends with .COM (CP/M convention)
        if (!name.toUpperCase().endsWith(".COM")) {
            name += ".COM";
        }

        Path filePath = CPM_APPS_DIR.resolve(name);

        if (Files.exists(filePath)) {
            try {
                byte[] data = Files.readAllBytes(filePath);
                mem.setContents(0x0100, data, 0, data.length);
                return true;
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }

        return false;
    }

    /**
     * Byte | Description
     * -----|------------
     * 0    | User number (0–15 or 0xE5 for unused)
     * 1–8  | Filename (ASCII, space-padded)
     * 9–11 | Extension (ASCII, space-padded)
     * 12   | EX (extent number)
     * 13   | S1 (reserved)
     * 14   | S2 (reserved)
     * 15   | RC (records used in extent)
     * 16–31 | 16 bytes of disk block numbers (ignored here)
     */
    private static byte[] createFcbEntry(String filename) {
        byte[] fcb = new byte[32];
        fcb[0] = 0x00; // User number

        String[] parts = filename.toUpperCase().split("\\.");
        String name = parts[0];
        String ext = parts.length > 1 ? parts[1] : "";

        // Bytes 1–8: Filename (padded with spaces)
        for (int i = 0; i < 8; i++) {
            fcb[1 + i] = (i < name.length()) ? (byte) name.charAt(i) : (byte) ' ';
        }

        // Bytes 9–11: Extension (padded with spaces)
        for (int i = 0; i < 3; i++) {
            fcb[9 + i] = (i < ext.length()) ? (byte) ext.charAt(i) : (byte) ' ';
        }

        fcb[12] = 0x00; // EX (extent number)
        fcb[13] = 0x00; // S1
        fcb[14] = 0x00; // S2
        fcb[15] = 0x01; // RC (record count: must be nonzero to show up in DIR)

        // Fill fake allocation blocks (16 bytes). Any non-0xE5 will suffice.
        for (int i = 16; i < 32; i++) {
            fcb[i] = (byte) i; // dummy block numbers
        }

        return fcb;
    }

    private static String formatFcb(Memory mem, int addr) {
        StringBuilder name = new StringBuilder();
        StringBuilder ext = new StringBuilder();

        // Bytes 1–8: filename
        for (int i = 0; i < 8; i++) {
            byte b = mem.get(addr + 1 + i);
            if (b == ' ') break; // stop at padding
            name.append((char) b);
        }

        // Bytes 9–11: extension
        for (int i = 0; i < 3; i++) {
            byte b = mem.get(addr + 9 + i);
            if (b == ' ') break;
            ext.append((char) b);
        }

        return ext.length() > 0 ? name + "." + ext : name.toString();
    }

    private static boolean matchesMask(String fileName, byte[] fcb) {
        String[] parts = fileName.toUpperCase().split("\\.");
        String name = parts[0];
        String ext = parts.length > 1 ? parts[1] : "";

        // Match filename (8 chars)
        for (int i = 0; i < 8; i++) {
            char fc = (char) fcb[1 + i];
            char fn = i < name.length() ? name.charAt(i) : ' ';
            if (fc != '?' && fc != fn) return false;
        }

        // Match extension (3 chars)
        for (int i = 0; i < 3; i++) {
            char fc = (char) fcb[9 + i];
            char fe = i < ext.length() ? ext.charAt(i) : ' ';
            if (fc != '?' && fc != fe) return false;
        }

        return true;
    }
}