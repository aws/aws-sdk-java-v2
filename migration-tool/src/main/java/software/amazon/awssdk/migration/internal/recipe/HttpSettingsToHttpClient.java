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

package software.amazon.awssdk.migration.internal.recipe;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;

@SdkInternalApi
public class HttpSettingsToHttpClient extends Recipe {

    private static final Set<String> HTTP_SETTINGS_NAMES = new HashSet<>(Arrays.asList("connectionTimeout",
                                                                                       "connectionMaxIdleMillis",
                                                                                       "tcpKeepAlive",
                                                                                       "connectionTtl",
                                                                                       "socketTimeout",
                                                                                       "maxConnections"));

    @Override
    public String getDisplayName() {
        return "Move HTTP settings on the ClientOverrideConfiguration to settings on the ApacheHttpClient";
    }

    @Override
    public String getDescription() {
        return "Move HTTP settings on the ClientOverrideConfiguration to settings on the ApacheHttpClient.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static final class Visitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            method = super.visitMethodInvocation(method, executionContext).cast();

            if (!Optional.ofNullable(method.getMethodType()).map(mt -> mt.getDeclaringType())
                        .filter(t -> t.isAssignableTo(ClientOverrideConfiguration.class.getCanonicalName()))
                         .isPresent()) {
                return method;
            }

            Expression methodSelectExpression = method.getSelect();
            if (methodSelectExpression == null || !(methodSelectExpression instanceof J.MethodInvocation)) {
                return method;
            }

            // method: ClientOverrideConfiguration.builder().maxConnection(xx).aNonHttpSetting(xx)
            // method.getSelect: ClientOverrideConfiguration.builder().maxConnection(xx)
            J.MethodInvocation selectInvoke = (J.MethodInvocation) methodSelectExpression;

            String methodName = selectInvoke.getSimpleName();
            if (!HTTP_SETTINGS_NAMES.contains(methodName)) {
                return method;
            }

            if (!(selectInvoke.getSelect() instanceof J.MethodInvocation)) {
                return method;
            }

            // select.getSelect: ClientOverrideConfiguration.builder()
            J.MethodInvocation selectInvokeSelect = (J.MethodInvocation) selectInvoke.getSelect();
            Space selectPrefix = selectInvoke.getPrefix();

            // new method: ClientOverrideConfiguration.builder().aNonHttpSetting(xx)
            method = method.withSelect(selectInvokeSelect).withPrefix(selectPrefix);

            return method;
        }
    }
}
