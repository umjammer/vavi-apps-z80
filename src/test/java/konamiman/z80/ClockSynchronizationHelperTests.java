package konamiman.z80;

import com.flextrade.jfixture.JFixture;
import konamiman.z80.impls.ClockSynchronizerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.assertTrue;


//@Explicit
public class ClockSynchronizationHelperTests {

    private static final int MinMicrosecondsToWait = 10 * 1000;

    private ClockSynchronizerImpl sut;
    private JFixture fixture;

    final int delta = 15;

    static final int times = 10; // TODO 1000;

    @BeforeEach
    public void setup() {
        sut = new ClockSynchronizerImpl();
        sut.setEffectiveClockFrequencyInMHz(1);
        fixture = new JFixture();
    }

    @RepeatedTest(times)
    public void TryWait_works_with_repeated_short_intervals() {
        sut.start();

        var sw = System.currentTimeMillis();

        var totalCyclesToWait = 50000;

        for (int i = 0; i < (totalCyclesToWait / 5); i++)
            sut.tryWait(5);

        var expected = 50;
        var actual = System.currentTimeMillis() - sw;
        assertTrue(actual >= expected - delta && actual <= expected + delta, "Actual value: " + actual);
    }

    @RepeatedTest(times)
    public void TryWait_works_with_one_long_interval() {
        sut.start();

        var sw = System.currentTimeMillis();

        var totalCyclesToWait = 50000;

        sut.tryWait(totalCyclesToWait);

        var expected = 50;
        var actual = System.currentTimeMillis() - sw;
        assertTrue(actual >= expected - delta && actual <= expected + delta, "Actual value: " + actual);
    }

    @RepeatedTest(times)
    public void TryWait_works_repeatedly() {
        sut.start();

        var sw = System.currentTimeMillis();

        var totalCyclesToWait = 50000;

        sut.tryWait(totalCyclesToWait);

        var expected = 50;
        var actual = System.currentTimeMillis() - sw;
        assertTrue(actual >= expected - delta && actual <= expected + delta, "Actual value 1: " + actual);

        actual = System.currentTimeMillis() - sw;
        assertTrue(actual >= expected - delta && actual <= expected + delta, "Actual value 2: " + actual);
    }
}
