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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awssdk.annotation.Immutable;
import software.amazon.awssdk.annotation.SdkInternalApi;

/**
 * Represents an HTTP response returned by an AWS service in response to a
 * service request.
 */
@SdkInternalApi
@Immutable
class DefaultSdkHttpFullResponse implements SdkHttpFullResponse {

    private final String statusText;
    private final int statusCode;
    private final AbortableInputStream content;
    private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    DefaultSdkHttpFullResponse(Builder builder) {
        this.statusCode = builder.statusCode;
        this.statusText = builder.statusText;
        this.content = builder.content;
        this.headers.putAll(builder.headers);
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    @Override
    public Collection<String> getValuesForHeader(String header) {
        Collection<String> values = headers.get(header);
        return values != null ? values : Collections.emptyList();
    }

    @Override
    public AbortableInputStream getContent() {
        return content;
    }

    @Override
    public String getStatusText() {
        return statusText;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

}
