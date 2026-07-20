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

package software.amazon.awssdk.crac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.internal.crac.ProtocolRestJsonWarmUpProvider;

/**
 * Verifies the generated {@code SdkWarmUpProvider} invokes the operation selected by
 * {@code WarmUpOperationSelector}. {@link OperationRecordingInterceptor} is registered as a global interceptor to
 * observe calls made by the clients the provider builds internally.
 */
class WarmUpProviderBindingTest {

    /**
     * The operation WarmUpOperationSelector picks for the ProtocolRestJson test model.
     */
    private static final String EXPECTED_OPERATION = "AllTypes";

    private final ProtocolRestJsonWarmUpProvider provider = new ProtocolRestJsonWarmUpProvider();

    @BeforeEach
    void resetRecorder() {
        OperationRecordingInterceptor.reset();
    }

    @Test
    void warmUpClient_sync_invokesSelectedOperation() {
        assertThatCode(() -> provider.warmUpClient(ClientType.SYNC)).doesNotThrowAnyException();

        assertThat(OperationRecordingInterceptor.operationNames()).containsExactly(EXPECTED_OPERATION);
    }

    @Test
    void warmUpClient_async_invokesSelectedOperation() {
        assertThatCode(() -> provider.warmUpClient(ClientType.ASYNC)).doesNotThrowAnyException();

        assertThat(OperationRecordingInterceptor.operationNames()).containsExactly(EXPECTED_OPERATION);
    }

    @Test
    void warmUp_invokesSelectedOperationOnBothClients() {
        assertThatCode(provider::warmUp).doesNotThrowAnyException();

        assertThat(OperationRecordingInterceptor.operationNames())
            .containsExactly(EXPECTED_OPERATION, EXPECTED_OPERATION);
    }

    @Test
    void clientClassNames_matchGeneratedClients() {
        assertThat(provider.syncClientClassName()).isEqualTo(ProtocolRestJsonClient.class.getName());
        assertThat(provider.asyncClientClassName()).isEqualTo(ProtocolRestJsonAsyncClient.class.getName());
    }
}
