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

package software.amazon.awssdk.protocol.asserts.unmarshalling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.InputStream;
import java.lang.reflect.Field;
import org.apache.commons.io.IOUtils;
import software.amazon.awssdk.protocol.reflect.ShapeModelReflector;

/**
 * Asserts on the unmarshalled result of a given operation.
 */
public class UnmarshalledResultAssertion extends UnmarshallingAssertion {

    private final JsonNode expectedResult;

    public UnmarshalledResultAssertion(JsonNode expectedResult) {
        this.expectedResult = expectedResult;
    }

    @Override
    protected void doAssert(UnmarshallingTestContext context, Object actual) throws Exception {
        ShapeModelReflector shapeModelReflector = createShapeReflector(context);
        Object expectedResult = shapeModelReflector.createShapeObject();
        for (Field field : expectedResult.getClass().getDeclaredFields()) {
            assertFieldEquals(field, actual, expectedResult);
        }

        // Streaming response is captured by the response handler so we have to handle it separately
        if (context.getStreamedResponse() != null) {
            assertEquals(shapeModelReflector.getStreamingMemberValue(), context.getStreamedResponse());
        }
    }

    /**
     * We can't use assertReflectionEquals on the result object directly. InputStreams require some
     * special handling so we compare field by field and use a special assertion for streaming
     * types.
     */
    private void assertFieldEquals(Field field, Object actual, Object expectedResult) throws
                                                                                      Exception {
        field.setAccessible(true);
        if (field.getType().isAssignableFrom(InputStream.class)) {
            assertTrue(IOUtils.contentEquals((InputStream) field.get(expectedResult),
                                             (InputStream) field.get(actual)));
        } else {
            assertReflectionEquals(field.get(expectedResult), field.get(actual));
        }
    }

    private ShapeModelReflector createShapeReflector(UnmarshallingTestContext context) {
        return new ShapeModelReflector(context.getModel(), getOutputClassName(context), this.expectedResult);
    }

    /**
     * @return Class name of the output model.
     */
    private String getOutputClassName(UnmarshallingTestContext context) {
        return context.getModel().getOperations().get(context.getOperationName()).getReturnType()
                      .getReturnType();
    }
}
