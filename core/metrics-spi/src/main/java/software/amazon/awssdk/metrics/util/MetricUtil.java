package software.amazon.awssdk.metrics.util;

import java.time.Duration;
import java.util.concurrent.Callable;
import software.amazon.awssdk.utils.Pair;

public class MetricUtil {
    public static <T> Pair<T, Duration> measureDuration(Callable<T> c) {
        long start = System.nanoTime();

        T result;

        try {
            result = c.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Duration d = Duration.ofNanos(System.nanoTime() - start);

        return Pair.of(result, d);
    }
}
