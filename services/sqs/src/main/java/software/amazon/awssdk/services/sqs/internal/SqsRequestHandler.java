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

package software.amazon.awssdk.services.sqs.internal;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.SdkHttpFullRequest;

public class SqsRequestHandler extends RequestHandler {

    private static final Map<String, String> NONSTANDARD_ENDPOINT_MAP = new HashMap<>();

    static {
        NONSTANDARD_ENDPOINT_MAP.put("queue.amazonaws.com", "sqs.us-east-1.amazonaws.com");
        NONSTANDARD_ENDPOINT_MAP.put("us-west-1.queue.amazonaws.com", "sqs.us-west-1.amazonaws.com");
        NONSTANDARD_ENDPOINT_MAP.put("us-west-2.queue.amazonaws.com", "sqs.us-west-2.amazonaws.com");
        NONSTANDARD_ENDPOINT_MAP.put("eu-west-1.queue.amazonaws.com", "sqs.eu-west-1.amazonaws.com");
        NONSTANDARD_ENDPOINT_MAP.put("ap-southeast-1.queue.amazonaws.com", "sqs.ap-southeast-1.amazonaws.com");
        NONSTANDARD_ENDPOINT_MAP.put("ap-northeast-1.queue.amazonaws.com", "sqs.ap-northeast-1.amazonaws.com");
        NONSTANDARD_ENDPOINT_MAP.put("sa-east-1.queue.amazonaws.com", "sqs.sa-east-1.amazonaws.com");
        NONSTANDARD_ENDPOINT_MAP.put("us-gov-west-1.queue.amazonaws.com", "sqs.us-gov-west-1.amazonaws.com");
        NONSTANDARD_ENDPOINT_MAP.put("ap-southeast-2.queue.amazonaws.com", "sqs.ap-southeast-2.amazonaws.com");
    }

    @Override
    public SdkHttpFullRequest beforeRequest(SdkHttpFullRequest request) {
        URI endpoint = request.getEndpoint();

        // If the request is using a non-standard endpoint, then
        // alter it to use the corresponding, standard endpoint
        if (NONSTANDARD_ENDPOINT_MAP.containsKey(endpoint.getHost())) {
            String newHost = NONSTANDARD_ENDPOINT_MAP.get(endpoint.getHost());
            String newEndpoint = endpoint.toString().replaceFirst(endpoint.getHost(), newHost);
            return request.toBuilder()
                          .endpoint(URI.create(newEndpoint))
                          .build();
        }
        return request;
    }
}
