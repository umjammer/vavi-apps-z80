package konamiman;

public class StringExtensions {

    public static byte AsBinaryByte(String binaryString) {
        return (byte) Integer.parseInt(binaryString.replace(" ", ""), 2);
    }
}

