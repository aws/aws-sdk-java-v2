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

package software.amazon.awssdk.v2migration.internal.utils;

import java.util.Collections;
import java.util.List;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class S3TransformUtils {

    public static final String V1_S3_CLIENT = "com.amazonaws.services.s3.AmazonS3";
    public static final String V1_S3_MODEL_PKG = "com.amazonaws.services.s3.model.";
    public static final String V1_S3_PKG = "com.amazonaws.services.s3.";

    public static final String V2_S3_CLIENT = "software.amazon.awssdk.services.s3.S3Client";
    public static final String V2_S3_MODEL_PKG = "software.amazon.awssdk.services.s3.model.";
    public static final String V2_S3_PKG = "software.amazon.awssdk.services.s3.";

    public static final String V2_TM_CLIENT = "software.amazon.awssdk.transfer.s3.S3TransferManager";
    public static final String V2_TM_MODEL_PKG = "software.amazon.awssdk.transfer.s3.model.";

    private S3TransformUtils() {

    }

    public static MethodMatcher v1S3MethodMatcher(String methodSignature) {
        return new MethodMatcher(V1_S3_CLIENT + " " + methodSignature, true);
    }

    public static MethodMatcher v2S3MethodMatcher(String methodSignature) {
        return new MethodMatcher(V2_S3_CLIENT + " " + methodSignature, true);
    }

    public static MethodMatcher v2TmMethodMatcher(String methodSignature) {
        return new MethodMatcher(V2_TM_CLIENT + " " + methodSignature, true);
    }

    public static List<Comment> createComments(String comment) {
        return Collections.singletonList(
            new TextComment(true, "AWS SDK for Java v2 migration: " + comment, "", Markers.EMPTY));
    }
}
