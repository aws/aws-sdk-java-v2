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

import static software.amazon.awssdk.v2migration.internal.utils.NamingUtils.isSetter;
import static software.amazon.awssdk.v2migration.internal.utils.NamingUtils.removeSet;
import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.isV2ModelClass;

import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class SettersToBuilderV2 extends Recipe {

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Convert V1 setters to V2 toBuilder setters";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Convert V1 setters to V2 toBuilder setters";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static final class Visitor extends JavaVisitor<ExecutionContext> {


        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {

            if (isV2ModelClassSetter(method)) {
                return convertSetterToBuilder(method);
            }

            return method;
        }

        private boolean isV2ModelClassSetter(J.MethodInvocation method) {
            return isV2ModelClass(method.getType())
                   && method.getArguments().size() == 1
                   && isSetter(method.getSimpleName());
        }

        private J convertSetterToBuilder(J.MethodInvocation method) {
            String v2Method = String.format("#{any()} = #{any()}.toBuilder().%s(#{any()}).build()",
                                            removeSet(method.getSimpleName()));

            return JavaTemplate.builder(v2Method).build()
                               .apply(getCursor(), method.getCoordinates().replace(),
                                      method.getSelect(), method.getSelect(), method.getArguments().get(0));
        }
    }
}
