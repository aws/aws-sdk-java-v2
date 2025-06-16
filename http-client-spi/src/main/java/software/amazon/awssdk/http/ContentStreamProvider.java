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

package software.amazon.awssdk.http;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.internal.EnumUtils;

/**
 * Provides the content stream of a request.
 * <p>
 * Each call to the {@link #newStream()} method must result in a stream whose position is at the beginning of the content.
 * Implementations may return a new stream or the same stream for each call. If returning a new stream, the implementation
 * must ensure to {@code close()} and free any resources acquired by the previous stream. The last stream returned by {@link
 * #newStream()}} will be closed by the SDK.
 */
@SdkPublicApi
@FunctionalInterface
public interface ContentStreamProvider {
    /**
     * Create {@link ContentStreamProvider} from a byte array. This will copy the contents of the byte array.
     */
    static ContentStreamProvider fromByteArray(byte[] bytes) {
        Validate.paramNotNull(bytes, "bytes");
        byte[] copy = Arrays.copyOf(bytes, bytes.length);
        return fromByteArrayUnsafe(copy);
    }

    /**
     * Create {@link ContentStreamProvider} from a byte array <b>without</b> copying the contents of the byte array.
     * This introduces concurrency risks, allowing the caller to modify the byte array stored in this
     * {@code ContentStreamProvider} implementation.
     *
     * <p>As the method name implies, this is unsafe. Use {@link #fromByteArray(byte[])} unless you're sure you know
     * the risks.
     */
    static ContentStreamProvider fromByteArrayUnsafe(byte[] bytes) {
        Validate.paramNotNull(bytes, "bytes");
        return new ContentStreamProvider() {
            @Override
            public InputStream newStream() {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public String name() {
                return ProviderType.BYTES.getName();
            }
        };
    }

    /**
     * Create {@link ContentStreamProvider} from a string, using the provided charset.
     */
    static ContentStreamProvider fromString(String string, Charset charset) {
        Validate.paramNotNull(string, "string");
        Validate.paramNotNull(charset, "charset");
        return new ContentStreamProvider() {
            @Override
            public InputStream newStream() {
                return new StringInputStream(string, charset);
            }

            @Override
            public String name() {
                return ProviderType.STRING.getName();
            }
        };
    }

    /**
     * Create {@link ContentStreamProvider} from a string, using the UTF-8 charset.
     */
    static ContentStreamProvider fromUtf8String(String string) {
        return fromString(string, StandardCharsets.UTF_8);
    }

    /**
     * Create a {@link ContentStreamProvider} from an input stream.
     * <p>
     * If the provided input stream supports mark/reset, the stream will be marked with a 128Kb read limit and reset
     * each time {@link #newStream()} is invoked. If the provided input stream does not support mark/reset,
     * {@link #newStream()} will return the provided stream once, but fail subsequent calls. To create new streams when
     * needed instead of using mark/reset, see {@link #fromInputStreamSupplier(Supplier)}.
     */
    static ContentStreamProvider fromInputStream(InputStream inputStream) {
        Validate.paramNotNull(inputStream, "inputStream");
        IoUtils.markStreamWithMaxReadLimit(inputStream);
        return new ContentStreamProvider() {
            private boolean first = true;
            @Override
            public InputStream newStream() {
                if (first) {
                    first = false;
                    return inputStream;
                }

                if (inputStream.markSupported()) {
                    invokeSafely(inputStream::reset);
                    return inputStream;
                }

                throw new IllegalStateException("Content input stream does not support mark/reset, "
                                                + "and was already read once.");
            }

            @Override
            public String name() {
                return ProviderType.STREAM.getName();
            }
        };
    }

    /**
     * Create {@link ContentStreamProvider} from an input stream supplier. Each time a new stream is retrieved from
     * this content stream provider, the last one returned will be closed.
     */
    static ContentStreamProvider fromInputStreamSupplier(Supplier<InputStream> inputStreamSupplier) {
        Validate.paramNotNull(inputStreamSupplier, "inputStreamSupplier");
        return new ContentStreamProvider() {
            private InputStream lastStream;

            @Override
            public InputStream newStream() {
                if (lastStream != null) {
                    invokeSafely(lastStream::close);
                }
                lastStream = inputStreamSupplier.get();
                return lastStream;
            }

            @Override
            public String name() {
                return ProviderType.STREAM.getName();
            }
        };
    }

    /**
     * @return The content stream.
     */
    InputStream newStream();

    /**
     * Each ContentStreamProvider should return a well-formed name that can be used to identify the implementation.
     * The stream name should only include alphanumeric characters.
     *
     * @return String containing the identifying name of this ContentStreamProvider implementation.
     */
    default String name() {
        return ProviderType.UNKNOWN.getName();
    }

    @SdkProtectedApi
    enum ProviderType {
        FILE("File", "f"),
        BYTES("Bytes", "b"),
        STRING("String", "c"),
        STREAM("Stream", "s"),
        UNKNOWN("Unknown", "u");

        private static final Map<String, ProviderType> VALUE_MAP =
            EnumUtils.uniqueIndex(ProviderType.class, ProviderType::getName);

        private final String name;
        private final String shortValue;


        ProviderType(String name, String shortValue) {
            this.name = name;
            this.shortValue = shortValue;
        }

        public String getName() {
            return name;
        }

        public String getShortValue() {
            return shortValue;
        }

        public static String shortValueFromName(String name) {
            return VALUE_MAP.getOrDefault(name, UNKNOWN).getShortValue();
        }
    }
}
