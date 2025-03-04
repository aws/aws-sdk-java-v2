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

import java.nio.ByteBuffer;
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
import org.openrewrite.java.tree.JavaType.FullyQualified;
import org.openrewrite.java.tree.JavaType.Method;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils;

@SdkInternalApi
public class ByteBufferToSdkBytes extends Recipe {
    private static final Pattern BYTE_BUFFER_PATTERN = Pattern.compile(ByteBuffer.class.getCanonicalName());

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Convert ByteBuffer to SdkBytes";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Convert ByteBuffer to SdkBytes by calling SdkBytes#fromByteBuffer";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new SdkBytesToBufferVisitor();
    }

    private static final class SdkBytesToBufferVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation originalMethod,
                                                        ExecutionContext executionContext) {
            J.MethodInvocation method =
                super.visitMethodInvocation(originalMethod, executionContext);
            if (!isV2ModelSetterWithByteBufferParam(method)) {
                return method;
            }

            JavaTemplate template = JavaTemplate
                .builder("SdkBytes.fromByteBuffer(#{any()})")
                .contextSensitive()
                .imports(SdkBytes.class.getCanonicalName())
                .build();

            method = template.apply(
                updateCursor(method),
                method.getCoordinates().replaceArguments(),
                method.getArguments().toArray()
            );

            maybeAddImport(SdkBytes.class.getCanonicalName(), false);
            return method;
        }

        private static boolean isV2ModelSetterWithByteBufferParam(J.MethodInvocation method) {
            Method mt = method.getMethodType();

            if (mt != null) {
                FullyQualified declaringType = mt.getDeclaringType();
                List<JavaType> parameterTypes = mt.getParameterTypes();
                if (parameterTypes.size() != 1) {
                    return false;
                }

                JavaType javaType = parameterTypes.get(0);
                if (javaType == null) {
                    return false;
                }

                boolean isByteBufferParam = javaType.isAssignableFrom(BYTE_BUFFER_PATTERN);
                if (SdkTypeUtils.isV2ModelBuilder(declaringType) && isByteBufferParam) {
                    return true;
                }
            }
            return false;
        }
    }
}
