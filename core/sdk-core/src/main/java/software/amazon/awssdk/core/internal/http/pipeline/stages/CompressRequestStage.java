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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static software.amazon.awssdk.core.client.config.SdkClientOption.COMPRESSION_CONFIGURATION;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.CompressionConfiguration;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.async.CompressionAsyncRequestBody;
import software.amazon.awssdk.core.internal.compression.Compressor;
import software.amazon.awssdk.core.internal.compression.CompressorType;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.core.internal.sync.CompressionContentStreamProvider;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Compress requests whose operations are marked with the "requestCompression" C2J trait.
 */
@SdkInternalApi
public class CompressRequestStage implements MutableRequestToRequestPipeline {
    private static final int DEFAULT_MIN_COMPRESSION_SIZE = 10_240;
    private static final int MIN_COMPRESSION_SIZE_LIMIT = 10_485_760;
    private final CompressionConfiguration compressionConfig;

    public CompressRequestStage(HttpClientDependencies dependencies) {
        compressionConfig = dependencies.clientConfiguration().option(COMPRESSION_CONFIGURATION);
    }

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder input, RequestExecutionContext context)
            throws Exception {

        if (!shouldCompress(input, context)) {
            return input;
        }

        Compressor compressor = resolveCompressorType(context.executionAttributes());

        if (!isStreaming(context)) {
            compressEntirePayload(input, compressor);
            updateContentEncodingHeader(input, compressor);
            updateContentLengthHeader(input);
            return input;
        }

        if (!isTransferEncodingChunked(input)) {
            return input;
        }

        if (context.requestProvider() == null) {
            input.contentStreamProvider(new CompressionContentStreamProvider(input.contentStreamProvider(), compressor));
        } else {
            context.requestProvider(CompressionAsyncRequestBody.builder()
                                                               .asyncRequestBody(context.requestProvider())
                                                               .compressor(compressor)
                                                               .build());
        }

        updateContentEncodingHeader(input, compressor);
        return input;
    }

    private boolean shouldCompress(SdkHttpFullRequest.Builder input, RequestExecutionContext context) {
        if (context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION) == null) {
            return false;
        }
        if (resolveCompressorType(context.executionAttributes()) == null) {
            return false;
        }
        if (!resolveRequestCompressionEnabled(context)) {
            return false;
        }
        if (isStreaming(context)) {
            return true;
        }
        if (input.contentStreamProvider() == null) {
            return false;
        }
        return isRequestSizeWithinThreshold(input, context);
    }

    private boolean isStreaming(RequestExecutionContext context) {
        return context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION).isStreaming();
    }

    private void compressEntirePayload(SdkHttpFullRequest.Builder input, Compressor compressor) {
        ContentStreamProvider wrappedProvider = input.contentStreamProvider();
        ContentStreamProvider compressedStreamProvider = () -> compressor.compress(wrappedProvider.newStream());
        input.contentStreamProvider(compressedStreamProvider);
    }

    private void updateContentEncodingHeader(SdkHttpFullRequest.Builder input,
                                                                          Compressor compressor) {
        if (input.firstMatchingHeader("Content-encoding").isPresent()) {
            input.appendHeader("Content-encoding", compressor.compressorType());
        } else {
            input.putHeader("Content-encoding", compressor.compressorType());
        }
    }

    private void updateContentLengthHeader(SdkHttpFullRequest.Builder input) {
        InputStream inputStream = input.contentStreamProvider().newStream();
        try {
            byte[] bytes = IoUtils.toByteArray(inputStream);
            String length = String.valueOf(bytes.length);
            input.putHeader("Content-Length", length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean isTransferEncodingChunked(SdkHttpFullRequest.Builder input) {
        return input.firstMatchingHeader("Transfer-Encoding")
                    .map(headerValue -> headerValue.equals("chunked"))
                    .orElse(false);
    }

    private Compressor resolveCompressorType(ExecutionAttributes executionAttributes) {
        List<String> encodings =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION).getEncodings();

        for (String encoding: encodings) {
            encoding = encoding.toLowerCase(Locale.ROOT);
            if (CompressorType.isSupported(encoding)) {
                return CompressorType.of(encoding).newCompressor();
            }
        }
        return null;
    }

    private boolean resolveRequestCompressionEnabled(RequestExecutionContext context) {

        Optional<Boolean> requestCompressionEnabledRequestLevel =
            context.originalRequest().overrideConfiguration()
                   .flatMap(RequestOverrideConfiguration::compressionConfiguration)
                   .map(CompressionConfiguration::requestCompressionEnabled);
        if (requestCompressionEnabledRequestLevel.isPresent()) {
            return requestCompressionEnabledRequestLevel.get();
        }

        Boolean isEnabled = compressionConfig.requestCompressionEnabled();
        if (isEnabled != null) {
            return isEnabled;
        }

        return true;
    }

    private boolean isRequestSizeWithinThreshold(SdkHttpFullRequest.Builder input, RequestExecutionContext context) {
        int minimumCompressionThreshold = resolveMinCompressionSize(context);
        validateMinCompressionSizeInput(minimumCompressionThreshold);
        int requestSize = SdkBytes.fromInputStream(input.contentStreamProvider().newStream()).asByteArray().length;
        return requestSize >= minimumCompressionThreshold;
    }

    private int resolveMinCompressionSize(RequestExecutionContext context) {

        Optional<Integer> minimumCompressionSizeRequestLevel =
            context.originalRequest().overrideConfiguration()
                   .flatMap(RequestOverrideConfiguration::compressionConfiguration)
                   .map(CompressionConfiguration::minimumCompressionThresholdInBytes);
        if (minimumCompressionSizeRequestLevel.isPresent()) {
            return minimumCompressionSizeRequestLevel.get();
        }

        Integer threshold = compressionConfig.minimumCompressionThresholdInBytes();
        if (threshold != null) {
            return threshold;
        }

        return DEFAULT_MIN_COMPRESSION_SIZE;
    }

    private void validateMinCompressionSizeInput(int minCompressionSize) {
        if (!(minCompressionSize >= 0 && minCompressionSize <= MIN_COMPRESSION_SIZE_LIMIT)) {
            throw SdkClientException.create("The minimum compression size must be non-negative with a maximum value of "
                                            + "10485760.", new IllegalArgumentException());
        }
    }
}
