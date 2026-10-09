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

package software.amazon.awssdk.mapper.dynamodb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.SaveBehavior;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Proves that every DynamoDB request issued through the mapper carries the {@code DYNAMODB_MAPPER_V2} business metric
 * in its {@code User-Agent}. The client is built without any explicit interceptor, so the metric can only come from
 * the mapper's classpath {@code execution.interceptors} registration.
 */
@RunWith(Parameterized.class)
public class DynamoDBMapperUserAgentTest {

    private static final String TARGET_HEADER = "X-Amz-Target";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String LEGACY_ENHANCED_CLIENT_API_NAME = "hll/ddb-enh";
    private static final Pattern METRICS_SECTION = Pattern.compile("(?:^|\\s)m/([A-Za-z0-9+\\-,]+)");
    private static final String FIRST_PAGE_WITH_MORE =
        "{\"Count\":0,\"Items\":[],\"LastEvaluatedKey\":{\"id\":{\"S\":\"next\"}}}";

    interface MapperAction {
        void run(DynamoDBMapper mapper);
    }

    static final class Case {
        final String name;
        final int minRequests;
        final List<String> scriptedResponses;
        final MapperAction action;

        Case(String name, MapperAction action) {
            this(name, 1, Collections.emptyList(), action);
        }

        Case(String name, int minRequests, List<String> scriptedResponses, MapperAction action) {
            this.name = name;
            this.minRequests = minRequests;
            this.scriptedResponses = scriptedResponses;
            this.action = action;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Parameters(name = "{0}")
    public static List<Case> cases() {
        List<Case> c = new ArrayList<>();
        c.add(new Case("load", m -> m.load(Item.class, "k")));
        c.add(new Case("save-update", m -> m.save(item("k"))));
        c.add(new Case("save-put", m -> m.save(item("k"), SaveBehavior.PUT.config())));
        c.add(new Case("delete", m -> m.delete(item("k"))));
        c.add(new Case("query", m -> m.query(Item.class, queryExpression()).size()));
        c.add(new Case("queryPage", m -> m.queryPage(Item.class, queryExpression())));
        c.add(new Case("query-next-page", 2, Collections.singletonList(FIRST_PAGE_WITH_MORE),
                       m -> m.query(Item.class, queryExpression()).size()));
        c.add(new Case("count-query", m -> m.count(Item.class, queryExpression())));
        c.add(new Case("scan", m -> m.scan(Item.class, new DynamoDBScanExpression()).size()));
        c.add(new Case("scanPage", m -> m.scanPage(Item.class, new DynamoDBScanExpression())));
        c.add(new Case("scan-next-page", 2, Collections.singletonList(FIRST_PAGE_WITH_MORE),
                       m -> m.scan(Item.class, new DynamoDBScanExpression()).size()));
        c.add(new Case("count-scan", m -> m.count(Item.class, new DynamoDBScanExpression())));
        c.add(new Case("parallelScan", 2, Collections.emptyList(),
                       m -> m.parallelScan(Item.class, new DynamoDBScanExpression(), 2).size()));
        c.add(new Case("batchLoad", m -> m.batchLoad(Arrays.asList(item("a"), item("b")))));
        c.add(new Case("batchSave", m -> m.batchSave(Arrays.asList(item("a"), item("b")))));
        c.add(new Case("transactionWrite", m -> m.transactionWrite(new TransactionWriteRequest()
                                                                       .addPut(item("p"))
                                                                       .withIdempotencyToken("fixed-token"))));
        c.add(new Case("transactionLoad", m -> m.transactionLoad(new TransactionLoadRequest().addLoad(item("a")))));
        c.add(new Case("tableMapper-load", m -> {
            DynamoDBTableMapper<Item, String, Object> tableMapper = m.newTableMapper(Item.class);
            tableMapper.load("k");
        }));
        return c;
    }

    @Parameter
    public Case testCase;

    @Test
    public void mapperOperation_classpathInterceptorRegistered_userAgentContainsMapperV2Metric() {
        CapturingHttpClient httpClient = new CapturingHttpClient(testCase.scriptedResponses);
        try (DynamoDbClient client = DynamoDbClient.builder()
                                                   .httpClient(httpClient)
                                                   .region(Region.US_EAST_1)
                                                   .credentialsProvider(StaticCredentialsProvider.create(
                                                       AwsBasicCredentials.create("akid", "skid")))
                                                   .build()) {
            DynamoDBMapper mapper = DynamoDBMapper.builder().dynamoDbClient(client).build();

            testCase.action.run(mapper);
        }

        List<SdkHttpRequest> requests = httpClient.requests;
        assertThat(requests.size(), greaterThanOrEqualTo(testCase.minRequests));
        for (SdkHttpRequest request : requests) {
            String userAgent = request.firstMatchingHeader(USER_AGENT_HEADER)
                                      .orElseThrow(() -> new AssertionError("Request had no User-Agent header"));
            List<String> metrics = businessMetrics(userAgent);
            assertThat(userAgent, metrics, hasItem(BusinessMetricFeatureId.DYNAMODB_MAPPER_V2.value()));
            // The mapper's metric is its own ID, not the enhanced client's "d". (The only enhanced client on this
            // test classpath is the 2.16.x one DynamoDBLocal pulls in, which tags the legacy "hll/ddb-enh" instead.)
            assertThat(userAgent, metrics, not(hasItem(BusinessMetricFeatureId.DDB_MAPPER.value())));
            // That enhanced client's classpath interceptor must survive alongside the mapper's, which proves the two
            // interceptors are not collapsed by ClasspathInterceptorChainFactory's simple-class-name de-duplication.
            assertThat(userAgent, containsString(LEGACY_ENHANCED_CLIENT_API_NAME));
        }
    }

    private static List<String> businessMetrics(String userAgent) {
        Matcher matcher = METRICS_SECTION.matcher(userAgent);
        if (!matcher.find()) {
            return Collections.emptyList();
        }
        return Arrays.asList(matcher.group(1).split(","));
    }

    private static DynamoDBQueryExpression<Item> queryExpression() {
        return new DynamoDBQueryExpression<Item>().withHashKeyValues(item("k"));
    }

    private static Item item(String id) {
        Item item = new Item();
        item.setId(id);
        item.setValue("v");
        return item;
    }

    /**
     * Records each outgoing HTTP request and answers it with a minimal 200 JSON body. Scripted bodies are served
     * first, in order; afterwards Scan and Query get {@code {"Count":0}} (the mapper's count loop unboxes it) and every
     * other operation gets {@code {}}, which the mapper treats as a complete, empty result.
     */
    private static final class CapturingHttpClient implements SdkHttpClient {
        private final List<SdkHttpRequest> requests = new CopyOnWriteArrayList<>();
        private final Deque<String> scriptedResponses;

        CapturingHttpClient(List<String> scriptedResponses) {
            this.scriptedResponses = new ArrayDeque<>(scriptedResponses);
        }

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            SdkHttpRequest httpRequest = request.httpRequest();
            requests.add(httpRequest);
            String body = responseBody(httpRequest);
            return new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() {
                    return HttpExecuteResponse.builder()
                                              .response(SdkHttpResponse.builder().statusCode(200).build())
                                              .responseBody(AbortableInputStream.create(
                                                  new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))))
                                              .build();
                }

                @Override
                public void abort() {
                }
            };
        }

        private synchronized String responseBody(SdkHttpRequest httpRequest) {
            String scripted = scriptedResponses.poll();
            if (scripted != null) {
                return scripted;
            }
            String target = httpRequest.firstMatchingHeader(TARGET_HEADER).orElse("");
            if (target.endsWith(".Scan") || target.endsWith(".Query")) {
                return "{\"Count\":0}";
            }
            return "{}";
        }

        @Override
        public String clientName() {
            return "CapturingHttpClient";
        }

        @Override
        public void close() {
        }
    }

    @DynamoDBTable(tableName = "UserAgentTestTable")
    public static class Item {
        private String id;
        private String value;

        public Item() {
        }

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
