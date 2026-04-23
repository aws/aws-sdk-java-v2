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

import com.amazonaws.http.HttpResponse;
import com.amazonaws.protocol.json.JsonClientMetadata;
import com.amazonaws.protocol.json.SdkJsonProtocolFactory;
import com.amazonaws.protocol.json.SdkStructuredPlainJsonFactory;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.transform.PutItemRequestProtocolMarshaller;
import com.amazonaws.services.dynamodbv2.model.transform.PutItemResultJsonUnmarshaller;
import com.amazonaws.transform.JsonUnmarshallerContextImpl;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
public class V1JsonProtocolBenchmark {

    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    private byte[] putItemResponseBytes;
    private HttpResponse httpResponse;
    private SdkJsonProtocolFactory protocolFactory;
    private PutItemRequest putItemRequest;

    @Setup
    public void setup() throws Exception {
        putItemResponseBytes = loadFixture("fixtures/json-protocol/putitem-response.json");
        httpResponse = new HttpResponse(null, null);
        httpResponse.setStatusCode(200);
        protocolFactory = new SdkJsonProtocolFactory(
            new JsonClientMetadata().withProtocolVersion("1.0"));
        putItemRequest = new PutItemRequest()
            .withTableName("benchmark-table")
            .withItem(itemMap());
    }

    @Benchmark
    public void putItemDeser(Blackhole bh) throws Exception {
        JsonParser parser = JSON_FACTORY.createParser(new ByteArrayInputStream(putItemResponseBytes));
        JsonUnmarshallerContextImpl ctx = new JsonUnmarshallerContextImpl(parser,
                                            SdkStructuredPlainJsonFactory.JSON_SCALAR_UNMARSHALLERS,
                                            SdkStructuredPlainJsonFactory.JSON_CUSTOM_TYPE_UNMARSHALLERS,
                                            httpResponse);

        bh.consume(PutItemResultJsonUnmarshaller.getInstance().unmarshall(ctx));
    }

    @Benchmark
    public void putItemSer(Blackhole bh) {
        bh.consume(new PutItemRequestProtocolMarshaller(protocolFactory).marshall(putItemRequest));
    }

    private static Map<String, AttributeValue> itemMap() {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("pk", new AttributeValue().withS("benchmark-key"));
        item.put("sk", new AttributeValue().withN("100"));
        item.put("stringField", new AttributeValue().withS("test-value"));
        item.put("numberField", new AttributeValue().withN("123.456"));
        item.put("binaryField", new AttributeValue()
            .withB(ByteBuffer.wrap("hello world".getBytes())));
        item.put("stringSetField", new AttributeValue()
            .withSS("value1", "value2", "value3"));
        item.put("numberSetField", new AttributeValue()
            .withNS("1.1", "2.2", "3.3"));
        item.put("boolField", new AttributeValue().withBOOL(false));
        item.put("nullField", new AttributeValue().withNULL(true));
        Map<String, AttributeValue> deep = new HashMap<String, AttributeValue>();
        deep.put("level2", new AttributeValue().withN("999"));
        Map<String, AttributeValue> nested = new HashMap<String, AttributeValue>();
        nested.put("nested", new AttributeValue().withS("nested-value"));
        nested.put("deepNested", new AttributeValue().withM(deep));
        item.put("mapField", new AttributeValue().withM(nested));
        item.put("listField", new AttributeValue().withL(
            new AttributeValue().withS("item1"),
            new AttributeValue().withN("42"),
            new AttributeValue().withBOOL(true),
            new AttributeValue().withNULL(true)));
        return item;
    }

    private static byte[] loadFixture(String path) throws IOException {
        return com.amazonaws.util.IOUtils.toByteArray(
            V1JsonProtocolBenchmark.class.getClassLoader().getResourceAsStream(path));
    }
}
