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

package software.amazon.awssdk.core.internal.util;

import java.math.BigDecimal;
import java.time.Instant;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;

@SdkInternalApi
@ReviewBeforeRelease("This is AWS-specific, so it should be moved to the AWS module.")
public class AwsDateUtils {
    private static final int AWS_DATE_MILLI_SECOND_PRECISION = 3;

    private AwsDateUtils() {}

    /**
     * Parses the given date string returned by the AWS service into a Date
     * object.
     */
    public static Instant parseServiceSpecificInstant(String dateString) {
        if (dateString == null) {
            return null;
        }
        try {
            BigDecimal dateValue = new BigDecimal(dateString);
            return Instant.ofEpochMilli(dateValue.scaleByPowerOfTen(AWS_DATE_MILLI_SECOND_PRECISION).longValue());
        } catch (NumberFormatException nfe) {
            throw SdkClientException.builder()
                                    .message("Unable to parse date : " + dateString)
                                    .cause(nfe)
                                    .build();
        }
    }

    /**
     * Formats the give date object into an AWS Service format.
     */
    public static String formatServiceSpecificDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        BigDecimal dateValue = BigDecimal.valueOf(instant.toEpochMilli());
        return dateValue.scaleByPowerOfTen(0 - AWS_DATE_MILLI_SECOND_PRECISION)
                   .toPlainString();
    }
}
