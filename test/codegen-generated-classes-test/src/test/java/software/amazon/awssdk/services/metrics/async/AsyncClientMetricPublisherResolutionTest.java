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

package software.amazon.awssdk.services.metrics.async;

import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonException;

@RunWith(MockitoJUnitRunner.class)
public class AsyncClientMetricPublisherResolutionTest {
    @Mock
    private AwsCredentialsProvider mockCredentialsProvider;

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ProtocolRestJsonAsyncClient client;


    @Before
    public void setup() {
        when(mockCredentialsProvider.resolveCredentials()).thenAnswer(invocation -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            return AwsBasicCredentials.create("foo", "bar");
        });
    }

    @After
    public void teardown() {
        wireMock.resetAll();
        if (client != null) {
            client.close();
        }
        client = null;
    }

    @Test
    public void testApiCall_noPublishersSet_noNpe() {
        client = clientWithPublishers();
        // This is thrown because all the requests to our wiremock are
        // nonsense, it's just important that we don't get NPE because we
        // don't have publishers set
        thrown.expectCause(instanceOf(ProtocolRestJsonException.class));
        client.allTypes().join();
    }

    @Test
    public void testApiCall_publishersSetOnClient_clientPublishersInvoked() throws IOException {
        MetricPublisher publisher1 = mock(MetricPublisher.class);
        MetricPublisher publisher2 = mock(MetricPublisher.class);

        client = clientWithPublishers(publisher1, publisher2);

        try {
            client.allTypes().join();
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
                    o.addMetricPublisher(publisher1).addMetricPublisher(publisher2)))
                    .join();
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
                    o.addMetricPublisher(requestPublisher1).addMetricPublisher(requestPublisher2)))
                    .join();
        } catch (Throwable t) {
            // ignored, call fails because our mock HTTP client isn't set up
        } finally {
            verify(requestPublisher1).publish(any(MetricCollection.class));
            verify(requestPublisher2).publish(any(MetricCollection.class));
            verifyZeroInteractions(clientPublisher1);
            verifyZeroInteractions(clientPublisher2);
        }
    }

    private ProtocolRestJsonAsyncClient clientWithPublishers(MetricPublisher... metricPublishers) {
        ProtocolRestJsonAsyncClientBuilder builder = ProtocolRestJsonAsyncClient.builder()
                .credentialsProvider(mockCredentialsProvider)
                .endpointOverride(URI.create("http://localhost:" + wireMock.port()));

        if (metricPublishers != null) {
            builder.overrideConfiguration(o -> o.metricPublishers(Arrays.asList(metricPublishers)));
        }

        return builder.build();
    }
}
