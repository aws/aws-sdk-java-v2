package software.amazon.awssdk.test.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DateUtilsTest {
    @Test
    public void test() {
        assertTrue("yyMMdd-hhmmss".length() == DateUtils.yyMMddhhmmss().length());
    }
}
