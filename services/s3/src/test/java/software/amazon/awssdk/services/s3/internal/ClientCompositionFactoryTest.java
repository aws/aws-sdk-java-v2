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

package software.amazon.awssdk.services.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.client.S3ClientComposer;
import software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionAsyncClient;
import software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionSyncClient;

public class ClientCompositionFactoryTest {

    @ParameterizedTest
    @MethodSource("syncTestCases")
    void syncClientTest(Consumer<S3Configuration.Builder> s3ConfigSettings, Class<Object> clazz, boolean isClass) {
        S3Client composedClient = S3ClientComposer.composeSync(S3Client.create(),
                                                               clientConfigWithServiceConfig(s3ConfigSettings));
        if (isClass) {
            assertThat(composedClient).isInstanceOf(clazz);
        } else {
            assertThat(composedClient).isNotInstanceOf(clazz);
        }
    }

    @ParameterizedTest
    @MethodSource("asyncTestCases")
    void asyncClientTest(Consumer<S3Configuration.Builder> s3ConfigSettings, Class<Object> clazz, boolean isClass) {
        S3AsyncClient composedClient = S3ClientComposer.composeAsync(S3AsyncClient.create(),
                                                                     clientConfigWithServiceConfig(s3ConfigSettings));
        if (isClass) {
            assertThat(composedClient).isInstanceOf(clazz);
        } else {
            assertThat(composedClient).isNotInstanceOf(clazz);
        }
    }

    private SdkClientConfiguration clientConfigWithServiceConfig(Consumer<S3Configuration.Builder> s3ConfigSettings) {
        S3Configuration s3Configuration = S3Configuration.builder().applyMutation(s3ConfigSettings).build();
        return SdkClientConfiguration.builder().option(SdkClientOption.SERVICE_CONFIGURATION, s3Configuration).build();
    }

    private static Stream<Arguments> syncTestCases() {
        return Stream.of(
            Arguments.of((Consumer<S3Configuration.Builder>) c -> {}, S3CrossRegionSyncClient.class, false),
            Arguments.of((Consumer<S3Configuration.Builder>) c -> c.crossRegionAccessEnabled(false), S3CrossRegionSyncClient.class, false),
            Arguments.of((Consumer<S3Configuration.Builder>) c -> c.crossRegionAccessEnabled(true), S3CrossRegionSyncClient.class, true)
        );
    }

    private static Stream<Arguments> asyncTestCases() {
        return Stream.of(
            Arguments.of((Consumer<S3Configuration.Builder>) c -> {}, S3CrossRegionAsyncClient.class, false),
            Arguments.of((Consumer<S3Configuration.Builder>) c -> c.crossRegionAccessEnabled(false), S3CrossRegionAsyncClient.class, false),
            Arguments.of((Consumer<S3Configuration.Builder>) c -> c.crossRegionAccessEnabled(true), S3CrossRegionAsyncClient.class, true)
        );
    }
}
