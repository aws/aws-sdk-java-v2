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

import static software.amazon.awssdk.utils.StringUtils.trim;

import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.StringUtils;

@SdkProtectedApi
public final class UserAgentConstant {

    //Known SDK metadata tags/names
    public static final String API_METADATA = "api";
    public static final String OS_METADATA = "os";
    public static final String LANG_METADATA = "lang";
    public static final String UA_METADATA = "ua";
    public static final String ENV_METADATA = "exec-env";
    public static final String JAVA_SDK_METADATA = "aws-sdk-java";
    public static final String FEATURE_METADATA = "ft";
    public static final String CONFIG_METADATA = "cfg";
    public static final String FRAMEWORK_METADATA = "lib";
    public static final String METADATA = "md";
    public static final String INTERNAL_METADATA_MARKER = "internal";
    public static final String APP_ID = "app";

    //Separators used in SDK user agent
    public static final String SLASH = "/";
    public static final String HASH = "#";
    public static final String SPACE = " ";

    //Java user agent tags/names
    public static final String IO = "io";
    public static final String HTTP = "http";
    public static final String RETRY_MODE = "retry-mode";
    public static final String AUTH_SOURCE = "auth-source";

    /** Disallowed characters in the user agent token: @see <a href="https://tools.ietf.org/html/rfc7230#section-3.2.6">RFC 7230</a> */
    private static final String UA_DENYLIST_REGEX = "[() ,/:;<=>?@\\[\\]{}\\\\]";
    private static final Pattern UA_DENYLIST_PATTERN = Pattern.compile(UA_DENYLIST_REGEX);
    private static final String UNKNOWN = "unknown";

    private UserAgentConstant() {
    }

    /**
     * According to specifications, the SDK user agent consists of metadata fields separated by RWS characters - in
     * this implementation, space.
     * Each field is represented by the name of the field, as specified and the contents of the field, separated
     * with a '/' (SLASH). Contents can be a single token, a specified value or a uaPair.
     */
    public static String field(String name, String value) {
        return concat(name, trim(value), SLASH);
    }

    /**
     * According to specifications, an SDK user agent pair is a name, value pair concatenated with a '#' (HASH).
     */
    public static String uaPair(String name, String value) {
        return concat(name, value, HASH);
    }

    /**
     * According to specifications, an SDK user agent pair is a name, value pair concatenated with a '#' (HASH).
     */
    public static String uaPairOrNull(String name, String value) {
        return value != null ? uaPair(name, value) : null;
    }

    /**
     * Add a metadata field to the string builder, followed by space. If 'value' can be empty, use
     * {@link #appendNonEmptyField(StringBuilder, String, String)} instead.
     */
    public static void appendFieldAndSpace(StringBuilder builder, String name, String value) {
        builder.append(name).append(SLASH).append(value);
        builder.append(SPACE);
    }

    /**
     * Add a metadata field to the string builder, preceded by space. If 'value' can be empty, use
     * {@link #appendNonEmptyField(StringBuilder, String, String)} instead.
     */
    public static void appendSpaceAndField(StringBuilder builder, String name, String value) {
        builder.append(SPACE);
        builder.append(name).append(SLASH).append(value);
    }

    /**
     * Add a metadata field to the string builder only if 'value' is non-empty.
     * Also see {@link #appendFieldAndSpace(StringBuilder, String, String)}
     */
    public static void appendNonEmptyField(StringBuilder builder, String name, String value) {
        if (!StringUtils.isEmpty(value)) {
            appendFieldAndSpace(builder, name, value);
        }
    }

    /**
     * Replace any spaces, parentheses in the input with underscores.
     */
    public static String sanitizeInput(String input) {
        return input == null ? UNKNOWN : UA_DENYLIST_PATTERN.matcher(input).replaceAll("_");
    }

    /**
     * Concatenates two values with the specified separator, if the second value is not null/empty, otherwise
     * returns the first value.
     */
    public static String concat(String prefix, String suffix, String separator) {
        return suffix != null && !suffix.isEmpty() ? prefix + separator + suffix : prefix;
    }
}
