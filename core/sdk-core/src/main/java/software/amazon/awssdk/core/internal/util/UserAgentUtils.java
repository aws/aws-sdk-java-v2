/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Optional;
import java.util.jar.JarInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.JavaSystemSetting;

/**
 * Utility class for accessing AWS SDK versioning information.
 */
@ThreadSafe
@SdkInternalApi
public final class UserAgentUtils {

    private static final String UA_STRING = "aws-sdk-{platform}/{version} {os.name}/{os.version} {java.vm.name}/{java.vm"
                                            + ".version} Java/{java.version}{language.and.region}{additional.languages} "
                                            + "vendor/{java.vendor}";

    /** Disallowed characters in the user agent token: @see <a href="https://tools.ietf.org/html/rfc7230#section-3.2.6">RFC 7230</a> */
    private static final String UA_BLACKLIST_REGEX = "[() ,/:;<=>?@\\[\\]{}\\\\]";

    /** Shared logger for any issues while loading version information. */
    private static final Logger log = LoggerFactory.getLogger(UserAgentUtils.class);
    private static final String UNKNOWN = "unknown";
    /** User Agent info. */
    private static volatile String userAgent;

    private UserAgentUtils() {
    }

    /**
     * @return Returns the User Agent string to be used when communicating with
     *     the AWS services.  The User Agent encapsulates SDK, Java, OS and
     *     region information.
     */
    public static String getUserAgent() {
        if (userAgent == null) {
            synchronized (UserAgentUtils.class) {
                if (userAgent == null) {
                    initializeUserAgent();
                }
            }
        }
        return userAgent;
    }

    /**
     * Initializes the user agent string by loading a template from
     * {@code InternalConfig} and filling in the detected version/platform
     * info.
     */
    private static void initializeUserAgent() {
        userAgent = userAgent();
    }

    static String userAgent() {

        String ua = UA_STRING;

        ua = ua
                .replace("{platform}", "java")
                .replace("{version}", VersionInfo.SDK_VERSION)
                .replace("{os.name}", sanitizeInput(JavaSystemSetting.OS_NAME.getStringValue().orElse(null)))
                .replace("{os.version}", sanitizeInput(JavaSystemSetting.OS_VERSION.getStringValue().orElse(null)))
                .replace("{java.vm.name}", sanitizeInput(JavaSystemSetting.JAVA_VM_NAME.getStringValue().orElse(null)))
                .replace("{java.vm.version}", sanitizeInput(JavaSystemSetting.JAVA_VM_VERSION.getStringValue().orElse(null)))
                .replace("{java.version}", sanitizeInput(JavaSystemSetting.JAVA_VERSION.getStringValue().orElse(null)))
                .replace("{java.vendor}", sanitizeInput(JavaSystemSetting.JAVA_VENDOR.getStringValue().orElse(null)))
                .replace("{additional.languages}", getAdditionalJvmLanguages());

        Optional<String> language = JavaSystemSetting.USER_LANGUAGE.getStringValue();
        Optional<String> region = JavaSystemSetting.USER_REGION.getStringValue();

        String languageAndRegion = "";
        if (language.isPresent() && region.isPresent()) {
            languageAndRegion = " (" + sanitizeInput(language.get()) + "_" + sanitizeInput(region.get()) + ")";
        }
        ua = ua.replace("{language.and.region}", languageAndRegion);

        return ua;
    }

    /**
     * Replace any spaces, parentheses in the input with underscores.
     *
     * @param input the input
     * @return the input with spaces replaced by underscores
     */
    private static String sanitizeInput(String input) {
        return input == null ? UNKNOWN : input.replaceAll(UA_BLACKLIST_REGEX, "_");
    }

    private static String getAdditionalJvmLanguages() {
        return concat(concat("", scalaVersion(), " "), kotlinVersion(), " ");
    }

    /**
     * Attempt to determine if Scala is on the classpath and if so what version is in use.
     * Does this by looking for a known Scala class (scala.util.Properties) and then calling
     * a static method on that class via reflection to determine the versionNumberString.
     *
     * @return Scala version if any, else empty string
     */
    private static String scalaVersion() {
        String scalaVersion = "";
        try {
            Class<?> scalaProperties = Class.forName("scala.util.Properties");
            scalaVersion = "scala";
            String version = (String) scalaProperties.getMethod("versionNumberString").invoke(null);
            scalaVersion = concat(scalaVersion, version, "/");
        } catch (ClassNotFoundException e) {
            //Ignore
        } catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace("Exception attempting to get Scala version.", e);
            }
        }
        return scalaVersion;
    }

    /**
     * Attempt to determine if Kotlin is on the classpath and if so what version is in use.
     * Does this by looking for a known Kotlin class (kotlin.Unit) and then loading the Manifest
     * from that class' JAR to determine the Kotlin version.
     *
     * @return Kotlin version if any, else empty string
     */
    private static String kotlinVersion() {
        String kotlinVersion = "";
        JarInputStream kotlinJar = null;
        try {
            Class<?> kotlinUnit = Class.forName("kotlin.Unit");
            kotlinVersion = "kotlin";
            kotlinJar = new JarInputStream(kotlinUnit.getProtectionDomain().getCodeSource().getLocation().openStream());
            String version = kotlinJar.getManifest().getMainAttributes().getValue("Implementation-Version");
            kotlinVersion = concat(kotlinVersion, version, "/");
        } catch (ClassNotFoundException e) {
            //Ignore
        } catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace("Exception attempting to get Kotlin version.", e);
            }
        } finally {
            IoUtils.closeQuietly(kotlinJar, log);
        }
        return kotlinVersion;
    }

    private static String concat(String prefix, String suffix, String separator) {
        return suffix != null && !suffix.isEmpty() ? prefix + separator + suffix : prefix;
    }
}
