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

package software.amazon.awssdk.services.s3.internal.crt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.services.s3.DelegatingS3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.endpoints.S3ClientContextParams;
import software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionAsyncClient;
import software.amazon.awssdk.utils.AttributeMap;

class DefaultS3CrtAsyncClientTest {

    @Test
    void requestSignerOverrideProvided_shouldThrowException() {
        try (S3AsyncClient s3AsyncClient = S3CrtAsyncClient.builder().build()) {
            assertThatThrownBy(() -> s3AsyncClient.getObject(
                b -> b.bucket("bucket").key("key").overrideConfiguration(o -> o.signer(AwsS3V4Signer.create())),
                AsyncResponseTransformer.toBytes()).join()).hasCauseInstanceOf(UnsupportedOperationException.class);

            assertThatThrownBy(() -> s3AsyncClient.putObject(
                b -> b.bucket("bucket").key("key").overrideConfiguration(o -> o.signer(AwsS3V4Signer.create())),
                AsyncRequestBody.fromString("foobar")).join()).hasCauseInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    void clientContextParamsSetOnBuilder_propagatedToInterceptors() {
        AtomicReference<AttributeMap> clientContexParams = new AtomicReference<>();

        ExecutionInterceptor paramsCaptor = new ExecutionInterceptor() {
            @Override
            public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
                clientContexParams.set(executionAttributes.getAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS));
                throw new RuntimeException("BOOM");
            }
        };

        DefaultS3CrtAsyncClient.DefaultS3CrtClientBuilder builder =
            (DefaultS3CrtAsyncClient.DefaultS3CrtClientBuilder) S3CrtAsyncClient.builder();

        builder.addExecutionInterceptor(paramsCaptor);

        try (S3AsyncClient s3AsyncClient = builder.accelerate(false)
                                                  .forcePathStyle(true)
                                                  .build()) {

            assertThatThrownBy(s3AsyncClient.listBuckets()::join).hasMessageContaining("BOOM");
            AttributeMap attributeMap = clientContexParams.get();

            assertThat(attributeMap.get(S3ClientContextParams.ACCELERATE)).isFalse();
            assertThat(attributeMap.get(S3ClientContextParams.FORCE_PATH_STYLE)).isTrue();
            assertThat(attributeMap.get(S3ClientContextParams.USE_ARN_REGION)).isFalse();
            assertThat(attributeMap.get(S3ClientContextParams.DISABLE_MULTI_REGION_ACCESS_POINTS)).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1L})
    void invalidConfig_shouldThrowException(long value) {

        assertThatThrownBy(() -> S3AsyncClient.crtBuilder().maxConcurrency((int) value).build()).isInstanceOf(IllegalArgumentException.class)
                                                                                                .hasMessageContaining("positive");
        assertThatThrownBy(() -> S3AsyncClient.crtBuilder().initialReadBufferSizeInBytes(value).build()).isInstanceOf(IllegalArgumentException.class)
                                                                                                        .hasMessageContaining(
                                                                                                            "positive");
        assertThatThrownBy(() -> S3AsyncClient.crtBuilder().minimumPartSizeInBytes(value).build()).isInstanceOf(IllegalArgumentException.class)
                                                                                                  .hasMessageContaining(
                                                                                                      "positive");
    }

    @Test
    void crtClient_with_crossRegionAccessEnabled_asTrue(){
        S3AsyncClient crossRegionCrtClient = S3AsyncClient.crtBuilder().crossRegionAccessEnabled(true).build();
        assertThat(crossRegionCrtClient).isInstanceOf(DefaultS3CrtAsyncClient.class);
        assertThat(((DelegatingS3AsyncClient)crossRegionCrtClient).delegate()).isInstanceOf(S3CrossRegionAsyncClient.class);
    }

    @Test
    void crtClient_with_crossRegionAccessEnabled_asFalse(){
        S3AsyncClient crossRegionDisabledCrtClient = S3AsyncClient.crtBuilder().crossRegionAccessEnabled(false).build();
        assertThat(crossRegionDisabledCrtClient).isInstanceOf(DefaultS3CrtAsyncClient.class);
        assertThat(((DelegatingS3AsyncClient)crossRegionDisabledCrtClient).delegate()).isNotInstanceOf(S3CrossRegionAsyncClient.class);

        S3AsyncClient defaultCrtClient = S3AsyncClient.crtBuilder().build();
        assertThat(defaultCrtClient).isInstanceOf(DefaultS3CrtAsyncClient.class);
        assertThat(((DelegatingS3AsyncClient)defaultCrtClient).delegate()).isNotInstanceOf(S3CrossRegionAsyncClient.class);
    }

}
