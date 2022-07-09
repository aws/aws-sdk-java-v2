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

package software.amazon.awssdk.imds.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;

/**
 * The class is used for Response Handling and Parsing.
 */
@SdkPublicApi
public class MetadataResponse {

    private static final Logger log = LoggerFactory.getLogger(MetadataResponse.class);

    private static final JsonNodeParser JSON_NODE_PARSER = JsonNode.parser();
    
    String body;

    public MetadataResponse(String body) {
        this.body = body;
    }

    public String asString() {
        return body;
    }

    /**
     * Method to parse the json String into a list of Strings
     * @return List obtained by splitting the json on "\n"
     */
    public List<String> asList() {

        if (null != body && body.contains("\n"))  {

            return Arrays.asList(body.split("\n"));
        }

        return Collections.emptyList();
    }

    /**
     * Method to get the String Array Values of the key in jsonResponse
     * @param key The key / field to retrieve from jsonResponse
     * @return String Array of the values of the key in the jsonResponse
     */
    public String[] getStringArrayValuesFromJson(String key) {

        if (null != key) {

            Map<String, JsonNode> jsonNode = JSON_NODE_PARSER.parse(body).asObject();
            return stringArrayValue(jsonNode.get(key));
        }

        return new String[0];

    }

    /**
     * Method to get the String Array Values of the key in jsonResponse
     * @param key The key / field to retrieve from jsonResponse
     * @return String value of the key in the jsonResponse
     */
    public String getStringValueFromJson(String key) {

        if (null != key) {

            Map<String, JsonNode> jsonNode = JSON_NODE_PARSER.parse(body).asObject();
            return stringValue(jsonNode.get(key));

        }

        return null;
    }

    private String[] stringArrayValue(JsonNode jsonNode) {

        if (null != jsonNode && jsonNode.isArray()) {
            try {
                return jsonNode.asArray()
                               .stream()
                               .filter(JsonNode::isString)
                               .map(JsonNode::asString)
                               .toArray(String[]::new);
            } catch (Exception e) {
                log.warn("Unable to parse jsonNode instance info : " + e.getMessage(), e);
            }
        }
        return new String[0];
    }

    private String stringValue(JsonNode jsonNode) {

        if (null != jsonNode && jsonNode.isString()) {
            try {
                return jsonNode.asString();
            } catch (Exception e) {
                log.warn("Unable to parse jsonNode instance info : " + e.getMessage(), e);
            }
        }
        return null;
    }
}
