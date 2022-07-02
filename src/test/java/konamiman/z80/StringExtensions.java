package konamiman.z80;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.flextrade.jfixture.JFixture;
import com.sun.source.tree.AssertTree;
import org.junit.jupiter.api.Test;
import vavi.util.Debug;

import static konamiman.z80.utils.NumberUtils.withBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class StringExtensions {

    static byte[] toByteArray(Collection<Byte> o) {
        byte[] a = new byte[o.size()];
        int i = 0;
        for (byte b : o) a[i++] = b;
        return a;
    }

    public static <K, V> Map<K, V> toMap(String[] ss, Function<String, K> k, Function<String, V> v) {
        Map<K, V> map = new HashMap<>();
        for (String s : ss) {
            map.put(k.apply(s), v.apply(s));
        }
        return map;
    }

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

