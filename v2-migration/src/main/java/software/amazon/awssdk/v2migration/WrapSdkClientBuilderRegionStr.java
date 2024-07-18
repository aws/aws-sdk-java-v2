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

import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.isV2ClientClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.regions.Region;

/**
 * Recipe to wrap the region string provided on the SDK client builder with Region.of.
 *
 * {@snippet :
 *    SqsClient.builder()
 *             .region("us-west-2")
 *             .build();
 * }
 *
 * to
 *
 * {@snippet :
 *    SqsClient.builder()
 *             .region(Region.of("us-west-2"))
 *             .build();
 * }
 */
@SdkInternalApi
public class WrapSdkClientBuilderRegionStr extends Recipe {
    @Override
    public String getDisplayName() {
        return "Wrap the region string provided on the SDK client builder with Region.of";
    }

    @Override
    public String getDescription() {
        return "Wrap the region string provided on the SDK client builder with Region.of.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new V1GetterToV2Visitor();
    }

    private static class V1GetterToV2Visitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation originalMethod, ExecutionContext executionContext) {
            J.MethodInvocation method = super.visitMethodInvocation(originalMethod, executionContext);

            if (!isSdkClientBuilder(method)) {
                return method;
            }

            String methodName = method.getSimpleName();
            if (!methodName.equals("region")) {
                return method;
            }

            JavaType.Method methodType = method.getMethodType();

            if (methodType == null) {
                return method;
            }

            List<JavaType> parameterTypes = methodType.getParameterTypes();
            if (parameterTypes.size() != 1 || !TypeUtils.isString(parameterTypes.get(0))) {
                return method;
            }

            String regionFqcn = Region.class.getCanonicalName();

            JavaTemplate template = JavaTemplate
                .builder("Region.of(#{any()})")
                .imports("software.amazon.awssdk.regions.Region")
                .build();

            maybeAddImport(regionFqcn, false);
            List<Object> arguments = new ArrayList<>(method.getArguments());

            method = template.apply(
                updateCursor(method),
                method.getCoordinates().replaceArguments(),
                arguments.toArray(new Object[0])
            );

            autoFormat(method, executionContext);

            return method;
        }

        private static boolean isSdkClientBuilder(J.MethodInvocation method) {
            return Optional.ofNullable(method.getMethodType()).map(mt -> mt.getReturnType())
                           .filter(t -> isV2ClientClass(t))
                           .isPresent();
        }
    }
}
