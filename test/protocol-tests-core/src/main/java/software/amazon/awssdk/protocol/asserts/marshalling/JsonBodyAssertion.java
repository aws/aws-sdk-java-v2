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

package software.amazon.awssdk.protocol.asserts.marshalling;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

/**
 * Asserts on the body (expected to be JSON) of the marshalled request.
 */
public class JsonBodyAssertion extends MarshallingAssertion {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String jsonEquals;

    public JsonBodyAssertion(String jsonEquals) {
        this.jsonEquals = jsonEquals;
    }

    @Override
    protected void doAssert(LoggedRequest actual) throws Exception {
        JsonNode expected = MAPPER.readTree(jsonEquals);
        JsonNode actualJson = convertWholeNumberDoubleToLong(MAPPER.readTree(actual.getBodyAsString()));
        assertEquals(expected, actualJson);
    }

    /**
     * We serialize some numbers (in particular epoch timestamps) as doubles such as 123.000.
     * In protocol tests, these values are parsed as longs.  This conversion insures that
     * 123.000 will equal 123.
     */
    public static JsonNode convertWholeNumberDoubleToLong(JsonNode node) {
        if (node.isDouble()) {
            double value = node.doubleValue();
            if (value % 1 == 0) {
                if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                    return new IntNode((int) value);
                }
                return new LongNode((long) value);
            }
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            ObjectNode result = obj.objectNode();
            obj.fieldNames().forEachRemaining(field -> result.set(field, convertWholeNumberDoubleToLong(obj.get(field))));
            return result;
        }
        if (node.isArray()) {
            ArrayNode array = (ArrayNode) node;
            ArrayNode result = array.arrayNode();
            for (JsonNode item : array) {
                result.add(convertWholeNumberDoubleToLong(item));
            }
            return result;
        }
        return node;
    }
}
