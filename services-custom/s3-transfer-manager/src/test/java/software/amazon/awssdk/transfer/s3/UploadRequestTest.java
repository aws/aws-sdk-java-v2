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

import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class UploadRequestTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void upload_noRequestParamsProvided_throws() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("putObjectRequest");

        UploadRequest.builder()
                     .source(Paths.get("."))
                     .build();
    }

    @Test
    public void pathMissing_shouldThrow() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("source");
        UploadRequest.builder()
                     .putObjectRequest(PutObjectRequest.builder().build())
                     .build();
    }

    @Test
    public void equals_hashcode() {
        PutObjectRequest getObjectRequest = PutObjectRequest.builder()
                                                            .bucket("bucket")
                                                            .key("key")
                                                            .build();

        UploadRequest request1 = UploadRequest.builder()
                                              .putObjectRequest(b -> b.bucket("bucket").key("key"))
                                              .source(Paths.get("."))
                                              .build();

        UploadRequest request2 = UploadRequest.builder()
                                              .putObjectRequest(getObjectRequest)
                                              .source(Paths.get("."))
                                              .build();

        UploadRequest request3 = UploadRequest.builder()
                                              .putObjectRequest(b -> b.bucket("bucket1").key("key1"))
                                              .source(Paths.get("."))
                                              .build();

        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());

        assertThat(request1.hashCode()).isNotEqualTo(request3.hashCode());
        assertThat(request1).isNotEqualTo(request3);
    }

}
