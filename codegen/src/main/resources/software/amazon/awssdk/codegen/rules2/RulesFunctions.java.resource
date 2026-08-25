import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;

@SdkInternalApi
public class RulesFunctions {
    private static final String[] ENCODED_CHARACTERS = { "+", "*", "%7E" };
    private static final String[] ENCODED_CHARACTERS_REPLACEMENTS = { "%20", "%2A", "~" };

    private static final LazyValue<PartitionData> PARTITION_DATA = LazyValue.<PartitionData> builder()
            .initializer(RulesFunctions::loadPartitionData).build();

    private static final int MAX_HOST_LABEL_SIZE = 63;
    private static final int MIN_BUCKET_SIZE = 3;

    @SafeVarargs
    public static <T> T coalesce(T... args) {
        if (args == null || args.length < 2) {
            throw new IllegalArgumentException("coalesce requires at least two arguments");
        }
        for (T arg : args) {
            if (arg != null) {
                return arg;
            }
        }
        // All preceding arguments empty, return last argument (even if empty)
        return args[args.length - 1];
    }

    @SafeVarargs
    public static String coalesce(String... args) {
        if (args == null || args.length < 2) {
            throw new IllegalArgumentException("coalesce requires at least two arguments");
        }
        for (String arg : args) {
            if (arg != null && !arg.isEmpty()) {
                return arg;
            }
        }
        return args[args.length - 1];
    }

    public static List<String> split(String value, String delimiter, int limit) {
        if (limit == 1) {
            return Collections.singletonList(value);
        }
        if (value.isEmpty()) {
            return Collections.singletonList("");
        }
        final int delimLen = delimiter.length();
        // Unlimited split if limit == 0
        final int maxSplits = (limit == 0) ? Integer.MAX_VALUE : limit - 1;
        List<String> result = new ArrayList<>(Math.min(limit, 8));
        int fromIndex = 0;
        int splits = 0;
        while (splits < maxSplits) {
            int index = value.indexOf(delimiter, fromIndex);
            if (index < 0) {
                break;
            }
            result.add(value.substring(fromIndex, index));
            fromIndex = index + delimLen;
            splits++;
        }
        // Remainder (always added)
        result.add(value.substring(fromIndex));
        return result;
    }

    public static String ite(boolean condition, String ifTrue, String ifFalse) {
        return condition ? ifTrue : ifFalse;
    }

    public static String substring(String value, int startIndex, int stopIndex, boolean reverse) {
        if (value == null) {
            return null;
        }

        int len = value.length();
        if (startIndex >= stopIndex || len < stopIndex) {
            return null;
        }

        if (!isAsciiOnly(value)) {
            return null;
        }

        if (reverse) {
            int revStart = len - stopIndex;
            int revStop = len - startIndex;
            return value.substring(revStart, revStop);
        } else {
            return value.substring(startIndex, stopIndex);
        }
    }

    /**
     * Equivalent to {@code stringEquals(coalesce(substring(value, startIndex, stopIndex, reverse), ""), literal)}
     * without allocating the intermediate substring. Called by optimized codegen.
     *
     * <p>This mirrors {@link #substring} exactly, including its rejection of any input containing a
     * character outside the 7-bit ASCII range. The positional comparison is done first because it
     * fails for almost every input; the O(n) ASCII scan is only reached once the characters already
     * match, which is precisely where {@link #substring} would have paid the same cost.
     *
     * <p>Only equivalent for a non-empty {@code literal}. When {@code startIndex == stopIndex} the
     * spec's {@code substring} returns null and the coalesce makes the comparison against {@code ""}
     * true even for a null input; codegen does not rewrite that case into this method.
     */
    public static boolean substringEquals(String value, int startIndex, int stopIndex, boolean reverse, String literal) {
        if (value == null) {
            return false;
        }

        int len = value.length();
        if (startIndex >= stopIndex || len < stopIndex) {
            return false;
        }

        int subLen = stopIndex - startIndex;
        if (literal.length() != subLen) {
            return false;
        }

        int offset = reverse ? len - stopIndex : startIndex;
        if (!value.regionMatches(offset, literal, 0, subLen)) {
            return false;
        }

        return isAsciiOnly(value);
    }

    private static boolean isAsciiOnly(String value) {
        for (int i = 0; i < value.length(); i++) {
            // non-ascii characters (values outside of the 7bit ASCII range)
            if (value.charAt(i) > 127) {
                return false;
            }
        }
        return true;
    }

    // URI related functions
    public static String uriEncode(String uri) {
        try {
            String encoded = URLEncoder.encode(uri, "UTF-8");
            for (int i = 0; i < ENCODED_CHARACTERS.length; i++) {
                encoded = encoded.replace(ENCODED_CHARACTERS[i], ENCODED_CHARACTERS_REPLACEMENTS[i]);
            }
            return encoded;
        } catch (UnsupportedEncodingException e) {
            throw SdkClientException.create("Unable to URI encode value: " + uri, e);
        }
    }

    public static RuleUrl parseURL(String url) {
        try {
            return RuleUrl.parse(url);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static boolean isValidHostLabel(String hostLabel, boolean allowDots) {
        int len = hostLabel == null ? 0 : hostLabel.length();
        if (len == 0) {
            return false;
        }

        // Single-label mode
        if (!allowDots) {
            return isValidSingleLabel(hostLabel, 0, len);
        }

        // Multi-label mode
        int start = 0;
        for (int i = 0; i <= len; i++) {
            if (i == len || hostLabel.charAt(i) == '.') {
                // chunk is hostLabel[start..i)
                int chunkLen = i - start;
                if (chunkLen < 1 || chunkLen > MAX_HOST_LABEL_SIZE) {
                    return false;
                } else if (!isValidSingleLabel(hostLabel, start, i)) {
                    return false;
                }
                start = i + 1;
            }
        }
        return true;
    }

    // Validates a single label in s[start..end): ^[A-Za-z0-9][A-Za-z0-9\-]{0,62}$
    private static boolean isValidSingleLabel(String s, int start, int end) {
        int length = end - start;
        if (length < 1 || length > MAX_HOST_LABEL_SIZE) {
            return false;
        }

        // first char must be [A-Za-z0-9]
        if (!isAlphanumeric(s.charAt(start))) {
            return false;
        }

        // remaining chars must be [A-Za-z0-9-]
        for (int i = start + 1; i < end; i++) {
            char c = s.charAt(i);
            if (!isAlphanumeric(c) && c != '-') {
                return false;
            }
        }

        return true;
    }

    private static boolean isAlphanumeric(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    /**
     * Validates a single DNS label (no dots allowed). Equivalent to isValidHostLabel(str, false)
     * but avoids the allowDots branch — called directly by optimized codegen.
     */
    public static boolean isValidHostLabelSingle(String hostLabel) {
        int len = hostLabel == null ? 0 : hostLabel.length();
        if (len == 0 || len > MAX_HOST_LABEL_SIZE) {
            return false;
        }
        return isValidSingleLabel(hostLabel, 0, len);
    }

    /**
     * Validates a multi-label DNS name (dots allowed). Equivalent to isValidHostLabel(str, true)
     * but avoids the allowDots branch — called directly by optimized codegen.
     */
    public static boolean isValidHostLabelMulti(String hostLabel) {
        int len = hostLabel == null ? 0 : hostLabel.length();
        if (len == 0) {
            return false;
        }
        int start = 0;
        for (int i = 0; i <= len; i++) {
            if (i == len || hostLabel.charAt(i) == '.') {
                int chunkLen = i - start;
                if (chunkLen < 1 || chunkLen > MAX_HOST_LABEL_SIZE) {
                    return false;
                } else if (!isValidSingleLabel(hostLabel, start, i)) {
                    return false;
                }
                start = i + 1;
            }
        }
        return true;
    }

    private static boolean isLowerCaseAlphanumeric(char c) {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }

    // AWS related functions

    /**
     * Resolves the partition a region belongs to.
     *
     * <p>{@link RulePartition} is immutable and is a pure function of the matched {@link Partition},
     * of which there are only a handful, loaded once. The instances are therefore built up front in
     * {@link #loadPartitionData()} and shared, so this method allocates nothing and holds no
     * per-call state.
     */
    public static RulePartition awsPartition(String regionName) {
        PartitionData data = PARTITION_DATA.value();

        // Known region
        RulePartition matched = data.regionMap.get(regionName);
        if (matched != null) {
            return matched;
        }

        // Try matching on region name pattern
        List<PartitionEntry> entries = data.partitions;
        for (int i = 0; i < entries.size(); i++) {
            PartitionEntry entry = entries.get(i);
            if (entry.partition.regionMatches(regionName)) {
                return entry.rulePartition;
            }
        }

        // Couldn't find the region by name or pattern matching. Fallback to 'aws' partition.
        return data.awsRulePartition;
    }

    public static RuleArn awsParseArn(String value) {
        return RuleArn.parse(value);
    }

    public static boolean stringEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }

    public static <T> T listAccess(List<T> values, int index) {
        if (values == null) {
            return null;
        }
        if (index < 0) {
            index = values.size() + index;
            if (index < 0) {
                return null;
            }
        }
        if (index >= values.size()) {
            return null;
        }
        return values.get(index);
    }

    public static boolean awsIsVirtualHostableS3Bucket(String hostLabel, boolean allowDots) {
        // Bucket names must be between 3 (min) and 63 (max) characters long.
        int bucketLength = hostLabel == null ? 0 : hostLabel.length();
        if (bucketLength < MIN_BUCKET_SIZE || bucketLength > MAX_HOST_LABEL_SIZE) {
            return false;
        }

        // Bucket names must begin and end with a letter or number.
        if (!isLowerCaseAlphanumeric(hostLabel.charAt(0)) || !isLowerCaseAlphanumeric(hostLabel.charAt(bucketLength - 1))) {
            return false;
        }

        // Bucket names can consist only of lowercase letters, numbers, periods (.), and hyphens (-).
        if (!allowDots) {
            for (int i = 1; i < bucketLength - 1; i++) { // already validated 0 and N - 1.
                if (!isValidBucketSegmentChar(hostLabel.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        // Check for consecutive dots or hyphens
        char last = hostLabel.charAt(0);
        for (int i = 1; i < bucketLength; i++) {
            char c = hostLabel.charAt(i);
            // Don't allow "bucket-.foo" or "bucket.-foo"
            if (c == '.') {
                if (last == '.' || last == '-') {
                    return false;
                }
            } else if (c == '-') {
                if (last == '.') {
                    return false;
                }
            } else if (!isLowerCaseAlphanumeric(c)) {
                return false;
            }
            last = c;
        }

        // Bucket names must not be formatted as an IP address (for example, 192.168.5.4).
        return !isIpAddr(hostLabel);
    }

    private static boolean isValidBucketSegmentChar(char c) {
        return isLowerCaseAlphanumeric(c) || c == '-';
    }

    private static boolean isIpAddr(String host) {
        if (host == null || host.length() < 2) {
            return false;
        }

        // Simple check for IPv6 (enclosed in square brackets)
        if (host.charAt(0) == '[' && host.charAt(host.length() - 1) == ']') {
            return true;
        }

        int from = 0;
        int segments = 0;
        boolean done = false;
        while (!done) {
            int index = host.indexOf('.', from);
            if (index == -1) {
                if (segments != 3) {
                    // E.g., 123.com
                    return false;
                }
                index = host.length();
                done = true;
            } else if (segments == 3) {
                // E.g., 1.2.3.4.5
                return false;
            }
            int length = index - from;
            if (length == 1) {
                char ch0 = host.charAt(from);
                if (ch0 < '0' || ch0 > '9') {
                    return false;
                }
            } else if (length == 2) {
                char ch0 = host.charAt(from);
                char ch1 = host.charAt(from + 1);
                if ((ch0 <= '0' || ch0 > '9') || (ch1 < '0' || ch1 > '9')) {
                    return false;
                }
            } else if (length == 3) {
                char ch0 = host.charAt(from);
                char ch1 = host.charAt(from + 1);
                char ch2 = host.charAt(from + 2);
                if ((ch0 <= '0' || ch0 > '9') || (ch1 < '0' || ch1 > '9') || (ch2 < '0' || ch2 > '9')) {
                    return false;
                }
                // This is a heuristic; We are intentionally not checking for the range 000-255.
            } else {
                return false;
            }
            from = index + 1;
            segments += 1;
        }
        return true;
    }

    private static PartitionData loadPartitionData() {
        PartitionDataProvider provider = new DefaultPartitionDataProvider();

        // TODO: support custom partitions.json
        Partitions partitions = provider.loadPartitions();
        PartitionData partitionData = new PartitionData();

        partitions.partitions().forEach(part -> {
            RulePartition rulePartition = RulePartition.from(part.id(), part.outputs());
            partitionData.partitions.add(new PartitionEntry(part, rulePartition));
            part.regions().forEach((name, override) -> {
                partitionData.regionMap.put(name, rulePartition);
            });
            if ("aws".equalsIgnoreCase(part.id())) {
                partitionData.awsRulePartition = rulePartition;
            }
        });

        return partitionData;
    }

    private static class PartitionData {
        private final List<PartitionEntry> partitions = new ArrayList<>();
        private final Map<String, RulePartition> regionMap = new HashMap<>();
        // Fallback used when a region matches neither a known region name nor a partition pattern.
        private RulePartition awsRulePartition;
    }

    /**
     * Pairs a {@link Partition} with its precomputed {@link RulePartition}. The former is needed to
     * evaluate the region pattern, the latter is what callers get back.
     */
    private static final class PartitionEntry {
        private final Partition partition;
        private final RulePartition rulePartition;

        private PartitionEntry(Partition partition, RulePartition rulePartition) {
            this.partition = partition;
            this.rulePartition = rulePartition;
        }
    }

    private static final class LazyValue<T> {
        private final Supplier<T> initializer;
        private T value;
        private boolean initialized;

        private LazyValue(Builder<T> builder) {
            this.initializer = builder.initializer;
        }

        public T value() {
            if (!initialized) {
                value = initializer.get();
                initialized = true;
            }
            return value;
        }

        public static <T> Builder<T> builder() {
            return new Builder<>();
        }

        public static class Builder<T> {
            private Supplier<T> initializer;

            public Builder<T> initializer(Supplier<T> initializer) {
                this.initializer = initializer;
                return this;
            }

            public LazyValue<T> build() {
                return new LazyValue<>(this);
            }
        }
    }
}
