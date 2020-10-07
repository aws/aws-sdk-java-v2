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

package software.amazon.awssdk.utils.internal;

import static java.util.stream.Collectors.joining;

import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Internal class used by the code generator and release scripts to produce sanitized names.
 *
 * In the future, we should consider adding a build-utils module for tools used at build time
 * by multiple modules so that we don't have these at runtime when they aren't needed.
 */
@SdkInternalApi
public final class CodegenNamingUtils {

    private CodegenNamingUtils() {
    }

    public static String[] splitOnWordBoundaries(String toSplit) {
        String result = toSplit;

        // All non-alphanumeric characters are spaces
        result = result.replaceAll("[^A-Za-z0-9]+", " "); // acm-success -> "acm success"

        // If a number has a standalone v in front of it, separate it out (version).
        result = result.replaceAll("([^a-z]{2,})v([0-9]+)", "$1 v$2 ") // TESTv4 -> "TEST v4 "
                       .replaceAll("([^A-Z]{2,})V([0-9]+)", "$1 V$2 "); // TestV4 -> "Test V4 "

        // Add a space between camelCased words
        result = String.join(" ", result.split("(?<=[a-z])(?=[A-Z]([a-zA-Z]|[0-9]))")); // AcmSuccess -> // "Acm Success"

        // Add a space after acronyms
        result = result.replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2"); // ACMSuccess -> "ACM Success"

        // Add space after a number in the middle of a word
        result = result.replaceAll("([0-9])([a-zA-Z])", "$1 $2"); // s3ec2 -> "s3 ec2"

        // Remove extra spaces - multiple consecutive ones or those and the beginning/end of words
        result = result.replaceAll(" +", " ") // "Foo  Bar" -> "Foo Bar"
                       .trim(); // " Foo " -> Foo

        return result.split(" ");
    }

    public static String pascalCase(String word) {
        return Stream.of(splitOnWordBoundaries(word)).map(StringUtils::lowerCase).map(StringUtils::capitalize).collect(joining());
    }

    public static String pascalCase(String... words) {
        return Stream.of(words).map(StringUtils::lowerCase).map(StringUtils::capitalize).collect(joining());
    }

    public static String lowercaseFirstChar(String word) {
        char[] chars = word.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return String.valueOf(chars);
    }
}
