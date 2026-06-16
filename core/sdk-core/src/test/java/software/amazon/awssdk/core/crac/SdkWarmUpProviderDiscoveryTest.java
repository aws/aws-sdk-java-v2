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

package software.amazon.awssdk.core.crac;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link ServiceLoader} discovers a registered {@link SdkWarmUpProvider} and that its
 * {@link SdkWarmUpProvider#warmUp()} method runs.
 */
public class SdkWarmUpProviderDiscoveryTest {

    private static List<SdkWarmUpProvider> loadProviders() {
        ServiceLoader<SdkWarmUpProvider> loader = ServiceLoader.load(SdkWarmUpProvider.class);
        return StreamSupport.stream(loader.spliterator(), false).collect(Collectors.toList());
    }

    private static TestWarmUpProvider discoverTestProvider() {
        return loadProviders().stream()
                              .filter(TestWarmUpProvider.class::isInstance)
                              .map(TestWarmUpProvider.class::cast)
                              .findFirst()
                              .orElse(null);
    }

    @Test
    void load_whenProviderRegistered_discoversTestProvider() {
        assertThat(loadProviders()).anyMatch(TestWarmUpProvider.class::isInstance);
    }

    @Test
    void warmUp_whenInvokedOnDiscoveredProvider_runsTheProvider() {
        TestWarmUpProvider provider = discoverTestProvider();

        assertThat(provider).isNotNull();
        provider.warmUp();

        assertThat(provider.warmUpInvocations()).isEqualTo(1);
    }
}
