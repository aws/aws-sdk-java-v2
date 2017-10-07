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


package software.amazon.awssdk.core.http.pipeline.stages;

import static software.amazon.awssdk.core.event.SdkProgressPublisher.publishRequestContentLength;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.RequestExecutionContext;
import software.amazon.awssdk.core.http.StreamManagingStage;
import software.amazon.awssdk.core.http.pipeline.RequestToRequestPipeline;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Report the Content-Length of the request input stream to the {@link software.amazon.awssdk.core.event.ProgressListener}.
 */
public class ReportRequestContentLengthStage implements RequestToRequestPipeline {

    private static final Logger log = LoggerFactory.getLogger(StreamManagingStage.class);

    @Override
    public SdkHttpFullRequest execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        try {
            request.firstMatchingHeader("Content-Length")
                   .map(Long::parseLong)
                   .ifPresent(l -> publishRequestContentLength(context.requestConfig().getProgressListener(), l));
        } catch (NumberFormatException e) {
            log.warn("Cannot parse the Content-Length header of the request.");
        }
        return request;
    }
}
