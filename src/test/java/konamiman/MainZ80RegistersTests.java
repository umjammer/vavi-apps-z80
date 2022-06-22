package konamiman;

import com.flextrade.jfixture.JFixture;
import konamiman.DataTypesAndUtils.Bit;
import konamiman.DataTypesAndUtils.NumberUtils;
import konamiman.DependenciesImplementations.MainZ80Registers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class MainZ80RegistersTests {

    private JFixture Fixture; public JFixture getFixture() { return Fixture; } public void setFixture(JFixture value) { Fixture = value; }
    MainZ80Registers Sut; public MainZ80Registers getSut() { return Sut; } public void setSut(MainZ80Registers value) { Sut = value; }

    @BeforeEach
    public void Setup() {
        Fixture = new JFixture();
        Sut = new MainZ80Registers();
    }

    @Test
    public void Gets_A_and_F_correctly_from_AF() {
        var A = Fixture.create(Byte.TYPE);
        var F = Fixture.create(Byte.TYPE);
        var AF = NumberUtils.createShort(F, A);

        Sut.setAF(AF);

        assertEquals(A, Sut.getA());
        assertEquals(F, Sut.getF());
    }

    @Test
    public void Sets_AF_correctly_from_A_and_F() {
        var A = Fixture.create(Byte.TYPE);
        var F = Fixture.create(Byte.TYPE);
        var expected = NumberUtils.createShort(F, A);

        Sut.setA(A);
        Sut.setF(F);

        assertEquals(expected, Sut.getAF());
    }

    @Test
    public void Gets_B_and_C_correctly_from_BC() {
        var B = Fixture.create(Byte.TYPE);
        var C = Fixture.create(Byte.TYPE);
        var BC = NumberUtils.createShort(C, B);

        Sut.setBC(BC);

        assertEquals(B, Sut.getB());
        assertEquals(C, Sut.getC());
    }

    @Test
    public void Sets_BC_correctly_from_B_and_C() {
        var B = Fixture.create(Byte.TYPE);
        var C = Fixture.create(Byte.TYPE);
        var expected = NumberUtils.createShort(C, B);

        Sut.setB(B);
        Sut.setC(C);

        assertEquals(expected, Sut.getBC());
    }

    @Test
    public void Gets_D_and_E_correctly_from_DE() {
        var D = Fixture.create(Byte.TYPE);
        var E = Fixture.create(Byte.TYPE);
        var DE = NumberUtils.createShort(E, D);

        Sut.setDE(DE);

        assertEquals(D, Sut.getD());
        assertEquals(E, Sut.getE());
    }

    @Test
    public void Sets_DE_correctly_from_D_and_E() {
        var D = Fixture.create(Byte.TYPE);
        var E = Fixture.create(Byte.TYPE);
        var expected = NumberUtils.createShort(E, D);

        Sut.setD(D);
        Sut.setE(E);

        assertEquals(expected, Sut.getDE());
    }

    @Test
    public void Gets_H_and_L_correctly_from_HL() {
        var H = Fixture.create(Byte.TYPE);
        var L = Fixture.create(Byte.TYPE);
        var HL = NumberUtils.createShort(L, H);

        Sut.setHL(HL);

        assertEquals(H, Sut.getH());
        assertEquals(L, Sut.getL());
    }

    @Test
    public void Sets_HL_correctly_from_H_and_L() {
        var H = Fixture.create(Byte.TYPE);
        var L = Fixture.create(Byte.TYPE);
        var expected = NumberUtils.createShort(L, H);

        Sut.setH(H);
        Sut.setL(L);

        assertEquals(expected, Sut.getHL());
    }

    @Test
    public void Gets_CF_correctly_from_F() {
        Sut.setF((byte) 0xFE);
        assertEquals(0, Sut.getCF().intValue());

        Sut.setF((byte) 0x01);
        assertEquals(1, Sut.getCF().intValue());
    }

    @Test
    public void Sets_F_correctly_from_CF() {
        Sut.setF((byte) 0xFF);
        Sut.setCF(Bit.OFF);
        assertEquals(0xFE, Sut.getF());

        Sut.setF((byte) 0x00);
        Sut.setCF(Bit.ON);
        assertEquals(0x01, Sut.getF());
    }

    @Test
    public void Gets_NF_correctly_from_F() {
        Sut.setF((byte) 0xFD);
        assertEquals(0, Sut.getNF().intValue());

        Sut.setF((byte) 0x02);
        assertEquals(1, Sut.getNF().intValue());
    }

    @Test
    public void Sets_F_correctly_from_NF() {
        Sut.setF((byte) 0xFF);
        Sut.setNF(Bit.OFF);
        assertEquals(0xFD, Sut.getF());

        Sut.setF((byte) 0x00);
        Sut.setNF(Bit.ON);
        assertEquals(0x02, Sut.getF());
    }

    @Test
    public void Gets_PF_correctly_from_F() {
        Sut.setF((byte) 0xFB);
        assertEquals(0, Sut.getPF().intValue());

        Sut.setF((byte) 0x04);
        assertEquals(1, Sut.getPF().intValue());
    }

    @Test
    public void Sets_F_correctly_from_PF() {
        Sut.setF((byte) 0xFF);
        Sut.setPF(Bit.OFF);
        assertEquals(0xFB, Sut.getF());

        Sut.setF((byte) 0x00);
        Sut.setPF(Bit.ON);
        assertEquals(0x04, Sut.getF());
    }

    @Test
    public void Gets_Flag3_correctly_from_F() {
        Sut.setF((byte) 0xF7);
        assertEquals(0, Sut.getFlag3().intValue());

        Sut.setF((byte) 0x08);
        assertEquals(1, Sut.getFlag3().intValue());
    }

    @Test
    public void Sets_F_correctly_from_Flag3() {
        Sut.setF((byte) 0xFF);
        Sut.setFlag3(Bit.OFF);
        assertEquals(0xF7, Sut.getF());

        Sut.setF((byte) 0x00);
        Sut.setFlag3(Bit.ON);
        assertEquals(0x08, Sut.getF());
    }

    @Test
    public void Gets_HF_correctly_from_F() {
        Sut.setF((byte) 0xEF);
        assertEquals(0, Sut.getHF().intValue());

        Sut.setF((byte) 0x10);
        assertEquals(1, Sut.getHF().intValue());
    }

    @Test
    public void Sets_F_correctly_from_HF() {
        Sut.setF((byte) 0xFF);
        Sut.setHF(Bit.OFF);
        assertEquals(0xEF, Sut.getF());

        Sut.setF((byte) 0x00);
        Sut.setHF(Bit.ON);
        assertEquals(0x10, Sut.getF());
    }

    @Test
    public void Gets_Flag5_correctly_from_F() {
        Sut.setF((byte) 0xDF);
        assertEquals(0, Sut.getFlag5().intValue());

        Sut.setF((byte) 0x20);
        assertEquals(1, Sut.getFlag5().intValue());
    }

    @Test
    public void Sets_F_correctly_from_Flag5() {
        Sut.setF((byte) 0xFF);
        Sut.setFlag5(Bit.OFF);
        assertEquals(0xDF, Sut.getF());

        Sut.setF((byte) 0x00);
        Sut.setFlag5(Bit.ON);
        assertEquals(0x20, Sut.getF());
    }

    @Test
    public void Gets_ZF_correctly_from_F() {
        Sut.setF((byte) 0xBF);
        assertEquals(0, Sut.getZF().intValue());

        Sut.setF((byte) 0x40);
        assertEquals(1, Sut.getZF().intValue());
    }

    @Test
    public void Sets_F_correctly_from_ZF() {
        Sut.setF((byte) 0xFF);
        Sut.setZF(Bit.OFF);
        assertEquals(0xBF, Sut.getF());

        Sut.setF((byte) 0x00);
        Sut.setZF(Bit.ON);
        assertEquals(0x40, Sut.getF());
    }

    @Test
    public void Gets_SF_correctly_from_F() {
        Sut.setF((byte) 0x7F);
        assertEquals(0, Sut.getSF().intValue());

        Sut.setF((byte) 0x80);
        assertEquals(1, Sut.getSF().intValue());
    }

    @Test
    public void Sets_F_correctly_from_SF() {
        Sut.setF((byte) 0xFF);
        Sut.setSF(Bit.OFF);
        assertEquals(0x7F, Sut.getF());

        Sut.setF((byte) 0x00);
        Sut.setSF(Bit.ON);
        assertEquals(0x80, Sut.getF());
    }
}
