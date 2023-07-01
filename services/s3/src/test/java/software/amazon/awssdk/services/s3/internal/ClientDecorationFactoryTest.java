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
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.endpoints.S3ClientContextParams;
import software.amazon.awssdk.services.s3.internal.client.S3AsyncClientDecorator;
import software.amazon.awssdk.services.s3.internal.client.S3SyncClientDecorator;
import software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionAsyncClient;
import software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionSyncClient;
import software.amazon.awssdk.utils.AttributeMap;

public class ClientDecorationFactoryTest {

    AttributeMap.Builder clientContextParams = AttributeMap.builder();

    @ParameterizedTest
    @MethodSource("syncTestCases")

    void syncClientTest(AttributeMap clientContextParams, Class<Object> clazz, boolean isClass) {
        S3SyncClientDecorator decorator = new S3SyncClientDecorator();
        S3Client decorateClient = decorator.decorate(S3Client.create(), null, clientContextParams);
        if (isClass) {
            assertThat(decorateClient).isInstanceOf(clazz);
        } else {
            assertThat(decorateClient).isNotInstanceOf(clazz);
        }
    }

    @ParameterizedTest
    @MethodSource("asyncTestCases")
    void asyncClientTest(AttributeMap clientContextParams, Class<Object> clazz, boolean isClass) {
        S3AsyncClientDecorator decorator = new S3AsyncClientDecorator();
        S3AsyncClient decoratedClient = decorator.decorate(S3AsyncClient.create(),
                                                         null ,clientContextParams);
        if (isClass) {
            assertThat(decoratedClient).isInstanceOf(clazz);
        } else {
            assertThat(decoratedClient).isNotInstanceOf(clazz);
        }
    }


    private static Stream<Arguments> syncTestCases() {
        return Stream.of(
            Arguments.of(AttributeMap.builder().build(), S3CrossRegionSyncClient.class, false),
            Arguments.of(AttributeMap.builder().put(S3ClientContextParams.CROSS_REGION_ACCESS_ENABLED, false).build(),
                         S3CrossRegionSyncClient.class, false),
            Arguments.of(AttributeMap.builder().put(S3ClientContextParams.CROSS_REGION_ACCESS_ENABLED, true).build(),
                         S3CrossRegionSyncClient.class, true)
        );
    }

    private static Stream<Arguments> asyncTestCases() {
        return Stream.of(
            Arguments.of(AttributeMap.builder().build(),
                         S3CrossRegionAsyncClient.class,
                         false),
            Arguments.of(AttributeMap.builder().put(S3ClientContextParams.CROSS_REGION_ACCESS_ENABLED, false).build(),
                         S3CrossRegionAsyncClient.class,
                         false),
            Arguments.of(AttributeMap.builder().put(S3ClientContextParams.CROSS_REGION_ACCESS_ENABLED, true).build()
                , S3CrossRegionAsyncClient.class,
                         true)
        );
    }
}
