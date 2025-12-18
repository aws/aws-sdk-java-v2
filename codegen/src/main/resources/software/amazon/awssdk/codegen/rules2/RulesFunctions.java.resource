import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
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

    private static final LazyValue<Partition> AWS_PARTITION = LazyValue.<Partition> builder()
            .initializer(RulesFunctions::findAwsPartition).build();

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

    public static List<String> split(String value, String delimiter, int limit) {
        if (limit == 1) {
            return List.of(value);
        }

        if (value.isEmpty()) {
            return List.of("");
        }

        final int delimLen = delimiter.length();
        // Unlimited split if limit == 0
        final int maxSplits = (limit == 0) ? Integer.MAX_VALUE : limit - 1;

        // Heuristic initial capacity (avoids resizing)
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

    public static String substring(String input, int start, int stop, boolean reverse) {
        int len = input.length();

        if (start >= stop || stop > len)
            return null;

        int realStart = start;
        int realStop = stop;

        if (reverse) {
            realStart = len - stop;
            realStop = len - start;
            if (realStart < 0 || realStop > len)
                return null;
        }

        int outLen = realStop - realStart;

        char[] src = input.toCharArray();
        char[] dst = new char[outLen];

        for (int i = 0; i < outLen; i++) {
            char ch = src[realStart + i];
            if ((ch & 0xFF80) != 0)
                return null;
            dst[i] = ch;
        }

        return new String(dst);
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
                if (chunkLen < 1 || chunkLen > 63) {
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
        if (length < 1 || length > 63) {
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

    private static boolean isLowerCaseAlphanumeric(char c) {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }

    // AWS related functions

    public static RulePartition awsPartition(String regionName) {
        PartitionData data = PARTITION_DATA.value();
        Partition matchedPartition;

        // Known region
        matchedPartition = data.regionMap.get(regionName);
        if (matchedPartition == null) {
            // try matching on region name pattern
            for (Partition p : data.partitions) {
                if (p.regionMatches(regionName)) {
                    matchedPartition = p;
                    break;
                }
            }
        }

        // Couldn't find the region by name or pattern matching. Fallback to 'aws' partition.
        if (matchedPartition == null) {
            matchedPartition = AWS_PARTITION.value();
        }

        return RulePartition.from(matchedPartition.id(), matchedPartition.outputs());
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
        if (index >= values.size()) {
            return null;
        }
        return values.get(index);
    }

    public static boolean awsIsVirtualHostableS3Bucket(String hostLabel, boolean allowDots) {
        // Bucket names must be between 3 (min) and 63 (max) characters long.
        int bucketLength = hostLabel == null ? 0 : hostLabel.length();
        if (bucketLength < 3 || bucketLength > 63) {
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
        return isIpAddr(hostLabel);
    }

    private static boolean isValidBucketSegmentChar(char c) {
        return isLowerCaseAlphanumeric(c) || c == '-';
    }

    public static boolean isIpAddr(String host) {
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
            partitionData.partitions.add(part);
            part.regions().forEach((name, override) -> {
                partitionData.regionMap.put(name, part);
            });
        });

        return partitionData;
    }

    // utility functions
    // this is valid ONLY when userInfo, port, query and fragment are known to be nil
    // skips much of the validation required by URI.create for this special case
    public static URI createURI(String schema, String hostAndPath) {
        try {
            int slash = -1;
            for (int i = 0, len = hostAndPath.length(); i < len; i++) {
                char c = hostAndPath.charAt(i);
                if (c == '/' && slash < 0) {
                    slash = i; // record first slash
                } else if (c == ':' || c == '@' || c == '#') {
                    return URI.create(schema + "://" + hostAndPath);
                }
            }
            if (slash < 0) {
                return new URI("https", null, hostAndPath, -1, null, null, null);
            }
            String path = (slash + 1 < hostAndPath.length()) ? hostAndPath.substring(slash) : "";
            return new URI("https", null, hostAndPath.substring(0, slash), -1, path, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private static Partition findAwsPartition() {
        return PARTITION_DATA.value().partitions.stream().filter(p -> p.id().equalsIgnoreCase("aws")).findFirst().orElse(null);
    }

    private static class PartitionData {
        private final List<Partition> partitions = new ArrayList<>();
        private final Map<String, Partition> regionMap = new HashMap<>();
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
