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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;

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
}
