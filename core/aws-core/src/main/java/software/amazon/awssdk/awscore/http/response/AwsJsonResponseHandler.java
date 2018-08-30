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

package software.amazon.awssdk.awscore.http.response;

import static software.amazon.awssdk.awscore.internal.DefaultAwsResponseMetadata.AWS_REQUEST_ID;

import com.fasterxml.jackson.core.JsonFactory;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.awscore.internal.DefaultAwsResponseMetadata;
import software.amazon.awssdk.core.http.JsonResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;

@SdkProtectedApi
public final class AwsJsonResponseHandler<T> extends JsonResponseHandler<T> {

    public AwsJsonResponseHandler(Unmarshaller<T, JsonUnmarshallerContext> responseUnmarshaller, Map<Class<?>, Unmarshaller<?,
        JsonUnmarshallerContext>> simpleTypeUnmarshallers, JsonFactory jsonFactory, boolean needsConnectionLeftOpen,
                                  boolean isPayloadJson) {
        super(responseUnmarshaller, simpleTypeUnmarshallers, jsonFactory, needsConnectionLeftOpen, isPayloadJson);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) throws Exception {
        T result = super.handle(response, executionAttributes);

        // As T is not bounded to AwsResponse, we need to do explicitly cast here.
        if (result instanceof AwsResponse) {
            AwsResponseMetadata responseMetadata = generateResponseMetadata(response);
            return (T) ((AwsResponse) result).toBuilder().responseMetadata(responseMetadata).build();
        }

        return result;
    }

    /**
     * Create the default {@link AwsResponseMetadata}.
     */
    private AwsResponseMetadata generateResponseMetadata(SdkHttpResponse response) {
        Map<String, String> metadata = new HashMap<>();

        metadata.put(AWS_REQUEST_ID, response.firstMatchingHeader(X_AMZN_REQUEST_ID_HEADER).orElse(null));
        response.headers().forEach((key, value) -> metadata.put(key, value.get(0)));

        return DefaultAwsResponseMetadata.create(metadata);
    }
}
