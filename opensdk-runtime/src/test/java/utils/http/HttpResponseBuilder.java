/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package utils.http;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.http.HttpResponse;

public class HttpResponseBuilder {

    private final Map<String, String> headers = new HashMap<>();

    private int statusCode = 200;

    public HttpResponseBuilder withHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public HttpResponseBuilder withStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public HttpResponse build() {
        HttpResponse httpResponse = new HttpResponse(null);
        headers.entrySet()
                .stream()
                .forEach(e -> httpResponse.addHeader(e.getKey(), e.getValue()));
        httpResponse.setStatusCode(statusCode);
        return httpResponse;
    }

}
