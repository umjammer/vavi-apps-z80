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
    @Override public MainZ80Registers getAlternate() {
        return alternate;
    }
    @Override public void setAlternate(MainZ80Registers value) {
        if (value == null)
            throw new NullPointerException("Alternate");

        alternate = value;
    }

    private short IX;
    @Override public short getIX() { return IX; }
    @Override public void setIX(short value) { IX = value; }

    @Override public void incIX() { IX++; }
    @Override public void decIX() { IX--; }

    private short IY;
    @Override public short getIY() { return IY; }
    @Override public void setIY(short value) { IY = value; }

    @Override public void incIY() { IY++; }
    @Override public void decIY() { IY--; }

    private short PC;
    @Override public short getPC() { return PC; }
    @Override public void setPC(short value) { PC = value; }

    @Override public short incPC() { return PC++; }
    @Override public void decPC() { PC--; }

    private short SP;
    @Override public short getSP() { return SP; }
    @Override public void setSP(short value) { SP = value; }

    @Override public void incSP() { SP++; }
    @Override public void decSP() { SP--; }
    @Override public void addSP(short value) { SP += value; }
    @Override public void subSP(short value) { SP -= value; }

    private short IR;
    @Override public short getIR() { return IR; }
    @Override public void setIR(short value) { IR = value; }

    private Bit IFF1 = Bit.OFF;
    @Override public Bit getIFF1() { return IFF1; }
    @Override public void setIFF1(Bit value) { IFF1 = value; }

    private Bit IFF2 = Bit.OFF;
    @Override public Bit getIFF2() { return IFF2; }
    @Override public void setIFF2(Bit value) { IFF2 = value; }

    @Override public byte getIXH() { return getHighByte(IX); }
    @Override public void setIXH(byte value) { IX = setHighByte(IX, value); }

    @Override public byte getIXL() { return getLowByte(IX); }
    @Override public void setIXL(byte value) { IX = setLowByte(IX, value); }

    @Override public byte getIYH() { return getHighByte(IY); }
    @Override public void setIYH(byte value) { IY = setHighByte(IY, value); }

    @Override public byte getIYL() { return getLowByte(IY); }
    @Override public void setIYL(byte value) { IY = setLowByte(IY, value); }

    @Override public byte getI() { return getHighByte(IR); }
    @Override public void setI(byte value) { IR = setHighByte(IR, value); }

    @Override public byte getR() { return getLowByte(IR); }
    @Override public void setR(byte value) { IR = setLowByte(IR, value); }
}
