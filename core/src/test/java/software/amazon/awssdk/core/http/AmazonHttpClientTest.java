/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.config.AdvancedClientOption;
import software.amazon.awssdk.config.ClientOverrideConfiguration;
import software.amazon.awssdk.config.MutableClientConfiguration;
import software.amazon.awssdk.config.defaults.GlobalClientConfigurationDefaults;
import software.amazon.awssdk.internal.http.timers.ClientExecutionAndRequestTimerTestUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;
import utils.HttpTestUtils;
import utils.ValidSdkObjects;

@RunWith(MockitoJUnitRunner.class)
public class AmazonHttpClientTest {

    @Mock
    private SdkHttpClient sdkHttpClient;

    @Mock
    private AbortableCallable<SdkHttpFullResponse> abortableCallable;

    private AmazonHttpClient client;

    @Before
    public void setUp() throws Exception {
        client = HttpTestUtils.testClientBuilder().httpClient(sdkHttpClient).build();
        when(sdkHttpClient.prepareRequest(any(), any())).thenReturn(abortableCallable);
        stubSuccessfulResponse();
    }

    @Test
    public void testRetryIoExceptionFromExecute() throws Exception {
        IOException ioException = new IOException("BOOM");

        when(abortableCallable.call()).thenThrow(ioException);

        ExecutionContext context = ClientExecutionAndRequestTimerTestUtils.executionContext(null);

        try {
            client.requestExecutionBuilder()
                    .request(ValidSdkObjects.legacyRequest())
                    .executionContext(context)
                    .execute();
            Assert.fail("No exception when request repeatedly fails!");

        } catch (AmazonClientException e) {
            Assert.assertSame(ioException, e.getCause());
        }

        // Verify that we called execute 4 times.
        verify(sdkHttpClient, times(4)).prepareRequest(any(), any());
    }

    @Test
    public void testRetryIoExceptionFromHandler() throws Exception {
        final IOException exception = new IOException("BOOM");

        HttpResponseHandler<?> mockHandler = mock(HttpResponseHandler.class);
        when(mockHandler.needsConnectionLeftOpen()).thenReturn(false);
        when(mockHandler.handle(any(), any())).thenThrow(exception);

        ExecutionContext context = ClientExecutionAndRequestTimerTestUtils.executionContext(null);

        try {
            client.requestExecutionBuilder()
                    .request(ValidSdkObjects.legacyRequest())
                    .executionContext(context)
                    .execute(mockHandler);
            Assert.fail("No exception when request repeatedly fails!");

        } catch (AmazonClientException e) {
            Assert.assertSame(exception, e.getCause());
        }

        // Verify that we called execute 4 times.
        verify(mockHandler, times(4)).handle(any(), any());
    }


    @Test
    public void testUserAgentPrefixAndSuffixAreAdded() throws Exception {
        String prefix = "somePrefix";
        String suffix = "someSuffix";
        Request<?> request = ValidSdkObjects.legacyRequest();

        HttpResponseHandler<?> handler = mock(HttpResponseHandler.class);
        ClientOverrideConfiguration overrideConfig =
                ClientOverrideConfiguration.builder()
                                           .advancedOption(AdvancedClientOption.USER_AGENT_PREFIX, prefix)
                                           .advancedOption(AdvancedClientOption.USER_AGENT_SUFFIX, suffix)
                                           .build();
        MutableClientConfiguration config = new MutableClientConfiguration().overrideConfiguration(overrideConfig)
                                                                            .httpClient(sdkHttpClient)
                                                                            .endpoint(URI.create("http://example.com"));

        new GlobalClientConfigurationDefaults().applySyncDefaults(config);

        AmazonHttpClient client = new AmazonHttpClient(config);

        client.requestExecutionBuilder()
              .request(request)
              .executionContext(ClientExecutionAndRequestTimerTestUtils.executionContext(null))
              .execute(handler);

        ArgumentCaptor<SdkHttpFullRequest> httpRequestCaptor = ArgumentCaptor.forClass(SdkHttpFullRequest.class);
        verify(sdkHttpClient).prepareRequest(httpRequestCaptor.capture(), any());

        final String userAgent = SdkHttpUtils.firstMatchingHeader(httpRequestCaptor.getValue().headers(), "User-Agent")
                                             .orElseThrow(() -> new AssertionError("User-Agent header was not found"));

        Assert.assertTrue(userAgent.startsWith(prefix));
        Assert.assertTrue(userAgent.endsWith(suffix));
    }

    private void stubSuccessfulResponse() throws Exception {
        when(abortableCallable.call()).thenReturn(SdkHttpFullResponse.builder()
                                                                     .statusCode(200)
                                                                     .build());
    }
}
