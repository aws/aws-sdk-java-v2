/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.acm.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.utils.internal.EnumUtils;

@Generated("software.amazon.awssdk:codegen")
public enum RenewalStatus {
    PENDING_AUTO_RENEWAL("PENDING_AUTO_RENEWAL"),

    PENDING_VALIDATION("PENDING_VALIDATION"),

    SUCCESS("SUCCESS"),

    FAILED("FAILED"),

    UNKNOWN_TO_SDK_VERSION(null);

    private static final Map<String, RenewalStatus> VALUE_MAP = EnumUtils.uniqueIndex(RenewalStatus.class,
            RenewalStatus::toString);

    private final String value;

    private RenewalStatus(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    /**
     * Use this in place of valueOf to convert the raw string returned by the service into the enum value.
     *
     * @param value
     *        real value
     * @return RenewalStatus corresponding to the value
     */
    public static RenewalStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        return VALUE_MAP.getOrDefault(value, UNKNOWN_TO_SDK_VERSION);
    }

    /**
     * Use this in place of {@link #values()} to return a {@link Set} of all values known to the SDK. This will return
     * all known enum values except {@link #UNKNOWN_TO_SDK_VERSION}.
     *
     * @return a {@link Set} of known {@link RenewalStatus}s
     */
    public static Set<RenewalStatus> knownValues() {
        Set<RenewalStatus> knownValues = EnumSet.allOf(RenewalStatus.class);
        knownValues.remove(UNKNOWN_TO_SDK_VERSION);
        return knownValues;
    }
}
