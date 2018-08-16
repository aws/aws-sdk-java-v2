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

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.utils.DateUtils;

@SdkInternalApi
public final class ClockSkewUtil {

    private static final Logger log = LoggerFactory.getLogger(ClockSkewUtil.class);

    private ClockSkewUtil() {}

    /**
     * Returns the difference between the client's clock time and the service clock time in unit
     * of seconds.
     */
    public static int parseClockSkewOffset(HttpResponse httpResponse) {
        Optional<String> dateHeader = Optional.ofNullable(httpResponse.getHeader("Date"));
        try {
            Instant serverDate = dateHeader
                .filter(h -> !h.isEmpty())
                .map(DateUtils::parseRfc1123Date)
                .orElseThrow(() -> new RuntimeException(
                    "Unable to parse clock skew offset from response. Server Date header missing"));
            long diff = Duration.between(Instant.now(), serverDate).getSeconds();
            return (int) diff;
        } catch (RuntimeException e) {
            log.warn("Unable to parse clock skew offset from response: " + dateHeader.orElse(""), e);
            return 0;
        }
    }
}
