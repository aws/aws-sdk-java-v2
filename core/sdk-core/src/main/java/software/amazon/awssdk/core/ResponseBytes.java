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

package software.amazon.awssdk.core;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * An in-memory representation of the service's response from a streaming operation. This usually obtained by calling the "bytes"
 * form of a streaming operation, like S3's {@code getObjectBytes}. Can also be retrieved by passing
 * {@link ResponseTransformer#toBytes()} or {@link AsyncResponseTransformer#toBytes()} to a streaming output operation.
 */
@SdkPublicApi
public final class ResponseBytes<ResponseT> extends BytesWrapper {
    private final ResponseT response;

    private ResponseBytes(ResponseT response, byte[] bytes) {
        super(bytes);
        this.response = Validate.paramNotNull(response, "response");
    }

    /**
     * Create {@link ResponseBytes} from a Byte array. This will copy the contents of the byte array.
     */
    public static <ResponseT> ResponseBytes<ResponseT> fromInputStream(ResponseT response, InputStream stream)
            throws UncheckedIOException {
        return new ResponseBytes<>(response, invokeSafely(() -> IoUtils.toByteArray(stream)));
    }

    /**
     * Create {@link ResponseBytes} from a Byte array. This will copy the contents of the byte array.
     */
    public static <ResponseT> ResponseBytes<ResponseT> fromByteArray(ResponseT response, byte[] bytes) {
        return new ResponseBytes<>(response, Arrays.copyOf(bytes, bytes.length));
    }

    /**
     * Create {@link ResponseBytes} from a Byte array <b>without</b> copying the contents of the byte array. This introduces
     * concurrency risks, allowing: (1) the caller to modify the byte array stored in this {@code SdkBytes} implementation AND
     * (2) any users of {@link #asByteArrayUnsafe()} to modify the byte array passed into this {@code SdkBytes} implementation.
     *
     * <p>As the method name implies, this is unsafe. Use {@link #fromByteArray(Object, byte[])} unless you're sure you know the
     * risks.
     */
    public static <ResponseT> ResponseBytes<ResponseT> fromByteArrayUnsafe(ResponseT response, byte[] bytes) {
        return new ResponseBytes<>(response, bytes);
    }

    /**
     * @return the unmarshalled response object from the service.
     */
    public ResponseT response() {
        return response;
    }

    @Override
    public String toString() {
        return ToString.builder("ResponseBytes")
                       .add("response", response)
                       .add("bytes", asByteArrayUnsafe())
                       .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        ResponseBytes<?> that = (ResponseBytes<?>) o;

        return response != null ? response.equals(that.response) : that.response == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (response != null ? response.hashCode() : 0);
        return result;
    }
}
