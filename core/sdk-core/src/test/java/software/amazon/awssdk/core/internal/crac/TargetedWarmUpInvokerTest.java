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
import static org.assertj.core.api.Assertions.assertThatCode;

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

    private static final String S3_SYNC = "software.amazon.awssdk.services.s3.S3Client";
    private static final String S3_ASYNC = "software.amazon.awssdk.services.s3.S3AsyncClient";
    private static final String DDB_SYNC = "software.amazon.awssdk.services.dynamodb.DynamoDbClient";

    @Test
    void invoke_syncClassRequested_warmsSyncTransportOnly() {
        RecordingProvider s3 = new RecordingProvider(S3_SYNC, S3_ASYNC);

        Set<ClientType> matched = invokerLoading(s3).invoke(Arrays.asList(S3_SYNC));

        assertThat(s3.warmedTypes()).containsExactly(ClientType.SYNC);
        assertThat(matched).containsExactly(ClientType.SYNC);
    }

    @Test
    void invoke_asyncClassRequested_warmsAsyncTransportOnly() {
        RecordingProvider s3 = new RecordingProvider(S3_SYNC, S3_ASYNC);

        Set<ClientType> matched = invokerLoading(s3).invoke(Arrays.asList(S3_ASYNC));

        assertThat(s3.warmedTypes()).containsExactly(ClientType.ASYNC);
        assertThat(matched).containsExactly(ClientType.ASYNC);
    }

    @Test
    void invoke_bothClassesRequested_warmsBothTransports() {
        RecordingProvider s3 = new RecordingProvider(S3_SYNC, S3_ASYNC);

        Set<ClientType> matched = invokerLoading(s3).invoke(Arrays.asList(S3_SYNC, S3_ASYNC));

        assertThat(s3.warmedTypes()).containsExactlyInAnyOrder(ClientType.SYNC, ClientType.ASYNC);
        assertThat(matched).containsExactlyInAnyOrder(ClientType.SYNC, ClientType.ASYNC);
    }

    @Test
    void invoke_classAcrossTwoProviders_warmsOnlyTheMatchingProvider() {
        RecordingProvider s3 = new RecordingProvider(S3_SYNC, S3_ASYNC);
        RecordingProvider ddb = new RecordingProvider(DDB_SYNC, null);

        invokerLoading(s3, ddb).invoke(Arrays.asList(DDB_SYNC));

        assertThat(s3.warmedTypes()).isEmpty();
        assertThat(ddb.warmedTypes()).containsExactly(ClientType.SYNC);
    }

    @Test
    void invoke_unmatchedClass_logsWarnAndDoesNotThrow() {
        RecordingProvider s3 = new RecordingProvider(S3_SYNC, S3_ASYNC);

        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            Set<ClientType> matched = invokerLoading(s3).invoke(Arrays.asList("com.example.NotAClient"));

            assertThat(matched).isEmpty();
            assertThat(s3.warmedTypes()).isEmpty();
            assertThat(logCaptor.loggedEvents())
                .anyMatch(event -> event.getLevel() == Level.WARN
                                   && event.getMessage().getFormattedMessage().contains("com.example.NotAClient"));
        }
    }

    @Test
    void invoke_whenProviderWarmUpThrows_stillReturnsMatchedTransport() {
        SdkWarmUpProvider throwing = new TestProvider(S3_SYNC, S3_ASYNC) {
            @Override
            public void warmUpClient(ClientType clientType) {
                throw new RuntimeException("boom");
            }
        };

        // A throwing warmUpClient must not stop the follow-on HTTP warm-up: the matched transport is still returned so
        // the caller can narrow the HTTP warmers.
        Set<ClientType> matched = invokerLoading(throwing).invoke(Arrays.asList(S3_SYNC));

        assertThat(matched).containsExactly(ClientType.SYNC);
    }

    @Test
    void invoke_emptyRequest_isNoOp() {
        RecordingProvider s3 = new RecordingProvider(S3_SYNC, S3_ASYNC);

        Set<ClientType> matched = invokerLoading(s3).invoke(Collections.emptyList());

        assertThat(matched).isEmpty();
        assertThat(s3.warmedTypes()).isEmpty();
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
