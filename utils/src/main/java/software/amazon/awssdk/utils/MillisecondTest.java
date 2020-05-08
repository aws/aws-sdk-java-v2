package software.amazon.awssdk.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class MillisecondDateTest {
    
    private static final String TEST_DATE = "1.583858715232899E12";
    public static void main(String[] args) {
        Instant parsed = DateUtils.parseUnixTimestampInstant(TEST_DATE);
        ZonedDateTime parsedDateTime = ZonedDateTime.ofInstant(parsed, ZoneId.of("UTC"));

        // prints +52160-06-26T02:13:52.899Z[UTC]
        System.out.println(parsedDateTime);
    }
}
