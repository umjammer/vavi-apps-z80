package konamiman.z80;

import com.flextrade.jfixture.JFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static konamiman.z80.utils.NumberUtils.sub;
import static konamiman.z80.utils.NumberUtils.add;
import static konamiman.z80.utils.NumberUtils.createShort;
import static konamiman.z80.utils.NumberUtils.getHighByte;
import static konamiman.z80.utils.NumberUtils.getLowByte;
import static konamiman.z80.utils.NumberUtils.inc;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class Z80ProcessorTests_Utils {

    private JFixture fixture;

    private Z80Processor sut;

    @BeforeEach
    public void setup() {
        fixture = new JFixture();
        sut = new Z80ProcessorImpl();
    }

    @Test
    public void ExecuteCall_pushes_PC_and_jumps() {
        var oldPC = fixture.create(Short.TYPE);
        var callAddress = fixture.create(Short.TYPE);
        var oldSP = fixture.create(Short.TYPE);
        sut.getRegisters().setSP(oldSP);
        sut.getRegisters().setPC(oldPC);

        sut.executeCall(callAddress);

        assertEquals(callAddress, sut.getRegisters().getPC());
        assertEquals(sub(oldSP, 2), sut.getRegisters().getSP());
        assertEquals(oldPC, ReadShortFromMemory(sut.getRegisters().getSP()));
    }

    @Test
    public void ExecuteRet_pops_PC() {
        var pushedAddress = fixture.create(Short.TYPE);
        var sp = fixture.create(Short.TYPE);

        sut.getRegisters().setSP(sp);
        sut.getMemory().set(sp & 0xffff, getLowByte(pushedAddress));
        sut.getMemory().set(inc(sp) & 0xffff, getHighByte(pushedAddress));

        sut.executeRet();

        assertEquals(pushedAddress, sut.getRegisters().getPC());
        assertEquals(add(sp, 2), sut.getRegisters().getSP());
    }

    private short ReadShortFromMemory(short address) {
        return createShort(sut.getMemory().get(address & 0xffff), sut.getMemory().get(inc(address) & 0xffff));
    }
}
