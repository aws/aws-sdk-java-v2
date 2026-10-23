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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.BatchLoadRetryStrategy;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.BatchWriteRetryStrategy;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.PaginationLoadingStrategy;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.SaveBehavior;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * Proves that the mapper tags the requests it sends, and only those, with the {@code DYNAMODB_MAPPER_V2} business
 * metric in the {@code User-Agent}. The client has no user agent override and no interceptor that touches the user
 * agent, so the metric can only come from the mapper's per-request tagging.
 */
@RunWith(Enclosed.class)
public class DynamoDBMapperUserAgentTest {

    private static final String TARGET_HEADER = "X-Amz-Target";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String TABLE_NAME = "UserAgentTestTable";
    private static final Pattern METRICS_SECTION = Pattern.compile("(?:^|\\s)m/([A-Za-z0-9+\\-,]+)");
    private static final String FIRST_PAGE_WITH_MORE =
        "{\"Count\":0,\"Items\":[],\"LastEvaluatedKey\":{\"id\":{\"S\":\"next\"}}}";
    private static final String BATCH_WRITE_WITH_UNPROCESSED_ITEMS =
        "{\"UnprocessedItems\":{\"" + TABLE_NAME + "\":[{\"PutRequest\":{\"Item\":{\"id\":{\"S\":\"a\"}}}}]}}";
    private static final String BATCH_GET_WITH_UNPROCESSED_KEYS =
        "{\"Responses\":{},\"UnprocessedKeys\":{\"" + TABLE_NAME + "\":{\"Keys\":[{\"id\":{\"S\":\"a\"}}]}}}";

    /**
     * Every operation the mapper exposes sends only requests that carry the mapper's metric, including later pages,
     * parallel scan segments and retries of unprocessed batch items.
     */
    @RunWith(Parameterized.class)
    public static class MapperOperations {

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
            c.add(new Case("count-query-next-page", 2, Collections.singletonList(FIRST_PAGE_WITH_MORE),
                           m -> m.count(Item.class, queryExpression())));
            c.add(new Case("scan", m -> m.scan(Item.class, new DynamoDBScanExpression()).size()));
            c.add(new Case("scanPage", m -> m.scanPage(Item.class, new DynamoDBScanExpression())));
            c.add(new Case("scan-next-page", 2, Collections.singletonList(FIRST_PAGE_WITH_MORE),
                           m -> m.scan(Item.class, new DynamoDBScanExpression()).size()));
            c.add(new Case("count-scan", m -> m.count(Item.class, new DynamoDBScanExpression())));
            c.add(new Case("count-scan-next-page", 2, Collections.singletonList(FIRST_PAGE_WITH_MORE),
                           m -> m.count(Item.class, new DynamoDBScanExpression())));
            c.add(new Case("parallelScan", 2, Collections.emptyList(),
                           m -> m.parallelScan(Item.class, new DynamoDBScanExpression(), 2).size()));
            c.add(new Case("parallelScan-next-page", 3, Collections.singletonList(FIRST_PAGE_WITH_MORE),
                           m -> m.parallelScan(Item.class, new DynamoDBScanExpression(), 2).size()));
            c.add(new Case("batchLoad", m -> m.batchLoad(Arrays.asList(item("a"), item("b")))));
            c.add(new Case("batchLoad-unprocessed-retry", 2, Collections.singletonList(BATCH_GET_WITH_UNPROCESSED_KEYS),
                           m -> m.batchLoad(Arrays.asList(item("a"), item("b")), immediateRetryConfig())));
            c.add(new Case("batchSave", m -> m.batchSave(Arrays.asList(item("a"), item("b")))));
            c.add(new Case("batchDelete", m -> m.batchDelete(Arrays.asList(item("a"), item("b")))));
            c.add(new Case("batchSave-unprocessed-retry", 2,
                           Collections.singletonList(BATCH_WRITE_WITH_UNPROCESSED_ITEMS),
                           m -> m.batchWrite(Arrays.asList(item("a"), item("b")), Collections.emptyList(),
                                             immediateRetryConfig())));
            c.add(new Case("transactionWrite", m -> m.transactionWrite(new TransactionWriteRequest()
                                                                           .addPut(item("p"))
                                                                           .addDelete(item("d"))
                                                                           .withIdempotencyToken("fixed-token"))));
            c.add(new Case("transactionLoad",
                           m -> m.transactionLoad(new TransactionLoadRequest().addLoad(item("a")))));
            c.add(new Case("tableMapper-load", m -> {
                DynamoDBTableMapper<Item, String, Object> tableMapper = m.newTableMapper(Item.class);
                tableMapper.load("k");
            }));
            return c;
        }

        @Parameter
        public Case testCase;

        @Test
        public void mapperOperation_sentThroughMapper_userAgentContainsMapperV2Metric() {
            CapturingHttpClient httpClient = new CapturingHttpClient(testCase.scriptedResponses);
            try (DynamoDbClient client = client(httpClient)) {
                DynamoDBMapper mapper = DynamoDBMapper.builder().dynamoDbClient(client).build();

                testCase.action.run(mapper);
            }

            List<SdkHttpRequest> requests = httpClient.requests;
            assertThat(requests.size(), greaterThanOrEqualTo(testCase.minRequests));
            for (SdkHttpRequest request : requests) {
                String userAgent = userAgent(request);
                List<String> metrics = businessMetrics(userAgent);
                assertThat(userAgent, metrics, hasItem(BusinessMetricFeatureId.DYNAMODB_MAPPER_V2.value()));
                // The mapper's metric is its own ID, not the enhanced client's "d".
                assertThat(userAgent, metrics, not(hasItem(BusinessMetricFeatureId.DDB_MAPPER.value())));
            }
        }
    }

    /**
     * Requests the mapper does not send carry no mapper metric, even on a client the mapper also uses.
     */
    public static class DirectClientCalls {

        @Test
        public void directClientCall_clientSharedWithMapper_userAgentDoesNotContainMapperV2Metric() {
            CapturingHttpClient httpClient = new CapturingHttpClient(Collections.emptyList());
            try (DynamoDbClient client = client(httpClient)) {
                DynamoDBMapper mapper = DynamoDBMapper.builder().dynamoDbClient(client).build();

                mapper.load(Item.class, "k");
                client.getItem(r -> r.tableName(TABLE_NAME).key(key("k")));
                client.query(r -> r.tableName(TABLE_NAME));
                mapper.load(Item.class, "k");
            }

            List<SdkHttpRequest> requests = httpClient.requests;
            assertThat(requests, hasSize(4));
            String mapperV2 = BusinessMetricFeatureId.DYNAMODB_MAPPER_V2.value();
            assertThat(businessMetrics(userAgent(requests.get(0))), hasItem(mapperV2));
            for (SdkHttpRequest direct : requests.subList(1, 3)) {
                String userAgent = userAgent(direct);
                List<String> metrics = businessMetrics(userAgent);
                // A non-empty m/ section shows the parse found the metrics, so the absence below is meaningful.
                assertThat(userAgent, metrics, is(not(empty())));
                assertThat(userAgent, metrics, not(hasItem(mapperV2)));
            }
            assertThat(businessMetrics(userAgent(requests.get(3))), hasItem(mapperV2));
        }
    }

    /**
     * A caller can build a paginated list from its own request. Later pages keep the caller's override configuration
     * and gain the mapper's metric exactly once, however many pages are fetched.
     */
    public static class CallerBuiltPaginatedLists {

        private static final ApiName CALLER_API_NAME = ApiName.builder().name("caller-lib").version("1.0").build();
        private static final Duration CALLER_API_CALL_TIMEOUT = Duration.ofSeconds(42);
        private static final String CALLER_HEADER = "x-caller-header";
        private static final String CALLER_HEADER_VALUE = "kept";
        private static final ApiName MAPPER_API_NAME =
            ApiName.builder().name("sdk-metrics").version(BusinessMetricFeatureId.DYNAMODB_MAPPER_V2.value()).build();

        @Test
        public void paginatedQueryList_callerOverrideConfiguration_laterPagesKeepItAndAddMapperV2MetricOnce() {
            QueryRequest callerRequest = QueryRequest.builder()
                                                     .tableName(TABLE_NAME)
                                                     .overrideConfiguration(CallerBuiltPaginatedLists::callerOverride)
                                                     .build();
            QueryResponse firstPage = QueryResponse.builder().items(Collections.emptyList())
                                                   .lastEvaluatedKey(key("first")).build();

            assertLaterPagesKeepCallerOverride((mapper, client) -> new PaginatedQueryList<>(
                mapper, Item.class, client, callerRequest, firstPage, PaginationLoadingStrategy.LAZY_LOADING,
                DynamoDBMapperConfig.DEFAULT).size());
        }

        @Test
        public void paginatedScanList_callerOverrideConfiguration_laterPagesKeepItAndAddMapperV2MetricOnce() {
            ScanRequest callerRequest = ScanRequest.builder()
                                                   .tableName(TABLE_NAME)
                                                   .overrideConfiguration(CallerBuiltPaginatedLists::callerOverride)
                                                   .build();
            ScanResponse firstPage = ScanResponse.builder().items(Collections.emptyList())
                                                 .lastEvaluatedKey(key("first")).build();

            assertLaterPagesKeepCallerOverride((mapper, client) -> new PaginatedScanList<>(
                mapper, Item.class, client, callerRequest, firstPage, PaginationLoadingStrategy.LAZY_LOADING,
                DynamoDBMapperConfig.DEFAULT).size());
        }

        private static void callerOverride(AwsRequestOverrideConfiguration.Builder o) {
            o.apiCallTimeout(CALLER_API_CALL_TIMEOUT)
             .addApiName(CALLER_API_NAME)
             .putHeader(CALLER_HEADER, CALLER_HEADER_VALUE);
        }

        private static void assertLaterPagesKeepCallerOverride(PaginatedListAction iterateAllPages) {
            // Page 2 has more results and page 3 is the last, so the list sends two requests derived page by page.
            CapturingHttpClient httpClient = new CapturingHttpClient(Collections.singletonList(FIRST_PAGE_WITH_MORE));
            List<SdkRequest> sdkRequests = new CopyOnWriteArrayList<>();
            ExecutionInterceptor capture = new ExecutionInterceptor() {
                @Override
                public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
                    sdkRequests.add(context.request());
                }
            };
            try (DynamoDbClient client = client(httpClient, capture)) {
                DynamoDBMapper mapper = DynamoDBMapper.builder().dynamoDbClient(client).build();

                iterateAllPages.run(mapper, client);
            }

            assertThat(sdkRequests, hasSize(2));
            for (SdkRequest sdkRequest : sdkRequests) {
                AwsRequestOverrideConfiguration override =
                    ((AwsRequest) sdkRequest).overrideConfiguration()
                                             .orElseThrow(() -> new AssertionError("Request lost its override"));
                assertThat(override.apiCallTimeout(), is(Optional.of(CALLER_API_CALL_TIMEOUT)));
                assertThat(override.headers().get(CALLER_HEADER), contains(CALLER_HEADER_VALUE));
                assertThat(override.apiNames(), hasItem(CALLER_API_NAME));
                assertThat(Collections.frequency(override.apiNames(), MAPPER_API_NAME), is(1));
            }
            assertThat(httpClient.requests, hasSize(2));
            for (SdkHttpRequest request : httpClient.requests) {
                String userAgent = userAgent(request);
                assertThat(userAgent, businessMetrics(userAgent),
                           hasItem(BusinessMetricFeatureId.DYNAMODB_MAPPER_V2.value()));
                assertThat(userAgent, containsString("caller-lib/1.0"));
                assertThat(request.firstMatchingHeader(CALLER_HEADER), is(Optional.of(CALLER_HEADER_VALUE)));
            }
        }

        interface PaginatedListAction {
            void run(DynamoDBMapper mapper, DynamoDbClient client);
        }
    }

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

    private static DynamoDbClient client(SdkHttpClient httpClient, ExecutionInterceptor... interceptors) {
        return DynamoDbClient.builder()
                             .httpClient(httpClient)
                             .region(Region.US_EAST_1)
                             .credentialsProvider(StaticCredentialsProvider.create(
                                 AwsBasicCredentials.create("akid", "skid")))
                             .overrideConfiguration(o -> o.executionInterceptors(Arrays.asList(interceptors)))
                             .build();
    }

    private static String userAgent(SdkHttpRequest request) {
        return request.firstMatchingHeader(USER_AGENT_HEADER)
                      .orElseThrow(() -> new AssertionError("Request had no User-Agent header"));
    }

    private static List<String> businessMetrics(String userAgent) {
        Matcher matcher = METRICS_SECTION.matcher(userAgent);
        if (!matcher.find()) {
            return Collections.emptyList();
        }
        return Arrays.asList(matcher.group(1).split(","));
    }

    /**
     * Retries unprocessed batch items once, without the default backoff delay.
     */
    private static DynamoDBMapperConfig immediateRetryConfig() {
        return DynamoDBMapperConfig.builder()
                                   .withBatchWriteRetryStrategy(new BatchWriteRetryStrategy() {
                                       @Override
                                       public int getMaxRetryOnUnprocessedItems(
                                           Map<String, List<WriteRequest>> batchWriteItemInput) {
                                           return 1;
                                       }

                                       @Override
                                       public long getDelayBeforeRetryUnprocessedItems(
                                           Map<String, List<WriteRequest>> unprocessedItems, int retriesAttempted) {
                                           return 0;
                                       }
                                   })
                                   .withBatchLoadRetryStrategy(new BatchLoadRetryStrategy() {
                                       @Override
                                       public boolean shouldRetry(BatchLoadContext context) {
                                           return !context.getBatchGetItemResult().unprocessedKeys().isEmpty()
                                                  && context.getRetriesAttempted() < 1;
                                       }

                                       @Override
                                       public long getDelayBeforeNextRetry(BatchLoadContext context) {
                                           return 0;
                                       }
                                   })
                                   .build();
    }

    private static DynamoDBQueryExpression<Item> queryExpression() {
        return new DynamoDBQueryExpression<Item>().withHashKeyValues(item("k"));
    }

    private static Map<String, AttributeValue> key(String id) {
        return Collections.singletonMap("id", AttributeValue.builder().s(id).build());
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

    @DynamoDBTable(tableName = TABLE_NAME)
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
