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

    private short AF;
    @Override public short getAF() { return AF; }
    @Override public void setAF(short value) { AF = value; }

    private short BC;
    @Override public short getBC() { return BC; }
    @Override public void setBC(short value) { BC = value; }

    @Override public void incBC() { BC++; }
    @Override public void decBC() { BC--; }

    private short DE;
    @Override public short getDE() { return DE; }
    @Override public void setDE(short value) { DE = value; }

    @Override public void incDE() { DE++; }
    @Override public void decDE() { DE--; }

    private short HL;
    @Override public short getHL() { return HL; }
    @Override public void setHL(short value) { HL = value; }

    @Override public void incHL() { HL++; }
    @Override public void decHL() { HL--; }

    @Override public byte getA() { return getHighByte(AF); }
    @Override public void setA(byte value) { AF = setHighByte(AF, value); }

    @Override public byte getF() { return getLowByte(AF); }
    @Override public void setF(byte value) { AF = setLowByte(AF, value); }

    @Override public byte getB() { return getHighByte(BC); }
    @Override public void setB(byte value) { BC = setHighByte(BC, value); }

    @Override public byte getC() { return getLowByte(BC); }
    @Override public void setC(byte value) { BC = setLowByte(BC, value); }

    @Override public byte getD() { return getHighByte(DE); }
    @Override public void setD(byte value) { DE = setHighByte(DE, value); }

    @Override public byte getE() { return getLowByte(DE); }
    @Override public void setE(byte value) { DE = setLowByte(DE, value); }

    @Override public byte getH() { return getHighByte(HL); }
    @Override public void setH(byte value) { HL = setHighByte(HL, value); }

    @Override public byte getL() { return getLowByte(HL); }
    @Override public void setL(byte value) { HL = setLowByte(HL, value); }

    @Override public Bit getCF() { return getBit(getF(), 0); }
    @Override public void setCF(Bit value) { setF(withBit(getF(), 0, value)); }

    @Override public Bit getNF() { return getBit(getF(), 1); }
    @Override public void setNF(Bit value) { setF(withBit(getF(), 1, value)); }

    @Override public Bit getPF() { return getBit(getF(), 2); }
    @Override public void setPF(Bit value) { setF(withBit(getF(), 2, value)); }

    @Override public Bit getFlag3() { return getBit(getF(), 3); }
    @Override public void setFlag3(Bit value) { setF(withBit(getF(), 3, value)); }

    @Override public Bit getHF() { return getBit(getF(), 4); }
    @Override public void setHF(Bit value) { setF(withBit(getF(), 4, value)); }

    @Override public Bit getFlag5() { return getBit(getF(), 5); }
    @Override public void setFlag5(Bit value) { setF(withBit(getF(), 5, value)); }

    @Override public Bit getZF() { return getBit(getF(), 6); }
    @Override public void setZF(Bit value) { setF(withBit(getF(), 6, value)); }

    @Override public Bit getSF() { return getBit(getF(), 7); }
    @Override public void setSF(Bit value) { setF(withBit(getF(), 7, value)); }
}
