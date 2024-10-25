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

package software.amazon.awssdk.core.useragent;

import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.COMMA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.CollectionUtils;

// CHECKSTYLE:OFF
@SdkProtectedApi
public class BusinessMetrics {
    // CHECKSTYLE:ON
    public static final int MAX_METRICS_STRING_IN_BYTES = 1024;

    public static final UnaryOperator<String> METRIC_SEARCH_PATTERN = metric -> ".*m/[a-zA-Z0-9+-,]*" + metric + ".*";

    private final List<String> recordedMetrics;
    private final int maxLengthInBytes;

    public BusinessMetrics() {
        this(MAX_METRICS_STRING_IN_BYTES);
    }

    public BusinessMetrics(int maxMetricsStringInBytes) {
        recordedMetrics = new ArrayList<>();
        this.maxLengthInBytes = maxMetricsStringInBytes;
    }

    public List<String> recordedMetrics() {
        return Collections.unmodifiableList(recordedMetrics);
    }

    public void addMetric(String metric) {
        recordedMetrics.add(metric);
    }

    public void merge(Collection<String> additionalMetrics) {
        if (!CollectionUtils.isNullOrEmpty(additionalMetrics)) {
            recordedMetrics.addAll(additionalMetrics);
        }
    }

    /**
     * Constructs a string representation of a collection of business metrics strings in Base64 formats.
     * The resulting string has a maximum length of {@link #MAX_METRICS_STRING_IN_BYTES} bytes.
     */
    public String asBoundedString() {
        String recordedMetricsString = String.join(COMMA, recordedMetrics);
        return checkSizeAndShortenIfNeeded(recordedMetricsString, maxLengthInBytes);
    }

    private static String checkSizeAndShortenIfNeeded(String commaSeparated, int maxAllowableLength) {
        if (commaSeparated.length() <= maxAllowableLength) {
            return commaSeparated;
        }
        return shortenToBeforeNearestComma(commaSeparated, maxAllowableLength);
    }

    private static String shortenToBeforeNearestComma(String commaSeparated, int maxAllowableLength) {
        boolean endsBeforeComma = commaSeparated.charAt(maxAllowableLength) == ',';
        if (endsBeforeComma) {
            return commaSeparated.substring(0, maxAllowableLength);
        }

        boolean endsOnComma = commaSeparated.charAt(maxAllowableLength - 1) == ',';
        if (endsOnComma) {
            return commaSeparated.substring(0, maxAllowableLength - 1);
        }
        String maxAllowableString = commaSeparated.substring(0, maxAllowableLength);
        int lastCommaIndex = maxAllowableString.lastIndexOf(',');
        return maxAllowableString.substring(0, lastCommaIndex);
    }
}
