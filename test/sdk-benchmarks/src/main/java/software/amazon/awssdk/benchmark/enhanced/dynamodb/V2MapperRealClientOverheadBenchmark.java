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

import static software.amazon.awssdk.core.client.config.SdkClientOption.CLIENT_ENDPOINT_PROVIDER;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Map;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.benchmark.utils.MockHttpClient;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.transform.PutItemRequestMarshaller;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Drives the v2 mapper through a real {@link DynamoDbClient} backed by a mocked {@code SdkHttpClient}, so the full
 * client pipeline runs on every call, including the user agent stage that turns the mapper's per-request
 * {@code DYNAMODB_MAPPER_V2} tag into the {@code User-Agent} business metric. The {@code V2TestDynamoDb*Client} stubs
 * used by the V1 mapper comparison benchmarks implement {@link DynamoDbClient} directly and bypass that pipeline.
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@State(Scope.Benchmark)
public class V2MapperRealClientOverheadBenchmark {
    private static final AwsJsonProtocolFactory JSON_PROTOCOL_FACTORY = AwsJsonProtocolFactory
            .builder()
            .clientConfiguration(SdkClientConfiguration.builder()
                    .option(CLIENT_ENDPOINT_PROVIDER,
                            ClientEndpointProvider.forEndpointOverride(URI.create("https://dynamodb.amazonaws.com")))
                    .build())
            .defaultServiceExceptionSupplier(DynamoDbException::builder)
            .protocol(AwsJsonProtocol.AWS_JSON)
            .protocolVersion("1.0")
            .build();

    private static final PutItemRequestMarshaller PUT_ITEM_REQUEST_MARSHALLER =
            new PutItemRequestMarshaller(JSON_PROTOCOL_FACTORY);

    private static final V2MapperItemFactory ITEM_FACTORY = new V2MapperItemFactory();

    private static final DynamoDBMapperConfig PUT_CONFIG =
            DynamoDBMapperConfig.builder()
                                .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.PUT)
                                .build();

    @Benchmark
    public Object v2MapperGet(TestState s) {
        return s.getMapper.load(s.testItem.bean.getClass(), "hashKey");
    }

    @Benchmark
    public void v2MapperPut(TestState s) {
        s.putMapper.save(s.testItem.bean);
    }

    @State(Scope.Benchmark)
    public static class TestState {
        @Param({"TINY", "SMALL", "HUGE", "HUGE_FLAT"})
        private TestItem testItem;

        private DynamoDBMapper getMapper;
        private DynamoDBMapper putMapper;

        @Setup
        public void setup() {
            getMapper = new DynamoDBMapper(client(testItem.getResponseContent));
            putMapper = new DynamoDBMapper(client("{}"), PUT_CONFIG);
        }

        private static DynamoDbClient client(String responseContent) {
            return DynamoDbClient.builder()
                                 .region(Region.US_WEST_2)
                                 .credentialsProvider(StaticCredentialsProvider.create(
                                     AwsBasicCredentials.create("akid", "skid")))
                                 .httpClient(new MockHttpClient(responseContent, "{}"))
                                 .build();
        }
    }

    public enum TestItem {
        TINY(ITEM_FACTORY.tiny(), ITEM_FACTORY.v2MapperTinyBean()),
        SMALL(ITEM_FACTORY.small(), ITEM_FACTORY.v2MapperSmallBean()),
        HUGE(ITEM_FACTORY.huge(), ITEM_FACTORY.v2MapperHugeBean()),
        HUGE_FLAT(ITEM_FACTORY.hugeFlat(), ITEM_FACTORY.v2MapperHugeBeanFlat());

        private final String getResponseContent;
        private final Object bean;

        TestItem(Map<String, AttributeValue> item, Object bean) {
            // A GetItem response has the same {"Item": {...}} shape as a marshalled PutItem request.
            this.getResponseContent = marshall(item);
            this.bean = bean;
        }
    }

    private static String marshall(Map<String, AttributeValue> item) {
        return PUT_ITEM_REQUEST_MARSHALLER.marshall(PutItemRequest.builder().item(item).build())
                .contentStreamProvider().map(cs -> {
                    try {
                        return IoUtils.toUtf8String(cs.newStream());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }).orElse(null);
    }
}
