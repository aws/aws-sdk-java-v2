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

package software.amazon.awssdk.utils;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Escapes control characters and Unicode line separators in values written to logs.
 */
@SdkProtectedApi
public final class LogSanitizer {

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private LogSanitizer() {
    }

    /**
     * Returns the given string with control characters and Unicode line separators replaced by visible escapes so it can
     * be logged on one line.
     *
     * <p>Carriage return, line feed and tab become a backslash followed by {@code r}, {@code n} or {@code t}. Other ISO
     * control characters and U+2028 and U+2029 become a backslash, {@code u} and four lowercase hex digits. Backslashes
     * and all other characters are preserved.
     *
     * <p>Returns the original string instance when no escaping is needed, and {@code "null"} when the input is null.
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

    /**
     * Returns a throwable with control characters and Unicode line separators escaped for safe logging.
     *
     * <p>Returns the original throwable when it is already safe to log. This check covers the throwable and every cause and
     * suppressed exception, including their descriptions, messages and stack frames.
     *
     * <p>Otherwise, this returns an internal stand-in copy. The copy preserves causes, suppressed exceptions, cycles,
     * shared references and stack-frame order without retaining the original throwable objects. Its description includes
     * the original exception type, but its concrete Java class differs from the original throwable.
     *
     * <p>If reading the throwable graph throws, returns a stand-in that identifies the original exception type and reports
     * that its details are unavailable.
     */
    public static Throwable sanitize(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        String className = throwable.getClass().getName();
        try {
            if (!needsEscape(throwable, Collections.newSetFromMap(new IdentityHashMap<>()))) {
                return throwable;
            }
            return sanitize(throwable, new IdentityHashMap<Throwable, Throwable>());
        } catch (RuntimeException e) {
            return new EscapedThrowable(escapeControlCharacters(className) + ": <exception details unavailable>");
        }
    }

    private static boolean needsEscape(Throwable throwable, Set<Throwable> visited) {
        if (throwable == null || !visited.add(throwable)) {
            return false;
        }
        if (needsEscape(throwable.toString())
            || needsEscape(throwable.getMessage())
            || needsEscape(throwable.getLocalizedMessage())) {
            return true;
        }
        for (StackTraceElement element : throwable.getStackTrace()) {
            if (needsEscape(element)) {
                return true;
            }
        }
        if (needsEscape(throwable.getCause(), visited)) {
            return true;
        }
        for (Throwable suppressed : throwable.getSuppressed()) {
            if (needsEscape(suppressed, visited)) {
                return true;
            }
        }
        return false;
    }

    private static Throwable sanitize(Throwable throwable, Map<Throwable, Throwable> sanitized) {
        if (throwable == null) {
            return null;
        }
        Throwable existing = sanitized.get(throwable);
        if (existing != null) {
            return existing;
        }

        EscapedThrowable result = new EscapedThrowable(describe(throwable));
        sanitized.put(throwable, result);
        result.setStackTrace(sanitize(throwable.getStackTrace()));

        Throwable cause = throwable.getCause();
        if (cause != null) {
            result.initCause(sanitize(cause, sanitized));
        }
        for (Throwable suppressed : throwable.getSuppressed()) {
            result.addSuppressed(sanitize(suppressed, sanitized));
        }
        return result;
    }

    private static String describe(Throwable throwable) {
        String className = throwable.getClass().getName();
        String description = String.valueOf(throwable.toString());
        if (!description.startsWith(className)) {
            description = className + ": " + description;
        }
        return escapeControlCharacters(description);
    }

    private static StackTraceElement[] sanitize(StackTraceElement[] stackTrace) {
        StackTraceElement[] result = new StackTraceElement[stackTrace.length];
        for (int i = 0; i < stackTrace.length; i++) {
            result[i] = sanitize(stackTrace[i]);
        }
        return result;
    }

    private static StackTraceElement sanitize(StackTraceElement element) {
        if (!needsEscape(element)) {
            return element;
        }
        return new StackTraceElement(escapeControlCharacters(element.getClassName()),
                                     escapeControlCharacters(element.getMethodName()),
                                     escapeNullable(element.getFileName()),
                                     element.getLineNumber());
    }

    /**
     * Uses the rendered frame to cover Java 9 class-loader and module fields without depending on APIs unavailable on
     * Java 8.
     */
    private static boolean needsEscape(StackTraceElement element) {
        return needsEscape(element.toString());
    }

    private static boolean needsEscape(String s) {
        return s != null && firstCharToEscape(s) >= 0;
    }

    private static String escapeNullable(String value) {
        return value == null ? null : escapeControlCharacters(value);
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

    private static final class EscapedThrowable extends Throwable {
        private static final long serialVersionUID = 1L;

        private final String description;

        private EscapedThrowable(String description) {
            super(description);
            this.description = description;
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
