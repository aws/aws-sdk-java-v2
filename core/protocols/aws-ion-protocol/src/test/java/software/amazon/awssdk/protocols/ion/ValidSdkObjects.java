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

package software.amazon.awssdk.protocols.ion;

import java.net.URI;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;

/**
 * A collection of objects (or object builder) pre-populated with all required fields. This allows tests to focus on what data
 * they care about, not necessarily what data is required.
 */
public final class ValidSdkObjects {
    private ValidSdkObjects() {}

    public static SdkHttpFullRequest.Builder sdkHttpFullRequest() {
        return SdkHttpFullRequest.builder()
                                 .uri(URI.create("http://test.com:80"))
                                 .method(SdkHttpMethod.GET);
    }

    public static SdkHttpFullResponse.Builder sdkHttpFullResponse() {
        return SdkHttpFullResponse.builder()
                                  .statusCode(200);
    }
}
