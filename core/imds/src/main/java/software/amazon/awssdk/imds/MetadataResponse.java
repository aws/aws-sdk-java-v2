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

package software.amazon.awssdk.imds;

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
 * The class is used for Response Handling and Parsing the metadata fetched by the get call in the {@link Ec2Metadata} interface.
 * Metadata is stored in the instance variable <b>body</b>. The class provides convenience methods to the users to parse the
 * metadata as a String, List and to parse the metadata in the JsonResponse according to the key.
 */
@SdkPublicApi
public class MetadataResponse {

    private static final Logger log = LoggerFactory.getLogger(MetadataResponse.class);

    private static final JsonNodeParser JSON_NODE_PARSER = JsonNode.parser();
    
    private final String body;

    public MetadataResponse(String body) {
        this.body = body;
    }

    /**
     * Returns the Metadata Response body as a String. This method can be used for parsing the retrieved
     * singular metadata from IMDS.
     *
     * @return String Representation of the Metadata Response Body.
     *
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * Ec2Metadata ec2Metadata = Ec2Metadata.builder().build();
     * MetadataResponse metadataResponse = client.get("/latest/meta-data/ami-id");
     * String response = metadataResponse.asString();
     *  }
     *  </pre>
     */
    public String asString() {
        return body;

    }

    /**
     * Parses the response String into a list of Strings split by delimiter ("\n"). This method can be used for parsing the
     * list-type metadata from IMDS.
     *
     * @return List Representation of the Metadata Response Body.
     *
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * Ec2Metadata ec2Metadata = Ec2Metadata.builder().build();
     * MetadataResponse metadataResponse = client.get("/latest/meta-data/ancestor-ami-ids");
     * List<String>response = metadataResponse.asList();
     * }
     * </pre>
     */
    public List<String> asList() {

        if (null != body && body.contains("\n"))  {

            return Arrays.asList(body.split("\n"));
        }

        return Collections.emptyList();
    }

    /**
     * Parses the response String to get the String Array Values of the key in jsonResponse. This method can be used for
     * parsing the metadata in a String Json Format.
     *
     * @param key The key / field to retrieve from jsonResponse.
     * @return String Array of the values of the key in the jsonResponse.
     * Returns Null in case the key is null or key doesn't exist in the json.
     *
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * Ec2Metadata ec2Metadata = Ec2Metadata.builder().build();
     * MetadataResponse metadataResponse = client.get("/latest/dynamic/instance-identity/document");
     * String[] devpayProductCodes = metadataResponse.getStringArrayValuesFromJson("devpayProductCodes");
     * }
     * </pre>
     */

    public String[] getStringArrayValuesFromJson(String key) {

        if (null != key) {

            Map<String, JsonNode> jsonNode = JSON_NODE_PARSER.parse(body).asObject();
            return stringArrayValue(jsonNode.get(key));
        }

        return new String[0];

    }

    /**
     * Parses the response String to get the String Value of the key in jsonResponse. This method can be used for
     * parsing the metadata in a String Json Format.
     *
     * @param key The key / field to retrieve from jsonResponse
     * @return Returns the String Value of the key in the jsonResponse .
     *
     * Returns Null in case the key is null or key doesn't exist in the json.
     *
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * Ec2Metadata ec2Metadata = Ec2Metadata.builder().build();
     * MetadataResponse metadataResponse = client.get("/latest/dynamic/instance-identity/document");
     * String region = metadataResponse.getStringArrayValuesFromJson("region");
     * }
     * </pre>
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
