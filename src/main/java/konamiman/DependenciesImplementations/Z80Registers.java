package konamiman.DependenciesImplementations;

import konamiman.DataTypesAndUtils.Bit;
import konamiman.DependenciesInterfaces.IMainZ80Registers;
import konamiman.DependenciesInterfaces.IZ80Registers;

import static konamiman.DataTypesAndUtils.NumberUtils.GetHighByte;
import static konamiman.DataTypesAndUtils.NumberUtils.GetLowByte;
import static konamiman.DataTypesAndUtils.NumberUtils.SetHighByte;
import static konamiman.DataTypesAndUtils.NumberUtils.SetLowByte;


/**
 * Represents a full set of Z80 registers. This is the default implementation of
 * {@link IZ80Registers}.
 */
public class Z80Registers extends MainZ80Registers implements IZ80Registers {
    public Z80Registers()
    {
        alternate = new MainZ80Registers();
    }

    private IMainZ80Registers alternate;
    public IMainZ80Registers getAlternate() {
        return alternate;
    }
    public void setAlternate(IMainZ80Registers value) {
        if(value == null)
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

    private Bit IFF1; public Bit getIFF1() { return IFF1; } public void setIFF1(Bit value) { IFF1 = value; }

    private Bit IFF2; public Bit getIFF2() { return IFF2; } public void setIFF2(Bit value) { IFF2 = value; }

    private byte IXH; public byte getIXH() { return GetHighByte(IX); } public void setIXH(byte value) { IX = IX = SetHighByte(IX, value); }

    private byte IXL; public byte getIXL() { return GetLowByte(IX); } public void setIXL(byte value) { IX = IX = SetLowByte(IX, value); }

    private byte IYH; public byte getIYH() { return GetHighByte(IY); } public void setIYH(byte value) { IY = IY = SetHighByte(IY, value); }

    private byte IYL; public byte getIYL() { return GetLowByte(IY); } public void setIYL(byte value) { IY = IY = SetLowByte(IY, value); }

    private byte I; public byte getI() { return GetHighByte(IR); } public void setI(byte value) { IR = IR = SetHighByte(IR, value); }

    private byte R; public byte getR() { return GetLowByte(IR); } public void setR(byte value) { IR = IR = SetLowByte(IR, value); }
}
