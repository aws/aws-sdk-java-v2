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

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.awscore.endpoints.AccountIdEndpointMode;
import software.amazon.awssdk.core.rules.testing.AsyncTestCase;
import software.amazon.awssdk.core.rules.testing.BaseRuleSetClientTest;
import software.amazon.awssdk.core.rules.testing.model.Expect;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;

/**
 * TODO
 * This class represents functional accountId endpoint routing tests. These tests are primarily intended to be
 * used during implementation and testing of this feature. Once the accountId rules are released, this test
 * class can be removed, because it should offer no additional functionality from the generated client
 * endpoint tests.
 */
class AccountIdEndpointRoutingTest extends BaseRuleSetClientTest {

    private static final String ACCOUNT_ID = "012345678999";

    @MethodSource("asyncTestCases")
    @ParameterizedTest
    void asyncClient_usesCorrectEndpoint(AsyncTestCase tc) {
        runAndVerify(tc);
    }

    private static List<AsyncTestCase> asyncTestCases() {
        return Arrays
            .asList(
                new AsyncTestCase(
                    "For region us-east-1 with account ID, default endpoint mode",
                    () -> {
                        DynamoDbAsyncClientBuilder builder =
                            DynamoDbAsyncClient.builder()
                                               .credentialsProvider(credentialsWithAccountId())
                                               .httpClient(getAsyncHttpClient())
                                               .region(Region.of("us-east-1"));
                        return builder.build().listTables(ListTablesRequest.builder().build());
                    }, Expect
                        .builder()
                        .endpoint(Endpoint.builder().url(
                            URI.create(String.format("https://%s.ddb.us-east-1.amazonaws.com", ACCOUNT_ID))).build())
                        .build()),
                new AsyncTestCase(
                    "For region us-east-1 with account ID, default endpoint mode explicitly configured",
                    () -> {
                        DynamoDbAsyncClientBuilder builder =
                            DynamoDbAsyncClient.builder()
                                               .credentialsProvider(credentialsWithAccountId())
                                               .accountIdEndpointMode(AccountIdEndpointMode.PREFERRED)
                                               .httpClient(getAsyncHttpClient())
                                               .region(Region.of("us-east-1"));
                        return builder.build().listTables(ListTablesRequest.builder().build());
                    }, Expect
                        .builder()
                        .endpoint(Endpoint.builder().url(
                            URI.create(String.format("https://%s.ddb.us-east-1.amazonaws.com", ACCOUNT_ID))).build())
                        .build()),
                new AsyncTestCase( //TODO: Incorrect. There should be no account id specific endpoints when mode is disabled
                    "For region us-east-1 with account ID, endpoint mode disabled",
                    () -> {
                        DynamoDbAsyncClientBuilder builder =
                            DynamoDbAsyncClient.builder()
                                               .credentialsProvider(credentialsWithAccountId())
                                               .accountIdEndpointMode(AccountIdEndpointMode.DISABLED)
                                               .httpClient(getAsyncHttpClient())
                                               .region(Region.of("us-east-1"));
                        return builder.build().listTables(ListTablesRequest.builder().build());
                    }, Expect
                        .builder()
                        .endpoint(Endpoint.builder().url(
                            URI.create(String.format("https://%s.ddb.us-east-1.amazonaws.com", ACCOUNT_ID))).build())
                        .build()),
                new AsyncTestCase(
                   "For region us-east-1 without account ID, default endpoint mode",
                   () -> {
                       DynamoDbAsyncClientBuilder builder =
                           DynamoDbAsyncClient.builder()
                                              .credentialsProvider(credentialsWithoutAccountId())
                                              .httpClient(getAsyncHttpClient())
                                              .region(Region.of("us-east-1"));
                       return builder.build().listTables(ListTablesRequest.builder().build());
                   }, Expect
                       .builder()
                       .endpoint(Endpoint.builder().url(URI.create("https://dynamodb.us-east-1.amazonaws.com")).build())
                       .build()),
                new AsyncTestCase( //TODO: Verify expected behavior
                   "For region us-east-1 without account ID, endpoint mode required",
                   () -> {
                       DynamoDbAsyncClientBuilder builder =
                           DynamoDbAsyncClient.builder()
                                              .credentialsProvider(credentialsWithoutAccountId())
                                              .accountIdEndpointMode(AccountIdEndpointMode.REQUIRED)
                                              .httpClient(getAsyncHttpClient())
                                              .region(Region.of("us-east-1"));
                       return builder.build().listTables(ListTablesRequest.builder().build());
                   }, Expect
                       .builder()
                       .endpoint(Endpoint.builder().url(URI.create("https://dynamodb.us-east-1.amazonaws.com")).build())
                       .build())
            );
    }

    private static AwsCredentialsProvider credentialsWithAccountId() {
        return () -> AwsSessionCredentials.builder()
                                          .accessKeyId("akid")
                                          .secretAccessKey("skid")
                                          .sessionToken("token")
                                          .accountId(ACCOUNT_ID)
                                          .build();
    }

    private static AwsCredentialsProvider credentialsWithoutAccountId() {
        return () -> AwsSessionCredentials.builder()
                                          .accessKeyId("akid")
                                          .secretAccessKey("skid")
                                          .sessionToken("token")
                                          .build();
    }

}
