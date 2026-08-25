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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.multipart.CopyObjectHelper;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CopyPartResult;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;

/**
 * Unit tests for request-level metric-publisher support on the CRT client: publisher resolution, the transforms that
 * move request-level publishers off the request, and copyObject-wrapper propagation.
 */
public class S3CrtMetricPublisherResolutionTest {

    private static final long PART_SIZE = 1024L;
    private static final long UPLOAD_THRESHOLD = PART_SIZE * 2;

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

    // ---- wrapper transform: request-level publishers are moved off the request (so the inner client stays no-op) ----

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

    // ---- putRequestMetricPublishers: attaches a copy's request-level publishers to each copy sub-request ----

    @Test
    public void put_addsAttributeAndClearsInlinePublishers_whenRequestHasNoOverride() {
        MetricPublisher request = publisher("request");
        GetObjectRequest in = GetObjectRequest.builder().bucket("b").key("k").build();

        GetObjectRequest out = DefaultS3CrtAsyncClient.putRequestMetricPublishers(in, singletonList(request));

        assertThat(out.overrideConfiguration()).isPresent();
        assertThat(out.overrideConfiguration().get().metricPublishers()).isEmpty();
        assertThat(out.overrideConfiguration().get().executionAttributes()
                      .getAttribute(DefaultS3CrtAsyncClient.REQUEST_METRIC_PUBLISHERS))
            .containsExactly(request);
    }

    @Test
    public void put_stripsInlinePublishers_soInnerPipelineStaysNoOp() {
        MetricPublisher inline = publisher("inline");
        MetricPublisher request = publisher("request");
        GetObjectRequest in = GetObjectRequest.builder()
                                              .bucket("b").key("k")
                                              .overrideConfiguration(o -> o.addMetricPublisher(inline))
                                              .build();

        GetObjectRequest out = DefaultS3CrtAsyncClient.putRequestMetricPublishers(in, singletonList(request));

        // inline publishers removed so the inner standard client's resolveMetricPublishers() stays empty (no leak)
        assertThat(out.overrideConfiguration().get().metricPublishers()).isEmpty();
        // the transport reads these to publish CRT telemetry to the request-level publishers
        assertThat(out.overrideConfiguration().get().executionAttributes()
                      .getAttribute(DefaultS3CrtAsyncClient.REQUEST_METRIC_PUBLISHERS))
            .containsExactly(request);
    }

    // ---- copyObject wrapper: the injecting client attaches the copy's publishers to every sub-request ----

    @Test
    public void injectingClient_multipartCopy_attachesRequestPublishersToEveryPartCopy() {
        MetricPublisher request = publisher("request");
        S3AsyncClient delegate = mock(S3AsyncClient.class);
        // A 4000-byte source with a 1024 part size and 2048 threshold fans out into 4 UploadPartCopy sub-requests.
        when(delegate.headObject(any(HeadObjectRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(HeadObjectResponse.builder().contentLength(4000L).build()));
        when(delegate.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(CreateMultipartUploadResponse.builder().uploadId("mpu").build()));
        when(delegate.uploadPartCopy(any(UploadPartCopyRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                UploadPartCopyResponse.builder().copyPartResult(CopyPartResult.builder().build()).build()));
        when(delegate.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(CompleteMultipartUploadResponse.builder().build()));

        copyThrough(delegate, request);

        ArgumentCaptor<UploadPartCopyRequest> captor = ArgumentCaptor.forClass(UploadPartCopyRequest.class);
        verify(delegate, times(4)).uploadPartCopy(captor.capture());
        assertThat(captor.getAllValues())
            .allSatisfy(part -> assertCarriesRequestPublishers(part.overrideConfiguration().orElse(null), request));
    }

    @Test
    public void injectingClient_singlePartCopy_attachesRequestPublishersToCopyRequest() {
        MetricPublisher request = publisher("request");
        S3AsyncClient delegate = mock(S3AsyncClient.class);
        // A 500-byte source stays under the threshold, so it is a single CopyObject sub-request.
        when(delegate.headObject(any(HeadObjectRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(HeadObjectResponse.builder().contentLength(500L).build()));
        when(delegate.copyObject(any(CopyObjectRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(CopyObjectResponse.builder().build()));

        copyThrough(delegate, request);

        ArgumentCaptor<CopyObjectRequest> captor = ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(delegate).copyObject(captor.capture());
        assertCarriesRequestPublishers(captor.getValue().overrideConfiguration().orElse(null), request);
    }

    private static void copyThrough(S3AsyncClient delegate, MetricPublisher requestPublisher) {
        S3AsyncClient injecting =
            new DefaultS3CrtAsyncClient.RequestMetricPublisherInjectingClient(delegate, singletonList(requestPublisher));
        new CopyObjectHelper(injecting, PART_SIZE, UPLOAD_THRESHOLD).copyObject(copyObjectRequest()).join();
    }

    private static void assertCarriesRequestPublishers(AwsRequestOverrideConfiguration override, MetricPublisher expected) {
        assertThat(override).isNotNull();
        // inline publishers are cleared so the inner standard client's pipeline stays no-op (no hollow ApiCall)
        assertThat(override.metricPublishers()).isEmpty();
        // and the request-level publishers ride the execution attribute the CRT transport reads
        assertThat(override.executionAttributes().getAttribute(DefaultS3CrtAsyncClient.REQUEST_METRIC_PUBLISHERS))
            .containsExactly(expected);
    }

    private static CopyObjectRequest copyObjectRequest() {
        return CopyObjectRequest.builder()
                                .sourceBucket("source").sourceKey("sourceKey")
                                .destinationBucket("destination").destinationKey("destinationKey")
                                .build();
    }
}
