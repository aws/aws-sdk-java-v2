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

package software.amazon.awssdk.services.ec2.transform;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSpotFleetRequestHistoryRequest;
import software.amazon.awssdk.services.ec2.model.RequestSpotFleetRequest;

/**
 * A request handler that strips out millisecond precision from requests to
 * RequestSpotFleet and DescribeSpotFleetRequestHistory, which don't expect
 * timestamps to be so precise.
 */
public final class TimestampFormatInterceptor implements ExecutionInterceptor {

    private static final Pattern PATTERN = Pattern.compile("\\.\\d\\d\\dZ");

    private static final String START_TIME = "StartTime";
    private static final String VALID_FROM = "SpotFleetRequestConfig.ValidFrom";
    private static final String VALID_UNTIL = "SpotFleetRequestConfig.ValidUntil";

    @Override
    public SdkHttpFullRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        SdkHttpFullRequest request = context.httpRequest();
        Object original = context.request();
        if (original instanceof DescribeSpotFleetRequestHistoryRequest) {
            Map<String, List<String>> params = request.rawQueryParameters();
            List<String> startTime = params.get(START_TIME);

            if (startTime != null && !startTime.isEmpty()) {
                return request.toBuilder()
                              .rawQueryParameter(START_TIME, sanitize(startTime.get(0)))
                              .build();
            }

        } else if (original instanceof RequestSpotFleetRequest) {

            Map<String, List<String>> params = request.rawQueryParameters();

            List<String> validFrom = params.get(VALID_FROM);
            List<String> validUntil = params.get(VALID_UNTIL);

            return request.toBuilder().apply(builder -> {
                if (validFrom != null && !validFrom.isEmpty()) {
                    builder.rawQueryParameter(VALID_FROM, sanitize(validFrom.get(0)));
                }
                if (validUntil != null && !validUntil.isEmpty()) {
                    builder.rawQueryParameter(VALID_UNTIL, sanitize(validUntil.get(0)));
                }
            }).build();

        }
        return request;
    }

    private String sanitize(String input) {
        return PATTERN.matcher(input).replaceFirst("Z");
    }
}
