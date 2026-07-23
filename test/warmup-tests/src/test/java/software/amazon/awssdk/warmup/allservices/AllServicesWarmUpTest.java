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
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;
import software.amazon.awssdk.testutils.LogCaptor;

/**
 * Tests the generated {@link SdkWarmUpProvider} of every service module on the classpath. For each provider, warm-up
 * must complete without throwing, must invoke the selected warm-up operation (checked with
 * {@link OperationRecordingInterceptor}), and must not emit SDK warn or error logs.
 *
 * <p>The {@code aws-sdk-java} aggregate artifact puts all service modules on the classpath and is updated when a new
 * service is added, so new services are tested here with no change to this module.
 */
class AllServicesWarmUpTest {

    /**
     * Providers for which {@code WarmUpOperationSelector} picked no operation because every operation is streaming,
     * event-streaming, or deprecated. Warm-up only builds and closes the client for these. The test asserts they
     * record no operation, so an entry here fails once codegen starts selecting an operation for the service.
     */
    private static final Set<String> KNOWN_NO_OP_PROVIDERS = new HashSet<>(Arrays.asList(
        //All APIs are deprecated
        "software.amazon.awssdk.services.cloudhsm.internal.crac.CloudHsmWarmUpProvider",
        "software.amazon.awssdk.services.finspacedata.internal.crac.FinspaceDataWarmUpProvider",
        "software.amazon.awssdk.services.iotthingsgraph.internal.crac.IoTThingsGraphWarmUpProvider",
        "software.amazon.awssdk.services.lexmodelbuilding.internal.crac.LexModelBuildingWarmUpProvider",
        "software.amazon.awssdk.services.proton.internal.crac.ProtonWarmUpProvider",

        // All streaming
        "software.amazon.awssdk.services.kinesisvideomedia.internal.crac.KinesisVideoMediaWarmUpProvider",
        "software.amazon.awssdk.services.sagemakerruntimehttp2.internal.crac.SageMakerRuntimeHttp2WarmUpProvider"));

    @BeforeEach
    void setUp() {
        OperationRecordingInterceptor.reset();
    }

    /**
     * All generated service providers. Generated providers live in
     * {@code software.amazon.awssdk.services.<service>.internal.crac}, so the filter excludes this module's
     * hand-written test providers in {@code software.amazon.awssdk.http.warmup}.
     */
    static Stream<Named<SdkWarmUpProvider>> generatedProviders() {
        return StreamSupport.stream(ServiceLoader.load(SdkWarmUpProvider.class).spliterator(), false)
                            .filter(p -> p.getClass().getName().startsWith("software.amazon.awssdk.services."))
                            .map(p -> Named.of(p.getClass().getSimpleName(), p));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generatedProviders")
    void warmUpClient_sync_invokesOperationWithoutErrors(SdkWarmUpProvider provider) {
        verifyWarmUp(provider, ClientType.SYNC, provider.syncClientClassName());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generatedProviders")
    void warmUpClient_async_invokesOperationWithoutErrors(SdkWarmUpProvider provider) {
        verifyWarmUp(provider, ClientType.ASYNC, provider.asyncClientClassName());
    }

    private void verifyWarmUp(SdkWarmUpProvider provider, ClientType clientType, String clientClassName) {
        // The client class name is null when the service does not generate this client type, for example an
        // async-only service has no sync client. Skip that case.
        assumeTrue(clientClassName != null,
                   () -> provider.getClass().getSimpleName() + " does not generate a " + clientType + " client");

        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            provider.warmUpClient(clientType);

            // Keep only warnings from SDK loggers and ignore the rest. We assert warm-up emits no SDK warnings,
            // not that the whole classpath is silent.
            List<String> sdkWarnings = logCaptor.loggedEvents().stream()
                                                .filter(e -> e.getLoggerName().startsWith("software.amazon.awssdk"))
                                                .map(e -> e.getLoggerName() + " - "
                                                          + e.getMessage().getFormattedMessage())
                                                .collect(Collectors.toList());
            assertThat(sdkWarnings)
                .as("%s warm-up of %s must not emit SDK warn/error logs", clientType, clientClassName)
                .isEmpty();
        }

        if (KNOWN_NO_OP_PROVIDERS.contains(provider.getClass().getName())) {
            // Once codegen selects an operation for this service, remove it from the list above.
            assertThat(OperationRecordingInterceptor.operationNames())
                .as("%s is listed in KNOWN_NO_OP_PROVIDERS but recorded an operation; remove the stale entry",
                    provider.getClass().getSimpleName())
                .isEmpty();
        } else {
            assertThat(OperationRecordingInterceptor.operationNames())
                .as("%s warm-up of %s must invoke its selected warm-up operation", clientType, clientClassName)
                .isNotEmpty();
        }
    }
}
