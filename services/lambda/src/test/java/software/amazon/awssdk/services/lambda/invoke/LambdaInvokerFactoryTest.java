/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.lambda.invoke;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LogType;

public class LambdaInvokerFactoryTest {

    private LambdaAsyncClient mock;
    private Invoker invoker;

    @Before
    public void before() {
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();

        mock = mock(LambdaAsyncClient.class);
        invoker = LambdaInvokerFactory.builder()
                                      .lambdaClient(mock)
                                      .build(Invoker.class);
    }

    @Test(expected = LambdaSerializationException.class)
    public void test_NoAnnotation() {
        invoker.doItWithoutAnnotations();
    }

    @Test(expected = LambdaSerializationException.class)
    public void test_TooManyArguments() {
        invoker.doItWithTooManyArguments(null, null);
    }

    @Test
    public void test_NoArgs_Void_Default() {
        InvokeRequest request = InvokeRequest.builder().functionName("doIt").invocationType("RequestResponse")
                                                   .logType("None").build();

        when(mock.invoke(request)).thenReturn(CompletableFuture.completedFuture(InvokeResponse.builder()
                .statusCode(204)
                .build()));

        invoker.doIt();
    }

    @Test
    public void test_NoArgs_Void_Async() {
        InvokeRequest request = InvokeRequest.builder().functionName("doIt").invocationType("Event")
                                                   .logType("None").build();

        when(mock.invoke(request)).thenReturn(CompletableFuture.completedFuture(InvokeResponse.builder().statusCode(202).build()));

        invoker.doItAsynchronously();
    }

    @Test
    public void test_NoArgs_Void_Logs() {
        InvokeRequest request = InvokeRequest.builder()
                .functionName("doIt")
                .invocationType("RequestResponse")
                .logType("Tail")
                .build();

        when(mock.invoke(request)).thenReturn(
                CompletableFuture.completedFuture(InvokeResponse.builder()
                        .statusCode(204)
                        .logResult("TG9nZ2l0eSBsb2dnaW5n")
                        .build()));

        invoker.doItWithLogs();
    }

    @Test(expected = LambdaSerializationException.class)
    public void test_LogsAndAsync_Invalid() {
        invoker.doItAsyncLogs();
    }

    @Test(expected = LambdaSerializationException.class)
    public void test_LogsAndDryRun_Invalid() {
        invoker.doItDryRunLogs();
    }

    @Test
    public void test_NoArgs_String_Default() {
        InvokeRequest request = InvokeRequest.builder()
                .functionName("getString")
                .invocationType("RequestResponse")
                .logType("None")
                .build();

        when(mock.invoke(request))
                .thenReturn(CompletableFuture.completedFuture(getStringSuccessResult()));

        String result = invoker.getString();
        assertEquals("OK", result);
    }

    @Test
    public void test_String_Void_Default() {
        InvokeRequest request = InvokeRequest.builder()
                .functionName("setString")
                .invocationType("RequestResponse")
                .logType("None")
                .payload(ByteBuffer.wrap("\"Hello World\"".getBytes(StandardCharsets.UTF_8)))
                .build();

        when(mock.invoke(request)).thenReturn(CompletableFuture.completedFuture(InvokeResponse.builder().statusCode(204).build()));

        invoker.setString("Hello World");
    }

    @Test
    public void test_List_Map_Default() {
        InvokeRequest request = InvokeRequest.builder()
                .functionName("keySet")
                .invocationType("RequestResponse")
                .logType("None")
                .payload(ByteBuffer.wrap("{\"a\":\"b\",\"c\":\"d\"}".getBytes(StandardCharsets.UTF_8)))
                .build();

        when(mock.invoke(request)).thenReturn(
                CompletableFuture.completedFuture(
                        InvokeResponse.builder().statusCode(200).payload(ByteBuffer.wrap("[\"a\",\"c\"]".getBytes())).build()));

        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("a", "b");
        map.put("c", "d");

        Set<String> keys = invoker.keySet(map);

        Set<String> expected = new TreeSet<String>();
        expected.add("a");
        expected.add("c");

        assertEquals(expected, keys);
    }

    @Test
    public void test_Pojo_Pojo_Default() {
        InvokeRequest request = InvokeRequest.builder()
                .functionName("transform")
                .invocationType("RequestResponse")
                .logType("None")
                .payload(ByteBuffer.wrap("{\"string\":\"Hello World\",\"integer\":12345}".getBytes(StandardCharsets.UTF_8)))
                .build();

        when(mock.invoke(request)).thenReturn(
                CompletableFuture.completedFuture(InvokeResponse.builder().statusCode(200).payload(
                        ByteBuffer.wrap("{\"string\":\"Hello Test\",\"integer\":67890}".getBytes())).build()));

        Pojo pojo = new Pojo();
        pojo.setString("Hello World");
        pojo.setInteger(12345);

        pojo = invoker.transform(pojo);

        assertEquals("Hello Test", pojo.getString());
        assertEquals((Integer) 67890, pojo.getInteger());
    }

    @Test
    public void test_UnhandledException() {
        InvokeRequest request = InvokeRequest.builder()
                .functionName("fail")
                .invocationType("RequestResponse")
                .logType("None")
                .build();

        when(mock.invoke(request)).thenReturn(
                CompletableFuture.completedFuture(InvokeResponse.builder()
                        .statusCode(200)
                        .functionError("Unhandled")
                        .payload(
                                ByteBuffer.wrap("{\"errorMessage\":\"I'm the message\",\"errorType\":\"BOOM\"}"
                                                        .getBytes())).build()));

        try {
            invoker.fail();
            fail("Expected LambdaFunctionException");
        } catch (LambdaFunctionException expected) {
            assertEquals("I'm the message", expected.getMessage());
            assertFalse(expected.isHandled());
            assertEquals("BOOM", expected.getType());
        }
    }

    @Test
    public void unhandledExceptionWithUnknownField_UnmarshallsCorrectly() {
        InvokeRequest request = InvokeRequest.builder()
                .functionName("fail")
                .invocationType("RequestResponse")
                .logType("None")
                .build();

        when(mock.invoke(request)).thenReturn(
                CompletableFuture.completedFuture(InvokeResponse.builder()
                        .statusCode(200)
                        .functionError("Unhandled")
                        .payload(
                                ByteBuffer
                                        .wrap("{\"errorMessage\":\"I'm the message\",\"errorType\":\"BOOM\",\"unknownField\": \"foo\"}"
                                                      .getBytes())).build()));

        try {
            invoker.fail();
            fail("Expected LambdaFunctionException");
        } catch (LambdaFunctionException expected) {
            assertEquals("I'm the message", expected.getMessage());
            assertFalse(expected.isHandled());
            assertEquals("BOOM", expected.getType());
        }
    }

    @Test
    public void unhandledPythonException_UnmarshallsCorrectly() {
        final String exceptionJson = "{\"stackTrace\":[[\"/var/task/lambda_function.py\",9,\"lambda_handler\",\"raise Exception('Something went wrong')\"]],\"errorType\":\"PythonException\",\"errorMessage\":\"I'm the message\"}";
        InvokeRequest request = InvokeRequest.builder()
                .functionName("fail")
                .invocationType("RequestResponse")
                .logType("None")
                .build();

        when(mock.invoke(request)).thenReturn(
                CompletableFuture.completedFuture(InvokeResponse.builder()
                        .statusCode(200)
                        .functionError("Unhandled")
                        .payload(ByteBuffer.wrap(exceptionJson.getBytes())).build()));

        try {
            invoker.fail();
            fail("Expected LambdaFunctionException");
        } catch (LambdaFunctionException expected) {
            assertEquals("I'm the message", expected.getMessage());
            assertFalse(expected.isHandled());
            assertEquals("PythonException", expected.getType());

            StackTraceElement[] elements = expected.getStackTrace();
            assertEquals(1, elements.length);

            assertEquals(Invoker.class.getName(), elements[0].getClassName());
            assertEquals("/var/task/lambda_function.py,9,lambda_handler,raise Exception('Something went wrong')",
                         elements[0].getMethodName());
        }
    }

    @Test
    public void test_HandledException() {
        InvokeRequest request = InvokeRequest.builder().functionName("fail").invocationType("RequestResponse")
                                                   .logType("None").build();

        when(mock.invoke(request)).thenReturn(
                CompletableFuture.completedFuture(InvokeResponse.builder()
                        .statusCode(200)
                        .functionError("Handled")
                        .payload(
                                ByteBuffer.wrap(("{\"errorMessage\":\"I'm the message\"," +
                                                 "\"errorType\":\"BOOM\"," +
                                                 "\"cause\":{}," +
                                                 "\"stackTrace\":[\"abc\",\"def\",\"ghi\"]}").getBytes())).build()));

        try {
            invoker.fail();
            fail("Expected LambdaFunctionException");
        } catch (LambdaFunctionException expected) {
            assertEquals("I'm the message", expected.getMessage());
            assertTrue(expected.isHandled());
            assertEquals("BOOM", expected.getType());

            StackTraceElement[] elements = expected.getStackTrace();
            assertEquals(3, elements.length);

            assertEquals(Invoker.class.getName(), elements[0].getClassName());
            assertEquals("abc", elements[0].getMethodName());

            assertEquals(Invoker.class.getName(), elements[1].getClassName());
            assertEquals("def", elements[1].getMethodName());

            assertEquals(Invoker.class.getName(), elements[2].getClassName());
            assertEquals("ghi", elements[2].getMethodName());
        }
    }

    /**
     * For Java Lambda functions, the error type will be the fully qualified class name.
     */
    @Test
    public void customExceptionTypeWithFullyQualifiedName_ThrowsCustomException() {
        InvokeRequest request = InvokeRequest.builder()
                .functionName("fail")
                .invocationType("RequestResponse")
                .logType("None")
                .build();

        when(mock.invoke(request)).thenReturn(
                CompletableFuture.completedFuture(InvokeResponse.builder()
                        .statusCode(200)
                        .functionError("Handled")
                        .payload(ByteBuffer.wrap((String.format(
                                "{\"errorMessage\":\"I'm the message\"," +
                                "\"errorType\":\"%s\",\"stackTrace\":[\"abc\",\"def\",\"ghi\"]}",
                                CustomException.class.getName())).getBytes())).build()));

        try {
            invoker.fail();
            fail("Expected CustomException");
        } catch (CustomException expected) {
            assertEquals("I'm the message", expected.getMessage());

            StackTraceElement[] elements = expected.getStackTrace();
            assertEquals(3, elements.length);

            assertEquals(Invoker.class.getName(), elements[0].getClassName());
            assertEquals("abc", elements[0].getMethodName());

            assertEquals(Invoker.class.getName(), elements[1].getClassName());
            assertEquals("def", elements[1].getMethodName());

            assertEquals(Invoker.class.getName(), elements[2].getClassName());
            assertEquals("ghi", elements[2].getMethodName());
        }
    }

    /**
     * A non fully qualified name may be returned for non Java Lambda functions.
     */
    @Test
    public void customExceptionTypeWithShortName_ThrowsCustomException() {
        InvokeRequest request = InvokeRequest.builder()
                .functionName("fail")
                .invocationType("RequestResponse")
                .logType("None")
                .build();

        when(mock.invoke(request)).thenReturn(
                CompletableFuture.completedFuture(InvokeResponse.builder()
                        .statusCode(200)
                        .functionError("Handled")
                        .payload(
                                ByteBuffer.wrap(("{\"errorMessage\":\"I'm the message\"," + "\"errorType\":\"CustomException\","
                                                 + "\"stackTrace\":[\"abc\",\"def\",\"ghi\"]}").getBytes())).build()));

        try {
            invoker.fail();
            fail("Expected CustomException");
        } catch (CustomException expected) {
            assertEquals("I'm the message", expected.getMessage());
        }
    }

    @Test
    public void test_SerializationException() {
        try {
            invoker.broken(new BrokenPojo());
            fail("Expected LambdaSerializationException");
        } catch (LambdaSerializationException expected) {
            // Ignored or expected.
        }
    }

    @Test
    public void test_DeserializationException() {
        InvokeRequest request = InvokeRequest.builder()
                .functionName("broken")
                .invocationType("RequestResponse")
                .logType("None")
                .build();

        when(mock.invoke(request)).thenReturn(
                CompletableFuture.completedFuture(
                        InvokeResponse.builder().statusCode(200).payload(ByteBuffer.wrap("I'm not even JSON!".getBytes())).build()));

        try {
            invoker.broken();
            fail("Expected LambdaSerializationException");
        } catch (LambdaSerializationException expected) {
            // Ignored or expected.
        }
    }

    @Test
    public void test_BogusLog() {
        InvokeRequest request = InvokeRequest.builder().functionName("doIt").invocationType("RequestResponse")
                                                   .logType("Tail").build();

        when(mock.invoke(request)).thenReturn(
                CompletableFuture.completedFuture(InvokeResponse.builder().statusCode(200).logResult("I'm not valid Base-64")
                                  .payload(ByteBuffer.wrap("I should be ignored".getBytes())).build()));

        // Should not fail just because the logged data is invalid.
        invoker.doItWithLogs();
    }

    @Test
    public void testBogusException() {
        InvokeRequest request = InvokeRequest.builder().functionName("fail").invocationType("RequestResponse")
                                                   .logType("None").build();

        when(mock.invoke(request)).thenReturn(
                CompletableFuture.completedFuture(InvokeResponse.builder().statusCode(200).functionError("Handled")
                                  .payload(ByteBuffer.wrap("Bogus".getBytes())).build()));

        try {
            invoker.fail();
            fail("Expected LambdaFunctionException");
        } catch (LambdaFunctionException expected) {
            assertNotNull(expected.getMessage());
            assertTrue(expected.isHandled());
            assertNull(expected.getType());
        }
    }

    @Test
    public void toString_DoesNotNeedLambdaFunctionMapping() throws InterruptedException {
        assertNotNull(invoker.toString());
    }

    @Test
    public void functionAliasConfigured_SetsQualifierOnInvokeRequest() {
        final String alias = "MyAlias";
        invoker = LambdaInvokerFactory.builder()
                                      .lambdaClient(mock)
                                      .functionAlias(alias)
                                      .build(Invoker.class);

        ArgumentCaptor<InvokeRequest> invokeArgCaptor = ArgumentCaptor
                .forClass(InvokeRequest.class);
        when(mock.invoke(invokeArgCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(getStringSuccessResult()));

        invoker.getString();

        assertEquals(alias, invokeArgCaptor.getValue().qualifier());
    }

    @Test
    public void functionVersionConfigured_SetsQualifierOnInvokeRequest() {
        final String functionVersion = "1234";
        invoker = LambdaInvokerFactory.builder()
                                      .lambdaClient(mock)
                                      .functionVersion(functionVersion)
                                      .build(Invoker.class);

        ArgumentCaptor<InvokeRequest> invokeArgCaptor = ArgumentCaptor
                .forClass(InvokeRequest.class);
        when(mock.invoke(invokeArgCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(getStringSuccessResult()));

        invoker.getString();

        assertEquals(functionVersion, invokeArgCaptor.getValue().qualifier());
    }

    private InvokeResponse getStringSuccessResult() {
        return InvokeResponse.builder()
                .statusCode(200)
                .payload(ByteBuffer.wrap("\"OK\"".getBytes())).build();
    }


    public static interface Invoker {

        void doItWithoutAnnotations();

        @LambdaFunction
        void doItWithTooManyArguments(String foo, String bar);

        @LambdaFunction
        void doIt();

        @LambdaFunction(functionName = "doIt", invocationType = InvocationType.Event)
        void doItAsynchronously();

        @LambdaFunction(functionName = "doIt", logType = LogType.Tail)
        void doItWithLogs();

        @LambdaFunction(functionName = "doIt", invocationType = InvocationType.Event, logType = LogType.Tail)
        void doItAsyncLogs();

        @LambdaFunction(functionName = "doIt", invocationType = InvocationType.DryRun, logType = LogType.Tail)
        void doItDryRunLogs();

        @LambdaFunction
        String getString();

        @LambdaFunction
        void setString(String value);

        @LambdaFunction
        Set<String> keySet(Map<String, String> map);

        @LambdaFunction
        Pojo transform(Pojo pojo);

        @LambdaFunction
        void fail() throws CustomException;

        @LambdaFunction
        void broken(BrokenPojo pojo);

        @LambdaFunction
        Pojo broken();

    }

    public static class Pojo {

        private String string;
        private Integer integer;

        public String getString() {
            return string;
        }

        public void setString(String value) {
            string = value;
        }

        public Integer getInteger() {
            return integer;
        }

        public void setInteger(Integer value) {
            integer = value;
        }
    }

    public static class BrokenPojo {
        public String getString() {
            throw new IllegalStateException();
        }
    }

    public static class CustomException extends RuntimeException {

        public CustomException(String message) {
            super(message);
        }
    }
}
