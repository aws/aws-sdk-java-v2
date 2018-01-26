/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.protocol.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Simple struct like class to hold both the raw json string content and it's parsed JsonNode
 */
@SdkInternalApi
@ReviewBeforeRelease("Do we need this? It isn't well encapsulated because of storing non-copied arrays.")
public class JsonContent {

    private static final Logger LOG = LoggerFactory.getLogger(JsonContent.class);

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
            LOG.debug("Unable to read HTTP response content", e);
        }
        return new JsonContent(rawJsonContent, new ObjectMapper(jsonFactory)
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true));
    }

    private static JsonNode parseJsonContent(byte[] rawJsonContent, ObjectMapper mapper) {
        if (rawJsonContent == null || rawJsonContent.length == 0) {
            // Note: behavior of mapper.readTree changed in 2.9 so we need to explicitly
            // check for an empty input and return an empty object or else the return
            // value will be null:
            // https://github.com/FasterXML/jackson-databind/issues/1406
            return mapper.createObjectNode();
        }
        try {
            return mapper.readTree(rawJsonContent);
        } catch (Exception e) {
            LOG.debug("Unable to parse HTTP response content", e);
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
