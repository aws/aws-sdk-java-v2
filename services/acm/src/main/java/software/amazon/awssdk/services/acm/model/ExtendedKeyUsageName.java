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
public enum ExtendedKeyUsageName {
    TLS_WEB_SERVER_AUTHENTICATION("TLS_WEB_SERVER_AUTHENTICATION"),

    TLS_WEB_CLIENT_AUTHENTICATION("TLS_WEB_CLIENT_AUTHENTICATION"),

    CODE_SIGNING("CODE_SIGNING"),

    EMAIL_PROTECTION("EMAIL_PROTECTION"),

    TIME_STAMPING("TIME_STAMPING"),

    OCSP_SIGNING("OCSP_SIGNING"),

    IPSEC_END_SYSTEM("IPSEC_END_SYSTEM"),

    IPSEC_TUNNEL("IPSEC_TUNNEL"),

    IPSEC_USER("IPSEC_USER"),

    ANY("ANY"),

    NONE("NONE"),

    CUSTOM("CUSTOM"),

    UNKNOWN_TO_SDK_VERSION(null);

    private static final Map<String, ExtendedKeyUsageName> VALUE_MAP = EnumUtils.uniqueIndex(ExtendedKeyUsageName.class,
            ExtendedKeyUsageName::toString);

    private final String value;

    private ExtendedKeyUsageName(String value) {
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
     * @return ExtendedKeyUsageName corresponding to the value
     */
    public static ExtendedKeyUsageName fromValue(String value) {
        if (value == null) {
            return null;
        }
        return VALUE_MAP.getOrDefault(value, UNKNOWN_TO_SDK_VERSION);
    }

    /**
     * Use this in place of {@link #values()} to return a {@link Set} of all values known to the SDK. This will return
     * all known enum values except {@link #UNKNOWN_TO_SDK_VERSION}.
     *
     * @return a {@link Set} of known {@link ExtendedKeyUsageName}s
     */
    public static Set<ExtendedKeyUsageName> knownValues() {
        Set<ExtendedKeyUsageName> knownValues = EnumSet.allOf(ExtendedKeyUsageName.class);
        knownValues.remove(UNKNOWN_TO_SDK_VERSION);
        return knownValues;
    }
}
