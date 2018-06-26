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

import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * The base class for all SDK responses.
 *
 * @see SdkRequest
 */
@Immutable
@SdkPublicApi
public abstract class SdkResponse {

    private final SdkHttpResponse sdkHttpResponse;

    protected SdkResponse(Builder builder) {
        this.sdkHttpResponse = builder.sdkHttpResponse();
    }

    /**
     * @return HTTP response data returned from the service.
     *
     * @see SdkHttpResponse
     */
    public SdkHttpResponse sdkHttpResponse() {
        return sdkHttpResponse;
    }

    /**
     * Used to retrieve the value of a field from any class that extends {@link SdkResponse}. The field name
     * specified should match the member name from the corresponding service-2.json model specified in the
     * codegen-resources folder for a given service. The class specifies what class to cast the returned value to.
     * If the returned value is also a modeled class, the {@link #getValueForField(String, Class)} method will
     * again be available.
     *
     * @param fieldName The name of the member to be retrieved.
     * @param clazz The class to cast the returned object to.
     * @return Optional containing the casted return value
     */
    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        return Optional.empty();
    }

    public abstract Builder toBuilder();

    public interface Builder {

        Builder sdkHttpResponse(SdkHttpResponse sdkHttpResponse);

        SdkHttpResponse sdkHttpResponse();

        SdkResponse build();
    }

    protected abstract static class BuilderImpl implements Builder {

        private SdkHttpResponse sdkHttpResponse;

        protected BuilderImpl() {
        }

        protected BuilderImpl(SdkResponse response) {
            this.sdkHttpResponse = response.sdkHttpResponse();
        }

        @Override
        public Builder sdkHttpResponse(SdkHttpResponse sdkHttpResponse) {
            this.sdkHttpResponse = sdkHttpResponse;
            return this;
        }

        @Override
        public SdkHttpResponse sdkHttpResponse() {
            return sdkHttpResponse;
        }
    }
}
