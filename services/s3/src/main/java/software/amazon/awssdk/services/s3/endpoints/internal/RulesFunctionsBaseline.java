/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.s3.endpoints.internal;

// PRE optimized RulesFunctions
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;

@SdkInternalApi
public class RulesFunctionsBaseline {
    private static final Pattern VALID_HOST_LABEL_SUBDOMAINS = Pattern.compile("[a-zA-Z\\d][a-zA-Z\\d\\-.]{0,62}");
    private static final Pattern VALID_HOST_LABEL = Pattern.compile("[a-zA-Z\\d][a-zA-Z\\d\\-]{0,62}");

    private static final Pattern VIRTUAL_HOSTABLE_BUCKET = Pattern.compile("[a-z\\d][a-z\\d\\-.]{1,61}[a-z\\d]");
    private static final Pattern VIRTUAL_HOSTABLE_BUCKET_NO_SUBDOMAINS = Pattern.compile("[a-z\\d][a-z\\d\\-]{1,61}[a-z\\d]");
    private static final Pattern NO_IPS = Pattern.compile("(\\d+\\.){3}\\d+");
    private static final Pattern NO_CONSECUTIVE_DASH_OR_DOTS = Pattern.compile(".*[.-]{2}.*");

    private static final String[] ENCODED_CHARACTERS = { "+", "*", "%7E" };
    private static final String[] ENCODED_CHARACTERS_REPLACEMENTS = { "%20", "%2A", "~" };

    private static final LazyValue<PartitionData> PARTITION_DATA = LazyValue.<PartitionData> builder()
                                                                            .initializer(RulesFunctionsBaseline::loadPartitionData).build();

    private static final LazyValue<Partition> AWS_PARTITION = LazyValue.<Partition> builder()
                                                                       .initializer(RulesFunctionsBaseline::findAwsPartition).build();

    public static String substring(String input, int start, int stop, boolean reverse) {
        int len = input.length();
        if (start >= stop || len < stop) {
            return null;
        }
        int realStart = start;
        int realStop = stop;
        if (reverse) {
            realStart = len - stop;
            realStop = len - start;
        }
        StringBuilder result = new StringBuilder(realStop - realStart);
        for (int idx = realStart; idx < realStop; idx++) {
            char ch = input.charAt(idx);
            if (ch > 0x7F) {
                return null;
            }
            result.append(ch);
        }
        return result.toString();
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

    public static boolean isValidHostLabel(String value, boolean allowSubDomains) {
        if (allowSubDomains) {
            return VALID_HOST_LABEL_SUBDOMAINS.matcher(value).matches();
        }
        return VALID_HOST_LABEL.matcher(value).matches();
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
        return parseArn(value);
    }

    private static RuleArn parseArn(String arn) {
        String[] base = arn.split(":", 6);
        if (base.length != 6) {
            return null;
        }
        // service, resource and `arn` may not be null
        if (!base[0].equals("arn")) {
            return null;
        }
        if (base[1].isEmpty() || base[2].isEmpty()) {
            return null;
        }
        if (base[5].isEmpty()) {
            return null;
        }
        return new RuleArn(base[1], base[2], base[3], base[4], Arrays.asList(base[5].split("[:/]", -1)));
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

    public static boolean awsIsVirtualHostableS3Bucket(String hostLabel, boolean allowSubDomains) {
        if (allowSubDomains) {
            return VIRTUAL_HOSTABLE_BUCKET.matcher(hostLabel).matches()
                   // don't allow ip address
                   && !NO_IPS.matcher(hostLabel).matches() && !NO_CONSECUTIVE_DASH_OR_DOTS.matcher(hostLabel).matches();
        }
        return VIRTUAL_HOSTABLE_BUCKET_NO_SUBDOMAINS.matcher(hostLabel).matches();
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
