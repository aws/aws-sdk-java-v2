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

import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.SUPPORTED_METADATA_TRANSFORMS;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.V2_S3_MODEL_PKG;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.V2_TM_MODEL_PKG;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.addCommentForUnsupportedPutObjectRequestSetter;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.addMetadataFields;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.createComments;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.getArgumentName;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.getSelectName;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.inputStreamBufferingWarningComment;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.isObjectMetadataSetter;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.isPayloadSetter;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.isPutObjectRequestBuilderSetter;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.isPutObjectRequestSetter;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.isRequestMetadataSetter;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.isRequestPayerSetter;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.isUnsupportedPutObjectRequestSetter;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.v2S3MethodMatcher;
import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.v2TmMethodMatcher;
import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.isFileType;
import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.isInputStreamType;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class S3PutObjectRequestToV2 extends Recipe {

    private static final MethodMatcher PUT_OBJECT_STREAM_METADATA =
        v2S3MethodMatcher(String.format("putObject(String, String, java.io.InputStream, %sHeadObjectResponse)",
                                        V2_S3_MODEL_PKG));

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

    private static final class Visitor extends JavaVisitor<ExecutionContext> {

        // queues for when setter is on PutObjectRequest that is instantiated directly inside putObject(), no assigned variable
        private final Queue<Expression> payloadQueue = new ArrayDeque<>();
        private final Queue<String> metadataNameQueue = new ArrayDeque<>();

        private final Map<String, Expression> requestToPayloadMap = new HashMap<>();
        private final Map<String, String> requestToMetadataNamesMap = new HashMap<>();
        private final Map<String, Map<String, Expression>> metadataMap = new HashMap<>();

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            if (isObjectMetadataSetter(method)) {
                return saveMetadataValueAndRemoveStatement(method);
            }

            if (isPutObjectRequestBuilderSetter(method)) {
                if (isRequestMetadataSetter(method)) {
                    method = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                    return transformWithMetadata(method);
                }
                if (isPayloadSetter(method)) {
                    return transformRequestBuilderPayloadSetter(method);
                }
                if (isRequestPayerSetter(method)) {
                    return transformWithRequesterPays(method);
                }
                if (isUnsupportedPutObjectRequestSetter(method)) {
                    method = addCommentForUnsupportedPutObjectRequestSetter(method);
                    return super.visitMethodInvocation(method, ctx);
                }
            }
            if (isPutObjectRequestSetter(method)) {
                if (isRequestMetadataSetter(method)) {
                    return convertSetMetadataToBuilder(method);
                }
                if (isPayloadSetter(method)) {
                    return transformRequestPayloadSetter(method);
                }
            }

            if (PUT_OBJECT_STREAM_METADATA.matches(method)) {
                method = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                return transformPutObjectWithStreamAndMetadata(method);
            }
            if (PUT_OBJ_WITH_REQUEST.matches(method)) {
                method = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                return transformPutObjectWithRequest(method);
            }
            if (UPLOAD_WITH_REQUEST.matches(method)) {
                method = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                return transformUploadWithRequest(method);
            }

            return super.visitMethodInvocation(method, ctx);
        }

        private J.MethodInvocation transformRequestBuilderPayloadSetter(J.MethodInvocation method) {
            Expression payload = method.getArguments().get(0);

            String variableName = retrieveVariableNameIfRequestPojoIsAssigned();
            if (variableName != null) {
                requestToPayloadMap.put(variableName, payload);
            } else {
                payloadQueue.add(payload);
            }

            // Remove setter .file(file)
            return (J.MethodInvocation) method.getSelect();
        }

        private J.MethodInvocation transformRequestPayloadSetter(J.MethodInvocation method) {
            J.Identifier requestPojo = (J.Identifier) method.getSelect();
            String variableName = requestPojo.getSimpleName();

            Expression payload = method.getArguments().get(0);
            requestToPayloadMap.put(variableName, payload);

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

        private J.MethodInvocation transformPutObjectWithRequest(J.MethodInvocation method) {
            Expression payload = retrieveRequestPayload(method);

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

        private Expression retrieveRequestPayload(J.MethodInvocation method) {
            Expression requestPojo = method.getArguments().get(0);
            Expression payload;

            if (requestPojo instanceof J.Identifier) {
                J.Identifier pojo = (J.Identifier) requestPojo;
                payload = requestToPayloadMap.get(pojo.getSimpleName());
            } else {
                payload = payloadQueue.poll();
            }

            return payload;
        }

        private J.MethodInvocation transformUploadWithRequest(J.MethodInvocation method) {
            Expression payload = retrieveRequestPayload(method);

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
            addTmImport("UploadRequest");
            addAsyncRequestBodyImport();

            StringBuilder sb = new StringBuilder("UploadRequest.builder().putObjectRequest(#{any()})"
                                                 + ".requestBody(AsyncRequestBody.fromInputStream(#{any()}, ");

            String metadataName = getMetadataForRequest(method);
            Expression contentLength;
            if (metadataName == null) {
                contentLength = null;
            } else {
                contentLength = retrieveContentLengthForMetadataIfSet(metadataName);
            }

            Expression[] params = {method.getArguments().get(0), inputStream};
            if (contentLength == null) {
                sb.append("-1L");
            } else {
                sb.append("#{any()}");
                params = Arrays.copyOf(params, 3);
                params[2] = contentLength;
            }

            sb.append(", newExecutorServiceVariableToDefine)).build()");

            String comment = "When using InputStream to upload with TransferManager, you must specify Content-Length and "
                             + "ExecutorService.";

            return JavaTemplate.builder(sb.toString()).build()
                                                     .apply(getCursor(), method.getCoordinates().replaceArguments(), params)
                               .withComments(createComments(comment));
        }

        private J.MethodInvocation addFileToPutObject(J.MethodInvocation method, Expression file) {
            String v2Method = "#{any()}, RequestBody.fromFile(#{any()})";
            return JavaTemplate.builder(v2Method).build()
                                 .apply(getCursor(), method.getCoordinates().replaceArguments(),
                                        method.getArguments().get(0), file);
        }

        private J.MethodInvocation addInputStreamToPutObject(J.MethodInvocation method, Expression inputStream) {
            String metadataName = getMetadataForRequest(method);
            Expression contentLen;
            if (metadataName == null) {
                contentLen = null;
            } else {
                contentLen = retrieveContentLengthForMetadataIfSet(metadataName);
            }

            String v2Method;

            if (contentLen == null) {
                // CHECKSTYLE:OFF: Regexp
                v2Method = "#{any()}, RequestBody.fromContentProvider(() -> #{any()}, \"application/octet-stream\")";
                // CHECKSTYLE:ON: Regexp
                return JavaTemplate.builder(v2Method).build()
                                   .apply(getCursor(), method.getCoordinates().replaceArguments(),
                                          method.getArguments().get(0), inputStream)
                    .withComments(inputStreamBufferingWarningComment());
            }

            StringBuilder sb = new StringBuilder("#{any()}, RequestBody.fromInputStream(#{any()}, #{any()}");
            if (contentLen instanceof J.Literal) {
                sb.append("L");
            }
            sb.append(")");

            return JavaTemplate.builder(sb.toString()).build()
                               .apply(getCursor(), method.getCoordinates().replaceArguments(),
                                      method.getArguments().get(0), inputStream, contentLen);
        }

        private J.MethodInvocation transformPutObjectWithStreamAndMetadata(J.MethodInvocation method) {
            addRequestBodyImport();
            addS3Import("PutObjectRequest");

            Expression metadata = method.getArguments().get(3);
            String metadataName = ((J.Identifier) metadata).getSimpleName();

            StringBuilder sb = new StringBuilder("PutObjectRequest.builder().bucket(#{any()}).key(#{any()})");
            addMetadataFields(sb, metadataName, metadataMap);
            Expression contentLen = retrieveContentLengthForMetadataIfSet(metadataName);

            Expression[] params = {method.getArguments().get(0), method.getArguments().get(1),
                                   method.getArguments().get(2)};

            if (contentLen == null) {
                // CHECKSTYLE:OFF: Regexp
                sb.append(".build(), RequestBody.fromContentProvider(() -> #{any()}, \"application/octet-stream\")");
                // CHECKSTYLE:ON: Regexp
                return JavaTemplate.builder(sb.toString()).build()
                                   .apply(getCursor(), method.getCoordinates().replaceArguments(), params)
                                   .withComments(inputStreamBufferingWarningComment());
            }

            sb.append(".build(), RequestBody.fromInputStream(#{any()}, #{any()}");

            if (contentLen instanceof J.Literal) {
                sb.append("L");
            }
            sb.append(")");

            params = Arrays.copyOf(params, 4);
            params[3] = contentLen;

            return JavaTemplate.builder(sb.toString()).build()
                               .apply(getCursor(), method.getCoordinates().replaceArguments(), params);
        }

        private J.MethodInvocation transformWithMetadata(J.MethodInvocation method) {
            String metadataName = getArgumentName(method);

            String requestName = retrieveVariableNameIfRequestPojoIsAssigned();
            if (requestName == null) {
                metadataNameQueue.add(metadataName);
            } else {
                requestToMetadataNamesMap.put(requestName, metadataName);
            }

            Map<String, Expression> map = metadataMap.get(metadataName);
            if (map == null) {
                // should never happen unless user passes in empty ObjectMetadata
                // remove metadata setter
                return (J.MethodInvocation) method.getSelect();
            }

            StringBuilder sb = new StringBuilder("#{any()}");
            addMetadataFields(sb, metadataName, metadataMap);

            return JavaTemplate.builder(sb.toString()).build()
                               .apply(getCursor(), method.getCoordinates().replace(), method.getSelect());
        }

        private J convertSetMetadataToBuilder(J.MethodInvocation method) {
            String requestName = getSelectName(method);
            String metadataName = getArgumentName(method);

            requestToMetadataNamesMap.put(requestName, metadataName);
            Map<String, Expression> map = metadataMap.get(metadataName);
            if (map == null) {
                // should never happen unless user passes in empty ObjectMetadata
                // remove entire line
                return null;
            }

            StringBuilder sb = new StringBuilder("#{any()} = #{any()}.toBuilder()");
            addMetadataFields(sb, metadataName, metadataMap);
            sb.append(".build()");

            // This class must be JavaVisitor instead of JavaIsoVisitor in order to return a Statement here
            return JavaTemplate.builder(sb.toString()).build()
                               .apply(getCursor(), method.getCoordinates().replace(), method.getSelect(), method.getSelect());
        }

        private String getMetadataForRequest(J.MethodInvocation method) {
            Expression arg = method.getArguments().get(0);

            if (arg instanceof J.Identifier) {
                String requestName = ((J.Identifier) arg).getSimpleName();
                return requestToMetadataNamesMap.get(requestName);
            }
            return metadataNameQueue.poll();
        }

        private Expression retrieveContentLengthForMetadataIfSet(String metadataName) {
            Map<String, Expression> map = metadataMap.get(metadataName);
            if (map == null) {
                return null;
            }
            return map.get("contentLength");
        }

        private J.MethodInvocation saveMetadataValueAndRemoveStatement(J.MethodInvocation method) {
            J.Identifier metadataPojo = (J.Identifier) method.getSelect();
            String variableName = metadataPojo.getSimpleName();

            String methodName = method.getSimpleName();

            if (!SUPPORTED_METADATA_TRANSFORMS.contains(methodName)) {
                String comment = String.format("Transform for ObjectMetadata setter - %s - is not supported, please manually "
                                               + "migrate the code by setting it on the v2 request/response object.", methodName);
                return method.withComments(createComments(comment));
            }

            Expression value = method.getArguments().get(0);

            Map<String, Expression> map = metadataMap.get(variableName);

            if (map == null) {
                map = new HashMap<>();
            }

            map.put(methodName, value);
            metadataMap.put(variableName, map);

            // remove entire line
            return null;
        }

        private J.MethodInvocation transformWithRequesterPays(J.MethodInvocation method) {
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
