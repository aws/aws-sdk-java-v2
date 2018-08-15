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

package software.amazon.awssdk.awscore.eventstream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContextImpl;
import software.amazon.awssdk.core.runtime.transform.SimpleTypeJsonUnmarshallers;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.ImmutableMap;

public class EventStreamExceptionJsonUnmarshallerTest {

    private static final String EXCEPTION_MESSAGE = "Random error message";
    private static final String ERROR_CODE = "modeledException";

    @Test
    public void testExceptionKey_WithLowerCase() throws Exception {
        String exception = String.format("{\"message\": \"%s\"}", EXCEPTION_MESSAGE);

        testHelper(exception);
    }

    @Test
    public void testExceptionKey_WithUpperCase() throws Exception {
        String exception = String.format("{\"Message\": \"%s\"}", EXCEPTION_MESSAGE);

        testHelper(exception);
    }

    private void testHelper(String exceptionMessage) throws Exception {
        AwsServiceException exception = EventStreamExceptionJsonUnmarshaller
            .populateDefaultException(() -> AwsServiceException.builder(),
                                      setupUnmarshaller(exceptionMessage, httpResponse()));

        assertThat(EXCEPTION_MESSAGE,  is(equalTo(exception.awsErrorDetails().errorMessage())));
        assertThat("modeledException",  is(equalTo(exception.awsErrorDetails().errorCode())));
    }

    private JsonUnmarshallerContext setupUnmarshaller(String snippet, SdkHttpFullResponse httpResponse) throws Exception {
        JsonParser jsonParser = new JsonFactory().createJsonParser(new ByteArrayInputStream(snippet.getBytes()));
        JsonUnmarshallerContext unmarshallerContext = new JsonUnmarshallerContextImpl(jsonParser,
                                                                                      JSON_SCALAR_UNMARSHALLERS,
                                                                                      httpResponse);
        return unmarshallerContext;
    }

    private SdkHttpFullResponse httpResponse() {
        SdkHttpFullResponse httpResponse = SdkHttpFullResponse.builder()
                                                              .statusCode(400)
                                                              .putHeader(":message-type", "exception")
                                                              .putHeader(":exception-type", ERROR_CODE)
                                                              .putHeader(":content-type", "application/json")
                                                              .content(AbortableInputStream.create(
                                                                  new ByteArrayInputStream("content".getBytes(
                                                                      StandardCharsets.UTF_8))))
                                                              .build();
        return httpResponse;
    }

    private static final Map<Class<?>, Unmarshaller<?, JsonUnmarshallerContext>> JSON_SCALAR_UNMARSHALLERS =
        new ImmutableMap.Builder<Class<?>, Unmarshaller<?, JsonUnmarshallerContext>>()
            .put(String.class, SimpleTypeJsonUnmarshallers.StringJsonUnmarshaller.getInstance())
            .build();
}
