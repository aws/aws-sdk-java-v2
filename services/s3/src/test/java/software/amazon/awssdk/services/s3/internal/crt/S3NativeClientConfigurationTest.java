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

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.crt.S3CrtHttpConfiguration;
import software.amazon.awssdk.testutils.LogCaptor;

class S3NativeClientConfigurationTest {

    private static final String SSL_WARNING = "SSL Certificate verification is disabled.";

    @Test
    void build_whenTrustAllCertificatesTrue_shouldLogWarning() {
        try (LogCaptor logCaptor = LogCaptor.create();
             S3NativeClientConfiguration config = buildConfig(
                 S3CrtHttpConfiguration.builder().trustAllCertificatesEnabled(true).build())) {

            assertThat(logCaptor.loggedEvents())
                .anySatisfy(event -> assertThat(event.getMessage().getFormattedMessage()).contains(SSL_WARNING));
        }
    }

    @ParameterizedTest
    @MethodSource("noWarningConfigurations")
    void build_whenTrustAllCertificatesNotTrue_shouldNotLogWarning(S3CrtHttpConfiguration httpConfig) {
        try (LogCaptor logCaptor = LogCaptor.create();
             S3NativeClientConfiguration config = buildConfig(httpConfig)) {

            assertThat(logCaptor.loggedEvents())
                .noneSatisfy(event -> assertThat(event.getMessage().getFormattedMessage()).contains(SSL_WARNING));
        }
    }

    private static Stream<Arguments> noWarningConfigurations() {
        return Stream.of(
            Arguments.of(S3CrtHttpConfiguration.builder().trustAllCertificatesEnabled(false).build()),
            Arguments.of(S3CrtHttpConfiguration.builder().build()),
            Arguments.of((S3CrtHttpConfiguration) null)
        );
    }

    private S3NativeClientConfiguration buildConfig(S3CrtHttpConfiguration httpConfig) {
        S3NativeClientConfiguration.Builder builder =
            S3NativeClientConfiguration.builder()
                                       .signingRegion("us-east-1")
                                       .credentialsProvider(
                                           StaticCredentialsProvider.create(
                                               AwsBasicCredentials.create("foo", "bar")));
        if (httpConfig != null) {
            builder.httpConfiguration(httpConfig);
        }
        return builder.build();
    }
}
