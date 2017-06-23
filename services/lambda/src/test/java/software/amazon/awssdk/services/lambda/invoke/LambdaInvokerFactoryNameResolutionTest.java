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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

/**
 * Tests the default function name resolution and the ability to customize it through
 * {@link LambdaInvokerFactoryConfig}.
 */
public class LambdaInvokerFactoryNameResolutionTest {

    /**
     * Name overridden in {@link LambdaFunction} functionName attribute.
     */
    private static final String OVERRIDDEN_NAME = "OverriddenFunctionName";

    /**
     * Dummy name returned by custom implementation of {@link LambdaFunctionNameResolver}.
     */
    private static final String STATIC_FUNCTION_NAME = "StaticFunctionName";

    @Mock
    private LambdaAsyncClient lambda;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        stubSucessfulInvokeResponse();
    }

    /**
     * These tests are only concerned with the setting of function name so we always return a
     * successful response
     */
    private void stubSucessfulInvokeResponse() {
        InvokeResponse result = InvokeResponse.builder()
                .payload(ByteBuffer.wrap(new byte[] {}))
                .statusCode(200).build();
        when(lambda.invoke(any(InvokeRequest.class))).thenReturn(CompletableFuture.completedFuture(result));
    }

    @Test
    public void functionNameOverridenInAnnotation_UsesOverridenNameAsFunctionName() {
        UnitTestInterface proxy = LambdaInvokerFactory.builder()
                                                      .lambdaClient(lambda)
                                                      .build(UnitTestInterface.class);
        proxy.functionNameOverridenInAnnotation();
        InvokeRequest capturedRequest = captureInvokeRequestArgument();
        assertEquals(OVERRIDDEN_NAME, capturedRequest.functionName());
    }

    @Test
    public void functionNameNotSetInAnnotation_UsesMethodNameAsFunctionName() {
        UnitTestInterface proxy = LambdaInvokerFactory.builder()
                                                      .lambdaClient(lambda)
                                                      .build(UnitTestInterface.class);
        proxy.functionNameNotSetInAnnotation();
        InvokeRequest capturedRequest = captureInvokeRequestArgument();
        assertEquals("functionNameNotSetInAnnotation", capturedRequest.functionName());
    }

    private InvokeRequest captureInvokeRequestArgument() {
        ArgumentCaptor<InvokeRequest> argument = ArgumentCaptor.forClass(InvokeRequest.class);
        verify(lambda).invoke(argument.capture());
        InvokeRequest value = argument.getValue();
        return value;
    }

    @Test
    public void customFunctionNameResolver_DoesNotUseOverrideInAnnotation() {
        UnitTestInterface proxy = LambdaInvokerFactory.builder()
                                                      .lambdaClient(lambda)
                                                      .lambdaFunctionNameResolver(new StaticFunctionNameResolver())
                                                      .build(UnitTestInterface.class);
        proxy.functionNameOverridenInAnnotation();
        InvokeRequest capturedRequest = captureInvokeRequestArgument();
        assertEquals(STATIC_FUNCTION_NAME, capturedRequest.functionName());
    }

    @Test
    public void customFunctionNameResolver_DoesNotUseMethodName() {
        UnitTestInterface proxy = LambdaInvokerFactory.builder()
                                                      .lambdaClient(lambda)
                                                      .lambdaFunctionNameResolver(new StaticFunctionNameResolver())
                                                      .build(UnitTestInterface.class);
        proxy.functionNameNotSetInAnnotation();
        InvokeRequest capturedRequest = captureInvokeRequestArgument();
        assertEquals(STATIC_FUNCTION_NAME, capturedRequest.functionName());
    }

    /**
     * Interface to proxy in unit tests
     */
    private static interface UnitTestInterface {

        @LambdaFunction(functionName = OVERRIDDEN_NAME)
        public void functionNameOverridenInAnnotation();

        @LambdaFunction
        public void functionNameNotSetInAnnotation();
    }

    /**
     * Dummy implementation of {@link LambdaFunctionNameResolver} that always returns the same
     * function name regardless of context.
     */
    public static class StaticFunctionNameResolver implements LambdaFunctionNameResolver {

        @Override
        public String getFunctionName(Method method, LambdaFunction annotation, LambdaInvokerFactoryConfig config) {
            return STATIC_FUNCTION_NAME;
        }

    }
}
