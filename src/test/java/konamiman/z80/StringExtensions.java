package konamiman.z80;

import java.util.Arrays;

import com.flextrade.jfixture.JFixture;
import org.junit.jupiter.api.Test;
import vavi.util.Debug;

import static konamiman.z80.utils.NumberUtils.withBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class StringExtensions {

    @Test
    void testX() {
        var a = new JFixture().create(byte[].class);
Debug.println(Arrays.toString(a));
        assertTrue(a.getClass().isArray());
    }

    @Test
    void testY() {
        var a = withBit(((byte) 0), 3, 1);
Debug.printf("%02x", a);
        assertEquals((byte) 0x08, a);
        a = withBit(((byte) 1), 3, 1);
Debug.printf("%02x", a);
        assertEquals((byte) 0x09, a);
    }
}

