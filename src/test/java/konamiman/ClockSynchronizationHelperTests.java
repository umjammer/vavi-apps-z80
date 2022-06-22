package konamiman;

import com.flextrade.jfixture.JFixture;
import konamiman.DependenciesImplementations.ClockSynchronizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


//@Explicit
public class ClockSynchronizationHelperTests {

    private static final int MinMicrosecondsToWait = 10*1000;

    private ClockSynchronizer Sut; public ClockSynchronizer getSut() { return Sut; } public void setSut(ClockSynchronizer value) { Sut = value; }
    private JFixture Fixture; public JFixture getFixture() { return Fixture; } public void setFixture(JFixture value) { Fixture = value; }

    @BeforeEach
    @RepeatedTest(1000)
    public void Setup() {
        Sut = new ClockSynchronizer();
        Sut.setEffectiveClockFrequencyInMHz(1);
        Fixture = new JFixture();
    }

    @Test
    public void TryWait_works_with_repeated_short_intervals() {
        Sut.Start();

        var sw = System.currentTimeMillis();

        var totalCyclesToWait = 50000;

        for (int i = 0; i < (totalCyclesToWait / 5); i++)
            Sut.TryWait(5);

        var expected = 50;
        var actual = System.currentTimeMillis() - sw;
        assertTrue(actual >= expected - 5 && actual <= expected + 5, "Actual value: " + actual);
    }

    @Test
    public void TryWait_works_with_one_long_interval() {
        Sut.Start();

        var sw = System.currentTimeMillis();

        var totalCyclesToWait = 50000;

        Sut.TryWait(totalCyclesToWait);

        var expected = 50;
        var actual = System.currentTimeMillis() - sw;
        assertTrue(actual >= expected - 5 && actual <= expected + 5, "Actual value: " + actual);
    }

    @Test
    public void TryWait_works_repeatedly() {
        Sut.Start();

        var sw = System.currentTimeMillis();

        var totalCyclesToWait = 50000;

        Sut.TryWait(totalCyclesToWait);

        var expected = 50;
        var actual = System.currentTimeMillis() - sw;
        assertTrue(actual >= expected - 5 && actual <= expected + 5, "Actual value 1: " + actual);

        actual = System.currentTimeMillis() - sw;
        assertTrue(actual >= expected - 5 && actual <= expected + 5, "Actual value 2: " + actual);
    }
}
