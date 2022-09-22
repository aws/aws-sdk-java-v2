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
import static software.amazon.awssdk.transfer.s3.internal.serialization.TransferManagerMarshallingUtils.getObjectSdkField;
import static software.amazon.awssdk.transfer.s3.internal.serialization.TransferManagerMarshallingUtils.getUnmarshaller;

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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class ResumableFileDownloadSerializer {
    private static final Logger log = Logger.loggerFor(S3TransferManager.class);

    private ResumableFileDownloadSerializer() {
    }

    /**
     * Serializes an instance of {@link ResumableFileDownload} into valid JSON. This object contains a nested GetObjectRequest and
     * therefore makes use of the standard JSON marshalling classes.
     */
    public static byte[] toJson(ResumableFileDownload download) {
        JsonWriter jsonGenerator = JsonWriter.create();

        jsonGenerator.writeStartObject();

        TransferManagerJsonMarshaller.LONG.marshall(download.bytesTransferred(), jsonGenerator, "bytesTransferred");
        TransferManagerJsonMarshaller.INSTANT.marshall(download.fileLastModified(), jsonGenerator, "fileLastModified");
        if (download.totalSizeInBytes().isPresent()) {
            TransferManagerJsonMarshaller.LONG.marshall(download.totalSizeInBytes().getAsLong(), jsonGenerator,
                                                        "totalSizeInBytes");
        }
        if (download.s3ObjectLastModified().isPresent()) {
            TransferManagerJsonMarshaller.INSTANT.marshall(download.s3ObjectLastModified().get(),
                                       jsonGenerator,
                                       "s3ObjectLastModified");
        }
        marshallDownloadFileRequest(download.downloadFileRequest(), jsonGenerator);
        jsonGenerator.writeEndObject();

        return jsonGenerator.getBytes();
    }

    /**
     * At this point we do not need to persist the TransferRequestOverrideConfiguration, because it only contains listeners and
     * they are not used in the resume operation.
     */
    private static void marshallDownloadFileRequest(DownloadFileRequest fileRequest, JsonWriter jsonGenerator) {
        jsonGenerator.writeFieldName("downloadFileRequest");
        jsonGenerator.writeStartObject();
        jsonGenerator.writeFieldName("destination");
        jsonGenerator.writeValue(fileRequest.destination().toString());
        marshallGetObjectRequest(fileRequest.getObjectRequest(), jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    private static void marshallGetObjectRequest(GetObjectRequest getObjectRequest, JsonWriter jsonGenerator) {
        jsonGenerator.writeFieldName("getObjectRequest");
        jsonGenerator.writeStartObject();
        validateNoRequestOverrideConfiguration(getObjectRequest);
        getObjectRequest.sdkFields().forEach(field -> marshallPojoField(field, getObjectRequest, jsonGenerator));
        jsonGenerator.writeEndObject();
    }

    private static void validateNoRequestOverrideConfiguration(GetObjectRequest getObjectRequest) {
        if (getObjectRequest.overrideConfiguration().isPresent()) {
            log.debug(() -> "ResumableFileDownload GetObjectRequest contains an override configuration that will not be "
                           + "serialized");
        }
    }

    private static void marshallPojoField(SdkField<?> field, GetObjectRequest request, JsonWriter jsonGenerator) {
        Object val = field.getValueOrDefault(request);
        TransferManagerJsonMarshaller<Object> marshaller = getMarshaller(field.marshallingType(), val);
        marshaller.marshall(val, jsonGenerator, field.locationName());
    }

    public static ResumableFileDownload fromJson(String bytes) {
        JsonNodeParser jsonNodeParser = JsonNodeParser.builder().build();
        Map<String, JsonNode> downloadNodes = jsonNodeParser.parse(bytes).asObject();
        return fromNodes(downloadNodes);
    }

    public static ResumableFileDownload fromJson(byte[] bytes) {
        JsonNodeParser jsonNodeParser = JsonNodeParser.builder().build();
        Map<String, JsonNode> downloadNodes = jsonNodeParser.parse(bytes).asObject();
        return fromNodes(downloadNodes);
    }

    public static ResumableFileDownload fromJson(InputStream bytes) {
        JsonNodeParser jsonNodeParser = JsonNodeParser.builder().build();
        Map<String, JsonNode> downloadNodes = jsonNodeParser.parse(bytes).asObject();
        return fromNodes(downloadNodes);
    }

    @SuppressWarnings("unchecked")
    private static ResumableFileDownload fromNodes(Map<String, JsonNode> downloadNodes) {
        TransferManagerJsonUnmarshaller<Long> longUnmarshaller =
            (TransferManagerJsonUnmarshaller<Long>) getUnmarshaller(MarshallingType.LONG);
        TransferManagerJsonUnmarshaller<Instant> instantUnmarshaller =
            (TransferManagerJsonUnmarshaller<Instant>) getUnmarshaller(MarshallingType.INSTANT);

        ResumableFileDownload.Builder builder = ResumableFileDownload.builder();
        builder.bytesTransferred(longUnmarshaller.unmarshall(downloadNodes.get("bytesTransferred")));
        builder.fileLastModified(instantUnmarshaller.unmarshall(downloadNodes.get("fileLastModified")));
        if (downloadNodes.get("totalSizeInBytes") != null) {
            builder.totalSizeInBytes(longUnmarshaller.unmarshall(downloadNodes.get("totalSizeInBytes")));
        }

        if (downloadNodes.get("s3ObjectLastModified") != null) {
            builder.s3ObjectLastModified(instantUnmarshaller.unmarshall(downloadNodes.get("s3ObjectLastModified")));
        }
        builder.downloadFileRequest(parseDownloadFileRequest(downloadNodes.get("downloadFileRequest")));

        return builder.build();
    }

    private static DownloadFileRequest parseDownloadFileRequest(JsonNode fileRequest) {
        DownloadFileRequest.Builder fileRequestBuilder = DownloadFileRequest.builder();
        Map<String, JsonNode> fileRequestNodes = fileRequest.asObject();

        fileRequestBuilder.destination(Paths.get(fileRequestNodes.get("destination").asString()));

        GetObjectRequest.Builder getObjectBuilder = GetObjectRequest.builder();
        Map<String, JsonNode> getObjectRequestNodes = fileRequestNodes.get("getObjectRequest").asObject();
        getObjectRequestNodes.forEach((key, value) -> setGetObjectParameters(getObjectBuilder, key, value));
        fileRequestBuilder.getObjectRequest(getObjectBuilder.build());

        return fileRequestBuilder.build();
    }

    private static void setGetObjectParameters(GetObjectRequest.Builder getObjectBuilder, String key, JsonNode value) {
        SdkField<?> f = getObjectSdkField(key);
        MarshallingType<?> marshallingType = f.marshallingType();
        TransferManagerJsonUnmarshaller<?> unmarshaller = getUnmarshaller(marshallingType);
        f.set(getObjectBuilder, unmarshaller.unmarshall(value));
    }
}
