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

package software.amazon.awssdk.services.ec2.transform;

import static java.util.Collections.singletonList;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.handlers.AwsHandlerKeys;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSpotFleetRequestHistoryRequest;
import software.amazon.awssdk.services.ec2.model.RequestSpotFleetRequest;

/**
 * A request handler that strips out millisecond precision from requests to
 * RequestSpotFleet and DescribeSpotFleetRequestHistory, which don't expect
 * timestamps to be so precise.
 */
public final class TimestampFormatRequestHandler extends RequestHandler {

    private static final Pattern PATTERN = Pattern.compile("\\.\\d\\d\\dZ");

    private static final String START_TIME = "StartTime";
    private static final String VALID_FROM = "SpotFleetRequestConfig.ValidFrom";
    private static final String VALID_UNTIL = "SpotFleetRequestConfig.ValidUntil";

    @Override
    public SdkHttpFullRequest beforeRequest(SdkHttpFullRequest request) {
        Object original = request.handlerContext(AwsHandlerKeys.REQUEST_CONFIG).getOriginalRequest();
        if (original instanceof DescribeSpotFleetRequestHistoryRequest) {
            Map<String, List<String>> params = request.getParameters();
            List<String> startTime = params.get(START_TIME);

            if (startTime != null && !startTime.isEmpty()) {
                return request.toBuilder()
                              .queryParameter(START_TIME, singletonList(sanitize(startTime.get(0))))
                              .build();
            }

        } else if (original instanceof RequestSpotFleetRequest) {

            Map<String, List<String>> params = request.getParameters();

            List<String> validFrom = params.get(VALID_FROM);
            List<String> validUntil = params.get(VALID_UNTIL);

            return request.toBuilder().apply(builder -> {
                if (validFrom != null && !validFrom.isEmpty()) {
                    builder.queryParameter(VALID_FROM, singletonList(sanitize(validFrom.get(0))));
                }
                if (validUntil != null && !validUntil.isEmpty()) {
                    builder.queryParameter(VALID_UNTIL, singletonList(sanitize(validUntil.get(0))));
                }
                return builder;
            }).build();

        }
        return request;
    }

    private String sanitize(String input) {
        return PATTERN.matcher(input).replaceFirst("Z");
    }

    @Override
    public void afterResponse(SdkHttpFullRequest request, Response<?> response) {
    }

    @Override
    public void afterError(
            SdkHttpFullRequest request,
            Response<?> response,
            Exception e) {
    }
}
