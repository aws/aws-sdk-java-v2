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

package software.amazon.awssdk.core.internal.useragent;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.internal.useragent.SdkUserAgent.buildSystemUserAgentString;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.util.SystemUserAgent;

class SystemUserAgentTest {

    @Test
    void when_userAgent_IsRetrived_itIsTheSameObject() {
        SystemUserAgent userAgent = SystemUserAgent.getOrCreate();
        SystemUserAgent userAgentCopy = SystemUserAgent.getOrCreate();
        assertThat(userAgent).isEqualTo(userAgentCopy);
    }

    @Test
    void when_defaultUserAgent_IsRetrived_itIsTheSameObject() {
        DefaultSystemUserAgent userAgent = DefaultSystemUserAgent.getOrCreate();
        DefaultSystemUserAgent userAgentCopy = DefaultSystemUserAgent.getOrCreate();
        assertThat(userAgent).isEqualTo(userAgentCopy);
    }

    @Test
    void when_defaultSystemUserAgent_isGenerated_itHasTheRightFormat() {
        SystemUserAgent userAgent = DefaultSystemUserAgent.getOrCreate();
        String[] userAgentFields = userAgent.userAgentString().split(" ");
        assertThat(userAgentFields.length).isGreaterThan(0);
        assertThat(Arrays.stream(userAgentFields).allMatch(field -> field.contains("/"))).isTrue();
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("inputValues")
    void when_systemAgentValues_areCustomized_resultingStringIsExpected(String description, String expected,
                                                                        SystemUserAgent systemUserAgent) {
        assertThat(systemUserAgent.userAgentString()).isEqualTo(expected);
    }

    private static Stream<Arguments> inputValues() {
        return Stream.of(
            Arguments.of("Minimal system agent",
                         "",
                         customSysAgent(null, null, null, null, null, null, null, null)),
            Arguments.of("System agent no vendor, env",
                         "aws-sdk-java/2.26.22-SNAPSHOT os/Mac_OS_X#14.6.1 lang/java#21.0.2 md/OpenJDK_64-Bit_Server_VM#21.0.2+13-LTS md/en_US md/Scala",
                         customSysAgent("2.26.22-SNAPSHOT", "Mac_OS_X#14.6.1", "java#21.0.2", "unknown",
                                        "OpenJDK_64-Bit_Server_VM#21.0.2+13-LTS", null, "en_US",
                                        Arrays.asList("Scala"))),
            Arguments.of("Maximal system agent",
                         "aws-sdk-java/2.26.22-SNAPSHOT os/Mac_OS_X#14.6.1 lang/java#21.0.2 md/OpenJDK_64-Bit_Server_VM#21.0.2+13-LTS md/vendor#Amazon.com_Inc. md/en_US md/Kotlin md/Scala exec-env/lambda",
                         customSysAgent("2.26.22-SNAPSHOT", "Mac_OS_X#14.6.1", "java#21.0.2", "lambda",
                                        "OpenJDK_64-Bit_Server_VM#21.0.2+13-LTS", "vendor#Amazon.com_Inc.", "en_US",
                                        Arrays.asList("Kotlin", "Scala")))
        );
    }

    private static SystemUserAgent customSysAgent(String sdkVersion, String osMetadata,
                                                  String langMetadata, String envMetadata, String vmMetadata,
                                                  String vendorMetadata, String languageTagMetadata,
                                                  List<String> additionalJvmLanguages) {
        return new SystemUserAgent() {
            @Override
            public String userAgentString() {
                return buildSystemUserAgentString(this);
            }

            @Override
            public String sdkVersion() {
                return sdkVersion;
            }

            @Override
            public String osMetadata() {
                return osMetadata;
            }

            @Override
            public String langMetadata() {
                return langMetadata;
            }

            @Override
            public String envMetadata() {
                return envMetadata;
            }

            @Override
            public String vmMetadata() {
                return vmMetadata;
            }

            @Override
            public String vendorMetadata() {
                return vendorMetadata;
            }

            @Override
            public Optional<String> languageTagMetadata() {
                return Optional.ofNullable(languageTagMetadata);
            }

            @Override
            public List<String> additionalJvmLanguages() {
                return additionalJvmLanguages == null ? Collections.emptyList() : additionalJvmLanguages;
            }
        };
    }
}
