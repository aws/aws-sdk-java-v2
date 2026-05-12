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
import java.util.ArrayList;
import java.util.List;
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
     * Validates that the S3Express bucket suffix used in {@link S3ExpressUtils#isS3ExpressBucket} matches the suffix
     * defined in the endpoint ruleset.
     */
    @Test
    void isS3ExpressBucket_suffixMatchesEndpointRuleset() throws IOException {
        String rulesetSuffix = extractBucketSuffixFromRuleset();
        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket("test-bucket" + rulesetSuffix)
                                                   .key("key")
                                                   .build();
        assertThat(S3ExpressUtils.isS3ExpressBucket(request))
            .as("isS3ExpressBucket should recognize the suffix '%s' from the endpoint ruleset", rulesetSuffix)
            .isTrue();
    }

    /**
     * Parses the endpoint-rule-set.json and extracts the S3Express bucket suffix.
     */
    private String extractBucketSuffixFromRuleset() throws IOException {
        Path rulesetPath = Paths.get("src/main/resources/codegen-resources/endpoint-rule-set.json");
        assertThat(rulesetPath.toFile()).as("endpoint-rule-set.json should exist").exists();
        JsonNode root = MAPPER.readTree(rulesetPath.toFile());
        List<String> suffixes = new ArrayList<>();
        findBucketSuffixValues(root, suffixes);
        assertThat(suffixes)
            .as("Expected exactly one bucketSuffix stringEquals check in the endpoint ruleset")
            .hasSize(1);
        return suffixes.get(0);
    }

    private void findBucketSuffixValues(JsonNode node, List<String> results) {
        if (node.isObject() && "stringEquals".equals(node.path("fn").asText(null))) {
            JsonNode argv = node.path("argv");
            if (argv.isArray() && argv.size() == 2) {
                for (int i = 0; i < 2; i++) {
                    JsonNode arg = argv.get(i);
                    JsonNode other = argv.get(1 - i);
                    if (arg.isObject() && "bucketSuffix".equals(arg.path("ref").asText(null))
                        && other.isTextual()) {
                        results.add(other.asText());
                        return;
                    }
                }
            }
        }
        for (JsonNode child : node) {
            findBucketSuffixValues(child, results);
        }
    }
}
