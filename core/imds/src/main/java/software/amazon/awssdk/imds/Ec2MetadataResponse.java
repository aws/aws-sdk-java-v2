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

import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.imds.internal.unmarshall.document.DocumentUnmarshaller;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParseException;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * This class is used for response handling and parsing the metadata fetched by the get call in the {@link Ec2MetadataClient}
 * interface. It provides convenience methods to the users to parse the metadata as a String and List. Also provides
 * ways to parse the metadata as Document type if it is in the json format.
 */
@SdkPublicApi
public final class Ec2MetadataResponse {

    private static final JsonNodeParser JSON_NODE_PARSER = JsonNode.parserBuilder().removeErrorLocations(true).build();
    
    private final String body;

    private Ec2MetadataResponse(String body) {
        this.body = Validate.notNull(body, "Metadata is null");
    }

    /**
     * Create a {@link Ec2MetadataResponse} with the given body as its content.
     * @param body the content of the response
     * @return a {@link Ec2MetadataResponse} with the given body as its content.
     */
    public static Ec2MetadataResponse create(String body) {
        return new Ec2MetadataResponse(body);
    }

    /**
     * @return String Representation of the Metadata Response Body.
     */
    public String asString() {
        return body;
    }

    /**
     * Splits the Metadata response body on new line character and returns it as a list.
     * @return The Metadata response split on each line.
     */
    public List<String> asList() {
        return Arrays.asList(body.split("\n"));
    }

    /**
     * Parses the response String into a {@link Document} type. This method can be used for
     * parsing the metadata in a String Json Format.
     * @return Document Representation, as json, of the Metadata Response Body.
     * @throws UncheckedIOException (wrapping a {@link JsonParseException} if the Response body is not of JSON format.
     */
    public Document asDocument() {
        JsonNode node = JSON_NODE_PARSER.parse(body);
        return node.visit(new DocumentUnmarshaller());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Ec2MetadataResponse that = (Ec2MetadataResponse) o;
        return body.equals(that.body);
    }

    @Override
    public int hashCode() {
        return body.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("MetadataResponse")
                       .add("body", body)
                       .build();
    }
}
