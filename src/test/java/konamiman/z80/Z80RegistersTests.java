package konamiman.z80;

import com.flextrade.jfixture.JFixture;
import konamiman.z80.impls.MainZ80RegistersImpl;
import konamiman.z80.impls.Z80RegistersImpl;
import konamiman.z80.interfaces.MainZ80Registers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static konamiman.z80.utils.NumberUtils.createShort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;


public class Z80RegistersTests {

    private JFixture fixture;
    Z80RegistersImpl sut;

    @BeforeEach
    public void Setup() {
        fixture = new JFixture();
        sut = new Z80RegistersImpl();
    }

    @Test
    public void Alternate_registers_are_properly_set() {
        assertInstanceOf(MainZ80RegistersImpl.class, sut.getAlternate());
    }

    @Test
    public void Can_set_Alternate_to_non_null_value() {
        var value = mock(MainZ80Registers.class);
        sut.setAlternate(value);
        assertEquals(value, sut.getAlternate());
    }

    @Test
    public void Cannot_set_Alternate_to_null() {
        assertThrows(NullPointerException.class, () -> sut.setAlternate(null));
    }

    @Test
    public void Gets_IXh_and_IXl_correctly_from_IX() {
        var IXh = fixture.create(Byte.TYPE);
        var IXl = fixture.create(Byte.TYPE);
        var IX = createShort(IXl, IXh);

        sut.setIX(IX);

        assertEquals(IXh, sut.getIXH());
        assertEquals(IXl, sut.getIXL());
    }

    @Test
    public void Sets_IX_correctly_from_IXh_and_IXl() {
        var IXh = fixture.create(Byte.TYPE);
        var IXl = fixture.create(Byte.TYPE);
        var expected = createShort(IXl, IXh);

        sut.setIXH(IXh);
        sut.setIXL(IXl);

        assertEquals(expected, sut.getIX());
    }

    @Test
    public void Gets_IYh_and_IYl_correctly_from_IY() {
        var IYh = fixture.create(Byte.TYPE);
        var IYl = fixture.create(Byte.TYPE);
        var IY = createShort(IYl, IYh);

        sut.setIY(IY);

        assertEquals(IYh, sut.getIYH());
        assertEquals(IYl, sut.getIYL());
    }

    @Test
    public void Sets_IY_correctly_from_IYh_and_IYl() {
        var IYh = fixture.create(Byte.TYPE);
        var IYl = fixture.create(Byte.TYPE);
        var expected = createShort(IYl, IYh);

        sut.setIYH(IYh);
        sut.setIYL(IYl);

        assertEquals(expected, sut.getIY());
    }

    @Test
    public void Gets_I_and_R_correctly_from_IR() {
        var I = fixture.create(Byte.TYPE);
        var R = fixture.create(Byte.TYPE);
        var IR = createShort(R, I);

        sut.setIR(IR);

        assertEquals(I, sut.getI());
        assertEquals(R, sut.getR());
    }

    @Test
    public void Sets_IR_correctly_from_I_and_R() {
        var I = fixture.create(Byte.TYPE);
        var R = fixture.create(Byte.TYPE);
        var expected = createShort(R, I);

        sut.setI(I);
        sut.setR(R);

        assertEquals(expected, sut.getIR());
    }
}
