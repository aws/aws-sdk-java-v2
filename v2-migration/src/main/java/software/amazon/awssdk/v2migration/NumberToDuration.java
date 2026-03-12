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

package software.amazon.awssdk.v2migration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Convert the method parameter from numeric types to duration.
 */
@SdkInternalApi
public class NumberToDuration extends Recipe {
    @Option(displayName = "Method pattern",
        description = "A method pattern that is used to find matching method invocations.",
        example = "com.amazonaws.ClientConfiguration setRequestTimeout(int)")
    private final String methodPattern;

    @Option(displayName = "Time Unit",
        description = "The TimeUnit enum value to convert. Defaults to `MILLISECONDS`.",
        example = "MILLISECONDS",
        required = false)
    private final TimeUnit timeUnit;

    @JsonCreator
    public NumberToDuration(@JsonProperty("methodPattern") String methodPattern,
                            @JsonProperty("timeUnit") TimeUnit timeUnit) {
        this.methodPattern = methodPattern;
        this.timeUnit = timeUnit == null ? TimeUnit.MILLISECONDS : timeUnit;
    }

    @Override
    public String getDisplayName() {
        return "Convert the method parameter from numeric type to duration";
    }

    @Override
    public String getDescription() {
        return "Convert the method parameter from numeric types to duration.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor(methodPattern, timeUnit);
    }

    private static final class Visitor extends JavaIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;
        private final TimeUnit timeUnit;

        Visitor(String methodPattern, TimeUnit timeUnit) {
            this.methodMatcher = new MethodMatcher(methodPattern, false);
            this.timeUnit = timeUnit;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation methodInvocation = super.visitMethodInvocation(method, ctx);
            if (!methodMatcher.matches(methodInvocation)) {
                return methodInvocation;
            }

            String durationStr = durationCreationStr();

            JavaTemplate template = JavaTemplate
                .builder(durationStr + "(#{any()})")
                .contextSensitive()
                .imports("java.time.Duration")
                .build();

            List<Object> arguments = new ArrayList<>(methodInvocation.getArguments());

            methodInvocation = template.apply(
                updateCursor(methodInvocation),
                methodInvocation.getCoordinates().replaceArguments(),
                arguments.toArray(new Object[0])
            );
            maybeAddImport("java.time.Duration");
            return methodInvocation;
        }

        private String durationCreationStr() {
            String durationStr;
            switch (timeUnit) {
                case MILLISECONDS:
                    durationStr = "Duration.ofMillis";
                    break;
                case SECONDS:
                    durationStr = "Duration.ofSeconds";
                    break;
                case MINUTES:
                    durationStr = "Duration.ofMinutes";
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported time unit: " + timeUnit);
            }
            return durationStr;
        }
    }
}
