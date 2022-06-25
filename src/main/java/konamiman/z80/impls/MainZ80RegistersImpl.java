package konamiman.z80.impls;

import konamiman.z80.utils.Bit;

import static konamiman.z80.utils.NumberUtils.getBit;
import static konamiman.z80.utils.NumberUtils.getHighByte;
import static konamiman.z80.utils.NumberUtils.getLowByte;
import static konamiman.z80.utils.NumberUtils.setHighByte;
import static konamiman.z80.utils.NumberUtils.setLowByte;
import static konamiman.z80.utils.NumberUtils.withBit;


/**
 * Default implementation of {@link konamiman.z80.interfaces.MainZ80Registers}.
 */
public class MainZ80RegistersImpl implements konamiman.z80.interfaces.MainZ80Registers {

    private short AF; public short getAF() { return AF; } public void setAF(short value) { AF = value; }

    private short BC; public short getBC() { return BC; } public void setBC(short value) { BC = value; }

    public void incBC() { BC++; } public void decBC() { BC--; }

    private short DE; public short getDE() { return DE; } public void setDE(short value) { DE = value; }

    public void incDE() { DE++; } public void decDE() { DE--; }

    private short HL; public short getHL() { return HL; } public void setHL(short value) { HL = value; }

    public void incHL() { HL++; } public void decHL() { HL--; }

    public byte getA() { return getHighByte(AF); } public void setA(byte value) { AF = setHighByte(AF, value); }

    public byte getF() { return getLowByte(AF); } public void setF(byte value) { AF = setLowByte(AF, value); }

    public byte getB() { return getHighByte(BC); } public void setB(byte value) { BC = setHighByte(BC, value); }

    public byte getC() { return getLowByte(BC); } public void setC(byte value) { BC = setLowByte(BC, value); }

    public byte getD() { return getHighByte(DE); } public void setD(byte value) { DE = setHighByte(DE, value); }

    public byte getE() { return getLowByte(DE); } public void setE(byte value) { DE = setLowByte(DE, value); }

    public byte getH() { return getHighByte(HL); } public void setH(byte value) { HL = setHighByte(HL, value); }

    public byte getL() { return getLowByte(HL); } public void setL(byte value) { HL = setLowByte(HL, value); }

    public Bit getCF() { return getBit(getF(), 0); } public void setCF(Bit value) { setF(withBit(getF(), 0, value)); }

    public Bit getNF() { return getBit(getF(), 1); } public void setNF(Bit value) { setF(withBit(getF(), 1, value)); }

    public Bit getPF() { return getBit(getF(), 2); } public void setPF(Bit value) { setF(withBit(getF(), 2, value)); }

    public Bit getFlag3() { return getBit(getF(), 3); } public void setFlag3(Bit value) { setF(withBit(getF(), 3, value)); }

    public Bit getHF() { return getBit(getF(), 4); } public void setHF(Bit value) { setF(withBit(getF(), 4, value)); }

    public Bit getFlag5() { return getBit(getF(), 5); } public void setFlag5(Bit value) { setF(withBit(getF(), 5, value)); }

    public Bit getZF() { return getBit(getF(), 6); } public void setZF(Bit value) { setF(withBit(getF(), 6, value)); }

    public Bit getSF() { return getBit(getF(), 7); } public void setSF(Bit value) { setF(withBit(getF(), 7, value)); }
}
