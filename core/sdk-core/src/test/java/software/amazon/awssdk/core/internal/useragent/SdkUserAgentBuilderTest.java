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
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.APP_ID;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.HTTP;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.INTERNAL_METADATA_MARKER;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.IO;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.RETRY_MODE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.util.SystemUserAgent;

class SdkUserAgentBuilderTest {

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("inputValues")
    void sdkUserAgentStringValidation(String description, String expected, SdkClientUserAgentProperties requestUserAgent,
                                      SystemUserAgent systemUserAgent) {
        String userAgent = SdkUserAgentBuilder.buildClientUserAgentString(systemUserAgent, requestUserAgent);
        assertThat(userAgent).isEqualTo(expected);
    }

    private static Stream<Arguments> inputValues() {
        SystemUserAgent standardValuesSysAgent =
            customSysAgent("2.26.22-SNAPSHOT", "Mac_OS_X#14.6.1", "java#21.0.2", null,
                           "OpenJDK_64-Bit_Server_VM#21.0.2+13-LTS", null, "en_US", null);
        SystemUserAgent maximalSysAgent =
            customSysAgent("2.26.22-SNAPSHOT", "Mac_OS_X#14.6.1", "java#21.0.2", "lambda",
                           "OpenJDK_64-Bit_Server_VM#21.0.2+13-LTS", "vendor#Amazon.com_Inc.", "en_US",
                           Arrays.asList("Kotlin", "Scala"));

        SdkClientUserAgentProperties minimalProperties = sdkProperties(null, null, null, null, null);
        SdkClientUserAgentProperties maximalProperties = sdkProperties("standard", "arbitrary", "async", "Netty", "someAppId");

        return Stream.of(
            Arguments.of("default sysagent, empty requestvalues",
                         "aws-sdk-java/2.26.22-SNAPSHOT ua/2.0 os/Mac_OS_X#14.6.1 lang/java#21.0.2 md/OpenJDK_64-Bit_Server_VM#21.0.2+13-LTS md/en_US",
                         minimalProperties,
                         standardValuesSysAgent),
            Arguments.of("standard sysagent, request values - retry",
                         "aws-sdk-java/2.26.22-SNAPSHOT ua/2.0 os/Mac_OS_X#14.6.1 lang/java#21.0.2 "
                         + "md/OpenJDK_64-Bit_Server_VM#21.0.2+13-LTS md/vendor#Amazon.com_Inc. md/en_US md/Kotlin md/Scala "
                         + "exec-env/lambda cfg/retry-mode#standard",
                         sdkProperties("standard", null, null, null, null),
                         maximalSysAgent),
            Arguments.of("standard sysagent, request values - internalMarker",
                         "aws-sdk-java/2.26.22-SNAPSHOT md/internal ua/2.0 os/Mac_OS_X#14.6.1 lang/java#21.0.2 "
                         + "md/OpenJDK_64-Bit_Server_VM#21.0.2+13-LTS md/vendor#Amazon.com_Inc. md/en_US md/Kotlin md/Scala exec-env/lambda",
                         sdkProperties(null, "arbitrary", null, null, null),
                         maximalSysAgent),
            Arguments.of("standard sysagent, request values - io",
                         "aws-sdk-java/2.26.22-SNAPSHOT md/io#async ua/2.0 os/Mac_OS_X#14.6.1 lang/java#21.0.2 "
                         + "md/OpenJDK_64-Bit_Server_VM#21.0.2+13-LTS md/vendor#Amazon.com_Inc. md/en_US md/Kotlin md/Scala exec-env/lambda",
                         sdkProperties(null, null, "async", null, null),
                         maximalSysAgent),
            Arguments.of("standard sysagent, request values - http",
                         "aws-sdk-java/2.26.22-SNAPSHOT md/http#Apache ua/2.0 os/Mac_OS_X#14.6.1 lang/java#21.0.2 "
                         + "md/OpenJDK_64-Bit_Server_VM#21.0.2+13-LTS md/vendor#Amazon.com_Inc. md/en_US md/Kotlin md/Scala exec-env/lambda",
                         sdkProperties(null, null, null, "Apache", null),
                         maximalSysAgent),
            Arguments.of("standard sysagent, request values - authSource",
                         "aws-sdk-java/2.26.22-SNAPSHOT ua/2.0 os/Mac_OS_X#14.6.1 lang/java#21.0.2 "
                         + "md/OpenJDK_64-Bit_Server_VM#21.0.2+13-LTS md/vendor#Amazon.com_Inc. md/en_US md/Kotlin md/Scala "
                         + "exec-env/lambda",
                         sdkProperties(null, null, null, null, null),
                         maximalSysAgent),
            Arguments.of("standard sysagent, request values - appId",
                         "aws-sdk-java/2.26.22-SNAPSHOT ua/2.0 os/Mac_OS_X#14.6.1 lang/java#21.0.2 "
                         + "md/OpenJDK_64-Bit_Server_VM#21.0.2+13-LTS md/vendor#Amazon.com_Inc. md/en_US md/Kotlin md/Scala "
                         + "exec-env/lambda app/someAppId",
                         sdkProperties(null, null, null, null, "someAppId"),
                         maximalSysAgent),
            Arguments.of("standard sysagent, request values - maximal",
                         "aws-sdk-java/2.26.22-SNAPSHOT md/io#async md/http#Netty md/internal ua/2.0 os/Mac_OS_X#14.6.1 "
                         + "lang/java#21.0.2 "
                         + "md/OpenJDK_64-Bit_Server_VM#21.0.2+13-LTS md/vendor#Amazon.com_Inc. md/en_US md/Kotlin md/Scala "
                         + "exec-env/lambda cfg/retry-mode#standard app/someAppId",
                         maximalProperties,
                         maximalSysAgent)
            );
    }

    private static SdkClientUserAgentProperties sdkProperties(String retryMode, String internalMarker, String io,
                                                              String http, String appId) {
        SdkClientUserAgentProperties properties = new SdkClientUserAgentProperties();

        if (retryMode != null) {
            properties.putProperty(RETRY_MODE, retryMode);
        }

        if (internalMarker != null) {
            properties.putProperty(INTERNAL_METADATA_MARKER, internalMarker);
        }

        if (io != null) {
            properties.putProperty(IO, io);
        }

        if (http != null) {
            properties.putProperty(HTTP, http);
        }

        if (appId != null) {
            properties.putProperty(APP_ID, appId);
        }

        return properties;
    }

    private static SystemUserAgent customSysAgent(String sdkVersion, String osMetadata,
                                                  String langMetadata, String envMetadata, String vmMetadata,
                                                  String vendorMetadata, String languageTagMetadata,
                                                  List<String> additionalJvmLanguages) {
        return new SystemUserAgent() {
            @Override
            public String userAgentString() {
                return null;
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
