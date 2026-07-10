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

package software.amazon.awssdk.core.internal.crac;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;
import software.amazon.awssdk.testutils.LogCaptor;

class TargetedWarmUpInvokerTest {

    private static final String SERVICE1_SYNC = "software.amazon.awssdk.services.service1.Service1Client";
    private static final String SERVICE1_ASYNC = "software.amazon.awssdk.services.service1.Service1AsyncClient";
    private static final String SERVICE2_SYNC = "software.amazon.awssdk.services.service2.Service2Client";

    @Test
    void invoke_syncClassRequested_warmsSyncTransportOnly() {
        RecordingProvider service1 = new RecordingProvider(SERVICE1_SYNC, SERVICE1_ASYNC);

        TargetedWarmUpResult result = invokerLoading(service1).invoke(Arrays.asList(SERVICE1_SYNC));

        assertThat(service1.warmedTypes()).containsExactly(ClientType.SYNC);
        assertThat(result.matchedTransports()).containsExactly(ClientType.SYNC);
        assertThat(result.warmedClientNames()).containsExactly(SERVICE1_SYNC);
    }

    @Test
    void invoke_asyncClassRequested_warmsAsyncTransportOnly() {
        RecordingProvider service1 = new RecordingProvider(SERVICE1_SYNC, SERVICE1_ASYNC);

        TargetedWarmUpResult result = invokerLoading(service1).invoke(Arrays.asList(SERVICE1_ASYNC));

        assertThat(service1.warmedTypes()).containsExactly(ClientType.ASYNC);
        assertThat(result.matchedTransports()).containsExactly(ClientType.ASYNC);
        assertThat(result.warmedClientNames()).containsExactly(SERVICE1_ASYNC);
    }

    @Test
    void invoke_bothClassesRequested_warmsBothTransports() {
        RecordingProvider service1 = new RecordingProvider(SERVICE1_SYNC, SERVICE1_ASYNC);

        TargetedWarmUpResult result =
            invokerLoading(service1).invoke(Arrays.asList(SERVICE1_SYNC, SERVICE1_ASYNC));

        assertThat(service1.warmedTypes()).containsExactlyInAnyOrder(ClientType.SYNC, ClientType.ASYNC);
        assertThat(result.matchedTransports()).containsExactlyInAnyOrder(ClientType.SYNC, ClientType.ASYNC);
        assertThat(result.warmedClientNames()).containsExactly(SERVICE1_SYNC, SERVICE1_ASYNC);
    }

    @Test
    void invoke_classAcrossTwoProviders_warmsOnlyTheMatchingProvider() {
        RecordingProvider service1 = new RecordingProvider(SERVICE1_SYNC, SERVICE1_ASYNC);
        RecordingProvider service2 = new RecordingProvider(SERVICE2_SYNC, null);

        invokerLoading(service1, service2).invoke(Arrays.asList(SERVICE2_SYNC));

        assertThat(service1.warmedTypes()).isEmpty();
        assertThat(service2.warmedTypes()).containsExactly(ClientType.SYNC);
    }

    @Test
    void invoke_unmatchedClass_logsWarnAndDoesNotThrow() {
        RecordingProvider service1 = new RecordingProvider(SERVICE1_SYNC, SERVICE1_ASYNC);

        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            TargetedWarmUpResult result =
                invokerLoading(service1).invoke(Arrays.asList("com.example.NotAClient"));

            assertThat(result.matchedTransports()).isEmpty();
            assertThat(result.warmedClientNames()).isEmpty();
            assertThat(service1.warmedTypes()).isEmpty();
            assertThat(logCaptor.loggedEvents())
                .anyMatch(event -> event.getLevel() == Level.WARN
                                   && event.getMessage().getFormattedMessage().contains("com.example.NotAClient"));
        }
    }

    @Test
    void invoke_whenProviderWarmUpThrows_stillReturnsMatchedTransport() {
        SdkWarmUpProvider throwing = new TestProvider(SERVICE1_SYNC, SERVICE1_ASYNC) {
            @Override
            public void warmUpClient(ClientType clientType) {
                throw new RuntimeException("boom");
            }
        };

        // A throwing warmUpClient must not stop the follow-on HTTP warm-up: the matched transport is still returned so
        // the caller can narrow the HTTP warmers.
        TargetedWarmUpResult result = invokerLoading(throwing).invoke(Arrays.asList(SERVICE1_SYNC));

        assertThat(result.matchedTransports()).containsExactly(ClientType.SYNC);
    }

    @Test
    void invoke_whenProviderWarmUpThrows_doesNotReportClientWarmed() {
        SdkWarmUpProvider throwing = new TestProvider(SERVICE1_SYNC, SERVICE1_ASYNC) {
            @Override
            public void warmUpClient(ClientType clientType) {
                throw new RuntimeException("boom");
            }
        };

        TargetedWarmUpResult result = invokerLoading(throwing).invoke(Arrays.asList(SERVICE1_SYNC));

        assertThat(result.warmedClientNames()).isEmpty();
    }

    @Test
    void invoke_mixedSuccessAndFailure_reportsOnlySuccessfulClientWarmed() {
        RecordingProvider healthy = new RecordingProvider(SERVICE1_SYNC, SERVICE1_ASYNC);
        SdkWarmUpProvider throwing = new TestProvider(SERVICE2_SYNC, null) {
            @Override
            public void warmUpClient(ClientType clientType) {
                throw new RuntimeException("boom");
            }
        };

        TargetedWarmUpResult result =
            invokerLoading(healthy, throwing).invoke(Arrays.asList(SERVICE1_SYNC, SERVICE2_SYNC));

        assertThat(result.warmedClientNames()).containsExactly(SERVICE1_SYNC);
        assertThat(result.matchedTransports()).containsExactly(ClientType.SYNC);
    }

    @Test
    void invoke_emptyRequest_isNoOp() {
        RecordingProvider service1 = new RecordingProvider(SERVICE1_SYNC, SERVICE1_ASYNC);

        TargetedWarmUpResult result = invokerLoading(service1).invoke(Collections.emptyList());

        assertThat(result.matchedTransports()).isEmpty();
        assertThat(result.warmedClientNames()).isEmpty();
        assertThat(service1.warmedTypes()).isEmpty();
    }

    private TargetedWarmUpInvoker invokerLoading(SdkWarmUpProvider... providers) {
        WarmUpServiceLoader loader = new WarmUpServiceLoader() {
            @Override
            Iterator<SdkWarmUpProvider> loadProviders() {
                return Arrays.asList(providers).iterator();
            }
        };
        return new TargetedWarmUpInvoker(loader);
    }

    private static class TestProvider implements SdkWarmUpProvider {
        private final String syncName;
        private final String asyncName;

        TestProvider(String syncName, String asyncName) {
            this.syncName = syncName;
            this.asyncName = asyncName;
        }

        @Override
        public String syncClientClassName() {
            return syncName;
        }

        @Override
        public String asyncClientClassName() {
            return asyncName;
        }

        @Override
        public void warmUpClient(ClientType clientType) {
        }
    }

    private static final class RecordingProvider extends TestProvider {
        private final Set<ClientType> warmed = EnumSet.noneOf(ClientType.class);

        RecordingProvider(String syncName, String asyncName) {
            super(syncName, asyncName);
        }

        @Override
        public void warmUpClient(ClientType clientType) {
            warmed.add(clientType);
        }

        Set<ClientType> warmedTypes() {
            return warmed;
        }
    }
}
