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

package software.amazon.awssdk.benchmark.util;

/**
 * Utility class for accessing system settings.
 */
public final class SystemSetting {
    
    private static final String JAVA_VERSION_PROPERTY = "java.version";
    private static final String PUBLISH_TO_CLOUDWATCH_ENV = "PUBLISH_TO_CLOUDWATCH";
    
    private SystemSetting() {
        // Prevent instantiation
    }
    
    /**
     * Get the Java version.
     * 
     * @return The Java version string
     */
    public static String getJavaVersion() {
        // CHECKSTYLE:OFF
        return System.getProperty(JAVA_VERSION_PROPERTY);
        // CHECKSTYLE:ON
    }
    
    /**
     * Check if publishing to CloudWatch is enabled.
     * 
     * @return true if publishing to CloudWatch is enabled
     */
    public static boolean isPublishToCloudWatchEnabled() {
        // CHECKSTYLE:OFF
        String value = System.getenv(PUBLISH_TO_CLOUDWATCH_ENV);
        // CHECKSTYLE:ON
        return "true".equalsIgnoreCase(value);
    }
}
