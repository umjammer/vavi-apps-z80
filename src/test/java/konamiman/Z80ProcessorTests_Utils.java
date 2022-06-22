package konamiman;

import com.flextrade.jfixture.JFixture;
import konamiman.DataTypesAndUtils.NumberUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.GetHighByte;
import static konamiman.DataTypesAndUtils.NumberUtils.GetLowByte;
import static konamiman.DataTypesAndUtils.NumberUtils.Inc;
import static konamiman.DataTypesAndUtils.NumberUtils.Sub;
import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static konamiman.DataTypesAndUtils.NumberUtils.ToUShort;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class Z80ProcessorTests_Utils {

    private JFixture Fixture; public JFixture getFixture() { return Fixture; } public void setFixture(JFixture value) { Fixture = value; }

    private Z80Processor Sut; public Z80Processor getSut() { return Sut; } public void setSut(Z80Processor value) { Sut = value; }

    @BeforeEach
    public void SetUp() {
        Fixture = new JFixture();
        Sut = new Z80Processor();
    }

    @Test
    public void ExecuteCall_pushes_PC_and_jumps() {
        var oldPC = Fixture.create(Short.TYPE);
        var callAddress = Fixture.create(Short.TYPE);
        var oldSP = Fixture.create(Short.TYPE);
        Sut.getRegisters().setSP(oldSP);
        Sut.getRegisters().setPC(oldPC);

        Sut.ExecuteCall(callAddress);

        assertEquals(callAddress, Sut.getRegisters().getPC());
        assertEquals(Sub(oldSP, (short) 2), Sut.getRegisters().getSP());
        assertEquals(ToShort(oldPC), ReadShortFromMemory(ToUShort(Sut.getRegisters().getSP())));
    }

    @Test
    public void ExecuteRet_pops_PC() {
        var pushedAddress = Fixture.create(Short.TYPE);
        var sp = Fixture.create(Short.TYPE);

        Sut.getRegisters().setSP(sp);
        Sut.getMemory().set(ToUShort(sp), GetLowByte(pushedAddress));
        Sut.getMemory().set(Inc(ToUShort(sp)), GetHighByte(pushedAddress));

        Sut.ExecuteRet();

        assertEquals(pushedAddress, Sut.getRegisters().getPC());
        assertEquals(Add(sp, (short) 2), Sut.getRegisters().getSP());
    }

    private short ReadShortFromMemory(short address) {
        return NumberUtils.createShort(Sut.getMemory().get(address), Sut.getMemory().get(Inc(address)));
    }
}
