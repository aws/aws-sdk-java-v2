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

package utils;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;

/**
 * A collection of objects (or object builder) pre-populated with all required fields. This allows tests to focus on what data
 * they care about, not necessarily what data is required.
 */
public final class ValidSdkObjects {
    private ValidSdkObjects() {}

    public static SdkRequest sdkRequest() {
        return new SdkRequest() {
            @Override
            public Optional<? extends RequestOverrideConfiguration> overrideConfiguration() {
                return Optional.empty();
            }

            @Override
            public Builder toBuilder() {
                return null;
            }

            @Override
            public List<SdkField<?>> sdkFields() {
                return null;
            }
        };
    }

    public static SdkHttpFullRequest.Builder sdkHttpFullRequest() {
        return sdkHttpFullRequest(80);
    }

    public static SdkHttpFullRequest.Builder sdkHttpFullRequest(int port) {
        return SdkHttpFullRequest.builder()
                                 .uri(URI.create("http://localhost"))
                                 .port(port)
                                 .putHeader("Host", "localhost")
                                 .method(SdkHttpMethod.GET);
    }

    public static SdkHttpFullResponse.Builder sdkHttpFullResponse() {
        return SdkHttpFullResponse.builder()
                                  .statusCode(200);
    }
}
