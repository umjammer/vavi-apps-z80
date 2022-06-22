package konamiman;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import com.flextrade.jfixture.JFixture;
import konamiman.DependenciesImplementations.PlainMemory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


//@Explicit
public class PlainMemoryTests {

    private int MemorySize; public int getMemorySize() { return MemorySize; } public void setMemorySize(int value) { MemorySize = value; }
    private PlainMemory Sut; public PlainMemory getSut() { return Sut; } public void setSut(PlainMemory value) { Sut = value; }
    private JFixture Fixture; public JFixture getFixture() { return Fixture; } public void setFixture(JFixture value) { Fixture = value; }
    private Random random = new Random();

    private int Random(int minValue, int maxValue)
    {
        return random.nextInt(minValue, maxValue);
    }

    @BeforeEach
    public void Setup() {
        Fixture = new JFixture();
        MemorySize = Random(100, 1000000);
        Sut = new PlainMemory(MemorySize);
    }

    @Test
    public void Can_create_instances() {
        assertNotNull(Sut);
    }

    @Test
    public void Cannot_create_memory_with_negative_size() {
        assertThrows(IllegalArgumentException.class, () -> new PlainMemory(-MemorySize));
    }

    @Test
    public void Cannot_create_memory_with_size_zero() {
        assertThrows(IllegalArgumentException.class, () -> new PlainMemory(0));
    }

    @Test
    public void Can_write_value_and_read_it_back_in_valid_address() {
        var address = Random(0, MemorySize - 1);
        var value = Fixture.create(Byte.TYPE);

        Sut.set(address, value);
        var actual = Sut.get(address);
        assertEquals(value, actual);

        value = (byte) (value ^ 255);

        Sut.set(address, value);
        actual = Sut.get(address);
        assertEquals(value, actual);
    }

    @Test
    public void Cannot_access_value_on_address_equal_to_memory_size() {
        assertThrows(IndexOutOfBoundsException.class, () -> Sut.get(MemorySize));
    }

    @Test
    public void Cannot_access_value_on_address_larger_than_memory_size() {
        assertThrows(IndexOutOfBoundsException.class, () -> Sut.get(MemorySize + 1));
    }

    @Test
    public void Cannot_access_value_on_negative_address() {
        assertThrows(IndexOutOfBoundsException.class, () -> Sut.get(-Fixture.create(Integer.TYPE)));
    }

    static byte[] toByteArray(Collection<Byte> o) {
        byte[] a = new byte[o.size()];
        int i = 0;
        for (byte b : o) a[i++] = b;
        return a;
    }

    @Test
    public void Can_set_contents_and_read_them_back_within_valid_range() {
        var data = toByteArray(Fixture.collections().createCollection(Byte.TYPE, MemorySize / 3));
        var address = Random(0, MemorySize / 3);

        Sut.SetContents(address, data, 0, null);

        var actual = Sut.getContents(address, data.length);
        assertArrayEquals(data, actual);
    }

    @Test
    public void Can_set_contents_and_read_them_back_when_touching_upper_boundary() {
        var data = toByteArray(Fixture.collections().createCollection(Byte.TYPE, MemorySize / 3));
        var address = MemorySize - data.length;

        Sut.SetContents(address, data, 0, null);

        var actual = Sut.getContents(address, data.length);
        assertArrayEquals(data, actual);
    }

    @Test
    public void Can_set_contents_from_partial_contents_of_array() {
        var data = toByteArray(Fixture.collections().createCollection(Byte.TYPE, MemorySize / 3));
        var dataStartIndex = Random(1, data.length - 1);
        var dataLength = Random(1, data.length - dataStartIndex);
        var address = Random(0, MemorySize / 3);

        Sut.SetContents(address, data, dataStartIndex, dataLength);

        var expected = Arrays.copyOfRange(data, dataStartIndex, dataStartIndex + dataLength);
        var actual = Sut.getContents(address, dataLength);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void Throws_if_setting_contents_with_wrong_startIndex_and_length_combination() {
        var data = toByteArray(Fixture.collections().createCollection(Byte.TYPE, MemorySize / 3));
        var dataStartIndex = Random(1, data.length - 1);
        var dataLength = data.length - dataStartIndex + 1;
        var address = Random(0, MemorySize / 3);

        assertThrows(IndexOutOfBoundsException.class, () -> Sut.SetContents(address, data, dataStartIndex, dataLength));
    }

    @Test
    public void Cannot_set_contents_from_null_array() {
        var address = Random(0, MemorySize / 3);

        assertThrows(NullPointerException.class, () -> Sut.SetContents(address, null, 0, null));
    }

    @Test
    public void Cannot_set_contents_specifying_negative_startIndex() {
        var data = toByteArray(Fixture.collections().createCollection(Byte.TYPE, MemorySize / 3));
        var dataStartIndex = Random(1, data.length - 1);
        var dataLength = Random(1, data.length - dataStartIndex);
        var address = Random(0, MemorySize / 3);

        assertThrows(IndexOutOfBoundsException.class, () -> Sut.SetContents(address, data, -dataStartIndex, dataLength));
    }

    @Test
    public void Can_set_contents_with_zero_length_and_nothing_changes() {
        var data = toByteArray(Fixture.collections().createCollection(Byte.TYPE, MemorySize / 3));
        var dataStartIndex = Random(1, data.length - 1);
        var address = Random(0, MemorySize / 3);

        var before = Sut.getContents(0, MemorySize);
        Sut.SetContents(address, data, dataStartIndex, /*length:*/ 0);
        var after = Sut.getContents(0, MemorySize);

        assertArrayEquals(before, after);
    }

    @Test
    public void Cannot_set_contents_beyond_memory_length() {
        var address = Random(0, MemorySize - 1);
        var data = toByteArray(Fixture.collections().createCollection(Byte.TYPE, MemorySize - address + 1));

        assertThrows(IndexOutOfBoundsException.class, () -> Sut.SetContents(address, data, 0, null));
    }

    @Test
    public void Cannot_set_contents_specifying_address_beyond_memory_length() {
        var address = MemorySize + 1;
        var data = toByteArray(Fixture.collections().createCollection(Byte.TYPE, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> Sut.SetContents(address, data, 0, null));
    }

    @Test
    public void Cannot_get_contents_beyond_memory_length() {
        var address = Random(0, MemorySize - 1);
        var length = MemorySize - address + 1;

        assertThrows(IndexOutOfBoundsException.class, () -> Sut.getContents(address, length));
    }

    @Test
    public void Cannot_get_contents_specifying_address_beyond_memory_length() {
        var address = MemorySize + 1;

        assertThrows(IndexOutOfBoundsException.class, () -> Sut.getContents(address, 1));
    }

    @Test
    public void Cannot_get_contents_specifying_negative_address() {
        var address = Random(0, MemorySize - 1);

        assertThrows(IndexOutOfBoundsException.class, () -> Sut.getContents(-address, 1));
    }

    @Test
    public void Can_get_contents_with_lenth_zero_and_empty_array_is_returned() {
        var address = Random(0, MemorySize - 1);
        var actual = Sut.getContents(address, 0);

        assertArrayEquals(new byte[0], actual);
    }
}

