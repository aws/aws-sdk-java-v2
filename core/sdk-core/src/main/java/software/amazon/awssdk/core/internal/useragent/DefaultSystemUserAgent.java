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

import static software.amazon.awssdk.core.internal.useragent.SdkUserAgentBuilder.buildSystemUserAgentString;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.sanitizeInput;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.uaPair;
import static software.amazon.awssdk.core.internal.useragent.UserAgentLangValues.getAdditionalJvmLanguages;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.util.SystemUserAgent;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.utils.JavaSystemSetting;
import software.amazon.awssdk.utils.SystemSetting;

/**
 * Common system level user agent properties that can either be accessed as a string or as individual values.
 * The former is useful when making generic calls, for instance to local endpoints when resolving identity, while
 * the latter is when incorporating this information into a user agent header in an SDK request.
 */
@ThreadSafe
@SdkProtectedApi
public final class DefaultSystemUserAgent implements SystemUserAgent {

    private static volatile DefaultSystemUserAgent instance;

    private final String sdkVersion;
    private final String osMetadata;
    private final String langMetadata;
    private final String envMetadata;
    private final String vmMetadata;
    private final String vendorMetadata;
    private final Optional<String> languageTagMetadata;
    private final List<String> additionalJvmLanguages;
    private final String systemUserAgent;

    private DefaultSystemUserAgent() {
        sdkVersion = VersionInfo.SDK_VERSION;
        osMetadata = uaPair(systemSetting(JavaSystemSetting.OS_NAME), systemSetting(JavaSystemSetting.OS_VERSION));
        langMetadata = uaPair("java", systemSetting(JavaSystemSetting.JAVA_VERSION));
        envMetadata = systemSetting(SdkSystemSetting.AWS_EXECUTION_ENV);
        vmMetadata = uaPair(systemSetting(JavaSystemSetting.JAVA_VM_NAME), systemSetting(JavaSystemSetting.JAVA_VM_VERSION));
        vendorMetadata = uaPair("vendor", systemSetting(JavaSystemSetting.JAVA_VENDOR));
        languageTagMetadata = getLanguageTagMetadata();
        additionalJvmLanguages = getAdditionalJvmLanguages();
        systemUserAgent = getUserAgent();
    }

    public static DefaultSystemUserAgent getOrCreate() {
        if (instance == null) {
            synchronized (DefaultSystemUserAgent.class) {
                if (instance == null) {
                    instance = new DefaultSystemUserAgent();
                }
            }
        }

        return instance;
    }

    /**
     * A generic user agent string to be used when communicating with backend services.
     * This string contains Java, OS and region information but does not contain client and request
     * specific values.
     */
    @Override
    public String userAgentString() {
        return systemUserAgent;
    }

    @Override
    public String sdkVersion() {
        return sdkVersion;
    }

    @Override
    public String osMetadata() {
        return osMetadata;
    }

    @Override
    public String langMetadata() {
        return langMetadata;
    }

    @Override
    public String envMetadata() {
        return envMetadata;
    }

    @Override
    public String vmMetadata() {
        return vmMetadata;
    }

    @Override
    public String vendorMetadata() {
        return vendorMetadata;
    }

    @Override
    public Optional<String> languageTagMetadata() {
        return languageTagMetadata;
    }

    @Override
    public List<String> additionalJvmLanguages() {
        return Collections.unmodifiableList(additionalJvmLanguages);
    }

    private String getUserAgent() {
        return buildSystemUserAgentString(this);
    }

    private String systemSetting(SystemSetting systemSetting) {
        return sanitizeInput(systemSetting.getStringValue().orElse(null));
    }

    private Optional<String> getLanguageTagMetadata() {
        Optional<String> language = JavaSystemSetting.USER_LANGUAGE.getStringValue();
        Optional<String> country = JavaSystemSetting.USER_COUNTRY.getStringValue();
        String value = null;
        if (language.isPresent() && country.isPresent()) {
            value = sanitizeInput(language.get()) + "_" + sanitizeInput(country.get());
        }
        return Optional.ofNullable(value);
    }
}
