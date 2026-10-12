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

package software.amazon.awssdk.eventnotifications.s3.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class S3BucketTest {

    @Test
    void awsGeneratedTags_nullInput_returnsNull() {
        S3Bucket bucket = new S3Bucket("name", new UserIdentity("p"), "arn", null);
        assertThat(bucket.getAwsGeneratedTags()).isNull();
    }

    @Test
    void threeArgConstructor_leavesAwsGeneratedTagsNull() {
        S3Bucket bucket = new S3Bucket("name", new UserIdentity("p"), "arn");
        assertThat(bucket.getAwsGeneratedTags()).isNull();
    }

    @Test
    void toString_omitsAwsGeneratedTags_whenNull() {
        S3Bucket bucket = new S3Bucket("mybucket", new UserIdentity("p"), "arn:aws:s3:::mybucket");
        assertThat(bucket.toString()).doesNotContain("awsGeneratedTags");
    }

    @Test
    void toString_includesAwsGeneratedTags_whenPresent() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("aws:resource:owner", "123456789012");
        tags.put("aws:resource:region", "us-west-2");

        S3Bucket bucket = new S3Bucket("mybucket", new UserIdentity("p"), "arn:aws:s3:::mybucket", tags);
        assertThat(bucket.toString())
            .contains("awsGeneratedTags={aws:resource:owner=123456789012, aws:resource:region=us-west-2}");
    }

    @Test
    void awsGeneratedTags_isDefensivelyCopied() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("aws:resource:owner", "123456789012");

        S3Bucket bucket = new S3Bucket("mybucket", new UserIdentity("p"), "arn:aws:s3:::mybucket", tags);

        // Mutating the caller's map after construction must not affect the bucket.
        tags.put("aws:resource:region", "us-west-2");
        tags.remove("aws:resource:owner");

        assertThat(bucket.getAwsGeneratedTags())
            .containsExactly(new java.util.AbstractMap.SimpleEntry<>("aws:resource:owner", "123456789012"));
    }

    @Test
    void getAwsGeneratedTags_isUnmodifiable() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("aws:resource:owner", "123456789012");

        S3Bucket bucket = new S3Bucket("mybucket", new UserIdentity("p"), "arn:aws:s3:::mybucket", tags);

        Map<String, String> returned = bucket.getAwsGeneratedTags();
        assertThatThrownBy(() -> returned.put("aws:resource:region", "us-west-2"))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
