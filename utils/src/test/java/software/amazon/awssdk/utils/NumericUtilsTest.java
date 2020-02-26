package software.amazon.awssdk.utils;

import java.time.Duration;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static software.amazon.awssdk.utils.NumericUtils.max;
import static software.amazon.awssdk.utils.NumericUtils.min;

public class NumericUtilsTest {

    private Duration s = Duration.ofMillis(10); // short duration
    private Duration ss = Duration.ofMillis(10); // short same duration
    private Duration l = Duration.ofMillis(100); // long duration
    private Duration neg_s = Duration.ofMillis(-10); // negative short duration
    private Duration neg_ss = Duration.ofMillis(-10); // negative short same duration
    private Duration neg_l = Duration.ofMillis(-100); // negative long duration

    @Test
    public void minTestDifferentDurations() {
        assertThat(min(s, l), is(s));
    }

    @Test
    public void minTestDifferentDurationsReverse() {
        assertThat(min(l, s), is(s));
    }

    @Test
    public void minTestSameDurations() {
        assertThat(min(s, ss), is(ss));
    }

    @Test
    public void minTestDifferentNegativeDurations() {
        assertThat(min(neg_s, neg_l), is(neg_l));
    }

    @Test
    public void minTestNegativeSameDurations() {
        assertThat(min(neg_s, neg_ss), is(neg_s));
    }

    @Test
    public void maxTestDifferentDurations() {
        assertThat(max(l, s), is(l));
    }

    @Test
    public void maxTestDifferentDurationsReverse() {
        assertThat(max(s, l), is(l));
    }

    @Test
    public void maxTestSameDurations() {
        assertThat(max(s, ss), is(ss));
    }

    @Test
    public void maxTestDifferentNegativeDurations() {
        assertThat(max(neg_s, neg_l), is(neg_s));
    }

    @Test
    public void maxTestNegativeSameDurations() {
        assertThat(max(neg_s, neg_ss), is(neg_s));
    }
}