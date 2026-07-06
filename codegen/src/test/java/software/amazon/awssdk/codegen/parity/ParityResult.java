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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Result of comparing two {@code IntermediateModel}s. Tests assert against
 * {@link #unexpectedDiffs()} (diffs not covered by an allowlist entry).
 */
final class ParityResult {

    private final String service;
    private final List<ParityDiff> allDiffs;
    private final List<ParityDiff> unexpectedDiffs;

    ParityResult(String service, List<ParityDiff> allDiffs, List<ParityDiff> unexpectedDiffs) {
        this.service = service;
        this.allDiffs = Collections.unmodifiableList(allDiffs);
        this.unexpectedDiffs = Collections.unmodifiableList(unexpectedDiffs);
    }

    String service() {
        return service;
    }

    List<ParityDiff> allDiffs() {
        return allDiffs;
    }

    List<ParityDiff> unexpectedDiffs() {
        return unexpectedDiffs;
    }

    String summary() {
        int allowed = allDiffs.size() - unexpectedDiffs.size();
        StringBuilder sb = new StringBuilder();
        sb.append("Parity result for ").append(service).append(":\n");
        sb.append("  total diffs: ").append(allDiffs.size())
          .append(" (").append(allowed).append(" allowlisted, ")
          .append(unexpectedDiffs.size()).append(" unexpected)\n");
        if (!unexpectedDiffs.isEmpty()) {
            sb.append("Unexpected diffs:\n");
            sb.append(unexpectedDiffs.stream()
                                     .map(d -> "  " + d.toString())
                                     .collect(Collectors.joining("\n")));
        }
        return sb.toString();
    }
}
