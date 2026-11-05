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

package software.amazon.awssdk.mapper.dynamodb;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Detects Joda-Time, an optional dependency of the mapper. It is needed only for {@code org.joda.time.DateTime}
 * attributes and for {@link DynamoDBTypeConvertedTimestamp}.
 *
 * <p>This class must not reference any Joda-Time type, so that it can be loaded without Joda-Time. Code that uses
 * Joda-Time lives in separate classes that are loaded only after {@link #isAvailable()} returns {@code true}.
 */
@SdkInternalApi
final class JodaTimeSupport {

    private static final String DATE_TIME_CLASS = "org.joda.time.DateTime";

    /**
     * Checked with the mapper's own class loader, because that is the loader that resolves the mapper's references to
     * Joda-Time. A copy visible only to the thread context class loader would not help.
     */
    private static final boolean AVAILABLE = isLoadable(DATE_TIME_CLASS);

    private JodaTimeSupport() {
    }

    /**
     * Returns whether Joda-Time is on the mapper's classpath.
     */
    static boolean isAvailable() {
        return AVAILABLE;
    }

    /**
     * Returns whether {@code type} is {@code org.joda.time.DateTime}, without loading Joda-Time. This can be true
     * even when {@link #isAvailable()} is false, if the application's class loader has Joda-Time but the mapper's
     * does not.
     */
    static boolean isDateTime(Class<?> type) {
        return type != null && DATE_TIME_CLASS.equals(type.getName());
    }

    /**
     * Returns the exception to throw when a feature that needs Joda-Time is used without it.
     *
     * @param feature the feature that needs Joda-Time, for example {@code "@DynamoDBTypeConvertedTimestamp"}
     */
    static DynamoDBMappingException missing(String feature) {
        return new DynamoDBMappingException(
            feature + " requires Joda-Time, which is an optional dependency of the mapper. "
            + "Add joda-time:joda-time to the classpath, where the mapper's class loader can load it.");
    }

    private static boolean isLoadable(String className) {
        try {
            Class.forName(className, false, JodaTimeSupport.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
