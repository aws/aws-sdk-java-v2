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

package software.amazon.awssdk.benchmark.enhanced.dynamodb;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.mapper.dynamodb.internal.ApplyMapperUserAgentInterceptor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * Direct cost of the v2 mapper's {@link ApplyMapperUserAgentInterceptor#modifyRequest}, which rebuilds each request via
 * {@code toBuilder().overrideConfiguration(...)}. {@code baseline} returns the request unchanged and measures the
 * harness cost.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@State(Scope.Benchmark)
public class V2MapperApplyUserAgentInterceptorBenchmark {
    private static final V2MapperItemFactory ITEM_FACTORY = new V2MapperItemFactory();

    private final ApplyMapperUserAgentInterceptor interceptor = new ApplyMapperUserAgentInterceptor();
    private final ExecutionAttributes executionAttributes = new ExecutionAttributes();

    @Param({"GET_ITEM", "PUT_ITEM_TINY", "PUT_ITEM_HUGE", "PUT_ITEM_HUGE_FLAT"})
    private RequestType requestType;

    /**
     * Whether the request already carries an override configuration (e.g. a caller-supplied API name), which makes the
     * interceptor rebuild that configuration too.
     */
    @Param({"false", "true"})
    private boolean existingOverrideConfiguration;

    private Context.ModifyRequest context;

    @Setup
    public void setup() {
        DynamoDbRequest request = requestType.request();
        if (existingOverrideConfiguration) {
            request = (DynamoDbRequest) request.toBuilder()
                                               .overrideConfiguration(AwsRequestOverrideConfiguration
                                                                          .builder()
                                                                          .addApiName(ApiName.builder()
                                                                                             .name("caller")
                                                                                             .version("1")
                                                                                             .build())
                                                                          .build())
                                               .build();
        }
        context = InterceptorContext.builder().request(request).build();
    }

    @Benchmark
    public SdkRequest baseline() {
        return context.request();
    }

    @Benchmark
    public SdkRequest modifyRequest() {
        return interceptor.modifyRequest(context, executionAttributes);
    }

    public enum RequestType {
        GET_ITEM {
            @Override
            DynamoDbRequest request() {
                return GetItemRequest.builder()
                                     .tableName("table")
                                     .key(ITEM_FACTORY.tiny())
                                     .build();
            }
        },
        PUT_ITEM_TINY {
            @Override
            DynamoDbRequest request() {
                return putItem(ITEM_FACTORY.tiny());
            }
        },
        PUT_ITEM_HUGE {
            @Override
            DynamoDbRequest request() {
                return putItem(ITEM_FACTORY.huge());
            }
        },
        PUT_ITEM_HUGE_FLAT {
            @Override
            DynamoDbRequest request() {
                return putItem(ITEM_FACTORY.hugeFlat());
            }
        };

        abstract DynamoDbRequest request();

        private static PutItemRequest putItem(Map<String, AttributeValue> item) {
            return PutItemRequest.builder().tableName("table").item(item).build();
        }
    }
}
