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

package software.amazon.awssdk.http.crt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.testng.Assert.fail;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.platform.commons.support.ReflectionSupport;
import software.amazon.awssdk.crt.io.SocketOptions;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

class AwsCrtHttpClientBuilderTest {

    @ParameterizedTest
    @ArgumentsSource(TimeoutArgumentProvider.class)
    void createBuilder_withConnectionTimeout_updateClientTimeout(Duration timeout, int millis) throws Exception {
        SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().connectionTimeout(timeout).build();
        validateTimeoutValue(client, millis);
        client.close();
    }

    @Test
    void createBuilder_withoutConnectionTimeout_shouldUseDefaultValue() throws Exception {
        SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.create();
        int globalDefaultConnectionTimeout =
            (int) SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS.get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT).toMillis();
        validateTimeoutValue(client, globalDefaultConnectionTimeout);
        client.close();
    }

    private void validateTimeoutValue(SdkAsyncHttpClient client, int expected) throws Exception {
        Field socketOptionsField = AwsCrtAsyncHttpClient.class.getDeclaredField("socketOptions");
        SocketOptions options = (SocketOptions) ReflectionSupport.tryToReadFieldValue(socketOptionsField, client)
                                                                 .ifFailure(e -> fail("Cannot read field socketOptions from class "
                                                                                      + "AwsCrtAsyncHttpClient", e))
                                                                 .get();
        assertThat(options.connectTimeoutMs).isEqualTo(expected);
    }

    private static class TimeoutArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                arguments(Duration.ofSeconds(0), 0),
                arguments(Duration.ofSeconds(1), 1_000),
                arguments(Duration.ofSeconds(10), 10_000),
                arguments(Duration.ofDays(40), Integer.MAX_VALUE)
            );
        }
    }
}
