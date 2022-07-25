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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.imds.internal.unmarshall.document.DocumentUnmarshaller;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.utils.Validate;

/**
 * The class is used for Response Handling and Parsing the metadata fetched by the get call in the {@link Ec2Metadata} interface.
 * The class provides convenience methods to the users to parse the metadata as a String, List and Document.
 */
@SdkPublicApi
public class MetadataResponse {

    private static final JsonNodeParser JSON_NODE_PARSER = JsonNode.parserBuilder().removeErrorLocations(true).build();
    
    private final String body;

    public MetadataResponse(String body) {
        this.body = Validate.notNull(body, "Metadata is null");
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
     * Ec2Metadata ec2Metadata = Ec2Metadata.create();
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
     * Ec2Metadata ec2Metadata = Ec2Metadata.create();
     * MetadataResponse metadataResponse = client.get("/latest/meta-data/ancestor-ami-ids");
     * List<String>response = metadataResponse.asList();
     * }
     * </pre>
     */
    public List<String> asList() {
        return Arrays.asList(body.split("\n"));
    }


    /**
     * Parses the response String into {@link Document} type. This method can be used for
     * parsing the metadata in a String Json Format.
     *
     * @return Document Representation of the Metadata Response Body.
     * @throws IOException in case parsing does not happen correctly.
     *
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * Ec2Metadata ec2Metadata = Ec2Metadata.create();
     * MetadataResponse metadataResponse = client.get("/latest/dynamic/instance-identity/document");
     * Document document = metadataResponse.asDocument();
     * }
     * </pre>
     */

    public Document asDocument() throws IOException {

        JsonNode node = JSON_NODE_PARSER.parse(body);
        return node.visit(new DocumentUnmarshaller());
    }


}
