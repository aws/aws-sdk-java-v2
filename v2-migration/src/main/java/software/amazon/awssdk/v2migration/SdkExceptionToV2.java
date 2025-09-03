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

import static software.amazon.awssdk.v2migration.internal.utils.NamingUtils.transformMethodName;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class SdkExceptionToV2 extends Recipe {

    public static MethodMatcher v1ExceptionMethod(String methodSignature) {
        return new MethodMatcher("com.amazonaws.AmazonServiceException " + methodSignature, true);
    }

    @Override
    public String getDisplayName() {
        return "SDK Exceptions Methods to V2";
    }

    @Override
    public String getDescription() {
        return "SDK Exceptions Methods to V2";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static final class Visitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            if (v1ExceptionMethod("getErrorCode()").matches(method)) {
                String newMethodName = "awsErrorDetails().errorCode()";
                method = transformMethodName(method, newMethodName, getCursor());
                return super.visitMethodInvocation(method, executionContext);
            }
            if (v1ExceptionMethod("getServiceName()").matches(method)) {
                String newMethodName = "awsErrorDetails().serviceName()";
                method = transformMethodName(method, newMethodName, getCursor());
                return super.visitMethodInvocation(method, executionContext);
            }
            if (v1ExceptionMethod("getErrorMessage()").matches(method)) {
                String newMethodName = "awsErrorDetails().errorMessage()";
                method = transformMethodName(method, newMethodName, getCursor());
                return super.visitMethodInvocation(method, executionContext);
            }
            if (v1ExceptionMethod("getStatusCode()").matches(method)) {
                String newMethodName = "awsErrorDetails().sdkHttpResponse().statusCode()";
                method = transformMethodName(method, newMethodName, getCursor());
                return super.visitMethodInvocation(method, executionContext);
            }
            if (v1ExceptionMethod("getHttpHeaders()").matches(method)) {
                // TODO: v2 returns Map<String, List<String>>. Convert it to Map<String, String>
                String newMethodName = "awsErrorDetails().sdkHttpResponse().headers()";
                method = transformMethodName(method, newMethodName, getCursor());
                return super.visitMethodInvocation(method, executionContext);
            }
            if (v1ExceptionMethod("getRawResponse()").matches(method)) {
                String newMethodName = "awsErrorDetails().rawResponse().asByteArray()";
                method = transformMethodName(method, newMethodName, getCursor());
                return super.visitMethodInvocation(method, executionContext);
            }
            if (v1ExceptionMethod("getRawResponseContent()").matches(method)) {
                String newMethodName = "awsErrorDetails().rawResponse().asUtf8String()";
                method = transformMethodName(method, newMethodName, getCursor());
                return super.visitMethodInvocation(method, executionContext);
            }

            return super.visitMethodInvocation(method, executionContext);
        }
    }


}
