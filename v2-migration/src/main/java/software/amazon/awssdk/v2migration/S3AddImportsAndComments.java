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

import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.V1_S3_MODEL_PKG;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.V2_S3_MODEL_PKG;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.createComments;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.v1EnMethodMatcher;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.v1S3MethodMatcher;

import java.util.List;
import java.util.regex.Pattern;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.RemoveImport;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class S3AddImportsAndComments extends Recipe {

    private static final MethodMatcher CREATE_BUCKET = v1S3MethodMatcher("createBucket(String, "
                                                                         + V1_S3_MODEL_PKG + "Region");
    private static final MethodMatcher LIST_NEXT_BATCH_OBJECTS = v1S3MethodMatcher("listNextBatchOfObjects(..)");
    private static final MethodMatcher LIST_NEXT_BATCH_VERSIONS = v1S3MethodMatcher("listNextBatchOfVersions(..)");
    private static final MethodMatcher GET_METADATA = v1S3MethodMatcher("getCachedResponseMetadata(..)");
    private static final MethodMatcher SET_BUCKET_ACL = v1S3MethodMatcher("setBucketAcl(..)");
    private static final MethodMatcher SET_BUCKET_LOGGING = v1S3MethodMatcher("setBucketLoggingConfiguration(..)");
    private static final MethodMatcher SET_ENDPOINT = v1S3MethodMatcher("setEndpoint(..)");
    private static final MethodMatcher SET_OBJECT_ACL = v1S3MethodMatcher("setObjectAcl(..)");
    private static final MethodMatcher SET_REGION = v1S3MethodMatcher("setRegion(..)");
    private static final MethodMatcher SET_PAYMENT_CONFIGURATION = v1S3MethodMatcher("setRequestPaymentConfiguration(..)");
    private static final MethodMatcher SET_S3CLIENT_OPTIONS = v1S3MethodMatcher("setS3ClientOptions(..)");
    private static final MethodMatcher SELECT_OBJECT_CONTENT = v1S3MethodMatcher("selectObjectContent(..)");
    private static final MethodMatcher SET_LIFECYCLE_CONFIGURATION = v1S3MethodMatcher("setBucketLifecycleConfiguration(..)");
    private static final MethodMatcher SET_TAGGING_CONFIGURATION = v1S3MethodMatcher("setBucketTaggingConfiguration(..)");
    private static final MethodMatcher GET_EVENT_TIME = v1EnMethodMatcher("S3EventNotification.S3EventNotificationRecord "
                                                                          + "getEventTime(..)");
    private static final MethodMatcher  GET_EXPIRY_TIME = v1EnMethodMatcher("S3EventNotification.RestoreEventDataEntity "
                                                                            + "getLifecycleRestorationExpiryTime(..)");


    private static final Pattern CANNED_ACL = Pattern.compile(V1_S3_MODEL_PKG + "CannedAccessControlList");
    private static final Pattern GET_OBJECT_REQUEST = Pattern.compile(V1_S3_MODEL_PKG + "GetObjectRequest");
    private static final Pattern CREATE_BUCKET_REQUEST = Pattern.compile(V1_S3_MODEL_PKG + "CreateBucketRequest");
    private static final Pattern DELETE_OBJECTS_RESULT = Pattern.compile(V1_S3_MODEL_PKG + "DeleteObjectsResult");
    private static final Pattern INITIATE_MPU = Pattern.compile(V1_S3_MODEL_PKG + "InitiateMultipartUpload");
    private static final Pattern MULTI_FACTOR_AUTH = Pattern.compile(V1_S3_MODEL_PKG + "MultiFactorAuthentication");
    private static final Pattern SET_BUCKET_VERSION_REQUEST = Pattern.compile(V1_S3_MODEL_PKG
                                                                              + "SetBucketVersioningConfigurationRequest");
    private static final Pattern BUCKET_NOTIFICATION_CONFIG = Pattern.compile(V1_S3_MODEL_PKG
                                                                              + "BucketNotificationConfiguration");

    @Override
    public String getDisplayName() {
        return "Add imports and comments to unsupported S3 transforms.";
    }

    @Override
    public String getDescription() {
        return "Add imports and comments to unsupported S3 transforms.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static class Visitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            boolean isSetObjectAcl = SET_OBJECT_ACL.matches(method);
            boolean isSetBucketAcl = SET_BUCKET_ACL.matches(method);

            if (isSetObjectAcl || isSetBucketAcl) {
                removeV1S3ModelImport("CannedAccessControlList");
                maybeAddV2CannedAclImport(method.getArguments(), isSetObjectAcl, isSetBucketAcl);

                // TODO: add the developer guide link in the comments once the doc is published.
                String comment = "Transform for AccessControlList and CannedAccessControlList not supported. "
                                 + "In v2, CannedAccessControlList is replaced by BucketCannedACL for buckets and "
                                 + "ObjectCannedACL for objects.";
                return method.withComments(createComments(comment));
            }
            if (LIST_NEXT_BATCH_OBJECTS.matches(method)) {
                // TODO: add the developer guide link in the comments once the doc is published.
                String comment = "Transform for listNextBatchOfObjects method not supported. "
                                 + "listNextBatchOfObjects() only exists in SDK v1, for SDK v2 use either "
                                 + "listObjectsV2Paginator().stream() for automatic pagination"
                                 + " or manually handle pagination with listObjectsV2() and nextToken in the response for more "
                                 + "control";
                return method.withComments(createComments(comment));
            }
            if (LIST_NEXT_BATCH_VERSIONS.matches(method)) {
                // TODO: add the developer guide link in the comments once the doc is published.
                String comment = "Transform for listNextBatchOfVersions method not supported."
                                + "listNextBatchOfVersions() only exists in SDK v1, for SDK v2 use either "
                                + "listObjectVersionsPaginator().stream for automatic pagination"
                                + " or manually handle pagination with listObjectVersions() and VersionIdMarker/KeyMarker. ";
                return method.withComments(createComments(comment));
            }
            if (SET_REGION.matches(method)) {
                String comment = "Transform for setRegion method not supported. Please manually "
                                 + "migrate your code by configuring the region in the s3 client builder";
                return method.withComments(createComments(comment));
            }
            if (SET_S3CLIENT_OPTIONS.matches(method)) {
                String comment = "Transform for setS3ClientOptions method not supported. Please manually "
                                 + "migrate setS3ClientOptions by configuring the equivalent settings in "
                                 + "S3Configuration.builder() when building your S3Client.";
                return method.withComments(createComments(comment));
            }
            if (SELECT_OBJECT_CONTENT.matches(method)) {
                String comment = "Note: selectObjectContent is only supported in AWS SDK v2 with S3AsyncClient. "
                                 + "Please manually migrate to event-based response handling using "
                                 + "SelectObjectContentEventStream";
                return method.withComments(createComments(comment));
            }

            if (CREATE_BUCKET.matches(method)) {
                String comment = "Transform for createBucket(String bucketName, Region region) method not supported. Please "
                                 + "manually migrate your code by using the following pattern: "
                                 + "createBucket(builder -> builder.bucket(bucketName)"
                                 + ".createBucketConfiguration(cfg -> cfg.locationConstraint(region)))";
                return method.withComments(createComments(comment));
            }

            if (SET_ENDPOINT.matches(method)) {
                String comment = "Transform for setEndpoint method not supported. setEndpoint() method is removed in SDK v2. "
                                 + "Please manually migrate your code by using endpointOverride(URI.create(endpoint)) in "
                                 + "S3ClientBuilder";
                return method.withComments(createComments(comment));
            }

            if (GET_METADATA.matches(method)) {
                String comment = "Transform for getCachedResponseMetadata method not "
                                 + "supported. getCachedResponseMetadata() is removed in SDK v2. Please manually migrate your "
                                 + "code by accessing metadata directly from specific response objects instead of cached "
                                 + "metadata";
                return method.withComments(createComments(comment));
            }

            if (SET_BUCKET_LOGGING.matches(method)) {
                // TODO: add the developer guide link in the comments once the doc is published.
                removeV1S3ModelImport("BucketLoggingConfiguration");
                addV2S3ModelImport("BucketLoggingStatus");
                addV2S3ModelImport("LoggingEnabled");

                String comment = "Transform for setBucketLoggingConfiguration method not "
                                 + "supported. The method is renamed to putBucketLogging. Please manually migrate your code by "
                                 + "replacing BucketLoggingConfiguration with BucketLoggingStatus and LoggingEnabled builders, "
                                 + "and updating the method name and parameters";
                return method.withComments(createComments(comment));
            }

            if (SET_PAYMENT_CONFIGURATION.matches(method)) {
                String comment = "Transform for setRequestPaymentConfiguration method not supported. Payer enum is a "
                                 + "separate class in v2 (not nested). Please manually migrate "
                                 + "your code by updating from RequestPaymentConfiguration.Payer to just Payer, and adjust "
                                 + "imports and names.";
                return method.withComments(createComments(comment));
            }

            if (SET_LIFECYCLE_CONFIGURATION.matches(method)) {
                // TODO: add the developer guide link in the comments once the doc is published.
                String comment = "Transform for setBucketLifecycleConfiguration method not supported. Please manually migrate"
                                 + " your code by using builder pattern, updating from BucketLifecycleConfiguration.Rule to "
                                 + "LifecycleRule, StorageClass to TransitionStorageClass, and adjust "
                                 + "imports and names.";
                return method.withComments(createComments(comment));
            }

            if (SET_TAGGING_CONFIGURATION.matches(method)) {
                // TODO: add the developer guide link in the comments once the doc is published.
                String comment = "Transform for setBucketTaggingConfiguration method not supported. Please manually migrate"
                                 + " your code by using builder pattern, replacing TagSet.setTag() with .tagSet(Arrays.asList"
                                 + "(Tag.builder())), and use Tagging instead of BucketTaggingConfiguration, and adjust imports"
                                 + " and names.";
                return method.withComments(createComments(comment));
            }

            if (GET_EVENT_TIME.matches(method) || GET_EXPIRY_TIME.matches(method)) {
                String comment = method.getSimpleName() + " returns Instant instead of DateTime in v2. AWS SDK v2 does not "
                                 + "include org.joda.time as a dependency. If you want to keep using DateTime, you'll need to "
                                 + "manually add \"org.joda.time:joda-time\" dependency to your"
                                 + " project after migration.";
                return method.withComments(createComments(comment));
            }
            return method;
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            JavaType type = newClass.getType();
            if (!(type instanceof JavaType.FullyQualified)) {
                return newClass;
            }

            boolean setBucketVersionUsingMFA =
                type.isAssignableFrom(SET_BUCKET_VERSION_REQUEST) && newClass.getArguments().size() == 3;
            if (type.isAssignableFrom(MULTI_FACTOR_AUTH) || setBucketVersionUsingMFA) {
                removeV1S3ModelImport("MultiFactorAuthentication");
                String comment = "v2 does not have a MultiFactorAuthentication POJO. Please manually set the String value on "
                                 + "the request POJO.";
                return newClass.withComments(createComments(comment));
            }

            if (type.isAssignableFrom(GET_OBJECT_REQUEST) && newClass.getArguments().size() == 1) {
                removeV1S3ModelImport("S3ObjectId");
                String comment = "v2 does not have S3ObjectId class. Please manually migrate the code by setting the configs "
                                 + "directly into the request builder pattern.";
                return newClass.withComments(createComments(comment));
            }

            if (type.isAssignableFrom(INITIATE_MPU) && newClass.getArguments().size() == 3) {
                String comment = "Transform for ObjectMetadata in initiateMultipartUpload() method is not supported. Please "
                                 + "manually migrate your code by replacing ObjectMetadata with individual setter methods "
                                 + "or metadata map in the request builder.";
                return newClass.withComments(createComments(comment));
            }

            if (type.isAssignableFrom(CREATE_BUCKET_REQUEST) && newClass.getArguments().size() == 2) {
                String comment = "Transform for createBucketRequest with region is not supported. Please manually "
                                 + "migrate your code by configuring the region as locationConstraint in "
                                 + "createBucketConfiguration in the request builder";
                return newClass.withComments(createComments(comment));
            }

            if (type.isAssignableFrom(DELETE_OBJECTS_RESULT)) {
                String comment = "Transform for DeleteObjectsResult class is not supported. DeletedObject class is a "
                                 + "separate class in v2 (not nested). Please manually migrate your code by updating "
                                 + "DeleteObjectsResult.DeletedObject to s3.model.DeletedObject";
                return newClass.withComments(createComments(comment));
            }

            if (type.isAssignableFrom(BUCKET_NOTIFICATION_CONFIG)) {
                // TODO: add the developer guide link in the comments once the doc is published.
                String comment = "Transform for BucketNotificationConfiguration class is not supported. "
                                 + "BucketNotificationConfiguration is renamed to NotificationConfiguration. There is no common"
                                 + " abstract class for lambdaFunction/topic/queue configurations. Use specific builders "
                                 + "instead of addConfiguration() to add configurations. Change the vararg arguments or EnumSet "
                                 + "in specific configurations constructor to List<String> in v2";
                return newClass.withComments(createComments(comment));
            }

            return newClass;
        }

        private void maybeAddV2CannedAclImport(List<Expression> args, boolean isSetObjectAcl, boolean isSetBucketAcl) {
            for (Expression expr : args) {
                JavaType type = expr.getType();
                if (type == null || !type.isAssignableFrom(CANNED_ACL)) {
                    continue;
                }
                removeV1S3ModelImport("CannedAccessControlList");
                if (isSetBucketAcl) {
                    addV2S3ModelImport("BucketCannedACL");
                }
                if (isSetObjectAcl) {
                    addV2S3ModelImport("ObjectCannedACL");
                }
            }
        }

        private void removeV1S3ModelImport(String className) {
            doAfterVisit(new RemoveImport<>(V1_S3_MODEL_PKG + className, true));
        }

        private void addV2S3ModelImport(String className) {
            doAfterVisit(new AddImport<>(V2_S3_MODEL_PKG + className, null, false));
        }
    }
}
