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

package software.amazon.awssdk.core.internal.interceptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.compression.CompressionType;
import software.amazon.awssdk.core.compression.Compressor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.async.CompressionAsyncRequestBody;
import software.amazon.awssdk.core.internal.io.AwsCompressionInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Interceptor to handles compression of requests whose operations are marked with the "requestCompression" C2J trait.
 * Compression of payload will be completed prior to checksum calculation. The Content-Encoding header will be updated
 * accordingly.
 */
@SdkInternalApi
public class RequestCompressionInterceptor implements ExecutionInterceptor {

    private static final int DEFAULT_MIN_COMPRESSION_SIZE = 10_240;
    private static final int MIN_COMPRESSION_SIZE_LIMIT = 10_485_760;
    private static final Supplier<ProfileFile> PROFILE_FILE = ProfileFile::defaultProfileFile;
    private static final String PROFILE_NAME = ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow();

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        if (!shouldCompress(context, executionAttributes)) {
            return context.httpRequest();
        }

        Compressor compressor = resolveCompressionType(executionAttributes);
        if (executionAttributes.getAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION).isStreaming()) {
            return updateContentEncodingHeader(context.httpRequest(), compressor);
        }

        SdkHttpFullRequest sdkHttpFullRequest = (SdkHttpFullRequest) context.httpRequest();
        ContentStreamProvider wrappedProvider = sdkHttpFullRequest.contentStreamProvider().get();
        ContentStreamProvider compressedStreamProvider = () -> compressor.compress(wrappedProvider.newStream());
        SdkHttpRequest sdkHttpRequest =
            sdkHttpFullRequest.toBuilder()
                              .contentStreamProvider(compressedStreamProvider)
                              .build();
        sdkHttpRequest = updateContentEncodingHeader(sdkHttpRequest, compressor);
        return updateContentLengthHeader(sdkHttpRequest);
    }

    @Override
    public Optional<RequestBody> modifyHttpContent(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        if (!context.requestBody().isPresent() || !shouldCompress(context, executionAttributes)) {
            return context.requestBody();
        }

        Compressor compressor = resolveCompressionType(executionAttributes);
        RequestBody requestBody = context.requestBody().get();

        if (isTransferEncodingChunked(context)) {
            InputStream compressedStream = compressor.compress(requestBody.contentStreamProvider().newStream());
            try {
                byte[] compressedBytes = IoUtils.toByteArray(compressedStream);
                return Optional.of(RequestBody.fromBytes(compressedBytes));
            } catch (IOException e) {
                throw SdkClientException.create(e.getMessage(), e);
            }
        }

        CompressionContentStreamProvider streamProvider =
            new CompressionContentStreamProvider(requestBody.contentStreamProvider(), compressor);
        return Optional.of(RequestBody.fromContentProvider(streamProvider, requestBody.contentType()));
    }

    @Override
    public Optional<AsyncRequestBody> modifyAsyncHttpContent(Context.ModifyHttpRequest context,
                                                             ExecutionAttributes executionAttributes) {
        if (!context.asyncRequestBody().isPresent() || !shouldCompress(context, executionAttributes)) {
            return context.asyncRequestBody();
        }

        AsyncRequestBody asyncRequestBody = context.asyncRequestBody().get();
        Compressor compressor = resolveCompressionType(executionAttributes);

        return Optional.of(CompressionAsyncRequestBody.builder()
                                                      .asyncRequestBody(asyncRequestBody)
                                                      .compressor(compressor)
                                                      .build());
    }

    private static boolean shouldCompress(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        if (executionAttributes.getAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION) == null) {
            return false;
        }
        if (resolveCompressionType(executionAttributes) == null) {
            return false;
        }
        if (!resolveRequestCompressionEnabled(context, executionAttributes)) {
            return false;
        }
        if (executionAttributes.getAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION).isStreaming()) {
            return true;
        }
        SdkHttpFullRequest sdkHttpFullRequest = (SdkHttpFullRequest) context.httpRequest();
        if (!sdkHttpFullRequest.contentStreamProvider().isPresent()) {
            return false;
        }
        return isRequestSizeWithinThreshold(context, executionAttributes);
    }

    private static SdkHttpRequest updateContentEncodingHeader(SdkHttpRequest sdkHttpRequest, Compressor compressor) {
        if (sdkHttpRequest.firstMatchingHeader("Content-encoding").isPresent()) {
            return sdkHttpRequest.copy(r -> r.appendHeader("Content-encoding", compressor.compressorType()));
        }

        return sdkHttpRequest.copy(r -> r.putHeader("Content-encoding", compressor.compressorType()));
    }

    private static SdkHttpRequest updateContentLengthHeader(SdkHttpRequest sdkHttpRequest) {
        SdkHttpFullRequest sdkHttpFullRequest = (SdkHttpFullRequest) sdkHttpRequest;
        InputStream inputStream = sdkHttpFullRequest.contentStreamProvider().get().newStream();
        try {
            byte[] bytes = IoUtils.toByteArray(inputStream);
            String length = String.valueOf(bytes.length);
            return sdkHttpRequest.copy(r -> r.putHeader("Content-Length", length));
        } catch (IOException e) {
            throw SdkClientException.create(e.getMessage(), e);
        }
    }

    private boolean isTransferEncodingChunked(Context.ModifyHttpRequest context) {
        SdkHttpRequest sdkHttpRequest = context.httpRequest();
        Optional<String> transferEncodingHeader = sdkHttpRequest.firstMatchingHeader("Transfer-Encoding");
        if (transferEncodingHeader.isPresent() && transferEncodingHeader.get().equals("chunked")) {
            return true;
        }
        return false;
    }

    private static Compressor resolveCompressionType(ExecutionAttributes executionAttributes) {
        List<String> encodings =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION).getEncodings();

        for (String encoding: encodings) {
            CompressionType compressionType = CompressionType.fromValue(encoding.toLowerCase(Locale.ROOT));
            if (compressionType == CompressionType.UNKNOWN_TO_SDK_VERSION) {
                continue;
            }
            return Compressor.forCompressorType(compressionType);
        }
        return null;
    }

    private static boolean resolveRequestCompressionEnabled(Context.ModifyHttpRequest context,
                                                            ExecutionAttributes executionAttributes) {

        if (context.request().overrideConfiguration().isPresent()
            && context.request().overrideConfiguration().get().requestCompressionConfiguration().isPresent()) {
            Boolean requestCompressionEnabled = context.request().overrideConfiguration().get()
                                                       .requestCompressionConfiguration().get()
                                                       .requestCompressionEnabled();
            if (requestCompressionEnabled != null) {
                return requestCompressionEnabled;
            }
        }

        if (executionAttributes.getAttribute(SdkExecutionAttribute.REQUEST_COMPRESSION_CONFIGURATION) != null) {
            Boolean requestCompressionEnabled = executionAttributes.getAttribute(
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

    private static boolean isRequestSizeWithinThreshold(Context.ModifyHttpRequest context,
                                                        ExecutionAttributes executionAttributes) {
        int minimumCompressionThreshold = resolveMinCompressionSize(context, executionAttributes);
        validateMinCompressionSizeInput(minimumCompressionThreshold);

        SdkHttpFullRequest sdkHttpFullRequest = (SdkHttpFullRequest) context.httpRequest();
        long contentLength = Long.parseLong(sdkHttpFullRequest.firstMatchingHeader("Content-Length").orElse("0"));
        return contentLength >= minimumCompressionThreshold;
    }

    private static int resolveMinCompressionSize(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {

        if (context.request().overrideConfiguration().isPresent()
            && context.request().overrideConfiguration().get().requestCompressionConfiguration().isPresent()) {
            Integer minCompressionSize = context.request().overrideConfiguration().get()
                                                .requestCompressionConfiguration().get()
                                                .minimumCompressionThresholdInBytes();
            if (minCompressionSize != null) {
                return minCompressionSize;
            }
        }

        if (executionAttributes.getAttribute(SdkExecutionAttribute.REQUEST_COMPRESSION_CONFIGURATION) != null) {
            Integer minCompressionSize = executionAttributes.getAttribute(SdkExecutionAttribute.REQUEST_COMPRESSION_CONFIGURATION)
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

    static final class CompressionContentStreamProvider implements ContentStreamProvider {
        private final ContentStreamProvider underlyingInputStreamProvider;
        private InputStream currentStream;
        private final Compressor compressor;

        CompressionContentStreamProvider(ContentStreamProvider underlyingInputStreamProvider, Compressor compressor) {
            this.underlyingInputStreamProvider = underlyingInputStreamProvider;
            this.compressor = compressor;
        }

        @Override
        public InputStream newStream() {
            closeCurrentStream();
            currentStream = AwsCompressionInputStream.builder()
                                                     .inputStream(underlyingInputStreamProvider.newStream())
                                                     .compressor(compressor)
                                                     .build();
            return currentStream;
        }

        private void closeCurrentStream() {
            if (currentStream != null) {
                IoUtils.closeQuietly(currentStream, null);
                currentStream = null;
            }
        }
    }
}
