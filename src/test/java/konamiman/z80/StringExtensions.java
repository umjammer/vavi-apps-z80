package konamiman.z80;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


public class StringExtensions {

    public static byte asBinaryByte(String binaryString) {
        return (byte) Integer.parseInt(binaryString.replace(" ", ""), 2);
    }

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
}

