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
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.Base64;

/**
 * Asserts on the body (expected to be CBOR) of the marshalled request.
 */
public class CborBodyAssertion extends MarshallingAssertion {
    private static final ObjectMapper MAPPER = new ObjectMapper(new CBORFactory());

    private final String cborEquals;

    public CborBodyAssertion(String cborEquals) {
        this.cborEquals = cborEquals;
    }

    @Override
    protected void doAssert(LoggedRequest actual) throws Exception {
        JsonNode expected = normalizeToDoubles(MAPPER.readTree(Base64.getDecoder().decode(cborEquals)));
        JsonNode actualJson = normalizeToDoubles(MAPPER.readTree(actual.getBody()));
        assertEquals(expected, actualJson);
    }

    /**
     * CBOR allows serializing floating point numbers to different sizes (eg, float16/32/64).
     * However, in assertEquals float and double nodes will never be equal so we convert them
     * all to doubles.
     */
    private JsonNode normalizeToDoubles(JsonNode node) {
        if (node.isFloat() || node.isDouble()) {
            return DoubleNode.valueOf(node.doubleValue());
        }
        if (node.isArray()) {
            ArrayNode array = MAPPER.createArrayNode();
            for (JsonNode item : node) {
                array.add(normalizeToDoubles(item));
            }
            return array;
        }
        if (node.isObject()) {
            ObjectNode obj = MAPPER.createObjectNode();
            node.fields().forEachRemaining(entry -> obj.set(entry.getKey(), normalizeToDoubles(entry.getValue())));
            return obj;
        }
        return node;
    }
}
