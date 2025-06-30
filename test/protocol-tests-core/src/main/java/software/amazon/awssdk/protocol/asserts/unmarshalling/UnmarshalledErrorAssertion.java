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


import static org.junit.Assert.fail;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.Field;
import org.junit.Assert;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.protocol.reflect.ShapeModelReflector;

public class UnmarshalledErrorAssertion extends UnmarshallingAssertion {
    private final JsonNode expectedError;

    public UnmarshalledErrorAssertion(JsonNode expectedError) {
        this.expectedError = expectedError;
    }

    @Override
    protected void doAssert(UnmarshallingTestContext context, Object actual) throws Exception {
        if (!(actual instanceof SdkServiceException)) {
            fail("Expected unmarshalled object to be an instance of SdkServiceException");
        }
        SdkServiceException actualException = (SdkServiceException) actual;
        SdkServiceException expectedException = createExpectedResult(context);
        for (Field field : expectedException.getClass().getDeclaredFields()) {
            assertFieldEquals(field, actualException, expectedException);
        }

        if (expectedException.getMessage() != null) {
            Assert.assertTrue(actualException.getMessage().startsWith(expectedException.getMessage()));
        }
    }

    private SdkServiceException createExpectedResult(UnmarshallingTestContext context) {
        return (SdkServiceException) new ShapeModelReflector(context.getModel(), context.getErrorName() + "Exception",
                                                             this.expectedError).createShapeObject();
    }

    private void assertFieldEquals(Field field, Object actual, Object expectedResult) throws
                                                                                      Exception {
        field.setAccessible(true);
        assertReflectionEquals(field.get(expectedResult), field.get(actual));
    }
}