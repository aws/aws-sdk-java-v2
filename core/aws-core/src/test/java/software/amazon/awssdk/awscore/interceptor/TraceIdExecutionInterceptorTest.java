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

package software.amazon.awssdk.awscore.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

public class TraceIdExecutionInterceptorTest {
    @Test
    public void nothingDoneWithoutEnvSettings() {
        EnvironmentVariableHelper.run(env -> {
            resetRelevantEnvVars(env);
            Context.ModifyHttpRequest context = context();
            assertThat(modifyHttpRequest(context)).isSameAs(context.httpRequest());
        });
    }

    @Test
    public void headerAddedWithEnvSettings() {
        EnvironmentVariableHelper.run(env -> {
            resetRelevantEnvVars(env);
            env.set("AWS_LAMBDA_FUNCTION_NAME", "foo");
            env.set("_X_AMZN_TRACE_ID", "bar");
            Context.ModifyHttpRequest context = context();
            assertThat(modifyHttpRequest(context).firstMatchingHeader("X-Amzn-Trace-Id")).hasValue("bar");
        });
    }


    @Test
    public void headerAddedWithSysPropWhenNoEnvSettings() {
        EnvironmentVariableHelper.run(env -> {
            resetRelevantEnvVars(env);
            env.set("AWS_LAMBDA_FUNCTION_NAME", "foo");
            Properties props = System.getProperties();
            props.setProperty("com.amazonaws.xray.traceHeader", "sys-prop");
            Context.ModifyHttpRequest context = context();
            assertThat(modifyHttpRequest(context).firstMatchingHeader("X-Amzn-Trace-Id")).hasValue("sys-prop");
        });
    }

    @Test
    public void headerAddedWithEnvVariableValueWhenBothEnvAndSysPropAreSet() {
        EnvironmentVariableHelper.run(env -> {
            resetRelevantEnvVars(env);
            env.set("AWS_LAMBDA_FUNCTION_NAME", "foo");
            env.set("_X_AMZN_TRACE_ID", "bar");
            Properties props = System.getProperties();
            props.setProperty("com.amazonaws.xray.traceHeader", "sys-prop");
            Context.ModifyHttpRequest context = context();
            assertThat(modifyHttpRequest(context).firstMatchingHeader("X-Amzn-Trace-Id")).hasValue("sys-prop");
        });
    }

    @Test
    public void headerNotAddedIfHeaderAlreadyExists() {
        EnvironmentVariableHelper.run(env -> {
            resetRelevantEnvVars(env);
            env.set("AWS_LAMBDA_FUNCTION_NAME", "foo");
            env.set("_X_AMZN_TRACE_ID", "bar");
            Context.ModifyHttpRequest context = context(SdkHttpRequest.builder()
                                                                      .uri(URI.create("https://localhost"))
                                                                      .method(SdkHttpMethod.GET)
                                                                      .putHeader("X-Amzn-Trace-Id", "existing")
                                                                      .build());
            assertThat(modifyHttpRequest(context)).isSameAs(context.httpRequest());
        });
    }

    @Test
    public void headerNotAddedIfNotInLambda() {
        EnvironmentVariableHelper.run(env -> {
            resetRelevantEnvVars(env);
            env.set("_X_AMZN_TRACE_ID", "bar");
            Context.ModifyHttpRequest context = context();
            assertThat(modifyHttpRequest(context)).isSameAs(context.httpRequest());
        });
    }

    @Test
    public void headerNotAddedIfNoTraceIdEnvVar() {
        EnvironmentVariableHelper.run(env -> {
            resetRelevantEnvVars(env);
            env.set("_X_AMZN_TRACE_ID", "bar");
            Context.ModifyHttpRequest context = context();
            assertThat(modifyHttpRequest(context)).isSameAs(context.httpRequest());
        });
    }

    private Context.ModifyHttpRequest context() {
        return context(SdkHttpRequest.builder()
                                     .uri(URI.create("https://localhost"))
                                     .method(SdkHttpMethod.GET)
                                     .build());
    }


    private Context.ModifyHttpRequest context(SdkHttpRequest request) {
        return InterceptorContext.builder()
                                 .request(Mockito.mock(SdkRequest.class))
                                 .httpRequest(request)
                                 .build();
    }

    private SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context) {
        return new TraceIdExecutionInterceptor().modifyHttpRequest(context, new ExecutionAttributes());
    }

    private void resetRelevantEnvVars(EnvironmentVariableHelper env) {
        env.remove("AWS_LAMBDA_FUNCTION_NAME");
        env.remove("_X_AMZN_TRACE_ID");
    }
}