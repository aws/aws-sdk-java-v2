/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.protocol.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Simple struct like class to hold both the raw json string content and it's parsed JsonNode
 */
@SdkInternalApi
@ReviewBeforeRelease("Do we need this? It isn't well encapsulated because of storing non-copied arrays.")
public class JsonContent {

    private static final Log LOG = LogFactory.getLog(JsonContent.class);

    private final byte[] rawContent;
    private final JsonNode jsonNode;

    public JsonContent(byte[] rawJsonContent, JsonNode jsonNode) {
        this.rawContent = rawJsonContent;
        this.jsonNode = jsonNode;
    }

    private JsonContent(byte[] rawJsonContent, ObjectMapper mapper) {
        this.rawContent = rawJsonContent;
        this.jsonNode = parseJsonContent(rawJsonContent, mapper);
    }

    /**
     * Static factory method to create a JsonContent object from the contents of the HttpResponse
     * provided
     */
    public static JsonContent createJsonContent(HttpResponse httpResponse,
                                                JsonFactory jsonFactory) {
        byte[] rawJsonContent = null;
        try {
            if (httpResponse.getContent() != null) {
                rawJsonContent = IoUtils.toByteArray(httpResponse.getContent());
            }
        } catch (Exception e) {
            LOG.info("Unable to read HTTP response content", e);
        }
        return new JsonContent(rawJsonContent, new ObjectMapper(jsonFactory)
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true));
    }

    private static JsonNode parseJsonContent(byte[] rawJsonContent, ObjectMapper mapper) {
        if (rawJsonContent == null) {
            return mapper.createObjectNode();
        }
        try {
            return mapper.readTree(rawJsonContent);
        } catch (Exception e) {
            LOG.info("Unable to parse HTTP response content", e);
            return mapper.createObjectNode();
        }
    }

    public byte[] getRawContent() {
        return rawContent;
    }

    public JsonNode getJsonNode() {
        return jsonNode;
    }
}
