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

import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.V1_S3_CLIENT;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.V1_S3_MODEL_PKG;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.v1S3MethodMatcher;
import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.fullyQualified;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.v2migration.internal.utils.IdentifierUtils;

@SdkInternalApi
public class S3NonStreamingRequestToV2 extends Recipe {

    private static final MethodMatcher DELETE_VERSION = v1S3MethodMatcher("deleteVersion(String, String, String)");
    private static final MethodMatcher COPY_OBJECT = v1S3MethodMatcher("copyObject(String, String, String, String)");
    private static final MethodMatcher LIST_VERSIONS =
        v1S3MethodMatcher("listVersions(String, String, String, String, String, Integer)");
    private static final MethodMatcher SET_BUCKET_POLICY = v1S3MethodMatcher("setBucketPolicy(String, String)");
    private static final MethodMatcher GET_OBJECT_ACL = v1S3MethodMatcher("getObjectAcl(String, String, String)");
    private static final MethodMatcher SET_BUCKET_ACCELERATE_CONFIGURATION = v1S3MethodMatcher(
        String.format("setBucketAccelerateConfiguration(String, %sBucketAccelerateConfiguration)", V1_S3_MODEL_PKG));
    private static final MethodMatcher SET_BUCKET_CROSS_ORIGIN_CONFIGURATION = v1S3MethodMatcher(
        String.format("setBucketCrossOriginConfiguration(String, %sBucketCrossOriginConfiguration)", V1_S3_MODEL_PKG));
    private static final MethodMatcher SET_BUCKET_ANALYTICS_CONFIGURATION = v1S3MethodMatcher(
        String.format("setBucketAnalyticsConfiguration(String, %sanalytics.AnalyticsConfiguration)", V1_S3_MODEL_PKG));
    private static final MethodMatcher SET_BUCKET_INTELLIGENT_TIERING_CONFIGURATION = v1S3MethodMatcher(
        String.format("setBucketIntelligentTieringConfiguration("
                      + "String, %sintelligenttiering.IntelligentTieringConfiguration)", V1_S3_MODEL_PKG));
    private static final MethodMatcher SET_BUCKET_INVENTORY_CONFIGURATION = v1S3MethodMatcher(
        String.format("setBucketInventoryConfiguration(String, %sinventory.InventoryConfiguration)", V1_S3_MODEL_PKG));
    private static final MethodMatcher SET_BUCKET_LIFECYCLE_CONFIGURATION = v1S3MethodMatcher(
        String.format("setBucketLifecycleConfiguration(String, %sBucketLifecycleConfiguration)", V1_S3_MODEL_PKG));
    private static final MethodMatcher SET_BUCKET_METRICS_CONFIGURATION = v1S3MethodMatcher(
        String.format("setBucketMetricsConfiguration(String, %smetrics.MetricsConfiguration)", V1_S3_MODEL_PKG));
    private static final MethodMatcher SET_BUCKET_NOTIFICATION_CONFIGURATION = v1S3MethodMatcher(
        String.format("setBucketNotificationConfiguration(String, %sBucketNotificationConfiguration)", V1_S3_MODEL_PKG));
    private static final MethodMatcher SET_BUCKET_OWNERSHIP_CONTROLS = v1S3MethodMatcher(
        String.format("setBucketOwnershipControls(String, %sownership.OwnershipControls)", V1_S3_MODEL_PKG));
    private static final MethodMatcher SET_BUCKET_REPLICATION_CONFIGURATION = v1S3MethodMatcher(
        String.format("setBucketReplicationConfiguration(String, %sBucketReplicationConfiguration)", V1_S3_MODEL_PKG));
    private static final MethodMatcher SET_BUCKET_TAGGING_CONFIGURATION = v1S3MethodMatcher(
        String.format("setBucketTaggingConfiguration(String, %sBucketTaggingConfiguration)", V1_S3_MODEL_PKG));
    private static final MethodMatcher SET_BUCKET_WEBSITE_CONFIGURATION = v1S3MethodMatcher(
        String.format("setBucketWebsiteConfiguration(String, %sBucketWebsiteConfiguration)", V1_S3_MODEL_PKG));

    private static final Map<MethodMatcher, JavaType.FullyQualified> BUCKET_ARG_METHODS = new HashMap<>();
    private static final Map<MethodMatcher, JavaType.FullyQualified> BUCKET_KEY_ARGS_METHODS = new HashMap<>();
    private static final Map<MethodMatcher, JavaType.FullyQualified> BUCKET_ID_ARGS_METHODS = new HashMap<>();
    private static final Map<MethodMatcher, JavaType.FullyQualified> BUCKET_PREFIX_ARGS_METHODS = new HashMap<>();

    static {
        BUCKET_ARG_METHODS.put(singleStringArgMethod("createBucket"), fqcn("createBucket"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("deleteBucket"), fqcn("deleteBucket"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("listObjects"), fqcn("listObjects"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("listObjectsV2"), fqcn("listObjectsV2"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("getBucketCrossOriginConfiguration"),
                               fqcn("getBucketCrossOriginConfiguration"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("deleteBucketCrossOriginConfiguration"),
                               fqcn("deleteBucketCrossOriginConfiguration"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("getBucketVersioningConfiguration"),
                               fqcn("getBucketVersioningConfiguration"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("deleteBucketEncryption"), fqcn("deleteBucketEncryption"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("deleteBucketPolicy"), fqcn("deleteBucketPolicy"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("getBucketAccelerateConfiguration"),
                               fqcn("getBucketAccelerateConfiguration"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("getBucketAcl"), fqcn("getBucketAcl"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("getBucketEncryption"), fqcn("getBucketEncryption"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("getBucketLifecycleConfiguration"),
                               fqcn("getBucketLifecycleConfiguration"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("getBucketNotificationConfiguration"),
                               fqcn("getBucketNotificationConfiguration"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("getBucketPolicy"), fqcn("getBucketPolicy"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("getBucketLocation"), fqcn("getBucketLocation"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("deleteBucketLifecycleConfiguration"),
                               fqcn("deleteBucketLifecycleConfiguration"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("deleteBucketReplicationConfiguration"),
                               fqcn("deleteBucketReplicationConfiguration"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("deleteBucketTaggingConfiguration"),
                               fqcn("deleteBucketTaggingConfiguration"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("deleteBucketWebsiteConfiguration"),
                               fqcn("deleteBucketWebsiteConfiguration"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("getBucketLoggingConfiguration"), fqcn("getBucketLoggingConfiguration"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("getBucketReplicationConfiguration"),
                               fqcn("getBucketReplicationConfiguration"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("getBucketTaggingConfiguration"), fqcn("getBucketTaggingConfiguration"));
        BUCKET_ARG_METHODS.put(singleStringArgMethod("getBucketWebsiteConfiguration"), fqcn("getBucketWebsiteConfiguration"));

        BUCKET_KEY_ARGS_METHODS.put(twoStringArgsMethod("deleteObject"), fqcn("deleteObject"));
        BUCKET_KEY_ARGS_METHODS.put(twoStringArgsMethod("getObject"), fqcn("getObject"));
        BUCKET_KEY_ARGS_METHODS.put(twoStringArgsMethod("getObjectAcl"), fqcn("getObjectAcl"));
        BUCKET_KEY_ARGS_METHODS.put(twoStringArgsMethod("getObjectMetadata"), fqcn("getObjectMetadata"));

        BUCKET_ID_ARGS_METHODS.put(twoStringArgsMethod("deleteBucketAnalyticsConfiguration"),
                                   fqcn("deleteBucketAnalyticsConfiguration"));
        BUCKET_ID_ARGS_METHODS.put(twoStringArgsMethod("deleteBucketIntelligentTieringConfiguration"),
                                   fqcn("deleteBucketIntelligentTieringConfiguration"));
        BUCKET_ID_ARGS_METHODS.put(twoStringArgsMethod("deleteBucketInventoryConfiguration"),
                                   fqcn("deleteBucketInventoryConfiguration"));
        BUCKET_ID_ARGS_METHODS.put(twoStringArgsMethod("deleteBucketMetricsConfiguration"),
                                   fqcn("deleteBucketMetricsConfiguration"));
        BUCKET_ID_ARGS_METHODS.put(twoStringArgsMethod("getBucketAnalyticsConfiguration"),
                                   fqcn("getBucketAnalyticsConfiguration"));
        BUCKET_ID_ARGS_METHODS.put(twoStringArgsMethod("getBucketIntelligentTieringConfiguration"),
                                   fqcn("getBucketIntelligentTieringConfiguration"));
        BUCKET_ID_ARGS_METHODS.put(twoStringArgsMethod("getBucketInventoryConfiguration"),
                                   fqcn("getBucketInventoryConfiguration"));
        BUCKET_ID_ARGS_METHODS.put(twoStringArgsMethod("getBucketMetricsConfiguration"), fqcn("getBucketMetricsConfiguration"));

        BUCKET_PREFIX_ARGS_METHODS.put(twoStringArgsMethod("listObjects"), fqcn("listObjects"));
        BUCKET_PREFIX_ARGS_METHODS.put(twoStringArgsMethod("listObjectsV2"), fqcn("listObjectsV2"));
        BUCKET_PREFIX_ARGS_METHODS.put(twoStringArgsMethod("listVersions"), fqcn("listVersions"));
    }

    private static MethodMatcher singleStringArgMethod(String method) {
        String signature = V1_S3_CLIENT + " " + method + "(java.lang.String)";
        return new MethodMatcher(signature,  true);
    }

    private static MethodMatcher twoStringArgsMethod(String method) {
        String signature = V1_S3_CLIENT + " " + method + "(java.lang.String, java.lang.String)";
        return new MethodMatcher(signature,  true);
    }

    private static JavaType.FullyQualified fqcn(String method) {
        String methodFirstLetterCaps = method.substring(0, 1).toUpperCase(Locale.ROOT) + method.substring(1);
        String typeName = V1_S3_MODEL_PKG + methodFirstLetterCaps + "Request";
        return fullyQualified(typeName);
    }

    @Override
    public String getDisplayName() {
        return "V1 S3 non-streaming requests to V2";
    }

    @Override
    public String getDescription() {
        return "Transform usage of V1 S3 non-streaming requests to V2.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static final class Visitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {

            if (DELETE_VERSION.matches(method)) {
                method = transformMethod(method, fqcn("deleteObject"), "bucket", "key", "versionId");
                return super.visitMethodInvocation(method, executionContext);
            }
            if (COPY_OBJECT.matches(method)) {
                method = transformMethod(method, fqcn("copyObject"),
                                         "sourceBucket", "sourceKey", "destinationBucket", "destinationKey");
                return super.visitMethodInvocation(method, executionContext);
            }
            if (LIST_VERSIONS.matches(method)) {
                method = transformMethod(method, fqcn("listVersions"),
                                         "bucket", "prefix", "keyMarker", "versionIdMarker", "delimiter", "maxKeys");
                return super.visitMethodInvocation(method, executionContext);
            }
            if (SET_BUCKET_POLICY.matches(method)) {
                method = transformMethod(method, fqcn("putBucketPolicy"), "bucket", "policy");
                return super.visitMethodInvocation(method, executionContext);
            }
            if (GET_OBJECT_ACL.matches(method)) {
                method = transformMethod(method, fqcn("getObjectAcl"), "bucket", "key", "versionId");
                return super.visitMethodInvocation(method, executionContext);
            }
            if (SET_BUCKET_ACCELERATE_CONFIGURATION.matches(method)) {
                method = transformMethod(method, fqcn("setBucketAccelerateConfiguration"), "bucket", "accelerateConfiguration");
                return super.visitMethodInvocation(method, executionContext);
            }
            if (SET_BUCKET_CROSS_ORIGIN_CONFIGURATION.matches(method)) {
                method = transformMethod(method, fqcn("setBucketCrossOriginConfiguration"), "bucket", "corsConfiguration");
                return super.visitMethodInvocation(method, executionContext);
            }
            if (SET_BUCKET_ANALYTICS_CONFIGURATION.matches(method)) {
                method = transformMethod(method, fqcn("setBucketAnalyticsConfiguration"), "bucket", "analyticsConfiguration");
                return super.visitMethodInvocation(method, executionContext);
            }
            if (SET_BUCKET_INTELLIGENT_TIERING_CONFIGURATION.matches(method)) {
                method = transformMethod(method, fqcn("setBucketIntelligentTieringConfiguration"),
                                         "bucket", "intelligentTieringConfiguration");
                return super.visitMethodInvocation(method, executionContext);
            }
            if (SET_BUCKET_INVENTORY_CONFIGURATION.matches(method)) {
                method = transformMethod(method, fqcn("setBucketInventoryConfiguration"), "bucket", "inventoryConfiguration");
                return super.visitMethodInvocation(method, executionContext);
            }
            if (SET_BUCKET_LIFECYCLE_CONFIGURATION.matches(method)) {
                method = transformMethod(method, fqcn("setBucketLifecycleConfiguration"), "bucket", "lifecycleConfiguration");
                return super.visitMethodInvocation(method, executionContext);
            }
            if (SET_BUCKET_METRICS_CONFIGURATION.matches(method)) {
                method = transformMethod(method, fqcn("setBucketMetricsConfiguration"), "bucket", "metricsConfiguration");
                return super.visitMethodInvocation(method, executionContext);
            }
            if (SET_BUCKET_NOTIFICATION_CONFIGURATION.matches(method)) {
                method = transformMethod(method, fqcn("setBucketNotificationConfiguration"),
                                         "bucket", "notificationConfiguration");
                return super.visitMethodInvocation(method, executionContext);
            }
            if (SET_BUCKET_OWNERSHIP_CONTROLS.matches(method)) {
                method = transformMethod(method, fqcn("setBucketOwnershipControls"), "bucket", "ownershipControls");
                return super.visitMethodInvocation(method, executionContext);
            }
            if (SET_BUCKET_REPLICATION_CONFIGURATION.matches(method)) {
                method = transformMethod(method, fqcn("setBucketReplicationConfiguration"), "bucket", "replicationConfiguration");
                return super.visitMethodInvocation(method, executionContext);
            }
            if (SET_BUCKET_TAGGING_CONFIGURATION.matches(method)) {
                method = transformMethod(method, fqcn("setBucketTaggingConfiguration"), "bucket", "taggingConfiguration");
                return super.visitMethodInvocation(method, executionContext);
            }
            if (SET_BUCKET_WEBSITE_CONFIGURATION.matches(method)) {
                method = transformMethod(method, fqcn("setBucketWebsiteConfiguration"), "bucket", "configuration");
                return super.visitMethodInvocation(method, executionContext);
            }

            for (Map.Entry<MethodMatcher, JavaType.FullyQualified> entry : BUCKET_ARG_METHODS.entrySet()) {
                if (entry.getKey().matches(method)) {
                    method = transformMethod(method, entry.getValue(), "bucket");
                    return super.visitMethodInvocation(method, executionContext);
                }
            }
            for (Map.Entry<MethodMatcher, JavaType.FullyQualified> entry : BUCKET_KEY_ARGS_METHODS.entrySet()) {
                if (entry.getKey().matches(method)) {
                    method = transformMethod(method, entry.getValue(), "bucket", "key");
                    return super.visitMethodInvocation(method, executionContext);
                }
            }
            for (Map.Entry<MethodMatcher, JavaType.FullyQualified> entry : BUCKET_ID_ARGS_METHODS.entrySet()) {
                if (entry.getKey().matches(method)) {
                    method = transformMethod(method, entry.getValue(), "bucket", "id");
                    return super.visitMethodInvocation(method, executionContext);
                }
            }
            for (Map.Entry<MethodMatcher, JavaType.FullyQualified> entry : BUCKET_PREFIX_ARGS_METHODS.entrySet()) {
                if (entry.getKey().matches(method)) {
                    method = transformMethod(method, entry.getValue(), "bucket", "prefix");
                    return super.visitMethodInvocation(method, executionContext);
                }
            }

            return super.visitMethodInvocation(method, executionContext);
        }

        private J.MethodInvocation transformMethod(J.MethodInvocation method, JavaType.FullyQualified fqcn,
                                                                 String... args) {
            JavaType.Method methodType = method.getMethodType();
            if (methodType == null) {
                return method;
            }

            List<String> names = Arrays.asList(args);
            List<JavaType> types = new ArrayList<>();
            List<JRightPadded<Expression>> expressions = new ArrayList<>();

            for (int i = 0; i < names.size(); i++) {
                Expression expr = method.getArguments().get(i);
                types.add(expr.getType());
                expressions.add(JRightPadded.build(expr));
            }

            Expression newPojo = argsToPojo(fqcn, names, types, JContainer.build(expressions));
            List<Expression> newArgs = Collections.singletonList(newPojo);
            methodType = addParamsToMethod(methodType, newArgs);
            return method.withMethodType(methodType).withArguments(newArgs);
        }

        private JavaType.Method addParamsToMethod(JavaType.Method methodType, List<Expression> newArgs) {
            List<String> paramNames = Collections.singletonList("request");
            List<JavaType> paramTypes = newArgs.stream()
                                               .map(Expression::getType)
                                               .collect(Collectors.toList());

            return methodType.withParameterTypes(paramTypes)
                             .withParameterNames(paramNames);
        }

        private Expression argsToPojo(JavaType.FullyQualified fqcn, List<String> names, List<JavaType> types,
                                      JContainer<Expression> args) {
            maybeAddImport(fqcn);

            J.Identifier requestId = IdentifierUtils.makeId(fqcn.getClassName(), fqcn);

            JavaType.Method ctorType = new JavaType.Method(
                null,
                0L,
                fqcn,
                "<init>",
                fqcn,
                names,
                types,
                null,
                null
            );

            return new J.NewClass(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                null,
                Space.EMPTY,
                requestId.withPrefix(Space.SINGLE_SPACE),
                args,
                null,
                ctorType
            );
        }
    }
}
