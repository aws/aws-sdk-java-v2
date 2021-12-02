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

package software.amazon.awssdk.services.dynamodb;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbAsyncWaiter;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

public class WaitersUserAgentTest {

    @Rule
    public WireMockRule mockServer = new WireMockRule(options().notifier(new ConsoleNotifier(true)).dynamicHttpsPort());

    private DynamoDbClient dynamoDbClient;
    private DynamoDbAsyncClient dynamoDbAsyncClient;
    private ExecutionInterceptor interceptor;

    @Before
    public void setup() {
        interceptor = Mockito.spy(AbstractExecutionInterceptor.class);

        dynamoDbClient = DynamoDbClient.builder()
                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test",
                                                                                                                        "test")))
                                       .region(Region.US_WEST_2)
                                       .endpointOverride(URI.create("http://localhost:" + mockServer.port()))
                                       .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor))
                                       .build();

        dynamoDbAsyncClient = DynamoDbAsyncClient.builder()
                                                 .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials
                                                                                                           .create("test",
                                                                                                                   "test")))
                                                 .region(Region.US_WEST_2)
                                                 .endpointOverride(URI.create("http://localhost:" + mockServer.port()))
                                                 .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor))
                                                 .build();
    }

    @Test
    public void syncWaiters_shouldHaveWaitersUserAgent() {
        stubFor(any(urlEqualTo("/")).willReturn(aResponse().withStatus(500)));

        DynamoDbWaiter waiter = dynamoDbClient.waiter();
        assertThatThrownBy(() -> waiter.waitUntilTableExists(DescribeTableRequest.builder().tableName("table").build())).isNotNull();

        ArgumentCaptor<Context.BeforeTransmission> context = ArgumentCaptor.forClass(Context.BeforeTransmission.class);
        Mockito.verify(interceptor).beforeTransmission(context.capture(), Matchers.any());

        assertTrue(context.getValue().httpRequest().headers().get("User-Agent").toString().contains("waiter"));
    }

    @Test
    public void asyncWaiters_shouldHaveWaitersUserAgent() {
        DynamoDbAsyncWaiter waiter = dynamoDbAsyncClient.waiter();
        CompletableFuture<WaiterResponse<DescribeTableResponse>> responseFuture = waiter.waitUntilTableExists(DescribeTableRequest.builder().tableName("table").build());

        ArgumentCaptor<Context.BeforeTransmission> context = ArgumentCaptor.forClass(Context.BeforeTransmission.class);
        Mockito.verify(interceptor).beforeTransmission(context.capture(), Matchers.any());

        assertTrue(context.getValue().httpRequest().headers().get("User-Agent").toString().contains("waiter"));

        responseFuture.cancel(true);
    }

    public static abstract class AbstractExecutionInterceptor implements ExecutionInterceptor {
        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            throw new RuntimeException("Interrupting the request.");
        }
    }
}
