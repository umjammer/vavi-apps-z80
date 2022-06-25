package konamiman.z80;

import com.flextrade.jfixture.JFixture;
import konamiman.z80.utils.Bit;
import konamiman.z80.impls.MainZ80RegistersImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static konamiman.z80.utils.NumberUtils.createShort;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class MainZ80RegistersTests {

    private JFixture fixture;
    MainZ80RegistersImpl sut;

    @BeforeEach
    public void Setup() {
        fixture = new JFixture();
        sut = new MainZ80RegistersImpl();
    }

    @Test
    public void Gets_A_and_F_correctly_from_AF() {
        var A = fixture.create(Byte.TYPE);
        var F = fixture.create(Byte.TYPE);
        var AF = createShort(F, A);

        sut.setAF(AF);

        assertEquals(A, sut.getA());
        assertEquals(F, sut.getF());
    }

    @Test
    public void Sets_AF_correctly_from_A_and_F() {
        var A = fixture.create(Byte.TYPE);
        var F = fixture.create(Byte.TYPE);
        var expected = createShort(F, A);

        sut.setA(A);
        sut.setF(F);

        assertEquals(expected, sut.getAF());
    }

    @Test
    public void Gets_B_and_C_correctly_from_BC() {
        var B = fixture.create(Byte.TYPE);
        var C = fixture.create(Byte.TYPE);
        var BC = createShort(C, B);

        sut.setBC(BC);

        assertEquals(B, sut.getB());
        assertEquals(C, sut.getC());
    }

    @Test
    public void Sets_BC_correctly_from_B_and_C() {
        var B = fixture.create(Byte.TYPE);
        var C = fixture.create(Byte.TYPE);
        var expected = createShort(C, B);

        sut.setB(B);
        sut.setC(C);

        assertEquals(expected, sut.getBC());
    }

    @Test
    public void Gets_D_and_E_correctly_from_DE() {
        var D = fixture.create(Byte.TYPE);
        var E = fixture.create(Byte.TYPE);
        var DE = createShort(E, D);

        sut.setDE(DE);

        assertEquals(D, sut.getD());
        assertEquals(E, sut.getE());
    }

    @Test
    public void Sets_DE_correctly_from_D_and_E() {
        var D = fixture.create(Byte.TYPE);
        var E = fixture.create(Byte.TYPE);
        var expected = createShort(E, D);

        sut.setD(D);
        sut.setE(E);

        assertEquals(expected, sut.getDE());
    }

    @Test
    public void Gets_H_and_L_correctly_from_HL() {
        var H = fixture.create(Byte.TYPE);
        var L = fixture.create(Byte.TYPE);
        var HL = createShort(L, H);

        sut.setHL(HL);

        assertEquals(H, sut.getH());
        assertEquals(L, sut.getL());
    }

    @Test
    public void Sets_HL_correctly_from_H_and_L() {
        var H = fixture.create(Byte.TYPE);
        var L = fixture.create(Byte.TYPE);
        var expected = createShort(L, H);

        sut.setH(H);
        sut.setL(L);

        assertEquals(expected, sut.getHL());
    }

    @Test
    public void Gets_CF_correctly_from_F() {
        sut.setF((byte) 0xFE);
        assertEquals(0, sut.getCF().intValue());

        sut.setF((byte) 0x01);
        assertEquals(1, sut.getCF().intValue());
    }

    @Test
    public void Sets_F_correctly_from_CF() {
        sut.setF((byte) 0xFF);
        sut.setCF(Bit.OFF);
        assertEquals((byte) 0xFE, sut.getF());

        sut.setF((byte) 0x00);
        sut.setCF(Bit.ON);
        assertEquals((byte) 0x01, sut.getF());
    }

    @Test
    public void Gets_NF_correctly_from_F() {
        sut.setF((byte) 0xFD);
        assertEquals(0, sut.getNF().intValue());

        sut.setF((byte) 0x02);
        assertEquals(1, sut.getNF().intValue());
    }

    @Test
    public void Sets_F_correctly_from_NF() {
        sut.setF((byte) 0xFF);
        sut.setNF(Bit.OFF);
        assertEquals((byte) 0xFD, sut.getF());

        sut.setF((byte) 0x00);
        sut.setNF(Bit.ON);
        assertEquals((byte) 0x02, sut.getF());
    }

    @Test
    public void Gets_PF_correctly_from_F() {
        sut.setF((byte) 0xFB);
        assertEquals(0, sut.getPF().intValue());

        sut.setF((byte) 0x04);
        assertEquals(1, sut.getPF().intValue());
    }

    @Test
    public void Sets_F_correctly_from_PF() {
        sut.setF((byte) 0xFF);
        sut.setPF(Bit.OFF);
        assertEquals((byte) 0xFB, sut.getF());

        sut.setF((byte) 0x00);
        sut.setPF(Bit.ON);
        assertEquals((byte) 0x04, sut.getF());
    }

    @Test
    public void Gets_Flag3_correctly_from_F() {
        sut.setF((byte) 0xF7);
        assertEquals(0, sut.getFlag3().intValue());

        sut.setF((byte) 0x08);
        assertEquals(1, sut.getFlag3().intValue());
    }

    @Test
    public void Sets_F_correctly_from_Flag3() {
        sut.setF((byte) 0xFF);
        sut.setFlag3(Bit.OFF);
        assertEquals((byte) 0xF7, sut.getF());

        sut.setF((byte) 0x00);
        sut.setFlag3(Bit.ON);
        assertEquals((byte) 0x08, sut.getF());
    }

    @Test
    public void Gets_HF_correctly_from_F() {
        sut.setF((byte) 0xEF);
        assertEquals(0, sut.getHF().intValue());

        sut.setF((byte) 0x10);
        assertEquals(1, sut.getHF().intValue());
    }

    @Test
    public void Sets_F_correctly_from_HF() {
        sut.setF((byte) 0xFF);
        sut.setHF(Bit.OFF);
        assertEquals((byte) 0xEF, sut.getF());

        sut.setF((byte) 0x00);
        sut.setHF(Bit.ON);
        assertEquals((byte) 0x10, sut.getF());
    }

    @Test
    public void Gets_Flag5_correctly_from_F() {
        sut.setF((byte) 0xDF);
        assertEquals(0, sut.getFlag5().intValue());

        sut.setF((byte) 0x20);
        assertEquals(1, sut.getFlag5().intValue());
    }

    @Test
    public void Sets_F_correctly_from_Flag5() {
        sut.setF((byte) 0xFF);
        sut.setFlag5(Bit.OFF);
        assertEquals((byte) 0xDF, sut.getF());

        sut.setF((byte) 0x00);
        sut.setFlag5(Bit.ON);
        assertEquals((byte) 0x20, sut.getF());
    }

    @Test
    public void Gets_ZF_correctly_from_F() {
        sut.setF((byte) 0xBF);
        assertEquals(0, sut.getZF().intValue());

        sut.setF((byte) 0x40);
        assertEquals(1, sut.getZF().intValue());
    }

    @Test
    public void Sets_F_correctly_from_ZF() {
        sut.setF((byte) 0xFF);
        sut.setZF(Bit.OFF);
        assertEquals((byte) 0xBF, sut.getF());

        sut.setF((byte) 0x00);
        sut.setZF(Bit.ON);
        assertEquals((byte) 0x40, sut.getF());
    }

    @Test
    public void Gets_SF_correctly_from_F() {
        sut.setF((byte) 0x7F);
        assertEquals(0, sut.getSF().intValue());

        sut.setF((byte) 0x80);
        assertEquals(1, sut.getSF().intValue());
    }

    @Test
    public void Sets_F_correctly_from_SF() {
        sut.setF((byte) 0xFF);
        sut.setSF(Bit.OFF);
        assertEquals((byte) 0x7F, sut.getF());

        sut.setF((byte) 0x00);
        sut.setSF(Bit.ON);
        assertEquals((byte) 0x80, sut.getF());
    }
}
