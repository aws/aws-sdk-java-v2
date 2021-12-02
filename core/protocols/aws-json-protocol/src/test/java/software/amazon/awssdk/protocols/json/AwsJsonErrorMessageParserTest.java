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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.json.internal.unmarshall.AwsJsonErrorMessageParser;
import software.amazon.awssdk.protocols.json.internal.unmarshall.ErrorMessageParser;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.utils.StringInputStream;

public class AwsJsonErrorMessageParserTest {

    private static final String X_AMZN_ERROR_MESSAGE = "x-amzn-error-message";

    private static final ErrorMessageParser parser = AwsJsonErrorMessageParser.DEFAULT_ERROR_MESSAGE_PARSER;

    private static final String MESSAGE_CONTENT = "boom";

    private SdkHttpFullResponse.Builder responseBuilder;

    private JsonNodeParser jsonParser;

    @Before
    public void setup() {
        jsonParser = JsonNode.parser();
        responseBuilder = ValidSdkObjects.sdkHttpFullResponse();
    }

    @Test
    public void testErrorMessageAt_message() {
        JsonNode jsonNode = parseJson("message", MESSAGE_CONTENT);
        String parsed = parser.parseErrorMessage(responseBuilder.build(), jsonNode);
        assertEquals(MESSAGE_CONTENT, parsed);
    }

    private JsonNode parseJson(String fieldName, String value) {
        return jsonParser.parse(new StringInputStream(String.format("{\"%s\": \"%s\"}", fieldName, value)));
    }

    private JsonNode parseJson(String json) {
        return jsonParser.parse(new StringInputStream(json));
    }

    @Test
    public void testErrorMessageAt_Message() {
        JsonNode jsonNode = parseJson("Message", MESSAGE_CONTENT);
        String parsed = parser.parseErrorMessage(responseBuilder.build(), jsonNode);
        assertEquals(MESSAGE_CONTENT, parsed);
    }

    @Test
    public void testErrorMessageAt_errorMessage() {
        JsonNode jsonNode = parseJson("errorMessage", MESSAGE_CONTENT);
        String parsed = parser.parseErrorMessage(responseBuilder.build(), jsonNode);
        assertEquals(MESSAGE_CONTENT, parsed);
    }

    @Test
    public void testNoErrorMessage_ReturnsNull() {
        String parsed = parser.parseErrorMessage(responseBuilder.build(), parseJson("{}"));
        assertNull(parsed);
    }

    @Test
    public void testErrorMessageIsNumber_ReturnsStringValue() {
        JsonNode jsonNode = parseJson("{\"message\": 1}");
        String parsed = parser.parseErrorMessage(responseBuilder.build(), jsonNode);
        assertEquals("1", parsed);
    }

    @Test
    public void testErrorMessageIsObject_ReturnsNull() {
        JsonNode jsonNode = parseJson("{\"message\": {\"foo\": \"bar\"}}");
        String parsed = parser.parseErrorMessage(responseBuilder.build(), jsonNode);
        assertNull(parsed);
    }

    @Test
    public void testErrorMessageAtMultipleLocations_ReturnsLowerMessage() {
        String randomStuff = UUID.randomUUID().toString();
        String json = String.format("{"
                                    + "   \"%s\": \"%s\","
                                    + "   \"%s\": \"%s\","
                                    + "   \"%s\": \"%s\""
                                    + "}", "message", MESSAGE_CONTENT,
                                    "Message", randomStuff,
                                    "errorMessage", randomStuff);
        String parsed = parser.parseErrorMessage(responseBuilder.build(), parseJson(json));
        assertEquals(MESSAGE_CONTENT, parsed);
    }

    @Test
    public void errorMessageInHeader_ReturnsHeaderValue() {
        responseBuilder.putHeader(X_AMZN_ERROR_MESSAGE, MESSAGE_CONTENT);
        String parsed = parser.parseErrorMessage(responseBuilder.build(), parseJson("{}"));
        assertEquals(MESSAGE_CONTENT, parsed);
    }

    @Test
    public void errorMessageInHeader_ReturnsHeaderValue_CaseInsensitive() {
        responseBuilder.putHeader("x-AMZN-error-message", MESSAGE_CONTENT);
        String parsed = parser.parseErrorMessage(responseBuilder.build(), parseJson("{}"));
        assertEquals(MESSAGE_CONTENT, parsed);
    }

    @Test
    public void errorMessageInHeader_TakesPrecedenceOverMessageInBody() {
        responseBuilder.putHeader(X_AMZN_ERROR_MESSAGE, MESSAGE_CONTENT);
        JsonNode jsonNode = parseJson("message", "other message in body");
        String parsed = parser.parseErrorMessage(responseBuilder.build(), jsonNode);
        assertEquals(MESSAGE_CONTENT, parsed);
    }

}
