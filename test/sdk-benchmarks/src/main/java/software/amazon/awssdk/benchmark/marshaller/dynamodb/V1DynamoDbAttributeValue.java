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

package software.amazon.awssdk.benchmark.marshaller.dynamodb;

import com.amazonaws.AmazonWebServiceResponse;
import com.amazonaws.Request;
import com.amazonaws.http.HttpResponse;
import com.amazonaws.http.HttpResponseHandler;
import com.amazonaws.protocol.json.JsonClientMetadata;
import com.amazonaws.protocol.json.JsonOperationMetadata;
import com.amazonaws.protocol.json.SdkJsonProtocolFactory;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.transform.GetItemResultJsonUnmarshaller;
import com.amazonaws.services.dynamodbv2.model.transform.PutItemRequestProtocolMarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class V1DynamoDbAttributeValue {
    private static final SdkJsonProtocolFactory PROTOCOL_FACTORY = protocolFactory();

    private static final PutItemRequestProtocolMarshaller PUT_ITEM_REQUEST_MARSHALLER
        = new PutItemRequestProtocolMarshaller(PROTOCOL_FACTORY);

    @Benchmark
    public Object putItem(PutItemState s) {
        return PUT_ITEM_REQUEST_MARSHALLER.marshall(s.getReq());
    }

    @Benchmark
    public Object getItem(GetItemState s) {
        HttpResponse resp = new HttpResponse(null, null);
        resp.setContent(new ByteArrayInputStream(s.testItem.utf8()));
        try {
            return getItemJsonResponseHandler().handle(resp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @State(Scope.Benchmark)
    public static class PutItemState {
        @Param({"TINY", "SMALL", "HUGE"})
        private TestItem testItem;

        private PutItemRequest req;

        @Setup
        public void setup() {
            req = new PutItemRequest().withItem(testItem.getValue());
        }

        public PutItemRequest getReq() {
            return req;
        }
    }

    @State(Scope.Benchmark)
    public static class GetItemState {
        @Param(
            {"TINY", "SMALL", "HUGE"}
        )
        private TestItemUnmarshalling testItem;
    }

    public enum TestItem {
        TINY,
        SMALL,
        HUGE;

        private static final AbstractItemFactory<AttributeValue> FACTORY = new V1ItemFactory();

        private Map<String, AttributeValue> av;

        static {
            TINY.av = FACTORY.tiny();
            SMALL.av = FACTORY.small();
            HUGE.av = FACTORY.huge();
        }

        public Map<String, AttributeValue> getValue() {
            return av;
        }
    }

    public enum TestItemUnmarshalling {
        TINY,
        SMALL,
        HUGE;

        private byte[] utf8;

        static {
            TINY.utf8 = toUtf8ByteArray(TestItem.TINY.av);
            SMALL.utf8 = toUtf8ByteArray(TestItem.SMALL.av);
            HUGE.utf8 = toUtf8ByteArray(TestItem.HUGE.av);
        }

        public byte[] utf8() {
            return utf8;
        }
    }

    private static byte[] toUtf8ByteArray(Map<String, AttributeValue> item) {
        Request<?> resp = PUT_ITEM_REQUEST_MARSHALLER.marshall(new PutItemRequest().withItem(item));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[8192];
        int read;
        try {
            while ((read = resp.getContent().read(buff)) != -1) {
                baos.write(buff, 0, read);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return baos.toByteArray();
    }

    private static HttpResponseHandler<AmazonWebServiceResponse<GetItemResult>> getItemJsonResponseHandler() {
        return PROTOCOL_FACTORY.createResponseHandler(new JsonOperationMetadata()
                                                          .withPayloadJson(true)
                                                          .withHasStreamingSuccessResponse(false),
                                                      new GetItemResultJsonUnmarshaller());
    }

    private static SdkJsonProtocolFactory protocolFactory() {
        return new com.amazonaws.protocol.json.SdkJsonProtocolFactory(
            new JsonClientMetadata()
                .withProtocolVersion("1.0")
                .withSupportsCbor(false)
                .withSupportsIon(false)

        );
    }
}
