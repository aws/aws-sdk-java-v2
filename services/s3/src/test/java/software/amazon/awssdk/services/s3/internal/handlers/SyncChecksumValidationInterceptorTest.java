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

package software.amazon.awssdk.services.s3.internal.handlers;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;
import static software.amazon.awssdk.core.ClientType.SYNC;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.CLIENT_TYPE;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.SERVICE_CONFIG;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.CHECKSUM_ENABLED_RESPONSE_HEADER;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.CONTENT_LENGTH_HEADER;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.ENABLE_MD5_CHECKSUM_HEADER_VALUE;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.SERVER_SIDE_ENCRYPTION_HEADER;
import static software.amazon.awssdk.services.s3.checksums.ChecksumsEnabledValidator.CHECKSUM;
import static software.amazon.awssdk.services.s3.model.ServerSideEncryption.AWS_KMS;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.core.checksums.Md5Checksum;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.checksums.ChecksumCalculatingInputStream;
import software.amazon.awssdk.services.s3.checksums.ChecksumValidatingInputStream;
import software.amazon.awssdk.services.s3.internal.handlers.SyncChecksumValidationInterceptor.ChecksumCalculatingStreamProvider;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.utils.InterceptorTestUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.internal.Base16Lower;

public class SyncChecksumValidationInterceptorTest {

    private static final byte[] CONTENT_BYTES = "CONTENT".getBytes(Charset.forName("UTF-8"));
    private static final String VALID_CHECKSUM = Base16Lower.encodeAsString(checkSumFor(CONTENT_BYTES).getChecksumBytes());
    private static final String INVALID_CHECKSUM = "3902ee7e149eb8313a34757e89e21af6";

    private SyncChecksumValidationInterceptor interceptor = new SyncChecksumValidationInterceptor();

    @Test
    public void modifyHttpContent_putObjectRequestChecksumEnabled_shouldWrapChecksumRequestBody() {
        ExecutionAttributes executionAttributes = getExecutionAttributes();
        Context.ModifyHttpRequest modifyHttpRequest =
            InterceptorTestUtils.modifyHttpRequestContext(PutObjectRequest.builder().build());
        Optional<RequestBody> requestBody = interceptor.modifyHttpContent(modifyHttpRequest,
                                                                          executionAttributes);

        assertThat(requestBody.isPresent()).isTrue();
        assertThat(executionAttributes.getAttribute(CHECKSUM)).isNotNull();
        assertThat(requestBody.get().contentStreamProvider()).isNotEqualTo(modifyHttpRequest.requestBody().get());
    }

    @Test
    public void modifyHttpContent_nonPutObjectRequest_shouldNotModify() {
        ExecutionAttributes executionAttributes = getExecutionAttributes();
        Context.ModifyHttpRequest modifyHttpRequest =
            InterceptorTestUtils.modifyHttpRequestContext(GetObjectRequest.builder().build());
        Optional<RequestBody> requestBody = interceptor.modifyHttpContent(modifyHttpRequest,
                                                                          executionAttributes);

        assertThat(requestBody).isEqualTo(modifyHttpRequest.requestBody());
    }

    @Test
    public void modifyHttpContent_putObjectRequest_checksumDisabled_shouldNotModify() {
        ExecutionAttributes executionAttributes = getExecutionAttributesWithChecksumDisabled();
        Context.ModifyHttpRequest modifyHttpRequest =
            InterceptorTestUtils.modifyHttpRequestContext(GetObjectRequest.builder().build());

        Optional<RequestBody> requestBody = interceptor.modifyHttpContent(modifyHttpRequest,
                                                                          executionAttributes);
        assertThat(requestBody).isEqualTo(modifyHttpRequest.requestBody());
    }

    @Test
    public void modifyHttpResponseContent_getObjectRequest_checksumEnabled_shouldWrapChecksumValidatingPublisher() {
        SdkHttpResponse sdkHttpResponse = getSdkHttpResponseWithChecksumHeader();
        Context.ModifyHttpResponse modifyHttpResponse =
            InterceptorTestUtils.modifyHttpResponse(GetObjectRequest.builder().build(), sdkHttpResponse);
        Optional<InputStream> publisher = interceptor.modifyHttpResponseContent(modifyHttpResponse,
                                                                                getExecutionAttributes());
        assertThat(publisher.get()).isExactlyInstanceOf(ChecksumValidatingInputStream.class);
    }

    @Test
    public void modifyHttpResponseContent_getObjectRequest_responseDoesNotContainChecksum_shouldNotModify() {
        ExecutionAttributes executionAttributes = getExecutionAttributesWithChecksumDisabled();
        SdkHttpResponse sdkHttpResponse = SdkHttpResponse.builder()
                                                         .putHeader(CONTENT_LENGTH_HEADER, "100")
                                                         .build();
        Context.ModifyHttpResponse modifyHttpResponse =
            InterceptorTestUtils.modifyHttpResponse(GetObjectRequest.builder().build(), sdkHttpResponse);
        Optional<InputStream> publisher = interceptor.modifyHttpResponseContent(modifyHttpResponse,
                                                                                executionAttributes);
        assertThat(publisher).isEqualTo(modifyHttpResponse.responseBody());
    }

    @Test
    public void modifyHttpResponseContent_nonGetObjectRequest_shouldNotModify() {
        ExecutionAttributes executionAttributes = getExecutionAttributes();
        SdkHttpResponse sdkHttpResponse = getSdkHttpResponseWithChecksumHeader();
        sdkHttpResponse.toBuilder().clearHeaders();
        Context.ModifyHttpResponse modifyHttpResponse =
            InterceptorTestUtils.modifyHttpResponse(PutObjectRequest.builder().build(), sdkHttpResponse);
        Optional<InputStream> publisher = interceptor.modifyHttpResponseContent(modifyHttpResponse,
                                                                                executionAttributes);
        assertThat(publisher).isEqualTo(modifyHttpResponse.responseBody());

    }

    @Test
    public void checksumCalculatingStreamProvider_shouldReturnNewStreamResetChecksum() throws IOException {
        List<CloseAwareStream> closeAwareStreams = new ArrayList<>();
        ContentStreamProvider underlyingStreamProvider = () -> {
            CloseAwareStream stream = new CloseAwareStream(new StringInputStream("helloWorld"));
            closeAwareStreams.add(stream);
            return stream;
        };
        SdkChecksum checksum = new Md5Checksum();
        ChecksumCalculatingStreamProvider checksumCalculatingStreamProvider =
            new ChecksumCalculatingStreamProvider(underlyingStreamProvider, checksum);

        ChecksumCalculatingInputStream currentStream = (ChecksumCalculatingInputStream) checksumCalculatingStreamProvider.newStream();
        IoUtils.drainInputStream(currentStream);
        byte[] checksumBytes = currentStream.getChecksumBytes();

        ChecksumCalculatingInputStream newStream = (ChecksumCalculatingInputStream) checksumCalculatingStreamProvider.newStream();
        assertThat(closeAwareStreams.get(0).isClosed).isTrue();
        IoUtils.drainInputStream(newStream);
        byte[] newStreamChecksumBytes = newStream.getChecksumBytes();

        assertThat(getChecksum(checksumBytes)).isEqualTo(getChecksum(newStreamChecksumBytes));
        newStream.close();
    }

    @Test
    public void afterUnmarshalling_putObjectRequest_shouldValidateChecksum() {
        SdkHttpResponse sdkHttpResponse = getSdkHttpResponseWithChecksumHeader();

        PutObjectResponse response = PutObjectResponse.builder()
                                                      .eTag(VALID_CHECKSUM)
                                                      .build();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                                            .build();

        SdkHttpRequest sdkHttpRequest = SdkHttpFullRequest.builder()
                                                          .uri(URI.create("http://localhost:8080"))
                                                          .method(SdkHttpMethod.PUT)
                                                          .build();

        Context.AfterUnmarshalling afterUnmarshallingContext =
            InterceptorTestUtils.afterUnmarshallingContext(putObjectRequest, sdkHttpRequest, response, sdkHttpResponse);

        interceptor.afterUnmarshalling(afterUnmarshallingContext, getExecutionAttributesWithChecksum());
    }

    @Test
    public void afterUnmarshalling_putObjectRequest_shouldValidateChecksum_throwExceptionIfInvalid() {
        SdkHttpResponse sdkHttpResponse = getSdkHttpResponseWithChecksumHeader();

        PutObjectResponse response = PutObjectResponse.builder()
                                                      .eTag(INVALID_CHECKSUM)
                                                      .build();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder().build();

        SdkHttpRequest sdkHttpRequest = SdkHttpFullRequest.builder()
                                                          .uri(URI.create("http://localhost:8080"))
                                                          .method(SdkHttpMethod.PUT)
                                                          .contentStreamProvider(() -> new StringInputStream("Test"))
                                                          .build();

        Context.AfterUnmarshalling afterUnmarshallingContext =
            InterceptorContext.builder()
                              .request(putObjectRequest)
                              .httpRequest(sdkHttpRequest)
                              .response(response)
                              .httpResponse(sdkHttpResponse)
                              .requestBody(RequestBody.fromString("Test"))
                              .build();

        ExecutionAttributes attributes = getExecutionAttributesWithChecksum();
        interceptor.modifyHttpContent(afterUnmarshallingContext, attributes);
        assertThatThrownBy(() -> interceptor.afterUnmarshalling(afterUnmarshallingContext, attributes))
            .hasMessageContaining("Data read has a different checksum than expected.");
    }

    @Test
    public void afterUnmarshalling_putObjectRequest_with_SSE_shouldNotValidateChecksum() {
        SdkHttpResponse sdkHttpResponse = getSdkHttpResponseWithChecksumHeader();

        PutObjectResponse response = PutObjectResponse.builder()
                                                      .eTag(INVALID_CHECKSUM)
                                                      .build();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder().build();

        SdkHttpRequest sdkHttpRequest = SdkHttpFullRequest.builder()
                                                          .putHeader(SERVER_SIDE_ENCRYPTION_HEADER, AWS_KMS.toString())
                                                          .putHeader("x-amz-server-side-encryption-aws-kms-key-id", ENABLE_MD5_CHECKSUM_HEADER_VALUE)
                                                          .uri(URI.create("http://localhost:8080"))
                                                          .method(SdkHttpMethod.PUT)
                                                          .build();

        Context.AfterUnmarshalling afterUnmarshallingContext =
            InterceptorTestUtils.afterUnmarshallingContext(putObjectRequest, sdkHttpRequest, response, sdkHttpResponse);

        interceptor.afterUnmarshalling(afterUnmarshallingContext, getExecutionAttributesWithChecksum());
    }

    private static final class CloseAwareStream extends InputStream {
        private StringInputStream inputStream;
        private boolean isClosed;

        private CloseAwareStream(StringInputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
            isClosed = true;
        }
    }

    private int getChecksum(byte[] checksumBytes) {
        ByteBuffer bb = ByteBuffer.wrap(checksumBytes);
        return bb.getInt();
    }

    private ExecutionAttributes getExecutionAttributes() {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(CLIENT_TYPE, SYNC);
        return executionAttributes;
    }

    private SdkHttpResponse getSdkHttpResponseWithChecksumHeader() {
        return SdkHttpResponse.builder()
                              .putHeader(CONTENT_LENGTH_HEADER, "100")
                              .putHeader(CHECKSUM_ENABLED_RESPONSE_HEADER, ENABLE_MD5_CHECKSUM_HEADER_VALUE)
                              .build();
    }

    private ExecutionAttributes getExecutionAttributesWithChecksumDisabled() {
        ExecutionAttributes executionAttributes = getExecutionAttributes();
        executionAttributes.putAttribute(SERVICE_CONFIG, S3Configuration.builder().checksumValidationEnabled(false).build());
        return executionAttributes;
    }

    private ExecutionAttributes getExecutionAttributesWithChecksum() {
        SdkChecksum checksum = checkSumFor(CONTENT_BYTES);
        return getExecutionAttributes().putAttribute(CHECKSUM, checksum);
    }

    private static SdkChecksum checkSumFor(byte[] bytes) {
        SdkChecksum checksum = new Md5Checksum();
        checksum.update(bytes, 0, bytes.length);
        return checksum;
    }
}
