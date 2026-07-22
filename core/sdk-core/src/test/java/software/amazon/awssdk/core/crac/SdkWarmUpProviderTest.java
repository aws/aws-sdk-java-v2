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
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.testutils.LogCaptor;

/**
 * Tests the default {@link SdkWarmUpProvider#warmUp()} method, which warms the sync and async clients independently so
 * a failure warming one does not stop the other.
 */
class SdkWarmUpProviderTest {

    @Test
    void warmUp_warmsSyncThenAsync() {
        RecordingProvider provider = new RecordingProvider();

        provider.warmUp();

        assertThat(provider.warmedTypes).containsExactly(ClientType.SYNC, ClientType.ASYNC);
    }

    @Test
    void warmUp_whenSyncFails_stillWarmsAsyncAndDoesNotThrow() {
        RecordingProvider provider = new RecordingProvider() {
            @Override
            public void warmUpClient(ClientType clientType) {
                if (clientType == ClientType.SYNC) {
                    throw new RuntimeException("sync boom");
                }
                super.warmUpClient(clientType);
            }
        };

        assertThatCode(provider::warmUp).doesNotThrowAnyException();
        assertThat(provider.warmedTypes).containsExactly(ClientType.ASYNC);
    }

    @Test
    void warmUp_whenSyncFailsToLink_stillWarmsAsync() {
        RecordingProvider provider = new RecordingProvider() {
            @Override
            public void warmUpClient(ClientType clientType) {
                if (clientType == ClientType.SYNC) {
                    throw new NoClassDefFoundError("missing signer");
                }
                super.warmUpClient(clientType);
            }
        };

        assertThatCode(provider::warmUp).doesNotThrowAnyException();
        assertThat(provider.warmedTypes).containsExactly(ClientType.ASYNC);
    }

    @Test
    void warmUp_whenClientFails_logsAtWarn() {
        RecordingProvider provider = new RecordingProvider() {
            @Override
            public void warmUpClient(ClientType clientType) {
                throw new RuntimeException("boom");
            }
        };

        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            provider.warmUp();

            assertThat(logCaptor.loggedEvents())
                .anyMatch(event -> event.getLevel() == Level.WARN
                                   && event.getMessage().getFormattedMessage().contains("Warm-up failed for"));
        }
    }

    private static class RecordingProvider implements SdkWarmUpProvider {
        private final List<ClientType> warmedTypes = new ArrayList<>();

        @Override
        public String syncClientClassName() {
            return "software.amazon.awssdk.services.example.ExampleClient";
        }

        @Override
        public String asyncClientClassName() {
            return "software.amazon.awssdk.services.example.ExampleAsyncClient";
        }

        @Override
        public void warmUpClient(ClientType clientType) {
            warmedTypes.add(clientType);
        }
    }
}
