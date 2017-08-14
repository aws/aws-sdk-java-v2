/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Generic representation of an HTTP response.
 *
 * TODO should we allow HTTP impl authors to implement this or should they only use the builder?
 */
public interface SdkHttpFullResponse
        extends SdkHttpResponse, ToCopyableBuilder<SdkHttpFullResponse.Builder, SdkHttpFullResponse> {

    /**
     * Returns the input stream containing the response content. Instance of {@link AbortableInputStream}
     * which can be aborted before all content is read, this usually means killing the underlying HTTP
     * connection but it's up to HTTP implementations to define.
     * <br/>
     * May be null, not all responses have content.
     *
     * @return The input stream containing the response content or null if there is no content.
     */
    AbortableInputStream getContent();

    /**
     * @return Builder instance to construct a {@link DefaultSdkHttpFullResponse}.
     */
    static Builder builder() {
        return new DefaultSdkHttpFullResponse.Builder();
    }

    /**
     * Builder for a {@link DefaultSdkHttpFullResponse}.
     */
    interface Builder extends CopyableBuilder<Builder, SdkHttpFullResponse> {

        Builder statusText(String statusText);

        Builder statusCode(int statusCode);

        Builder content(AbortableInputStream content);

        @ReviewBeforeRelease("Should we only allow setting the AbortableInputStream?")
        Builder content(InputStream content);

        Builder headers(Map<String, List<String>> headers);

        @ReviewBeforeRelease("This is completely different from the HTTP request. "
                             + "This should be the same between both interfaces.")
        Builder addHeader(String headerName, List<String> headerValues);
    }
}
