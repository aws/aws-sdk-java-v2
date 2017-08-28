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

package software.amazon.awssdk.interceptor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.SdkRequest;
import software.amazon.awssdk.SdkResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;

/**
 * Verify the functionality of the {@link ExecutionInterceptorChain}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ExecutionInterceptorChainTest {
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private NoOpInterceptor first;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private NoOpInterceptor second;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private NoOpInterceptor third;

    private ExecutionInterceptorChain chain;

    @Before
    public void sortInterceptors() {
        when(first.priority()).thenReturn(new Priority(1));
        when(second.priority()).thenReturn(new Priority(2));
        when(third.priority()).thenReturn(new Priority(2));

        // Intentionally create the chain in a different order to make sure:
        // FIRST gets moved to the front, SECOND and THIRD stay in the provided order
        chain = new ExecutionInterceptorChain(Arrays.asList(second, first, third));
    }

    @Test
    public void methodOrderIsCorrect() {
        InterceptorContext context = InterceptorContext.builder()
                                                       .request(new SdkRequest() {})
                                                       .httpRequest(SdkHttpFullRequest.builder().build())
                                                       .httpResponse(SdkHttpFullResponse.builder().build())
                                                       .response(new SdkResponse() {})
                                                       .build();

        chain.beforeExecution(context, null);
        chain.modifyRequest(context, null);
        chain.beforeMarshalling(context, null);
        chain.afterMarshalling(context, null);
        chain.modifyHttpRequest(context, null);
        chain.beforeTransmission(context, null);
        chain.afterTransmission(context, null);
        chain.modifyHttpResponse(context, null);
        chain.beforeUnmarshalling(context, null);
        chain.afterUnmarshalling(context, null);
        chain.modifyResponse(context, null);
        chain.afterExecution(context, null);

        verifyInOrder(i -> i.beforeExecution(any(), any()));
        verifyInOrder(i -> i.modifyRequest(any(), any()));
        verifyInOrder(i -> i.beforeMarshalling(any(), any()));
        verifyInOrder(i -> i.afterMarshalling(any(), any()));
        verifyInOrder(i -> i.modifyHttpRequest(any(), any()));
        verifyInOrder(i -> i.beforeTransmission(any(), any()));
        verifyReverseOrder(i -> i.afterTransmission(any(), any()));
        verifyReverseOrder(i -> i.modifyHttpResponse(any(), any()));
        verifyReverseOrder(i -> i.beforeUnmarshalling(any(), any()));
        verifyReverseOrder(i -> i.afterUnmarshalling(any(), any()));
        verifyReverseOrder(i -> i.modifyResponse(any(), any()));
        verifyReverseOrder(i -> i.afterExecution(any(), any()));
    }

    public void verifyInOrder(Consumer<ExecutionInterceptor> methodCall) {
        InOrder inOrder = Mockito.inOrder(first, second, third);
        methodCall.accept(inOrder.verify(first));
        methodCall.accept(inOrder.verify(second));
        methodCall.accept(inOrder.verify(third));
    }

    public void verifyReverseOrder(Consumer<ExecutionInterceptor> methodCall) {
        InOrder inOrder = Mockito.inOrder(first, second, third);
        methodCall.accept(inOrder.verify(third));
        methodCall.accept(inOrder.verify(second));
        methodCall.accept(inOrder.verify(first));
    }

    public static class NoOpInterceptor implements ExecutionInterceptor {
        @Override
        public Priority priority() {
            return Priority.USER;
        }
    }
}
