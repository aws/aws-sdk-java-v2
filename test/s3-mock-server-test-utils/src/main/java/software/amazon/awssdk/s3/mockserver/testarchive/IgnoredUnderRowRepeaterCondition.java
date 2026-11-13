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

package software.amazon.awssdk.s3.mockserver.testarchive;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import software.amazon.awssdk.s3.mockserver.helpers.Immutable;

public final class IgnoredUnderRowRepeaterCondition implements ExecutionCondition {

    private static final String INVOCATION_SEGMENT = "[test-template-invocation:";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<IgnoredUnderRowRepeater> ignored = context.getTestMethod()
                .flatMap(m -> AnnotationSupport.findAnnotation(m, IgnoredUnderRowRepeater.class));
        if (!ignored.isPresent() || !RowRepeater.isInstalled()) {
            return ConditionEvaluationResult.enabled("not under a RowRepeater");
        }
        return evaluate(ignored.get(), context.getDisplayName(), context.getUniqueId().contains(INVOCATION_SEGMENT));
    }

    static ConditionEvaluationResult evaluate(IgnoredUnderRowRepeater ignored, String displayName, boolean invocation) {
        String[] rows = ignored.rows();
        if (rows.length == 0) {
            return ConditionEvaluationResult.disabled("ignored under a RowRepeater: " + ignored.reason());
        }
        if (!invocation) {
            return ConditionEvaluationResult.enabled("the method's template; its rows are judged one by one");
        }
        List<String> entries = Arrays.stream(displayName.split(",")).map(String::trim).collect(Immutable.toList());
        if (Arrays.stream(rows).anyMatch(entries::contains)) {
            return ConditionEvaluationResult.disabled("ignored under a RowRepeater: " + ignored.reason());
        }
        return ConditionEvaluationResult.enabled("row [" + displayName + "] is not one of " + Arrays.toString(rows));
    }
}
