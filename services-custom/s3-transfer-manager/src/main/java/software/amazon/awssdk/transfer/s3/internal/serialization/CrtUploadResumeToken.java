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

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.protocols.jsoncore.JsonWriter;

/**
 * POJO class for the resume token used by CRT
 */
@SdkInternalApi
public final class CrtUploadResumeToken {
    private static final String CRT_S3_REQUEST_TYPE = "AWS_S3_META_REQUEST_TYPE_PUT_OBJECT";
    private static final String TOTAL_NUM_PARTS_FIELD = "total_num_parts";
    private static final String PARTITION_SIZE_FIELD = "partition_size";
    private static final String TYPE_FIELD = "type";
    private static final String MULTIPART_UPLOAD_ID_FIELD = "multipart_upload_id";
    private final Long totalNumOfParts;
    private final Long partSizeInBytes;
    private final String multipartUploadId;

    public CrtUploadResumeToken(Long totalNumOfParts, Long partSizeInBytes, String multipartUploadId) {
        this.totalNumOfParts = totalNumOfParts;
        this.partSizeInBytes = partSizeInBytes;
        this.multipartUploadId = multipartUploadId;
    }

    public Long totalNumOfParts() {
        return totalNumOfParts;
    }

    public Long partSizeInBytes() {
        return partSizeInBytes;
    }

    public String multipartUploadId() {
        return multipartUploadId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CrtUploadResumeToken token = (CrtUploadResumeToken) o;

        if (!Objects.equals(totalNumOfParts, token.totalNumOfParts)) {
            return false;
        }
        if (!Objects.equals(partSizeInBytes, token.partSizeInBytes)) {
            return false;
        }
        return Objects.equals(multipartUploadId, token.multipartUploadId);
    }

    @Override
    public int hashCode() {
        int result = totalNumOfParts != null ? totalNumOfParts.hashCode() : 0;
        result = 31 * result + (partSizeInBytes != null ? partSizeInBytes.hashCode() : 0);
        result = 31 * result + (multipartUploadId != null ? multipartUploadId.hashCode() : 0);
        return result;
    }

    public String marshallResumeToken() {
        JsonWriter jsonGenerator = JsonWriter.create();
        jsonGenerator.writeStartObject();

        TransferManagerJsonMarshaller.LONG.marshall(totalNumOfParts(), jsonGenerator, TOTAL_NUM_PARTS_FIELD);
        TransferManagerJsonMarshaller.LONG.marshall(partSizeInBytes(), jsonGenerator, PARTITION_SIZE_FIELD);
        TransferManagerJsonMarshaller.STRING.marshall(CRT_S3_REQUEST_TYPE, jsonGenerator, TYPE_FIELD);
        TransferManagerJsonMarshaller.STRING.marshall(multipartUploadId(), jsonGenerator, MULTIPART_UPLOAD_ID_FIELD);

        jsonGenerator.writeEndObject();
        return new String(jsonGenerator.getBytes(), StandardCharsets.UTF_8);
    }

    public static CrtUploadResumeToken unmarshallResumeToken(String resumeToken) {
        JsonNodeParser jsonNodeParser = JsonNodeParser.builder().build();
        Map<String, JsonNode> nodes = jsonNodeParser.parse(resumeToken).asObject();

        Long totalNumOfParts = Long.valueOf(nodes.get(TOTAL_NUM_PARTS_FIELD).asNumber());
        Long partitionSize = Long.valueOf(nodes.get(PARTITION_SIZE_FIELD).asNumber());
        String multipartUploadId = nodes.get(MULTIPART_UPLOAD_ID_FIELD).asString();
        return new CrtUploadResumeToken(totalNumOfParts, partitionSize, multipartUploadId);
    }
}
