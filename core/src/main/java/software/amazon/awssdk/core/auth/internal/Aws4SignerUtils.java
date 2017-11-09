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

package software.amazon.awssdk.core.auth.internal;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Utility methods that is used by the different AWS Signer implementations.
 * This class is strictly internal and is subjected to change.
 */
public final class Aws4SignerUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC"));

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneId.of("UTC"));

    private Aws4SignerUtils() {
    }

    /**
     * Returns a string representation of the given date time in yyyyMMdd
     * format. The date returned is in the UTC zone.
     *
     * For example, given a time "1416863450581", this method returns "20141124"
     */
    public static String formatDateStamp(long timeMilli) {
        return DATE_FORMATTER.format(Instant.ofEpochMilli(timeMilli));
    }

    /**
     * Returns a string representation of the given date time in
     * yyyyMMdd'T'HHmmss'Z' format. The date returned is in the UTC zone.
     *
     * For example, given a time "1416863450581", this method returns
     * "20141124T211050Z"
     */
    public static String formatTimestamp(long timeMilli) {
        return TIME_FORMATTER.format(Instant.ofEpochMilli(timeMilli));
    }
}
