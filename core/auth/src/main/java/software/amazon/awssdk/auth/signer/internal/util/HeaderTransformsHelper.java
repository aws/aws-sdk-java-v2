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

package software.amazon.awssdk.auth.signer.internal.util;

import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Helper class for transforming headers required during signing of headers.
 */
@SdkInternalApi
public final class HeaderTransformsHelper {

    private static final List<String> LIST_OF_HEADERS_TO_IGNORE_IN_LOWER_CASE =
            Arrays.asList("connection", "x-amzn-trace-id", "user-agent", "expect");

    private HeaderTransformsHelper() {
    }

    public static Map<String, List<String>> canonicalizeSigningHeaders(Map<String, List<String>> headers) {
        Map<String, List<String>> result = new TreeMap<>();

        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            String lowerCaseHeader = lowerCase(header.getKey());
            if (LIST_OF_HEADERS_TO_IGNORE_IN_LOWER_CASE.contains(lowerCaseHeader)) {
                continue;
            }

            result.computeIfAbsent(lowerCaseHeader, x -> new ArrayList<>()).addAll(header.getValue());
        }
        return result;
    }

    /**
     * "The Trimall function removes excess white space before and after values,
     * and converts sequential spaces to a single space."
     * <p>
     * https://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html
     * <p>
     * The collapse-whitespace logic is equivalent to:
     * <pre>
     *     value.replaceAll("\\s+", " ")
     * </pre>
     * but does not create a Pattern object that needs to compile the match
     * string; it also prevents us from having to make a Matcher object as well.
     */
    public static String trimAll(String value) {
        boolean previousIsWhiteSpace = false;
        StringBuilder sb = new StringBuilder(value.length());

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (isWhiteSpace(ch)) {
                if (previousIsWhiteSpace) {
                    continue;
                }
                sb.append(' ');
                previousIsWhiteSpace = true;
            } else {
                sb.append(ch);
                previousIsWhiteSpace = false;
            }
        }

        return sb.toString().trim();
    }

    private static List<String> trimAll(List<String> values) {
        return values.stream().map(HeaderTransformsHelper::trimAll).collect(Collectors.toList());
    }

    /**
     * Tests a char to see if is it whitespace.
     * This method considers the same characters to be white
     * space as the Pattern class does when matching \s
     *
     * @param ch the character to be tested
     * @return true if the character is white  space, false otherwise.
     */
    private static boolean isWhiteSpace(final char ch) {
        return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\u000b' || ch == '\r' || ch == '\f';
    }


    public static String getCanonicalizedHeaderString(Map<String, List<String>> canonicalizedHeaders) {
        StringBuilder buffer = new StringBuilder();

        canonicalizedHeaders.forEach((headerName, headerValues) -> {
            buffer.append(headerName);
            buffer.append(":");
            buffer.append(String.join(",", trimAll(headerValues)));
            buffer.append("\n");
        });

        return buffer.toString();
    }

}
