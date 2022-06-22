package konamiman;

import com.flextrade.jfixture.JFixture;
import konamiman.DataTypesAndUtils.NumberUtils;
import konamiman.DependenciesImplementations.MainZ80Registers;
import konamiman.DependenciesImplementations.Z80Registers;
import konamiman.DependenciesInterfaces.IMainZ80Registers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;


public class Z80RegistersTests {

    private JFixture Fixture; public JFixture getFixture() { return Fixture; } public void setFixture(JFixture value) { Fixture = value; }
    Z80Registers Sut; public Z80Registers getSut() { return Sut; } public void setSut(Z80Registers value) { Sut = value; }

    @BeforeEach
    public void Setup() {
        Fixture = new JFixture();
        Sut = new Z80Registers();
    }

    @Test
    public void Alternate_registers_are_properly_set() {
        assertInstanceOf(MainZ80Registers.class, Sut.getAlternate());
    }

    @Test
    public void Can_set_Alternate_to_non_null_value() {
        var value = mock(IMainZ80Registers.class);
        Sut.setAlternate(value);
        assertEquals(value, Sut.getAlternate());
    }

    @Test
    public void Cannot_set_Alternate_to_null() {
        assertThrows(NullPointerException.class, () -> Sut.setAlternate(null));
    }

    @Test
    public void Gets_IXh_and_IXl_correctly_from_IX() {
        var IXh = Fixture.create(Byte.TYPE);
        var IXl = Fixture.create(Byte.TYPE);
        var IX = NumberUtils.createShort(IXl, IXh);

        Sut.setIX(IX);

        assertEquals(IXh, Sut.getIXH());
        assertEquals(IXl, Sut.getIXL());
    }

    @Test
    public void Sets_IX_correctly_from_IXh_and_IXl() {
        var IXh = Fixture.create(Byte.TYPE);
        var IXl = Fixture.create(Byte.TYPE);
        var expected = NumberUtils.createShort(IXl, IXh);

        Sut.setIXH(IXh);
        Sut.setIXL(IXl);

        assertEquals(expected, Sut.getIX());
    }

    @Test
    public void Gets_IYh_and_IYl_correctly_from_IY() {
        var IYh = Fixture.create(Byte.TYPE);
        var IYl = Fixture.create(Byte.TYPE);
        var IY = NumberUtils.createShort(IYl, IYh);

        Sut.setIY(IY);

        assertEquals(IYh, Sut.getIYH());
        assertEquals(IYl, Sut.getIYL());
    }

    @Test
    public void Sets_IY_correctly_from_IYh_and_IYl() {
        var IYh = Fixture.create(Byte.TYPE);
        var IYl = Fixture.create(Byte.TYPE);
        var expected = NumberUtils.createShort(IYl, IYh);

        Sut.setIYH(IYh);
        Sut.setIYL(IYl);

        assertEquals(expected, Sut.getIY());
    }

    @Test
    public void Gets_I_and_R_correctly_from_IR() {
        var I = Fixture.create(Byte.TYPE);
        var R = Fixture.create(Byte.TYPE);
        var IR = NumberUtils.createShort(R, I);

        Sut.setIR(IR);

        assertEquals(I, Sut.getI());
        assertEquals(R, Sut.getR());
    }

    @Test
    public void Sets_IR_correctly_from_I_and_R() {
        var I = Fixture.create(Byte.TYPE);
        var R = Fixture.create(Byte.TYPE);
        var expected = NumberUtils.createShort(R, I);

        Sut.setI(I);
        Sut.setR(R);

        assertEquals(expected, Sut.getIR());
    }
}
