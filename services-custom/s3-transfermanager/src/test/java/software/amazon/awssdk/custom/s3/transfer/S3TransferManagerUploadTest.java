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

package software.amazon.awssdk.custom.s3.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class S3TransferManagerUploadTest {
    private S3CrtAsyncClient mockS3Crt;
    private S3TransferManager tm;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void methodSetup() {
        mockS3Crt = mock(S3CrtAsyncClient.class);
        tm = S3TransferManager.builder()
                .s3CrtClient(mockS3Crt)
                .build();
    }

    @After
    public void methodTeardown() {
        tm.close();
    }

    @Test
    public void upload_returnsResponse() {
        PutObjectResponse response = PutObjectResponse.builder().build();
        when(mockS3Crt.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        CompletedUpload completedUpload = tm.upload(UploadRequest.builder()
                .bucket("bucket")
                .key("key")
                .source(Paths.get("."))
                .build())
                .completionFuture()
                .join();

        assertThat(completedUpload.response()).isEqualTo(response);
    }

}
