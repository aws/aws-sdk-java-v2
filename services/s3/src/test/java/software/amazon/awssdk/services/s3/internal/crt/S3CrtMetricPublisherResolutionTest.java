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

package software.amazon.awssdk.services.s3.internal.crt;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * Unit tests for request-level metric-publisher support on the CRT client.
 */
public class S3CrtMetricPublisherResolutionTest {

    private static MetricPublisher publisher(String name) {
        return new MetricPublisher() {
            @Override
            public void publish(MetricCollection metricCollection) {
            }

            @Override
            public void close() {
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    // ---- effective-publisher resolution matrix ----

    @Test
    public void resolveEffectiveMetricPublishers_noPublishers_returnsEmpty() {
        assertThat(S3CrtAsyncHttpClient.resolveEffectiveMetricPublishers(emptyList(), emptyList())).isEmpty();
        assertThat(S3CrtAsyncHttpClient.resolveEffectiveMetricPublishers(null, emptyList())).isEmpty();
    }

    @Test
    public void resolveEffectiveMetricPublishers_requestLevelOnly_usesRequestLevel() {
        MetricPublisher request = publisher("request");
        assertThat(S3CrtAsyncHttpClient.resolveEffectiveMetricPublishers(singletonList(request), emptyList()))
            .containsExactly(request);
    }

    @Test
    public void resolveEffectiveMetricPublishers_clientLevelOnly_usesClientLevel() {
        MetricPublisher client = publisher("client");
        assertThat(S3CrtAsyncHttpClient.resolveEffectiveMetricPublishers(emptyList(), singletonList(client)))
            .containsExactly(client);
        assertThat(S3CrtAsyncHttpClient.resolveEffectiveMetricPublishers(null, singletonList(client)))
            .containsExactly(client);
    }

    @Test
    public void resolveEffectiveMetricPublishers_requestAndClientLevel_requestTakesPrecedence() {
        MetricPublisher client = publisher("client");
        MetricPublisher request = publisher("request");
        assertThat(S3CrtAsyncHttpClient.resolveEffectiveMetricPublishers(singletonList(request), singletonList(client)))
            .containsExactly(request);
    }

    // ---- stashRequestMetricPublishers: move publishers off the request into the execution attribute ----

    @Test
    public void stash_movesRequestPublishersToExecutionAttributeAndStripsOverride() {
        MetricPublisher request = publisher("request");
        GetObjectRequest in = GetObjectRequest.builder()
                                              .bucket("b").key("k")
                                              .overrideConfiguration(o -> o.addMetricPublisher(request))
                                              .build();

        GetObjectRequest out = DefaultS3CrtAsyncClient.stashRequestMetricPublishers(in);

        // stripped off the override so the inner standard client's resolveMetricPublishers() sees nothing
        assertThat(out.overrideConfiguration().get().metricPublishers()).isEmpty();
        // stashed in the execution attribute the CRT transport reads
        assertThat(out.overrideConfiguration().get().executionAttributes()
                      .getAttribute(DefaultS3CrtAsyncClient.REQUEST_METRIC_PUBLISHERS))
            .containsExactly(request);
    }

    @Test
    public void stash_copyObjectRequest_movesPublishersOffOverrideIntoAttribute() {
        MetricPublisher request = publisher("request");
        CopyObjectRequest in = CopyObjectRequest.builder()
                                                .sourceBucket("src").sourceKey("k")
                                                .destinationBucket("dst").destinationKey("k2")
                                                .overrideConfiguration(o -> o.addMetricPublisher(request))
                                                .build();

        CopyObjectRequest out = DefaultS3CrtAsyncClient.stashRequestMetricPublishers(in);

        assertThat(out.overrideConfiguration().get().metricPublishers()).isEmpty();
        assertThat(out.overrideConfiguration().get().executionAttributes()
                      .getAttribute(DefaultS3CrtAsyncClient.REQUEST_METRIC_PUBLISHERS))
            .containsExactly(request);
    }

    @Test
    public void stash_noOverrideConfiguration_returnsSameRequest() {
        GetObjectRequest in = GetObjectRequest.builder().bucket("b").key("k").build();
        assertThat(DefaultS3CrtAsyncClient.stashRequestMetricPublishers(in)).isSameAs(in);
    }

    @Test
    public void stash_overrideWithoutPublishers_returnsSameRequest() {
        GetObjectRequest in = GetObjectRequest.builder()
                                              .bucket("b").key("k")
                                              .overrideConfiguration(o -> o.putRawQueryParameter("x", "y"))
                                              .build();
        assertThat(DefaultS3CrtAsyncClient.stashRequestMetricPublishers(in)).isSameAs(in);
    }
}
