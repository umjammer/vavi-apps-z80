package konamiman.DependenciesInterfaces;

import konamiman.DataTypesAndUtils.Bit;


/**
 * Represents a full set of Z80 registers.
 */
public interface IZ80Registers extends IMainZ80Registers {

    /**
     * The alternate register set (AF', BC', DE', HL')
     */
    IMainZ80Registers getAlternate(); void setAlternate(IMainZ80Registers value);

    /**
     * The IX register pair
     */
    short getIX(); void setIX(short value); void incIX(); void decIX();

    /**
     * The IY register pair
     */
    short getIY(); void setIY(short value); void incIY(); void decIY();

    /**
     * The program counter
     */
    short getPC(); void setPC(short value); short incPC();

    /**
     * The stack pointer
     */
    short getSP(); void setSP(short value); void incSP(); void decSP(); void addSP(short value); void subSP(short value);

    /**
     * The interrupt and refresh register
     */
    short getIR(); void setIR(short value);

    /**
     * The IFF1 flag. It has always the value 0 or 1.
     */
    Bit getIFF1(); void setIFF1(Bit value);

    /**
     * The IFF2 flag. It has always the value 0 or 1.
     */
    Bit getIFF2(); void setIFF2(Bit value);

    /**
     * The IXH register.
     */
    byte getIXH(); void setIXH(byte value);

    /**
     * The IXL register.
     */
    byte getIXL(); void setIXL(byte value);

    /**
     * The IYH register.
     */
    byte getIYH(); void setIYH(byte value);

    /**
     * The IYL register.
     */
    byte getIYL(); void setIYL(byte value);

    /**
     * The I register.
     */
    byte getI(); void setI(byte value);

    /**
     * The R register.
     */
    byte getR(); void setR(byte value);
}
