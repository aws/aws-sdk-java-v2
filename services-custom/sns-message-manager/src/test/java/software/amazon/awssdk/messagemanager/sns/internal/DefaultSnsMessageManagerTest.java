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

package software.amazon.awssdk.messagemanager.sns.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.AttributeMap;

public class DefaultSnsMessageManagerTest {
    private static SdkHttpClient.Builder<?> mockHttpClientBuilder;
    private static SdkHttpClient mockHttpClient;

    @BeforeAll
    static void setup() {
        mockHttpClientBuilder = mock(SdkHttpClient.Builder.class);
        mockHttpClient = mock(SdkHttpClient.class);
        when(mockHttpClientBuilder.buildWithDefaults(any(AttributeMap.class))).thenReturn(mockHttpClient);
    }

    @Test
    void close_httpClientConfiguredOnBuilder_httpClientNotClosed() {
        SdkHttpClient mockClient = mock(SdkHttpClient.class);

        SnsMessageManager msgManager = DefaultSnsMessageManager.builder()
                                                               .httpClient(mockClient)
                                                               .region(Region.US_WEST_2)
                                                               .build();

        msgManager.close();

        verifyNoInteractions(mockClient);
    }

    @Test
    void parseMessage_streamNull_throws() {
        SnsMessageManager msgManager =
            DefaultSnsMessageManager.builder().httpClient(mockHttpClient).region(Region.US_WEST_2).build();

        assertThatThrownBy(() -> msgManager.parseMessage((InputStream) null))
            .hasMessage("message cannot be null");
    }

    @Test
    void parseMessage_stringNull_throws() {
        SnsMessageManager msgManager =
            DefaultSnsMessageManager.builder().httpClient(mockHttpClient).region(Region.US_WEST_2).build();

        assertThatThrownBy(() -> msgManager.parseMessage((String) null))
            .hasMessage("message cannot be null");
    }

    @Test
    void close_certRetrieverClosed() {
        CertificateRetriever mockCertRetriever = mock(CertificateRetriever.class);

        SnsMessageManager msgManager = new DefaultSnsMessageManager.BuilderImpl()
            .certificateRetriever(mockCertRetriever)
            .region(Region.US_WEST_2)
            .build();

        msgManager.close();

        verify(mockCertRetriever).close();
    }

    @Test
    void build_httpClientNotConfigured_usesDefaultHttpClient() {
        System.setProperty(SdkSystemSetting.SYNC_HTTP_SERVICE_IMPL.property(), TestHttpService.class.getName());
        try {
            DefaultSnsMessageManager.builder().region(Region.US_WEST_2).build();

            ArgumentCaptor<AttributeMap> captor = ArgumentCaptor.forClass(AttributeMap.class);
            verify(mockHttpClientBuilder).buildWithDefaults(captor.capture());

            AttributeMap buildArgs = captor.getValue();
            assertThat(buildArgs.get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT)).isEqualTo(Duration.ofSeconds(10));
            assertThat(buildArgs.get(SdkHttpConfigurationOption.READ_TIMEOUT)).isEqualTo(Duration.ofSeconds(30));
        } finally {
            System.clearProperty(SdkSystemSetting.SYNC_HTTP_SERVICE_IMPL.property());
        }
    }

    @Test
    void close_defaultHttpClientUsed_isClosed() {
        System.setProperty(SdkSystemSetting.SYNC_HTTP_SERVICE_IMPL.property(), TestHttpService.class.getName());
        try {
            SnsMessageManager msgManager = DefaultSnsMessageManager.builder()
                                                                   .region(Region.US_WEST_2)
                                                                   .build();
            msgManager.close();

            verify(mockHttpClient).close();
        } finally {
            System.clearProperty(SdkSystemSetting.SYNC_HTTP_SERVICE_IMPL.property());
        }
    }

    // NOTE: needs to be public to work with the service loader
    public static class TestHttpService implements SdkHttpService {

        @Override
        public SdkHttpClient.Builder<?> createHttpClientBuilder() {
            return mockHttpClientBuilder;
        }
    }
}
