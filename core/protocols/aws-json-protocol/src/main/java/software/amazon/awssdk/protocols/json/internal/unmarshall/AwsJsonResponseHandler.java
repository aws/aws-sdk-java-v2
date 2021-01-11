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

package software.amazon.awssdk.protocols.json.internal.unmarshall;

import static software.amazon.awssdk.awscore.util.AwsHeader.AWS_REQUEST_ID;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.awscore.DefaultAwsResponseMetadata;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

@SdkInternalApi
public final class AwsJsonResponseHandler<T> implements HttpResponseHandler<T> {

    private final HttpResponseHandler<T> responseHandler;

    public AwsJsonResponseHandler(HttpResponseHandler<T> responseHandler) {
        this.responseHandler = responseHandler;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) throws Exception {
        T result = responseHandler.handle(response, executionAttributes);

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

        metadata.put(AWS_REQUEST_ID, SdkHttpUtils.firstMatchingHeaderFromCollection(response.headers(),
                                                                                    X_AMZN_REQUEST_ID_HEADERS)
                                                 .orElse(null));
        response.headers().forEach((key, value) -> metadata.put(key, value.get(0)));

        return DefaultAwsResponseMetadata.create(metadata);
    }
}
