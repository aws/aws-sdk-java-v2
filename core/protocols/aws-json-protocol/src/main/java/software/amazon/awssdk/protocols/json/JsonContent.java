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

package software.amazon.awssdk.protocols.json;

import com.fasterxml.jackson.core.JsonFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.json.internal.dom.JsonDomParser;
import software.amazon.awssdk.protocols.json.internal.dom.SdkJsonNode;
import software.amazon.awssdk.protocols.json.internal.dom.SdkObjectNode;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Simple struct like class to hold both the raw json string content and it's parsed JsonNode
 */
@SdkProtectedApi
//TODO Do we need this? It isn't well encapsulated because of storing non-copied arrays.
public class JsonContent {

    private static final Logger LOG = LoggerFactory.getLogger(JsonContent.class);

    private final byte[] rawContent;
    private final SdkJsonNode jsonNode;

    JsonContent(byte[] rawJsonContent, SdkJsonNode jsonNode) {
        this.rawContent = rawJsonContent;
        this.jsonNode = jsonNode;
    }

    private JsonContent(byte[] rawJsonContent, JsonFactory jsonFactory) {
        this.rawContent = rawJsonContent;
        this.jsonNode = parseJsonContent(rawJsonContent, jsonFactory);
    }

    /**
     * Static factory method to create a JsonContent object from the contents of the HttpResponse
     * provided
     */
    public static JsonContent createJsonContent(SdkHttpFullResponse httpResponse,
                                                JsonFactory jsonFactory) {

        byte[] rawJsonContent = httpResponse.content().map(c -> {
            try {
                return IoUtils.toByteArray(c);
            } catch (IOException e) {
                LOG.debug("Unable to read HTTP response content", e);
            }
            return null;
        }).orElse(null);
        return new JsonContent(rawJsonContent, jsonFactory);
    }

    private static SdkJsonNode parseJsonContent(byte[] rawJsonContent, JsonFactory jsonFactory) {
        if (rawJsonContent == null || rawJsonContent.length == 0) {
            return SdkObjectNode.emptyObject();
        }
        try {
            JsonDomParser parser = JsonDomParser.create(jsonFactory);
            return parser.parse(new ByteArrayInputStream(rawJsonContent));
        } catch (Exception e) {
            LOG.debug("Unable to parse HTTP response content", e);
            return SdkObjectNode.emptyObject();
        }
    }

    public byte[] getRawContent() {
        return rawContent;
    }

    public SdkJsonNode getJsonNode() {
        return jsonNode;
    }
}
