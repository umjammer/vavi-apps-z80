package konamiman.z80.instructions.core;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;

import konamiman.z80.events.InstructionFetchFinishedEvent;
import konamiman.z80.interfaces.Z80InstructionExecutor;
import konamiman.z80.interfaces.Z80ProcessorAgent;
import konamiman.z80.interfaces.Z80Registers;
import konamiman.z80.utils.Bit;
import dotnet4j.util.compat.EventHandler;
import vavi.util.Debug;

import static konamiman.z80.utils.NumberUtils.add;
import static konamiman.z80.utils.NumberUtils.addAsInt;
import static konamiman.z80.utils.NumberUtils.between;
import static konamiman.z80.utils.NumberUtils.createShort;
import static konamiman.z80.utils.NumberUtils.dec;
import static konamiman.z80.utils.NumberUtils.getBit;
import static konamiman.z80.utils.NumberUtils.getHighByte;
import static konamiman.z80.utils.NumberUtils.getLowByte;
import static konamiman.z80.utils.NumberUtils.inc;
import static konamiman.z80.utils.NumberUtils.inc7Bits;
import static konamiman.z80.utils.NumberUtils.shift7Left;
import static konamiman.z80.utils.NumberUtils.shift7Right;
import static konamiman.z80.utils.NumberUtils.shiftRight;
import static konamiman.z80.utils.NumberUtils.siftLeft;
import static konamiman.z80.utils.NumberUtils.sub;
import static konamiman.z80.utils.NumberUtils.subAsInt;
import static konamiman.z80.utils.NumberUtils.withBit;


/**
 * Default implementation of {@link Z80InstructionExecutor}
 */
public class Z80InstructionExecutorImpl implements Z80InstructionExecutor {

    private Z80Registers registers;

    private Z80ProcessorAgent processorAgent; public Z80ProcessorAgent getProcessorAgent() { return processorAgent; } public void setProcessorAgent(Z80ProcessorAgent value) { processorAgent = value; }

    public Z80InstructionExecutorImpl() {
        initialize_CB_InstructionsTable();
        initialize_DDCB_InstructionsTable();
        initialize_ED_InstructionsTable();
        initialize_FDCB_InstructionsTable();
        initialize_SingleByte_InstructionsTable();
        generateParityTable();
    }

    public int execute(byte firstOpcodeByte) {
        registers = processorAgent.getRegisters();

        switch(firstOpcodeByte & 0xff) {
        case 0xCB:
            return execute_CB_Instruction();
        case 0xDD:
            return execute_DD_Instruction();
        case 0xED:
            return execute_ED_Instruction();
        case 0xFD:
            return execute_FD_Instruction();
        default:
            return execute_SingleByte_Instruction(firstOpcodeByte);
        }
    }

    private int execute_CB_Instruction() {
        incR();
        incR();
        return CB_InstructionExecutors[processorAgent.fetchNextOpcode() & 0xff].get();
    }

    private int execute_ED_Instruction() {
        incR();
        incR();
        var secondOpcodeByte = processorAgent.fetchNextOpcode();
        if (isUnsupportedInstruction(secondOpcodeByte))
            return executeUnsupported_ED_Instruction(secondOpcodeByte);
        else if((secondOpcodeByte & 0xff) >= 0xA0)
            return ED_Block_InstructionExecutors[(secondOpcodeByte & 0xff) - 0xA0].get();
        else
            return ED_InstructionExecutors[(secondOpcodeByte & 0xff) - 0x40].get();
    }

    private static boolean isUnsupportedInstruction(byte secondOpcodeByte) {
        return
            (secondOpcodeByte & 0xff) < 0x40 ||
            between(secondOpcodeByte, (byte) 0x80, (byte) 0x9F) ||
            between(secondOpcodeByte, (byte) 0xA4, (byte) 0xA7) ||
            between(secondOpcodeByte, (byte) 0xAC, (byte) 0xAF) ||
            between(secondOpcodeByte, (byte) 0xB4, (byte) 0xB7) ||
            between(secondOpcodeByte, (byte) 0xBC, (byte) 0xBF) ||
            (secondOpcodeByte & 0xff) > 0xBF;
    }

    /**
     * Executes an unsupported ED instruction, that is, an instruction whose opcode is
     * ED xx, where xx is 00-3F, 80-9F, A4-A7, AC-AF, B4-B7, BC-BF or C0-FF.
     * <p>
     * You can override this method in derived classes in order to implement a custom
     * behavior for these unsupported instructions (for example, to implement the multiplication
     * instructions of the R800 processor).
     *
     * @param secondOpcodeByte The opcode byte fetched after the 0xED.
     * @return The total amount of T states required for the instruction execution.
     */
    protected int executeUnsupported_ED_Instruction(byte secondOpcodeByte) {
        return NOP2();
    }

    private int execute_SingleByte_Instruction(byte firstOpcodeByte) {
        incR();
        return SingleByte_InstructionExecutors[firstOpcodeByte & 0xff].get();
    }

    private EventHandler<InstructionFetchFinishedEvent> instructionFetchFinished = new EventHandler<>();

    public EventHandler<InstructionFetchFinishedEvent> instructionFetchFinished() {
        return instructionFetchFinished;
    }

//#region Auxiliary methods

    /** TODO for performance, not thread safe */
    private InstructionFetchFinishedEvent instructionFetchFinishedEvent = new InstructionFetchFinishedEvent(this);

    private void fetchFinished(boolean isRet/*= false*/, boolean isHalt/*= false*/, boolean isLdSp/*= false*/, boolean isEiOrDi/*= false*/) {
        instructionFetchFinishedEvent.setRetInstruction(isRet);
        instructionFetchFinishedEvent.setHaltInstruction(isHalt);
        instructionFetchFinishedEvent.setLdSpInstruction(isLdSp);
        instructionFetchFinishedEvent.setEiOrDiInstruction(isEiOrDi);
        instructionFetchFinished.fireEvent(instructionFetchFinishedEvent);
    }

    private void incR() {
        processorAgent.getRegisters().setR(inc7Bits(processorAgent.getRegisters().getR()));
    }

    private short fetchWord() {
        return createShort(
            /*lowByte:*/ processorAgent.fetchNextOpcode(),
            /*highByte:*/ processorAgent.fetchNextOpcode());
    }

    private void writeShortToMemory(short address, short value) {
        processorAgent.writeToMemory(address, getLowByte(value));
        processorAgent.writeToMemory((short) (address + 1), getHighByte(value));
    }

    private short readShortFromMemory(short address) {
        return createShort(
                processorAgent.readFromMemory(address),
                processorAgent.readFromMemory((short) (address + 1)));
    }

    private void setFlags3and5From(byte value) {
        final int flags_3_5 = 0x28;

        registers.setF((byte) ((registers.getF() & ~flags_3_5) | (value & flags_3_5)));
    }

//#endregion

//#region Execute_xD_Instruction

    private int execute_DD_Instruction() {
        incR();
        var secondOpcodeByte = processorAgent.peekNextOpcode();

        if (secondOpcodeByte == (byte) 0xCB) {
            incR();
            processorAgent.fetchNextOpcode();
            var offset = processorAgent.fetchNextOpcode();
            return DDCB_InstructionExecutors[processorAgent.fetchNextOpcode() & 0xff].apply(offset);
        }

        incR();
        processorAgent.fetchNextOpcode();
        return execute_DD_Instructions(secondOpcodeByte);
    }

    private int execute_FD_Instruction() {
        incR();
        var secondOpcodeByte = processorAgent.peekNextOpcode();

        if (secondOpcodeByte == (byte) 0xCB) {
            incR();
            processorAgent.fetchNextOpcode();
            var offset = processorAgent.fetchNextOpcode();
            return FDCB_InstructionExecutors[processorAgent.fetchNextOpcode() & 0xff].apply(offset);
        }

        incR();
        processorAgent.fetchNextOpcode();
        return execute_FD_Instructions(secondOpcodeByte);
    }

//#endregion

//#region InstructionsTable.CB

    private Supplier<Byte>[] CB_InstructionExecutors;

    private void initialize_CB_InstructionsTable() {
        CB_InstructionExecutors = new Supplier[] {
                this::RLC_B,    // 00
                this::RLC_C,    // 01
                this::RLC_D,    // 02
                this::RLC_E,    // 03
                this::RLC_H,    // 04
                this::RLC_L,    // 05
                this::RLC_aHL,    // 06
                this::RLC_A,    // 07
                this::RRC_B,    // 08
                this::RRC_C,    // 09
                this::RRC_D,    // 0A
                this::RRC_E,    // 0B
                this::RRC_H,    // 0C
                this::RRC_L,    // 0D
                this::RRC_aHL,    // 0E
                this::RRC_A,    // 0F
                this::RL_B,    // 10
                this::RL_C,    // 11
                this::RL_D,    // 12
                this::RL_E,    // 13
                this::RL_H,    // 14
                this::RL_L,    // 15
                this::RL_aHL,    // 16
                this::RL_A,    // 17
                this::RR_B,    // 18
                this::RR_C,    // 19
                this::RR_D,    // 1A
                this::RR_E,    // 1B
                this::RR_H,    // 1C
                this::RR_L,    // 1D
                this::RR_aHL,    // 1E
                this::RR_A,    // 1F
                this::SLA_B,    // 20
                this::SLA_C,    // 21
                this::SLA_D,    // 22
                this::SLA_E,    // 23
                this::SLA_H,    // 24
                this::SLA_L,    // 25
                this::SLA_aHL,    // 26
                this::SLA_A,    // 27
                this::SRA_B,    // 28
                this::SRA_C,    // 29
                this::SRA_D,    // 2A
                this::SRA_E,    // 2B
                this::SRA_H,    // 2C
                this::SRA_L,    // 2D
                this::SRA_aHL,    // 2E
                this::SRA_A,    // 2F
                this::SLL_B,    // 30
                this::SLL_C,    // 31
                this::SLL_D,    // 32
                this::SLL_E,    // 33
                this::SLL_H,    // 34
                this::SLL_L,    // 35
                this::SLL_aHL,    // 36
                this::SLL_A,    // 37
                this::SRL_B,    // 38
                this::SRL_C,    // 39
                this::SRL_D,    // 3A
                this::SRL_E,    // 3B
                this::SRL_H,    // 3C
                this::SRL_L,    // 3D
                this::SRL_aHL,    // 3E
                this::SRL_A,    // 3F
                this::BIT_0_B,    // 40
                this::BIT_0_C,    // 41
                this::BIT_0_D,    // 42
                this::BIT_0_E,    // 43
                this::BIT_0_H,    // 44
                this::BIT_0_L,    // 45
                this::BIT_0_aHL,    // 46
                this::BIT_0_A,    // 47
                this::BIT_1_B,    // 48
                this::BIT_1_C,    // 49
                this::BIT_1_D,    // 4A
                this::BIT_1_E,    // 4B
                this::BIT_1_H,    // 4C
                this::BIT_1_L,    // 4D
                this::BIT_1_aHL,    // 4E
                this::BIT_1_A,    // 4F
                this::BIT_2_B,    // 50
                this::BIT_2_C,    // 51
                this::BIT_2_D,    // 52
                this::BIT_2_E,    // 53
                this::BIT_2_H,    // 54
                this::BIT_2_L,    // 55
                this::BIT_2_aHL,    // 56
                this::BIT_2_A,    // 57
                this::BIT_3_B,    // 58
                this::BIT_3_C,    // 59
                this::BIT_3_D,    // 5A
                this::BIT_3_E,    // 5B
                this::BIT_3_H,    // 5C
                this::BIT_3_L,    // 5D
                this::BIT_3_aHL,    // 5E
                this::BIT_3_A,    // 5F
                this::BIT_4_B,    // 60
                this::BIT_4_C,    // 61
                this::BIT_4_D,    // 62
                this::BIT_4_E,    // 63
                this::BIT_4_H,    // 64
                this::BIT_4_L,    // 65
                this::BIT_4_aHL,    // 66
                this::BIT_4_A,    // 67
                this::BIT_5_B,    // 68
                this::BIT_5_C,    // 69
                this::BIT_5_D,    // 6A
                this::BIT_5_E,    // 6B
                this::BIT_5_H,    // 6C
                this::BIT_5_L,    // 6D
                this::BIT_5_aHL,    // 6E
                this::BIT_5_A,    // 6F
                this::BIT_6_B,    // 70
                this::BIT_6_C,    // 71
                this::BIT_6_D,    // 72
                this::BIT_6_E,    // 73
                this::BIT_6_H,    // 74
                this::BIT_6_L,    // 75
                this::BIT_6_aHL,    // 76
                this::BIT_6_A,    // 77
                this::BIT_7_B,    // 78
                this::BIT_7_C,    // 79
                this::BIT_7_D,    // 7A
                this::BIT_7_E,    // 7B
                this::BIT_7_H,    // 7C
                this::BIT_7_L,    // 7D
                this::BIT_7_aHL,    // 7E
                this::BIT_7_A,    // 7F
                this::RES_0_B,    // 80
                this::RES_0_C,    // 81
                this::RES_0_D,    // 82
                this::RES_0_E,    // 83
                this::RES_0_H,    // 84
                this::RES_0_L,    // 85
                this::RES_0_aHL,    // 86
                this::RES_0_A,    // 87
                this::RES_1_B,    // 88
                this::RES_1_C,    // 89
                this::RES_1_D,    // 8A
                this::RES_1_E,    // 8B
                this::RES_1_H,    // 8C
                this::RES_1_L,    // 8D
                this::RES_1_aHL,    // 8E
                this::RES_1_A,    // 8F
                this::RES_2_B,    // 90
                this::RES_2_C,    // 91
                this::RES_2_D,    // 92
                this::RES_2_E,    // 93
                this::RES_2_H,    // 94
                this::RES_2_L,    // 95
                this::RES_2_aHL,    // 96
                this::RES_2_A,    // 97
                this::RES_3_B,    // 98
                this::RES_3_C,    // 99
                this::RES_3_D,    // 9A
                this::RES_3_E,    // 9B
                this::RES_3_H,    // 9C
                this::RES_3_L,    // 9D
                this::RES_3_aHL,    // 9E
                this::RES_3_A,    // 9F
                this::RES_4_B,    // A0
                this::RES_4_C,    // A1
                this::RES_4_D,    // A2
                this::RES_4_E,    // A3
                this::RES_4_H,    // A4
                this::RES_4_L,    // A5
                this::RES_4_aHL,    // A6
                this::RES_4_A,    // A7
                this::RES_5_B,    // A8
                this::RES_5_C,    // A9
                this::RES_5_D,    // AA
                this::RES_5_E,    // AB
                this::RES_5_H,    // AC
                this::RES_5_L,    // AD
                this::RES_5_aHL,    // AE
                this::RES_5_A,    // AF
                this::RES_6_B,    // B0
                this::RES_6_C,    // B1
                this::RES_6_D,    // B2
                this::RES_6_E,    // B3
                this::RES_6_H,    // B4
                this::RES_6_L,    // B5
                this::RES_6_aHL,    // B6
                this::RES_6_A,    // B7
                this::RES_7_B,    // B8
                this::RES_7_C,    // B9
                this::RES_7_D,    // BA
                this::RES_7_E,    // BB
                this::RES_7_H,    // BC
                this::RES_7_L,    // BD
                this::RES_7_aHL,    // BE
                this::RES_7_A,    // BF
                this::SET_0_B,    // C0
                this::SET_0_C,    // C1
                this::SET_0_D,    // C2
                this::SET_0_E,    // C3
                this::SET_0_H,    // C4
                this::SET_0_L,    // C5
                this::SET_0_aHL,    // C6
                this::SET_0_A,    // C7
                this::SET_1_B,    // C8
                this::SET_1_C,    // C9
                this::SET_1_D,    // CA
                this::SET_1_E,    // CB
                this::SET_1_H,    // CC
                this::SET_1_L,    // CD
                this::SET_1_aHL,    // CE
                this::SET_1_A,    // CF
                this::SET_2_B,    // D0
                this::SET_2_C,    // D1
                this::SET_2_D,    // D2
                this::SET_2_E,    // D3
                this::SET_2_H,    // D4
                this::SET_2_L,    // D5
                this::SET_2_aHL,    // D6
                this::SET_2_A,    // D7
                this::SET_3_B,    // D8
                this::SET_3_C,    // D9
                this::SET_3_D,    // DA
                this::SET_3_E,    // DB
                this::SET_3_H,    // DC
                this::SET_3_L,    // DD
                this::SET_3_aHL,    // DE
                this::SET_3_A,    // DF
                this::SET_4_B,    // E0
                this::SET_4_C,    // E1
                this::SET_4_D,    // E2
                this::SET_4_E,    // E3
                this::SET_4_H,    // E4
                this::SET_4_L,    // E5
                this::SET_4_aHL,    // E6
                this::SET_4_A,    // E7
                this::SET_5_B,    // E8
                this::SET_5_C,    // E9
                this::SET_5_D,    // EA
                this::SET_5_E,    // EB
                this::SET_5_H,    // EC
                this::SET_5_L,    // ED
                this::SET_5_aHL,    // EE
                this::SET_5_A,    // EF
                this::SET_6_B,    // F0
                this::SET_6_C,    // F1
                this::SET_6_D,    // F2
                this::SET_6_E,    // F3
                this::SET_6_H,    // F4
                this::SET_6_L,    // F5
                this::SET_6_aHL,    // F6
                this::SET_6_A,    // F7
                this::SET_7_B,    // F8
                this::SET_7_C,    // F9
                this::SET_7_D,    // FA
                this::SET_7_E,    // FB
                this::SET_7_H,    // FC
                this::SET_7_L,    // FD
                this::SET_7_aHL,    // FE
                this::SET_7_A    // FF
        };
    }

//#endregion

//#region InstructionsTable.DD

    private int execute_DD_Instructions(byte o) {
        return switch (o) {
            case (byte) 0x09 -> ADD_IX_BC();
            case (byte) 0x19 -> ADD_IX_DE();
            case (byte) 0x21 -> LD_IX_nn();
            case (byte) 0x22 -> LD_aa_IX();
            case (byte) 0x23 -> INC_IX();
            case (byte) 0x24 -> INC_IXH();
            case (byte) 0x25 -> DEC_IXH();
            case (byte) 0x26 -> LD_IXH_n();
            case (byte) 0x29 -> ADD_IX_IX();
            case (byte) 0x2A -> LD_IX_aa();
            case (byte) 0x2B -> DEC_IX();
            case (byte) 0x2C -> INC_IXL();
            case (byte) 0x2D -> DEC_IXL();
            case (byte) 0x2E -> LD_IXL_n();
            case (byte) 0x34 -> INC_aIX_plus_n();
            case (byte) 0x35 -> DEC_aIX_plus_n();
            case (byte) 0x36 -> LD_aIX_plus_n_N();
            case (byte) 0x39 -> ADD_IX_SP();
            case (byte) 0x44 -> LD_B_IXH();
            case (byte) 0x45 -> LD_B_IXL();
            case (byte) 0x46 -> LD_B_aIX_plus_n();
            case (byte) 0x4C -> LD_C_IXH();
            case (byte) 0x4D -> LD_C_IXL();
            case (byte) 0x4E -> LD_C_aIX_plus_n();
            case (byte) 0x54 -> LD_D_IXH();
            case (byte) 0x55 -> LD_D_IXL();
            case (byte) 0x56 -> LD_D_aIX_plus_n();
            case (byte) 0x5C -> LD_E_IXH();
            case (byte) 0x5D -> LD_E_IXL();
            case (byte) 0x5E -> LD_E_aIX_plus_n();
            case (byte) 0x60 -> LD_IXH_B();
            case (byte) 0x61 -> LD_IXH_C();
            case (byte) 0x62 -> LD_IXH_D();
            case (byte) 0x63 -> LD_IXH_E();
            case (byte) 0x64 -> LD_IXH_IXH();
            case (byte) 0x65 -> LD_IXH_IXL();
            case (byte) 0x66 -> LD_H_aIX_plus_n();
            case (byte) 0x67 -> LD_IXH_A();
            case (byte) 0x68 -> LD_IXL_B();
            case (byte) 0x69 -> LD_IXL_C();
            case (byte) 0x6A -> LD_IXL_D();
            case (byte) 0x6B -> LD_IXL_E();
            case (byte) 0x6C -> LD_IXL_H();
            case (byte) 0x6D -> LD_IXL_IXL();
            case (byte) 0x6E -> LD_L_aIX_plus_n();
            case (byte) 0x6F -> LD_IXL_A();
            case (byte) 0x70 -> LD_aIX_plus_n_B();
            case (byte) 0x71 -> LD_aIX_plus_n_C();
            case (byte) 0x72 -> LD_aIX_plus_n_D();
            case (byte) 0x73 -> LD_aIX_plus_n_E();
            case (byte) 0x74 -> LD_aIX_plus_n_H();
            case (byte) 0x75 -> LD_aIX_plus_n_L();
            case (byte) 0x77 -> LD_aIX_plus_n_A();
            case (byte) 0x7C -> LD_A_IXH();
            case (byte) 0x7D -> LD_A_IXL();
            case (byte) 0x7E -> LD_A_aIX_plus_n();
            case (byte) 0x84 -> ADD_A_IXH();
            case (byte) 0x85 -> ADD_A_IXL();
            case (byte) 0x86 -> ADD_A_aIX_plus_n();
            case (byte) 0x8C -> ADC_A_IXH();
            case (byte) 0x8D -> ADC_A_IXL();
            case (byte) 0x8E -> ADC_A_aIX_plus_n();
            case (byte) 0x94 -> SUB_IXH();
            case (byte) 0x95 -> SUB_IXL();
            case (byte) 0x96 -> SUB_aIX_plus_n();
            case (byte) 0x9C -> SBC_A_IXH();
            case (byte) 0x9D -> SBC_A_IXL();
            case (byte) 0x9E -> SBC_A_aIX_plus_n();
            case (byte) 0xA4 -> AND_IXH();
            case (byte) 0xA5 -> AND_IXL();
            case (byte) 0xA6 -> AND_aIX_plus_n();
            case (byte) 0xAC -> XOR_IXH();
            case (byte) 0xAD -> XOR_IXL();
            case (byte) 0xAE -> XOR_aIX_plus_n();
            case (byte) 0xB4 -> OR_IXH();
            case (byte) 0xB5 -> OR_IXL();
            case (byte) 0xB6 -> OR_aIX_plus_n();
            case (byte) 0xBC -> CP_IXH();
            case (byte) 0xBD -> CP_IXL();
            case (byte) 0xBE -> CP_aIX_plus_n();
            case (byte) 0xE1 -> POP_IX();
            case (byte) 0xE3 -> EX_aSP_IX();
            case (byte) 0xE5 -> PUSH_IX();
            case (byte) 0xE9 -> JP_aIX();
            case (byte) 0xF9 -> LD_SP_IX();
            // passed 'zexall' test, but if you want to emulate more precisely, pop 'r' register also like 'pc'.
            default -> { Debug.printf(Level.FINE, "DD %02x", o); registers.decPC(); yield NOP(); }
        };
    }

//#endregion

//#region InstructionsTable.DDCB

    private Function<Byte, Byte>[] DDCB_InstructionExecutors;

    private void initialize_DDCB_InstructionsTable() {
        DDCB_InstructionExecutors = Arrays.<Function<Byte, Byte>>asList(
                this::RLC_aIX_plus_n_and_load_B,    // 00
                this::RLC_aIX_plus_n_and_load_C,    // 01
                this::RLC_aIX_plus_n_and_load_D,    // 02
                this::RLC_aIX_plus_n_and_load_E,    // 03
                this::RLC_aIX_plus_n_and_load_H,    // 04
                this::RLC_aIX_plus_n_and_load_L,    // 05
                this::RLC_aIX_plus_n,    // 06
                this::RLC_aIX_plus_n_and_load_A,    // 07
                this::RRC_aIX_plus_n_and_load_B,    // 08
                this::RRC_aIX_plus_n_and_load_C,    // 09
                this::RRC_aIX_plus_n_and_load_D,    // 0A
                this::RRC_aIX_plus_n_and_load_E,    // 0B
                this::RRC_aIX_plus_n_and_load_H,    // 0C
                this::RRC_aIX_plus_n_and_load_L,    // 0D
                this::RRC_aIX_plus_n,    // 0E
                this::RRC_aIX_plus_n_and_load_A,    // 0F
                this::RL_aIX_plus_n_and_load_B,    // 10
                this::RL_aIX_plus_n_and_load_C,    // 11
                this::RL_aIX_plus_n_and_load_D,    // 12
                this::RL_aIX_plus_n_and_load_E,    // 13
                this::RL_aIX_plus_n_and_load_H,    // 14
                this::RL_aIX_plus_n_and_load_L,    // 15
                this::RL_aIX_plus_n,    // 16
                this::RL_aIX_plus_n_and_load_A,    // 17
                this::RR_aIX_plus_n_and_load_B,    // 18
                this::RR_aIX_plus_n_and_load_C,    // 19
                this::RR_aIX_plus_n_and_load_D,    // 1A
                this::RR_aIX_plus_n_and_load_E,    // 1B
                this::RR_aIX_plus_n_and_load_H,    // 1C
                this::RR_aIX_plus_n_and_load_L,    // 1D
                this::RR_aIX_plus_n,    // 1E
                this::RR_aIX_plus_n_and_load_A,    // 1F
                this::SLA_aIX_plus_n_and_load_B,    // 20
                this::SLA_aIX_plus_n_and_load_C,    // 21
                this::SLA_aIX_plus_n_and_load_D,    // 22
                this::SLA_aIX_plus_n_and_load_E,    // 23
                this::SLA_aIX_plus_n_and_load_H,    // 24
                this::SLA_aIX_plus_n_and_load_L,    // 25
                this::SLA_aIX_plus_n,    // 26
                this::SLA_aIX_plus_n_and_load_A,    // 27
                this::SRA_aIX_plus_n_and_load_B,    // 28
                this::SRA_aIX_plus_n_and_load_C,    // 29
                this::SRA_aIX_plus_n_and_load_D,    // 2A
                this::SRA_aIX_plus_n_and_load_E,    // 2B
                this::SRA_aIX_plus_n_and_load_H,    // 2C
                this::SRA_aIX_plus_n_and_load_L,    // 2D
                this::SRA_aIX_plus_n,    // 2E
                this::SRA_aIX_plus_n_and_load_A,    // 2F
                this::SLL_aIX_plus_n_and_load_B,    // 30
                this::SLL_aIX_plus_n_and_load_C,    // 31
                this::SLL_aIX_plus_n_and_load_D,    // 32
                this::SLL_aIX_plus_n_and_load_E,    // 33
                this::SLL_aIX_plus_n_and_load_H,    // 34
                this::SLL_aIX_plus_n_and_load_L,    // 35
                this::SLL_aIX_plus_n,    // 36
                this::SLL_aIX_plus_n_and_load_A,    // 37
                this::SRL_aIX_plus_n_and_load_B,    // 38
                this::SRL_aIX_plus_n_and_load_C,    // 39
                this::SRL_aIX_plus_n_and_load_D,    // 3A
                this::SRL_aIX_plus_n_and_load_E,    // 3B
                this::SRL_aIX_plus_n_and_load_H,    // 3C
                this::SRL_aIX_plus_n_and_load_L,    // 3D
                this::SRL_aIX_plus_n,    // 3E
                this::SRL_aIX_plus_n_and_load_A,    // 3F
                this::BIT_0_aIX_plus_n,    // 40
                this::BIT_0_aIX_plus_n,    // 41
                this::BIT_0_aIX_plus_n,    // 42
                this::BIT_0_aIX_plus_n,    // 43
                this::BIT_0_aIX_plus_n,    // 44
                this::BIT_0_aIX_plus_n,    // 45
                this::BIT_0_aIX_plus_n,    // 46
                this::BIT_0_aIX_plus_n,    // 47
                this::BIT_1_aIX_plus_n,    // 48
                this::BIT_1_aIX_plus_n,    // 49
                this::BIT_1_aIX_plus_n,    // 4A
                this::BIT_1_aIX_plus_n,    // 4B
                this::BIT_1_aIX_plus_n,    // 4C
                this::BIT_1_aIX_plus_n,    // 4D
                this::BIT_1_aIX_plus_n,    // 4E
                this::BIT_1_aIX_plus_n,    // 4F
                this::BIT_2_aIX_plus_n,    // 50
                this::BIT_2_aIX_plus_n,    // 51
                this::BIT_2_aIX_plus_n,    // 52
                this::BIT_2_aIX_plus_n,    // 53
                this::BIT_2_aIX_plus_n,    // 54
                this::BIT_2_aIX_plus_n,    // 55
                this::BIT_2_aIX_plus_n,    // 56
                this::BIT_2_aIX_plus_n,    // 57
                this::BIT_3_aIX_plus_n,    // 58
                this::BIT_3_aIX_plus_n,    // 59
                this::BIT_3_aIX_plus_n,    // 5A
                this::BIT_3_aIX_plus_n,    // 5B
                this::BIT_3_aIX_plus_n,    // 5C
                this::BIT_3_aIX_plus_n,    // 5D
                this::BIT_3_aIX_plus_n,    // 5E
                this::BIT_3_aIX_plus_n,    // 5F
                this::BIT_4_aIX_plus_n,    // 60
                this::BIT_4_aIX_plus_n,    // 61
                this::BIT_4_aIX_plus_n,    // 62
                this::BIT_4_aIX_plus_n,    // 63
                this::BIT_4_aIX_plus_n,    // 64
                this::BIT_4_aIX_plus_n,    // 65
                this::BIT_4_aIX_plus_n,    // 66
                this::BIT_4_aIX_plus_n,    // 67
                this::BIT_5_aIX_plus_n,    // 68
                this::BIT_5_aIX_plus_n,    // 69
                this::BIT_5_aIX_plus_n,    // 6A
                this::BIT_5_aIX_plus_n,    // 6B
                this::BIT_5_aIX_plus_n,    // 6C
                this::BIT_5_aIX_plus_n,    // 6D
                this::BIT_5_aIX_plus_n,    // 6E
                this::BIT_5_aIX_plus_n,    // 6F
                this::BIT_6_aIX_plus_n,    // 70
                this::BIT_6_aIX_plus_n,    // 71
                this::BIT_6_aIX_plus_n,    // 72
                this::BIT_6_aIX_plus_n,    // 73
                this::BIT_6_aIX_plus_n,    // 74
                this::BIT_6_aIX_plus_n,    // 75
                this::BIT_6_aIX_plus_n,    // 76
                this::BIT_6_aIX_plus_n,    // 77
                this::BIT_7_aIX_plus_n,    // 78
                this::BIT_7_aIX_plus_n,    // 79
                this::BIT_7_aIX_plus_n,    // 7A
                this::BIT_7_aIX_plus_n,    // 7B
                this::BIT_7_aIX_plus_n,    // 7C
                this::BIT_7_aIX_plus_n,    // 7D
                this::BIT_7_aIX_plus_n,    // 7E
                this::BIT_7_aIX_plus_n,    // 7F
                this::RES_0_aIX_plus_n_and_load_B,    // 80
                this::RES_0_aIX_plus_n_and_load_C,    // 81
                this::RES_0_aIX_plus_n_and_load_D,    // 82
                this::RES_0_aIX_plus_n_and_load_E,    // 83
                this::RES_0_aIX_plus_n_and_load_H,    // 84
                this::RES_0_aIX_plus_n_and_load_L,    // 85
                this::RES_0_aIX_plus_n,    // 86
                this::RES_0_aIX_plus_n_and_load_A,    // 87
                this::RES_1_aIX_plus_n_and_load_B,    // 88
                this::RES_1_aIX_plus_n_and_load_C,    // 89
                this::RES_1_aIX_plus_n_and_load_D,    // 8A
                this::RES_1_aIX_plus_n_and_load_E,    // 8B
                this::RES_1_aIX_plus_n_and_load_H,    // 8C
                this::RES_1_aIX_plus_n_and_load_L,    // 8D
                this::RES_1_aIX_plus_n,    // 8E
                this::RES_1_aIX_plus_n_and_load_A,    // 8F
                this::RES_2_aIX_plus_n_and_load_B,    // 90
                this::RES_2_aIX_plus_n_and_load_C,    // 91
                this::RES_2_aIX_plus_n_and_load_D,    // 92
                this::RES_2_aIX_plus_n_and_load_E,    // 93
                this::RES_2_aIX_plus_n_and_load_H,    // 94
                this::RES_2_aIX_plus_n_and_load_L,    // 95
                this::RES_2_aIX_plus_n,    // 96
                this::RES_2_aIX_plus_n_and_load_A,    // 97
                this::RES_3_aIX_plus_n_and_load_B,    // 98
                this::RES_3_aIX_plus_n_and_load_C,    // 99
                this::RES_3_aIX_plus_n_and_load_D,    // 9A
                this::RES_3_aIX_plus_n_and_load_E,    // 9B
                this::RES_3_aIX_plus_n_and_load_H,    // 9C
                this::RES_3_aIX_plus_n_and_load_L,    // 9D
                this::RES_3_aIX_plus_n,    // 9E
                this::RES_3_aIX_plus_n_and_load_A,    // 9F
                this::RES_4_aIX_plus_n_and_load_B,    // A0
                this::RES_4_aIX_plus_n_and_load_C,    // A1
                this::RES_4_aIX_plus_n_and_load_D,    // A2
                this::RES_4_aIX_plus_n_and_load_E,    // A3
                this::RES_4_aIX_plus_n_and_load_H,    // A4
                this::RES_4_aIX_plus_n_and_load_L,    // A5
                this::RES_4_aIX_plus_n,    // A6
                this::RES_4_aIX_plus_n_and_load_A,    // A7
                this::RES_5_aIX_plus_n_and_load_B,    // A8
                this::RES_5_aIX_plus_n_and_load_C,    // A9
                this::RES_5_aIX_plus_n_and_load_D,    // AA
                this::RES_5_aIX_plus_n_and_load_E,    // AB
                this::RES_5_aIX_plus_n_and_load_H,    // AC
                this::RES_5_aIX_plus_n_and_load_L,    // AD
                this::RES_5_aIX_plus_n,    // AE
                this::RES_5_aIX_plus_n_and_load_A,    // AF
                this::RES_6_aIX_plus_n_and_load_B,    // B0
                this::RES_6_aIX_plus_n_and_load_C,    // B1
                this::RES_6_aIX_plus_n_and_load_D,    // B2
                this::RES_6_aIX_plus_n_and_load_E,    // B3
                this::RES_6_aIX_plus_n_and_load_H,    // B4
                this::RES_6_aIX_plus_n_and_load_L,    // B5
                this::RES_6_aIX_plus_n,    // B6
                this::RES_6_aIX_plus_n_and_load_A,    // B7
                this::RES_7_aIX_plus_n_and_load_B,    // B8
                this::RES_7_aIX_plus_n_and_load_C,    // B9
                this::RES_7_aIX_plus_n_and_load_D,    // BA
                this::RES_7_aIX_plus_n_and_load_E,    // BB
                this::RES_7_aIX_plus_n_and_load_H,    // BC
                this::RES_7_aIX_plus_n_and_load_L,    // BD
                this::RES_7_aIX_plus_n,    // BE
                this::RES_7_aIX_plus_n_and_load_A,    // BF
                this::SET_0_aIX_plus_n_and_load_B,    // C0
                this::SET_0_aIX_plus_n_and_load_C,    // C1
                this::SET_0_aIX_plus_n_and_load_D,    // C2
                this::SET_0_aIX_plus_n_and_load_E,    // C3
                this::SET_0_aIX_plus_n_and_load_H,    // C4
                this::SET_0_aIX_plus_n_and_load_L,    // C5
                this::SET_0_aIX_plus_n,    // C6
                this::SET_0_aIX_plus_n_and_load_A,    // C7
                this::SET_1_aIX_plus_n_and_load_B,    // C8
                this::SET_1_aIX_plus_n_and_load_C,    // C9
                this::SET_1_aIX_plus_n_and_load_D,    // CA
                this::SET_1_aIX_plus_n_and_load_E,    // CB
                this::SET_1_aIX_plus_n_and_load_H,    // CC
                this::SET_1_aIX_plus_n_and_load_L,    // CD
                this::SET_1_aIX_plus_n,    // CE
                this::SET_1_aIX_plus_n_and_load_A,    // CF
                this::SET_2_aIX_plus_n_and_load_B,    // D0
                this::SET_2_aIX_plus_n_and_load_C,    // D1
                this::SET_2_aIX_plus_n_and_load_D,    // D2
                this::SET_2_aIX_plus_n_and_load_E,    // D3
                this::SET_2_aIX_plus_n_and_load_H,    // D4
                this::SET_2_aIX_plus_n_and_load_L,    // D5
                this::SET_2_aIX_plus_n,    // D6
                this::SET_2_aIX_plus_n_and_load_A,    // D7
                this::SET_3_aIX_plus_n_and_load_B,    // D8
                this::SET_3_aIX_plus_n_and_load_C,    // D9
                this::SET_3_aIX_plus_n_and_load_D,    // DA
                this::SET_3_aIX_plus_n_and_load_E,    // DB
                this::SET_3_aIX_plus_n_and_load_H,    // DC
                this::SET_3_aIX_plus_n_and_load_L,    // DD
                this::SET_3_aIX_plus_n,    // DE
                this::SET_3_aIX_plus_n_and_load_A,    // DF
                this::SET_4_aIX_plus_n_and_load_B,    // E0
                this::SET_4_aIX_plus_n_and_load_C,    // E1
                this::SET_4_aIX_plus_n_and_load_D,    // E2
                this::SET_4_aIX_plus_n_and_load_E,    // E3
                this::SET_4_aIX_plus_n_and_load_H,    // E4
                this::SET_4_aIX_plus_n_and_load_L,    // E5
                this::SET_4_aIX_plus_n,    // E6
                this::SET_4_aIX_plus_n_and_load_A,    // E7
                this::SET_5_aIX_plus_n_and_load_B,    // E8
                this::SET_5_aIX_plus_n_and_load_C,    // E9
                this::SET_5_aIX_plus_n_and_load_D,    // EA
                this::SET_5_aIX_plus_n_and_load_E,    // EB
                this::SET_5_aIX_plus_n_and_load_H,    // EC
                this::SET_5_aIX_plus_n_and_load_L,    // ED
                this::SET_5_aIX_plus_n,    // EE
                this::SET_5_aIX_plus_n_and_load_A,    // EF
                this::SET_6_aIX_plus_n_and_load_B,    // F0
                this::SET_6_aIX_plus_n_and_load_C,    // F1
                this::SET_6_aIX_plus_n_and_load_D,    // F2
                this::SET_6_aIX_plus_n_and_load_E,    // F3
                this::SET_6_aIX_plus_n_and_load_H,    // F4
                this::SET_6_aIX_plus_n_and_load_L,    // F5
                this::SET_6_aIX_plus_n,    // F6
                this::SET_6_aIX_plus_n_and_load_A,    // F7
                this::SET_7_aIX_plus_n_and_load_B,    // F8
                this::SET_7_aIX_plus_n_and_load_C,    // F9
                this::SET_7_aIX_plus_n_and_load_D,    // FA
                this::SET_7_aIX_plus_n_and_load_E,    // FB
                this::SET_7_aIX_plus_n_and_load_H,    // FC
                this::SET_7_aIX_plus_n_and_load_L,    // FD
                this::SET_7_aIX_plus_n,    // FE
                this::SET_7_aIX_plus_n_and_load_A    // FF
        ).toArray(Function[]::new);
    }

//#endregion

//#region InstructionsTable.ED

    private Supplier<Byte>[] ED_InstructionExecutors;
    private Supplier<Byte>[] ED_Block_InstructionExecutors;

    private void initialize_ED_InstructionsTable() {
        ED_InstructionExecutors = Arrays.<Supplier<Byte>>asList(
                this::IN_B_C,    // 40
                this::OUT_C_B,    // 41
                this::SBC_HL_BC,    // 42
                this::LD_aa_BC,    // 43
                this::NEG,    // 44
                this::RETN,    // 45
                this::IM_0,    // 46
                this::LD_I_A,    // 47
                this::IN_C_C,    // 48
                this::OUT_C_C,    // 49
                this::ADC_HL_BC,    // 4A
                this::LD_BC_aa,    // 4B
                this::NEG,    // 4C
                this::RETI,    // 4D
                this::IM_0,    // 4E
                this::LD_R_A,    // 4F
                this::IN_D_C,    // 50
                this::OUT_C_D,    // 51
                this::SBC_HL_DE,    // 52
                this::LD_aa_DE,    // 53
                this::NEG,    // 54
                this::RETN,    // 55
                this::IM_1,    // 56
                this::LD_A_I,    // 57
                this::IN_E_C,    // 58
                this::OUT_C_E,    // 59
                this::ADC_HL_DE,    // 5A
                this::LD_DE_aa,    // 5B
                this::NEG,    // 5C
                this::RETI,    // 5D
                this::IM_2,    // 5E
                this::LD_A_R,    // 5F
                this::IN_H_C,    // 60
                this::OUT_C_H,    // 61
                this::SBC_HL_HL,    // 62
                this::LD_aa_HL,    // 63
                this::NEG,    // 64
                this::RETN,    // 65
                this::IM_0,    // 66
                this::RRD,    // 67
                this::IN_L_C,    // 68
                this::OUT_C_L,    // 69
                this::ADC_HL_HL,    // 6A
                this::LD_HL_aa,    // 6B
                this::NEG,    // 6C
                this::RETI,    // 6D
                this::IM_0,    // 6E
                this::RLD,    // 6F
                this::IN_F_C,    // 70
                this::OUT_C_0,    // 71
                this::SBC_HL_SP,    // 72
                this::LD_aa_SP,    // 73
                this::NEG,    // 74
                this::RETN,    // 75
                this::IM_1,    // 76
                this::NOP2,    // 77
                this::IN_A_C,    // 78
                this::OUT_C_A,    // 79
                this::ADC_HL_SP,    // 7A
                this::LD_SP_aa,    // 7B
                this::NEG,    // 7C
                this::RETI,    // 7D
                this::IM_2,    // 7E
                this::NOP2    // 7F
        ).toArray(Supplier[]::new);

        ED_Block_InstructionExecutors = new Supplier[] {
                this::LDI,         // A0
                this::CPI,         // A1
                this::INI,         // A2
                this::OUTI,        // A3
                null, null, null, null,
                this::LDD,         // A8
                this::CPD,         // A9
                this::IND,         // AA
                this::OUTD,        // AB
                null, null, null, null,
                this::LDIR,        // B0
                this::CPIR,        // B1
                this::INIR,        // B2
                this::OTIR,        // B3
                null, null, null, null,
                this::LDDR,        // B8
                this::CPDR,        // B9
                this::INDR,        // BA
                this::OTDR,        // BB
        };
    }

//#endregion

//#region InstructionsTable.FD

    private int execute_FD_Instructions(byte o) {
        return switch (o) {
            case (byte) 0x09 -> ADD_IY_BC();
            case (byte) 0x19 -> ADD_IY_DE();
            case (byte) 0x21 -> LD_IY_nn();
            case (byte) 0x22 -> LD_aa_IY();
            case (byte) 0x23 -> INC_IY();
            case (byte) 0x24 -> INC_IYH();
            case (byte) 0x25 -> DEC_IYH();
            case (byte) 0x26 -> LD_IYH_n();
            case (byte) 0x29 -> ADD_IY_IY();
            case (byte) 0x2A -> LD_IY_aa();
            case (byte) 0x2B -> DEC_IY();
            case (byte) 0x2C -> INC_IYL();
            case (byte) 0x2D -> DEC_IYL();
            case (byte) 0x2E -> LD_IYL_n();
            case (byte) 0x34 -> INC_aIY_plus_n();
            case (byte) 0x35 -> DEC_aIY_plus_n();
            case (byte) 0x36 -> LD_aIY_plus_n_N();
            case (byte) 0x39 -> ADD_IY_SP();
            case (byte) 0x44 -> LD_B_IYH();
            case (byte) 0x45 -> LD_B_IYL();
            case (byte) 0x46 -> LD_B_aIY_plus_n();
            case (byte) 0x4C -> LD_C_IYH();
            case (byte) 0x4D -> LD_C_IYL();
            case (byte) 0x4E -> LD_C_aIY_plus_n();
            case (byte) 0x54 -> LD_D_IYH();
            case (byte) 0x55 -> LD_D_IYL();
            case (byte) 0x56 -> LD_D_aIY_plus_n();
            case (byte) 0x5C -> LD_E_IYH();
            case (byte) 0x5D -> LD_E_IYL();
            case (byte) 0x5E -> LD_E_aIY_plus_n();
            case (byte) 0x60 -> LD_IYH_B();
            case (byte) 0x61 -> LD_IYH_C();
            case (byte) 0x62 -> LD_IYH_D();
            case (byte) 0x63 -> LD_IYH_E();
            case (byte) 0x64 -> LD_IYH_IYH();
            case (byte) 0x65 -> LD_IYH_IYL();
            case (byte) 0x66 -> LD_H_aIY_plus_n();
            case (byte) 0x67 -> LD_IYH_A();
            case (byte) 0x68 -> LD_IYL_B();
            case (byte) 0x69 -> LD_IYL_C();
            case (byte) 0x6A -> LD_IYL_D();
            case (byte) 0x6B -> LD_IYL_E();
            case (byte) 0x6C -> LD_IYL_H();
            case (byte) 0x6D -> LD_IYL_IYL();
            case (byte) 0x6E -> LD_L_aIY_plus_n();
            case (byte) 0x6F -> LD_IYL_A();
            case (byte) 0x70 -> LD_aIY_plus_n_B();
            case (byte) 0x71 -> LD_aIY_plus_n_C();
            case (byte) 0x72 -> LD_aIY_plus_n_D();
            case (byte) 0x73 -> LD_aIY_plus_n_E();
            case (byte) 0x74 -> LD_aIY_plus_n_H();
            case (byte) 0x75 -> LD_aIY_plus_n_L();
            case (byte) 0x77 -> LD_aIY_plus_n_A();
            case (byte) 0x7C -> LD_A_IYH();
            case (byte) 0x7D -> LD_A_IYL();
            case (byte) 0x7E -> LD_A_aIY_plus_n();
            case (byte) 0x84 -> ADD_A_IYH();
            case (byte) 0x85 -> ADD_A_IYL();
            case (byte) 0x86 -> ADD_A_aIY_plus_n();
            case (byte) 0x8C -> ADC_A_IYH();
            case (byte) 0x8D -> ADC_A_IYL();
            case (byte) 0x8E -> ADC_A_aIY_plus_n();
            case (byte) 0x94 -> SUB_IYH();
            case (byte) 0x95 -> SUB_IYL();
            case (byte) 0x96 -> SUB_aIY_plus_n();
            case (byte) 0x9C -> SBC_A_IYH();
            case (byte) 0x9D -> SBC_A_IYL();
            case (byte) 0x9E -> SBC_A_aIY_plus_n();
            case (byte) 0xA4 -> AND_IYH();
            case (byte) 0xA5 -> AND_IYL();
            case (byte) 0xA6 -> AND_aIY_plus_n();
            case (byte) 0xAC -> XOR_IYH();
            case (byte) 0xAD -> XOR_IYL();
            case (byte) 0xAE -> XOR_aIY_plus_n();
            case (byte) 0xB4 -> OR_IYH();
            case (byte) 0xB5 -> OR_IYL();
            case (byte) 0xB6 -> OR_aIY_plus_n();
            case (byte) 0xBC -> CP_IYH();
            case (byte) 0xBD -> CP_IYL();
            case (byte) 0xBE -> CP_aIY_plus_n();
            case (byte) 0xE1 -> POP_IY();
            case (byte) 0xE3 -> EX_aSP_IY();
            case (byte) 0xE5 -> PUSH_IY();
            case (byte) 0xE9 -> JP_aIY();
            case (byte) 0xF9 -> LD_SP_IY();
            // passed 'zexall' test, but if you want to emulate more precisely, pop 'r' register also like 'pc'.
            default -> { Debug.printf(Level.FINE, "FD %02x", o); registers.decPC(); yield NOP(); }
        };
    }

//#endregion

//#region InstructionsTable.FDCB

    private Function<Byte, Byte>[] FDCB_InstructionExecutors;

    private void initialize_FDCB_InstructionsTable() {
        FDCB_InstructionExecutors = Arrays.<Function<Byte, Byte>>asList(
                this::RLC_aIY_plus_n_and_load_B,    // 00
                this::RLC_aIY_plus_n_and_load_C,    // 01
                this::RLC_aIY_plus_n_and_load_D,    // 02
                this::RLC_aIY_plus_n_and_load_E,    // 03
                this::RLC_aIY_plus_n_and_load_H,    // 04
                this::RLC_aIY_plus_n_and_load_L,    // 05
                this::RLC_aIY_plus_n,    // 06
                this::RLC_aIY_plus_n_and_load_A,    // 07
                this::RRC_aIY_plus_n_and_load_B,    // 08
                this::RRC_aIY_plus_n_and_load_C,    // 09
                this::RRC_aIY_plus_n_and_load_D,    // 0A
                this::RRC_aIY_plus_n_and_load_E,    // 0B
                this::RRC_aIY_plus_n_and_load_H,    // 0C
                this::RRC_aIY_plus_n_and_load_L,    // 0D
                this::RRC_aIY_plus_n,    // 0E
                this::RRC_aIY_plus_n_and_load_A,    // 0F
                this::RL_aIY_plus_n_and_load_B,    // 10
                this::RL_aIY_plus_n_and_load_C,    // 11
                this::RL_aIY_plus_n_and_load_D,    // 12
                this::RL_aIY_plus_n_and_load_E,    // 13
                this::RL_aIY_plus_n_and_load_H,    // 14
                this::RL_aIY_plus_n_and_load_L,    // 15
                this::RL_aIY_plus_n,    // 16
                this::RL_aIY_plus_n_and_load_A,    // 17
                this::RR_aIY_plus_n_and_load_B,    // 18
                this::RR_aIY_plus_n_and_load_C,    // 19
                this::RR_aIY_plus_n_and_load_D,    // 1A
                this::RR_aIY_plus_n_and_load_E,    // 1B
                this::RR_aIY_plus_n_and_load_H,    // 1C
                this::RR_aIY_plus_n_and_load_L,    // 1D
                this::RR_aIY_plus_n,    // 1E
                this::RR_aIY_plus_n_and_load_A,    // 1F
                this::SLA_aIY_plus_n_and_load_B,    // 20
                this::SLA_aIY_plus_n_and_load_C,    // 21
                this::SLA_aIY_plus_n_and_load_D,    // 22
                this::SLA_aIY_plus_n_and_load_E,    // 23
                this::SLA_aIY_plus_n_and_load_H,    // 24
                this::SLA_aIY_plus_n_and_load_L,    // 25
                this::SLA_aIY_plus_n,    // 26
                this::SLA_aIY_plus_n_and_load_A,    // 27
                this::SRA_aIY_plus_n_and_load_B,    // 28
                this::SRA_aIY_plus_n_and_load_C,    // 29
                this::SRA_aIY_plus_n_and_load_D,    // 2A
                this::SRA_aIY_plus_n_and_load_E,    // 2B
                this::SRA_aIY_plus_n_and_load_H,    // 2C
                this::SRA_aIY_plus_n_and_load_L,    // 2D
                this::SRA_aIY_plus_n,    // 2E
                this::SRA_aIY_plus_n_and_load_A,    // 2F
                this::SLL_aIY_plus_n_and_load_B,    // 30
                this::SLL_aIY_plus_n_and_load_C,    // 31
                this::SLL_aIY_plus_n_and_load_D,    // 32
                this::SLL_aIY_plus_n_and_load_E,    // 33
                this::SLL_aIY_plus_n_and_load_H,    // 34
                this::SLL_aIY_plus_n_and_load_L,    // 35
                this::SLL_aIY_plus_n,    // 36
                this::SLL_aIY_plus_n_and_load_A,    // 37
                this::SRL_aIY_plus_n_and_load_B,    // 38
                this::SRL_aIY_plus_n_and_load_C,    // 39
                this::SRL_aIY_plus_n_and_load_D,    // 3A
                this::SRL_aIY_plus_n_and_load_E,    // 3B
                this::SRL_aIY_plus_n_and_load_H,    // 3C
                this::SRL_aIY_plus_n_and_load_L,    // 3D
                this::SRL_aIY_plus_n,    // 3E
                this::SRL_aIY_plus_n_and_load_A,    // 3F
                this::BIT_0_aIY_plus_n,    // 40
                this::BIT_0_aIY_plus_n,    // 41
                this::BIT_0_aIY_plus_n,    // 42
                this::BIT_0_aIY_plus_n,    // 43
                this::BIT_0_aIY_plus_n,    // 44
                this::BIT_0_aIY_plus_n,    // 45
                this::BIT_0_aIY_plus_n,    // 46
                this::BIT_0_aIY_plus_n,    // 47
                this::BIT_1_aIY_plus_n,    // 48
                this::BIT_1_aIY_plus_n,    // 49
                this::BIT_1_aIY_plus_n,    // 4A
                this::BIT_1_aIY_plus_n,    // 4B
                this::BIT_1_aIY_plus_n,    // 4C
                this::BIT_1_aIY_plus_n,    // 4D
                this::BIT_1_aIY_plus_n,    // 4E
                this::BIT_1_aIY_plus_n,    // 4F
                this::BIT_2_aIY_plus_n,    // 50
                this::BIT_2_aIY_plus_n,    // 51
                this::BIT_2_aIY_plus_n,    // 52
                this::BIT_2_aIY_plus_n,    // 53
                this::BIT_2_aIY_plus_n,    // 54
                this::BIT_2_aIY_plus_n,    // 55
                this::BIT_2_aIY_plus_n,    // 56
                this::BIT_2_aIY_plus_n,    // 57
                this::BIT_3_aIY_plus_n,    // 58
                this::BIT_3_aIY_plus_n,    // 59
                this::BIT_3_aIY_plus_n,    // 5A
                this::BIT_3_aIY_plus_n,    // 5B
                this::BIT_3_aIY_plus_n,    // 5C
                this::BIT_3_aIY_plus_n,    // 5D
                this::BIT_3_aIY_plus_n,    // 5E
                this::BIT_3_aIY_plus_n,    // 5F
                this::BIT_4_aIY_plus_n,    // 60
                this::BIT_4_aIY_plus_n,    // 61
                this::BIT_4_aIY_plus_n,    // 62
                this::BIT_4_aIY_plus_n,    // 63
                this::BIT_4_aIY_plus_n,    // 64
                this::BIT_4_aIY_plus_n,    // 65
                this::BIT_4_aIY_plus_n,    // 66
                this::BIT_4_aIY_plus_n,    // 67
                this::BIT_5_aIY_plus_n,    // 68
                this::BIT_5_aIY_plus_n,    // 69
                this::BIT_5_aIY_plus_n,    // 6A
                this::BIT_5_aIY_plus_n,    // 6B
                this::BIT_5_aIY_plus_n,    // 6C
                this::BIT_5_aIY_plus_n,    // 6D
                this::BIT_5_aIY_plus_n,    // 6E
                this::BIT_5_aIY_plus_n,    // 6F
                this::BIT_6_aIY_plus_n,    // 70
                this::BIT_6_aIY_plus_n,    // 71
                this::BIT_6_aIY_plus_n,    // 72
                this::BIT_6_aIY_plus_n,    // 73
                this::BIT_6_aIY_plus_n,    // 74
                this::BIT_6_aIY_plus_n,    // 75
                this::BIT_6_aIY_plus_n,    // 76
                this::BIT_6_aIY_plus_n,    // 77
                this::BIT_7_aIY_plus_n,    // 78
                this::BIT_7_aIY_plus_n,    // 79
                this::BIT_7_aIY_plus_n,    // 7A
                this::BIT_7_aIY_plus_n,    // 7B
                this::BIT_7_aIY_plus_n,    // 7C
                this::BIT_7_aIY_plus_n,    // 7D
                this::BIT_7_aIY_plus_n,    // 7E
                this::BIT_7_aIY_plus_n,    // 7F
                this::RES_0_aIY_plus_n_and_load_B,    // 80
                this::RES_0_aIY_plus_n_and_load_C,    // 81
                this::RES_0_aIY_plus_n_and_load_D,    // 82
                this::RES_0_aIY_plus_n_and_load_E,    // 83
                this::RES_0_aIY_plus_n_and_load_H,    // 84
                this::RES_0_aIY_plus_n_and_load_L,    // 85
                this::RES_0_aIY_plus_n,    // 86
                this::RES_0_aIY_plus_n_and_load_A,    // 87
                this::RES_1_aIY_plus_n_and_load_B,    // 88
                this::RES_1_aIY_plus_n_and_load_C,    // 89
                this::RES_1_aIY_plus_n_and_load_D,    // 8A
                this::RES_1_aIY_plus_n_and_load_E,    // 8B
                this::RES_1_aIY_plus_n_and_load_H,    // 8C
                this::RES_1_aIY_plus_n_and_load_L,    // 8D
                this::RES_1_aIY_plus_n,    // 8E
                this::RES_1_aIY_plus_n_and_load_A,    // 8F
                this::RES_2_aIY_plus_n_and_load_B,    // 90
                this::RES_2_aIY_plus_n_and_load_C,    // 91
                this::RES_2_aIY_plus_n_and_load_D,    // 92
                this::RES_2_aIY_plus_n_and_load_E,    // 93
                this::RES_2_aIY_plus_n_and_load_H,    // 94
                this::RES_2_aIY_plus_n_and_load_L,    // 95
                this::RES_2_aIY_plus_n,    // 96
                this::RES_2_aIY_plus_n_and_load_A,    // 97
                this::RES_3_aIY_plus_n_and_load_B,    // 98
                this::RES_3_aIY_plus_n_and_load_C,    // 99
                this::RES_3_aIY_plus_n_and_load_D,    // 9A
                this::RES_3_aIY_plus_n_and_load_E,    // 9B
                this::RES_3_aIY_plus_n_and_load_H,    // 9C
                this::RES_3_aIY_plus_n_and_load_L,    // 9D
                this::RES_3_aIY_plus_n,    // 9E
                this::RES_3_aIY_plus_n_and_load_A,    // 9F
                this::RES_4_aIY_plus_n_and_load_B,    // A0
                this::RES_4_aIY_plus_n_and_load_C,    // A1
                this::RES_4_aIY_plus_n_and_load_D,    // A2
                this::RES_4_aIY_plus_n_and_load_E,    // A3
                this::RES_4_aIY_plus_n_and_load_H,    // A4
                this::RES_4_aIY_plus_n_and_load_L,    // A5
                this::RES_4_aIY_plus_n,    // A6
                this::RES_4_aIY_plus_n_and_load_A,    // A7
                this::RES_5_aIY_plus_n_and_load_B,    // A8
                this::RES_5_aIY_plus_n_and_load_C,    // A9
                this::RES_5_aIY_plus_n_and_load_D,    // AA
                this::RES_5_aIY_plus_n_and_load_E,    // AB
                this::RES_5_aIY_plus_n_and_load_H,    // AC
                this::RES_5_aIY_plus_n_and_load_L,    // AD
                this::RES_5_aIY_plus_n,    // AE
                this::RES_5_aIY_plus_n_and_load_A,    // AF
                this::RES_6_aIY_plus_n_and_load_B,    // B0
                this::RES_6_aIY_plus_n_and_load_C,    // B1
                this::RES_6_aIY_plus_n_and_load_D,    // B2
                this::RES_6_aIY_plus_n_and_load_E,    // B3
                this::RES_6_aIY_plus_n_and_load_H,    // B4
                this::RES_6_aIY_plus_n_and_load_L,    // B5
                this::RES_6_aIY_plus_n,    // B6
                this::RES_6_aIY_plus_n_and_load_A,    // B7
                this::RES_7_aIY_plus_n_and_load_B,    // B8
                this::RES_7_aIY_plus_n_and_load_C,    // B9
                this::RES_7_aIY_plus_n_and_load_D,    // BA
                this::RES_7_aIY_plus_n_and_load_E,    // BB
                this::RES_7_aIY_plus_n_and_load_H,    // BC
                this::RES_7_aIY_plus_n_and_load_L,    // BD
                this::RES_7_aIY_plus_n,    // BE
                this::RES_7_aIY_plus_n_and_load_A,    // BF
                this::SET_0_aIY_plus_n_and_load_B,    // C0
                this::SET_0_aIY_plus_n_and_load_C,    // C1
                this::SET_0_aIY_plus_n_and_load_D,    // C2
                this::SET_0_aIY_plus_n_and_load_E,    // C3
                this::SET_0_aIY_plus_n_and_load_H,    // C4
                this::SET_0_aIY_plus_n_and_load_L,    // C5
                this::SET_0_aIY_plus_n,    // C6
                this::SET_0_aIY_plus_n_and_load_A,    // C7
                this::SET_1_aIY_plus_n_and_load_B,    // C8
                this::SET_1_aIY_plus_n_and_load_C,    // C9
                this::SET_1_aIY_plus_n_and_load_D,    // CA
                this::SET_1_aIY_plus_n_and_load_E,    // CB
                this::SET_1_aIY_plus_n_and_load_H,    // CC
                this::SET_1_aIY_plus_n_and_load_L,    // CD
                this::SET_1_aIY_plus_n,    // CE
                this::SET_1_aIY_plus_n_and_load_A,    // CF
                this::SET_2_aIY_plus_n_and_load_B,    // D0
                this::SET_2_aIY_plus_n_and_load_C,    // D1
                this::SET_2_aIY_plus_n_and_load_D,    // D2
                this::SET_2_aIY_plus_n_and_load_E,    // D3
                this::SET_2_aIY_plus_n_and_load_H,    // D4
                this::SET_2_aIY_plus_n_and_load_L,    // D5
                this::SET_2_aIY_plus_n,    // D6
                this::SET_2_aIY_plus_n_and_load_A,    // D7
                this::SET_3_aIY_plus_n_and_load_B,    // D8
                this::SET_3_aIY_plus_n_and_load_C,    // D9
                this::SET_3_aIY_plus_n_and_load_D,    // DA
                this::SET_3_aIY_plus_n_and_load_E,    // DB
                this::SET_3_aIY_plus_n_and_load_H,    // DC
                this::SET_3_aIY_plus_n_and_load_L,    // DD
                this::SET_3_aIY_plus_n,    // DE
                this::SET_3_aIY_plus_n_and_load_A,    // DF
                this::SET_4_aIY_plus_n_and_load_B,    // E0
                this::SET_4_aIY_plus_n_and_load_C,    // E1
                this::SET_4_aIY_plus_n_and_load_D,    // E2
                this::SET_4_aIY_plus_n_and_load_E,    // E3
                this::SET_4_aIY_plus_n_and_load_H,    // E4
                this::SET_4_aIY_plus_n_and_load_L,    // E5
                this::SET_4_aIY_plus_n,    // E6
                this::SET_4_aIY_plus_n_and_load_A,    // E7
                this::SET_5_aIY_plus_n_and_load_B,    // E8
                this::SET_5_aIY_plus_n_and_load_C,    // E9
                this::SET_5_aIY_plus_n_and_load_D,    // EA
                this::SET_5_aIY_plus_n_and_load_E,    // EB
                this::SET_5_aIY_plus_n_and_load_H,    // EC
                this::SET_5_aIY_plus_n_and_load_L,    // ED
                this::SET_5_aIY_plus_n,    // EE
                this::SET_5_aIY_plus_n_and_load_A,    // EF
                this::SET_6_aIY_plus_n_and_load_B,    // F0
                this::SET_6_aIY_plus_n_and_load_C,    // F1
                this::SET_6_aIY_plus_n_and_load_D,    // F2
                this::SET_6_aIY_plus_n_and_load_E,    // F3
                this::SET_6_aIY_plus_n_and_load_H,    // F4
                this::SET_6_aIY_plus_n_and_load_L,    // F5
                this::SET_6_aIY_plus_n,    // F6
                this::SET_6_aIY_plus_n_and_load_A,    // F7
                this::SET_7_aIY_plus_n_and_load_B,    // F8
                this::SET_7_aIY_plus_n_and_load_C,    // F9
                this::SET_7_aIY_plus_n_and_load_D,    // FA
                this::SET_7_aIY_plus_n_and_load_E,    // FB
                this::SET_7_aIY_plus_n_and_load_H,    // FC
                this::SET_7_aIY_plus_n_and_load_L,    // FD
                this::SET_7_aIY_plus_n,    // FE
                this::SET_7_aIY_plus_n_and_load_A    // FF
        ).toArray(Function[]::new);
    }

//#endregion

//#region InstructionsTable.SingleByte

    private Supplier<Byte>[] SingleByte_InstructionExecutors;

    private void initialize_SingleByte_InstructionsTable() {
        SingleByte_InstructionExecutors = new Supplier[] {
                this::NOP,    // 00
                this::LD_BC_nn,    // 01
                this::LD_aBC_A,    // 02
                this::INC_BC,    // 03
                this::INC_B,    // 04
                this::DEC_B,    // 05
                this::LD_B_n,    // 06
                this::RLCA,    // 7
                this::EX_AF_AF,    // 08
                this::ADD_HL_BC,    // 09
                this::LD_A_aBC,    // 0A
                this::DEC_BC,    // 0B
                this::INC_C,    // 0C
                this::DEC_C,    // 0D
                this::LD_C_n,    // 0E
                this::RRCA,    // 0F
                this::DJNZ_d,    // 10
                this::LD_DE_nn,    // 11
                this::LD_aDE_A,    // 12
                this::INC_DE,    // 13
                this::INC_D,    // 14
                this::DEC_D,    // 15
                this::LD_D_n,    // 16
                this::RLA,    // 17
                this::JR_d,    // 18
                this::ADD_HL_DE,    // 19
                this::LD_A_aDE,    // 1A
                this::DEC_DE,    // 1B
                this::INC_E,    // 1C
                this::DEC_E,    // 1D
                this::LD_E_n,    // 1E
                this::RRA,    // 1F
                this::JR_NZ_d,    // 20
                this::LD_HL_nn,    // 21
                this::LD_aa_HL,    // 22
                this::INC_HL,    // 23
                this::INC_H,    // 24
                this::DEC_H,    // 25
                this::LD_H_n,    // 26
                this::DAA,    // 27
                this::JR_Z_d,    // 28
                this::ADD_HL_HL,    // 29
                this::LD_HL_aa,    // 2A
                this::DEC_HL,    // 2B
                this::INC_L,    // 2C
                this::DEC_L,    // 2D
                this::LD_L_n,    // 2E
                this::CPL,    // 2F
                this::JR_NC_d,    // 30
                this::LD_SP_nn,    // 31
                this::LD_aa_A,    // 32
                this::INC_SP,    // 33
                this::INC_aHL,    // 34
                this::DEC_aHL,    // 35
                this::LD_aHL_N,    // 36
                this::SCF,    // 37
                this::JR_C_d,    // 38
                this::ADD_HL_SP,    // 39
                this::LD_A_aa,    // 3A
                this::DEC_SP,    // 3B
                this::INC_A,    // 3C
                this::DEC_A,    // 3D
                this::LD_A_n,    // 3E
                this::CCF,    // 3F
                this::LD_B_B,    // 40
                this::LD_B_C,    // 41
                this::LD_B_D,    // 42
                this::LD_B_E,    // 43
                this::LD_B_H,    // 44
                this::LD_B_L,    // 45
                this::LD_B_aHL,    // 46
                this::LD_B_A,    // 47
                this::LD_C_B,    // 48
                this::LD_C_C,    // 49
                this::LD_C_D,    // 4A
                this::LD_C_E,    // 4B
                this::LD_C_H,    // 4C
                this::LD_C_L,    // 4D
                this::LD_C_aHL,    // 4E
                this::LD_C_A,    // 4F
                this::LD_D_B,    // 50
                this::LD_D_C,    // 51
                this::LD_D_D,    // 52
                this::LD_D_E,    // 53
                this::LD_D_H,    // 54
                this::LD_D_L,    // 55
                this::LD_D_aHL,    // 56
                this::LD_D_A,    // 57
                this::LD_E_B,    // 58
                this::LD_E_C,    // 59
                this::LD_E_D,    // 5A
                this::LD_E_E,    // 5B
                this::LD_E_H,    // 5C
                this::LD_E_L,    // 5D
                this::LD_E_aHL,    // 5E
                this::LD_E_A,    // 5F
                this::LD_H_B,    // 60
                this::LD_H_C,    // 61
                this::LD_H_D,    // 62
                this::LD_H_E,    // 63
                this::LD_H_H,    // 64
                this::LD_H_L,    // 65
                this::LD_H_aHL,    // 66
                this::LD_H_A,    // 67
                this::LD_L_B,    // 68
                this::LD_L_C,    // 69
                this::LD_L_D,    // 6A
                this::LD_L_E,    // 6B
                this::LD_L_H,    // 6C
                this::LD_L_L,    // 6D
                this::LD_L_aHL,    // 6E
                this::LD_L_A,    // 6F
                this::LD_aHL_B,    // 70
                this::LD_aHL_C,    // 71
                this::LD_aHL_D,    // 72
                this::LD_aHL_E,    // 73
                this::LD_aHL_H,    // 74
                this::LD_aHL_L,    // 75
                this::HALT,    // 76
                this::LD_aHL_A,    // 77
                this::LD_A_B,    // 78
                this::LD_A_C,    // 79
                this::LD_A_D,    // 7A
                this::LD_A_E,    // 7B
                this::LD_A_H,    // 7C
                this::LD_A_L,    // 7D
                this::LD_A_aHL,    // 7E
                this::LD_A_A,    // 7F
                this::ADD_A_B,    // 80
                this::ADD_A_C,    // 81
                this::ADD_A_D,    // 82
                this::ADD_A_E,    // 83
                this::ADD_A_H,    // 84
                this::ADD_A_L,    // 85
                this::ADD_A_aHL,    // 86
                this::ADD_A_A,    // 87
                this::ADC_A_B,    // 88
                this::ADC_A_C,    // 89
                this::ADC_A_D,    // 8A
                this::ADC_A_E,    // 8B
                this::ADC_A_H,    // 8C
                this::ADC_A_L,    // 8D
                this::ADC_A_aHL,    // 8E
                this::ADC_A_A,    // 8F
                this::SUB_B,    // 90
                this::SUB_C,    // 91
                this::SUB_D,    // 92
                this::SUB_E,    // 93
                this::SUB_H,    // 94
                this::SUB_L,    // 95
                this::SUB_aHL,    // 96
                this::SUB_A,    // 97
                this::SBC_A_B,    // 98
                this::SBC_A_C,    // 99
                this::SBC_A_D,    // 9A
                this::SBC_A_E,    // 9B
                this::SBC_A_H,    // 9C
                this::SBC_A_L,    // 9D
                this::SBC_A_aHL,    // 9E
                this::SBC_A_A,    // 9F
                this::AND_B,    // A0
                this::AND_C,    // A1
                this::AND_D,    // A2
                this::AND_E,    // A3
                this::AND_H,    // A4
                this::AND_L,    // A5
                this::AND_aHL,    // A6
                this::AND_A,    // A7
                this::XOR_B,    // A8
                this::XOR_C,    // A9
                this::XOR_D,    // AA
                this::XOR_E,    // AB
                this::XOR_H,    // AC
                this::XOR_L,    // AD
                this::XOR_aHL,    // AE
                this::XOR_A,    // AF
                this::OR_B,    // B0
                this::OR_C,    // B1
                this::OR_D,    // B2
                this::OR_E,    // B3
                this::OR_H,    // B4
                this::OR_L,    // B5
                this::OR_aHL,    // B6
                this::OR_A,    // B7
                this::CP_B,    // B8
                this::CP_C,    // B9
                this::CP_D,    // BA
                this::CP_E,    // BB
                this::CP_H,    // BC
                this::CP_L,    // BD
                this::CP_aHL,    // BE
                this::CP_A,    // BF
                this::RET_NZ,    // C0
                this::POP_BC,    // C1
                this::JP_NZ_nn,    // C2
                this::JP_nn,    // C3
                this::CALL_NZ_nn,    // C4
                this::PUSH_BC,    // C5
                this::ADD_A_n,    // C6
                this::RST_00,    // C7
                this::RET_Z,    // C8
                this::RET,    // C9
                this::JP_Z_nn,    // CA
                null,    // CB
                this::CALL_Z_nn,    // CC
                this::CALL_nn,    // CD
                this::ADC_A_n,    // CE
                this::RST_08,    // CF
                this::RET_NC,    // D0
                this::POP_DE,    // D1
                this::JP_NC_nn,    // D2
                this::OUT_n_A,    // D3
                this::CALL_NC_nn,    // D4
                this::PUSH_DE,    // D5
                this::SUB_n,    // D6
                this::RST_10,    // D7
                this::RET_C,    // D8
                this::EXX,    // D9
                this::JP_C_nn,    // DA
                this::IN_A_n,    // DB
                this::CALL_C_nn,    // DC
                null,    // DD
                this::SBC_A_n,    // DE
                this::RST_18,    // DF
                this::RET_PO,    // E0
                this::POP_HL,    // E1
                this::JP_PO_nn,    // E2
                this::EX_aSP_HL,    // E3
                this::CALL_PO_nn,    // E4
                this::PUSH_HL,    // E5
                this::AND_n,    // E6
                this::RST_20,    // E7
                this::RET_PE,    // E8
                this::JP_aHL,    // E9
                this::JP_PE_nn,    // EA
                this::EX_DE_HL,    // EB
                this::CALL_PE_nn,    // EC
                null,    // ED
                this::XOR_n,    // EE
                this::RST_28,    // EF
                this::RET_P,    // F0
                this::POP_AF,    // F1
                this::JP_P_nn,    // F2
                this::DI,    // F3
                this::CALL_P_nn,    // F4
                this::PUSH_AF,    // F5
                this::OR_n,    // F6
                this::RST_30,    // F7
                this::RET_M,    // F8
                this::LD_SP_HL,    // F9
                this::JP_M_nn,    // FA
                this::EI,    // FB
                this::CALL_M_nn,    // FC
                null,    // FD
                this::CP_n,    // FE
                this::RST_38    // FF
        };
    }

//#endregion

//#region ParityTable

    private static Bit[] parity;

    private static void generateParityTable() {
        parity = new Bit[256];

        for(var result = 0; result <= 255; result++) {
            var ones = 0;
            var temp = result;
            for (var i = 0; i <= 7; i++) {
                ones += (temp & 1);
                temp >>= 1;
            }
            parity[result & 0xff] = Bit.of((ones & 1) ^ 1);
        }
    }

//#endregion

//#region ADC HL,rr +

    /**
     * The ADC HL,BC instruction
     */
    byte ADC_HL_BC() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getHL();
        var valueToAdd = registers.getBC();
        var newValueInt = addAsInt(oldValue, valueToAdd) + registers.getCF().intValue();
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setHL(newValue);

        registers.setSF(Bit.of(newValue & 0x8000));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x8000) & (valueToAdd ^ newValue) & 0x8000));

        registers.setNF(Bit.OFF);
        setFlags3and5From(getHighByte(newValue));

        return 15;
    }

    /**
     * The SBC HL,BC instruction
     */
    byte SBC_HL_BC() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getHL();
        var valueToAdd = registers.getBC();
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setHL(newValue);

        registers.setSF(Bit.of(newValue & 0x8000));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x8000));

        registers.setNF(Bit.ON);
        setFlags3and5From(getHighByte(newValue));

        return 15;
    }

    /**
     * The ADC HL,DE instruction
     */
    byte ADC_HL_DE() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getHL();
        var valueToAdd = registers.getDE();
        var newValueInt = addAsInt(oldValue, valueToAdd) + registers.getCF().intValue();
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setHL(newValue);

        registers.setSF(Bit.of(newValue & 0x8000));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x8000) & (valueToAdd ^ newValue) & 0x8000));

        registers.setNF(Bit.OFF);
        setFlags3and5From(getHighByte(newValue));

        return 15;
    }

    /**
     * The SBC HL,DE instruction
     */
    byte SBC_HL_DE() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getHL();
        var valueToAdd = registers.getDE();
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setHL(newValue);

        registers.setSF(Bit.of(newValue & 0x8000));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x8000));

        registers.setNF(Bit.ON);
        setFlags3and5From(getHighByte(newValue));

        return 15;
    }

    /**
     * The ADC HL,HL instruction
     */
    byte ADC_HL_HL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getHL();
        var valueToAdd = registers.getHL();
        var newValueInt = addAsInt(oldValue, valueToAdd) + registers.getCF().intValue();
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setHL(newValue);

        registers.setSF(Bit.of(newValue & 0x8000));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x8000) & (valueToAdd ^ newValue) & 0x8000));

        registers.setNF(Bit.OFF);
        setFlags3and5From(getHighByte(newValue));

        return 15;
    }

    /**
     * The SBC HL,HL instruction
     */
    byte SBC_HL_HL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getHL();
        var valueToAdd = registers.getHL();
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setHL(newValue);

        registers.setSF(Bit.of(newValue & 0x8000));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x8000));

        registers.setNF(Bit.ON);
        setFlags3and5From(getHighByte(newValue));

        return 15;
    }

    /**
     * The ADC HL,SP instruction
     */
    byte ADC_HL_SP() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getHL();
        var valueToAdd = registers.getSP();
        var newValueInt = addAsInt(oldValue, valueToAdd) + registers.getCF().intValue();
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setHL(newValue);

        registers.setSF(Bit.of(newValue & 0x8000));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x8000) & (valueToAdd ^ newValue) & 0x8000));

        registers.setNF(Bit.OFF);
        setFlags3and5From(getHighByte(newValue));

        return 15;
    }

    /**
     * The SBC HL,SP instruction
     */
    byte SBC_HL_SP() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getHL();
        var valueToAdd = registers.getSP();
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setHL(newValue);

        registers.setSF(Bit.of(newValue & 0x8000));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x8000));

        registers.setNF(Bit.ON);
        setFlags3and5From(getHighByte(newValue));

        return 15;
    }

//#endregion

//#region ADC ADD A,r +

    /**
     * The ADC A,A instruction.
     */
    private byte ADC_A_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getA();
        var newValueInt = addAsInt(oldValue, valueToAdd) + registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The SBC A,A instruction.
     */
    private byte SBC_A_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getA();
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The ADD A,A instruction.
     */
    private byte ADD_A_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getA();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The SUB A instruction.
     */
    private byte SUB_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getA();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The CP A instruction.
     */
    private byte CP_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getA();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(valueToAdd);

        return 4;
    }

    /**
     * The CPI instruction.
     */
    private byte CPI() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = processorAgent.readFromMemory(registers.getHL());
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.incHL();
        registers.decBC();

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setPF(Bit.of((registers.getBC() != 0)));
        registers.setNF(Bit.ON);
        var valueForFlags3And5 = (byte) ((newValueInt - registers.getHF().intValue()) & 0xff);
        registers.setFlag3(getBit(valueForFlags3And5, 3));
        registers.setFlag5(getBit(valueForFlags3And5, 1));

        return 16;
    }

    /**
     * The CPD instruction.
     */
    private byte CPD() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = processorAgent.readFromMemory(registers.getHL());
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.decHL();
        registers.decBC();

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setPF(Bit.of((registers.getBC() != 0)));
        registers.setNF(Bit.ON);
        var valueForFlags3And5 = (byte) ((newValueInt - registers.getHF().intValue()) & 0xff);
        registers.setFlag3(getBit(valueForFlags3And5, 3));
        registers.setFlag5(getBit(valueForFlags3And5, 1));

        return 16;
    }

    /**
     * The CPIR instruction.
     */
    private byte CPIR() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = processorAgent.readFromMemory(registers.getHL());
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.incHL();
        registers.decBC();

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setPF(Bit.of((registers.getBC() != 0)));
        registers.setNF(Bit.ON);
        var valueForFlags3And5 = (byte) ((newValueInt - registers.getHF().intValue()) & 0xff);
        registers.setFlag3(getBit(valueForFlags3And5, 3));
        registers.setFlag5(getBit(valueForFlags3And5, 1));

        if (registers.getBC() != 0 && !registers.getZF().booleanValue()) {
            registers.setPC(sub(registers.getPC(), 2));
            return 21;
        }

        return 16;
    }

    /**
     * The CPDR instruction.
     */
    private byte CPDR() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = processorAgent.readFromMemory(registers.getHL());
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.decHL();
        registers.decBC();

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setPF(Bit.of((registers.getBC() != 0)));
        registers.setNF(Bit.ON);
        var valueForFlags3And5 = (byte) ((newValueInt - registers.getHF().intValue()) & 0xff);
        registers.setFlag3(getBit(valueForFlags3And5, 3));
        registers.setFlag5(getBit(valueForFlags3And5, 1));

        if (registers.getBC() != 0 && !registers.getZF().booleanValue()) {
            registers.setPC(sub(registers.getPC(), 2));
            return 21;
        }

        return 16;
    }

    /**
     * The ADC A,B instruction.
     */
    private byte ADC_A_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getB();
        var newValueInt = (oldValue & 0xff) + (valueToAdd & 0xff) + registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The SBC A,B instruction.
     */
    private byte SBC_A_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getB();
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The ADD A,B instruction.
     */
    private byte ADD_A_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getB();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The SUB B instruction.
     */
    private byte SUB_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getB();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The CP B instruction.
     */
    private byte CP_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getB();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(valueToAdd);

        return 4;
    }

    /**
     * The ADC A,C instruction.
     */
    private byte ADC_A_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getC();
        var newValueInt = (oldValue & 0xff) + (valueToAdd & 0xff) + registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The SBC A,C instruction.
     */
    private byte SBC_A_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getC();
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The ADD A,C instruction.
     */
    private byte ADD_A_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getC();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The SUB C instruction.
     */
    private byte SUB_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getC();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The CP C instruction.
     */
    private byte CP_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getC();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(valueToAdd);

        return 4;
    }

    /**
     * The ADC A,D instruction.
     */
    private byte ADC_A_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getD();
        var newValueInt = (oldValue & 0xff) + (valueToAdd & 0xff) + registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The SBC A,D instruction.
     */
    private byte SBC_A_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getD();
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The ADD A,D instruction.
     */
    private byte ADD_A_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getD();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The SUB D instruction.
     */
    private byte SUB_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getD();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The CP D instruction.
     */
    private byte CP_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getD();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(valueToAdd);

        return 4;
    }

    /**
     * The ADC A,E instruction.
     */
    private byte ADC_A_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getE();
        var newValueInt = (oldValue & 0xff) + (valueToAdd & 0xff) + registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The SBC A,E instruction.
     */
    private byte SBC_A_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getE();
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The ADD A,E instruction.
     */
    private byte ADD_A_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getE();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The SUB E instruction.
     */
    private byte SUB_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getE();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The CP E instruction.
     */
    private byte CP_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getE();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(valueToAdd);

        return 4;
    }

    /**
     * The ADC A,H instruction.
     */
    private byte ADC_A_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getH();
        var newValueInt = (oldValue & 0xff) + (valueToAdd & 0xff) + registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The SBC A,H instruction.
     */
    private byte SBC_A_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getH();
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The ADD A,H instruction.
     */
    private byte ADD_A_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getH();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The SUB H instruction.
     */
    private byte SUB_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getH();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The CP H instruction.
     */
    private byte CP_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getH();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(valueToAdd);

        return 4;
    }

    /**
     * The ADC A,L instruction.
     */
    private byte ADC_A_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getL();
        var newValueInt = (oldValue & 0xff) + (valueToAdd & 0xff) + registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The SBC A,L instruction.
     */
    private byte SBC_A_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getL();
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The ADD A,L instruction.
     */
    private byte ADD_A_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getL();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The SUB L instruction.
     */
    private byte SUB_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getL();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The CP L instruction.
     */
    private byte CP_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getL();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(valueToAdd);

        return 4;
    }

    /**
     * The ADC A,(HL) instruction.
     */
    private byte ADC_A_aHL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = registers.getHL();
        var valueToAdd = processorAgent.readFromMemory(address);
        var newValueInt = (oldValue & 0xff) + (valueToAdd & 0xff) + registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 7;
    }

    /**
     * The SBC A,(HL) instruction.
     */
    private byte SBC_A_aHL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = registers.getHL();
        var valueToAdd = processorAgent.readFromMemory(address);
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 7;
    }

    /**
     * The ADD A,(HL) instruction.
     */
    private byte ADD_A_aHL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = registers.getHL();
        var valueToAdd = processorAgent.readFromMemory(address);
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 7;
    }

    /**
     * The SUB (HL) instruction.
     */
    private byte SUB_aHL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = registers.getHL();
        var valueToAdd = processorAgent.readFromMemory(address);
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 7;
    }

    /**
     * The CP (HL) instruction.
     */
    private byte CP_aHL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = registers.getHL();
        var valueToAdd = processorAgent.readFromMemory(address);
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(valueToAdd);

        return 7;
    }

    /**
     * The ADC A,n instruction.
     */
    private byte ADC_A_n() {
        var valueToAdd = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValueInt = (oldValue & 0xff) + (valueToAdd & 0xff) + registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 7;
    }

    /**
     * The SBC A,n instruction.
     */
    private byte SBC_A_n() {
        var valueToAdd = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 7;
    }

    /**
     * The ADD A,n instruction.
     */
    private byte ADD_A_n() {
        var valueToAdd = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 7;
    }

    /**
     * The SUB n instruction.
     */
    private byte SUB_n() {
        var valueToAdd = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 7;
    }

    /**
     * The CP n instruction.
     */
    private byte CP_n() {
        var valueToAdd = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(valueToAdd);

        return 7;
    }

    /**
     * The ADC A,IXH instruction.
     */
    private byte ADC_A_IXH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIXH();
        var newValueInt = addAsInt(oldValue, valueToAdd) + registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The SBC A,IXH instruction.
     */
    private byte SBC_A_IXH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIXH();
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The ADD A,IXH instruction.
     */
    private byte ADD_A_IXH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIXH();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The SUB IXH instruction.
     */
    private byte SUB_IXH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIXH();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The CP IXH instruction.
     */
    private byte CP_IXH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIXH();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(valueToAdd);

        return 8;
    }

    /**
     * The ADC A,IXL instruction.
     */
    private byte ADC_A_IXL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIXL();
        var newValueInt = addAsInt(oldValue, valueToAdd) + registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The SBC A,IXL instruction.
     */
    private byte SBC_A_IXL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIXL();
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The ADD A,IXL instruction.
     */
    private byte ADD_A_IXL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIXL();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The SUB IXL instruction.
     */
    private byte SUB_IXL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIXL();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The CP IXL instruction.
     */
    private byte CP_IXL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIXL();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(valueToAdd);

        return 8;
    }

    /**
     * The ADC A,IYH instruction.
     */
    private byte ADC_A_IYH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIYH();
        var newValueInt = addAsInt(oldValue, valueToAdd) + registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The SBC A,IYH instruction.
     */
    private byte SBC_A_IYH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIYH();
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The ADD A,IYH instruction.
     */
    private byte ADD_A_IYH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIYH();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The SUB IYH instruction.
     */
    private byte SUB_IYH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIYH();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The CP IYH instruction.
     */
    private byte CP_IYH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIYH();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(valueToAdd);

        return 8;
    }

    /**
     * The ADC A,IYL instruction.
     */
    private byte ADC_A_IYL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIYL();
        var newValueInt = addAsInt(oldValue, valueToAdd) + registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The SBC A,IYL instruction.
     */
    private byte SBC_A_IYL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIYL();
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The ADD A,IYL instruction.
     */
    private byte ADD_A_IYL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIYL();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The SUB IYL instruction.
     */
    private byte SUB_IYL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIYL();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The CP IYL instruction.
     */
    private byte CP_IYL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var valueToAdd = registers.getIYL();
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(valueToAdd);

        return 8;
    }

    /**
     * The ADC A,(IX+n) instruction.
     */
    private byte ADC_A_aIX_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = add(registers.getIX(), offset);
        var valueToAdd = processorAgent.readFromMemory(address);
        var newValueInt = addAsInt(oldValue, valueToAdd) + registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 19;
    }

    /**
     * The SBC A,(IX+n) instruction.
     */
    private byte SBC_A_aIX_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = add(registers.getIX(), offset);
        var valueToAdd = processorAgent.readFromMemory(address);
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 19;
    }

    /**
     * The ADD A,(IX+n) instruction.
     */
    private byte ADD_A_aIX_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = add(registers.getIX(), offset);
        var valueToAdd = processorAgent.readFromMemory(address);
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 19;
    }

    /**
     * The SUB (IX+n) instruction.
     */
    private byte SUB_aIX_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = add(registers.getIX(), offset);
        var valueToAdd = processorAgent.readFromMemory(address);
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 19;
    }

    /**
     * The CP (IX+n) instruction.
     */
    private byte CP_aIX_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = add(registers.getIX(), offset);
        var valueToAdd = processorAgent.readFromMemory(address);
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(valueToAdd);

        return 19;
    }

    /**
     * The ADC A,(IY+n) instruction.
     */
    private byte ADC_A_aIY_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = add(registers.getIY(), offset);
        var valueToAdd = processorAgent.readFromMemory(address);
        var newValueInt = addAsInt(oldValue, valueToAdd) + registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 19;
    }

    /**
     * The SBC A,(IY+n) instruction.
     */
    private byte SBC_A_aIY_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = add(registers.getIY(), offset);
        var valueToAdd = processorAgent.readFromMemory(address);
        var newValueInt = subAsInt(oldValue, valueToAdd) - registers.getCF().intValue();
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 19;
    }

    /**
     * The ADD A,(IY+n) instruction.
     */
    private byte ADD_A_aIY_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = add(registers.getIY(), offset);
        var valueToAdd = processorAgent.readFromMemory(address);
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd ^ 0x80) & (valueToAdd ^ newValue) & 0x80));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 19;
    }

    /**
     * The SUB (IY+n) instruction.
     */
    private byte SUB_aIY_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = add(registers.getIY(), offset);
        var valueToAdd = processorAgent.readFromMemory(address);
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);
        registers.setA(newValue);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 19;
    }

    /**
     * The CP (IY+n) instruction.
     */
    private byte CP_aIY_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = add(registers.getIY(), offset);
        var valueToAdd = processorAgent.readFromMemory(address);
        var newValueInt = subAsInt(oldValue, valueToAdd);
        var newValue = (byte) (newValueInt & 0xFF);

        registers.setSF(Bit.of(newValue & 0x80));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x10));
        registers.setCF(Bit.of((newValueInt & 0x100)));
        registers.setPF(Bit.of((oldValue ^ valueToAdd) & (oldValue ^ newValue) & 0x80));
        registers.setNF(Bit.ON);
        setFlags3and5From(valueToAdd);

        return 19;
    }

//#endregion

//#region ADC HL,rr +

    /**
     * The ADD HL,BC instruction
     */
    byte ADD_HL_BC() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getHL();
        var valueToAdd = registers.getBC();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setHL(newValue);

        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(getHighByte(newValue));

        return 11;
    }

    /**
     * The ADD HL,DE instruction
     */
    byte ADD_HL_DE() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getHL();
        var valueToAdd = registers.getDE();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setHL(newValue);

        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(getHighByte(newValue));

        return 11;
    }

    /**
     * The ADD HL,HL instruction
     */
    byte ADD_HL_HL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getHL();
        var valueToAdd = registers.getHL();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setHL(newValue);

        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(getHighByte(newValue));

        return 11;
    }

    /**
     * The ADD HL,SP instruction
     */
    byte ADD_HL_SP() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getHL();
        var valueToAdd = registers.getSP();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setHL(newValue);

        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(getHighByte(newValue));

        return 11;
    }

    /**
     * The ADD IX,BC instruction
     */
    byte ADD_IX_BC() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getIX();
        var valueToAdd = registers.getBC();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setIX(newValue);

        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(getHighByte(newValue));

        return 15;
    }

    /**
     * The ADD IX,DE instruction
     */
    byte ADD_IX_DE() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getIX();
        var valueToAdd = registers.getDE();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setIX(newValue);

        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(getHighByte(newValue));

        return 15;
    }

    /**
     * The ADD IX,IX instruction
     */
    byte ADD_IX_IX() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getIX();
        var valueToAdd = registers.getIX();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setIX(newValue);

        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(getHighByte(newValue));

        return 15;
    }

    /**
     * The ADD IX,SP instruction
     */
    byte ADD_IX_SP() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getIX();
        var valueToAdd = registers.getSP();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setIX(newValue);

        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(getHighByte(newValue));

        return 15;
    }

    /**
     * The ADD IY,BC instruction
     */
    byte ADD_IY_BC() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getIY();
        var valueToAdd = registers.getBC();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setIY(newValue);

        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(getHighByte(newValue));

        return 15;
    }

    /**
     * The ADD IY,DE instruction
     */
    byte ADD_IY_DE() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getIY();
        var valueToAdd = registers.getDE();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setIY(newValue);

        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(getHighByte(newValue));

        return 15;
    }

    /**
     * The ADD IY,IY instruction
     */
    byte ADD_IY_IY() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getIY();
        var valueToAdd = registers.getIY();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setIY(newValue);

        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(getHighByte(newValue));

        return 15;
    }

    /**
     * The ADD IY,SP instruction
     */
    byte ADD_IY_SP() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getIY();
        var valueToAdd = registers.getSP();
        var newValueInt = addAsInt(oldValue, valueToAdd);
        var newValue = (short) (newValueInt & 0xFFFF);
        registers.setIY(newValue);

        registers.setHF(Bit.of((oldValue ^ newValue ^ valueToAdd) & 0x1000));
        registers.setCF(Bit.of((newValueInt & 0x10000)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(getHighByte(newValue));

        return 15;
    }

//#endregion

//#region AND r +

    /**
     * The AND A instruction.
     */
    private byte AND_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getA();
        var newValue = (byte) (oldValue & argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.ON);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The XOR A instruction.
     */
    private byte XOR_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getA();
        var newValue = (byte) (oldValue ^ argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The OR A instruction.
     */
    private byte OR_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getA();
        var newValue = (byte) (oldValue | argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The AND B instruction.
     */
    private byte AND_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getB();
        var newValue = (byte) (oldValue & argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.ON);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The XOR B instruction.
     */
    private byte XOR_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getB();
        var newValue = (byte) (oldValue ^ argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The OR B instruction.
     */
    private byte OR_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getB();
        var newValue = (byte) (oldValue | argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The AND C instruction.
     */
    private byte AND_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getC();
        var newValue = (byte) (oldValue & argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.ON);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The XOR C instruction.
     */
    private byte XOR_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getC();
        var newValue = (byte) (oldValue ^ argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The OR C instruction.
     */
    private byte OR_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getC();
        var newValue = (byte) (oldValue | argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The AND D instruction.
     */
    private byte AND_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getD();
        var newValue = (byte) (oldValue & argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.ON);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The XOR D instruction.
     */
    private byte XOR_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getD();
        var newValue = (byte) (oldValue ^ argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The OR D instruction.
     */
    private byte OR_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getD();
        var newValue = (byte) (oldValue | argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The AND E instruction.
     */
    private byte AND_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getE();
        var newValue = (byte) (oldValue & argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.ON);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The XOR E instruction.
     */
    private byte XOR_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getE();
        var newValue = (byte) (oldValue ^ argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The OR E instruction.
     */
    private byte OR_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getE();
        var newValue = (byte) (oldValue | argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The AND H instruction.
     */
    private byte AND_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getH();
        var newValue = (byte) (oldValue & argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.ON);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The XOR H instruction.
     */
    private byte XOR_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getH();
        var newValue = (byte) (oldValue ^ argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The OR H instruction.
     */
    private byte OR_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getH();
        var newValue = (byte) (oldValue | argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The AND L instruction.
     */
    private byte AND_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getL();
        var newValue = (byte) (oldValue & argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.ON);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The XOR L instruction.
     */
    private byte XOR_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getL();
        var newValue = (byte) (oldValue ^ argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The OR L instruction.
     */
    private byte OR_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getL();
        var newValue = (byte) (oldValue | argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The AND (HL) instruction.
     */
    private byte AND_aHL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = registers.getHL();
        var argument = processorAgent.readFromMemory(address);
        var newValue = (byte) (oldValue & argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.ON);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 7;
    }

    /**
     * The XOR (HL) instruction.
     */
    private byte XOR_aHL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = registers.getHL();
        var argument = processorAgent.readFromMemory(address);
        var newValue = (byte) (oldValue ^ argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 7;
    }

    /**
     * The OR (HL) instruction.
     */
    private byte OR_aHL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = registers.getHL();
        var argument = processorAgent.readFromMemory(address);
        var newValue = (byte) (oldValue | argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 7;
    }

    /**
     * The AND n instruction.
     */
    private byte AND_n() {
        var argument = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = (byte) (oldValue & argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.ON);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 7;
    }

    /**
     * The XOR n instruction.
     */
    private byte XOR_n() {
        var argument = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = (byte) (oldValue ^ argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 7;
    }

    /**
     * The OR n instruction.
     */
    private byte OR_n() {
        var argument = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = (byte) (oldValue | argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 7;
    }

    /**
     * The AND IXH instruction.
     */
    private byte AND_IXH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getIXH();
        var newValue = (byte) (oldValue & argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.ON);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The XOR IXH instruction.
     */
    private byte XOR_IXH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getIXH();
        var newValue = (byte) (oldValue ^ argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The OR IXH instruction.
     */
    private byte OR_IXH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getIXH();
        var newValue = (byte) (oldValue | argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The AND IXL instruction.
     */
    private byte AND_IXL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getIXL();
        var newValue = (byte) (oldValue & argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.ON);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The XOR IXL instruction.
     */
    private byte XOR_IXL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getIXL();
        var newValue = (byte) (oldValue ^ argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The OR IXL instruction.
     */
    private byte OR_IXL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getIXL();
        var newValue = (byte) (oldValue | argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The AND IYH instruction.
     */
    private byte AND_IYH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getIYH();
        var newValue = (byte) (oldValue & argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.ON);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The XOR IYH instruction.
     */
    private byte XOR_IYH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getIYH();
        var newValue = (byte) (oldValue ^ argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The OR IYH instruction.
     */
    private byte OR_IYH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getIYH();
        var newValue = (byte) (oldValue | argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The AND IYL instruction.
     */
    private byte AND_IYL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getIYL();
        var newValue = (byte) (oldValue & argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.ON);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The XOR IYL instruction.
     */
    private byte XOR_IYL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getIYL();
        var newValue = (byte) (oldValue ^ argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The OR IYL instruction.
     */
    private byte OR_IYL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var argument = registers.getIYL();
        var newValue = (byte) (oldValue | argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The AND (IX+n) instruction.
     */
    private byte AND_aIX_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = add(registers.getIX(), offset);
        var argument = processorAgent.readFromMemory(address);
        var newValue = (byte) (oldValue & argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.ON);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 19;
    }

    /**
     * The XOR (IX+n) instruction.
     */
    private byte XOR_aIX_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = add(registers.getIX(), offset);
        var argument = processorAgent.readFromMemory(address);
        var newValue = (byte) (oldValue ^ argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 19;
    }

    /**
     * The OR (IX+n) instruction.
     */
    private byte OR_aIX_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = add(registers.getIX(), offset);
        var argument = processorAgent.readFromMemory(address);
        var newValue = (byte) (oldValue | argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 19;
    }

    /**
     * The AND (IY+n) instruction.
     */
    private byte AND_aIY_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = add(registers.getIY(), offset);
        var argument = processorAgent.readFromMemory(address);
        var newValue = (byte) (oldValue & argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.ON);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 19;
    }

    /**
     * The XOR (IY+n) instruction.
     */
    private byte XOR_aIY_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = add(registers.getIY(), offset);
        var argument = processorAgent.readFromMemory(address);
        var newValue = (byte) (oldValue ^ argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 19;
    }

    /**
     * The OR (IY+n) instruction.
     */
    private byte OR_aIY_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var address = add(registers.getIY(), offset);
        var argument = processorAgent.readFromMemory(address);
        var newValue = (byte) (oldValue | argument);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newValue & 0xff]);
        registers.setNF(Bit.OFF);
        registers.setCF(Bit.OFF);
        setFlags3and5From(newValue);

        return 19;
    }

//#endregion

//#region BIT b,r +

    /**
     * The BIT 0,A instruction
     */
    byte BIT_0_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var bitValue = getBit(oldValue, 0);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 1,A instruction
     */
    byte BIT_1_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var bitValue = getBit(oldValue, 1);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 2,A instruction
     */
    byte BIT_2_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var bitValue = getBit(oldValue, 2);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 3,A instruction
     */
    byte BIT_3_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var bitValue = getBit(oldValue, 3);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 4,A instruction
     */
    byte BIT_4_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var bitValue = getBit(oldValue, 4);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 5,A instruction
     */
    byte BIT_5_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var bitValue = getBit(oldValue, 5);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 6,A instruction
     */
    byte BIT_6_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var bitValue = getBit(oldValue, 6);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 7,A instruction
     */
    byte BIT_7_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var bitValue = getBit(oldValue, 7);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(bitValue);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 0,B instruction
     */
    byte BIT_0_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var bitValue = getBit(oldValue, 0);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 1,B instruction
     */
    byte BIT_1_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var bitValue = getBit(oldValue, 1);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 2,B instruction
     */
    byte BIT_2_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var bitValue = getBit(oldValue, 2);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 3,B instruction
     */
    byte BIT_3_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var bitValue = getBit(oldValue, 3);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 4,B instruction
     */
    byte BIT_4_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var bitValue = getBit(oldValue, 4);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 5,B instruction
     */
    byte BIT_5_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var bitValue = getBit(oldValue, 5);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 6,B instruction
     */
    byte BIT_6_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var bitValue = getBit(oldValue, 6);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 7,B instruction
     */
    byte BIT_7_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var bitValue = getBit(oldValue, 7);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(bitValue);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 0,C instruction
     */
    byte BIT_0_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var bitValue = getBit(oldValue, 0);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 1,C instruction
     */
    byte BIT_1_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var bitValue = getBit(oldValue, 1);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 2,C instruction
     */
    byte BIT_2_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var bitValue = getBit(oldValue, 2);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 3,C instruction
     */
    byte BIT_3_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var bitValue = getBit(oldValue, 3);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 4,C instruction
     */
    byte BIT_4_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var bitValue = getBit(oldValue, 4);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 5,C instruction
     */
    byte BIT_5_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var bitValue = getBit(oldValue, 5);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 6,C instruction
     */
    byte BIT_6_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var bitValue = getBit(oldValue, 6);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 7,C instruction
     */
    byte BIT_7_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var bitValue = getBit(oldValue, 7);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(bitValue);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 0,D instruction
     */
    byte BIT_0_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var bitValue = getBit(oldValue, 0);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 1,D instruction
     */
    byte BIT_1_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var bitValue = getBit(oldValue, 1);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 2,D instruction
     */
    byte BIT_2_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var bitValue = getBit(oldValue, 2);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 3,D instruction
     */
    byte BIT_3_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var bitValue = getBit(oldValue, 3);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 4,D instruction
     */
    byte BIT_4_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var bitValue = getBit(oldValue, 4);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 5,D instruction
     */
    byte BIT_5_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var bitValue = getBit(oldValue, 5);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 6,D instruction
     */
    byte BIT_6_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var bitValue = getBit(oldValue, 6);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 7,D instruction
     */
    byte BIT_7_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var bitValue = getBit(oldValue, 7);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(bitValue);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 0,E instruction
     */
    byte BIT_0_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var bitValue = getBit(oldValue, 0);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 1,E instruction
     */
    byte BIT_1_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var bitValue = getBit(oldValue, 1);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 2,E instruction
     */
    byte BIT_2_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var bitValue = getBit(oldValue, 2);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 3,E instruction
     */
    byte BIT_3_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var bitValue = getBit(oldValue, 3);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 4,E instruction
     */
    byte BIT_4_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var bitValue = getBit(oldValue, 4);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 5,E instruction
     */
    byte BIT_5_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var bitValue = getBit(oldValue, 5);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 6,E instruction
     */
    byte BIT_6_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var bitValue = getBit(oldValue, 6);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 7,E instruction
     */
    byte BIT_7_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var bitValue = getBit(oldValue, 7);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(bitValue);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 0,H instruction
     */
    byte BIT_0_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var bitValue = getBit(oldValue, 0);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 1,H instruction
     */
    byte BIT_1_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var bitValue = getBit(oldValue, 1);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 2,H instruction
     */
    byte BIT_2_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var bitValue = getBit(oldValue, 2);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 3,H instruction
     */
    byte BIT_3_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var bitValue = getBit(oldValue, 3);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 4,H instruction
     */
    byte BIT_4_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var bitValue = getBit(oldValue, 4);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 5,H instruction
     */
    byte BIT_5_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var bitValue = getBit(oldValue, 5);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 6,H instruction
     */
    byte BIT_6_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var bitValue = getBit(oldValue, 6);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 7,H instruction
     */
    byte BIT_7_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var bitValue = getBit(oldValue, 7);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(bitValue);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 0,L instruction
     */
    byte BIT_0_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var bitValue = getBit(oldValue, 0);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 1,L instruction
     */
    byte BIT_1_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var bitValue = getBit(oldValue, 1);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 2,L instruction
     */
    byte BIT_2_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var bitValue = getBit(oldValue, 2);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 3,L instruction
     */
    byte BIT_3_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var bitValue = getBit(oldValue, 3);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 4,L instruction
     */
    byte BIT_4_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var bitValue = getBit(oldValue, 4);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 5,L instruction
     */
    byte BIT_5_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var bitValue = getBit(oldValue, 5);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 6,L instruction
     */
    byte BIT_6_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var bitValue = getBit(oldValue, 6);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 7,L instruction
     */
    byte BIT_7_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var bitValue = getBit(oldValue, 7);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(bitValue);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 8;
    }

    /**
     * The BIT 0,(HL) instruction
     */
    byte BIT_0_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 0);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 12;
    }

    /**
     * The BIT 1,(HL) instruction
     */
    byte BIT_1_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 1);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 12;
    }

    /**
     * The BIT 2,(HL) instruction
     */
    byte BIT_2_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 2);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 12;
    }

    /**
     * The BIT 3,(HL) instruction
     */
    byte BIT_3_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 3);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 12;
    }

    /**
     * The BIT 4,(HL) instruction
     */
    byte BIT_4_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 4);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 12;
    }

    /**
     * The BIT 5,(HL) instruction
     */
    byte BIT_5_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 5);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 12;
    }

    /**
     * The BIT 6,(HL) instruction
     */
    byte BIT_6_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 6);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 12;
    }

    /**
     * The BIT 7,(HL) instruction
     */
    byte BIT_7_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 7);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(bitValue);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 12;
    }

    /**
     * The BIT 0,(IX+n) instruction
     */
    byte BIT_0_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 0);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 20;
    }

    /**
     * The BIT 1,(IX+n) instruction
     */
    byte BIT_1_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 1);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 20;
    }

    /**
     * The BIT 2,(IX+n) instruction
     */
    byte BIT_2_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 2);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 20;
    }

    /**
     * The BIT 3,(IX+n) instruction
     */
    byte BIT_3_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 3);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 20;
    }

    /**
     * The BIT 4,(IX+n) instruction
     */
    byte BIT_4_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 4);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 20;
    }

    /**
     * The BIT 5,(IX+n) instruction
     */
    byte BIT_5_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 5);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 20;
    }

    /**
     * The BIT 6,(IX+n) instruction
     */
    byte BIT_6_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 6);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 20;
    }

    /**
     * The BIT 7,(IX+n) instruction
     */
    byte BIT_7_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 7);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(bitValue);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 20;
    }

    /**
     * The BIT 0,(IY+n) instruction
     */
    byte BIT_0_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 0);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 20;
    }

    /**
     * The BIT 1,(IY+n) instruction
     */
    byte BIT_1_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 1);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 20;
    }

    /**
     * The BIT 2,(IY+n) instruction
     */
    byte BIT_2_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 2);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 20;
    }

    /**
     * The BIT 3,(IY+n) instruction
     */
    byte BIT_3_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 3);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 20;
    }

    /**
     * The BIT 4,(IY+n) instruction
     */
    byte BIT_4_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 4);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 20;
    }

    /**
     * The BIT 5,(IY+n) instruction
     */
    byte BIT_5_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 5);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 20;
    }

    /**
     * The BIT 6,(IY+n) instruction
     */
    byte BIT_6_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 6);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(Bit.OFF);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 20;
    }

    /**
     * The BIT 7,(IY+n) instruction
     */
    byte BIT_7_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var bitValue = getBit(oldValue, 7);
        registers.setPF(bitValue.operatorNEG());
        registers.setZF(registers.getPF());
        registers.setSF(bitValue);
        registers.setHF(Bit.ON);
        registers.setNF(Bit.OFF);

        return 20;
    }

//#endregion

//#region CCF

    /**
     * The CCF instruction.
     */
    byte CCF() {
        fetchFinished(false, false, false, false);

        var oldCF = registers.getCF();
        registers.setNF(Bit.OFF);
        registers.setHF(oldCF);
        registers.setCF(oldCF.operatorNOT());
        setFlags3and5From(registers.getA());

        return 4;
    }

//#endregion

//#region CPL

    /**
     * The CPL instruction.
     */
    byte CPL() {
        fetchFinished(false, false, false, false);

        registers.setA((byte) (registers.getA() ^ 0xFF));

        registers.setHF(Bit.ON);
        registers.setNF(Bit.ON);
        setFlags3and5From(registers.getA());

        return 4;
    }

//#endregion

//#region DAA

    /**
     * The DAA instruction.
     */
    byte DAA() {
//        final byte CF_NF = 3;

        fetchFinished(false, false, false, false);

        // Algorithm borrowed from MAME:
        // https://github.com/mamedev/mame/blob/master/src/emu/cpu/z80/z80.c

        var oldValue = registers.getA();
        var newValue = oldValue;

        if (registers.getHF().booleanValue() || (oldValue & 0x0F) > 9)
            newValue = (byte) (newValue + (registers.getNF().booleanValue() ? -0x06 : 0x06)); // FA
        if (registers.getCF().booleanValue() || (oldValue & 0xff) > 0x99)
            newValue = (byte) (newValue + (registers.getNF().booleanValue() ? -0x60 : 0x60)); // A0

        registers.setCF(registers.getCF().operatorOR(Bit.of((oldValue & 0xff) > 0x99)));
        registers.setHF(Bit.of(((oldValue ^ newValue) & 0x10)));
        registers.setSF(Bit.of((newValue & 0x80)));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);
        setFlags3and5From(newValue);

        registers.setA(newValue);

        return 4;
    }

//#endregion

//#region DI

    /**
     * The DI instruction.
     */
    byte DI() {
        fetchFinished(false, false, false, /*isEiOrDi:*/ true);

        registers.setIFF1(Bit.OFF);
        registers.setIFF2(Bit.OFF);

        return 4;
    }

//#endregion

//#region DJNZ

    /**
     * The DJNZ d instruction.
     */
    byte DJNZ_d() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        registers.setB((byte) (oldValue - 1));

        if (oldValue == 1)
            return 8;

        registers.setPC(add(registers.getPC(), offset));
        return 13;
    }

//#endregion

//#region EI

    /**
     * The EI instruction.
     */
    byte EI() {
        fetchFinished(false, false, false, /*isEiOrDi:*/ true);

        registers.setIFF1(Bit.ON);
        registers.setIFF2(Bit.ON);

        return 4;
    }

//#endregion

//#region EX (SP),HL +

    /**
     * The EX (SP),HL instruction.
     */
    byte EX_aSP_HL() {
        fetchFinished(false, false, false, false);

        var sp = registers.getSP();

        var temp = readShortFromMemory(sp);
        writeShortToMemory(sp, registers.getHL());
        registers.setHL(temp);

        return 19;
    }

    /**
     * The EX (SP),IX instruction.
     */
    byte EX_aSP_IX() {
        fetchFinished(false, false, false, false);

        var sp = registers.getSP();

        var temp = readShortFromMemory(sp);
        writeShortToMemory(sp, registers.getIX());
        registers.setIX(temp);

        return 23;
    }

    /**
     * The EX (SP),IY instruction.
     */
    byte EX_aSP_IY() {
        fetchFinished(false, false, false, false);

        var sp = registers.getSP();

        var temp = readShortFromMemory(sp);
        writeShortToMemory(sp, registers.getIY());
        registers.setIY(temp);

        return 23;
    }

//#endregion

//#region EX AF,AF'

    /**
     * The EX AF,AF' instruction
     */
    byte EX_AF_AF() {
        fetchFinished(false, false, false, false);

        var temp = registers.getAF();
        registers.setAF(registers.getAlternate().getAF());
        registers.getAlternate().setAF(temp);

        return 4;
    }

//#endregion

//#region EX DE,HL

    /**
     * The EX DE,HL instruction
     */
    byte EX_DE_HL() {
        fetchFinished(false, false, false, false);

        var temp = registers.getDE();
        registers.setDE(registers.getHL());
        registers.setHL(temp);

        return 4;
    }

//#endregion

//#region EXX

    /**
     * The EXX instruction.
     */
    byte EXX() {
        fetchFinished(false, false, false, false);

        var tempBC = registers.getBC();
        var tempDE = registers.getDE();
        var tempHL = registers.getHL();

        registers.setBC(registers.getAlternate().getBC());
        registers.setDE(registers.getAlternate().getDE());
        registers.setHL(registers.getAlternate().getHL());

        registers.getAlternate().setBC(tempBC);
        registers.getAlternate().setDE(tempDE);
        registers.getAlternate().setHL(tempHL);

        return 4;
    }

//#endregion

//#region HALT

    /**
     * The HALT instruction.
     */
    byte HALT() {
        fetchFinished(false, /*isHalt: */true, false, false);

        return 4;
    }

//#endregion

//#region IM n

    /**
     * The IM 0 instruction.
     */
    private byte IM_0() {
        fetchFinished(false, false, false, false);

        processorAgent.setInterruptMode2((byte) 0);

        return 8;
    }

    /**
     * The IM 1 instruction.
     */
    private byte IM_1() {
        fetchFinished(false, false, false, false);

        processorAgent.setInterruptMode2((byte) 1);

        return 8;
    }

    /**
     * The IM 2 instruction.
     */
    private byte IM_2() {
        fetchFinished(false, false, false, false);

        processorAgent.setInterruptMode2((byte) 2);

        return 8;
    }

//#endregion

//#region IN A,(n)

    /**
     * The IN A,(n) instruction.
     */
    byte IN_A_n() {
        var portNumber = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        registers.setA(processorAgent.readFromPort(portNumber));

        return 11;
    }

//#endregion

//#region IN r,(C)

    /**
     * The IN A,(C) instruction.
     */
    byte IN_A_C() {
        fetchFinished(false, false, false, false);

        var value = processorAgent.readFromPort(registers.getC());
        registers.setA(value);

        registers.setSF(getBit(value, 7));
        registers.setZF(Bit.of((value == 0)));
        registers.setNF(Bit.OFF);
        registers.setHF(Bit.OFF);
        registers.setPF(parity[value & 0xff]);
        setFlags3and5From(value);

        return 12;
    }

    /**
     * The IN B,(C) instruction.
     */
    byte IN_B_C() {
        fetchFinished(false, false, false, false);

        var value = processorAgent.readFromPort(registers.getC());
        registers.setB(value);

        registers.setSF(getBit(value, 7));
        registers.setZF(Bit.of((value == 0)));
        registers.setNF(Bit.OFF);
        registers.setHF(Bit.OFF);
        registers.setPF(parity[value & 0xff]);
        setFlags3and5From(value);

        return 12;
    }

    /**
     * The IN C,(C) instruction.
     */
    byte IN_C_C() {
        fetchFinished(false, false, false, false);

        var value = processorAgent.readFromPort(registers.getC());
        registers.setC(value);

        registers.setSF(getBit(value, 7));
        registers.setZF(Bit.of((value == 0)));
        registers.setNF(Bit.OFF);
        registers.setHF(Bit.OFF);
        registers.setPF(parity[value & 0xff]);
        setFlags3and5From(value);

        return 12;
    }

    /**
     * The IN D,(C) instruction.
     */
    byte IN_D_C() {
        fetchFinished(false, false, false, false);

        var value = processorAgent.readFromPort(registers.getC());
        registers.setD(value);

        registers.setSF(getBit(value, 7));
        registers.setZF(Bit.of((value == 0)));
        registers.setNF(Bit.OFF);
        registers.setHF(Bit.OFF);
        registers.setPF(parity[value & 0xff]);
        setFlags3and5From(value);

        return 12;
    }

    /**
     * The IN E,(C) instruction.
     */
    byte IN_E_C() {
        fetchFinished(false, false, false, false);

        var value = processorAgent.readFromPort(registers.getC());
        registers.setE(value);

        registers.setSF(getBit(value, 7));
        registers.setZF(Bit.of((value == 0)));
        registers.setNF(Bit.OFF);
        registers.setHF(Bit.OFF);
        registers.setPF(parity[value & 0xff]);
        setFlags3and5From(value);

        return 12;
    }

    /**
     * The IN H,(C) instruction.
     */
    byte IN_H_C() {
        fetchFinished(false, false, false, false);

        var value = processorAgent.readFromPort(registers.getC());
        registers.setH(value);

        registers.setSF(getBit(value, 7));
        registers.setZF(Bit.of((value == 0)));
        registers.setNF(Bit.OFF);
        registers.setHF(Bit.OFF);
        registers.setPF(parity[value & 0xff]);
        setFlags3and5From(value);

        return 12;
    }

    /**
     * The IN L,(C) instruction.
     */
    byte IN_L_C() {
        fetchFinished(false, false, false, false);

        var value = processorAgent.readFromPort(registers.getC());
        registers.setL(value);

        registers.setSF(getBit(value, 7));
        registers.setZF(Bit.of((value == 0)));
        registers.setNF(Bit.OFF);
        registers.setHF(Bit.OFF);
        registers.setPF(parity[value & 0xff]);
        setFlags3and5From(value);

        return 12;
    }

    /**
     * The IN F,(C) instruction.
     */
    byte IN_F_C() {
        fetchFinished(false, false, false, false);

        var value = processorAgent.readFromPort(registers.getC());

        registers.setSF(getBit(value, 7));
        registers.setZF(Bit.of((value == 0)));
        registers.setNF(Bit.OFF);
        registers.setHF(Bit.OFF);
        registers.setPF(parity[value & 0xff]);
        setFlags3and5From(value);

        return 12;
    }

//#endregion

//#region INC r +

    /**
     * The INC A instruction.
     */
    private byte INC_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = inc(oldValue);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x00)));
        registers.setPF(Bit.of(((newValue & 0xff) == 0x80)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The DEC A instruction.
     */
    private byte DEC_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = dec(oldValue);
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x0F)));
        registers.setPF(Bit.of((newValue == 0x7F)));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The INC B instruction.
     */
    private byte INC_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = inc(oldValue);
        registers.setB(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x00)));
        registers.setPF(Bit.of(((newValue & 0xff) == 0x80)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The DEC B instruction.
     */
    private byte DEC_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = dec(oldValue);
        registers.setB(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x0F)));
        registers.setPF(Bit.of((newValue == 0x7F)));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The INC C instruction.
     */
    private byte INC_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = inc(oldValue);
        registers.setC(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x00)));
        registers.setPF(Bit.of(((newValue & 0xff) == 0x80)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The DEC C instruction.
     */
    private byte DEC_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = dec(oldValue);
        registers.setC(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x0F)));
        registers.setPF(Bit.of((newValue == 0x7F)));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The INC D instruction.
     */
    private byte INC_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = inc(oldValue);
        registers.setD(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x00)));
        registers.setPF(Bit.of(((newValue & 0xff) == 0x80)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The DEC D instruction.
     */
    private byte DEC_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = dec(oldValue);
        registers.setD(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x0F)));
        registers.setPF(Bit.of((newValue == 0x7F)));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The INC E instruction.
     */
    private byte INC_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = inc(oldValue);
        registers.setE(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x00)));
        registers.setPF(Bit.of(((newValue & 0xff) == 0x80)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The DEC E instruction.
     */
    private byte DEC_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = dec(oldValue);
        registers.setE(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x0F)));
        registers.setPF(Bit.of((newValue == 0x7F)));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The INC H instruction.
     */
    private byte INC_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = inc(oldValue);
        registers.setH(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x00)));
        registers.setPF(Bit.of(((newValue & 0xff) == 0x80)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The DEC H instruction.
     */
    private byte DEC_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = dec(oldValue);
        registers.setH(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x0F)));
        registers.setPF(Bit.of((newValue == 0x7F)));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The INC L instruction.
     */
    private byte INC_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = inc(oldValue);
        registers.setL(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x00)));
        registers.setPF(Bit.of(((newValue & 0xff) == 0x80)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The DEC L instruction.
     */
    private byte DEC_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = dec(oldValue);
        registers.setL(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x0F)));
        registers.setPF(Bit.of((newValue == 0x7F)));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 4;
    }

    /**
     * The INC IXH instruction.
     */
    private byte INC_IXH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getIXH();
        var newValue = inc(oldValue);
        registers.setIXH(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x00)));
        registers.setPF(Bit.of(((newValue & 0xff) == 0x80)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The DEC IXH instruction.
     */
    private byte DEC_IXH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getIXH();
        var newValue = dec(oldValue);
        registers.setIXH(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x0F)));
        registers.setPF(Bit.of((newValue == 0x7F)));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The INC IXL instruction.
     */
    private byte INC_IXL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getIXL();
        var newValue = inc(oldValue);
        registers.setIXL(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x00)));
        registers.setPF(Bit.of(((newValue & 0xff) == 0x80)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The DEC IXL instruction.
     */
    private byte DEC_IXL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getIXL();
        var newValue = dec(oldValue);
        registers.setIXL(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x0F)));
        registers.setPF(Bit.of((newValue == 0x7F)));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The INC IYH instruction.
     */
    private byte INC_IYH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getIYH();
        var newValue = inc(oldValue);
        registers.setIYH(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x00)));
        registers.setPF(Bit.of(((newValue & 0xff) == 0x80)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The DEC IYH instruction.
     */
    private byte DEC_IYH() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getIYH();
        var newValue = dec(oldValue);
        registers.setIYH(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x0F)));
        registers.setPF(Bit.of((newValue == 0x7F)));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The INC IYL instruction.
     */
    private byte INC_IYL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getIYL();
        var newValue = inc(oldValue);
        registers.setIYL(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x00)));
        registers.setPF(Bit.of(((newValue & 0xff) == 0x80)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The DEC IYL instruction.
     */
    private byte DEC_IYL() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getIYL();
        var newValue = dec(oldValue);
        registers.setIYL(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x0F)));
        registers.setPF(Bit.of((newValue == 0x7F)));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 8;
    }

    /**
     * The INC (HL) instruction.
     */
    private byte INC_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = inc(oldValue);
        processorAgent.writeToMemory(address, newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x00)));
        registers.setPF(Bit.of(((newValue & 0xff) == 0x80)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 11;
    }

    /**
     * The DEC (HL) instruction.
     */
    private byte DEC_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = dec(oldValue);
        processorAgent.writeToMemory(address, newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x0F)));
        registers.setPF(Bit.of((newValue == 0x7F)));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 11;
    }

    /**
     * The INC (IX+n) instruction.
     */
    private byte INC_aIX_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = inc(oldValue);
        processorAgent.writeToMemory(address, newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x00)));
        registers.setPF(Bit.of(((newValue & 0xff) == 0x80)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 23;
    }

    /**
     * The DEC (IX+n) instruction.
     */
    private byte DEC_aIX_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = dec(oldValue);
        processorAgent.writeToMemory(address, newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x0F)));
        registers.setPF(Bit.of((newValue == 0x7F)));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 23;
    }

    /**
     * The INC (IY+n) instruction.
     */
    private byte INC_aIY_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = inc(oldValue);
        processorAgent.writeToMemory(address, newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x00)));
        registers.setPF(Bit.of(((newValue & 0xff) == 0x80)));
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);

        return 23;
    }

    /**
     * The DEC (IY+n) instruction.
     */
    private byte DEC_aIY_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = dec(oldValue);
        processorAgent.writeToMemory(address, newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of(((newValue & 0x0F) == 0x0F)));
        registers.setPF(Bit.of((newValue == 0x7F)));
        registers.setNF(Bit.ON);
        setFlags3and5From(newValue);

        return 23;
    }

//#endregion

//#region INC rr

    /**
     * The INC BC instruction.
     */
    byte INC_BC() {
        fetchFinished(false, false, false, false);
        registers.incBC();
        return 6;
    }

    /**
     * The DEC BC instruction.
     */
    byte DEC_BC() {
        fetchFinished(false, false, false, false);
        registers.decBC();
        return 6;
    }

    /**
     * The INC DE instruction.
     */
    byte INC_DE() {
        fetchFinished(false, false, false, false);
        registers.incDE();
        return 6;
    }

    /**
     * The DEC DE instruction.
     */
    byte DEC_DE() {
        fetchFinished(false, false, false, false);
        registers.decDE();
        return 6;
    }

    /**
     * The INC HL instruction.
     */
    byte INC_HL() {
        fetchFinished(false, false, false, false);
        registers.incHL();
        return 6;
    }

    /**
     * The DEC HL instruction.
     */
    byte DEC_HL() {
        fetchFinished(false, false, false, false);
        registers.decHL();
        return 6;
    }

    /**
     * The INC SP instruction.
     */
    byte INC_SP() {
        fetchFinished(false, false, false, false);
        registers.incSP();
        return 6;
    }

    /**
     * The DEC SP instruction.
     */
    byte DEC_SP() {
        fetchFinished(false, false, false, false);
        registers.decSP();
        return 6;
    }

    /**
     * The INC IX instruction.
     */
    byte INC_IX() {
        fetchFinished(false, false, false, false);
        registers.incIX();
        return 10;
    }

    /**
     * The DEC IX instruction.
     */
    byte DEC_IX() {
        fetchFinished(false, false, false, false);
        registers.decIX();
        return 10;
    }

    /**
     * The INC IY instruction.
     */
    byte INC_IY() {
        fetchFinished(false, false, false, false);
        registers.incIY();
        return 10;
    }

    /**
     * The DEC IY instruction.
     */
    byte DEC_IY() {
        fetchFinished(false, false, false, false);
        registers.decIY();
        return 10;
    }

//#endregion

//#region INI +

    /**
     * The INI instruction.
     */
    byte INI() {
        fetchFinished(false, false, false, false);

        var portNumber = registers.getC();
        var address = registers.getHL();
        var value = processorAgent.readFromPort(portNumber);
        processorAgent.writeToMemory(address, value);

        registers.incHL();
        var counter = registers.getB();
        counter = (byte) (counter - 1);
        registers.setB(counter);
        registers.setZF(Bit.of((counter == 0)));
        registers.setNF(Bit.ON);
        registers.setSF(getBit(counter, 7));
        setFlags3and5From(counter);


        return 16;
    }

    /**
     * The IND instruction.
     */
    byte IND() {
        fetchFinished(false, false, false, false);

        var portNumber = registers.getC();
        var address = registers.getHL();
        var value = processorAgent.readFromPort(portNumber);
        processorAgent.writeToMemory(address, value);

        registers.decHL();
        var counter = registers.getB();
        counter = (byte) (counter - 1);
        registers.setB(counter);
        registers.setZF(Bit.of((counter == 0)));
        registers.setNF(Bit.ON);
        registers.setSF(getBit(counter, 7));
        setFlags3and5From(counter);


        return 16;
    }

    /**
     * The INIR instruction.
     */
    byte INIR() {
        fetchFinished(false, false, false, false);

        var portNumber = registers.getC();
        var address = registers.getHL();
        var value = processorAgent.readFromPort(portNumber);
        processorAgent.writeToMemory(address, value);

        registers.incHL();
        var counter = registers.getB();
        counter = (byte) (counter - 1);
        registers.setB(counter);
        registers.setZF(Bit.of((counter == 0)));
        registers.setNF(Bit.ON);
        registers.setSF(getBit(counter, 7));
        setFlags3and5From(counter);

        if (counter != 0) {
            registers.setPC((short) (registers.getPC() - 2));
            return 21;
        }

        return 16;
    }

    /**
     * The INDR instruction.
     */
    byte INDR() {
        fetchFinished(false, false, false, false);

        var portNumber = registers.getC();
        var address = registers.getHL();
        var value = processorAgent.readFromPort(portNumber);
        processorAgent.writeToMemory(address, value);

        registers.decHL();
        var counter = registers.getB();
        counter = (byte) (counter - 1);
        registers.setB(counter);
        registers.setZF(Bit.of((counter == 0)));
        registers.setNF(Bit.ON);
        registers.setSF(getBit(counter, 7));
        setFlags3and5From(counter);

        if (counter != 0) {
            registers.setPC((short) (registers.getPC() - 2));
            return 21;
        }

        return 16;
    }

    /**
     * The OUTI instruction.
     */
    byte OUTI() {
        fetchFinished(false, false, false, false);

        var portNumber = registers.getC();
        var address = registers.getHL();
        var value = processorAgent.readFromMemory(address);
        processorAgent.writeToPort(portNumber, value);

        registers.incHL();
        var counter = registers.getB();
        counter = (byte) (counter - 1);
        registers.setB(counter);
        registers.setZF(Bit.of((counter == 0)));
        registers.setNF(Bit.ON);
        registers.setSF(getBit(counter, 7));
        setFlags3and5From(counter);


        return 16;
    }

    /**
     * The OUTD instruction.
     */
    byte OUTD() {
        fetchFinished(false, false, false, false);

        var portNumber = registers.getC();
        var address = registers.getHL();
        var value = processorAgent.readFromMemory(address);
        processorAgent.writeToPort(portNumber, value);

        registers.decHL();
        var counter = registers.getB();
        counter = (byte) (counter - 1);
        registers.setB(counter);
        registers.setZF(Bit.of((counter == 0)));
        registers.setNF(Bit.ON);
        registers.setSF(getBit(counter, 7));
        setFlags3and5From(counter);


        return 16;
    }

    /**
     * The OTIR instruction.
     */
    byte OTIR() {
        fetchFinished(false, false, false, false);

        var portNumber = registers.getC();
        var address = registers.getHL();
        var value = processorAgent.readFromMemory(address);
        processorAgent.writeToPort(portNumber, value);

        registers.incHL();
        var counter = registers.getB();
        counter = (byte) (counter - 1);
        registers.setB(counter);
        registers.setZF(Bit.of((counter == 0)));
        registers.setNF(Bit.ON);
        registers.setSF(getBit(counter, 7));
        setFlags3and5From(counter);

        if (counter != 0) {
            registers.setPC((short) (registers.getPC() - 2));
            return 21;
        }

        return 16;
    }

    /**
     * The OTDR instruction.
     */
    byte OTDR() {
        fetchFinished(false, false, false, false);

        var portNumber = registers.getC();
        var address = registers.getHL();
        var value = processorAgent.readFromMemory(address);
        processorAgent.writeToPort(portNumber, value);

        registers.decHL();
        var counter = registers.getB();
        counter = (byte) (counter - 1);
        registers.setB(counter);
        registers.setZF(Bit.of((counter == 0)));
        registers.setNF(Bit.ON);
        registers.setSF(getBit(counter, 7));
        setFlags3and5From(counter);

        if (counter != 0) {
            registers.setPC((short) (registers.getPC() - 2));
            return 21;
        }

        return 16;
    }

//#endregion

//#region JP (HL) +

    /**
     * The JP (HL) instruction.
     */
    byte JP_aHL() {
        fetchFinished(false, false, false, false);

        registers.setPC(registers.getHL());

        return 4;
    }

    /**
     * The JP (IX) instruction.
     */
    byte JP_aIX() {
        fetchFinished(false, false, false, false);

        registers.setPC(registers.getIX());

        return 8;
    }

    /**
     * The JP (IY) instruction.
     */
    byte JP_aIY() {
        fetchFinished(false, false, false, false);

        registers.setPC(registers.getIY());

        return 8;
    }

//#endregion

//#region JR cc 1

    /**
     * The JR C,d instruction.
     */
    byte JR_C_d() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        if (!registers.getCF().booleanValue())
            return 7;

        registers.setPC(add(registers.getPC(), offset));
        return 12;
    }

    /**
     * The JR NC,d instruction.
     */
    byte JR_NC_d() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        if (registers.getCF().booleanValue())
            return 7;

        registers.setPC(add(registers.getPC(), offset));
        return 12;
    }

    /**
     * The JR Z,d instruction.
     */
    byte JR_Z_d() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        if (!registers.getZF().booleanValue())
            return 7;

        registers.setPC(add(registers.getPC(), offset));
        return 12;
    }

    /**
     * The JR NZ,d instruction.
     */
    byte JR_NZ_d() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        if (registers.getZF().booleanValue())
            return 7;

        registers.setPC(add(registers.getPC(), offset));
        return 12;
    }

//#endregion

//#region JR

    /**
     * The JR d instruction.
     */
    byte JR_d() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        registers.setPC(add(registers.getPC(), offset));

        return 12;
    }

//#endregion

//#region LD (aa),A

    /**
     * The LD (nn),A instruction.
     */
    private byte LD_aa_A() {
        var address = fetchWord();
        fetchFinished(false, false, false, false);

        processorAgent.writeToMemory(address, registers.getA());

        return 13;
    }

//#endregion

//#region LD (aa),rr

    /**
     * The LD (aa),HL instruction.
     */
    byte LD_aa_HL() {
        var address = fetchWord();
        fetchFinished(false, false, false, false);

        writeShortToMemory(address, registers.getHL());

        return 16;
    }

    /**
     * The LD (aa),DE instruction.
     */
    byte LD_aa_DE() {
        var address = fetchWord();
        fetchFinished(false, false, false, false);

        writeShortToMemory(address, registers.getDE());

        return 20;
    }

    /**
     * The LD (aa),BC instruction.
     */
    byte LD_aa_BC() {
        var address = fetchWord();
        fetchFinished(false, false, false, false);

        writeShortToMemory(address, registers.getBC());

        return 20;
    }

    /**
     * The LD (aa),SP instruction.
     */
    byte LD_aa_SP() {
        var address = fetchWord();
        fetchFinished(false, false, false, false);

        writeShortToMemory(address, registers.getSP());

        return 20;
    }

    /**
     * The LD (aa),IX instruction.
     */
    byte LD_aa_IX() {
        var address = fetchWord();
        fetchFinished(false, false, false, false);

        writeShortToMemory(address, registers.getIX());

        return 20;
    }

    /**
     * The LD (aa),IY instruction.
     */
    byte LD_aa_IY() {
        var address = fetchWord();
        fetchFinished(false, false, false, false);

        writeShortToMemory(address, registers.getIY());

        return 20;
    }

//#endregion

//#region LD (HL),n +

    /**
     * The LD (HL),n instruction.
     */
    private byte LD_aHL_N() {
        var newValue = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        processorAgent.writeToMemory(address, newValue);

        return 10;
    }

    /**
     * The LD (IX+n),n instruction.
     */
    private byte LD_aIX_plus_n_N() {
        var offset = processorAgent.fetchNextOpcode();
        var newValue = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        processorAgent.writeToMemory(address, newValue);

        return 19;
    }

    /**
     * The LD (IY+n),n instruction.
     */
    private byte LD_aIY_plus_n_N() {
        var offset = processorAgent.fetchNextOpcode();
        var newValue = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        processorAgent.writeToMemory(address, newValue);

        return 19;
    }

//#endregion

//#region LD A,(aa)

    /**
     * The LD A,(nn) instruction.
     */
    private byte LD_A_aa() {
        var address = fetchWord();
        fetchFinished(false, false, false, false);

        registers.setA(processorAgent.readFromMemory(address));

        return 13;
    }

//#endregion

//#region LD A,I +

    /**
     * The LD A,I instruction.
     */
    byte LD_A_I() {
        fetchFinished(false, false, false, false);

        var value = registers.getI();
        registers.setA(value);

        registers.setSF(getBit(value, 7));
        registers.setZF(Bit.of((value == 0)));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        registers.setPF(registers.getIFF2());
        setFlags3and5From(value);

        return 9;
    }

    /**
     * The LD A,R instruction.
     */
    byte LD_A_R() {
        fetchFinished(false, false, false, false);

        var value = registers.getR();
        registers.setA(value);

        registers.setSF(getBit(value, 7));
        registers.setZF(Bit.of((value == 0)));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        registers.setPF(registers.getIFF2());
        setFlags3and5From(value);

        return 9;
    }

//#endregion

//#region LD I,A +

    /**
     * The LD I,A instruction.
     */
    byte LD_I_A() {
        fetchFinished(false, false, false, false);

        registers.setI(registers.getA());

        return 9;
    }

    /**
     * The LD R,A instruction.
     */
    byte LD_R_A() {
        fetchFinished(false, false, false, false);

        registers.setR(registers.getA());

        return 9;
    }

//#endregion

//#region LD r,(rr) +

    /**
     * The LD A,(BC) instruction.
     */
    byte LD_A_aBC() {
        fetchFinished(false, false, false, false);

        var address = registers.getBC();
        var oldValue = processorAgent.readFromMemory(address);
        registers.setA(oldValue);

        return 7;
    }

    /**
     * The LD (BC),A instruction.
     */
    byte LD_aBC_A() {
        fetchFinished(false, false, false, false);

        var newValue = registers.getA();
        var address = registers.getBC();
        processorAgent.writeToMemory(address, newValue);

        return 7;
    }

    /**
     * The LD A,(DE) instruction.
     */
    byte LD_A_aDE() {
        fetchFinished(false, false, false, false);

        var address = registers.getDE();
        var oldValue = processorAgent.readFromMemory(address);
        registers.setA(oldValue);

        return 7;
    }

    /**
     * The LD (DE),A instruction.
     */
    byte LD_aDE_A() {
        fetchFinished(false, false, false, false);

        var newValue = registers.getA();
        var address = registers.getDE();
        processorAgent.writeToMemory(address, newValue);

        return 7;
    }

    /**
     * The LD A,(HL) instruction.
     */
    byte LD_A_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        registers.setA(oldValue);

        return 7;
    }

    /**
     * The LD (HL),A instruction.
     */
    byte LD_aHL_A() {
        fetchFinished(false, false, false, false);

        var newValue = registers.getA();
        var address = registers.getHL();
        processorAgent.writeToMemory(address, newValue);

        return 7;
    }

    /**
     * The LD B,(HL) instruction.
     */
    byte LD_B_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        registers.setB(oldValue);

        return 7;
    }

    /**
     * The LD (HL),B instruction.
     */
    byte LD_aHL_B() {
        fetchFinished(false, false, false, false);

        var newValue = registers.getB();
        var address = registers.getHL();
        processorAgent.writeToMemory(address, newValue);

        return 7;
    }

    /**
     * The LD C,(HL) instruction.
     */
    byte LD_C_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        registers.setC(oldValue);

        return 7;
    }

    /**
     * The LD (HL),C instruction.
     */
    byte LD_aHL_C() {
        fetchFinished(false, false, false, false);

        var newValue = registers.getC();
        var address = registers.getHL();
        processorAgent.writeToMemory(address, newValue);

        return 7;
    }

    /**
     * The LD D,(HL) instruction.
     */
    byte LD_D_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        registers.setD(oldValue);

        return 7;
    }

    /**
     * The LD (HL),D instruction.
     */
    byte LD_aHL_D() {
        fetchFinished(false, false, false, false);

        var newValue = registers.getD();
        var address = registers.getHL();
        processorAgent.writeToMemory(address, newValue);

        return 7;
    }

    /**
     * The LD E,(HL) instruction.
     */
    byte LD_E_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        registers.setE(oldValue);

        return 7;
    }

    /**
     * The LD (HL),E instruction.
     */
    byte LD_aHL_E() {
        fetchFinished(false, false, false, false);

        var newValue = registers.getE();
        var address = registers.getHL();
        processorAgent.writeToMemory(address, newValue);

        return 7;
    }

    /**
     * The LD H,(HL) instruction.
     */
    byte LD_H_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        registers.setH(oldValue);

        return 7;
    }

    /**
     * The LD (HL),H instruction.
     */
    byte LD_aHL_H() {
        fetchFinished(false, false, false, false);

        var newValue = registers.getH();
        var address = registers.getHL();
        processorAgent.writeToMemory(address, newValue);

        return 7;
    }

    /**
     * The LD L,(HL) instruction.
     */
    byte LD_L_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        registers.setL(oldValue);

        return 7;
    }

    /**
     * The LD (HL),L instruction.
     */
    byte LD_aHL_L() {
        fetchFinished(false, false, false, false);

        var newValue = registers.getL();
        var address = registers.getHL();
        processorAgent.writeToMemory(address, newValue);

        return 7;
    }

    /**
     * The LD A,(IX+n) instruction.
     */
    byte LD_A_aIX_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        registers.setA(oldValue);

        return 19;
    }

    /**
     * The LD (IX+n),A instruction.
     */
    byte LD_aIX_plus_n_A() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var newValue = registers.getA();
        var address = add(registers.getIX(), offset);
        processorAgent.writeToMemory(address, newValue);

        return 19;
    }

    /**
     * The LD B,(IX+n) instruction.
     */
    byte LD_B_aIX_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        registers.setB(oldValue);

        return 19;
    }

    /**
     * The LD (IX+n),B instruction.
     */
    byte LD_aIX_plus_n_B() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var newValue = registers.getB();
        var address = add(registers.getIX(), offset);
        processorAgent.writeToMemory(address, newValue);

        return 19;
    }

    /**
     * The LD C,(IX+n) instruction.
     */
    byte LD_C_aIX_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        registers.setC(oldValue);

        return 19;
    }

    /**
     * The LD (IX+n),C instruction.
     */
    byte LD_aIX_plus_n_C() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var newValue = registers.getC();
        var address = add(registers.getIX(), offset);
        processorAgent.writeToMemory(address, newValue);

        return 19;
    }

    /**
     * The LD D,(IX+n) instruction.
     */
    byte LD_D_aIX_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        registers.setD(oldValue);

        return 19;
    }

    /**
     * The LD (IX+n),D instruction.
     */
    byte LD_aIX_plus_n_D() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var newValue = registers.getD();
        var address = add(registers.getIX(), offset);
        processorAgent.writeToMemory(address, newValue);

        return 19;
    }

    /**
     * The LD E,(IX+n) instruction.
     */
    byte LD_E_aIX_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        registers.setE(oldValue);

        return 19;
    }

    /**
     * The LD (IX+n),E instruction.
     */
    byte LD_aIX_plus_n_E() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var newValue = registers.getE();
        var address = add(registers.getIX(), offset);
        processorAgent.writeToMemory(address, newValue);

        return 19;
    }

    /**
     * The LD H,(IX+n) instruction.
     */
    byte LD_H_aIX_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        registers.setH(oldValue);

        return 19;
    }

    /**
     * The LD (IX+n),H instruction.
     */
    byte LD_aIX_plus_n_H() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var newValue = registers.getH();
        var address = add(registers.getIX(), offset);
        processorAgent.writeToMemory(address, newValue);

        return 19;
    }

    /**
     * The LD L,(IX+n) instruction.
     */
    byte LD_L_aIX_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        registers.setL(oldValue);

        return 19;
    }

    /**
     * The LD (IX+n),L instruction.
     */
    byte LD_aIX_plus_n_L() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var newValue = registers.getL();
        var address = add(registers.getIX(), offset);
        processorAgent.writeToMemory(address, newValue);

        return 19;
    }

    /**
     * The LD A,(IY+n) instruction.
     */
    byte LD_A_aIY_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        registers.setA(oldValue);

        return 19;
    }

    /**
     * The LD (IY+n),A instruction.
     */
    byte LD_aIY_plus_n_A() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var newValue = registers.getA();
        var address = add(registers.getIY(), offset);
        processorAgent.writeToMemory(address, newValue);

        return 19;
    }

    /**
     * The LD B,(IY+n) instruction.
     */
    byte LD_B_aIY_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        registers.setB(oldValue);

        return 19;
    }

    /**
     * The LD (IY+n),B instruction.
     */
    byte LD_aIY_plus_n_B() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var newValue = registers.getB();
        var address = add(registers.getIY(), offset);
        processorAgent.writeToMemory(address, newValue);

        return 19;
    }

    /**
     * The LD C,(IY+n) instruction.
     */
    byte LD_C_aIY_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        registers.setC(oldValue);

        return 19;
    }

    /**
     * The LD (IY+n),C instruction.
     */
    byte LD_aIY_plus_n_C() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var newValue = registers.getC();
        var address = add(registers.getIY(), offset);
        processorAgent.writeToMemory(address, newValue);

        return 19;
    }

    /**
     * The LD D,(IY+n) instruction.
     */
    byte LD_D_aIY_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        registers.setD(oldValue);

        return 19;
    }

    /**
     * The LD (IY+n),D instruction.
     */
    byte LD_aIY_plus_n_D() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var newValue = registers.getD();
        var address = add(registers.getIY(), offset);
        processorAgent.writeToMemory(address, newValue);

        return 19;
    }

    /**
     * The LD E,(IY+n) instruction.
     */
    byte LD_E_aIY_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        registers.setE(oldValue);

        return 19;
    }

    /**
     * The LD (IY+n),E instruction.
     */
    byte LD_aIY_plus_n_E() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var newValue = registers.getE();
        var address = add(registers.getIY(), offset);
        processorAgent.writeToMemory(address, newValue);

        return 19;
    }

    /**
     * The LD H,(IY+n) instruction.
     */
    byte LD_H_aIY_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        registers.setH(oldValue);

        return 19;
    }

    /**
     * The LD (IY+n),H instruction.
     */
    byte LD_aIY_plus_n_H() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var newValue = registers.getH();
        var address = add(registers.getIY(), offset);
        processorAgent.writeToMemory(address, newValue);

        return 19;
    }

    /**
     * The LD L,(IY+n) instruction.
     */
    byte LD_L_aIY_plus_n() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        registers.setL(oldValue);

        return 19;
    }

    /**
     * The LD (IY+n),L instruction.
     */
    byte LD_aIY_plus_n_L() {
        var offset = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        var newValue = registers.getL();
        var address = add(registers.getIY(), offset);
        processorAgent.writeToMemory(address, newValue);

        return 19;
    }

//#endregion

//#region LD r,n

    /**
     * The LD A,n instruction.
     */
    byte LD_A_n() {
        var value = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);
        registers.setA(value);
        return 7;
    }

    /**
     * The LD B,n instruction.
     */
    byte LD_B_n() {
        var value = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);
        registers.setB(value);
        return 7;
    }

    /**
     * The LD C,n instruction.
     */
    byte LD_C_n() {
        var value = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);
        registers.setC(value);
        return 7;
    }

    /**
     * The LD D,n instruction.
     */
    byte LD_D_n() {
        var value = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);
        registers.setD(value);
        return 7;
    }

    /**
     * The LD E,n instruction.
     */
    byte LD_E_n() {
        var value = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);
        registers.setE(value);
        return 7;
    }

    /**
     * The LD H,n instruction.
     */
    byte LD_H_n() {
        var value = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);
        registers.setH(value);
        return 7;
    }

    /**
     * The LD L,n instruction.
     */
    byte LD_L_n() {
        var value = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);
        registers.setL(value);
        return 7;
    }

    /**
     * The LD IXH,n instruction.
     */
    byte LD_IXH_n() {
        var value = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);
        registers.setIXH(value);
        return 11;
    }

    /**
     * The LD IXL,n instruction.
     */
    byte LD_IXL_n() {
        var value = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);
        registers.setIXL(value);
        return 11;
    }

    /**
     * The LD IYH,n instruction.
     */
    byte LD_IYH_n() {
        var value = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);
        registers.setIYH(value);
        return 11;
    }

    /**
     * The LD IYL,n instruction.
     */
    byte LD_IYL_n() {
        var value = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);
        registers.setIYL(value);
        return 11;
    }

//#endregion

//#region LD rr,(aa)

    /**
     * The LD HL,(aa) instruction.
     */
    byte LD_HL_aa() {
        var address = fetchWord();
        fetchFinished(false, false, false, false);

        registers.setHL(readShortFromMemory(address));

        return 16;
    }

    /**
     * The LD DE,(aa) instruction.
     */
    byte LD_DE_aa() {
        var address = fetchWord();
        fetchFinished(false, false, false, false);

        registers.setDE(readShortFromMemory(address));

        return 20;
    }

    /**
     * The LD BC,(aa) instruction.
     */
    byte LD_BC_aa() {
        var address = fetchWord();
        fetchFinished(false, false, false, false);

        registers.setBC(readShortFromMemory(address));

        return 20;
    }

    /**
     * The LD SP,(aa) instruction.
     */
    byte LD_SP_aa() {
        var address = fetchWord();
        fetchFinished(false, false, false, false);

        registers.setSP(readShortFromMemory(address));

        return 20;
    }

    /**
     * The LD IX,(aa) instruction.
     */
    byte LD_IX_aa() {
        var address = fetchWord();
        fetchFinished(false, false, false, false);

        registers.setIX(readShortFromMemory(address));

        return 20;
    }

    /**
     * The LD IY,(aa) instruction.
     */
    byte LD_IY_aa() {
        var address = fetchWord();
        fetchFinished(false, false, false, false);

        registers.setIY(readShortFromMemory(address));

        return 20;
    }

//#endregion

//#region LD rr,nn

    /**
     * The LD BC,nn instruction.
     */
    byte LD_BC_nn() {
        var value = fetchWord();
        fetchFinished(false, false, false, false);
        registers.setBC(value);
        return 10;
    }

    /**
     * The LD DE,nn instruction.
     */
    byte LD_DE_nn() {
        var value = fetchWord();
        fetchFinished(false, false, false, false);
        registers.setDE(value);
        return 10;
    }

    /**
     * The LD HL,nn instruction.
     */
    byte LD_HL_nn() {
        var value = fetchWord();
        fetchFinished(false, false, false, false);
        registers.setHL(value);
        return 10;
    }

    /**
     * The LD SP,nn instruction.
     */
    byte LD_SP_nn() {
        var value = fetchWord();
        fetchFinished(false, false, /*isLdSp:*/ true, false);
        registers.setSP(value);
        return 10;
    }

    /**
     * The LD IX,nn instruction.
     */
    byte LD_IX_nn() {
        var value = fetchWord();
        fetchFinished(false, false, false, false);
        registers.setIX(value);
        return 14;
    }

    /**
     * The LD IY,nn instruction.
     */
    byte LD_IY_nn() {
        var value = fetchWord();
        fetchFinished(false, false, false, false);
        registers.setIY(value);
        return 14;
    }

//#endregion

//#region LD r,r

    /**
     * The LD A,A instruction.
     */
    byte LD_A_A() {
        fetchFinished(false, false, false, false);

        return 4;
    }

    /**
     * The LD B,A instruction.
     */
    byte LD_B_A() {
        fetchFinished(false, false, false, false);

        registers.setB(registers.getA());

        return 4;
    }

    /**
     * The LD C,A instruction.
     */
    byte LD_C_A() {
        fetchFinished(false, false, false, false);

        registers.setC(registers.getA());

        return 4;
    }

    /**
     * The LD D,A instruction.
     */
    byte LD_D_A() {
        fetchFinished(false, false, false, false);

        registers.setD(registers.getA());

        return 4;
    }

    /**
     * The LD E,A instruction.
     */
    byte LD_E_A() {
        fetchFinished(false, false, false, false);

        registers.setE(registers.getA());

        return 4;
    }

    /**
     * The LD H,A instruction.
     */
    byte LD_H_A() {
        fetchFinished(false, false, false, false);

        registers.setH(registers.getA());

        return 4;
    }

    /**
     * The LD L,A instruction.
     */
    byte LD_L_A() {
        fetchFinished(false, false, false, false);

        registers.setL(registers.getA());

        return 4;
    }

    /**
     * The LD IXH,A instruction.
     */
    byte LD_IXH_A() {
        fetchFinished(false, false, false, false);

        registers.setIXH(registers.getA());

        return 8;
    }

    /**
     * The LD IXL,A instruction.
     */
    byte LD_IXL_A() {
        fetchFinished(false, false, false, false);

        registers.setIXL(registers.getA());

        return 8;
    }

    /**
     * The LD IYH,A instruction.
     */
    byte LD_IYH_A() {
        fetchFinished(false, false, false, false);

        registers.setIYH(registers.getA());

        return 8;
    }

    /**
     * The LD IYL,A instruction.
     */
    byte LD_IYL_A() {
        fetchFinished(false, false, false, false);

        registers.setIYL(registers.getA());

        return 8;
    }

    /**
     * The LD A,B instruction.
     */
    byte LD_A_B() {
        fetchFinished(false, false, false, false);

        registers.setA(registers.getB());

        return 4;
    }

    /**
     * The LD B,B instruction.
     */
    byte LD_B_B() {
        fetchFinished(false, false, false, false);

        return 4;
    }

    /**
     * The LD C,B instruction.
     */
    byte LD_C_B() {
        fetchFinished(false, false, false, false);

        registers.setC(registers.getB());

        return 4;
    }

    /**
     * The LD D,B instruction.
     */
    byte LD_D_B() {
        fetchFinished(false, false, false, false);

        registers.setD(registers.getB());

        return 4;
    }

    /**
     * The LD E,B instruction.
     */
    byte LD_E_B() {
        fetchFinished(false, false, false, false);

        registers.setE(registers.getB());

        return 4;
    }

    /**
     * The LD H,B instruction.
     */
    byte LD_H_B() {
        fetchFinished(false, false, false, false);

        registers.setH(registers.getB());

        return 4;
    }

    /**
     * The LD L,B instruction.
     */
    byte LD_L_B() {
        fetchFinished(false, false, false, false);

        registers.setL(registers.getB());

        return 4;
    }

    /**
     * The LD IXH,B instruction.
     */
    byte LD_IXH_B() {
        fetchFinished(false, false, false, false);

        registers.setIXH(registers.getB());

        return 8;
    }

    /**
     * The LD IXL,B instruction.
     */
    byte LD_IXL_B() {
        fetchFinished(false, false, false, false);

        registers.setIXL(registers.getB());

        return 8;
    }

    /**
     * The LD IYH,B instruction.
     */
    byte LD_IYH_B() {
        fetchFinished(false, false, false, false);

        registers.setIYH(registers.getB());

        return 8;
    }

    /**
     * The LD IYL,B instruction.
     */
    byte LD_IYL_B() {
        fetchFinished(false, false, false, false);

        registers.setIYL(registers.getB());

        return 8;
    }

    /**
     * The LD A,C instruction.
     */
    byte LD_A_C() {
        fetchFinished(false, false, false, false);

        registers.setA(registers.getC());

        return 4;
    }

    /**
     * The LD B,C instruction.
     */
    byte LD_B_C() {
        fetchFinished(false, false, false, false);

        registers.setB(registers.getC());

        return 4;
    }

    /**
     * The LD C,C instruction.
     */
    byte LD_C_C() {
        fetchFinished(false, false, false, false);

        return 4;
    }

    /**
     * The LD D,C instruction.
     */
    byte LD_D_C() {
        fetchFinished(false, false, false, false);

        registers.setD(registers.getC());

        return 4;
    }

    /**
     * The LD E,C instruction.
     */
    byte LD_E_C() {
        fetchFinished(false, false, false, false);

        registers.setE(registers.getC());

        return 4;
    }

    /**
     * The LD H,C instruction.
     */
    byte LD_H_C() {
        fetchFinished(false, false, false, false);

        registers.setH(registers.getC());

        return 4;
    }

    /**
     * The LD L,C instruction.
     */
    byte LD_L_C() {
        fetchFinished(false, false, false, false);

        registers.setL(registers.getC());

        return 4;
    }

    /**
     * The LD IXH,C instruction.
     */
    byte LD_IXH_C() {
        fetchFinished(false, false, false, false);

        registers.setIXH(registers.getC());

        return 8;
    }

    /**
     * The LD IXL,C instruction.
     */
    byte LD_IXL_C() {
        fetchFinished(false, false, false, false);

        registers.setIXL(registers.getC());

        return 8;
    }

    /**
     * The LD IYH,C instruction.
     */
    byte LD_IYH_C() {
        fetchFinished(false, false, false, false);

        registers.setIYH(registers.getC());

        return 8;
    }

    /**
     * The LD IYL,C instruction.
     */
    byte LD_IYL_C() {
        fetchFinished(false, false, false, false);

        registers.setIYL(registers.getC());

        return 8;
    }

    /**
     * The LD A,D instruction.
     */
    byte LD_A_D() {
        fetchFinished(false, false, false, false);

        registers.setA(registers.getD());

        return 4;
    }

    /**
     * The LD B,D instruction.
     */
    byte LD_B_D() {
        fetchFinished(false, false, false, false);

        registers.setB(registers.getD());

        return 4;
    }

    /**
     * The LD C,D instruction.
     */
    byte LD_C_D() {
        fetchFinished(false, false, false, false);

        registers.setC(registers.getD());

        return 4;
    }

    /**
     * The LD D,D instruction.
     */
    byte LD_D_D() {
        fetchFinished(false, false, false, false);

        return 4;
    }

    /**
     * The LD E,D instruction.
     */
    byte LD_E_D() {
        fetchFinished(false, false, false, false);

        registers.setE(registers.getD());

        return 4;
    }

    /**
     * The LD H,D instruction.
     */
    byte LD_H_D() {
        fetchFinished(false, false, false, false);

        registers.setH(registers.getD());

        return 4;
    }

    /**
     * The LD L,D instruction.
     */
    byte LD_L_D() {
        fetchFinished(false, false, false, false);

        registers.setL(registers.getD());

        return 4;
    }

    /**
     * The LD IXH,D instruction.
     */
    byte LD_IXH_D() {
        fetchFinished(false, false, false, false);

        registers.setIXH(registers.getD());

        return 8;
    }

    /**
     * The LD IXL,D instruction.
     */
    byte LD_IXL_D() {
        fetchFinished(false, false, false, false);

        registers.setIXL(registers.getD());

        return 8;
    }

    /**
     * The LD IYH,D instruction.
     */
    byte LD_IYH_D() {
        fetchFinished(false, false, false, false);

        registers.setIYH(registers.getD());

        return 8;
    }

    /**
     * The LD IYL,D instruction.
     */
    byte LD_IYL_D() {
        fetchFinished(false, false, false, false);

        registers.setIYL(registers.getD());

        return 8;
    }

    /**
     * The LD A,E instruction.
     */
    byte LD_A_E() {
        fetchFinished(false, false, false, false);

        registers.setA(registers.getE());

        return 4;
    }

    /**
     * The LD B,E instruction.
     */
    byte LD_B_E() {
        fetchFinished(false, false, false, false);

        registers.setB(registers.getE());

        return 4;
    }

    /**
     * The LD C,E instruction.
     */
    byte LD_C_E() {
        fetchFinished(false, false, false, false);

        registers.setC(registers.getE());

        return 4;
    }

    /**
     * The LD D,E instruction.
     */
    byte LD_D_E() {
        fetchFinished(false, false, false, false);

        registers.setD(registers.getE());

        return 4;
    }

    /**
     * The LD E,E instruction.
     */
    byte LD_E_E() {
        fetchFinished(false, false, false, false);

        return 4;
    }

    /**
     * The LD H,E instruction.
     */
    byte LD_H_E() {
        fetchFinished(false, false, false, false);

        registers.setH(registers.getE());

        return 4;
    }

    /**
     * The LD L,E instruction.
     */
    byte LD_L_E() {
        fetchFinished(false, false, false, false);

        registers.setL(registers.getE());

        return 4;
    }

    /**
     * The LD IXH,E instruction.
     */
    byte LD_IXH_E() {
        fetchFinished(false, false, false, false);

        registers.setIXH(registers.getE());

        return 8;
    }

    /**
     * The LD IXL,E instruction.
     */
    byte LD_IXL_E() {
        fetchFinished(false, false, false, false);

        registers.setIXL(registers.getE());

        return 8;
    }

    /**
     * The LD IYH,E instruction.
     */
    byte LD_IYH_E() {
        fetchFinished(false, false, false, false);

        registers.setIYH(registers.getE());

        return 8;
    }

    /**
     * The LD IYL,E instruction.
     */
    byte LD_IYL_E() {
        fetchFinished(false, false, false, false);

        registers.setIYL(registers.getE());

        return 8;
    }

    /**
     * The LD A,H instruction.
     */
    byte LD_A_H() {
        fetchFinished(false, false, false, false);

        registers.setA(registers.getH());

        return 4;
    }

    /**
     * The LD B,H instruction.
     */
    byte LD_B_H() {
        fetchFinished(false, false, false, false);

        registers.setB(registers.getH());

        return 4;
    }

    /**
     * The LD C,H instruction.
     */
    byte LD_C_H() {
        fetchFinished(false, false, false, false);

        registers.setC(registers.getH());

        return 4;
    }

    /**
     * The LD D,H instruction.
     */
    byte LD_D_H() {
        fetchFinished(false, false, false, false);

        registers.setD(registers.getH());

        return 4;
    }

    /**
     * The LD E,H instruction.
     */
    byte LD_E_H() {
        fetchFinished(false, false, false, false);

        registers.setE(registers.getH());

        return 4;
    }

    /**
     * The LD H,H instruction.
     */
    byte LD_H_H() {
        fetchFinished(false, false, false, false);

        return 4;
    }

    /**
     * The LD L,H instruction.
     */
    byte LD_L_H() {
        fetchFinished(false, false, false, false);

        registers.setL(registers.getH());

        return 4;
    }

    /**
     * The LD IXH,H instruction.
     */
    byte LD_IXH_H() {
        fetchFinished(false, false, false, false);

        registers.setIXH(registers.getH());

        return 8;
    }

    /**
     * The LD IXL,H instruction.
     */
    byte LD_IXL_H() {
        fetchFinished(false, false, false, false);

        registers.setIXL(registers.getH());

        return 8;
    }

    /**
     * The LD IYL,H instruction.
     */
    byte LD_IYL_H() {
        fetchFinished(false, false, false, false);

        registers.setIYL(registers.getH());

        return 8;
    }

    /**
     * The LD A,L instruction.
     */
    byte LD_A_L() {
        fetchFinished(false, false, false, false);

        registers.setA(registers.getL());

        return 4;
    }

    /**
     * The LD B,L instruction.
     */
    byte LD_B_L() {
        fetchFinished(false, false, false, false);

        registers.setB(registers.getL());

        return 4;
    }

    /**
     * The LD C,L instruction.
     */
    byte LD_C_L() {
        fetchFinished(false, false, false, false);

        registers.setC(registers.getL());

        return 4;
    }

    /**
     * The LD D,L instruction.
     */
    byte LD_D_L() {
        fetchFinished(false, false, false, false);

        registers.setD(registers.getL());

        return 4;
    }

    /**
     * The LD E,L instruction.
     */
    byte LD_E_L() {
        fetchFinished(false, false, false, false);

        registers.setE(registers.getL());

        return 4;
    }

    /**
     * The LD H,L instruction.
     */
    byte LD_H_L() {
        fetchFinished(false, false, false, false);

        registers.setH(registers.getL());

        return 4;
    }

    /**
     * The LD L,L instruction.
     */
    byte LD_L_L() {
        fetchFinished(false, false, false, false);

        return 4;
    }

    /**
     * The LD A,IXH instruction.
     */
    byte LD_A_IXH() {
        fetchFinished(false, false, false, false);

        registers.setA(registers.getIXH());

        return 8;
    }

    /**
     * The LD B,IXH instruction.
     */
    byte LD_B_IXH() {
        fetchFinished(false, false, false, false);

        registers.setB(registers.getIXH());

        return 8;
    }

    /**
     * The LD C,IXH instruction.
     */
    byte LD_C_IXH() {
        fetchFinished(false, false, false, false);

        registers.setC(registers.getIXH());

        return 8;
    }

    /**
     * The LD D,IXH instruction.
     */
    byte LD_D_IXH() {
        fetchFinished(false, false, false, false);

        registers.setD(registers.getIXH());

        return 8;
    }

    /**
     * The LD E,IXH instruction.
     */
    byte LD_E_IXH() {
        fetchFinished(false, false, false, false);

        registers.setE(registers.getIXH());

        return 8;
    }

    /**
     * The LD IXH,IXH instruction.
     */
    byte LD_IXH_IXH() {
        fetchFinished(false, false, false, false);

        return 8;
    }

    /**
     * The LD A,IXL instruction.
     */
    byte LD_A_IXL() {
        fetchFinished(false, false, false, false);

        registers.setA(registers.getIXL());

        return 8;
    }

    /**
     * The LD B,IXL instruction.
     */
    byte LD_B_IXL() {
        fetchFinished(false, false, false, false);

        registers.setB(registers.getIXL());

        return 8;
    }

    /**
     * The LD C,IXL instruction.
     */
    byte LD_C_IXL() {
        fetchFinished(false, false, false, false);

        registers.setC(registers.getIXL());

        return 8;
    }

    /**
     * The LD D,IXL instruction.
     */
    byte LD_D_IXL() {
        fetchFinished(false, false, false, false);

        registers.setD(registers.getIXL());

        return 8;
    }

    /**
     * The LD E,IXL instruction.
     */
    byte LD_E_IXL() {
        fetchFinished(false, false, false, false);

        registers.setE(registers.getIXL());

        return 8;
    }

    /**
     * The LD IXH,IXL instruction.
     */
    byte LD_IXH_IXL() {
        fetchFinished(false, false, false, false);

        registers.setIXH(registers.getIXL());

        return 8;
    }

    /**
     * The LD IXL,IXL instruction.
     */
    byte LD_IXL_IXL() {
        fetchFinished(false, false, false, false);

        return 8;
    }

    /**
     * The LD A,IYH instruction.
     */
    byte LD_A_IYH() {
        fetchFinished(false, false, false, false);

        registers.setA(registers.getIYH());

        return 8;
    }

    /**
     * The LD B,IYH instruction.
     */
    byte LD_B_IYH() {
        fetchFinished(false, false, false, false);

        registers.setB(registers.getIYH());

        return 8;
    }

    /**
     * The LD C,IYH instruction.
     */
    byte LD_C_IYH() {
        fetchFinished(false, false, false, false);

        registers.setC(registers.getIYH());

        return 8;
    }

    /**
     * The LD D,IYH instruction.
     */
    byte LD_D_IYH() {
        fetchFinished(false, false, false, false);

        registers.setD(registers.getIYH());

        return 8;
    }

    /**
     * The LD E,IYH instruction.
     */
    byte LD_E_IYH() {
        fetchFinished(false, false, false, false);

        registers.setE(registers.getIYH());

        return 8;
    }

    /**
     * The LD IYH,IYH instruction.
     */
    byte LD_IYH_IYH() {
        fetchFinished(false, false, false, false);

        return 8;
    }

    /**
     * The LD A,IYL instruction.
     */
    byte LD_A_IYL() {
        fetchFinished(false, false, false, false);

        registers.setA(registers.getIYL());

        return 8;
    }

    /**
     * The LD B,IYL instruction.
     */
    byte LD_B_IYL() {
        fetchFinished(false, false, false, false);

        registers.setB(registers.getIYL());

        return 8;
    }

    /**
     * The LD C,IYL instruction.
     */
    byte LD_C_IYL() {
        fetchFinished(false, false, false, false);

        registers.setC(registers.getIYL());

        return 8;
    }

    /**
     * The LD D,IYL instruction.
     */
    byte LD_D_IYL() {
        fetchFinished(false, false, false, false);

        registers.setD(registers.getIYL());

        return 8;
    }

    /**
     * The LD E,IYL instruction.
     */
    byte LD_E_IYL() {
        fetchFinished(false, false, false, false);

        registers.setE(registers.getIYL());

        return 8;
    }

    /**
     * The LD IYH,IYL instruction.
     */
    byte LD_IYH_IYL() {
        fetchFinished(false, false, false, false);

        registers.setIYH(registers.getIYL());

        return 8;
    }

    /**
     * The LD IYL,IYL instruction.
     */
    byte LD_IYL_IYL() {
        fetchFinished(false, false, false, false);

        return 8;
    }

//#endregion

//#region LD SP,HL +

    /**
     * The LD SP,HL instruction.
     */
    byte LD_SP_HL() {
        fetchFinished(false, false, /*isLdSp:*/ true, false);

        registers.setSP(registers.getHL());

        return 6;
    }

    /**
     * The LD SP,IX instruction.
     */
    byte LD_SP_IX() {
        fetchFinished(false, false, /*isLdSp:*/ true, false);

        registers.setSP(registers.getIX());

        return 10;
    }

    /**
     * The LD SP,IY instruction.
     */
    byte LD_SP_IY() {
        fetchFinished(false, false, /*isLdSp:*/ true, false);

        registers.setSP(registers.getIY());

        return 10;
    }

//#endregion

//#region LDI +

    /**
     * The LDI instruction.
     */
    byte LDI() {
        fetchFinished(false, false, false, false);

        var sourceAddress = registers.getHL();
        var destAddress = registers.getDE();
        var counter = registers.getBC();
        var value = processorAgent.readFromMemory(sourceAddress);
        processorAgent.writeToMemory(destAddress, value);

        registers.setHL((short) (sourceAddress + 1));
        registers.setDE((short) (destAddress + 1));
        counter--;
        registers.setBC(counter);

        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        registers.setPF(Bit.of((counter != 0)));

        var valuePlusA = (byte) (value + registers.getA());
        registers.setFlag3(getBit(valuePlusA, 3));
        registers.setFlag5(getBit(valuePlusA, 1));


        return 16;
    }

    /**
     * The LDD instruction.
     */
    byte LDD() {
        fetchFinished(false, false, false, false);

        var sourceAddress = registers.getHL();
        var destAddress = registers.getDE();
        var counter = registers.getBC();
        var value = processorAgent.readFromMemory(sourceAddress);
        processorAgent.writeToMemory(destAddress, value);

        registers.setHL((short) (sourceAddress - 1));
        registers.setDE((short) (destAddress - 1));
        counter--;
        registers.setBC(counter);

        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        registers.setPF(Bit.of((counter != 0)));

        var valuePlusA = (byte) (value + registers.getA());
        registers.setFlag3(getBit(valuePlusA, 3));
        registers.setFlag5(getBit(valuePlusA, 1));


        return 16;
    }

    /**
     * The LDIR instruction.
     */
    byte LDIR() {
        fetchFinished(false, false, false, false);

        var sourceAddress = registers.getHL();
        var destAddress = registers.getDE();
        var counter = registers.getBC();
        var value = processorAgent.readFromMemory(sourceAddress);
        processorAgent.writeToMemory(destAddress, value);

        registers.setHL((short) (sourceAddress + 1));
        registers.setDE((short) (destAddress + 1));
        counter--;
        registers.setBC(counter);

        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        registers.setPF(Bit.of((counter != 0)));

        var valuePlusA = (byte) (value + registers.getA());
        registers.setFlag3(getBit(valuePlusA, 3));
        registers.setFlag5(getBit(valuePlusA, 1));

        if (counter != 0) {
            registers.setPC((short) (registers.getPC() - 2));
            return 21;
        }

        return 16;
    }

    /**
     * The LDDR instruction.
     */
    byte LDDR() {
        fetchFinished(false, false, false, false);

        var sourceAddress = registers.getHL();
        var destAddress = registers.getDE();
        var counter = registers.getBC();
        var value = processorAgent.readFromMemory(sourceAddress);
        processorAgent.writeToMemory(destAddress, value);

        registers.setHL((short) (sourceAddress - 1));
        registers.setDE((short) (destAddress - 1));
        counter--;
        registers.setBC(counter);

        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        registers.setPF(Bit.of((counter != 0)));

        var valuePlusA = (byte) (value + registers.getA());
        registers.setFlag3(getBit(valuePlusA, 3));
        registers.setFlag5(getBit(valuePlusA, 1));

        if (counter != 0) {
            registers.setPC((short) (registers.getPC() - 2));
            return 21;
        }

        return 16;
    }

//#endregion

//#region NEG

    /**
     * The NEG instruction.
     */
    byte NEG() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = (byte) -oldValue;
        registers.setA(newValue);

        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setHF(Bit.of((oldValue ^ newValue) & 0x10));
        registers.setPF(Bit.of((newValue & 0xff) == 0x80));
        registers.setNF(Bit.ON);
        registers.setCF(Bit.of(oldValue != 0));
        setFlags3and5From(newValue);

        return 8;
    }

//#endregion

//#region NOP

    /**
     * The NOP instruction.
     */
    byte NOP() {
        fetchFinished(false, false, false, false);
        return 4;
    }

//#endregion

//#region NOP2

    /**
     * The NOP2 instruction (equivalent to two NOPs, used for unsupported instructions).
     */
    byte NOP2() {
        fetchFinished(false, false, false, false);
        return 8;
    }

//#endregion

//#region OUT (C),r

    /**
     * The OUT (C),A instruction.
     */
    byte OUT_C_A() {
        fetchFinished(false, false, false, false);

        processorAgent.writeToPort(registers.getC(), registers.getA());

        return 12;
    }

    /**
     * The OUT (C),B instruction.
     */
    byte OUT_C_B() {
        fetchFinished(false, false, false, false);

        processorAgent.writeToPort(registers.getC(), registers.getB());

        return 12;
    }

    /**
     * The OUT (C),C instruction.
     */
    byte OUT_C_C() {
        fetchFinished(false, false, false, false);

        processorAgent.writeToPort(registers.getC(), registers.getC());

        return 12;
    }

    /**
     * The OUT (C),D instruction.
     */
    byte OUT_C_D() {
        fetchFinished(false, false, false, false);

        processorAgent.writeToPort(registers.getC(), registers.getD());

        return 12;
    }

    /**
     * The OUT (C),E instruction.
     */
    byte OUT_C_E() {
        fetchFinished(false, false, false, false);

        processorAgent.writeToPort(registers.getC(), registers.getE());

        return 12;
    }

    /**
     * The OUT (C),H instruction.
     */
    byte OUT_C_H() {
        fetchFinished(false, false, false, false);

        processorAgent.writeToPort(registers.getC(), registers.getH());

        return 12;
    }

    /**
     * The OUT (C),L instruction.
     */
    byte OUT_C_L() {
        fetchFinished(false, false, false, false);

        processorAgent.writeToPort(registers.getC(), registers.getL());

        return 12;
    }

    /**
     * The OUT (C),0 instruction.
     */
    byte OUT_C_0() {
        fetchFinished(false, false, false, false);

        processorAgent.writeToPort(registers.getC(), (byte) 0);

        return 12;
    }

//#endregion

//#region OUT (n),A

    /**
     * The OUT (n),A instruction.
     */
    byte OUT_n_A() {
        var portNumber = processorAgent.fetchNextOpcode();
        fetchFinished(false, false, false, false);

        processorAgent.writeToPort(portNumber, registers.getA());

        return 11;
    }

//#endregion

//#region PUSH rr +

    /**
     * The PUSH AF instruction.
     */
    private byte PUSH_AF() {
        fetchFinished(false, false, false, false);

        var valueToPush = registers.getAF();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);

        return 11;
    }

    /**
     * The POP AF instruction.
     */
    private byte POP_AF() {
        fetchFinished(false, false, false, false);

        var sp = registers.getSP();
        var newAF = createShort(
                processorAgent.readFromMemory(sp),
                processorAgent.readFromMemory(inc(sp)));
        registers.setAF(newAF);

        registers.addSP((short) 2);

        return 10;
    }

    /**
     * The PUSH BC instruction.
     */
    private byte PUSH_BC() {
        fetchFinished(false, false, false, false);

        var valueToPush = registers.getBC();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);

        return 11;
    }

    /**
     * The POP BC instruction.
     */
    private byte POP_BC() {
        fetchFinished(false, false, false, false);

        var sp = registers.getSP();
        var newBC = createShort(
                processorAgent.readFromMemory(sp),
                processorAgent.readFromMemory(inc(sp)));
        registers.setBC(newBC);

        registers.addSP((short) 2);

        return 10;
    }

    /**
     * The PUSH DE instruction.
     */
    private byte PUSH_DE() {
        fetchFinished(false, false, false, false);

        var valueToPush = registers.getDE();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);

        return 11;
    }

    /**
     * The POP DE instruction.
     */
    private byte POP_DE() {
        fetchFinished(false, false, false, false);

        var sp = registers.getSP();
        var newDE = createShort(
                processorAgent.readFromMemory(sp),
                processorAgent.readFromMemory(inc(sp)));
        registers.setDE(newDE);

        registers.addSP((short) 2);

        return 10;
    }

    /**
     * The PUSH HL instruction.
     */
    private byte PUSH_HL() {
        fetchFinished(false, false, false, false);

        var valueToPush = registers.getHL();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);

        return 11;
    }

    /**
     * The POP HL instruction.
     */
    private byte POP_HL() {
        fetchFinished(false, false, false, false);

        var sp = registers.getSP();
        var newHL = createShort(
                processorAgent.readFromMemory(sp),
                processorAgent.readFromMemory(inc(sp)));
        registers.setHL(newHL);

        registers.addSP((short) 2);

        return 10;
    }

    /**
     * The PUSH IX instruction.
     */
    private byte PUSH_IX() {
        fetchFinished(false, false, false, false);

        var valueToPush = registers.getIX();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);

        return 15;
    }

    /**
     * The POP IX instruction.
     */
    private byte POP_IX() {
        fetchFinished(false, false, false, false);

        var sp = registers.getSP();
        var newIX = createShort(
                processorAgent.readFromMemory(sp),
                processorAgent.readFromMemory(inc(sp)));
        registers.setIX(newIX);

        registers.addSP((short) 2);

        return 14;
    }

    /**
     * The PUSH IY instruction.
     */
    private byte PUSH_IY() {
        fetchFinished(false, false, false, false);

        var valueToPush = registers.getIY();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);

        return 15;
    }

    /**
     * The POP IY instruction.
     */
    private byte POP_IY() {
        fetchFinished(false, false, false, false);

        var sp = registers.getSP();
        var newIY = createShort(
                processorAgent.readFromMemory(sp),
                processorAgent.readFromMemory(inc(sp)));
        registers.setIY(newIY);

        registers.addSP((short) 2);

        return 14;
    }

//#endregion

//#region RET +

    /**
     * The RET instruction.
     */
    private byte RET() {
        fetchFinished(/*isRet:*/true, false, false, false);

        var sp = registers.getSP();
        var newPC = createShort(
                processorAgent.readFromMemory(sp),
                processorAgent.readFromMemory(inc(sp)));
        registers.setPC(newPC);

        registers.addSP((short) 2);

        return 10;
    }

    /**
     * The RETI instruction.
     */
    private byte RETI() {
        fetchFinished(/*isRet:*/true, false, false, false);

        var sp = registers.getSP();
        var newPC = createShort(
                processorAgent.readFromMemory(sp),
                processorAgent.readFromMemory(inc(sp)));
        registers.setPC(newPC);

        registers.addSP((short) 2);

        return 14;
    }

    /**
     * The JP instruction.
     */
    private byte JP_nn() {
        var newAddress = fetchWord();

        fetchFinished(false, false, false, false);

        registers.setPC(newAddress);

        return 10;
    }

    /**
     * The CALL instruction.
     */
    private byte CALL_nn() {
        var newAddress = fetchWord();

        fetchFinished(false, false, false, false);

        var valueToPush = registers.getPC();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);
        registers.setPC(newAddress);

        return 17;
    }

    /**
     * The RET C instruction.
     */
    private byte RET_C() {
        if (!registers.getCF().booleanValue()) {
            fetchFinished(/*isRet:*/ false, false, false, false);
            return 5;
        }

        fetchFinished(/*isRet:*/true, false, false, false);

        var sp = registers.getSP();
        var newPC = createShort(
                processorAgent.readFromMemory(sp),
                processorAgent.readFromMemory(inc(sp)));
        registers.setPC(newPC);

        registers.addSP((short) 2);

        return 11;
    }

    /**
     * The JP C instruction.
     */
    private byte JP_C_nn() {
        var newAddress = fetchWord();

        fetchFinished(false, false, false, false);
        if (!registers.getCF().booleanValue())
            return 10;

        registers.setPC(newAddress);

        return 10;
    }

    /**
     * The CALL C instruction.
     */
    private byte CALL_C_nn() {
        var newAddress = fetchWord();

        fetchFinished(false, false, false, false);
        if (!registers.getCF().booleanValue())
            return 10;

        var valueToPush = registers.getPC();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);
        registers.setPC(newAddress);

        return 17;
    }

    /**
     * The RET NC instruction.
     */
    private byte RET_NC() {
        if (registers.getCF().booleanValue()) {
            fetchFinished(/*isRet:*/ false, false, false, false);
            return 5;
        }

        fetchFinished(/*isRet:*/true, false, false, false);

        var sp = registers.getSP();
        var newPC = createShort(
                processorAgent.readFromMemory(sp),
                processorAgent.readFromMemory(inc(sp)));
        registers.setPC(newPC);

        registers.addSP((short) 2);

        return 11;
    }

    /**
     * The JP NC instruction.
     */
    private byte JP_NC_nn() {
        var newAddress = fetchWord();

        fetchFinished(false, false, false, false);
        if (registers.getCF().booleanValue())
            return 10;

        registers.setPC(newAddress);

        return 10;
    }

    /**
     * The CALL NC instruction.
     */
    private byte CALL_NC_nn() {
        var newAddress = fetchWord();

        fetchFinished(false, false, false, false);
        if (registers.getCF().booleanValue())
            return 10;

        var valueToPush = registers.getPC();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);
        registers.setPC(newAddress);

        return 17;
    }

    /**
     * The RET Z instruction.
     */
    private byte RET_Z() {
        if (!registers.getZF().booleanValue()) {
            fetchFinished(/*isRet:*/ false, false, false, false);
            return 5;
        }

        fetchFinished(/*isRet:*/true, false, false, false);

        var sp = registers.getSP();
        var newPC = createShort(
                processorAgent.readFromMemory(sp),
                processorAgent.readFromMemory(inc(sp)));
        registers.setPC(newPC);

        registers.addSP((short) 2);

        return 11;
    }

    /**
     * The JP Z instruction.
     */
    private byte JP_Z_nn() {
        var newAddress = fetchWord();

        fetchFinished(false, false, false, false);
        if (!registers.getZF().booleanValue())
            return 10;

        registers.setPC(newAddress);

        return 10;
    }

    /**
     * The CALL Z instruction.
     */
    private byte CALL_Z_nn() {
        var newAddress = fetchWord();

        fetchFinished(false, false, false, false);
        if (!registers.getZF().booleanValue())
            return 10;

        var valueToPush = registers.getPC();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);
        registers.setPC(newAddress);

        return 17;
    }

    /**
     * The RET NZ instruction.
     */
    private byte RET_NZ() {
        if (registers.getZF().booleanValue()) {
            fetchFinished(/*isRet:*/ false, false, false, false);
            return 5;
        }

        fetchFinished(/*isRet:*/true, false, false, false);

        var sp = registers.getSP();
        var newPC = createShort(
                processorAgent.readFromMemory(sp),
                processorAgent.readFromMemory(inc(sp)));
        registers.setPC(newPC);

        registers.addSP((short) 2);

        return 11;
    }

    /**
     * The JP NZ instruction.
     */
    private byte JP_NZ_nn() {
        var newAddress = fetchWord();

        fetchFinished(false, false, false, false);
        if (registers.getZF().booleanValue())
            return 10;

        registers.setPC(newAddress);

        return 10;
    }

    /**
     * The CALL NZ instruction.
     */
    private byte CALL_NZ_nn() {
        var newAddress = fetchWord();

        fetchFinished(false, false, false, false);
        if (registers.getZF().booleanValue())
            return 10;

        var valueToPush = registers.getPC();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);
        registers.setPC(newAddress);

        return 17;
    }

    /**
     * The RET PE instruction.
     */
    private byte RET_PE() {
        if (!registers.getPF().booleanValue()) {
            fetchFinished(/*isRet:*/ false, false, false, false);
            return 5;
        }

        fetchFinished(/*isRet:*/true, false, false, false);

        var sp = registers.getSP();
        var newPC = createShort(
                processorAgent.readFromMemory(sp),
                processorAgent.readFromMemory(inc(sp)));
        registers.setPC(newPC);

        registers.addSP((short) 2);

        return 11;
    }

    /**
     * The JP PE instruction.
     */
    private byte JP_PE_nn() {
        var newAddress = fetchWord();

        fetchFinished(false, false, false, false);
        if (!registers.getPF().booleanValue())
            return 10;

        registers.setPC(newAddress);

        return 10;
    }

    /**
     * The CALL PE instruction.
     */
    private byte CALL_PE_nn() {
        var newAddress = fetchWord();

        fetchFinished(false, false, false, false);
        if (!registers.getPF().booleanValue())
            return 10;

        var valueToPush = registers.getPC();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);
        registers.setPC(newAddress);

        return 17;
    }

    /**
     * The RET PO instruction.
     */
    private byte RET_PO() {
        if (registers.getPF().booleanValue()) {
            fetchFinished(/*isRet:*/ false, false, false, false);
            return 5;
        }

        fetchFinished(/*isRet:*/true, false, false, false);

        var sp = registers.getSP();
        var newPC = createShort(
                processorAgent.readFromMemory(sp),
                processorAgent.readFromMemory(inc(sp)));
        registers.setPC(newPC);

        registers.addSP((short) 2);

        return 11;
    }

    /**
     * The JP PO instruction.
     */
    private byte JP_PO_nn() {
        var newAddress = fetchWord();

        fetchFinished(false, false, false, false);
        if (registers.getPF().booleanValue())
            return 10;

        registers.setPC(newAddress);

        return 10;
    }

    /**
     * The CALL PO instruction.
     */
    private byte CALL_PO_nn() {
        var newAddress = fetchWord();

        fetchFinished(false, false, false, false);
        if (registers.getPF().booleanValue())
            return 10;

        var valueToPush = registers.getPC();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);
        registers.setPC(newAddress);

        return 17;
    }

    /**
     * The RET M instruction.
     */
    private byte RET_M() {
        if (!registers.getSF().booleanValue()) {
            fetchFinished(/*isRet:*/ false, false, false, false);
            return 5;
        }

        fetchFinished(/*isRet:*/true, false, false, false);

        var sp = registers.getSP();
        var newPC = createShort(
                processorAgent.readFromMemory(sp),
                processorAgent.readFromMemory(inc(sp)));
        registers.setPC(newPC);

        registers.addSP((short) 2);

        return 11;
    }

    /**
     * The JP M instruction.
     */
    private byte JP_M_nn() {
        var newAddress = fetchWord();

        fetchFinished(false, false, false, false);
        if (!registers.getSF().booleanValue())
            return 10;

        registers.setPC(newAddress);

        return 10;
    }

    /**
     * The CALL M instruction.
     */
    private byte CALL_M_nn() {
        var newAddress = fetchWord();

        fetchFinished(false, false, false, false);
        if (!registers.getSF().booleanValue())
            return 10;

        var valueToPush = registers.getPC();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);
        registers.setPC(newAddress);

        return 17;
    }

    /**
     * The RET P instruction.
     */
    private byte RET_P() {
        if (registers.getSF().booleanValue()) {
            fetchFinished(/*isRet:*/ false, false, false, false);
            return 5;
        }

        fetchFinished(/*isRet:*/true, false, false, false);

        var sp = registers.getSP();
        var newPC = createShort(
                processorAgent.readFromMemory(sp),
                processorAgent.readFromMemory(inc(sp)));
        registers.setPC(newPC);

        registers.addSP((short) 2);

        return 11;
    }

    /**
     * The JP P instruction.
     */
    private byte JP_P_nn() {
        var newAddress = fetchWord();

        fetchFinished(false, false, false, false);
        if (registers.getSF().booleanValue())
            return 10;

        registers.setPC(newAddress);

        return 10;
    }

    /**
     * The CALL P instruction.
     */
    private byte CALL_P_nn() {
        var newAddress = fetchWord();

        fetchFinished(false, false, false, false);
        if (registers.getSF().booleanValue())
            return 10;

        var valueToPush = registers.getPC();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);
        registers.setPC(newAddress);

        return 17;
    }

//#endregion

//#region RETN

    /**
     * The RETN instruction.
     */
    private byte RETN() {
        fetchFinished(/*isRet:*/true, false, false, false);

        var sp = registers.getSP();
        var newPC = createShort(
                processorAgent.readFromMemory(sp),
                processorAgent.readFromMemory(inc(sp)));
        registers.setPC(newPC);

        registers.addSP((short) 2);

        registers.setIFF1(registers.getIFF2());

        return 14;
    }

//#endregion

//#region RLCA +

    /**
     * The RLC A instruction
     */
    byte RLC_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RLC B instruction
     */
    byte RLC_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RLC C instruction
     */
    byte RLC_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RLC D instruction
     */
    byte RLC_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RLC E instruction
     */
    byte RLC_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RLC H instruction
     */
    byte RLC_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RLC L instruction
     */
    byte RLC_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RLC (HL) instruction
     */
    byte RLC_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 15;
    }

    /**
     * The RLCA instruction
     */
    byte RLCA() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        return 4;
    }

    /**
     * The RLC (IX+n),A instruction
     */
    byte RLC_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RLC (IX+n),B instruction
     */
    byte RLC_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RLC (IX+n),C instruction
     */
    byte RLC_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RLC (IX+n),D instruction
     */
    byte RLC_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RLC (IX+n),E instruction
     */
    byte RLC_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RLC (IX+n),H instruction
     */
    byte RLC_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RLC (IX+n),L instruction
     */
    byte RLC_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RLC (IX+n) instruction
     */
    byte RLC_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RLC (IY+n),A instruction
     */
    byte RLC_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RLC (IY+n),B instruction
     */
    byte RLC_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RLC (IY+n),C instruction
     */
    byte RLC_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RLC (IY+n),D instruction
     */
    byte RLC_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RLC (IY+n),E instruction
     */
    byte RLC_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RLC (IY+n),H instruction
     */
    byte RLC_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RLC (IY+n),L instruction
     */
    byte RLC_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RLC (IY+n) instruction
     */
    byte RLC_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | shift7Right(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RRC A instruction
     */
    byte RRC_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RRC B instruction
     */
    byte RRC_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RRC C instruction
     */
    byte RRC_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RRC D instruction
     */
    byte RRC_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RRC E instruction
     */
    byte RRC_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RRC H instruction
     */
    byte RRC_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RRC L instruction
     */
    byte RRC_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RRC (HL) instruction
     */
    byte RRC_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 15;
    }

    /**
     * The RRCA instruction
     */
    byte RRCA() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        return 4;
    }

    /**
     * The RRC (IX+n),A instruction
     */
    byte RRC_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RRC (IX+n),B instruction
     */
    byte RRC_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RRC (IX+n),C instruction
     */
    byte RRC_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RRC (IX+n),D instruction
     */
    byte RRC_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RRC (IX+n),E instruction
     */
    byte RRC_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RRC (IX+n),H instruction
     */
    byte RRC_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RRC (IX+n),L instruction
     */
    byte RRC_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RRC (IX+n) instruction
     */
    byte RRC_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RRC (IY+n),A instruction
     */
    byte RRC_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RRC (IY+n),B instruction
     */
    byte RRC_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RRC (IY+n),C instruction
     */
    byte RRC_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RRC (IY+n),D instruction
     */
    byte RRC_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RRC (IY+n),E instruction
     */
    byte RRC_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RRC (IY+n),H instruction
     */
    byte RRC_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RRC (IY+n),L instruction
     */
    byte RRC_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RRC (IY+n) instruction
     */
    byte RRC_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | shift7Left(oldValue)) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RL A instruction
     */
    byte RL_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RL B instruction
     */
    byte RL_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RL C instruction
     */
    byte RL_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RL D instruction
     */
    byte RL_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RL E instruction
     */
    byte RL_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RL H instruction
     */
    byte RL_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RL L instruction
     */
    byte RL_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RL (HL) instruction
     */
    byte RL_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 15;
    }

    /**
     * The RLA instruction
     */
    byte RLA() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        return 4;
    }

    /**
     * The RL (IX+n),A instruction
     */
    byte RL_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RL (IX+n),B instruction
     */
    byte RL_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RL (IX+n),C instruction
     */
    byte RL_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RL (IX+n),D instruction
     */
    byte RL_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RL (IX+n),E instruction
     */
    byte RL_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RL (IX+n),H instruction
     */
    byte RL_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RL (IX+n),L instruction
     */
    byte RL_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RL (IX+n) instruction
     */
    byte RL_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RL (IY+n),A instruction
     */
    byte RL_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RL (IY+n),B instruction
     */
    byte RL_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RL (IY+n),C instruction
     */
    byte RL_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RL (IY+n),D instruction
     */
    byte RL_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RL (IY+n),E instruction
     */
    byte RL_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RL (IY+n),H instruction
     */
    byte RL_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RL (IY+n),L instruction
     */
    byte RL_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RL (IY+n) instruction
     */
    byte RL_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | registers.getCF().intValue()) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RR A instruction
     */
    byte RR_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RR B instruction
     */
    byte RR_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RR C instruction
     */
    byte RR_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RR D instruction
     */
    byte RR_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RR E instruction
     */
    byte RR_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RR H instruction
     */
    byte RR_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RR L instruction
     */
    byte RR_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The RR (HL) instruction
     */
    byte RR_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 15;
    }

    /**
     * The RRA instruction
     */
    byte RRA() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        return 4;
    }

    /**
     * The RR (IX+n),A instruction
     */
    byte RR_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RR (IX+n),B instruction
     */
    byte RR_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RR (IX+n),C instruction
     */
    byte RR_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RR (IX+n),D instruction
     */
    byte RR_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RR (IX+n),E instruction
     */
    byte RR_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RR (IX+n),H instruction
     */
    byte RR_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RR (IX+n),L instruction
     */
    byte RR_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RR (IX+n) instruction
     */
    byte RR_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RR (IY+n),A instruction
     */
    byte RR_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RR (IY+n),B instruction
     */
    byte RR_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RR (IY+n),C instruction
     */
    byte RR_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RR (IY+n),D instruction
     */
    byte RR_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RR (IY+n),E instruction
     */
    byte RR_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RR (IY+n),H instruction
     */
    byte RR_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RR (IY+n),L instruction
     */
    byte RR_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The RR (IY+n) instruction
     */
    byte RR_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (registers.getCF().booleanValue() ? 0x80 : 0)) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLA A instruction
     */
    byte SLA_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SLA B instruction
     */
    byte SLA_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SLA C instruction
     */
    byte SLA_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SLA D instruction
     */
    byte SLA_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SLA E instruction
     */
    byte SLA_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SLA H instruction
     */
    byte SLA_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SLA L instruction
     */
    byte SLA_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SLA (HL) instruction
     */
    byte SLA_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 15;
    }

    /**
     * The SLA (IX+n),A instruction
     */
    byte SLA_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLA (IX+n),B instruction
     */
    byte SLA_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLA (IX+n),C instruction
     */
    byte SLA_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLA (IX+n),D instruction
     */
    byte SLA_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLA (IX+n),E instruction
     */
    byte SLA_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLA (IX+n),H instruction
     */
    byte SLA_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLA (IX+n),L instruction
     */
    byte SLA_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLA (IX+n) instruction
     */
    byte SLA_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLA (IY+n),A instruction
     */
    byte SLA_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLA (IY+n),B instruction
     */
    byte SLA_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLA (IY+n),C instruction
     */
    byte SLA_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLA (IY+n),D instruction
     */
    byte SLA_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLA (IY+n),E instruction
     */
    byte SLA_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLA (IY+n),H instruction
     */
    byte SLA_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLA (IY+n),L instruction
     */
    byte SLA_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLA (IY+n) instruction
     */
    byte SLA_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (siftLeft(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRA A instruction
     */
    byte SRA_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SRA B instruction
     */
    byte SRA_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SRA C instruction
     */
    byte SRA_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SRA D instruction
     */
    byte SRA_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SRA E instruction
     */
    byte SRA_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SRA H instruction
     */
    byte SRA_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SRA L instruction
     */
    byte SRA_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SRA (HL) instruction
     */
    byte SRA_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 15;
    }

    /**
     * The SRA (IX+n),A instruction
     */
    byte SRA_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRA (IX+n),B instruction
     */
    byte SRA_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRA (IX+n),C instruction
     */
    byte SRA_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRA (IX+n),D instruction
     */
    byte SRA_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRA (IX+n),E instruction
     */
    byte SRA_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRA (IX+n),H instruction
     */
    byte SRA_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRA (IX+n),L instruction
     */
    byte SRA_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRA (IX+n) instruction
     */
    byte SRA_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRA (IY+n),A instruction
     */
    byte SRA_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRA (IY+n),B instruction
     */
    byte SRA_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRA (IY+n),C instruction
     */
    byte SRA_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRA (IY+n),D instruction
     */
    byte SRA_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRA (IY+n),E instruction
     */
    byte SRA_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRA (IY+n),H instruction
     */
    byte SRA_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRA (IY+n),L instruction
     */
    byte SRA_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRA (IY+n) instruction
     */
    byte SRA_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((shiftRight(oldValue) | (oldValue & 0x80)) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLL A instruction
     */
    byte SLL_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SLL B instruction
     */
    byte SLL_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SLL C instruction
     */
    byte SLL_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SLL D instruction
     */
    byte SLL_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SLL E instruction
     */
    byte SLL_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SLL H instruction
     */
    byte SLL_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SLL L instruction
     */
    byte SLL_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SLL (HL) instruction
     */
    byte SLL_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 15;
    }

    /**
     * The SLL (IX+n),A instruction
     */
    byte SLL_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLL (IX+n),B instruction
     */
    byte SLL_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLL (IX+n),C instruction
     */
    byte SLL_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLL (IX+n),D instruction
     */
    byte SLL_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLL (IX+n),E instruction
     */
    byte SLL_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLL (IX+n),H instruction
     */
    byte SLL_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLL (IX+n),L instruction
     */
    byte SLL_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLL (IX+n) instruction
     */
    byte SLL_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLL (IY+n),A instruction
     */
    byte SLL_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLL (IY+n),B instruction
     */
    byte SLL_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLL (IY+n),C instruction
     */
    byte SLL_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLL (IY+n),D instruction
     */
    byte SLL_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLL (IY+n),E instruction
     */
    byte SLL_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLL (IY+n),H instruction
     */
    byte SLL_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLL (IY+n),L instruction
     */
    byte SLL_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SLL (IY+n) instruction
     */
    byte SLL_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) ((siftLeft(oldValue) | 1) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 7));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRL A instruction
     */
    byte SRL_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SRL B instruction
     */
    byte SRL_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SRL C instruction
     */
    byte SRL_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SRL D instruction
     */
    byte SRL_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SRL E instruction
     */
    byte SRL_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SRL H instruction
     */
    byte SRL_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SRL L instruction
     */
    byte SRL_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 8;
    }

    /**
     * The SRL (HL) instruction
     */
    byte SRL_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 15;
    }

    /**
     * The SRL (IX+n),A instruction
     */
    byte SRL_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRL (IX+n),B instruction
     */
    byte SRL_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRL (IX+n),C instruction
     */
    byte SRL_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRL (IX+n),D instruction
     */
    byte SRL_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRL (IX+n),E instruction
     */
    byte SRL_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRL (IX+n),H instruction
     */
    byte SRL_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRL (IX+n),L instruction
     */
    byte SRL_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRL (IX+n) instruction
     */
    byte SRL_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRL (IY+n),A instruction
     */
    byte SRL_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRL (IY+n),B instruction
     */
    byte SRL_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRL (IY+n),C instruction
     */
    byte SRL_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRL (IY+n),D instruction
     */
    byte SRL_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRL (IY+n),E instruction
     */
    byte SRL_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRL (IY+n),H instruction
     */
    byte SRL_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRL (IY+n),L instruction
     */
    byte SRL_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

    /**
     * The SRL (IY+n) instruction
     */
    byte SRL_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = (byte) (shiftRight(oldValue) & 0xff);
        processorAgent.writeToMemory(address, newValue);

        registers.setCF(getBit(oldValue, 0));
        registers.setHF(Bit.OFF);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newValue);
        registers.setSF(getBit(newValue, 7));
        registers.setZF(Bit.of((newValue == 0)));
        registers.setPF(parity[newValue & 0xff]);

        return 23;
    }

//#endregion

//#region RRD +

    /**
     * The RRD instruction.
     */
    byte RRD() {
        fetchFinished(false, false, false, false);

        var memoryAddress = registers.getHL();

        var Avalue = registers.getA();
        var HLcontents = processorAgent.readFromMemory(memoryAddress);

        var newAvalue = (byte) ((Avalue & 0xF0) | (HLcontents & 0x0F));
        var newHLcontents = (byte) (((HLcontents >> 4) & 0x0F) | ((Avalue << 4) & 0xF0));
        registers.setA(newAvalue);
        processorAgent.writeToMemory(memoryAddress, newHLcontents);

        registers.setSF(getBit(newAvalue, 7));
        registers.setZF(Bit.of((newAvalue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newAvalue & 0xff]);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newAvalue);

        return 18;
    }

    /**
     * The RLD instruction.
     */
    byte RLD() {
        fetchFinished(false, false, false, false);

        var memoryAddress = registers.getHL();

        var Avalue = registers.getA();
        var HLcontents = processorAgent.readFromMemory(memoryAddress);

        var newAvalue = (byte) ((Avalue & 0xF0) | ((HLcontents >> 4) & 0x0F));
        var newHLcontents = (byte) (((HLcontents << 4) & 0xF0) | (Avalue & 0x0F));
        registers.setA(newAvalue);
        processorAgent.writeToMemory(memoryAddress, newHLcontents);

        registers.setSF(getBit(newAvalue, 7));
        registers.setZF(Bit.of((newAvalue == 0)));
        registers.setHF(Bit.OFF);
        registers.setPF(parity[newAvalue & 0xff]);
        registers.setNF(Bit.OFF);
        setFlags3and5From(newAvalue);

        return 18;
    }

//#endregion

//#region RST

    /**
     * The RST 00h instruction.
     */
    private byte RST_00() {
        fetchFinished(false, false, false, false);

        var valueToPush = registers.getPC();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);
        registers.setPC((short) 0x00);

        return 11;
    }

    /**
     * The RST 08h instruction.
     */
    private byte RST_08() {
        fetchFinished(false, false, false, false);

        var valueToPush = registers.getPC();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);
        registers.setPC((short) 0x08);

        return 11;
    }

    /**
     * The RST 10h instruction.
     */
    private byte RST_10() {
        fetchFinished(false, false, false, false);

        var valueToPush = registers.getPC();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);
        registers.setPC((short) 0x10);

        return 11;
    }

    /**
     * The RST 18h instruction.
     */
    private byte RST_18() {
        fetchFinished(false, false, false, false);

        var valueToPush = registers.getPC();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);
        registers.setPC((short) 0x18);

        return 11;
    }

    /**
     * The RST 20h instruction.
     */
    private byte RST_20() {
        fetchFinished(false, false, false, false);

        var valueToPush = registers.getPC();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);
        registers.setPC((short) 0x20);

        return 11;
    }

    /**
     * The RST 28h instruction.
     */
    private byte RST_28() {
        fetchFinished(false, false, false, false);

        var valueToPush = registers.getPC();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);
        registers.setPC((short) 0x28);

        return 11;
    }

    /**
     * The RST 30h instruction.
     */
    private byte RST_30() {
        fetchFinished(false, false, false, false);

        var valueToPush = registers.getPC();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);
        registers.setPC((short) 0x30);

        return 11;
    }

    /**
     * The RST 38h instruction.
     */
    private byte RST_38() {
        fetchFinished(false, false, false, false);

        var valueToPush = registers.getPC();
        var sp = dec(registers.getSP());
        processorAgent.writeToMemory(sp, getHighByte(valueToPush));
        sp = dec(sp);
        processorAgent.writeToMemory(sp, getLowByte(valueToPush));
        registers.setSP(sp);
        registers.setPC((short) 0x38);

        return 11;
    }

//#endregion

//#region SCF

    private static final int HF_NF_reset = 0xED;
    private static final int CF_set = 1;

    /**
     * The SCF instruction.
     */
    byte SCF() {
        fetchFinished(false, false, false, false);

        registers.setF((byte) (registers.getF() & HF_NF_reset | CF_set));
        setFlags3and5From(registers.getA());

        return 4;
    }

//#endregion

//#region SET b,r +

    /**
     * The SET 0,A instruction
     */
    byte SET_0_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = withBit(oldValue, 0, 1);
        registers.setA(newValue);

        return 8;
    }

    /**
     * The SET 1,A instruction
     */
    byte SET_1_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = withBit(oldValue, 1, 1);
        registers.setA(newValue);

        return 8;
    }

    /**
     * The SET 2,A instruction
     */
    byte SET_2_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = withBit(oldValue, 2, 1);
        registers.setA(newValue);

        return 8;
    }

    /**
     * The SET 3,A instruction
     */
    byte SET_3_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = withBit(oldValue, 3, 1);
        registers.setA(newValue);

        return 8;
    }

    /**
     * The SET 4,A instruction
     */
    byte SET_4_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = withBit(oldValue, 4, 1);
        registers.setA(newValue);

        return 8;
    }

    /**
     * The SET 5,A instruction
     */
    byte SET_5_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = withBit(oldValue, 5, 1);
        registers.setA(newValue);

        return 8;
    }

    /**
     * The SET 6,A instruction
     */
    byte SET_6_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = withBit(oldValue, 6, 1);
        registers.setA(newValue);

        return 8;
    }

    /**
     * The SET 7,A instruction
     */
    byte SET_7_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = withBit(oldValue, 7, 1);
        registers.setA(newValue);

        return 8;
    }

    /**
     * The RES 0,A instruction
     */
    byte RES_0_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = withBit(oldValue, 0, 0);
        registers.setA(newValue);

        return 8;
    }

    /**
     * The RES 1,A instruction
     */
    byte RES_1_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = withBit(oldValue, 1, 0);
        registers.setA(newValue);

        return 8;
    }

    /**
     * The RES 2,A instruction
     */
    byte RES_2_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = withBit(oldValue, 2, 0);
        registers.setA(newValue);

        return 8;
    }

    /**
     * The RES 3,A instruction
     */
    byte RES_3_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = withBit(oldValue, 3, 0);
        registers.setA(newValue);

        return 8;
    }

    /**
     * The RES 4,A instruction
     */
    byte RES_4_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = withBit(oldValue, 4, 0);
        registers.setA(newValue);

        return 8;
    }

    /**
     * The RES 5,A instruction
     */
    byte RES_5_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = withBit(oldValue, 5, 0);
        registers.setA(newValue);

        return 8;
    }

    /**
     * The RES 6,A instruction
     */
    byte RES_6_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = withBit(oldValue, 6, 0);
        registers.setA(newValue);

        return 8;
    }

    /**
     * The RES 7,A instruction
     */
    byte RES_7_A() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getA();
        var newValue = withBit(oldValue, 7, 0);
        registers.setA(newValue);

        return 8;
    }

    /**
     * The SET 0,B instruction
     */
    byte SET_0_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = withBit(oldValue, 0, 1);
        registers.setB(newValue);

        return 8;
    }

    /**
     * The SET 1,B instruction
     */
    byte SET_1_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = withBit(oldValue, 1, 1);
        registers.setB(newValue);

        return 8;
    }

    /**
     * The SET 2,B instruction
     */
    byte SET_2_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = withBit(oldValue, 2, 1);
        registers.setB(newValue);

        return 8;
    }

    /**
     * The SET 3,B instruction
     */
    byte SET_3_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = withBit(oldValue, 3, 1);
        registers.setB(newValue);

        return 8;
    }

    /**
     * The SET 4,B instruction
     */
    byte SET_4_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = withBit(oldValue, 4, 1);
        registers.setB(newValue);

        return 8;
    }

    /**
     * The SET 5,B instruction
     */
    byte SET_5_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = withBit(oldValue, 5, 1);
        registers.setB(newValue);

        return 8;
    }

    /**
     * The SET 6,B instruction
     */
    byte SET_6_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = withBit(oldValue, 6, 1);
        registers.setB(newValue);

        return 8;
    }

    /**
     * The SET 7,B instruction
     */
    byte SET_7_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = withBit(oldValue, 7, 1);
        registers.setB(newValue);

        return 8;
    }

    /**
     * The RES 0,B instruction
     */
    byte RES_0_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = withBit(oldValue, 0, 0);
        registers.setB(newValue);

        return 8;
    }

    /**
     * The RES 1,B instruction
     */
    byte RES_1_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = withBit(oldValue, 1, 0);
        registers.setB(newValue);

        return 8;
    }

    /**
     * The RES 2,B instruction
     */
    byte RES_2_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = withBit(oldValue, 2, 0);
        registers.setB(newValue);

        return 8;
    }

    /**
     * The RES 3,B instruction
     */
    byte RES_3_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = withBit(oldValue, 3, 0);
        registers.setB(newValue);

        return 8;
    }

    /**
     * The RES 4,B instruction
     */
    byte RES_4_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = withBit(oldValue, 4, 0);
        registers.setB(newValue);

        return 8;
    }

    /**
     * The RES 5,B instruction
     */
    byte RES_5_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = withBit(oldValue, 5, 0);
        registers.setB(newValue);

        return 8;
    }

    /**
     * The RES 6,B instruction
     */
    byte RES_6_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = withBit(oldValue, 6, 0);
        registers.setB(newValue);

        return 8;
    }

    /**
     * The RES 7,B instruction
     */
    byte RES_7_B() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getB();
        var newValue = withBit(oldValue, 7, 0);
        registers.setB(newValue);

        return 8;
    }

    /**
     * The SET 0,C instruction
     */
    byte SET_0_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = withBit(oldValue, 0, 1);
        registers.setC(newValue);

        return 8;
    }

    /**
     * The SET 1,C instruction
     */
    byte SET_1_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = withBit(oldValue, 1, 1);
        registers.setC(newValue);

        return 8;
    }

    /**
     * The SET 2,C instruction
     */
    byte SET_2_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = withBit(oldValue, 2, 1);
        registers.setC(newValue);

        return 8;
    }

    /**
     * The SET 3,C instruction
     */
    byte SET_3_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = withBit(oldValue, 3, 1);
        registers.setC(newValue);

        return 8;
    }

    /**
     * The SET 4,C instruction
     */
    byte SET_4_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = withBit(oldValue, 4, 1);
        registers.setC(newValue);

        return 8;
    }

    /**
     * The SET 5,C instruction
     */
    byte SET_5_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = withBit(oldValue, 5, 1);
        registers.setC(newValue);

        return 8;
    }

    /**
     * The SET 6,C instruction
     */
    byte SET_6_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = withBit(oldValue, 6, 1);
        registers.setC(newValue);

        return 8;
    }

    /**
     * The SET 7,C instruction
     */
    byte SET_7_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = withBit(oldValue, 7, 1);
        registers.setC(newValue);

        return 8;
    }

    /**
     * The RES 0,C instruction
     */
    byte RES_0_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = withBit(oldValue, 0, 0);
        registers.setC(newValue);

        return 8;
    }

    /**
     * The RES 1,C instruction
     */
    byte RES_1_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = withBit(oldValue, 1, 0);
        registers.setC(newValue);

        return 8;
    }

    /**
     * The RES 2,C instruction
     */
    byte RES_2_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = withBit(oldValue, 2, 0);
        registers.setC(newValue);

        return 8;
    }

    /**
     * The RES 3,C instruction
     */
    byte RES_3_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = withBit(oldValue, 3, 0);
        registers.setC(newValue);

        return 8;
    }

    /**
     * The RES 4,C instruction
     */
    byte RES_4_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = withBit(oldValue, 4, 0);
        registers.setC(newValue);

        return 8;
    }

    /**
     * The RES 5,C instruction
     */
    byte RES_5_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = withBit(oldValue, 5, 0);
        registers.setC(newValue);

        return 8;
    }

    /**
     * The RES 6,C instruction
     */
    byte RES_6_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = withBit(oldValue, 6, 0);
        registers.setC(newValue);

        return 8;
    }

    /**
     * The RES 7,C instruction
     */
    byte RES_7_C() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getC();
        var newValue = withBit(oldValue, 7, 0);
        registers.setC(newValue);

        return 8;
    }

    /**
     * The SET 0,D instruction
     */
    byte SET_0_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = withBit(oldValue, 0, 1);
        registers.setD(newValue);

        return 8;
    }

    /**
     * The SET 1,D instruction
     */
    byte SET_1_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = withBit(oldValue, 1, 1);
        registers.setD(newValue);

        return 8;
    }

    /**
     * The SET 2,D instruction
     */
    byte SET_2_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = withBit(oldValue, 2, 1);
        registers.setD(newValue);

        return 8;
    }

    /**
     * The SET 3,D instruction
     */
    byte SET_3_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = withBit(oldValue, 3, 1);
        registers.setD(newValue);

        return 8;
    }

    /**
     * The SET 4,D instruction
     */
    byte SET_4_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = withBit(oldValue, 4, 1);
        registers.setD(newValue);

        return 8;
    }

    /**
     * The SET 5,D instruction
     */
    byte SET_5_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = withBit(oldValue, 5, 1);
        registers.setD(newValue);

        return 8;
    }

    /**
     * The SET 6,D instruction
     */
    byte SET_6_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = withBit(oldValue, 6, 1);
        registers.setD(newValue);

        return 8;
    }

    /**
     * The SET 7,D instruction
     */
    byte SET_7_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = withBit(oldValue, 7, 1);
        registers.setD(newValue);

        return 8;
    }

    /**
     * The RES 0,D instruction
     */
    byte RES_0_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = withBit(oldValue, 0, 0);
        registers.setD(newValue);

        return 8;
    }

    /**
     * The RES 1,D instruction
     */
    byte RES_1_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = withBit(oldValue, 1, 0);
        registers.setD(newValue);

        return 8;
    }

    /**
     * The RES 2,D instruction
     */
    byte RES_2_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = withBit(oldValue, 2, 0);
        registers.setD(newValue);

        return 8;
    }

    /**
     * The RES 3,D instruction
     */
    byte RES_3_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = withBit(oldValue, 3, 0);
        registers.setD(newValue);

        return 8;
    }

    /**
     * The RES 4,D instruction
     */
    byte RES_4_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = withBit(oldValue, 4, 0);
        registers.setD(newValue);

        return 8;
    }

    /**
     * The RES 5,D instruction
     */
    byte RES_5_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = withBit(oldValue, 5, 0);
        registers.setD(newValue);

        return 8;
    }

    /**
     * The RES 6,D instruction
     */
    byte RES_6_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = withBit(oldValue, 6, 0);
        registers.setD(newValue);

        return 8;
    }

    /**
     * The RES 7,D instruction
     */
    byte RES_7_D() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getD();
        var newValue = withBit(oldValue, 7, 0);
        registers.setD(newValue);

        return 8;
    }

    /**
     * The SET 0,E instruction
     */
    byte SET_0_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = withBit(oldValue, 0, 1);
        registers.setE(newValue);

        return 8;
    }

    /**
     * The SET 1,E instruction
     */
    byte SET_1_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = withBit(oldValue, 1, 1);
        registers.setE(newValue);

        return 8;
    }

    /**
     * The SET 2,E instruction
     */
    byte SET_2_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = withBit(oldValue, 2, 1);
        registers.setE(newValue);

        return 8;
    }

    /**
     * The SET 3,E instruction
     */
    byte SET_3_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = withBit(oldValue, 3, 1);
        registers.setE(newValue);

        return 8;
    }

    /**
     * The SET 4,E instruction
     */
    byte SET_4_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = withBit(oldValue, 4, 1);
        registers.setE(newValue);

        return 8;
    }

    /**
     * The SET 5,E instruction
     */
    byte SET_5_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = withBit(oldValue, 5, 1);
        registers.setE(newValue);

        return 8;
    }

    /**
     * The SET 6,E instruction
     */
    byte SET_6_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = withBit(oldValue, 6, 1);
        registers.setE(newValue);

        return 8;
    }

    /**
     * The SET 7,E instruction
     */
    byte SET_7_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = withBit(oldValue, 7, 1);
        registers.setE(newValue);

        return 8;
    }

    /**
     * The RES 0,E instruction
     */
    byte RES_0_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = withBit(oldValue, 0, 0);
        registers.setE(newValue);

        return 8;
    }

    /**
     * The RES 1,E instruction
     */
    byte RES_1_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = withBit(oldValue, 1, 0);
        registers.setE(newValue);

        return 8;
    }

    /**
     * The RES 2,E instruction
     */
    byte RES_2_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = withBit(oldValue, 2, 0);
        registers.setE(newValue);

        return 8;
    }

    /**
     * The RES 3,E instruction
     */
    byte RES_3_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = withBit(oldValue, 3, 0);
        registers.setE(newValue);

        return 8;
    }

    /**
     * The RES 4,E instruction
     */
    byte RES_4_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = withBit(oldValue, 4, 0);
        registers.setE(newValue);

        return 8;
    }

    /**
     * The RES 5,E instruction
     */
    byte RES_5_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = withBit(oldValue, 5, 0);
        registers.setE(newValue);

        return 8;
    }

    /**
     * The RES 6,E instruction
     */
    byte RES_6_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = withBit(oldValue, 6, 0);
        registers.setE(newValue);

        return 8;
    }

    /**
     * The RES 7,E instruction
     */
    byte RES_7_E() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getE();
        var newValue = withBit(oldValue, 7, 0);
        registers.setE(newValue);

        return 8;
    }

    /**
     * The SET 0,H instruction
     */
    byte SET_0_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = withBit(oldValue, 0, 1);
        registers.setH(newValue);

        return 8;
    }

    /**
     * The SET 1,H instruction
     */
    byte SET_1_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = withBit(oldValue, 1, 1);
        registers.setH(newValue);

        return 8;
    }

    /**
     * The SET 2,H instruction
     */
    byte SET_2_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = withBit(oldValue, 2, 1);
        registers.setH(newValue);

        return 8;
    }

    /**
     * The SET 3,H instruction
     */
    byte SET_3_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = withBit(oldValue, 3, 1);
        registers.setH(newValue);

        return 8;
    }

    /**
     * The SET 4,H instruction
     */
    byte SET_4_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = withBit(oldValue, 4, 1);
        registers.setH(newValue);

        return 8;
    }

    /**
     * The SET 5,H instruction
     */
    byte SET_5_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = withBit(oldValue, 5, 1);
        registers.setH(newValue);

        return 8;
    }

    /**
     * The SET 6,H instruction
     */
    byte SET_6_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = withBit(oldValue, 6, 1);
        registers.setH(newValue);

        return 8;
    }

    /**
     * The SET 7,H instruction
     */
    byte SET_7_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = withBit(oldValue, 7, 1);
        registers.setH(newValue);

        return 8;
    }

    /**
     * The RES 0,H instruction
     */
    byte RES_0_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = withBit(oldValue, 0, 0);
        registers.setH(newValue);

        return 8;
    }

    /**
     * The RES 1,H instruction
     */
    byte RES_1_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = withBit(oldValue, 1, 0);
        registers.setH(newValue);

        return 8;
    }

    /**
     * The RES 2,H instruction
     */
    byte RES_2_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = withBit(oldValue, 2, 0);
        registers.setH(newValue);

        return 8;
    }

    /**
     * The RES 3,H instruction
     */
    byte RES_3_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = withBit(oldValue, 3, 0);
        registers.setH(newValue);

        return 8;
    }

    /**
     * The RES 4,H instruction
     */
    byte RES_4_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = withBit(oldValue, 4, 0);
        registers.setH(newValue);

        return 8;
    }

    /**
     * The RES 5,H instruction
     */
    byte RES_5_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = withBit(oldValue, 5, 0);
        registers.setH(newValue);

        return 8;
    }

    /**
     * The RES 6,H instruction
     */
    byte RES_6_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = withBit(oldValue, 6, 0);
        registers.setH(newValue);

        return 8;
    }

    /**
     * The RES 7,H instruction
     */
    byte RES_7_H() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getH();
        var newValue = withBit(oldValue, 7, 0);
        registers.setH(newValue);

        return 8;
    }

    /**
     * The SET 0,L instruction
     */
    byte SET_0_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = withBit(oldValue, 0, 1);
        registers.setL(newValue);

        return 8;
    }

    /**
     * The SET 1,L instruction
     */
    byte SET_1_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = withBit(oldValue, 1, 1);
        registers.setL(newValue);

        return 8;
    }

    /**
     * The SET 2,L instruction
     */
    byte SET_2_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = withBit(oldValue, 2, 1);
        registers.setL(newValue);

        return 8;
    }

    /**
     * The SET 3,L instruction
     */
    byte SET_3_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = withBit(oldValue, 3, 1);
        registers.setL(newValue);

        return 8;
    }

    /**
     * The SET 4,L instruction
     */
    byte SET_4_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = withBit(oldValue, 4, 1);
        registers.setL(newValue);

        return 8;
    }

    /**
     * The SET 5,L instruction
     */
    byte SET_5_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = withBit(oldValue, 5, 1);
        registers.setL(newValue);

        return 8;
    }

    /**
     * The SET 6,L instruction
     */
    byte SET_6_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = withBit(oldValue, 6, 1);
        registers.setL(newValue);

        return 8;
    }

    /**
     * The SET 7,L instruction
     */
    byte SET_7_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = withBit(oldValue, 7, 1);
        registers.setL(newValue);

        return 8;
    }

    /**
     * The RES 0,L instruction
     */
    byte RES_0_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = withBit(oldValue, 0, 0);
        registers.setL(newValue);

        return 8;
    }

    /**
     * The RES 1,L instruction
     */
    byte RES_1_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = withBit(oldValue, 1, 0);
        registers.setL(newValue);

        return 8;
    }

    /**
     * The RES 2,L instruction
     */
    byte RES_2_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = withBit(oldValue, 2, 0);
        registers.setL(newValue);

        return 8;
    }

    /**
     * The RES 3,L instruction
     */
    byte RES_3_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = withBit(oldValue, 3, 0);
        registers.setL(newValue);

        return 8;
    }

    /**
     * The RES 4,L instruction
     */
    byte RES_4_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = withBit(oldValue, 4, 0);
        registers.setL(newValue);

        return 8;
    }

    /**
     * The RES 5,L instruction
     */
    byte RES_5_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = withBit(oldValue, 5, 0);
        registers.setL(newValue);

        return 8;
    }

    /**
     * The RES 6,L instruction
     */
    byte RES_6_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = withBit(oldValue, 6, 0);
        registers.setL(newValue);

        return 8;
    }

    /**
     * The RES 7,L instruction
     */
    byte RES_7_L() {
        fetchFinished(false, false, false, false);

        var oldValue = registers.getL();
        var newValue = withBit(oldValue, 7, 0);
        registers.setL(newValue);

        return 8;
    }

    /**
     * The SET 0,(HL) instruction
     */
    byte SET_0_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 1);
        processorAgent.writeToMemory(address, newValue);

        return 15;
    }

    /**
     * The SET 1,(HL) instruction
     */
    byte SET_1_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 1);
        processorAgent.writeToMemory(address, newValue);

        return 15;
    }

    /**
     * The SET 2,(HL) instruction
     */
    byte SET_2_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 1);
        processorAgent.writeToMemory(address, newValue);

        return 15;
    }

    /**
     * The SET 3,(HL) instruction
     */
    byte SET_3_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 1);
        processorAgent.writeToMemory(address, newValue);

        return 15;
    }

    /**
     * The SET 4,(HL) instruction
     */
    byte SET_4_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 1);
        processorAgent.writeToMemory(address, newValue);

        return 15;
    }

    /**
     * The SET 5,(HL) instruction
     */
    byte SET_5_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 1);
        processorAgent.writeToMemory(address, newValue);

        return 15;
    }

    /**
     * The SET 6,(HL) instruction
     */
    byte SET_6_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 1);
        processorAgent.writeToMemory(address, newValue);

        return 15;
    }

    /**
     * The SET 7,(HL) instruction
     */
    byte SET_7_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 1);
        processorAgent.writeToMemory(address, newValue);

        return 15;
    }

    /**
     * The RES 0,(HL) instruction
     */
    byte RES_0_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 0);
        processorAgent.writeToMemory(address, newValue);

        return 15;
    }

    /**
     * The RES 1,(HL) instruction
     */
    byte RES_1_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 0);
        processorAgent.writeToMemory(address, newValue);

        return 15;
    }

    /**
     * The RES 2,(HL) instruction
     */
    byte RES_2_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 0);
        processorAgent.writeToMemory(address, newValue);

        return 15;
    }

    /**
     * The RES 3,(HL) instruction
     */
    byte RES_3_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 0);
        processorAgent.writeToMemory(address, newValue);

        return 15;
    }

    /**
     * The RES 4,(HL) instruction
     */
    byte RES_4_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 0);
        processorAgent.writeToMemory(address, newValue);

        return 15;
    }

    /**
     * The RES 5,(HL) instruction
     */
    byte RES_5_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 0);
        processorAgent.writeToMemory(address, newValue);

        return 15;
    }

    /**
     * The RES 6,(HL) instruction
     */
    byte RES_6_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 0);
        processorAgent.writeToMemory(address, newValue);

        return 15;
    }

    /**
     * The RES 7,(HL) instruction
     */
    byte RES_7_aHL() {
        fetchFinished(false, false, false, false);

        var address = registers.getHL();
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 0);
        processorAgent.writeToMemory(address, newValue);

        return 15;
    }

    /**
     * The SET 0,(IX+n),A instruction
     */
    byte SET_0_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The SET 1,(IX+n),A instruction
     */
    byte SET_1_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The SET 2,(IX+n),A instruction
     */
    byte SET_2_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The SET 3,(IX+n),A instruction
     */
    byte SET_3_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The SET 4,(IX+n),A instruction
     */
    byte SET_4_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The SET 5,(IX+n),A instruction
     */
    byte SET_5_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The SET 6,(IX+n),A instruction
     */
    byte SET_6_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The SET 7,(IX+n),A instruction
     */
    byte SET_7_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The SET 0,(IX+n),B instruction
     */
    byte SET_0_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The SET 1,(IX+n),B instruction
     */
    byte SET_1_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The SET 2,(IX+n),B instruction
     */
    byte SET_2_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The SET 3,(IX+n),B instruction
     */
    byte SET_3_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The SET 4,(IX+n),B instruction
     */
    byte SET_4_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The SET 5,(IX+n),B instruction
     */
    byte SET_5_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The SET 6,(IX+n),B instruction
     */
    byte SET_6_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The SET 7,(IX+n),B instruction
     */
    byte SET_7_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The SET 0,(IX+n),C instruction
     */
    byte SET_0_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The SET 1,(IX+n),C instruction
     */
    byte SET_1_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The SET 2,(IX+n),C instruction
     */
    byte SET_2_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The SET 3,(IX+n),C instruction
     */
    byte SET_3_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The SET 4,(IX+n),C instruction
     */
    byte SET_4_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The SET 5,(IX+n),C instruction
     */
    byte SET_5_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The SET 6,(IX+n),C instruction
     */
    byte SET_6_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The SET 7,(IX+n),C instruction
     */
    byte SET_7_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The SET 0,(IX+n),D instruction
     */
    byte SET_0_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The SET 1,(IX+n),D instruction
     */
    byte SET_1_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The SET 2,(IX+n),D instruction
     */
    byte SET_2_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The SET 3,(IX+n),D instruction
     */
    byte SET_3_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The SET 4,(IX+n),D instruction
     */
    byte SET_4_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The SET 5,(IX+n),D instruction
     */
    byte SET_5_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The SET 6,(IX+n),D instruction
     */
    byte SET_6_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The SET 7,(IX+n),D instruction
     */
    byte SET_7_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The SET 0,(IX+n),E instruction
     */
    byte SET_0_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The SET 1,(IX+n),E instruction
     */
    byte SET_1_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The SET 2,(IX+n),E instruction
     */
    byte SET_2_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The SET 3,(IX+n),E instruction
     */
    byte SET_3_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The SET 4,(IX+n),E instruction
     */
    byte SET_4_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The SET 5,(IX+n),E instruction
     */
    byte SET_5_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The SET 6,(IX+n),E instruction
     */
    byte SET_6_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The SET 7,(IX+n),E instruction
     */
    byte SET_7_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The SET 0,(IX+n),H instruction
     */
    byte SET_0_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The SET 1,(IX+n),H instruction
     */
    byte SET_1_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The SET 2,(IX+n),H instruction
     */
    byte SET_2_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The SET 3,(IX+n),H instruction
     */
    byte SET_3_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The SET 4,(IX+n),H instruction
     */
    byte SET_4_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The SET 5,(IX+n),H instruction
     */
    byte SET_5_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The SET 6,(IX+n),H instruction
     */
    byte SET_6_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The SET 7,(IX+n),H instruction
     */
    byte SET_7_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The SET 0,(IX+n),L instruction
     */
    byte SET_0_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The SET 1,(IX+n),L instruction
     */
    byte SET_1_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The SET 2,(IX+n),L instruction
     */
    byte SET_2_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The SET 3,(IX+n),L instruction
     */
    byte SET_3_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The SET 4,(IX+n),L instruction
     */
    byte SET_4_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The SET 5,(IX+n),L instruction
     */
    byte SET_5_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The SET 6,(IX+n),L instruction
     */
    byte SET_6_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The SET 7,(IX+n),L instruction
     */
    byte SET_7_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The SET 0,(IX+n) instruction
     */
    byte SET_0_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 1);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The SET 1,(IX+n) instruction
     */
    byte SET_1_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 1);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The SET 2,(IX+n) instruction
     */
    byte SET_2_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 1);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The SET 3,(IX+n) instruction
     */
    byte SET_3_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 1);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The SET 4,(IX+n) instruction
     */
    byte SET_4_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 1);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The SET 5,(IX+n) instruction
     */
    byte SET_5_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 1);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The SET 6,(IX+n) instruction
     */
    byte SET_6_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 1);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The SET 7,(IX+n) instruction
     */
    byte SET_7_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 1);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The RES 0,(IX+n),A instruction
     */
    byte RES_0_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The RES 1,(IX+n),A instruction
     */
    byte RES_1_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The RES 2,(IX+n),A instruction
     */
    byte RES_2_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The RES 3,(IX+n),A instruction
     */
    byte RES_3_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The RES 4,(IX+n),A instruction
     */
    byte RES_4_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The RES 5,(IX+n),A instruction
     */
    byte RES_5_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The RES 6,(IX+n),A instruction
     */
    byte RES_6_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The RES 7,(IX+n),A instruction
     */
    byte RES_7_aIX_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The RES 0,(IX+n),B instruction
     */
    byte RES_0_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The RES 1,(IX+n),B instruction
     */
    byte RES_1_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The RES 2,(IX+n),B instruction
     */
    byte RES_2_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The RES 3,(IX+n),B instruction
     */
    byte RES_3_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The RES 4,(IX+n),B instruction
     */
    byte RES_4_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The RES 5,(IX+n),B instruction
     */
    byte RES_5_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The RES 6,(IX+n),B instruction
     */
    byte RES_6_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The RES 7,(IX+n),B instruction
     */
    byte RES_7_aIX_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The RES 0,(IX+n),C instruction
     */
    byte RES_0_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The RES 1,(IX+n),C instruction
     */
    byte RES_1_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The RES 2,(IX+n),C instruction
     */
    byte RES_2_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The RES 3,(IX+n),C instruction
     */
    byte RES_3_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The RES 4,(IX+n),C instruction
     */
    byte RES_4_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The RES 5,(IX+n),C instruction
     */
    byte RES_5_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The RES 6,(IX+n),C instruction
     */
    byte RES_6_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The RES 7,(IX+n),C instruction
     */
    byte RES_7_aIX_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The RES 0,(IX+n),D instruction
     */
    byte RES_0_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The RES 1,(IX+n),D instruction
     */
    byte RES_1_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The RES 2,(IX+n),D instruction
     */
    byte RES_2_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The RES 3,(IX+n),D instruction
     */
    byte RES_3_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The RES 4,(IX+n),D instruction
     */
    byte RES_4_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The RES 5,(IX+n),D instruction
     */
    byte RES_5_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The RES 6,(IX+n),D instruction
     */
    byte RES_6_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The RES 7,(IX+n),D instruction
     */
    byte RES_7_aIX_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The RES 0,(IX+n),E instruction
     */
    byte RES_0_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The RES 1,(IX+n),E instruction
     */
    byte RES_1_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The RES 2,(IX+n),E instruction
     */
    byte RES_2_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The RES 3,(IX+n),E instruction
     */
    byte RES_3_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The RES 4,(IX+n),E instruction
     */
    byte RES_4_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The RES 5,(IX+n),E instruction
     */
    byte RES_5_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The RES 6,(IX+n),E instruction
     */
    byte RES_6_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The RES 7,(IX+n),E instruction
     */
    byte RES_7_aIX_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The RES 0,(IX+n),H instruction
     */
    byte RES_0_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The RES 1,(IX+n),H instruction
     */
    byte RES_1_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The RES 2,(IX+n),H instruction
     */
    byte RES_2_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The RES 3,(IX+n),H instruction
     */
    byte RES_3_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The RES 4,(IX+n),H instruction
     */
    byte RES_4_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The RES 5,(IX+n),H instruction
     */
    byte RES_5_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The RES 6,(IX+n),H instruction
     */
    byte RES_6_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The RES 7,(IX+n),H instruction
     */
    byte RES_7_aIX_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The RES 0,(IX+n),L instruction
     */
    byte RES_0_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The RES 1,(IX+n),L instruction
     */
    byte RES_1_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The RES 2,(IX+n),L instruction
     */
    byte RES_2_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The RES 3,(IX+n),L instruction
     */
    byte RES_3_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The RES 4,(IX+n),L instruction
     */
    byte RES_4_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The RES 5,(IX+n),L instruction
     */
    byte RES_5_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The RES 6,(IX+n),L instruction
     */
    byte RES_6_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The RES 7,(IX+n),L instruction
     */
    byte RES_7_aIX_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The RES 0,(IX+n) instruction
     */
    byte RES_0_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 0);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The RES 1,(IX+n) instruction
     */
    byte RES_1_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 0);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The RES 2,(IX+n) instruction
     */
    byte RES_2_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 0);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The RES 3,(IX+n) instruction
     */
    byte RES_3_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 0);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The RES 4,(IX+n) instruction
     */
    byte RES_4_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 0);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The RES 5,(IX+n) instruction
     */
    byte RES_5_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 0);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The RES 6,(IX+n) instruction
     */
    byte RES_6_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 0);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The RES 7,(IX+n) instruction
     */
    byte RES_7_aIX_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIX(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 0);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The SET 0,(IY+n),A instruction
     */
    byte SET_0_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The SET 1,(IY+n),A instruction
     */
    byte SET_1_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The SET 2,(IY+n),A instruction
     */
    byte SET_2_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The SET 3,(IY+n),A instruction
     */
    byte SET_3_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The SET 4,(IY+n),A instruction
     */
    byte SET_4_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The SET 5,(IY+n),A instruction
     */
    byte SET_5_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The SET 6,(IY+n),A instruction
     */
    byte SET_6_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The SET 7,(IY+n),A instruction
     */
    byte SET_7_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The SET 0,(IY+n),B instruction
     */
    byte SET_0_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The SET 1,(IY+n),B instruction
     */
    byte SET_1_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The SET 2,(IY+n),B instruction
     */
    byte SET_2_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The SET 3,(IY+n),B instruction
     */
    byte SET_3_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The SET 4,(IY+n),B instruction
     */
    byte SET_4_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The SET 5,(IY+n),B instruction
     */
    byte SET_5_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The SET 6,(IY+n),B instruction
     */
    byte SET_6_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The SET 7,(IY+n),B instruction
     */
    byte SET_7_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The SET 0,(IY+n),C instruction
     */
    byte SET_0_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The SET 1,(IY+n),C instruction
     */
    byte SET_1_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The SET 2,(IY+n),C instruction
     */
    byte SET_2_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The SET 3,(IY+n),C instruction
     */
    byte SET_3_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The SET 4,(IY+n),C instruction
     */
    byte SET_4_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The SET 5,(IY+n),C instruction
     */
    byte SET_5_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The SET 6,(IY+n),C instruction
     */
    byte SET_6_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The SET 7,(IY+n),C instruction
     */
    byte SET_7_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The SET 0,(IY+n),D instruction
     */
    byte SET_0_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The SET 1,(IY+n),D instruction
     */
    byte SET_1_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The SET 2,(IY+n),D instruction
     */
    byte SET_2_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The SET 3,(IY+n),D instruction
     */
    byte SET_3_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The SET 4,(IY+n),D instruction
     */
    byte SET_4_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The SET 5,(IY+n),D instruction
     */
    byte SET_5_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The SET 6,(IY+n),D instruction
     */
    byte SET_6_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The SET 7,(IY+n),D instruction
     */
    byte SET_7_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The SET 0,(IY+n),E instruction
     */
    byte SET_0_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The SET 1,(IY+n),E instruction
     */
    byte SET_1_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The SET 2,(IY+n),E instruction
     */
    byte SET_2_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The SET 3,(IY+n),E instruction
     */
    byte SET_3_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The SET 4,(IY+n),E instruction
     */
    byte SET_4_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The SET 5,(IY+n),E instruction
     */
    byte SET_5_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The SET 6,(IY+n),E instruction
     */
    byte SET_6_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The SET 7,(IY+n),E instruction
     */
    byte SET_7_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The SET 0,(IY+n),H instruction
     */
    byte SET_0_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The SET 1,(IY+n),H instruction
     */
    byte SET_1_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The SET 2,(IY+n),H instruction
     */
    byte SET_2_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The SET 3,(IY+n),H instruction
     */
    byte SET_3_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The SET 4,(IY+n),H instruction
     */
    byte SET_4_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The SET 5,(IY+n),H instruction
     */
    byte SET_5_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The SET 6,(IY+n),H instruction
     */
    byte SET_6_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The SET 7,(IY+n),H instruction
     */
    byte SET_7_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The SET 0,(IY+n),L instruction
     */
    byte SET_0_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The SET 1,(IY+n),L instruction
     */
    byte SET_1_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The SET 2,(IY+n),L instruction
     */
    byte SET_2_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The SET 3,(IY+n),L instruction
     */
    byte SET_3_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The SET 4,(IY+n),L instruction
     */
    byte SET_4_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The SET 5,(IY+n),L instruction
     */
    byte SET_5_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The SET 6,(IY+n),L instruction
     */
    byte SET_6_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The SET 7,(IY+n),L instruction
     */
    byte SET_7_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 1);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The SET 0,(IY+n) instruction
     */
    byte SET_0_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 1);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The SET 1,(IY+n) instruction
     */
    byte SET_1_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 1);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The SET 2,(IY+n) instruction
     */
    byte SET_2_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 1);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The SET 3,(IY+n) instruction
     */
    byte SET_3_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 1);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The SET 4,(IY+n) instruction
     */
    byte SET_4_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 1);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The SET 5,(IY+n) instruction
     */
    byte SET_5_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 1);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The SET 6,(IY+n) instruction
     */
    byte SET_6_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 1);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The SET 7,(IY+n) instruction
     */
    byte SET_7_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 1);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The RES 0,(IY+n),A instruction
     */
    byte RES_0_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The RES 1,(IY+n),A instruction
     */
    byte RES_1_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The RES 2,(IY+n),A instruction
     */
    byte RES_2_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The RES 3,(IY+n),A instruction
     */
    byte RES_3_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The RES 4,(IY+n),A instruction
     */
    byte RES_4_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The RES 5,(IY+n),A instruction
     */
    byte RES_5_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The RES 6,(IY+n),A instruction
     */
    byte RES_6_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The RES 7,(IY+n),A instruction
     */
    byte RES_7_aIY_plus_n_and_load_A(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setA(newValue);

        return 23;
    }

    /**
     * The RES 0,(IY+n),B instruction
     */
    byte RES_0_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The RES 1,(IY+n),B instruction
     */
    byte RES_1_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The RES 2,(IY+n),B instruction
     */
    byte RES_2_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The RES 3,(IY+n),B instruction
     */
    byte RES_3_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The RES 4,(IY+n),B instruction
     */
    byte RES_4_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The RES 5,(IY+n),B instruction
     */
    byte RES_5_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The RES 6,(IY+n),B instruction
     */
    byte RES_6_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The RES 7,(IY+n),B instruction
     */
    byte RES_7_aIY_plus_n_and_load_B(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setB(newValue);

        return 23;
    }

    /**
     * The RES 0,(IY+n),C instruction
     */
    byte RES_0_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The RES 1,(IY+n),C instruction
     */
    byte RES_1_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The RES 2,(IY+n),C instruction
     */
    byte RES_2_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The RES 3,(IY+n),C instruction
     */
    byte RES_3_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The RES 4,(IY+n),C instruction
     */
    byte RES_4_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The RES 5,(IY+n),C instruction
     */
    byte RES_5_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The RES 6,(IY+n),C instruction
     */
    byte RES_6_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The RES 7,(IY+n),C instruction
     */
    byte RES_7_aIY_plus_n_and_load_C(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setC(newValue);

        return 23;
    }

    /**
     * The RES 0,(IY+n),D instruction
     */
    byte RES_0_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The RES 1,(IY+n),D instruction
     */
    byte RES_1_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The RES 2,(IY+n),D instruction
     */
    byte RES_2_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The RES 3,(IY+n),D instruction
     */
    byte RES_3_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The RES 4,(IY+n),D instruction
     */
    byte RES_4_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The RES 5,(IY+n),D instruction
     */
    byte RES_5_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The RES 6,(IY+n),D instruction
     */
    byte RES_6_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The RES 7,(IY+n),D instruction
     */
    byte RES_7_aIY_plus_n_and_load_D(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setD(newValue);

        return 23;
    }

    /**
     * The RES 0,(IY+n),E instruction
     */
    byte RES_0_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The RES 1,(IY+n),E instruction
     */
    byte RES_1_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The RES 2,(IY+n),E instruction
     */
    byte RES_2_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The RES 3,(IY+n),E instruction
     */
    byte RES_3_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The RES 4,(IY+n),E instruction
     */
    byte RES_4_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The RES 5,(IY+n),E instruction
     */
    byte RES_5_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The RES 6,(IY+n),E instruction
     */
    byte RES_6_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The RES 7,(IY+n),E instruction
     */
    byte RES_7_aIY_plus_n_and_load_E(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setE(newValue);

        return 23;
    }

    /**
     * The RES 0,(IY+n),H instruction
     */
    byte RES_0_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The RES 1,(IY+n),H instruction
     */
    byte RES_1_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The RES 2,(IY+n),H instruction
     */
    byte RES_2_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The RES 3,(IY+n),H instruction
     */
    byte RES_3_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The RES 4,(IY+n),H instruction
     */
    byte RES_4_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The RES 5,(IY+n),H instruction
     */
    byte RES_5_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The RES 6,(IY+n),H instruction
     */
    byte RES_6_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The RES 7,(IY+n),H instruction
     */
    byte RES_7_aIY_plus_n_and_load_H(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setH(newValue);

        return 23;
    }

    /**
     * The RES 0,(IY+n),L instruction
     */
    byte RES_0_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The RES 1,(IY+n),L instruction
     */
    byte RES_1_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The RES 2,(IY+n),L instruction
     */
    byte RES_2_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The RES 3,(IY+n),L instruction
     */
    byte RES_3_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The RES 4,(IY+n),L instruction
     */
    byte RES_4_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The RES 5,(IY+n),L instruction
     */
    byte RES_5_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The RES 6,(IY+n),L instruction
     */
    byte RES_6_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The RES 7,(IY+n),L instruction
     */
    byte RES_7_aIY_plus_n_and_load_L(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 0);
        processorAgent.writeToMemory(address, newValue);
        registers.setL(newValue);

        return 23;
    }

    /**
     * The RES 0,(IY+n) instruction
     */
    byte RES_0_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 0, 0);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The RES 1,(IY+n) instruction
     */
    byte RES_1_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 1, 0);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The RES 2,(IY+n) instruction
     */
    byte RES_2_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 2, 0);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The RES 3,(IY+n) instruction
     */
    byte RES_3_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 3, 0);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The RES 4,(IY+n) instruction
     */
    byte RES_4_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 4, 0);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The RES 5,(IY+n) instruction
     */
    byte RES_5_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 5, 0);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The RES 6,(IY+n) instruction
     */
    byte RES_6_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 6, 0);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

    /**
     * The RES 7,(IY+n) instruction
     */
    byte RES_7_aIY_plus_n(byte offset) {
        fetchFinished(false, false, false, false);

        var address = add(registers.getIY(), offset);
        var oldValue = processorAgent.readFromMemory(address);
        var newValue = withBit(oldValue, 7, 0);
        processorAgent.writeToMemory(address, newValue);

        return 23;
    }

//#endregion
}
