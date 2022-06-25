package konamiman.z80.interfaces;

import konamiman.z80.utils.Bit;


/**
 * Represents a set of the Z80 main registers (AF, BC, DE, HL)
 */
public interface MainZ80Registers {

    /**
     * The AF register pair
     */
    short getAF(); void setAF(short value);

    /**
     * The BC register pair
     */
    short getBC(); void setBC(short value); void incBC(); void decBC();

    /**
     * The DE register pair
     */
    short getDE(); void setDE(short value); void incDE(); void decDE();

    /**
     * The HL register pair
     */
    short getHL(); void setHL(short value); void incHL(); void decHL();

    /**
     * The A register.
     */
    byte getA(); void setA(byte value);

    /**
     * The F register.
     */
    byte getF(); void setF(byte value);

    /**
     * The B register.
     */
    byte getB(); void setB(byte value);

    /**
     * The C register.
     */
    byte getC(); void setC(byte value);

    /**
     * The D register.
     */
    byte getD(); void setD(byte value);

    /**
     * The E register.
     */
    byte getE(); void setE(byte value);

    /**
     * The H register.
     */
    byte getH(); void setH(byte value);

    /**
     * The E register.
     */
    byte getL(); void setL(byte value);

    /**
     * The carry (C) flag.
     */
    Bit getCF(); void setCF(Bit value);

    /**
     * The addition/substraction (N) flag.
     */
    Bit getNF(); void setNF(Bit value);

    /**
     * The parity/overflow (P/V) flag.
     */
    Bit getPF(); void setPF(Bit value);

    /**
     * The unused flag at bit 3 of F.
     */
    Bit getFlag3(); void setFlag3(Bit value);

    /**
     * The half carry (H) flag.
     */
    Bit getHF(); void setHF(Bit value);

    /**
     * The unused flag at bit 5 of F.
     */
    Bit getFlag5(); void setFlag5(Bit value);

    /**
     * The zero (Z) flag.
     */
    Bit getZF(); void setZF(Bit value);

    /**
     * The sign (S) flag.
     */
    Bit getSF(); void setSF(Bit value);
}
