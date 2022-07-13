package konamiman.z80;

import java.util.Arrays;
import java.util.Random;

import com.flextrade.jfixture.JFixture;
import konamiman.z80.impls.PlainMemory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dotnet4j.util.compat.CollectionUtilities.toByteArray;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


//@Explicit
public class PlainMemoryTests {

    private int memorySize;
    private PlainMemory sut;
    private JFixture fixture;

    private Random random = new Random();

    private int Random(int minValue, int maxValue)
    {
        return random.nextInt(minValue, maxValue);
    }

    @BeforeEach
    public void setup() {
        fixture = new JFixture();
        memorySize = Random(100, 1000000);
        sut = new PlainMemory(memorySize);
    }

    @Test
    public void Can_create_instances() {
        assertNotNull(sut);
    }

    @Test
    public void Cannot_create_memory_with_negative_size() {
        assertThrows(IllegalArgumentException.class, () -> new PlainMemory(-memorySize));
    }

    @Test
    public void Cannot_create_memory_with_size_zero() {
        assertThrows(IllegalArgumentException.class, () -> new PlainMemory(0));
    }

    @Test
    public void Can_write_value_and_read_it_back_in_valid_address() {
        var address = Random(0, memorySize - 1);
        var value = fixture.create(Byte.TYPE);

        sut.set(address, value);
        var actual = sut.get(address);
        assertEquals(value, actual);

        value = (byte) (value ^ 255);

        sut.set(address, value);
        actual = sut.get(address);
        assertEquals(value, actual);
    }

    @Test
    public void Cannot_access_value_on_address_equal_to_memory_size() {
        assertThrows(IndexOutOfBoundsException.class, () -> sut.get(memorySize));
    }

    @Test
    public void Cannot_access_value_on_address_larger_than_memory_size() {
        assertThrows(IndexOutOfBoundsException.class, () -> sut.get(memorySize + 1));
    }

    @Test
    public void Cannot_access_value_on_negative_address() {
        assertThrows(IndexOutOfBoundsException.class, () -> sut.get(-Math.abs(fixture.create(Integer.TYPE))));
    }

    @Test
    public void Can_set_contents_and_read_them_back_within_valid_range() {
        var data = toByteArray(fixture.collections().createCollection(Byte.TYPE, memorySize / 3));
        var address = Random(0, memorySize / 3);

        sut.setContents(address, data, 0, null);

        var actual = sut.getContents(address, data.length);
        assertArrayEquals(data, actual);
    }

    @Test
    public void Can_set_contents_and_read_them_back_when_touching_upper_boundary() {
        var data = toByteArray(fixture.collections().createCollection(Byte.TYPE, memorySize / 3));
        var address = memorySize - data.length;

        sut.setContents(address, data, 0, null);

        var actual = sut.getContents(address, data.length);
        assertArrayEquals(data, actual);
    }

    @Test
    public void Can_set_contents_from_partial_contents_of_array() {
        var data = toByteArray(fixture.collections().createCollection(Byte.TYPE, memorySize / 3));
        var dataStartIndex = Random(1, data.length - 1);
        var dataLength = Random(1, data.length - dataStartIndex);
        var address = Random(0, memorySize / 3);

        sut.setContents(address, data, dataStartIndex, dataLength);

        var expected = Arrays.copyOfRange(data, dataStartIndex, dataStartIndex + dataLength);
        var actual = sut.getContents(address, dataLength);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void Throws_if_setting_contents_with_wrong_startIndex_and_length_combination() {
        var data = toByteArray(fixture.collections().createCollection(Byte.TYPE, memorySize / 3));
        var dataStartIndex = Random(1, data.length - 1);
        var dataLength = data.length - dataStartIndex + 1;
        var address = Random(0, memorySize / 3);

        assertThrows(IndexOutOfBoundsException.class, () -> sut.setContents(address, data, dataStartIndex, dataLength));
    }

    @Test
    public void Cannot_set_contents_from_null_array() {
        var address = Random(0, memorySize / 3);

        assertThrows(NullPointerException.class, () -> sut.setContents(address, null, 0, null));
    }

    @Test
    public void Cannot_set_contents_specifying_negative_startIndex() {
        var data = toByteArray(fixture.collections().createCollection(Byte.TYPE, memorySize / 3));
        var dataStartIndex = Random(1, data.length - 1);
        var dataLength = Random(1, data.length - dataStartIndex);
        var address = Random(0, memorySize / 3);

        assertThrows(IndexOutOfBoundsException.class, () -> sut.setContents(address, data, -dataStartIndex, dataLength));
    }

    @Test
    public void Can_set_contents_with_zero_length_and_nothing_changes() {
        var data = toByteArray(fixture.collections().createCollection(Byte.TYPE, memorySize / 3));
        var dataStartIndex = Random(1, data.length - 1);
        var address = Random(0, memorySize / 3);

        var before = sut.getContents(0, memorySize);
        sut.setContents(address, data, dataStartIndex, 0);
        var after = sut.getContents(0, memorySize);

        assertArrayEquals(before, after);
    }

    @Test
    public void Cannot_set_contents_beyond_memory_length() {
        var address = Random(0, memorySize - 1);
        var data = toByteArray(fixture.collections().createCollection(Byte.TYPE, memorySize - address + 1));

        assertThrows(IndexOutOfBoundsException.class, () -> sut.setContents(address, data, 0, null));
    }

    @Test
    public void Cannot_set_contents_specifying_address_beyond_memory_length() {
        var address = memorySize + 1;
        var data = toByteArray(fixture.collections().createCollection(Byte.TYPE, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> sut.setContents(address, data, 0, null));
    }

    @Test
    public void Cannot_get_contents_beyond_memory_length() {
        var address = Random(0, memorySize - 1);
        var length = memorySize - address + 1;

        assertThrows(IndexOutOfBoundsException.class, () -> sut.getContents(address, length));
    }

    @Test
    public void Cannot_get_contents_specifying_address_beyond_memory_length() {
        var address = memorySize + 1;

        assertThrows(IndexOutOfBoundsException.class, () -> sut.getContents(address, 1));
    }

    @Test
    public void Cannot_get_contents_specifying_negative_address() {
        var address = Random(0, memorySize - 1);

        assertThrows(IndexOutOfBoundsException.class, () -> sut.getContents(-address, 1));
    }

    @Test
    public void Can_get_contents_with_lenth_zero_and_empty_array_is_returned() {
        var address = Random(0, memorySize - 1);
        var actual = sut.getContents(address, 0);

        assertArrayEquals(new byte[0], actual);
    }
}

