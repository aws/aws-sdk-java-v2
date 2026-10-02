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

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils;

@SdkInternalApi
public class DateToInstant extends Recipe {
    private static final Pattern DATE_PATTERN = Pattern.compile(Date.class.getCanonicalName());

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Convert Date to Instant";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Convert Date to Instant by calling Date#toInstant";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DateToInstantVisitor();
    }

    private static final class DateToInstantVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation originalMethod, ExecutionContext ctx) {
            J.MethodInvocation method = super.visitMethodInvocation(originalMethod, ctx);

            if (!isV2ModelSetterWithDateParam(method)) {
                return method;
            }

            JavaTemplate template = JavaTemplate.builder("#{any()}.toInstant()").contextSensitive().build();

            return template.apply(updateCursor(method), method.getCoordinates().replaceArguments(), method.getArguments().get(0));
        }

        private static boolean isV2ModelSetterWithDateParam(J.MethodInvocation method) {
            JavaType.Method mt = method.getMethodType();

            if (mt == null) {
                return false;
            }

            JavaType.FullyQualified declaringType = mt.getDeclaringType();
            List<JavaType> parameterTypes = mt.getParameterTypes();
            if (parameterTypes.size() != 1) {
                return false;
            }

            JavaType javaType = parameterTypes.get(0);
            if (javaType == null) {
                return false;
            }

            boolean isDateParam = javaType.isAssignableFrom(DATE_PATTERN);
            boolean isV2Model = SdkTypeUtils.isV2ModelBuilder(declaringType) || SdkTypeUtils.isV2ModelClass(declaringType);
            return isDateParam && isV2Model;
        }
    }
}
