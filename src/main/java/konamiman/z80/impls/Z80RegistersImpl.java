package konamiman.z80.impls;

import konamiman.z80.utils.Bit;
import konamiman.z80.interfaces.MainZ80Registers;

import static konamiman.z80.utils.NumberUtils.getHighByte;
import static konamiman.z80.utils.NumberUtils.getLowByte;
import static konamiman.z80.utils.NumberUtils.setHighByte;
import static konamiman.z80.utils.NumberUtils.setLowByte;


/**
 * Represents a full set of Z80 registers. This is the default implementation of
 * {@link konamiman.z80.interfaces.Z80Registers}.
 */
public class Z80RegistersImpl extends MainZ80RegistersImpl implements konamiman.z80.interfaces.Z80Registers {

    public Z80RegistersImpl()
    {
        alternate = new MainZ80RegistersImpl();
    }

    private MainZ80Registers alternate;
    public MainZ80Registers getAlternate() {
        return alternate;
    }
    public void setAlternate(MainZ80Registers value) {
        if (value == null)
            throw new NullPointerException("Alternate");

        alternate = value;
    }

    private short IX; public short getIX() { return IX; } public void setIX(short value) { IX = value; }

    public void incIX() { IX++; } public void decIX() { IX--; }

    private short IY; public short getIY() { return IY; } public void setIY(short value) { IY = value; }

    public void incIY() { IY++; } public void decIY() { IY--; }

    private short PC; public short getPC() { return PC; } public void setPC(short value) { PC = value; }

    public short incPC() { return PC++; }

    private short SP; public short getSP() { return SP; } public void setSP(short value) { SP = value; }

    public void incSP() { SP++; } public void decSP() { SP--; } public void addSP(short value) { SP += value; } public void subSP(short value) { SP -= value; }

    private short IR; public short getIR() { return IR; } public void setIR(short value) { IR = value; }

    private Bit IFF1 = Bit.OFF; public Bit getIFF1() { return IFF1; } public void setIFF1(Bit value) { IFF1 = value; }

    private Bit IFF2 = Bit.OFF; public Bit getIFF2() { return IFF2; } public void setIFF2(Bit value) { IFF2 = value; }

    public byte getIXH() { return getHighByte(IX); } public void setIXH(byte value) { IX = setHighByte(IX, value); }

    public byte getIXL() { return getLowByte(IX); } public void setIXL(byte value) { IX = setLowByte(IX, value); }

    public byte getIYH() { return getHighByte(IY); } public void setIYH(byte value) { IY = setHighByte(IY, value); }

    public byte getIYL() { return getLowByte(IY); } public void setIYL(byte value) { IY = setLowByte(IY, value); }

    public byte getI() { return getHighByte(IR); } public void setI(byte value) { IR = setHighByte(IR, value); }

    public byte getR() { return getLowByte(IR); } public void setR(byte value) { IR = setLowByte(IR, value); }
}
