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

package software.amazon.awssdk.benchmark.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.protocols.json.internal.AwsStructuredPlainJsonFactory;
import software.amazon.awssdk.protocols.json.internal.marshall.JsonProtocolMarshallerBuilder;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonProtocolUnmarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

/**
 * Isolated ser/de benchmark for V2 DynamoDB (JSON protocol).
 * Measures only JSON parsing + object construction -- no HTTP, signing, or retries.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
public class V2JsonProtocolBenchmark {

    private static final String CONTENT_TYPE = "application/x-amz-json-1.0";
    private static final URI ENDPOINT = URI.create("http://localhost/");
    private static final OperationInfo OP_INFO = OperationInfo.builder()
                                                              .httpMethod(SdkHttpMethod.POST)
                                                              .hasImplicitPayloadMembers(true)
                                                              .build();

    private static final AwsJsonProtocolMetadata METADATA = AwsJsonProtocolMetadata.builder()
                                                                                   .protocol(AwsJsonProtocol.AWS_JSON)
                                                                                   .contentType(CONTENT_TYPE)
                                                                                   .build();

    private JsonProtocolUnmarshaller unmarshaller;
    private byte[] putItemResponseBytes;
    private PutItemRequest putItemRequest;

    @Setup
    public void setup() throws Exception {
        unmarshaller = JsonProtocolUnmarshaller.builder()
            .enableFastUnmarshalling(true)
            .protocolUnmarshallDependencies(JsonProtocolUnmarshaller.defaultProtocolUnmarshallDependencies())
            .build();

        putItemResponseBytes = loadFixture("fixtures/json-protocol/putitem-response.json");
        putItemRequest = PutItemRequest.builder()
            .tableName("benchmark-table")
            .item(itemMap())
            .build();
    }

    @Benchmark
    public void putItemDeser(Blackhole bh) throws Exception {
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
            .statusCode(200)
            .putHeader("Content-Type", CONTENT_TYPE)
            .content(AbortableInputStream.create(new ByteArrayInputStream(putItemResponseBytes)))
            .build();

        bh.consume(unmarshaller.unmarshall(PutItemResponse.builder(), response));
    }

    @Benchmark
    public void putItemSer(Blackhole bh) {
        ProtocolMarshaller<SdkHttpFullRequest> marshaller = JsonProtocolMarshallerBuilder.create()
                .endpoint(ENDPOINT)
                .jsonGenerator(AwsStructuredPlainJsonFactory.SDK_JSON_FACTORY
                    .createWriter(CONTENT_TYPE))
                .contentType(CONTENT_TYPE)
                .operationInfo(OP_INFO)
                .sendExplicitNullForPayload(false)
                .protocolMetadata(METADATA)
                .build();

        bh.consume(marshaller.marshall(putItemRequest));
    }

    private static Map<String, AttributeValue> itemMap() {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("pk", AttributeValue.fromS("benchmark-key"));
        item.put("sk", AttributeValue.fromN("100"));
        item.put("stringField", AttributeValue.fromS("test-value"));
        item.put("numberField", AttributeValue.fromN("123.456"));
        item.put("binaryField", AttributeValue.fromB(
            SdkBytes.fromByteArray("hello world".getBytes())));
        item.put("stringSetField", AttributeValue.builder()
            .ss("value1", "value2", "value3").build());
        item.put("numberSetField", AttributeValue.builder()
            .ns("1.1", "2.2", "3.3").build());
        item.put("boolField", AttributeValue.fromBool(false));
        item.put("nullField", AttributeValue.builder().nul(true).build());
        Map<String, AttributeValue> deep = new HashMap<String, AttributeValue>();
        deep.put("level2", AttributeValue.fromN("999"));
        Map<String, AttributeValue> nested = new HashMap<String, AttributeValue>();
        nested.put("nested", AttributeValue.fromS("nested-value"));
        nested.put("deepNested", AttributeValue.fromM(deep));
        item.put("mapField", AttributeValue.fromM(nested));
        item.put("listField", AttributeValue.builder().l(
            AttributeValue.fromS("item1"),
            AttributeValue.fromN("42"),
            AttributeValue.fromBool(true),
            AttributeValue.builder().nul(true).build()).build());
        return item;
    }

    private static byte[] loadFixture(String path) throws IOException {
        return software.amazon.awssdk.utils.IoUtils.toByteArray(
            V2JsonProtocolBenchmark.class.getClassLoader().getResourceAsStream(path));
    }
}
