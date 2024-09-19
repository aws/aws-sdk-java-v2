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

import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.concat;

import java.util.Arrays;
import java.util.List;
import java.util.jar.JarInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.util.SdkUserAgent;
import software.amazon.awssdk.utils.IoUtils;

@SdkInternalApi
public final class UserAgentLangValues {

    private static final Logger log = LoggerFactory.getLogger(UserAgentLangValues.class);

    private UserAgentLangValues() {
    }

    public static List<String> getAdditionalJvmLanguages() {
        return Arrays.asList(scalaVersion(), kotlinVersion());
    }

    /**
     * Attempt to determine if Scala is on the classpath and if so what version is in use.
     * Does this by looking for a known Scala class (scala.util.Properties) and then calling
     * a static method on that class via reflection to determine the versionNumberString.
     *
     * @return Scala version if any, else empty string
     */
    public static String scalaVersion() {
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
    public static String kotlinVersion() {
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
}
