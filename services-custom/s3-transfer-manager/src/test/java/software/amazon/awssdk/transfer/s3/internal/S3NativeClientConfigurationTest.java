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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR;

import java.util.concurrent.ExecutorService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;

@RunWith(MockitoJUnitRunner.class)
public class S3NativeClientConfigurationTest {

    @Mock
    private ExecutorService executorService;

    @BeforeClass
    public static void setup() {
        System.setProperty("aws.crt.debugnative", "true");
    }

    // TODO: verify CRT resources are closed appropriately

    @Test
    public void defaultConfiguration_close_shouldShutdownDefaultExecutor() {
        S3NativeClientConfiguration configuration = S3NativeClientConfiguration.builder().build();
        assertThat(((ExecutorService) configuration.futureCompletionExecutor()).isShutdown()).isFalse();
        configuration.close();
        assertThat(configuration.futureCompletionExecutor()).isInstanceOf(ExecutorService.class);
        assertThat(((ExecutorService) configuration.futureCompletionExecutor()).isShutdown()).isTrue();
    }

    @Test
    public void customExecutor_close_shouldNotShutdownCustomExecutor() {
        S3NativeClientConfiguration configuration = S3NativeClientConfiguration.builder()
                                                                               .asyncConfiguration(ClientAsyncConfiguration.builder()
                                                                                                                                   .advancedOption(FUTURE_COMPLETION_EXECUTOR, executorService)
                                                                                                                                   .build())
                                                                               .build();

        configuration.close();
        Mockito.verifyZeroInteractions(executorService);
    }
}
