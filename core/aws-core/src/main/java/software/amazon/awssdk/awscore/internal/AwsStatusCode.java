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

package software.amazon.awssdk.awscore.internal;

import static java.util.Collections.unmodifiableSet;

import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Contains AWS-specific meanings behind status codes.
 */
@SdkInternalApi
public class AwsStatusCode {
    public static final Set<Integer> POSSIBLE_CLOCK_SKEW_STATUS_CODES;

    static {
        Set<Integer> clockSkewErrorCodes = new HashSet<>(2);
        clockSkewErrorCodes.add(401);
        clockSkewErrorCodes.add(403);

        POSSIBLE_CLOCK_SKEW_STATUS_CODES = unmodifiableSet(clockSkewErrorCodes);
    }

    private AwsStatusCode() {
    }

    public static boolean isPossibleClockSkewStatusCode(int statusCode) {
        return POSSIBLE_CLOCK_SKEW_STATUS_CODES.contains(statusCode);
    }
}
