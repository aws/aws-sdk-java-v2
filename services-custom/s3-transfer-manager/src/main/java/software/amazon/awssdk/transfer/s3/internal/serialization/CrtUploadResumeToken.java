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
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.protocols.jsoncore.JsonWriter;

@SdkInternalApi
public class CrtUploadResumeToken {

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

    public static String marshallResumeToken(CrtUploadResumeToken resumeToken) {
        JsonWriter jsonGenerator = JsonWriter.create();
        jsonGenerator.writeStartObject();

        TransferManagerJsonMarshaller.LONG.marshall(resumeToken.totalNumOfParts(), jsonGenerator, "total_num_parts");
        TransferManagerJsonMarshaller.LONG.marshall(resumeToken.partSizeInBytes(), jsonGenerator, "partition_size");
        TransferManagerJsonMarshaller.STRING.marshall("AWS_S3_META_REQUEST_TYPE_PUT_OBJECT", jsonGenerator, "type");
        TransferManagerJsonMarshaller.STRING.marshall(resumeToken.multipartUploadId(), jsonGenerator, "multipart_upload_id");

        jsonGenerator.writeEndObject();
        return new String(jsonGenerator.getBytes(), StandardCharsets.UTF_8);
    }

    public static CrtUploadResumeToken unmarshallResumeToken(String resumeToken) {
        JsonNodeParser jsonNodeParser = JsonNodeParser.builder().build();
        Map<String, JsonNode> nodes = jsonNodeParser.parse(resumeToken).asObject();

        Long totalNumOfParts = Long.valueOf(nodes.get("total_num_parts").asNumber());
        Long partitionSize = Long.valueOf(nodes.get("partition_size").asNumber());
        String multipartUploadId = nodes.get("multipart_upload_id").asString();
        return new CrtUploadResumeToken(totalNumOfParts, partitionSize, multipartUploadId);
    }
}
