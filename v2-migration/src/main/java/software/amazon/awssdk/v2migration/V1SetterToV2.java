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

import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.hasArguments;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.isS3PutObjectOrObjectMetadata;
import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.isEligibleToConvertToBuilder;
import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.isV2CoreClassBuilder;
import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.isV2ModelBuilder;

import java.util.Map;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.utils.ImmutableMap;
import software.amazon.awssdk.v2migration.internal.utils.NamingUtils;

/**
 * Internal recipe that renames fluent V1 setters (withers), to V2 equivalents for generated model classes and client classes.
 *
 * @see NewClassToBuilderPattern
 * TODO: separate model classes and client classes
 */
@SdkInternalApi
public class V1SetterToV2 extends Recipe {
    private static final Map<String, String> CLIENT_CONFIG_NAMING_MAPPING =
        // TODO: handle other settings on the builder such as withEndpointConfiguration,
        //  withMonitoringListener and withMetricsCollector
        ImmutableMap.<String, String>builder()
                    .put("credentials", "credentialsProvider")
                    .put("clientConfiguration", "overrideConfiguration")
                    .put("endpointConfiguration", "endpointOverride")
                    .build();

    @Override
    public String getDisplayName() {
        return "V1 Setter to V2";
    }

    @Override
    public String getDescription() {
        return "Transforms V1 setter to fluent setter in V2.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new V1SetterToV2Visitor();
    }

    private static class V1SetterToV2Visitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation previousMethodInvocation,
                                                        ExecutionContext executionContext) {
            J.MethodInvocation method = super.visitMethodInvocation(previousMethodInvocation, executionContext);

            if (!hasArguments(method)) {
                return method;
            }

            JavaType selectType = null;

            Expression select = method.getSelect();
            if (select != null) {
                selectType = select.getType();
            }

            JavaType.Method methodType = method.getMethodType();
            if (selectType == null || methodType == null) {
                return method;
            }

            String methodName = method.getSimpleName();
            JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(selectType);

            if (fullyQualified == null || !shouldChangeSetter(fullyQualified)) {
                return method;
            }

            if (!NamingUtils.isWither(methodName) && !NamingUtils.isSetter(methodName) && !isClientBuilderClass(methodType)) {
                return method;
            }

            if (methodName.contains("BucketName")) {
                methodName = methodName.replace("BucketName", "Bucket");
            }

            if (NamingUtils.isWither(methodName)) {
                methodName = NamingUtils.removeWith(methodName);
            } else if (NamingUtils.isSetter(methodName) && isS3PutObjectOrObjectMetadata(method)) {
                // We will change remaining setters to `request = request.toBuilder().setter(val).build()` in SettersToBuilderV2
                methodName = NamingUtils.removeSet(methodName);
            }

            if (isClientBuilderClass(methodType)) {
                methodName = CLIENT_CONFIG_NAMING_MAPPING.getOrDefault(methodName, methodName);
            }

            methodType = methodType.withName(methodName)
                                   .withReturnType(selectType)
                                   .withDeclaringType(fullyQualified);

            method = method.withName(method.getName()
                                           .withSimpleName(methodName)
                                           .withType(methodType))
                           .withMethodType(methodType);

            return maybeAutoFormat(previousMethodInvocation, method, executionContext);
        }

        private static boolean isClientBuilderClass(JavaType.Method methodType) {
            String fullyQualifiedName = methodType.getDeclaringType().getFullyQualifiedName();
            return AwsSyncClientBuilder.class.getCanonicalName().equals(fullyQualifiedName) ||
                   AwsAsyncClientBuilder.class.getCanonicalName().equals(fullyQualifiedName);
        }

        private static boolean shouldChangeSetter(JavaType.FullyQualified selectType) {
            return isEligibleToConvertToBuilder(selectType)
                   || isV2ModelBuilder(selectType)
                   || isV2CoreClassBuilder(selectType.getFullyQualifiedName());
        }
    }
}
