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

package software.amazon.awssdk.codegen.parity;

import java.util.regex.Pattern;

/**
 * Allowlisted parity diff path with a free-text reason.
 *
 * <p>{@code path} is matched against {@link ParityDiff#path()} as a glob where
 * {@code *} matches one dot-separated segment and {@code **} matches any number
 * of segments.
 */
final class ParityAllowlistEntry {

    private final String path;
    private final String reason;
    private final Pattern compiled;

    ParityAllowlistEntry(String path, String reason) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("allowlist entry: 'path' is required");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "allowlist entry for path '" + path + "': 'reason' is required");
        }
        this.path = path;
        this.reason = reason;
        this.compiled = compileGlob(path);
    }

    boolean matches(String diffPath) {
        return compiled.matcher(diffPath).matches();
    }

    String path() {
        return path;
    }

    String reason() {
        return reason;
    }

    private static Pattern compileGlob(String glob) {
        StringBuilder regex = new StringBuilder("^");
        int i = 0;
        while (i < glob.length()) {
            char c = glob.charAt(i);
            if (c == '*' && i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                regex.append(".*");
                i += 2;
            } else if (c == '*') {
                regex.append("[^.]+");
                i++;
            } else if ("\\.+()[]{}^$|?".indexOf(c) >= 0) {
                regex.append('\\').append(c);
                i++;
            } else {
                regex.append(c);
                i++;
            }
        }
        regex.append("$");
        return Pattern.compile(regex.toString());
    }
}
