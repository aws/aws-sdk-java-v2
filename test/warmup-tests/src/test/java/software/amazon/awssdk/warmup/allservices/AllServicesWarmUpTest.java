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

package software.amazon.awssdk.warmup.allservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.crac.SdkWarmUp;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;
import software.amazon.awssdk.testutils.LogCaptor;
import software.amazon.awssdk.utils.ClassLoaderHelper;

/**
 * Warms every service on the classpath through the public API: {@link SdkWarmUp#prime(Class[])} per generated
 * client, and {@link SdkWarmUp#prime()} for all of them at once.
 */
class AllServicesWarmUpTest {

    private static final String SERVICES_PACKAGE_PREFIX = "software.amazon.awssdk.services.";

    /**
     * Services with no selectable warm-up operation (all operations are streaming or deprecated); warm-up only
     * builds and closes the client.
     */
    private static final Set<String> KNOWN_NO_OP_SERVICES = new HashSet<>(Arrays.asList(
        // All APIs are deprecated
        "cloudhsm",
        "finspacedata",
        "iotthingsgraph",
        "lexmodelbuilding",
        "proton",

        // All streaming operations
        "kinesisvideomedia",
        "sagemakerruntimehttp2"));

    /**
     * Known warm-up failures. Document the reason per entry.
     */
    private static final Set<String> KNOWN_WARMUP_FAILURE_SERVICES = new HashSet<>(Collections.singletonList(
        // Endpoint rules require a real CloudFront ARN; the generated dummy ARN is rejected.
        "cloudfrontkeyvaluestore"));

    @BeforeEach
    void setUp() {
        OperationRecordingInterceptor.reset();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generatedProviders")
    void prime_syncClient_invokesOperationWithoutErrors(SdkWarmUpProvider provider) {
        verifyWarmUp(provider, provider.syncClientClassName());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generatedProviders")
    void prime_asyncClient_invokesOperationWithoutErrors(SdkWarmUpProvider provider) {
        verifyWarmUp(provider, provider.asyncClientClassName());
    }

    @Test
    void prime_withAllServicesOnClasspath_noServiceProviderFails() {
        String savedRegionProperty = System.getProperty("aws.region");
        System.setProperty("aws.region", "us-east-1");
        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            SdkWarmUp.prime();

            assertThat(serviceWarmUpFailures(logCaptor))
                .as("SdkWarmUp.prime() must not log a warm-up failure for any generated service provider")
                .isEmpty();
        } finally {
            restoreRegionProperty(savedRegionProperty);
        }

        // prime() runs once per JVM; an empty recording means it already ran and this test verified nothing.
        assertThat(OperationRecordingInterceptor.operationNames())
            .as("prime() must have invoked warm-up operations")
            .isNotEmpty();
    }

    private void verifyWarmUp(SdkWarmUpProvider provider, String clientClassName) {
        assumeTrue(clientClassName != null,
                   () -> provider.getClass().getSimpleName() + " does not generate this client type");
        assumeTrue(!KNOWN_WARMUP_FAILURE_SERVICES.contains(serviceName(provider)),
                   () -> provider.getClass().getSimpleName()
                         + " is a known warm-up failure; see KNOWN_WARMUP_FAILURE_SERVICES");

        List<String> sdkWarnings;
        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            SdkWarmUp.prime(clientClass(clientClassName));
            sdkWarnings = sdkWarnings(logCaptor);
        }

        // prime(Class) logs warm-up failures at WARN instead of throwing.
        assertThat(sdkWarnings)
            .as("Warm-up of %s must not emit SDK warn/error logs", clientClassName)
            .isEmpty();
        assertExpectedOperationRecorded(provider, clientClassName);
    }

    private void assertExpectedOperationRecorded(SdkWarmUpProvider provider, String clientClassName) {
        if (KNOWN_NO_OP_SERVICES.contains(serviceName(provider))) {
            assertThat(OperationRecordingInterceptor.operationNames())
                .as("%s is listed in KNOWN_NO_OP_SERVICES but recorded an operation; remove the stale entry",
                    provider.getClass().getSimpleName())
                .isEmpty();
        } else {
            assertThat(OperationRecordingInterceptor.operationNames())
                .as("Warm-up of %s must invoke its selected warm-up operation", clientClassName)
                .isNotEmpty();
        }
    }

    /**
     * All generated service providers; excludes this module's hand-written test providers.
     */
    static Stream<Named<SdkWarmUpProvider>> generatedProviders() {
        return serviceProviders().map(p -> Named.of(p.getClass().getSimpleName(), p));
    }

    private static Stream<SdkWarmUpProvider> serviceProviders() {
        return StreamSupport.stream(ServiceLoader.load(SdkWarmUpProvider.class).spliterator(), false)
                            .filter(p -> p.getClass().getName().startsWith(SERVICES_PACKAGE_PREFIX));
    }

    private static String serviceName(SdkWarmUpProvider provider) {
        String remainder = provider.getClass().getName().substring(SERVICES_PACKAGE_PREFIX.length());
        return remainder.substring(0, remainder.indexOf('.'));
    }

    private static Class<? extends SdkClient> clientClass(String clientClassName) {
        try {
            return ClassLoaderHelper.loadClass(clientClassName, SdkWarmUp.class).asSubclass(SdkClient.class);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Client class not on classpath: " + clientClassName, e);
        }
    }

    /**
     * Captured warnings from SDK loggers; warnings from the rest of the classpath are ignored.
     */
    private static List<String> sdkWarnings(LogCaptor logCaptor) {
        return logCaptor.loggedEvents().stream()
                        .filter(e -> e.getLoggerName().startsWith("software.amazon.awssdk"))
                        .map(e -> e.getLoggerName() + " - " + e.getMessage().getFormattedMessage())
                        .collect(Collectors.toList());
    }

    /**
     * Warm-up failures logged by {@code prime()}, excluding {@link #KNOWN_WARMUP_FAILURE_SERVICES}. prime() reports
     * failures by client class name, so known failures are matched by their client names.
     */
    private static List<String> serviceWarmUpFailures(LogCaptor logCaptor) {
        Set<String> knownFailureClients = knownWarmUpFailureClients();
        return logCaptor.loggedEvents().stream()
                        .map(e -> e.getMessage().getFormattedMessage())
                        .filter(msg -> msg.contains("software.amazon"))
                        .filter(msg -> knownFailureClients.stream().noneMatch(msg::contains))
                        .collect(Collectors.toList());
    }

    private static Set<String> knownWarmUpFailureClients() {
        return serviceProviders().filter(p -> KNOWN_WARMUP_FAILURE_SERVICES.contains(serviceName(p)))
                                 .flatMap(p -> Stream.of(p.syncClientClassName(), p.asyncClientClassName()))
                                 .filter(Objects::nonNull)
                                 .collect(Collectors.toSet());
    }

    private static void restoreRegionProperty(String savedValue) {
        if (savedValue != null) {
            System.setProperty("aws.region", savedValue);
        } else {
            System.clearProperty("aws.region");
        }
    }
}
