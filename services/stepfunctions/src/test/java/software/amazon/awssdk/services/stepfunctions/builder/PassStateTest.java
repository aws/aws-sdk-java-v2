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

package software.amazon.awssdk.services.stepfunctions.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.end;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.passState;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Test;
import software.amazon.awssdk.services.stepfunctions.builder.states.PassState;

public class PassStateTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static void assertJsonEquals(String expected, String actual) {
        try {
            assertEquals(MAPPER.readTree(expected), MAPPER.readTree(actual));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getResult_ResultCreatedFromString_ReturnsJsonResult() {
        final String strResult = "{\"Foo\": \"Bar\"}";
        assertJsonEquals(strResult, newPassState().result(strResult).build().getResult());
    }

    @Test
    public void getResult_ResultCreatedFromObject_ReturnsJsonResult() {
        assertJsonEquals("{\"foo\": \"value\"}", newPassState().result(new SimplePojo("value")).build().getResult());
    }

    @Test
    public void getResult_NullResult_ReturnsNull() {
        assertNull(newPassState().build().getResult());
    }

    @Test(expected = Exception.class)
    public void setResult_MalformedJson_ThrowsException() {
        newPassState().result("{").build().getResult();
    }

    @Test
    public void setResult_PojoWithJacksonAnnotations_IgnoresAnnotations() {
        PojoWithJacksonAnnotations pojo = new PojoWithJacksonAnnotations();
        pojo.setFoo("FooValue");
        pojo.setBar("BarValue");
        pojo.baz = "BazValue";
        assertJsonEquals("{\"foo\": \"FooValue\", \"bar\": \"BarValue\"}", newPassState().result(pojo).build().getResult());
    }

    private PassState.Builder newPassState() {
        return passState().transition(end());
    }
}
