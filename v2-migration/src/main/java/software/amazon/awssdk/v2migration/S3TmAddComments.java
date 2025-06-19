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

import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.V1_TM_PKG;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.V2_S3_CLIENT;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.V2_S3_MODEL_PKG;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.V2_TM_CLIENT;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.createComments;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.v2TmMethodMatcher;

import java.util.regex.Pattern;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class S3TmAddComments extends Recipe {

    private static final Pattern S3_TM = Pattern.compile(V2_TM_CLIENT);
    private static final Pattern S3_CLIENT = Pattern.compile(V2_S3_CLIENT);

    private static final MethodMatcher COPY = v2TmMethodMatcher("copy(..)");
    private static final MethodMatcher DOWNLOAD = v2TmMethodMatcher(String.format("download(%sGetObjectRequest, java.io.File, "
                                                                              + "%sinternal.S3ProgressListener, ..)", V2_S3_MODEL_PKG,
                                                                                            V1_TM_PKG));
    private static final MethodMatcher DOWNLOAD_DIRECTORY = v2TmMethodMatcher("downloadDirectory(..)");
    private static final MethodMatcher UPLOAD = v2TmMethodMatcher("upload(..)");
    private static final MethodMatcher UPLOAD_DIRECTORY = v2TmMethodMatcher("uploadDirectory(..)");

    @Override
    public String getDisplayName() {
        return "Add imports and comments to unsupported S3 transfer manager transforms.";
    }

    @Override
    public String getDescription() {
        return "Add imports and comments to unsupported S3 transfer manager transforms.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new S3TmAddComments.Visitor();
    }

    private static class Visitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            if (COPY.matches(method) && (method.getArguments().size() == 2 || method.getArguments().size() == 3)) {
                String comment = "Migration for TransferStateChangeListener is not supported by the migration tool. Please "
                                 + "manually migrate the code using TransferListener in v2";
                return method.withComments(createComments(comment));
            }
            if (DOWNLOAD.matches(method)) {
                String comment = "Migration for S3ProgressListener is not supported by the migration tool. Please manually "
                                 + "migrate the code using TransferListener in v2";
                return method.withComments(createComments(comment));
            }
            if (DOWNLOAD_DIRECTORY.matches(method) && method.getArguments().size() > 3) {
                String comment = "Migration for KeyFilter is not supported by the migration tool. Please "
                                 + "manually migrate the code using DownloadFilter in v2";
                return method.withComments(createComments(comment));
            }
            if (UPLOAD.matches(method) && method.getArguments().size() == 4) {
                String comment = "Migration for InputStream and ObjectMetadata as argument for upload is not supported by the "
                                 + "migration tool.";
                return method.withComments(createComments(comment));
            }
            if (UPLOAD_DIRECTORY.matches(method) && method.getArguments().size() > 4) {
                String comment = "Migration for ObjectMetadataProvider as argument for uploadDirectory is not supported by the "
                                 + "migration tool.";
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

            if (type.isAssignableFrom(S3_TM) &&
                !newClass.getArguments().isEmpty() &&
                newClass.getArguments().get(0).getType() != null) {
                if (newClass.getArguments().get(0).getType().isAssignableFrom(S3_CLIENT)) {
                    String comment = "S3TransferManager requires S3AsyncClient in v2. Please create a new S3AsyncClient "
                                     + "instance for v2 S3TransferManager.";
                    return newClass.withComments(createComments(comment));
                }
            }

            return newClass;
        }
    }
}
