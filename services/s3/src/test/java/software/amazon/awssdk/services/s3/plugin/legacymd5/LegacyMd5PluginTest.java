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

package software.amazon.awssdk.services.s3.plugin.legacymd5;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.s3.LegacyMd5Plugin;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.internal.handlers.LegacyMd5ExecutionInterceptor;

class LegacyMd5PluginTest {

    private S3ServiceClientConfiguration.Builder configBuilder;
    private ClientOverrideConfiguration.Builder overrideConfigBuilder;
    private ClientOverrideConfiguration originalOverrideConfig;

    @BeforeEach
    void setup() {
        // Create mocks
        configBuilder = mock(S3ServiceClientConfiguration.Builder.class);
        overrideConfigBuilder = mock(ClientOverrideConfiguration.Builder.class);
        originalOverrideConfig = mock(ClientOverrideConfiguration.class);

        when(configBuilder.overrideConfiguration()).thenReturn(originalOverrideConfig);
        when(originalOverrideConfig.toBuilder()).thenReturn(overrideConfigBuilder);
        when(overrideConfigBuilder.addExecutionInterceptor(any(ExecutionInterceptor.class)))
            .thenReturn(overrideConfigBuilder);
        when(overrideConfigBuilder.build()).thenReturn(originalOverrideConfig);
        when(configBuilder.responseChecksumValidation(any(ResponseChecksumValidation.class)))
            .thenReturn(configBuilder);
        when(configBuilder.requestChecksumCalculation(any(RequestChecksumCalculation.class)))
            .thenReturn(configBuilder);
        when(configBuilder.overrideConfiguration(any(ClientOverrideConfiguration.class)))
            .thenReturn(configBuilder);
    }

    @Test
    void testLegacyMd5PluginCreation() {
        SdkPlugin plugin = LegacyMd5Plugin.create();
        assertThat(plugin).isNotNull();
        assertThat(plugin).isInstanceOf(LegacyMd5Plugin.class);
    }

    @Test
    void testConfigureClient() {
        SdkPlugin plugin = LegacyMd5Plugin.create();
        plugin.configureClient(configBuilder);

        verify(configBuilder).responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
        verify(configBuilder).requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED);

        ArgumentCaptor<ExecutionInterceptor> interceptorCaptor =
            ArgumentCaptor.forClass(ExecutionInterceptor.class);
        verify(overrideConfigBuilder).addExecutionInterceptor(interceptorCaptor.capture());

        ExecutionInterceptor capturedInterceptor = interceptorCaptor.getValue();
        assertThat(capturedInterceptor).isNotNull();
        assertThat(capturedInterceptor).isInstanceOf(LegacyMd5ExecutionInterceptor.class);

        verify(configBuilder).overrideConfiguration(originalOverrideConfig);
    }
}
