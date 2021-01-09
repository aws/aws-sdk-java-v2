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

package software.amazon.awssdk.services.metrics;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;

@RunWith(MockitoJUnitRunner.class)
public class SyncClientMetricPublisherResolutionTest {

    @Mock
    private SdkHttpClient mockHttpClient;

    @Mock
    private AwsCredentialsProvider mockCredentialsProvider;

    private ProtocolRestJsonClient client;

    @After
    public void teardown() {
        if (client != null) {
            client.close();
        }

        client = null;
    }

    @Test
    public void testApiCall_noPublishersSet_noException() throws IOException {
        client = clientWithPublishers();
        client.allTypes();
    }

    @Test
    public void testApiCall_publishersSetOnClient_clientPublishersInvoked() throws IOException {
        MetricPublisher publisher1 = mock(MetricPublisher.class);
        MetricPublisher publisher2 = mock(MetricPublisher.class);

        client = clientWithPublishers(publisher1, publisher2);

        try {
            client.allTypes();
        } catch (Throwable t) {
            // ignored, call fails because our mock HTTP client isn't set up
        } finally {
            verify(publisher1).publish(any(MetricCollection.class));
            verify(publisher2).publish(any(MetricCollection.class));
        }
    }

    @Test
    public void testApiCall_publishersSetOnRequest_requestPublishersInvoked() throws IOException {
        MetricPublisher publisher1 = mock(MetricPublisher.class);
        MetricPublisher publisher2 = mock(MetricPublisher.class);

        client = clientWithPublishers();

        try {
            client.allTypes(r -> r.overrideConfiguration(o ->
                    o.addMetricPublisher(publisher1).addMetricPublisher(publisher2)));
        } catch (Throwable t) {
            // ignored, call fails because our mock HTTP client isn't set up
        } finally {
            verify(publisher1).publish(any(MetricCollection.class));
            verify(publisher2).publish(any(MetricCollection.class));
        }
    }

    @Test
    public void testApiCall_publishersSetOnClientAndRequest_requestPublishersInvoked() throws IOException {
        MetricPublisher clientPublisher1 = mock(MetricPublisher.class);
        MetricPublisher clientPublisher2 = mock(MetricPublisher.class);

        MetricPublisher requestPublisher1 = mock(MetricPublisher.class);
        MetricPublisher requestPublisher2 = mock(MetricPublisher.class);

        client = clientWithPublishers(clientPublisher1, clientPublisher2);

        try {
            client.allTypes(r -> r.overrideConfiguration(o ->
                    o.addMetricPublisher(requestPublisher1).addMetricPublisher(requestPublisher2)));
        } catch (Throwable t) {
            // ignored, call fails because our mock HTTP client isn't set up
        } finally {
            verify(requestPublisher1).publish(any(MetricCollection.class));
            verify(requestPublisher2).publish(any(MetricCollection.class));
            verifyZeroInteractions(clientPublisher1);
            verifyZeroInteractions(clientPublisher2);
        }
    }

    private ProtocolRestJsonClient clientWithPublishers(MetricPublisher... metricPublishers) throws IOException {
        ProtocolRestJsonClientBuilder builder = ProtocolRestJsonClient.builder()
                .httpClient(mockHttpClient)
                .credentialsProvider(mockCredentialsProvider);

        AbortableInputStream content = AbortableInputStream.create(new ByteArrayInputStream("{}".getBytes()));
        SdkHttpFullResponse httpResponse = SdkHttpFullResponse.builder()
                .statusCode(200)
                .content(content)
                .build();

        HttpExecuteResponse mockResponse = mockExecuteResponse(httpResponse);

        ExecutableHttpRequest mockExecuteRequest = mock(ExecutableHttpRequest.class);
        when(mockExecuteRequest.call()).thenAnswer(invocation -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            return mockResponse;
        });

        when(mockHttpClient.prepareRequest(any(HttpExecuteRequest.class)))
                .thenReturn(mockExecuteRequest);

        when(mockCredentialsProvider.resolveCredentials()).thenAnswer(invocation -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            return AwsBasicCredentials.create("foo", "bar");
        });

        if (metricPublishers != null) {
            builder.overrideConfiguration(o -> o.metricPublishers(Arrays.asList(metricPublishers)));
        }

        return builder.build();
    }

    private static HttpExecuteResponse mockExecuteResponse(SdkHttpFullResponse httpResponse) {
        HttpExecuteResponse mockResponse = mock(HttpExecuteResponse.class);
        when(mockResponse.httpResponse()).thenReturn(httpResponse);
        when(mockResponse.responseBody()).thenReturn(httpResponse.content());
        return mockResponse;
    }
}
