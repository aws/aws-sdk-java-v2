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

package software.amazon.awssdk.awscore.protocol.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.awscore.client.utils.ValidSdkObjects;
import software.amazon.awssdk.core.internal.protocol.json.ErrorMessageParser;
import software.amazon.awssdk.http.SdkHttpFullResponse;

public class AwsJsonErrorMessageParserTest {

    private static final String X_AMZN_ERROR_MESSAGE = "x-amzn-error-message";

    private static final ErrorMessageParser parser = AwsJsonErrorMessageParser.DEFAULT_ERROR_MESSAGE_PARSER;

    private static final String MESSAGE_CONTENT = "boom";

    private SdkHttpFullResponse.Builder responseBuilder;

    private ObjectNode jsonNode;

    @Before
    public void setup() {
        jsonNode = JsonNodeFactory.instance.objectNode();
        responseBuilder = ValidSdkObjects.sdkHttpFullResponse();
    }

    @Test
    public void testErrorMessageAt_message() {
        jsonNode.put("message", MESSAGE_CONTENT);
        String parsed = parser.parseErrorMessage(responseBuilder.build(), jsonNode);
        assertEquals(MESSAGE_CONTENT, parsed);
    }

    @Test
    public void testErrorMessageAt_Message() {
        jsonNode.put("Message", MESSAGE_CONTENT);
        String parsed = parser.parseErrorMessage(responseBuilder.build(), jsonNode);
        assertEquals(MESSAGE_CONTENT, parsed);
    }

    @Test
    public void testErrorMessageAt_errorMessage() {
        jsonNode.put("errorMessage", MESSAGE_CONTENT);
        String parsed = parser.parseErrorMessage(responseBuilder.build(), jsonNode);
        assertEquals(MESSAGE_CONTENT, parsed);
    }

    @Test
    public void testNoErrorMessage_ReturnsNull() {
        String parsed = parser.parseErrorMessage(responseBuilder.build(), jsonNode);
        assertNull(parsed);
    }

    @Test
    public void testErrorMessageIsNumber_ReturnsNull() {
        jsonNode.put("message", 1);
        String parsed = parser.parseErrorMessage(responseBuilder.build(), jsonNode);
        assertNull(parsed);
    }

    @Test
    public void testErrorMessageIsObject_ReturnsNull() {
        jsonNode.set("message", JsonNodeFactory.instance.objectNode().put("foo", "bar"));
        String parsed = parser.parseErrorMessage(responseBuilder.build(), jsonNode);
        assertNull(parsed);
    }

    @Test
    public void testErrorMessageAtMultipleLocations_ReturnsLowerMessage() {
        jsonNode.put("message", MESSAGE_CONTENT);
        String randomStuff = UUID.randomUUID().toString();
        jsonNode.put("Message", randomStuff);
        jsonNode.put("errorMessage", randomStuff);
        String parsed = parser.parseErrorMessage(responseBuilder.build(), jsonNode);
        assertEquals(MESSAGE_CONTENT, parsed);
    }

    @Test
    public void errorMessageInHeader_ReturnsHeaderValue() {
        responseBuilder.putHeader(X_AMZN_ERROR_MESSAGE, MESSAGE_CONTENT);
        String parsed = parser.parseErrorMessage(responseBuilder.build(), jsonNode);
        assertEquals(MESSAGE_CONTENT, parsed);
    }

    @Test
    public void errorMessageInHeader_ReturnsHeaderValue_CaseInsensitive() {
        responseBuilder.putHeader("x-AMZN-error-message", MESSAGE_CONTENT);
        String parsed = parser.parseErrorMessage(responseBuilder.build(), jsonNode);
        assertEquals(MESSAGE_CONTENT, parsed);
    }

    @Test
    public void errorMessageInHeader_TakesPrecedenceOverMessageInBody() {
        responseBuilder.putHeader(X_AMZN_ERROR_MESSAGE, MESSAGE_CONTENT);
        jsonNode.put("message", "other message in body");
        String parsed = parser.parseErrorMessage(responseBuilder.build(), jsonNode);
        assertEquals(MESSAGE_CONTENT, parsed);
    }

}
