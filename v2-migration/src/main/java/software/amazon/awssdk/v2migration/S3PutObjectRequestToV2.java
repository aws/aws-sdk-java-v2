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
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.v2S3MethodMatcher;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.v2TmMethodMatcher;
import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.isFileType;
import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.isInputStreamType;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class S3PutObjectRequestToV2 extends Recipe {

    private static final MethodMatcher PUT_OBJ_WITH_REQUEST =
        v2S3MethodMatcher(String.format("putObject(%sPutObjectRequest)", V2_S3_MODEL_PKG));

    private static final MethodMatcher UPLOAD_WITH_REQUEST =
        v2TmMethodMatcher(String.format("upload(%sPutObjectRequest)", V2_S3_MODEL_PKG));

    @Override
    public String getDisplayName() {
        return "V1 S3 PutObjectRequest, AmazonS3.putObject(PutObjectRequest), and TransferManager.upload(PutObjectRequest) to V2";
    }

    @Override
    public String getDescription() {
        return "Transform V1 S3 PutObjectRequest to V2, as well as methods that take it as an argument.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static final class Visitor extends JavaIsoVisitor<ExecutionContext> {

        private Queue<Expression> filesQueue = new ArrayDeque<>();

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {

            if (isPutObjectRequestBuilderSetter(method)) {
                if (isPayloadSetter(method)) {
                    return transformRequestBuilderPayloadSetter(method, executionContext);
                }
                if (isRequestPayerSetter(method)) {
                    return transformRequesterPaysSetter(method);
                }
            }
            if (isPutObjectRequestSetter(method)) {
                if (isPayloadSetter(method)) {
                    return transformRequestPayloadSetter(method, executionContext);
                }
            }

            if (PUT_OBJ_WITH_REQUEST.matches(method)) {
                method = super.visitMethodInvocation(method, executionContext);
                method = transformPutObjectWithRequest(method, executionContext);
                return method;
            }
            if (UPLOAD_WITH_REQUEST.matches(method)) {
                method = super.visitMethodInvocation(method, executionContext);
                method = transformUploadWithRequest(method, executionContext);
                return method;
            }

            return super.visitMethodInvocation(method, executionContext);
        }

        private J.MethodInvocation transformRequestBuilderPayloadSetter(J.MethodInvocation method,
                                                                        ExecutionContext executionContext) {
            Expression payload = method.getArguments().get(0);

            String variableName = retrieveVariableNameIfRequestPojoIsAssigned();
            if (variableName != null) {
                executionContext.putMessage(variableName, payload);
            } else {
                filesQueue.add(payload);
            }

            // Remove setter .file(file)
            return (J.MethodInvocation) method.getSelect();
        }

        private J.MethodInvocation transformRequestPayloadSetter(J.MethodInvocation method, ExecutionContext executionContext) {
            J.Identifier requestPojo = (J.Identifier) method.getSelect();
            String variableName = requestPojo.getSimpleName();

            Expression payload = method.getArguments().get(0);
            executionContext.putMessage(variableName, payload);

            // Remove entire statement e.g., request.setFile(file)
            return null;
        }

        /**
         * Request POJO may be assigned to a variable, or instantiated directly in putObject() API
         */
        private String retrieveVariableNameIfRequestPojoIsAssigned() {
            J parent = getCursor()
                .dropParentUntil(p -> p instanceof J.VariableDeclarations.NamedVariable || p instanceof J.Block)
                .getValue();

            if (parent instanceof J.VariableDeclarations.NamedVariable) {
                J.VariableDeclarations.NamedVariable namedVariable = (J.VariableDeclarations.NamedVariable) parent;
                return namedVariable.getSimpleName();
            }
            return null;
        }

        private J.MethodInvocation transformPutObjectWithRequest(J.MethodInvocation method, ExecutionContext executionContext) {
            Expression payload = retrieveRequestPayload(method, executionContext);

            if (payload == null) {
                method = addEmptyRequestBodyToPutObject(method);
            } else if (isFileType(payload.getType())) {
                method = addFileToPutObject(method, payload);
            } else if (isInputStreamType(payload.getType())) {
                method = addInputStreamToPutObject(method, payload);
            }

            addRequestBodyImport();
            return method;
        }

        private Expression retrieveRequestPayload(J.MethodInvocation method, ExecutionContext executionContext) {
            Expression requestPojo = method.getArguments().get(0);
            Expression payload;

            if (requestPojo instanceof J.Identifier) {
                J.Identifier pojo = (J.Identifier) requestPojo;
                payload = executionContext.pollMessage(pojo.getSimpleName());
            } else {
                payload = filesQueue.poll();
            }

            return payload;
        }

        private J.MethodInvocation transformUploadWithRequest(J.MethodInvocation method, ExecutionContext executionContext) {
            Expression payload = retrieveRequestPayload(method, executionContext);

            if (payload == null) {
                method = addEmptyAsyncRequestBodyToUpload(method);
            } else if (isFileType(payload.getType())) {
                method = addFileAndChangeMethodToUploadFile(method, payload);
            } else if (isInputStreamType(payload.getType())) {
                method = addInputStreamToUpload(method, payload);
            }

            return method;
        }

        private J.MethodInvocation addEmptyAsyncRequestBodyToUpload(J.MethodInvocation method) {
            String v2Method = "UploadRequest.builder().putObjectRequest(#{any()}).requestBody(AsyncRequestBody.empty()).build()";
            addTmImport("UploadRequest");
            addAsyncRequestBodyImport();
            return JavaTemplate.builder(v2Method).build()
                               .apply(getCursor(), method.getCoordinates().replaceArguments(), method.getArguments().get(0));
        }

        private J.MethodInvocation addEmptyRequestBodyToPutObject(J.MethodInvocation method) {
            String v2Method = "#{any()}, RequestBody.empty()";
            return JavaTemplate.builder(v2Method).build()
                               .apply(getCursor(), method.getCoordinates().replaceArguments(), method.getArguments().get(0));
        }

        private J.MethodInvocation addFileAndChangeMethodToUploadFile(J.MethodInvocation method, Expression file) {
            String v2Method = "#{any()}.uploadFile(UploadFileRequest.builder()"
                              + ".putObjectRequest(#{any()}).source(#{any()}).build())";
            addTmImport("UploadFileRequest");
            return JavaTemplate.builder(v2Method).build()
                               .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(),
                                      method.getArguments().get(0), file);
        }

        private J.MethodInvocation addInputStreamToUpload(J.MethodInvocation method, Expression inputStream) {
            long contentLength = extractContentLengthIfSet(method);

            String v2Method = String.format("UploadRequest.builder().putObjectRequest(#{any()})"
                                            + ".requestBody(AsyncRequestBody.fromInputStream(#{any()}, %dL, "
                                            + "newExecutorServiceVariableToDefine)).build()",
                                            contentLength);

            addTmImport("UploadRequest");
            addAsyncRequestBodyImport();

            String comment = "When using InputStream to upload with TransferManager, you must specify Content-Length and "
                             + "ExecutorService.";

            return JavaTemplate.builder(v2Method).build()
                                                     .apply(getCursor(), method.getCoordinates().replaceArguments(),
                                                            method.getArguments().get(0), inputStream)
                               .withComments(createComments(comment));
        }

        private J.MethodInvocation addFileToPutObject(J.MethodInvocation method, Expression file) {
            String v2Method = "#{any()}, RequestBody.fromFile(#{any()})";
            return JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replaceArguments(),
                                        method.getArguments().get(0), file);
        }

        private J.MethodInvocation addInputStreamToPutObject(J.MethodInvocation method, Expression inputStream) {
            long contentLength = extractContentLengthIfSet(method);
            String v2Method;

            if (contentLength < 0) {
                String comment = "When using InputStream to upload with S3Client, Content-Length should be specified and used "
                                 + "with RequestBody.fromInputStream(). Otherwise, the entire stream will be buffered in memory.";
                // CHECKSTYLE:OFF: Regexp
                v2Method = "#{any()}, RequestBody.fromContentProvider(() -> #{any()}, \"binary/octet-stream\")";
                // CHECKSTYLE:ON: Regexp
                return JavaTemplate.builder(v2Method).build()
                                   .apply(getCursor(), method.getCoordinates().replaceArguments(),
                                          method.getArguments().get(0), inputStream)
                    .withComments(createComments(comment));
            } else {
                v2Method = String.format("#{any()}, RequestBody.fromInputStream(#{any()}, %d)", contentLength);
                return JavaTemplate.builder(v2Method).build()
                                   .apply(getCursor(), method.getCoordinates().replaceArguments(),
                                          method.getArguments().get(0), inputStream);
            }
        }

        private long extractContentLengthIfSet(J.MethodInvocation method) {
            // TODO - check ObjectMetadata and return if set
            // Need to implement ObjectMetadata transform

            return -1;
        }

        private J.MethodInvocation transformRequesterPaysSetter(J.MethodInvocation method) {
            Expression expression = method.getArguments().get(0);
            if (expression instanceof J.Literal) {
                J.Literal literal = (J.Literal) expression;
                if (Boolean.TRUE.equals(literal.getValue())) {
                    addS3Import("RequestPayer");
                } else {
                    // Removes method - there is no enum value for false
                    return (J.MethodInvocation) method.getSelect();
                }
            }

            return JavaTemplate.builder("RequestPayer.REQUESTER").build()
                               .apply(getCursor(), method.getCoordinates().replaceArguments());
        }

        private List<Comment> createComments(String comment) {
            return Collections.singletonList(
                new TextComment(true, "AWS SDK for Java v2 migration: " + comment, "", Markers.EMPTY));
        }

        /** Field set during POJO instantiation, e.g.,
         * PutObjectRequest request = new PutObjectRequest("bucket" "key", "redirectLocation").withFile(file);
         */
        private boolean isPutObjectRequestBuilderSetter(J.MethodInvocation method) {
            return isSetterForClassType(method, "software.amazon.awssdk.services.s3.model.PutObjectRequest$Builder");
        }

        /** Field set after POJO instantiation, e.g.,
         * PutObjectRequest request = new PutObjectRequest("bucket" "key", "redirectLocation");
         * request.setFile(file);
         */
        private boolean isPutObjectRequestSetter(J.MethodInvocation method) {
            return isSetterForClassType(method, "software.amazon.awssdk.services.s3.model.PutObjectRequest");
        }

        private boolean isSetterForClassType(J.MethodInvocation method, String fqcn) {
            if (method.getSelect() == null || method.getSelect().getType() == null) {
                return false;
            }
            return TypeUtils.isOfClassType(method.getSelect().getType(), fqcn);
        }

        private boolean isPayloadSetter(J.MethodInvocation method) {
            return "file".equals(method.getSimpleName()) || "inputStream".equals(method.getSimpleName());
        }

        private boolean isRequestPayerSetter(J.MethodInvocation method) {
            return "requestPayer".equals(method.getSimpleName());
        }

        private void addRequestBodyImport() {
            String fqcn = "software.amazon.awssdk.core.sync.RequestBody";
            doAfterVisit(new AddImport<>(fqcn, null, false));
        }

        private void addAsyncRequestBodyImport() {
            String fqcn = "software.amazon.awssdk.core.async.AsyncRequestBody";
            doAfterVisit(new AddImport<>(fqcn, null, false));
        }

        private void addS3Import(String pojoName) {
            String fqcn = V2_S3_MODEL_PKG + pojoName;
            doAfterVisit(new AddImport<>(fqcn, null, false));
        }

        private void addTmImport(String pojoName) {
            String fqcn = V2_TM_MODEL_PKG + pojoName;
            doAfterVisit(new AddImport<>(fqcn, null, false));
        }
    }
}
