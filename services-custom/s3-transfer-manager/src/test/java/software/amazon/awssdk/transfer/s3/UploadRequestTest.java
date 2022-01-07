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

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class UploadRequestTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void upload_noRequestParamsProvided_throws() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("putObjectRequest");

        UploadRequest.builder()
                     .requestBody(AsyncRequestBody.fromString("foo"))
                     .build();
    }

    @Test
    public void bodyMissing_shouldThrow() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("requestBody");
        UploadRequest.builder()
                     .putObjectRequest(PutObjectRequest.builder().build())
                     .build();
    }

    @Test
    public void bodyEqualsGivenBody() {
        AsyncRequestBody requestBody = AsyncRequestBody.fromString("foo");
        UploadRequest request = UploadRequest.builder()
                                             .putObjectRequest(b -> b.bucket("bucket").key("key"))
                                             .requestBody(requestBody)
                                             .build();
        assertThat(request.requestBody()).isSameAs(requestBody);
    }

    @Test
    public void null_requestBody_shouldThrowException() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("requestBody");
        UploadRequest.builder()
                     .putObjectRequest(b -> b.bucket("bucket").key("key"))
                     .requestBody(null)
                     .build();
    }

    @Test
    public void equals_hashcode() {
        EqualsVerifier.forClass(UploadRequest.class)
                      .withNonnullFields("requestBody", "putObjectRequest")
                      .verify();
    }

}
