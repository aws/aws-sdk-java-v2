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

package software.amazon.awssdk.core.internal.useragent;

import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.AUTH_SOURCE;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.CONFIG_METADATA;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.ENV_METADATA;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.HTTP;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.INTERNAL_METADATA_MARKER;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.IO;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.JAVA_SDK_METADATA;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.LANG_METADATA;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.METADATA;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.OS_METADATA;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.RETRY_MODE;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.UA_METADATA;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.appendField;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.appendNonEmptyField;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.uaPair;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.uaPairOrNull;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.util.SystemUserAgent;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Represents an AWS SDK user agent and stores a copy of client specific values as well as request level values.
 * Use {@link #buildSdkUserAgentString(SystemUserAgent, SdkUserAgentProperties)} to return a string representation
 * to use as the header value.
 * <p>
 * Note that {@link #buildSdkUserAgentString(SystemUserAgent, SdkUserAgentProperties)} also includes system properties
 * from {@link SystemUserAgent}.
 */
@ThreadSafe
@SdkProtectedApi
public final class SdkUserAgent {

    private SdkUserAgent() {
    }

    public static String buildSdkUserAgentString(SystemUserAgent systemValues, SdkUserAgentProperties userAgentProperties) {
        StringBuilder uaString = new StringBuilder(255);

        appendNonEmptyField(uaString, JAVA_SDK_METADATA, systemValues.sdkVersion());
        appendAdditionalSdkMetadata(uaString, userAgentProperties);

        if (userAgentProperties.getAttribute(INTERNAL_METADATA_MARKER) != null) {
            appendField(uaString, METADATA, INTERNAL_METADATA_MARKER);
        }

        appendNonEmptyField(uaString, UA_METADATA, "2.0");
        appendNonEmptyField(uaString, OS_METADATA, systemValues.osMetadata());
        appendNonEmptyField(uaString, LANG_METADATA, systemValues.langMetadata());
        appendAdditionalJvmMetadata(uaString, systemValues);

        String envMetadata = systemValues.envMetadata();
        if (!isEmptyOrUnknown(envMetadata)) {
            appendField(uaString, ENV_METADATA, envMetadata);
        }

        String retryMode = userAgentProperties.getAttribute(RETRY_MODE);
        if (!StringUtils.isEmpty(retryMode)) {
            appendField(uaString, CONFIG_METADATA, uaPair(RETRY_MODE, retryMode));
        }

        String authSource = userAgentProperties.getAttribute(AUTH_SOURCE);
        if (!StringUtils.isEmpty(authSource)) {
            appendField(uaString, CONFIG_METADATA, uaPair(AUTH_SOURCE, authSource));
        }

        //TODO (user-agent) add appId
        //appendField(uaString, APP_ID ("app"), requestUserAgent.getAttribute(APP_ID));

        //TODO (user-agent) add business metrics
        //appendField(uaString, BUSINESS_METRICS ("m"),
        //createBusinessMetricsString(requestUserAgent.getAttribute(BUSINESS_METRICS_VALUES)));

        removeFinalWhitespace(uaString);
        return uaString.toString();
    }

    public static String buildSystemUserAgentString(SystemUserAgent systemValues) {
        StringBuilder uaString = new StringBuilder(128);

        appendNonEmptyField(uaString, JAVA_SDK_METADATA, systemValues.sdkVersion());
        appendNonEmptyField(uaString, OS_METADATA, systemValues.osMetadata());
        appendNonEmptyField(uaString, LANG_METADATA, systemValues.langMetadata());
        appendAdditionalJvmMetadata(uaString, systemValues);

        String envMetadata = systemValues.envMetadata();
        if (!isEmptyOrUnknown(envMetadata)) {
            appendField(uaString, ENV_METADATA, systemValues.envMetadata());
        }

        removeFinalWhitespace(uaString);
        return uaString.toString();
    }

    private static void removeFinalWhitespace(StringBuilder builder) {
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) == ' ') {
            builder.deleteCharAt(builder.length() - 1);
        }
    }

    private static boolean isEmptyOrUnknown(String envMetadata) {
        return StringUtils.isEmpty(envMetadata) || envMetadata.equalsIgnoreCase("unknown");
    }

    private static void appendAdditionalSdkMetadata(StringBuilder builder, SdkUserAgentProperties userAgentProperties) {
        appendNonEmptyField(builder, METADATA, uaPairOrNull(IO, userAgentProperties.getAttribute(IO)));
        appendNonEmptyField(builder, METADATA, uaPairOrNull(HTTP, userAgentProperties.getAttribute(HTTP)));
    }

    private static void appendAdditionalJvmMetadata(StringBuilder builder, SystemUserAgent systemProperties) {
        appendNonEmptyField(builder, METADATA, systemProperties.vmMetadata());
        appendNonEmptyField(builder, METADATA, systemProperties.vendorMetadata());
        systemProperties.languageTagMetadata().ifPresent(value -> appendField(builder, METADATA, value));
        for (String lang : systemProperties.additionalJvmLanguages()) {
            appendNonEmptyField(builder, METADATA, lang);
        }
    }
}
