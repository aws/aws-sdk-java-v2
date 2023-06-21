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
public enum FailureReason {
    NO_AVAILABLE_CONTACTS("NO_AVAILABLE_CONTACTS"),

    ADDITIONAL_VERIFICATION_REQUIRED("ADDITIONAL_VERIFICATION_REQUIRED"),

    DOMAIN_NOT_ALLOWED("DOMAIN_NOT_ALLOWED"),

    INVALID_PUBLIC_DOMAIN("INVALID_PUBLIC_DOMAIN"),

    DOMAIN_VALIDATION_DENIED("DOMAIN_VALIDATION_DENIED"),

    CAA_ERROR("CAA_ERROR"),

    PCA_LIMIT_EXCEEDED("PCA_LIMIT_EXCEEDED"),

    PCA_INVALID_ARN("PCA_INVALID_ARN"),

    PCA_INVALID_STATE("PCA_INVALID_STATE"),

    PCA_REQUEST_FAILED("PCA_REQUEST_FAILED"),

    PCA_NAME_CONSTRAINTS_VALIDATION("PCA_NAME_CONSTRAINTS_VALIDATION"),

    PCA_RESOURCE_NOT_FOUND("PCA_RESOURCE_NOT_FOUND"),

    PCA_INVALID_ARGS("PCA_INVALID_ARGS"),

    PCA_INVALID_DURATION("PCA_INVALID_DURATION"),

    PCA_ACCESS_DENIED("PCA_ACCESS_DENIED"),

    SLR_NOT_FOUND("SLR_NOT_FOUND"),

    OTHER("OTHER"),

    UNKNOWN_TO_SDK_VERSION(null);

    private static final Map<String, FailureReason> VALUE_MAP = EnumUtils.uniqueIndex(FailureReason.class,
            FailureReason::toString);

    private final String value;

    private FailureReason(String value) {
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
     * @return FailureReason corresponding to the value
     */
    public static FailureReason fromValue(String value) {
        if (value == null) {
            return null;
        }
        return VALUE_MAP.getOrDefault(value, UNKNOWN_TO_SDK_VERSION);
    }

    /**
     * Use this in place of {@link #values()} to return a {@link Set} of all values known to the SDK. This will return
     * all known enum values except {@link #UNKNOWN_TO_SDK_VERSION}.
     *
     * @return a {@link Set} of known {@link FailureReason}s
     */
    public static Set<FailureReason> knownValues() {
        Set<FailureReason> knownValues = EnumSet.allOf(FailureReason.class);
        knownValues.remove(UNKNOWN_TO_SDK_VERSION);
        return knownValues;
    }
}
