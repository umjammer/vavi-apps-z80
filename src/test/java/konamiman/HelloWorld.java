package konamiman;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class HelloWorld {

    @Test
    public void HelloWorldTest() {
        var Sut = new Z80Processor();
        Sut.setAutoStopOnRetWithStackEmpty(true);

        var program = new byte[] {
                0x3E, 0x07,        // LD A,7
                (byte) 0xC6, 0x04, // ADD A,4
                0x3C,              // INC A
                (byte) 0xC9        // RET
        };
        Sut.getMemory().SetContents(0, program, 0, null);
        // hola

        Sut.Start(null);

        assertEquals(12, Sut.getRegisters().getA());
        assertEquals(28, Sut.getTStatesElapsedSinceStart());
    }
}

