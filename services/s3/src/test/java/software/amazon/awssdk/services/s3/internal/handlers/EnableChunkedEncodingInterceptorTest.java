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
import static software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute.ENABLE_CHUNKED_ENCODING;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.SERVICE_CONFIG;

import java.net.URI;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

public class EnableChunkedEncodingInterceptorTest {
    private final EnableChunkedEncodingInterceptor interceptor = new EnableChunkedEncodingInterceptor();

    @Test
    public void modifyRequest_EnablesChunckedEncoding_ForPutObectRequest() {

        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        assertThat(executionAttributes.getAttribute(ENABLE_CHUNKED_ENCODING)).isNull();

        interceptor.modifyRequest(context(PutObjectRequest.builder().build()), executionAttributes);

        assertThat(executionAttributes.getAttribute(ENABLE_CHUNKED_ENCODING)).isEqualTo(true);
    }

    @Test
    public void modifyRequest_EnablesChunckedEncoding_ForUploadPartRequest() {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        assertThat(executionAttributes.getAttribute(ENABLE_CHUNKED_ENCODING)).isNull();

        interceptor.modifyRequest(context(UploadPartRequest.builder().build()), executionAttributes);

        assertThat(executionAttributes.getAttribute(ENABLE_CHUNKED_ENCODING)).isEqualTo(true);
    }

    @Test
    public void modifyRequest_DoesNotEnableChunckedEncoding_ForGetObjectRequest() {

        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        assertThat(executionAttributes.getAttribute(ENABLE_CHUNKED_ENCODING)).isNull();

        interceptor.modifyRequest(context(GetObjectRequest.builder().build()), executionAttributes);

        assertThat(executionAttributes.getAttribute(ENABLE_CHUNKED_ENCODING)).isNull();
    }

    @Test
    public void modifyRequest_DoesNotOverwriteExistingAttributeValue() {

        ExecutionAttributes executionAttributes = new ExecutionAttributes();

        interceptor.modifyRequest(context(PutObjectRequest.builder().build()), executionAttributes);

        boolean newValue = !executionAttributes.getAttribute(ENABLE_CHUNKED_ENCODING);

        executionAttributes.putAttribute(ENABLE_CHUNKED_ENCODING, newValue);

        interceptor.modifyRequest(context(PutObjectRequest.builder().build()), executionAttributes);

        assertThat(executionAttributes.getAttribute(ENABLE_CHUNKED_ENCODING)).isEqualTo(newValue);
    }

    @Test
    public void modifyRequest_valueOnServiceConfig_TakesPrecedenceOverDefaultEnabled() {

        S3Configuration config = S3Configuration.builder()
                .chunkedEncodingEnabled(false)
                .build();

        ExecutionAttributes executionAttributes = new ExecutionAttributes()
                .putAttribute(SERVICE_CONFIG, config);

        interceptor.modifyRequest(context(PutObjectRequest.builder().build()), executionAttributes);

        assertThat(executionAttributes.getAttribute(ENABLE_CHUNKED_ENCODING)).isEqualTo(false);
    }

    @Test
    public void modifyRequest_existingValueInExecutionAttributes_TakesPrecedenceOverClientConfig() {

        boolean configValue = false;
        S3Configuration config = S3Configuration.builder()
                .chunkedEncodingEnabled(configValue)
                .build();

        ExecutionAttributes executionAttributes = new ExecutionAttributes()
                .putAttribute(SERVICE_CONFIG, config)
                .putAttribute(ENABLE_CHUNKED_ENCODING, !configValue);

        interceptor.modifyRequest(context(PutObjectRequest.builder().build()), executionAttributes);

        assertThat(executionAttributes.getAttribute(ENABLE_CHUNKED_ENCODING)).isEqualTo(!configValue);
    }

    private Context.ModifyHttpRequest context(SdkRequest request) {
        return new Context.ModifyHttpRequest() {
            @Override
            public SdkHttpRequest httpRequest() {
                return null;
            }

            @Override
            public Optional<RequestBody> requestBody() {
                return null;
            }

            @Override
            public Optional<AsyncRequestBody> asyncRequestBody() {
                return null;
            }

            @Override
            public SdkRequest request() {
                return request;
            }
        };
    }

    private SdkHttpFullRequest sdkHttpFullRequest() {
        return SdkHttpFullRequest.builder()
                                 .uri(URI.create("http://test.com:80"))
                                 .method(SdkHttpMethod.GET)
                                 .build();
    }
}
