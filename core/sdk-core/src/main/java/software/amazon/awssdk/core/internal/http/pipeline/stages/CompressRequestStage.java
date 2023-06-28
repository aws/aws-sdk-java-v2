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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.compression.Compressor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Compress requests whose operations are marked with the "requestCompression" C2J trait.
 */
@SdkInternalApi
public class CompressRequestStage implements MutableRequestToRequestPipeline {
    private static final int DEFAULT_MIN_COMPRESSION_SIZE = 10_240;
    private static final int MIN_COMPRESSION_SIZE_LIMIT = 10_485_760;
    private static final Supplier<ProfileFile> PROFILE_FILE = ProfileFile::defaultProfileFile;
    private static final String PROFILE_NAME = ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow();

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder input, RequestExecutionContext context)
        throws Exception {

        if (!shouldCompress(input, context)) {
            return input;
        }

        Compressor compressor = resolveCompressionType(context.executionAttributes());

        // non-streaming
        if (!context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION).isStreaming()) {
            ContentStreamProvider wrappedProvider = input.contentStreamProvider();
            ContentStreamProvider compressedStreamProvider = () -> compressor.compress(wrappedProvider.newStream());
            input.contentStreamProvider(compressedStreamProvider);
            input = updateContentEncodingHeader(input, compressor);
            return updateContentLengthHeader(input);
        }

        if (context.requestProvider() == null) {
            // sync streaming

        } else {
            // async streaming

        }
        return updateContentEncodingHeader(input, compressor);
    }

    private static boolean shouldCompress(SdkHttpFullRequest.Builder input, RequestExecutionContext context) {
        if (context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION) == null) {
            return false;
        }
        if (resolveCompressionType(context.executionAttributes()) == null) {
            return false;
        }
        if (!resolveRequestCompressionEnabled(context)) {
            return false;
        }
        if (context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION).isStreaming()) {
            return true;
        }
        if (input.contentStreamProvider() == null) {
            return false;
        }
        return isRequestSizeWithinThreshold(input, context);
    }

    private static SdkHttpFullRequest.Builder updateContentEncodingHeader(SdkHttpFullRequest.Builder input, Compressor compressor) {
        if (input.firstMatchingHeader("Content-encoding").isPresent()) {
            return input.appendHeader("Content-encoding", compressor.compressorType());
        }

        return input.putHeader("Content-encoding", compressor.compressorType());
    }

    private static SdkHttpFullRequest.Builder updateContentLengthHeader(SdkHttpFullRequest.Builder input) {
        InputStream inputStream = input.contentStreamProvider().newStream();
        try {
            byte[] bytes = IoUtils.toByteArray(inputStream);
            String length = String.valueOf(bytes.length);
            return input.putHeader("Content-Length", length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean isTransferEncodingChunked(Context.ModifyHttpRequest context) {
        return context.httpRequest()
                      .firstMatchingHeader("Transfer-Encoding")
                      .map(headerValue -> headerValue.equals("chunked"))
                      .orElse(false);
    }

    private static Compressor resolveCompressionType(ExecutionAttributes executionAttributes) {
        /*List<String> encodings =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION).getEncodings();

        TODO: will refactor CompressionType in the other PR and update this
        for (String encoding: encodings) {
            encoding = encoding.toLowerCase(Locale.ROOT);
            if (CompressionType.compressionTypes().contains(encoding)) {
                return CompressionType.of(encoding).newCompressor().get();
            }
        }*/
        return null;
    }

    private static boolean resolveRequestCompressionEnabled(RequestExecutionContext context) {

        if (context.originalRequest().overrideConfiguration().isPresent()
            && context.originalRequest().overrideConfiguration().get().requestCompressionConfiguration().isPresent()) {
            Boolean requestCompressionEnabled = context.originalRequest().overrideConfiguration().get()
                                                       .requestCompressionConfiguration().get()
                                                       .requestCompressionEnabled();
            if (requestCompressionEnabled != null) {
                return requestCompressionEnabled;
            }
        }

        if (context.executionAttributes().getAttribute(SdkExecutionAttribute.REQUEST_COMPRESSION_CONFIGURATION) != null) {
            Boolean requestCompressionEnabled = context.executionAttributes().getAttribute(
                SdkExecutionAttribute.REQUEST_COMPRESSION_CONFIGURATION).requestCompressionEnabled();
            if (requestCompressionEnabled != null) {
                return requestCompressionEnabled;
            }
        }

        if (SdkSystemSetting.AWS_DISABLE_REQUEST_COMPRESSION.getBooleanValue().isPresent()) {
            return !SdkSystemSetting.AWS_DISABLE_REQUEST_COMPRESSION.getBooleanValue().get();
        }

        Optional<Boolean> profileSetting =
            PROFILE_FILE.get()
                        .profile(PROFILE_NAME)
                        .flatMap(p -> p.booleanProperty(ProfileProperty.DISABLE_REQUEST_COMPRESSION));
        if (profileSetting.isPresent()) {
            return !profileSetting.get();
        }

        return true;
    }

    private static boolean isRequestSizeWithinThreshold(SdkHttpFullRequest.Builder input, RequestExecutionContext context) {
        int minimumCompressionThreshold = resolveMinCompressionSize(context);
        validateMinCompressionSizeInput(minimumCompressionThreshold);

        long contentLength = Long.parseLong(input.firstMatchingHeader("Content-Length").orElse("0"));
        return contentLength >= minimumCompressionThreshold;
    }

    private static int resolveMinCompressionSize(RequestExecutionContext context) {

        if (context.originalRequest().overrideConfiguration().isPresent()
            && context.originalRequest().overrideConfiguration().get().requestCompressionConfiguration().isPresent()) {
            Integer minCompressionSize = context.originalRequest().overrideConfiguration().get()
                                                .requestCompressionConfiguration().get()
                                                .minimumCompressionThresholdInBytes();
            if (minCompressionSize != null) {
                return minCompressionSize;
            }
        }

        if (context.executionAttributes().getAttribute(SdkExecutionAttribute.REQUEST_COMPRESSION_CONFIGURATION) != null) {
            Integer minCompressionSize = context.executionAttributes()
                                                .getAttribute(SdkExecutionAttribute.REQUEST_COMPRESSION_CONFIGURATION)
                                                .minimumCompressionThresholdInBytes();
            if (minCompressionSize != null) {
                return minCompressionSize;
            }
        }

        if (SdkSystemSetting.AWS_REQUEST_MIN_COMPRESSION_SIZE_BYTES.getIntegerValue().isPresent()) {
            return SdkSystemSetting.AWS_REQUEST_MIN_COMPRESSION_SIZE_BYTES.getIntegerValue().get();
        }

        Optional<String> profileSetting =
            PROFILE_FILE.get()
                        .profile(PROFILE_NAME)
                        .flatMap(p -> p.property(ProfileProperty.REQUEST_MIN_COMPRESSION_SIZE_BYTES));
        if (profileSetting.isPresent()) {
            return Integer.parseInt(profileSetting.get());
        }

        return DEFAULT_MIN_COMPRESSION_SIZE;
    }

    private static void validateMinCompressionSizeInput(int minCompressionSize) {
        if (!(minCompressionSize >= 0 && minCompressionSize <= MIN_COMPRESSION_SIZE_LIMIT)) {
            throw SdkClientException.create("The minimum compression size must be non-negative with a maximum value of "
                                            + "10485760.", new IllegalArgumentException());
        }
    }
}
