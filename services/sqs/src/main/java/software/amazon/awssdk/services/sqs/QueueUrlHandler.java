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

package software.amazon.awssdk.services.sqs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Custom request handler for SQS that processes the request before it gets routed to the client
 * runtime layer.
 * <p>
 * SQS MessageQueue operations take a QueueUrl parameter that needs special handling to update the
 * endpoint and resource path on the request before it's executed.
 */
@ReviewBeforeRelease("Do we still want to do this?")
public class QueueUrlHandler extends RequestHandler {
    private static final String QUEUE_URL_PARAMETER = "QueueUrl";

    public SdkHttpFullRequest beforeRequest(SdkHttpFullRequest request) {

        final Map<String, List<String>> requestParams = request.getParameters();
        final List<String> queueUrlParam = requestParams.get(QUEUE_URL_PARAMETER);
        if (queueUrlParam != null && !queueUrlParam.isEmpty()) {
            List<String> queueUrlParameter = requestParams.remove(QUEUE_URL_PARAMETER);
            String queueUrl = queueUrlParameter.iterator().next();

            try {
                URI uri = new URI(queueUrl);
                SdkHttpFullRequest.Builder mutableRequest = request.toBuilder();
                mutableRequest.resourcePath(uri.getPath());

                if (uri.getHost() != null) {
                    // If the URI has a host specified, set the request's endpoint to the queue URLs
                    // endpoint, so that queue URLs from different regions will send the request to
                    // the correct endpoint.
                    URI uriWithoutPath = new URI(uri.toString().replace(uri.getPath(), ""));
                    mutableRequest.endpoint(uriWithoutPath);
                }
                return mutableRequest.build();
            } catch (URISyntaxException e) {
                throw new AmazonClientException("Unable to parse SQS queue URL '" + queueUrl + "'", e);
            }
        }
        return request;
    }
}
