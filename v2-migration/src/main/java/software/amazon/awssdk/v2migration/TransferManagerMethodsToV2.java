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

import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.V2_S3_MODEL_PKG;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.V2_TM_MODEL_PKG;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.v2TmMethodMatcher;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class TransferManagerMethodsToV2 extends Recipe {

    private static final MethodMatcher DOWNLOAD_BUCKET_KEY_FILE = v2TmMethodMatcher("download(String, String, java.io.File)");
    private static final MethodMatcher DOWNLOAD_BUCKET_KEY_FILE_TIMEOUT =
        v2TmMethodMatcher("download(String, String, java.io.File, long)");
    private static final MethodMatcher DOWNLOAD_REQUEST_FILE =
        v2TmMethodMatcher(String.format("download(%sGetObjectRequest, java.io.File)", V2_S3_MODEL_PKG));
    private static final MethodMatcher DOWNLOAD_REQUEST_FILE_TIMEOUT =
        v2TmMethodMatcher(String.format("download(%sGetObjectRequest, java.io.File, long)", V2_S3_MODEL_PKG));

    private static final MethodMatcher UPLOAD_BUCKET_KEY_FILE = v2TmMethodMatcher("upload(String, String, java.io.File)");

    private static final MethodMatcher COPY_REQUEST =
        v2TmMethodMatcher(String.format("copy(%sCopyObjectRequest)", V2_S3_MODEL_PKG));
    private static final MethodMatcher COPY_BUCKET_KEY =
        v2TmMethodMatcher("copy(String, String, String, String");

    @Override
    public String getDisplayName() {
        return "Transfer Manager Methods to V2";
    }

    @Override
    public String getDescription() {
        return "Transfer Manager Methods to V2";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static final class Visitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {

            if (DOWNLOAD_BUCKET_KEY_FILE.matches(method, false)) {
                method = transformDownloadWithBucketKeyFile(method);
                return super.visitMethodInvocation(method, executionContext);
            }
            if (DOWNLOAD_BUCKET_KEY_FILE_TIMEOUT.matches(method, false)) {
                method = transformDownloadWithBucketKeyFileTimeout(method);
                return super.visitMethodInvocation(method, executionContext);
            }
            if (DOWNLOAD_REQUEST_FILE.matches(method, false)) {
                method = transformDownloadWithRequestFile(method);
                return super.visitMethodInvocation(method, executionContext);
            }
            if (DOWNLOAD_REQUEST_FILE_TIMEOUT.matches(method, false)) {
                method = transformDownloadWithRequestFileTimeout(method);
                return super.visitMethodInvocation(method, executionContext);
            }
            if (COPY_REQUEST.matches(method, false)) {
                method = transformCopyWithRequest(method);
                return super.visitMethodInvocation(method, executionContext);
            }
            if (COPY_BUCKET_KEY.matches(method, false)) {
                method = transformCopyWithBucketKey(method);
                return super.visitMethodInvocation(method, executionContext);
            }
            if (UPLOAD_BUCKET_KEY_FILE.matches(method, false)) {
                method = transformUploadWithBucketKeyFile(method);
                return super.visitMethodInvocation(method, executionContext);
            }

            return super.visitMethodInvocation(method, executionContext);
        }

        private J.MethodInvocation transformUploadWithBucketKeyFile(J.MethodInvocation method) {
            String v2Method = "#{any()}.uploadFile(UploadFileRequest.builder()"
                              + ".putObjectRequest(PutObjectRequest.builder().bucket(#{any()}).key(#{any()}).build())"
                              + ".source(#{any()}).build())";

            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0), method.getArguments().get(1),
                                        method.getArguments().get(2));

            addTmImport("UploadFileRequest");
            addS3Import("PutObjectRequest");
            return method;
        }

        private J.MethodInvocation transformCopyWithRequest(J.MethodInvocation method) {
            String v2Method = "#{any()}.copy(CopyRequest.builder().copyObjectRequest(#{any()}).build())";

            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0));

            addTmImport("CopyRequest");
            return method;
        }

        private J.MethodInvocation transformCopyWithBucketKey(J.MethodInvocation method) {
            String v2Method = "#{any()}.copy(CopyRequest.builder()"
                              + ".copyObjectRequest(CopyObjectRequest.builder().sourceBucket(#{any()}).sourceKey(#{any()})"
                              + ".destinationBucket(#{any()}).destinationKey(#{any()}).build())"
                              + ".build())";

            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0), method.getArguments().get(1),
                                        method.getArguments().get(2), method.getArguments().get(3));

            addTmImport("CopyRequest");
            addS3Import("CopyObjectRequest");
            return method;
        }

        private J.MethodInvocation transformDownloadWithRequestFile(J.MethodInvocation method) {
            String v2Method = "#{any()}.downloadFile(DownloadFileRequest.builder()"
                              + ".getObjectRequest(#{any()})"
                              + ".destination(#{any()}).build())";

            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0), method.getArguments().get(1));

            addTmImport("DownloadFileRequest");
            return method;
        }

        private J.MethodInvocation transformDownloadWithRequestFileTimeout(J.MethodInvocation method) {
            String v2Method = "#{any()}.downloadFile(DownloadFileRequest.builder()"
                              + ".getObjectRequest(#{any()}.toBuilder().overrideConfiguration(#{any()}.overrideConfiguration()"
                              + ".get().toBuilder().apiCallTimeout(Duration.ofMillis(#{any()})).build()).build())"
                              + ".destination(#{any()}).build())";

            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0), method.getArguments().get(0),
                                        method.getArguments().get(2), method.getArguments().get(1));

            addTmImport("DownloadFileRequest");
            return method;
        }

        private J.MethodInvocation transformDownloadWithBucketKeyFile(J.MethodInvocation method) {
            String v2Method = "#{any()}.downloadFile(DownloadFileRequest.builder()"
                              + ".getObjectRequest(GetObjectRequest.builder().bucket(#{any()}).key(#{any()}).build())"
                              + ".destination(#{any()}).build())";

            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0), method.getArguments().get(1),
                                        method.getArguments().get(2));

            addTmImport("DownloadFileRequest");
            addS3Import("GetObjectRequest");
            return method;
        }

        private J.MethodInvocation transformDownloadWithBucketKeyFileTimeout(J.MethodInvocation method) {
            String v2Method = "#{any()}.downloadFile(DownloadFileRequest.builder()"
                              + ".getObjectRequest(GetObjectRequest.builder().bucket(#{any()}).key(#{any()})"
                              + ".overrideConfiguration(AwsRequestOverrideConfiguration.builder()"
                              + ".apiCallTimeout(Duration.ofMillis(#{any()})).build()).build())"
                              + ".destination(#{any()}).build())";

            method = JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                        method.getArguments().get(0), method.getArguments().get(1),
                                        method.getArguments().get(3), method.getArguments().get(2));

            addTmImport("DownloadFileRequest");
            addS3Import("GetObjectRequest");
            addRequestOverrideConfigImport();
            addDurationImport();
            return method;
        }

        private void addTmImport(String pojoName) {
            String fqcn = V2_TM_MODEL_PKG + pojoName;
            doAfterVisit(new AddImport<>(fqcn, null, false));
        }

        private void addS3Import(String pojoName) {
            String fqcn = V2_S3_MODEL_PKG + pojoName;
            doAfterVisit(new AddImport<>(fqcn, null, false));
        }

        private void addDurationImport() {
            doAfterVisit(new AddImport<>("java.time.Duration", null, false));
        }

        private void addRequestOverrideConfigImport() {
            doAfterVisit(new AddImport<>("software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration", null, false));
        }
    }
}
