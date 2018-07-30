/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.ResponseTransformer;
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

    ResponseBytes(ResponseT response, byte[] bytes) {
        super(bytes);
        this.response = Validate.paramNotNull(response, "response");
    }

    public static <ResponseT> ResponseBytes<ResponseT> fromByteArray(ResponseT response, byte[] bytes) {
        return new ResponseBytes<>(response, Arrays.copyOf(bytes, bytes.length));
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
                       .add("bytes", wrappedBytes())
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

        final ResponseBytes<?> that = (ResponseBytes<?>) o;

        return response != null ? response.equals(that.response) : that.response == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (response != null ? response.hashCode() : 0);
        return result;
    }
}
