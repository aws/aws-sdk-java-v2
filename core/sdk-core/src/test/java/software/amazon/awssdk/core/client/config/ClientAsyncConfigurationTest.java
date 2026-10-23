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

package software.amazon.awssdk.core.client.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption.EXECUTION_AWARE_FUTURE_COMPLETION_EXECUTOR;
import static software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;

class ClientAsyncConfigurationTest {

    @Test
    void build_withBothFutureCompletionExecutors_throwsException() {
        ClientAsyncConfiguration.Builder builder =
            ClientAsyncConfiguration.builder()
                                    .advancedOption(FUTURE_COMPLETION_EXECUTOR, Runnable::run)
                                    .advancedOption(EXECUTION_AWARE_FUTURE_COMPLETION_EXECUTOR,
                                                    (command, executionAttributes) -> command.run());

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Only one of FUTURE_COMPLETION_EXECUTOR or EXECUTION_AWARE_FUTURE_COMPLETION_EXECUTOR");
    }

    @Test
    void build_withBothFutureCompletionExecutorsInAdvancedOptionsMap_throwsException() {
        Map<SdkAdvancedAsyncClientOption<?>, Object> advancedOptions = new HashMap<>();
        advancedOptions.put(FUTURE_COMPLETION_EXECUTOR, (Executor) Runnable::run);
        advancedOptions.put(EXECUTION_AWARE_FUTURE_COMPLETION_EXECUTOR,
                            (ExecutionAwareExecutor) (command, executionAttributes) -> command.run());
        ClientAsyncConfiguration.Builder builder = ClientAsyncConfiguration.builder().advancedOptions(advancedOptions);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Only one of FUTURE_COMPLETION_EXECUTOR or EXECUTION_AWARE_FUTURE_COMPLETION_EXECUTOR");
    }

    @Test
    void advancedOption_withExecutionAwareFutureCompletionExecutor_delegatesToIt() {
        ExecutionAwareExecutor executor = mock(ExecutionAwareExecutor.class);
        ClientAsyncConfiguration configuration =
            ClientAsyncConfiguration.builder()
                                    .advancedOption(EXECUTION_AWARE_FUTURE_COMPLETION_EXECUTOR, executor)
                                    .build();

        Runnable command = () -> { };
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        configuration.advancedOption(EXECUTION_AWARE_FUTURE_COMPLETION_EXECUTOR).execute(command, executionAttributes);

        verify(executor).execute(command, executionAttributes);
    }

    @Test
    void advancedOption_withNullExecutionAwareFutureCompletionExecutor_isNotConfigured() {
        ClientAsyncConfiguration configuration =
            ClientAsyncConfiguration.builder()
                                    .advancedOption(EXECUTION_AWARE_FUTURE_COMPLETION_EXECUTOR, null)
                                    .build();

        assertThat(configuration.advancedOption(EXECUTION_AWARE_FUTURE_COMPLETION_EXECUTOR)).isNull();
    }
}
