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

package software.amazon.awssdk.core.internal.useragent.businessmetrics;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.internal.EnumUtils;

/**
 * An enum class representing a short form of identity providers to record in the UA string.
 *
 * Unimplemented metrics: I,J,K,M-c,e-[latest]
 * Unsupported metrics (these will never be added): A,H
 */
@SdkInternalApi
public enum BusinessMetricFeatureId {

    WAITER("B"),
    PAGINATOR("C"),
    RETRY_MODE_LEGACY("D"),
    RETRY_MODE_STANDARD("E"),
    RETRY_MODE_ADAPTIVE("F"),
    S3_TRANSFER("G"),
    GZIP_REQUEST_COMPRESSION("L"), //TODO(metrics): Not working, compression happens after header
    DDB_MAPPER("d"),
    UNKNOWN("Unknown");

    private static final Pattern CLASS_NAME_CHARACTERS = Pattern.compile("[a-zA-Z_$\\d]{0,62}");
    private static final Map<String, BusinessMetricFeatureId> VALUE_MAP =
        EnumUtils.uniqueIndex(BusinessMetricFeatureId.class, BusinessMetricFeatureId::toString);
    private final String value;

    BusinessMetricFeatureId(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    /**
     * Map the given provider name to a shorter form. If null or empty, return unknown.
     * If not recognized, use the given string if it conforms to the accepted pattern.
     */
    public static Optional<String> mapFrom(String source) {
        if (StringUtils.isBlank(source)) {
            return Optional.of(UNKNOWN.name().toLowerCase(Locale.US));
        }
        return mappedName(source).map(mapping -> Optional.of(mapping.name().toLowerCase(Locale.US)))
                                 .orElseGet(() -> sanitizedProviderOrNull(source));
    }

    private static Optional<BusinessMetricFeatureId> mappedName(String value) {
        if (VALUE_MAP.containsKey(value)) {
            return Optional.of(VALUE_MAP.get(value));
        }
        return Optional.empty();
    }

    private static Optional<String> sanitizedProviderOrNull(String value) {
        if (hasAcceptedFormat(value)) {
            return Optional.of(value);
        }
        return Optional.empty();
    }

    private static boolean hasAcceptedFormat(String input) {
        return CLASS_NAME_CHARACTERS.matcher(input).matches();
    }
}
