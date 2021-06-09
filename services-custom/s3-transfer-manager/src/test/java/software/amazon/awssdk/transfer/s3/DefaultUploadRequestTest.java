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

package software.amazon.awssdk.transfer.s3;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class DefaultUploadRequestTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void toApiRequest_usesGivenBucketAndKey() {
        String bucket = "bucket";
        String key = "key";

        UploadRequest request = UploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        assertThat(request.bucket()).isEqualTo(bucket);
        assertThat(request.key()).isEqualTo(key);
    }

    @Test
    public void upload_noRequestParamsProvided_throws() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Exactly one");

        UploadRequest.builder().build();
    }

    @Test
    public void upload_bothBucketKeyPairAndRequestProvided_throws() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Exactly one");

        UploadRequest.builder()
                .bucket("bucket")
                .key("key")
                .apiRequest(PutObjectRequest.builder().build())
                .build();
    }

}
