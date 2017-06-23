/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.util;

import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.internal.config.InternalConfig;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.JavaSystemSetting;

/**
 * Utility class for accessing AWS SDK versioning information.
 */
@ThreadSafe
public class VersionInfoUtils {
    /** The AWS SDK version info file with SDK versioning info. */
    static final String VERSION_INFO_FILE = "/software/amazon/awssdk/sdk/versionInfo.properties";
    /** Shared logger for any issues while loading version information. */
    private static final Log log = LogFactory.getLog(VersionInfoUtils.class);
    private static final String UNKNOWN = "unknown";
    /** SDK version info. */
    private static volatile String version;
    /** SDK platform info. */
    private static volatile String platform;
    /** User Agent info. */
    private static volatile String userAgent;

    /**
     * Returns the current version for the AWS SDK in which this class is
     * running. Version information is obtained from from the
     * versionInfo.properties file which the AWS Java SDK build process
     * generates.
     *
     * @return The current version for the AWS SDK, if known, otherwise
     *         returns a string indicating that the version information is
     *         not available.
     */
    public static String getVersion() {
        if (version == null) {
            synchronized (VersionInfoUtils.class) {
                if (version == null) {
                    initializeVersion();
                }
            }
        }
        return version;
    }

    /**
     * Returns the current platform for the AWS SDK in which this class is
     * running. Version information is obtained from from the
     * versionInfo.properties file which the AWS Java SDK build process
     * generates.
     *
     * @return The current platform for the AWS SDK, if known, otherwise
     *         returns a string indicating that the platform information is
     *         not available.
     */
    public static String getPlatform() {
        if (platform == null) {
            synchronized (VersionInfoUtils.class) {
                if (platform == null) {
                    initializeVersion();
                }
            }
        }
        return platform;
    }

    /**
     * @return Returns the User Agent string to be used when communicating with
     *     the AWS services.  The User Agent encapsulates SDK, Java, OS and
     *     region information.
     */
    public static String getUserAgent() {
        if (userAgent == null) {
            synchronized (VersionInfoUtils.class) {
                if (userAgent == null) {
                    initializeUserAgent();
                }
            }
        }
        return userAgent;
    }

    /**
     * Loads the versionInfo.properties file from the AWS Java SDK and
     * stores the information so that the file doesn't have to be read the
     * next time the data is needed.
     */
    private static void initializeVersion() {
        InputStream inputStream = ClassLoaderHelper.getResourceAsStream(
                VERSION_INFO_FILE, true, VersionInfoUtils.class);
        Properties versionInfoProperties = new Properties();
        try {
            if (inputStream == null) {
                throw new Exception(VERSION_INFO_FILE + " not found on classpath");
            }

            versionInfoProperties.load(inputStream);
            version = versionInfoProperties.getProperty("version");
            platform = versionInfoProperties.getProperty("platform");
        } catch (Exception e) {
            log.info("Unable to load version information for the running SDK: " + e.getMessage());
            version = "unknown-version";
            platform = "java";
        } finally {
            IoUtils.closeQuietly(inputStream, log);
        }
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

        String ua = InternalConfig.Factory.getInternalConfig()
                                          .getUserAgentTemplate();

        if (ua == null) {
            return "aws-sdk-java";
        }

        ua = ua
                .replace("{platform}", StringUtils.lowerCase(getPlatform()))
                .replace("{version}", getVersion())
                .replace("{os.name}", replaceSpaces(JavaSystemSetting.OS_NAME.getStringValue().orElse(null)))
                .replace("{os.version}", replaceSpaces(JavaSystemSetting.OS_VERSION.getStringValue().orElse(null)))
                .replace("{java.vm.name}", replaceSpaces(JavaSystemSetting.JAVA_VM_NAME.getStringValue().orElse(null)))
                .replace("{java.vm.version}", replaceSpaces(JavaSystemSetting.JAVA_VM_VERSION.getStringValue().orElse(null)))
                .replace("{java.version}", replaceSpaces(JavaSystemSetting.JAVA_VERSION.getStringValue().orElse(null)))
                .replace("{additional.languages}", getAdditionalJvmLanguages());

        Optional<String> language = JavaSystemSetting.USER_LANGUAGE.getStringValue();
        Optional<String> region = JavaSystemSetting.USER_REGION.getStringValue();

        String languageAndRegion = "";
        if (language.isPresent() && region.isPresent()) {
            languageAndRegion = " " + replaceSpaces(language.get()) + "_" + replaceSpaces(region.get());
        }
        ua = ua.replace("{language.and.region}", languageAndRegion);

        return ua;
    }

    /**
     * Replace any spaces in the input with underscores.
     *
     * @param input the input
     * @return the input with spaces replaced by underscores
     */
    private static String replaceSpaces(final String input) {
        return input == null ? UNKNOWN : input.replace(' ', '_');
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
