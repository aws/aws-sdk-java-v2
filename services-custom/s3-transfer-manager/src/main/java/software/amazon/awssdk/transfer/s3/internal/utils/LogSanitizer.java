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

package software.amazon.awssdk.transfer.s3.internal.utils;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Makes log messages that hold file names or object keys safe to write as a single log line.
 */
@SdkInternalApi
public final class LogSanitizer {

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private LogSanitizer() {
    }

    /**
     * Returns {@code s} with every control or line-separator character replaced by a visible escape, so it logs as one
     * line. Carriage return, line feed and tab become a backslash followed by {@code r}, {@code n} or {@code t}. The other
     * ISO control characters and U+2028 and U+2029 become a backslash, {@code u} and four lowercase hex digits. Every other
     * character, including backslash, is kept. Returns {@code s} itself when nothing needs escaping, and {@code "null"} when
     * {@code s} is null.
     */
    public static String escapeControlCharacters(String s) {
        if (s == null) {
            return "null";
        }
        int first = firstCharToEscape(s);
        if (first < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        sb.append(s, 0, first);
        for (int i = first; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (needsEscape(c)) {
                sb.append("\\u");
                for (int shift = 12; shift >= 0; shift -= 4) {
                    sb.append(HEX_DIGITS[(c >> shift) & 0xF]);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static int firstCharToEscape(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (needsEscape(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean needsEscape(char c) {
        return Character.isISOControl(c) || c == '\u2028' || c == '\u2029';
    }
}
