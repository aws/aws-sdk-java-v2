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

import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.APP_ID;
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
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.appendFieldAndSpace;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.appendNonEmptyField;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.uaPair;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.uaPairOrNull;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.util.SystemUserAgent;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Responsible for building user agent strings for different use cases.
 */
@ThreadSafe
@SdkProtectedApi
public final class SdkUserAgentBuilder {

    private static final Logger log = Logger.loggerFor(SdkUserAgentBuilder.class);

    private SdkUserAgentBuilder() {
    }

    /**
     * Constructs a string representation of an SDK client user agent string, based on system and client data.
     * Note that request level values must be added separately.
     */
    public static String buildClientUserAgentString(SystemUserAgent systemValues,
                                                    SdkClientUserAgentProperties userAgentProperties) {
        StringBuilder uaString = new StringBuilder(255);

        appendNonEmptyField(uaString, JAVA_SDK_METADATA, systemValues.sdkVersion());
        appendAdditionalSdkMetadata(uaString, userAgentProperties);

        String internalMarkerValue = userAgentProperties.getProperty(INTERNAL_METADATA_MARKER);
        if (!StringUtils.isEmpty(internalMarkerValue)) {
            appendFieldAndSpace(uaString, METADATA, INTERNAL_METADATA_MARKER);
        }

        appendNonEmptyField(uaString, UA_METADATA, "2.0");
        appendNonEmptyField(uaString, OS_METADATA, systemValues.osMetadata());
        appendNonEmptyField(uaString, LANG_METADATA, systemValues.langMetadata());
        appendAdditionalJvmMetadata(uaString, systemValues);

        String envMetadata = systemValues.envMetadata();
        if (!isEmptyOrUnknown(envMetadata)) {
            appendFieldAndSpace(uaString, ENV_METADATA, envMetadata);
        }

        String retryMode = userAgentProperties.getProperty(RETRY_MODE);
        if (!StringUtils.isEmpty(retryMode)) {
            appendFieldAndSpace(uaString, CONFIG_METADATA, uaPair(RETRY_MODE, retryMode));
        }

        String appId = userAgentProperties.getProperty(APP_ID);
        if (!StringUtils.isEmpty(appId)) {
            checkLengthAndWarn(appId);
            appendFieldAndSpace(uaString, APP_ID, appId);
        }

        removeFinalWhitespace(uaString);
        return uaString.toString();
    }

    /**
     * Constructs a string representation of system user agent values only, that can be used for any backend calls.
     */
    public static String buildSystemUserAgentString(SystemUserAgent systemValues) {
        StringBuilder uaString = new StringBuilder(128);

        appendNonEmptyField(uaString, JAVA_SDK_METADATA, systemValues.sdkVersion());
        appendNonEmptyField(uaString, OS_METADATA, systemValues.osMetadata());
        appendNonEmptyField(uaString, LANG_METADATA, systemValues.langMetadata());
        appendAdditionalJvmMetadata(uaString, systemValues);

        String envMetadata = systemValues.envMetadata();
        if (!isEmptyOrUnknown(envMetadata)) {
            appendFieldAndSpace(uaString, ENV_METADATA, systemValues.envMetadata());
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

    private static void appendAdditionalSdkMetadata(StringBuilder builder, SdkClientUserAgentProperties userAgentProperties) {
        appendNonEmptyField(builder, METADATA, uaPairOrNull(IO, userAgentProperties.getProperty(IO)));
        appendNonEmptyField(builder, METADATA, uaPairOrNull(HTTP, userAgentProperties.getProperty(HTTP)));
    }

    private static void appendAdditionalJvmMetadata(StringBuilder builder, SystemUserAgent systemProperties) {
        appendNonEmptyField(builder, METADATA, systemProperties.vmMetadata());
        appendNonEmptyField(builder, METADATA, systemProperties.vendorMetadata());
        systemProperties.languageTagMetadata().ifPresent(value -> appendFieldAndSpace(builder, METADATA, value));
        for (String lang : systemProperties.additionalJvmLanguages()) {
            appendNonEmptyField(builder, METADATA, lang);
        }
    }

    private static void checkLengthAndWarn(String appId) {
        if (appId.length() > 50) {
            log.warn(() -> String.format("The configured appId '%s' is longer than the recommended maximum length of 50. "
                                         + "This could result in not being able to transmit and log the whole user agent string, "
                                         + "including the complete value of this string.", appId));
        }
    }
}
