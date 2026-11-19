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

package software.amazon.awssdk.services.s3.internal.s3express;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

class S3ExpressUtilsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void isS3ExpressBucket_bucketWithS3ExpressSuffix_returnsTrue() {
        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket("my-bucket--use1-az1--x-s3")
                                                   .key("key")
                                                   .build();
        assertThat(S3ExpressUtils.isS3ExpressBucket(request)).isTrue();
    }

    @Test
    void isS3ExpressBucket_regularBucket_returnsFalse() {
        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket("my-regular-bucket")
                                                   .key("key")
                                                   .build();
        assertThat(S3ExpressUtils.isS3ExpressBucket(request)).isFalse();
    }

    @Test
    void isS3ExpressBucket_noBucketField_returnsFalse() {
        GetObjectRequest request = GetObjectRequest.builder()
                                                   .key("key")
                                                   .build();
        assertThat(S3ExpressUtils.isS3ExpressBucket(request)).isFalse();
    }

    /**
     * Validates that the S3Express bucket suffix used in {@link S3ExpressUtils#isS3ExpressBucket} is one the endpoint
     * model tests for, and that the model tests for it the same way: as the trailing characters of the bucket name.
     *
     * <p>The model compares several trailing substrings of the bucket name ({@code --x-s3}, {@code --xa-s3}, and a
     * bare {@code --}), so this looks for the S3Express one among them rather than expecting a single such condition.
     */
    @Test
    void isS3ExpressBucket_suffixMatchesEndpointModel() throws IOException {
        String suffix = "--x-s3";
        Map<String, Integer> modelSuffixChecks = trailingBucketSuffixChecks();

        assertThat(modelSuffixChecks)
            .as("the endpoint model should compare a trailing substring of the bucket name against '%s'", suffix)
            .containsKey(suffix);
        assertThat(modelSuffixChecks.get(suffix))
            .as("the model compares the trailing characters of the bucket name against '%s', so it must compare "
                + "exactly %s of them", suffix, suffix.length())
            .isEqualTo(suffix.length());

        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket("test-bucket" + suffix)
                                                   .key("key")
                                                   .build();
        assertThat(S3ExpressUtils.isS3ExpressBucket(request))
            .as("isS3ExpressBucket should recognize the suffix '%s' from the endpoint model", suffix)
            .isTrue();
    }

    /**
     * Parses endpoint-bdd-1.json and returns every suffix the model compares the end of the bucket name against,
     * mapped to the number of trailing characters it compares.
     */
    private Map<String, Integer> trailingBucketSuffixChecks() throws IOException {
        Path modelPath = Paths.get("src/main/resources/codegen-resources/endpoint-bdd-1.json");
        assertThat(modelPath.toFile()).as("endpoint-bdd-1.json should exist").exists();

        JsonNode conditions = MAPPER.readTree(modelPath.toFile()).path("conditions");
        assertThat(conditions.isArray()).as("the endpoint BDD model should declare a conditions array").isTrue();

        Map<String, Integer> checks = new LinkedHashMap<>();
        for (JsonNode condition : conditions) {
            bucketSuffixCheck(condition).ifPresent(check -> checks.putIfAbsent(check.suffix, check.substringLength));
        }
        assertThat(checks)
            .as("the endpoint BDD model should compare the end of the bucket name against at least one suffix")
            .isNotEmpty();
        return checks;
    }

    /**
     * Matches {@code stringEquals(<a trailing substring of Bucket>, "<suffix>")}. The BDD model inlines that substring
     * into the condition; the rules model it replaced bound it to a named {@code bucketSuffix} variable first, so this
     * matches on the shape of the comparison rather than on a variable name.
     */
    private Optional<BucketSuffixCheck> bucketSuffixCheck(JsonNode condition) {
        if (!"stringEquals".equals(condition.path("fn").asText(null))) {
            return Optional.empty();
        }
        JsonNode argv = condition.path("argv");
        if (!argv.isArray() || argv.size() != 2) {
            return Optional.empty();
        }
        for (int i = 0; i < 2; i++) {
            JsonNode literal = argv.get(i);
            if (!literal.isTextual()) {
                continue;
            }
            OptionalInt substringLength = trailingBucketSubstringLength(argv.get(1 - i));
            if (substringLength.isPresent()) {
                return Optional.of(new BucketSuffixCheck(literal.asText(), substringLength.getAsInt()));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the length of the trailing substring of {@code Bucket} this expression takes, if it takes one:
     * {@code substring(Bucket, start, stop, true)}, where the trailing {@code true} means "counting from the end". The
     * call may be wrapped, in the S3 model by a {@code coalesce} that defaults to an empty string.
     */
    private OptionalInt trailingBucketSubstringLength(JsonNode node) {
        if ("substring".equals(node.path("fn").asText(null))) {
            JsonNode argv = node.path("argv");
            if (argv.isArray() && argv.size() == 4
                && "Bucket".equals(argv.get(0).path("ref").asText(null))
                && argv.get(3).asBoolean(false)) {
                return OptionalInt.of(argv.get(2).asInt() - argv.get(1).asInt());
            }
        }
        for (JsonNode child : node) {
            OptionalInt substringLength = trailingBucketSubstringLength(child);
            if (substringLength.isPresent()) {
                return substringLength;
            }
        }
        return OptionalInt.empty();
    }

    /**
     * The suffix the endpoint model compares the bucket name against, and the number of trailing characters it compares.
     */
    private static final class BucketSuffixCheck {
        private final String suffix;
        private final int substringLength;

        private BucketSuffixCheck(String suffix, int substringLength) {
            this.suffix = suffix;
            this.substringLength = substringLength;
        }
    }
}
