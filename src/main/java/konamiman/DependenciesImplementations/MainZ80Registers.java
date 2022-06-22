package konamiman.DependenciesImplementations;

import konamiman.DataTypesAndUtils.Bit;
import konamiman.DependenciesInterfaces.IMainZ80Registers;

import static konamiman.DataTypesAndUtils.NumberUtils.GetBit;
import static konamiman.DataTypesAndUtils.NumberUtils.GetHighByte;
import static konamiman.DataTypesAndUtils.NumberUtils.GetLowByte;
import static konamiman.DataTypesAndUtils.NumberUtils.SetHighByte;
import static konamiman.DataTypesAndUtils.NumberUtils.SetLowByte;
import static konamiman.DataTypesAndUtils.NumberUtils.WithBit;


/**
 * Default implementation of {@link IMainZ80Registers}.
 */
public class MainZ80Registers implements IMainZ80Registers {

    private short AF; public short getAF() { return AF; } public void setAF(short value) { AF = value; }

    private short BC; public short getBC() { return BC; } public void setBC(short value) { BC = value; }

    public void incBC() { BC++; } public void decBC() { BC--; }

    private short DE; public short getDE() { return DE; } public void setDE(short value) { DE = value; }

    public void incDE() { DE++; } public void decDE() { DE--; }

    private short HL; public short getHL() { return HL; } public void setHL(short value) { HL = value; }

    public void incHL() { HL++; } public void decHL() { HL--; }

    private byte A; public byte getA() { return GetHighByte(AF); } public void setA(byte value) { AF = AF = SetHighByte(AF, value); }

    private byte F; public byte getF() { return GetLowByte(AF); } public void setF(byte value) { AF = AF = SetLowByte(AF, value); }

    private byte B; public byte getB() { return GetHighByte(BC); } public void setB(byte value) { BC = BC = SetHighByte(BC, value); }

    private byte C; public byte getC() { return GetLowByte(BC); } public void setC(byte value) { BC = BC = SetLowByte(BC, value); }

    private byte D; public byte getD() { return GetHighByte(DE); } public void setD(byte value) { DE = DE = SetHighByte(DE, value); }

    private byte E; public byte getE() { return GetLowByte(DE); } public void setE(byte value) { DE = DE = SetLowByte(DE, value); }

    private byte H; public byte getH() { return GetHighByte(HL); } public void setH(byte value) { HL = HL = SetHighByte(HL, value); }

    private byte L; public byte getL() { return GetLowByte(HL); } public void setL(byte value) { HL = HL = SetLowByte(HL, value); }

    private Bit CF; public Bit getCF() { return GetBit(F, 0); } public void setCF(Bit value) { F = WithBit(F, 0, value); }

    private Bit NF; public Bit getNF() { return GetBit(F, 1); } public void setNF(Bit value) { F = WithBit(F, 1, value); }

    private Bit PF; public Bit getPF() { return GetBit(F, 2); } public void setPF(Bit value) { F = WithBit(F, 2, value); }

    private Bit Flag3; public Bit getFlag3() { return GetBit(F, 3); } public void setFlag3(Bit value) { F = WithBit(F, 3, value); }

    private Bit HF; public Bit getHF() { return GetBit(F, 4); } public void setHF(Bit value) { F = WithBit(F, 4, value); }

    private Bit Flag5; public Bit getFlag5() { return GetBit(F, 5); } public void setFlag5(Bit value) { F = WithBit(F, 5, value); }

    private Bit ZF; public Bit getZF() { return GetBit(F, 6); } public void setZF(Bit value) { F = WithBit(F, 6, value); }

    private Bit SF; public Bit getSF() { return GetBit(F, 7); } public void setSF(Bit value) { F = WithBit(F, 7, value); }
}
