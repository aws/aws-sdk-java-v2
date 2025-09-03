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

import java.util.List;
import java.util.regex.Pattern;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class S3PojoToV2 extends Recipe {

    private static final Pattern COMPETE_MPU = Pattern.compile(V1_S3_MODEL_PKG + "CompleteMultipartUploadRequest");
    private static final Pattern OBJECT_TAGGING = Pattern.compile(V1_S3_MODEL_PKG + "ObjectTagging");
    private static final Pattern GET_OBJECT_TAGGING_RESULT = Pattern.compile(V1_S3_MODEL_PKG + "GetObjectTaggingResult");

    @Override
    public String getDisplayName() {
        return "S3 POJOs to V2";
    }

    @Override
    public String getDescription() {
        return "S3 POJOs to V2";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static final class Visitor extends JavaVisitor<ExecutionContext> {

        @Override
        public J visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
            JavaType type = newClass.getType();
            if (!(type instanceof JavaType.FullyQualified)) {
                return newClass;
            }

            if (type.isAssignableFrom(COMPETE_MPU) && newClass.getArguments().size() == 4) {
                addV2S3ModelImport("CompletedMultipartUpload");
                List<Expression> params = newClass.getArguments();
                String v2Builder = "CompleteMultipartUploadRequest.builder().bucket(#{any()}).key(#{any()}).uploadId(#{any()})"
                                   + ".multipartUpload(CompletedMultipartUpload.builder().parts(#{any()}).build()).build()";
                return JavaTemplate.builder(v2Builder)
                                   .build().apply(getCursor(), newClass.getCoordinates().replace(),
                                                  params.get(0), params.get(1), params.get(2),  params.get(3));
            }
            if (type.isAssignableFrom(OBJECT_TAGGING) && newClass.getArguments().size() == 1) {
                String v2Builder = "Tagging.builder().tagSet(#{any()}).build();";
                return JavaTemplate.builder(v2Builder)
                                   .build().apply(getCursor(), newClass.getCoordinates().replace(),
                                                  newClass.getArguments().get(0));
            }
            if (type.isAssignableFrom(GET_OBJECT_TAGGING_RESULT) && newClass.getArguments().size() == 1) {
                String v2Builder = "GetObjectTaggingResponse.builder().tagSet(#{any()}).build();";
                return JavaTemplate.builder(v2Builder)
                                   .build().apply(getCursor(), newClass.getCoordinates().replace(),
                                                  newClass.getArguments().get(0));
            }

            return super.visitNewClass(newClass, executionContext);
        }

        private void addV2S3ModelImport(String className) {
            doAfterVisit(new AddImport<>(V2_S3_MODEL_PKG + className, null, false));
        }
    }
}
