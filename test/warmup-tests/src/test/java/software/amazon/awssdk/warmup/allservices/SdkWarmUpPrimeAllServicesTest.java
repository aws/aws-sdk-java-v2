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

import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.crac.SdkWarmUp;
import software.amazon.awssdk.testutils.LogCaptor;

/**
 * Tests the customer entry point {@link SdkWarmUp#prime()} with all service modules on the classpath.
 *
 * <p>{@code prime()} catches provider failures and logs them at WARN
 * ({@code "Warm-up failed for <class> and was skipped."}), so the captured logs are the only failure signal here.
 * {@link AllServicesWarmUpTest} checks each provider directly.
 *
 * <p>The assertion looks only at warnings that name a generated service provider. The HTTP-client warm-up step of
 * {@code prime()} logs the same "Warm-up failed" message when an HTTP client fails to build (for example the CRT
 * native library is not available), and that must not fail this test. Network failures of the HTTP GET are logged at
 * debug, so this test passes offline.
 */
class SdkWarmUpPrimeAllServicesTest {

    private String savedRegionProperty;

    @BeforeEach
    void setUp() {
        savedRegionProperty = System.getProperty("aws.region");
        System.setProperty("aws.region", "us-east-1");
    }

    @AfterEach
    void tearDown() {
        if (savedRegionProperty != null) {
            System.setProperty("aws.region", savedRegionProperty);
        } else {
            System.clearProperty("aws.region");
        }
    }

    @Test
    void prime_withAllServicesOnClasspath_noServiceProviderFails() {
        OperationRecordingInterceptor.reset();
        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            SdkWarmUp.prime();

            List<String> serviceWarmUpFailures =
                logCaptor.loggedEvents().stream()
                         .map(e -> e.getMessage().getFormattedMessage())
                         .filter(msg -> msg.contains("Warm-up failed") || msg.contains("could not be loaded"))
                         .filter(msg -> msg.contains("software.amazon.awssdk.services."))
                         .collect(Collectors.toList());

            assertThat(serviceWarmUpFailures)
                .as("SdkWarmUp.prime() must not log a warm-up failure for any generated service provider")
                .isEmpty();
        }

        // prime() runs once per JVM. If another test in this module called prime() first, this test would see no log
        // events and no recorded operations and pass without checking anything. This assertion catches that case.
        assertThat(OperationRecordingInterceptor.operationNames())
            .as("prime() must have invoked warm-up operations; if empty, prime() already ran earlier in this JVM "
                + "and this test verified nothing")
            .isNotEmpty();
    }
}
