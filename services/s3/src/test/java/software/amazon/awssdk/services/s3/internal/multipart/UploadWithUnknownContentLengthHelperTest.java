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

package software.amazon.awssdk.services.s3.internal.multipart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.services.s3.internal.multipart.MpuTestUtils.stubSuccessfulCompleteMultipartCall;
import static software.amazon.awssdk.services.s3.internal.multipart.MpuTestUtils.stubSuccessfulCreateMultipartCall;
import static software.amazon.awssdk.services.s3.internal.multipart.MpuTestUtils.stubSuccessfulUploadPartCalls;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.BlockingInputStreamAsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.testutils.RandomTempFile;

public class UploadWithUnknownContentLengthHelperTest {
    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final String UPLOAD_ID = "1234";

    // Should contain 126 parts
    private static final long MPU_CONTENT_SIZE = 1005 * 1024;
    private static final long PART_SIZE = 8 * 1024;

    private UploadWithUnknownContentLengthHelper helper;
    private S3AsyncClient s3AsyncClient;
    private static RandomTempFile testFile;

    @BeforeAll
    public static void beforeAll() throws IOException {
        testFile = new RandomTempFile("testfile.dat", MPU_CONTENT_SIZE);
    }

    @AfterAll
    public static void afterAll() throws Exception {
        testFile.delete();
    }

    @BeforeEach
    public void beforeEach() {
        s3AsyncClient = Mockito.mock(S3AsyncClient.class);
        helper = new UploadWithUnknownContentLengthHelper(s3AsyncClient, PART_SIZE, PART_SIZE, PART_SIZE * 4);
    }

    @Test
    void upload_blockingInputStream_shouldInOrder() throws FileNotFoundException {
        stubSuccessfulCreateMultipartCall(UPLOAD_ID, s3AsyncClient);
        stubSuccessfulUploadPartCalls(s3AsyncClient);
        stubSuccessfulCompleteMultipartCall(BUCKET, KEY, s3AsyncClient);

        BlockingInputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingInputStream(null);

        CompletableFuture<PutObjectResponse> future = helper.uploadObject(putObjectRequest(), body);

        body.writeInputStream(new FileInputStream(testFile));

        future.join();

        ArgumentCaptor<UploadPartRequest> requestArgumentCaptor = ArgumentCaptor.forClass(UploadPartRequest.class);
        ArgumentCaptor<AsyncRequestBody> requestBodyArgumentCaptor = ArgumentCaptor.forClass(AsyncRequestBody.class);
        int numTotalParts = 126;
        verify(s3AsyncClient, times(numTotalParts)).uploadPart(requestArgumentCaptor.capture(),
                                                   requestBodyArgumentCaptor.capture());

        List<UploadPartRequest> actualRequests = requestArgumentCaptor.getAllValues();
        List<AsyncRequestBody> actualRequestBodies = requestBodyArgumentCaptor.getAllValues();
        assertThat(actualRequestBodies).hasSize(numTotalParts);
        assertThat(actualRequests).hasSize(numTotalParts);

        for (int i = 0; i < actualRequests.size(); i++) {
            UploadPartRequest request = actualRequests.get(i);
            AsyncRequestBody requestBody = actualRequestBodies.get(i);
            assertThat(request.partNumber()).isEqualTo( i + 1);
            assertThat(request.bucket()).isEqualTo(BUCKET);
            assertThat(request.key()).isEqualTo(KEY);

            if (i == actualRequests.size() - 1) {
                assertThat(requestBody.contentLength()).hasValue(5120L);
            } else{
                assertThat(requestBody.contentLength()).hasValue(PART_SIZE);
            }
        }

        ArgumentCaptor<CompleteMultipartUploadRequest> completeMpuArgumentCaptor = ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
        verify(s3AsyncClient).completeMultipartUpload(completeMpuArgumentCaptor.capture());

        CompleteMultipartUploadRequest actualRequest = completeMpuArgumentCaptor.getValue();
        assertThat(actualRequest.multipartUpload().parts()).isEqualTo(completedParts(numTotalParts));

    }

    private static PutObjectRequest putObjectRequest() {
        return PutObjectRequest.builder()
                               .bucket(BUCKET)
                               .key(KEY)
                               .build();
    }

    private List<CompletedPart> completedParts(int totalNumParts) {
        return IntStream.range(1, totalNumParts + 1).mapToObj(i -> CompletedPart.builder().partNumber(i).build()).collect(Collectors.toList());
    }

}
