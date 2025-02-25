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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.RemoveImport;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class TransferManagerToV2 extends Recipe {

    private static final Map<String, String> CLASS_MAPPINGS = new HashMap<>();
    private static final String V1_PREFIX = "com.amazonaws.services.s3.transfer.";
    private static final String V2_PREFIX = "software.amazon.awssdk.transfer.s3.";

    static {
        CLASS_MAPPINGS.put(v1Fcqn("TransferManager"), V2_PREFIX + "S3TransferManager");
        CLASS_MAPPINGS.put(v1Fcqn("TransferManagerBuilder"), V2_PREFIX + "S3TransferManager");
        CLASS_MAPPINGS.put(v1Fcqn("Transfer"), v2ModelFqcn("Transfer"));
        CLASS_MAPPINGS.put(v1Fcqn("AbortableTransfer"), v2ModelFqcn("Transfer"));
        CLASS_MAPPINGS.put(v1Fcqn("Copy"), v2ModelFqcn("Copy"));
        CLASS_MAPPINGS.put(v1Fcqn("Download"), v2ModelFqcn("Download"));
        // TODO - v2 has Upload AND FileUpload
        CLASS_MAPPINGS.put(v1Fcqn("Upload"), v2ModelFqcn("Upload"));
        CLASS_MAPPINGS.put(v1Fcqn("MultipleFileDownload"), v2ModelFqcn("DirectoryDownload"));
        CLASS_MAPPINGS.put(v1Fcqn("MultipleFileUpload"), v2ModelFqcn("DirectoryUpload"));
        CLASS_MAPPINGS.put(v1Fcqn("PersistableDownload"), v2ModelFqcn("ResumableFileDownload"));
        CLASS_MAPPINGS.put(v1Fcqn("PersistableTransfer"), v2ModelFqcn("ResumableTransfer"));
        CLASS_MAPPINGS.put(v1Fcqn("PersistableUpload"), v2ModelFqcn("ResumableFileUpload"));
        CLASS_MAPPINGS.put(v1Fcqn("PauseResult"), v2ModelFqcn("ResumableFileUpload"));
        CLASS_MAPPINGS.put(V1_PREFIX + "model.CopyResult", v2ModelFqcn("CompletedCopy"));
        CLASS_MAPPINGS.put(V1_PREFIX + "model.UploadResult", v2ModelFqcn("CompletedUpload"));
        CLASS_MAPPINGS.put(v1Fcqn("KeyFilter"), v2ConfigFqcn("DownloadFilter"));
        CLASS_MAPPINGS.put(v1Fcqn("TransferProgress"), v2ProgressFqcn("TransferProgress"));
        CLASS_MAPPINGS.put(v1Fcqn("TransferManagerConfiguration"), v2S3MultipartFqcn("MultipartConfiguration"));
    }

    private static String v1Fcqn(String name) {
        return V1_PREFIX + name;
    }

    private static String v2ModelFqcn(String name) {
        return V2_PREFIX + "model." + name;
    }

    private static String v2ProgressFqcn(String name) {
        return V2_PREFIX + "progress." + name;
    }

    private static String v2ConfigFqcn(String name) {
        return V2_PREFIX + "config." + name;
    }

    private static String v2S3MultipartFqcn(String name) {
        return "software.amazon.awssdk.services.s3.multipart." + name;
    }

    @Override
    public String getDisplayName() {
        return "Change AWS SDK for Java v1 Transfer Manager imports to v2 equivalents.";
    }

    @Override
    public String getDescription() {
        return "Change AWS SDK for Java v1 Transfer Manager imports to v2 equivalents.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static class Visitor extends JavaVisitor<ExecutionContext> {

        private static boolean isTmClass(String fullName) {
            return CLASS_MAPPINGS.containsKey(fullName);
        }

        @Override
        public J visitImport(J.Import anImport, ExecutionContext ctx) {
            JavaType.FullyQualified fullyQualified =
                Optional.ofNullable(anImport.getQualid())
                        .map(J.FieldAccess::getType)
                        .map(TypeUtils::asFullyQualified)
                        .orElse(null);

            if (fullyQualified != null) {
                String fqcn = fullyQualified.getFullyQualifiedName();
                if (isTmClass(fqcn)) {
                    doAfterVisit(new AddImport<>(CLASS_MAPPINGS.get(fqcn), null, false));
                    doAfterVisit(new RemoveImport<>(fqcn, true));
                }
            }
            return anImport;
        }
    }
}
