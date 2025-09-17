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

package software.amazon.awssdk.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.interceptor.TraceIdExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utilslite.SdkInternalThreadLocal;

/**
 * Verifies that the {@link TraceIdExecutionInterceptor} is actually wired up for AWS services.
 */
public class TraceIdTest {
    @Test
    public void traceIdInterceptorIsEnabled() {
        EnvironmentVariableHelper.run(env -> {
            env.set("AWS_LAMBDA_FUNCTION_NAME", "foo");
            env.set("_X_AMZN_TRACE_ID", "bar");

            try (MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
                 ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                                                                       .region(Region.US_WEST_2)
                                                                       .credentialsProvider(AnonymousCredentialsProvider.create())
                                                                       .httpClient(mockHttpClient)
                                                                       .build()) {
                mockHttpClient.stubNextResponse(HttpExecuteResponse.builder()
                                                                   .response(SdkHttpResponse.builder()
                                                                                            .statusCode(200)
                                                                                            .build())
                                                                   .responseBody(AbortableInputStream.create(new StringInputStream("{}")))
                                                                   .build());
                client.allTypes();
                assertThat(mockHttpClient.getLastRequest().firstMatchingHeader("X-Amzn-Trace-Id")).hasValue("bar");
            }
        });
    }

    @Test
    public void traceIdInterceptorPreservesTraceIdAcrossRetries() {
        EnvironmentVariableHelper.run(env -> {
            env.set("AWS_LAMBDA_FUNCTION_NAME", "foo");
            SdkInternalThreadLocal.put("AWS_LAMBDA_X_TRACE_ID", "SdkInternalThreadLocal-trace-123");

            try (MockAsyncHttpClient mockHttpClient = new MockAsyncHttpClient();
                 ProtocolRestJsonAsyncClient client = ProtocolRestJsonAsyncClient.builder()
                                                                                 .region(Region.US_WEST_2)
                                                                                 .credentialsProvider(AnonymousCredentialsProvider.create())
                                                                                 .httpClient(mockHttpClient)
                                                                                 .build()) {

                mockHttpClient.stubResponses(
                    HttpExecuteResponse.builder()
                                       .response(SdkHttpResponse.builder().statusCode(500).build())
                                       .responseBody(AbortableInputStream.create(new StringInputStream("{}")))
                                       .build(),
                    HttpExecuteResponse.builder()
                                       .response(SdkHttpResponse.builder().statusCode(500).build())
                                       .responseBody(AbortableInputStream.create(new StringInputStream("{}")))
                                       .build(),
                    HttpExecuteResponse.builder().response(SdkHttpResponse.builder().statusCode(200).build())
                                       .responseBody(AbortableInputStream.create(new StringInputStream("{}")))
                                       .build());

                client.allTypes().join();

                List<SdkHttpRequest> requests = mockHttpClient.getRequests();
                assertThat(requests).hasSize(3);

                assertThat(requests.get(0).firstMatchingHeader("X-Amzn-Trace-Id")).hasValue("SdkInternalThreadLocal-trace-123");
                assertThat(requests.get(1).firstMatchingHeader("X-Amzn-Trace-Id")).hasValue("SdkInternalThreadLocal-trace-123");
                assertThat(requests.get(2).firstMatchingHeader("X-Amzn-Trace-Id")).hasValue("SdkInternalThreadLocal-trace-123");

            } finally {
                SdkInternalThreadLocal.clear();
            }
        });
    }

    @Test
    public void traceIdInterceptorPreservesTraceIdAcrossChainedFutures() {
        EnvironmentVariableHelper.run(env -> {
            env.set("AWS_LAMBDA_FUNCTION_NAME", "foo");
            SdkInternalThreadLocal.put("AWS_LAMBDA_X_TRACE_ID", "SdkInternalThreadLocal-trace-123");

            try (MockAsyncHttpClient mockHttpClient = new MockAsyncHttpClient();
                 ProtocolRestJsonAsyncClient client = ProtocolRestJsonAsyncClient.builder()
                                                                                 .region(Region.US_WEST_2)
                                                                                 .credentialsProvider(AnonymousCredentialsProvider.create())
                                                                                 .httpClient(mockHttpClient)
                                                                                 .build()) {

                mockHttpClient.stubResponses(
                    HttpExecuteResponse.builder()
                                       .response(SdkHttpResponse.builder().statusCode(200).build())
                                       .responseBody(AbortableInputStream.create(new StringInputStream("{}")))
                                       .build(),
                    HttpExecuteResponse.builder()
                                       .response(SdkHttpResponse.builder().statusCode(200).build())
                                       .responseBody(AbortableInputStream.create(new StringInputStream("{}")))
                                       .build()
                );

                client.allTypes()
                      .thenRun(() -> {
                          client.allTypes().join();
                      })
                      .join();

                List<SdkHttpRequest> requests = mockHttpClient.getRequests();

                assertThat(requests).hasSize(2);

                assertThat(requests.get(0).firstMatchingHeader("X-Amzn-Trace-Id")).hasValue("SdkInternalThreadLocal-trace-123");
                assertThat(requests.get(1).firstMatchingHeader("X-Amzn-Trace-Id")).hasValue("SdkInternalThreadLocal-trace-123");

            } finally {
                SdkInternalThreadLocal.clear();
            }
        });
    }

    @Test
    public void traceIdInterceptorPreservesTraceIdAcrossExceptionallyCompletedFutures() {
        EnvironmentVariableHelper.run(env -> {
            env.set("AWS_LAMBDA_FUNCTION_NAME", "foo");
            SdkInternalThreadLocal.put("AWS_LAMBDA_X_TRACE_ID", "SdkInternalThreadLocal-trace-123");

            try (MockAsyncHttpClient mockHttpClient = new MockAsyncHttpClient();
                 ProtocolRestJsonAsyncClient client = ProtocolRestJsonAsyncClient.builder()
                                                                                 .region(Region.US_WEST_2)
                                                                                 .credentialsProvider(AnonymousCredentialsProvider.create())
                                                                                 .httpClient(mockHttpClient)
                                                                                 .build()) {

                mockHttpClient.stubResponses(
                    HttpExecuteResponse.builder()
                                       .response(SdkHttpResponse.builder().statusCode(400).build())
                                       .responseBody(AbortableInputStream.create(new StringInputStream("{}")))
                                       .build(),
                    HttpExecuteResponse.builder()
                                       .response(SdkHttpResponse.builder().statusCode(200).build())
                                       .responseBody(AbortableInputStream.create(new StringInputStream("{}")))
                                       .build()
                );

                client.allTypes()
                      .exceptionally(throwable -> {
                          client.allTypes().join();
                          return null;
                      }).join();

                List<SdkHttpRequest> requests = mockHttpClient.getRequests();

                assertThat(requests).hasSize(2);

                assertThat(requests.get(0).firstMatchingHeader("X-Amzn-Trace-Id")).hasValue("SdkInternalThreadLocal-trace-123");
                assertThat(requests.get(1).firstMatchingHeader("X-Amzn-Trace-Id")).hasValue("SdkInternalThreadLocal-trace-123");

            } finally {
                SdkInternalThreadLocal.clear();
            }
        });
    }

    @Test
    public void traceIdInterceptorPreservesTraceIdAcrossExceptionallyCompletedFuturesThrownInPreExecution() {
        EnvironmentVariableHelper.run(env -> {
            env.set("AWS_LAMBDA_FUNCTION_NAME", "foo");
            SdkInternalThreadLocal.put("AWS_LAMBDA_X_TRACE_ID", "SdkInternalThreadLocal-trace-123");

            ExecutionInterceptor throwingInterceptor = new ExecutionInterceptor() {
                private boolean hasThrown = false;

                @Override
                public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {
                    if (!hasThrown) {
                        hasThrown = true;
                        throw new RuntimeException("failing in pre execution");
                    }
                }
            };

            try (MockAsyncHttpClient mockHttpClient = new MockAsyncHttpClient();
                 ProtocolRestJsonAsyncClient client = ProtocolRestJsonAsyncClient.builder()
                                                                                 .region(Region.US_WEST_2)
                                                                                 .credentialsProvider(AnonymousCredentialsProvider.create())
                                                                                 .overrideConfiguration(o -> o.addExecutionInterceptor(throwingInterceptor))
                                                                                 .httpClient(mockHttpClient)
                                                                                 .build()) {

                mockHttpClient.stubResponses(
                    HttpExecuteResponse.builder()
                                       .response(SdkHttpResponse.builder().statusCode(200).build())
                                       .responseBody(AbortableInputStream.create(new StringInputStream("{}")))
                                       .build()
                );

                client.allTypes()
                      .exceptionally(throwable -> {
                          client.allTypes().join();
                          return null;
                      }).join();

                List<SdkHttpRequest> requests = mockHttpClient.getRequests();

                assertThat(requests).hasSize(1);
                assertThat(requests.get(0).firstMatchingHeader("X-Amzn-Trace-Id")).hasValue("SdkInternalThreadLocal-trace-123");

            } finally {
                SdkInternalThreadLocal.clear();
            }
        });
    }

    @Test
    public void traceIdInterceptorWithNewThreadInheritsTraceId() {
        EnvironmentVariableHelper.run(env -> {
            env.set("AWS_LAMBDA_FUNCTION_NAME", "foo");

            SdkInternalThreadLocal.put("AWS_LAMBDA_X_TRACE_ID", "SdkInternalThreadLocal-trace-123");

            try (MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
                 ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                                                                       .region(Region.US_WEST_2)
                                                                       .credentialsProvider(AnonymousCredentialsProvider.create())
                                                                       .httpClient(mockHttpClient)
                                                                       .build()) {

                mockHttpClient.stubNextResponse(HttpExecuteResponse.builder()
                                                                   .response(SdkHttpResponse.builder().statusCode(200).build())
                                                                   .responseBody(AbortableInputStream.create(new StringInputStream("{}")))
                                                                   .build());

                Thread childThread = new Thread(client::allTypes);
                childThread.start();
                childThread.join();

                List<SdkHttpRequest> requests = mockHttpClient.getRequests();
                assertThat(requests.get(0).firstMatchingHeader("X-Amzn-Trace-Id")).hasValue("SdkInternalThreadLocal-trace-123");

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                SdkInternalThreadLocal.clear();
            }
        });
    }

    @Test
    public void traceIdInterceptorWithExecutiveServicePreservesTraceId() {
        EnvironmentVariableHelper.run(env -> {
            env.set("AWS_LAMBDA_FUNCTION_NAME", "foo");

            SdkInternalThreadLocal.put("AWS_LAMBDA_X_TRACE_ID", "SdkInternalThreadLocal-trace-123");
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try (MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
                 ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                                                                       .region(Region.US_WEST_2)
                                                                       .credentialsProvider(AnonymousCredentialsProvider.create())
                                                                       .httpClient(mockHttpClient)
                                                                       .build()) {

                mockHttpClient.stubNextResponse(HttpExecuteResponse.builder()
                                                                   .response(SdkHttpResponse.builder().statusCode(200).build())
                                                                   .responseBody(AbortableInputStream.create(new StringInputStream("{}")))
                                                                   .build());

                executor.submit(() -> client.allTypes()).get();

                List<SdkHttpRequest> requests = mockHttpClient.getRequests();
                assertThat(requests.get(0).firstMatchingHeader("X-Amzn-Trace-Id")).hasValue("SdkInternalThreadLocal-trace-123");

            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            } finally {
                SdkInternalThreadLocal.clear();
            }
        });
    }

    @Test
    public void traceIdInterceptorWithRunAsyncDoesNotPreservesTraceId() throws Exception {
        EnvironmentVariableHelper.run(env -> {
            env.set("AWS_LAMBDA_FUNCTION_NAME", "foo");

            SdkInternalThreadLocal.put("AWS_LAMBDA_X_TRACE_ID", "SdkInternalThreadLocal-trace-123");
            try (MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
                 ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                                                                       .region(Region.US_WEST_2)
                                                                       .credentialsProvider(AnonymousCredentialsProvider.create())
                                                                       .httpClient(mockHttpClient)
                                                                       .build()) {

                mockHttpClient.stubNextResponse(HttpExecuteResponse.builder()
                                                                   .response(SdkHttpResponse.builder().statusCode(200).build())
                                                                   .responseBody(AbortableInputStream.create(new StringInputStream("{}")))
                                                                   .build());

                CompletableFuture.runAsync(client::allTypes).get();

                List<SdkHttpRequest> requests = mockHttpClient.getRequests();
                assertThat(requests.get(0).firstMatchingHeader("X-Amzn-Trace-Id")).isEmpty();

            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            } finally {
                SdkInternalThreadLocal.clear();
            }
        });
    }
}