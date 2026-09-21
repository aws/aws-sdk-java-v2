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

package software.amazon.awssdk.services.s3.internal.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SplittingTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.internal.async.ConfigurableAsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.endpoints.S3ClientContextParams;
import software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionAsyncClient;
import software.amazon.awssdk.services.s3.internal.multipart.MultipartS3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;
import software.amazon.awssdk.services.s3.presignedurl.AsyncPresignedUrlExtension;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.utils.AttributeMap;

class S3AsyncClientDecoratorGzipSupportTest {

    @Test
    void decorate_whenSupportDefaultOrExplicitlyEnabled_addsNoGzipDecorator() {
        S3AsyncClient base = mock(S3AsyncClient.class);

        assertThat(decorate(base, false, null, null)).isSameAs(base);
        assertThat(decorate(base, false, null, true)).isSameAs(base);
    }

    @Test
    void decorate_whenSupportDisabled_addsGzipDecorator() {
        S3AsyncClient decorated = decorate(mock(S3AsyncClient.class), false, null, false);

        assertThat(decorated).isInstanceOf(ConcatenatedGzipStreamSupportS3AsyncClient.class);
    }

    @Test
    void decorate_whenSupportDisabledAndMultipartEnabled_addsGzipDecoratorOutermost() {
        S3AsyncClient decorated = decorate(mock(S3AsyncClient.class), true, null, false);

        assertThat(decorated).isInstanceOf(ConcatenatedGzipStreamSupportS3AsyncClient.class);
        assertThat(((ConcatenatedGzipStreamSupportS3AsyncClient) decorated).delegate())
            .isInstanceOf(MultipartS3AsyncClient.class);
    }

    @Test
    void decorate_whenSupportDisabledAndCrossRegionEnabled_addsGzipDecoratorOutermost() {
        S3AsyncClient decorated = decorate(mock(S3AsyncClient.class), false, true, false);

        assertThat(decorated).isInstanceOf(ConcatenatedGzipStreamSupportS3AsyncClient.class);
        assertThat(((ConcatenatedGzipStreamSupportS3AsyncClient) decorated).delegate())
            .isInstanceOf(S3CrossRegionAsyncClient.class);
    }

    @Test
    void getObject_whenSupportDisabled_configuresTransformerBeforeDelegating() {
        S3AsyncClient base = mock(S3AsyncClient.class);
        ConfigurableAsyncResponseTransformer<GetObjectResponse, String> original =
            mock(ConfigurableAsyncResponseTransformer.class);
        AsyncResponseTransformer<GetObjectResponse, String> configured = mock(AsyncResponseTransformer.class);
        GetObjectRequest request = GetObjectRequest.builder().bucket("bucket").key("key").build();
        CompletableFuture<String> expected = CompletableFuture.completedFuture("result");
        when(original.withConcatenatedGzipStreamSupportEnabled(false)).thenReturn(configured);
        when(base.getObject(request, configured)).thenReturn(expected);
        S3AsyncClient decorated = decorate(base, false, null, false);

        assertThat(decorated.getObject(request, original)).isSameAs(expected);

        verify(original).withConcatenatedGzipStreamSupportEnabled(false);
        verify(base).getObject(request, configured);
    }

    @Test
    void presignedGetObject_whenSupportDisabled_configuresTransformerBeforeDelegating() throws MalformedURLException {
        S3AsyncClient base = mock(S3AsyncClient.class);
        AsyncPresignedUrlExtension baseExtension = mock(AsyncPresignedUrlExtension.class);
        when(base.presignedUrlExtension()).thenReturn(baseExtension);
        ConfigurableAsyncResponseTransformer<GetObjectResponse, String> original =
            mock(ConfigurableAsyncResponseTransformer.class);
        AsyncResponseTransformer<GetObjectResponse, String> configured = mock(AsyncResponseTransformer.class);
        PresignedUrlDownloadRequest request =
            PresignedUrlDownloadRequest.builder()
                                       .presignedUrl(new URL("https://s3.amazonaws.com/bucket/key?signature=abc"))
                                       .build();
        CompletableFuture<String> expected = CompletableFuture.completedFuture("result");
        when(original.withConcatenatedGzipStreamSupportEnabled(false)).thenReturn(configured);
        when(baseExtension.getObject(request, configured)).thenReturn(expected);
        S3AsyncClient decorated = decorate(base, false, null, false);

        assertThat(decorated.presignedUrlExtension().getObject(request, original)).isSameAs(expected);

        verify(original).withConcatenatedGzipStreamSupportEnabled(false);
        verify(baseExtension).getObject(request, configured);
    }

    @Test
    void getObject_whenSupportDisabledAndMultipartEnabled_configuresTransformerBeforeSplit() {
        S3AsyncClient base = mock(S3AsyncClient.class);
        ConfigurableAsyncResponseTransformer<GetObjectResponse, String> original =
            mock(ConfigurableAsyncResponseTransformer.class);
        AsyncResponseTransformer<GetObjectResponse, String> configured = mock(AsyncResponseTransformer.class);
        IllegalStateException splitFailure = new IllegalStateException("configured transformer split");
        when(original.withConcatenatedGzipStreamSupportEnabled(false)).thenReturn(configured);
        when(configured.split(any(SplittingTransformerConfiguration.class))).thenThrow(splitFailure);
        S3AsyncClient decorated = decorate(base, true, null, false);

        assertThatThrownBy(() -> decorated.getObject(
            GetObjectRequest.builder().bucket("bucket").key("key").build(), original)).isSameAs(splitFailure);

        verify(original).withConcatenatedGzipStreamSupportEnabled(false);
        verify(configured).split(any(SplittingTransformerConfiguration.class));
        verify(original, never()).split(any(SplittingTransformerConfiguration.class));
    }

    @Test
    void presignedGetObject_whenSupportDisabledAndMultipartEnabled_configuresTransformerBeforeSplit()
        throws MalformedURLException {
        S3AsyncClient base = mock(S3AsyncClient.class);
        when(base.presignedUrlExtension()).thenReturn(mock(AsyncPresignedUrlExtension.class));
        ConfigurableAsyncResponseTransformer<GetObjectResponse, String> original =
            mock(ConfigurableAsyncResponseTransformer.class);
        AsyncResponseTransformer<GetObjectResponse, String> configured = mock(AsyncResponseTransformer.class);
        IllegalStateException splitFailure = new IllegalStateException("configured transformer split");
        when(original.withConcatenatedGzipStreamSupportEnabled(false)).thenReturn(configured);
        when(configured.split(any(SplittingTransformerConfiguration.class))).thenThrow(splitFailure);
        S3AsyncClient decorated = decorate(base, true, null, false);
        PresignedUrlDownloadRequest request =
            PresignedUrlDownloadRequest.builder()
                                       .presignedUrl(new URL("https://s3.amazonaws.com/bucket/key?signature=abc"))
                                       .build();

        assertThatThrownBy(() -> decorated.presignedUrlExtension().getObject(request, original)).isSameAs(splitFailure);

        verify(original).withConcatenatedGzipStreamSupportEnabled(false);
        verify(configured).split(any(SplittingTransformerConfiguration.class));
        verify(original, never()).split(any(SplittingTransformerConfiguration.class));
    }

    private static S3AsyncClient decorate(S3AsyncClient base,
                                           boolean multipartEnabled,
                                           Boolean crossRegionEnabled,
                                           Boolean gzipSupportEnabled) {
        AttributeMap.Builder context = AttributeMap.builder();
        if (multipartEnabled) {
            context.put(S3AsyncClientDecorator.MULTIPART_ENABLED_KEY, true);
            context.put(S3AsyncClientDecorator.MULTIPART_CONFIGURATION_KEY,
                        MultipartConfiguration.builder().build());
        }
        if (crossRegionEnabled != null) {
            context.put(S3ClientContextParams.CROSS_REGION_ACCESS_ENABLED, crossRegionEnabled);
        }
        SdkClientConfiguration.Builder configuration =
            SdkClientConfiguration.builder()
                                  .option(SdkClientOption.CLIENT_CONTEXT_PARAMS, context.build())
                                  .option(SdkClientOption.REQUEST_CHECKSUM_CALCULATION,
                                          RequestChecksumCalculation.WHEN_SUPPORTED);
        if (gzipSupportEnabled != null) {
            configuration.option(SdkAdvancedClientOption.CONCATENATED_GZIP_STREAM_SUPPORT_ENABLED,
                                 gzipSupportEnabled);
        }
        return new S3AsyncClientDecorator().decorate(base, configuration.build());
    }
}
