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

package software.amazon.awssdk.transfer.s3.internal.serialization;

import static software.amazon.awssdk.transfer.s3.internal.serialization.TransferManagerMarshallingUtils.getMarshaller;
import static software.amazon.awssdk.transfer.s3.internal.serialization.TransferManagerMarshallingUtils.getUnmarshaller;
import static software.amazon.awssdk.transfer.s3.internal.serialization.TransferManagerMarshallingUtils.putObjectSdkField;

import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.protocols.jsoncore.JsonWriter;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class ResumableFileUploadSerializer {
    private static final Logger log = Logger.loggerFor(S3TransferManager.class);
    private static final String MULTIPART_UPLOAD_ID = "multipartUploadId";
    private static final String FILE_LENGTH = "fileLength";
    private static final String FILE_LAST_MODIFIED = "fileLastModified";
    private static final String PART_SIZE_IN_BYTES = "partSizeInBytes";
    private static final String TOTAL_PARTS = "totalParts";
    private static final String TRANSFERRED_PARTS = "transferredParts";
    private static final String UPLOAD_FILE_REQUEST = "uploadFileRequest";
    private static final String SOURCE = "source";
    private static final String PUT_OBJECT_REQUEST = "putObjectRequest";

    private ResumableFileUploadSerializer() {
    }

    /**
     * Serializes an instance of {@link ResumableFileUpload} into valid JSON. This object contains a nested PutObjectRequest and
     * therefore makes use of the standard JSON marshalling classes.
     */
    public static byte[] toJson(ResumableFileUpload upload) {
        JsonWriter jsonGenerator = JsonWriter.create();

        jsonGenerator.writeStartObject();

        TransferManagerJsonMarshaller.LONG.marshall(upload.fileLength(), jsonGenerator, FILE_LENGTH);
        TransferManagerJsonMarshaller.INSTANT.marshall(upload.fileLastModified(), jsonGenerator, FILE_LAST_MODIFIED);

        if (upload.multipartUploadId().isPresent()) {
            TransferManagerJsonMarshaller.STRING.marshall(upload.multipartUploadId().get(), jsonGenerator,
                                                          MULTIPART_UPLOAD_ID);
        }

        if (upload.partSizeInBytes().isPresent()) {
            TransferManagerJsonMarshaller.LONG.marshall(upload.partSizeInBytes().getAsLong(),
                                                        jsonGenerator,
                                                        PART_SIZE_IN_BYTES);
        }

        if (upload.totalParts().isPresent()) {
            TransferManagerJsonMarshaller.LONG.marshall(upload.totalParts().getAsLong(),
                                                        jsonGenerator,
                                                        TOTAL_PARTS);
        }

        if (upload.transferredParts().isPresent()) {
            TransferManagerJsonMarshaller.LONG.marshall(upload.transferredParts().getAsLong(),
                                                        jsonGenerator,
                                                        TRANSFERRED_PARTS);
        }

        marshallUploadFileRequest(upload.uploadFileRequest(), jsonGenerator);
        jsonGenerator.writeEndObject();

        return jsonGenerator.getBytes();
    }

    /**
     * At this point we do not need to persist the TransferRequestOverrideConfiguration, because it only contains listeners and
     * they are not used in the resume operation.
     */
    private static void marshallUploadFileRequest(UploadFileRequest fileRequest, JsonWriter jsonGenerator) {
        jsonGenerator.writeFieldName(UPLOAD_FILE_REQUEST);
        jsonGenerator.writeStartObject();
        jsonGenerator.writeFieldName(SOURCE);
        jsonGenerator.writeValue(fileRequest.source().toString());
        marshallPutObjectRequest(fileRequest.putObjectRequest(), jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    private static void marshallPutObjectRequest(PutObjectRequest putObjectRequest, JsonWriter jsonGenerator) {
        jsonGenerator.writeFieldName(PUT_OBJECT_REQUEST);
        jsonGenerator.writeStartObject();
        validateNoRequestOverrideConfiguration(putObjectRequest);
        putObjectRequest.sdkFields().forEach(field -> marshallPojoField(field, putObjectRequest, jsonGenerator));
        jsonGenerator.writeEndObject();
    }

    private static void validateNoRequestOverrideConfiguration(PutObjectRequest putObjectRequest) {
        if (putObjectRequest.overrideConfiguration().isPresent()) {
            log.debug(() -> "ResumableFileUpload PutObjectRequest contains an override configuration that will not be "
                            + "serialized");
        }
    }

    private static void marshallPojoField(SdkField<?> field, PutObjectRequest request, JsonWriter jsonGenerator) {
        Object val = field.getValueOrDefault(request);
        TransferManagerJsonMarshaller<Object> marshaller = getMarshaller(field.marshallingType(), val);
        marshaller.marshall(val, jsonGenerator, field.locationName());
    }

    public static ResumableFileUpload fromJson(String bytes) {
        JsonNodeParser jsonNodeParser = JsonNodeParser.builder().build();
        Map<String, JsonNode> uploadNodes = jsonNodeParser.parse(bytes).asObject();
        return fromNodes(uploadNodes);
    }

    public static ResumableFileUpload fromJson(byte[] string) {
        JsonNodeParser jsonNodeParser = JsonNodeParser.builder().build();
        Map<String, JsonNode> uploadNodes = jsonNodeParser.parse(string).asObject();
        return fromNodes(uploadNodes);
    }

    public static ResumableFileUpload fromJson(InputStream inputStream) {
        JsonNodeParser jsonNodeParser = JsonNodeParser.builder().build();
        Map<String, JsonNode> uploadNodes = jsonNodeParser.parse(inputStream).asObject();
        return fromNodes(uploadNodes);
    }

    @SuppressWarnings("unchecked")
    private static ResumableFileUpload fromNodes(Map<String, JsonNode> uploadNodes) {
        TransferManagerJsonUnmarshaller<Long> longUnmarshaller =
            (TransferManagerJsonUnmarshaller<Long>) getUnmarshaller(MarshallingType.LONG);
        TransferManagerJsonUnmarshaller<Instant> instantUnmarshaller =
            (TransferManagerJsonUnmarshaller<Instant>) getUnmarshaller(MarshallingType.INSTANT);
        TransferManagerJsonUnmarshaller<String> stringUnmarshaller =
            (TransferManagerJsonUnmarshaller<String>) getUnmarshaller(MarshallingType.STRING);

        ResumableFileUpload.Builder builder = ResumableFileUpload.builder();
        JsonNode fileLength = Validate.paramNotNull(uploadNodes.get(FILE_LENGTH), FILE_LENGTH);

        builder.fileLength(longUnmarshaller.unmarshall(fileLength));

        JsonNode fileLastModified = Validate.paramNotNull(uploadNodes.get(FILE_LAST_MODIFIED), FILE_LAST_MODIFIED);
        builder.fileLastModified(instantUnmarshaller.unmarshall(fileLastModified));

        if (uploadNodes.get(MULTIPART_UPLOAD_ID) != null) {
            builder.multipartUploadId(stringUnmarshaller.unmarshall(uploadNodes.get(MULTIPART_UPLOAD_ID)));
        }

        if (uploadNodes.get(PART_SIZE_IN_BYTES) != null) {
            builder.partSizeInBytes(longUnmarshaller.unmarshall(uploadNodes.get(PART_SIZE_IN_BYTES)));
        }

        if (uploadNodes.get(TOTAL_PARTS) != null) {
            builder.totalParts(longUnmarshaller.unmarshall(uploadNodes.get(TOTAL_PARTS)));
        }

        if (uploadNodes.get(TRANSFERRED_PARTS) != null) {
            builder.transferredParts(longUnmarshaller.unmarshall(uploadNodes.get(TRANSFERRED_PARTS)));
        }

        JsonNode jsonNode = Validate.paramNotNull(uploadNodes.get(UPLOAD_FILE_REQUEST), UPLOAD_FILE_REQUEST);
        builder.uploadFileRequest(parseUploadFileRequest(jsonNode));

        return builder.build();
    }

    private static UploadFileRequest parseUploadFileRequest(JsonNode fileRequest) {
        UploadFileRequest.Builder fileRequestBuilder = UploadFileRequest.builder();
        Map<String, JsonNode> fileRequestNodes = fileRequest.asObject();

        fileRequestBuilder.source(Paths.get(fileRequestNodes.get(SOURCE).asString()));

        PutObjectRequest.Builder putObjectBuilder = PutObjectRequest.builder();
        Map<String, JsonNode> putObjectRequestNodes = fileRequestNodes.get(PUT_OBJECT_REQUEST).asObject();
        putObjectRequestNodes.forEach((key, value) -> setPutObjectParameters(putObjectBuilder, key, value));
        fileRequestBuilder.putObjectRequest(putObjectBuilder.build());

        return fileRequestBuilder.build();
    }

    private static void setPutObjectParameters(PutObjectRequest.Builder putObjectBuilder, String key, JsonNode value) {
        SdkField<?> f = putObjectSdkField(key);
        MarshallingType<?> marshallingType = f.marshallingType();
        TransferManagerJsonUnmarshaller<?> unmarshaller = getUnmarshaller(marshallingType);
        f.set(putObjectBuilder, unmarshaller.unmarshall(value, f));
    }
}
