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

package software.amazon.awssdk.protocol.tests.exception;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.protocol.tests.util.exception.ExceptionTestUtils.stub404Response;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolcbor.ProtocolCborClient;
import software.amazon.awssdk.services.protocolcbor.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolcbor.model.EmptyModeledException;
import software.amazon.awssdk.services.protocolcbor.model.ImplicitPayloadException;
import software.amazon.awssdk.services.protocolcbor.model.ProtocolCborException;
import software.amazon.awssdk.thirdparty.jackson.dataformat.cbor.CBORFactory;
import software.amazon.awssdk.thirdparty.jackson.dataformat.cbor.CBORGenerator;
import software.amazon.awssdk.utils.MapUtils;

/**
 * Exception related tests for CBOR.
 */
public class CborExceptionTest {
    private static final String PATH = "/";

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolCborClient client;

    private CBORFactory cborFactory;

    @Before
    public void setupClient() {
        client = ProtocolCborClient.builder()
                                   .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                   .region(Region.US_EAST_1)
                                   .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                   .build();

        cborFactory = new CBORFactory();
    }

    @Test
    public void unmodeledException_UnmarshalledIntoBaseServiceException() throws IOException {
        Map<?, ?> obj = MapUtils.of(
            "__type", "SomeUnknownType"
        );

        stub404Response(PATH, toCbor(obj));
        assertThatThrownBy(() -> client.allTypes(AllTypesRequest.builder().build()))
            .isExactlyInstanceOf(ProtocolCborException.class);
    }

    @Test
    public void modeledExceptionWithImplicitPayloadMembers_UnmarshalledIntoModeledException() throws IOException {
        Map obj = new HashMap();
        obj.put("__type", "ImplicitPayloadException");
        obj.put("StringMember", "foo");
        obj.put("IntegerMember", Integer.valueOf(42));
        obj.put("LongMember", Long.valueOf(9001));
        obj.put("DoubleMember", Double.valueOf(1234.56));
        obj.put("FloatMember", Float.valueOf(789.10f));
        obj.put("TimestampMember", Long.valueOf(1398796238123L));
        obj.put("BooleanMember", true);
        obj.put("BlobMember", "dGhlcmUh");
        obj.put("ListMember", Arrays.asList("valOne", "valTwo"));
        obj.put("MapMember", MapUtils.of("keyOne", "valOne", "keyTwo", "valTwo"));
        obj.put("SimpleStructMember", MapUtils.of("StringMember", "foobar"));

        stubFor(post(urlEqualTo(PATH)).willReturn(
            aResponse().withStatus(404)
                       .withBody(toCbor(obj))));
        try {
            client.allTypes();
        } catch (ImplicitPayloadException e) {
            assertThat(e.stringMember()).isEqualTo("foo");
            assertThat(e.integerMember()).isEqualTo(42);
            assertThat(e.longMember()).isEqualTo(9001);
            assertThat(e.doubleMember()).isEqualTo(1234.56);
            assertThat(e.floatMember()).isEqualTo(789.10f);
            assertThat(e.timestampMember()).isEqualTo(Instant.ofEpochMilli(1398796238123L));
            assertThat(e.booleanMember()).isEqualTo(true);
            assertThat(e.blobMember().asUtf8String()).isEqualTo("there!");
            assertThat(e.listMember()).contains("valOne", "valTwo");
            assertThat(e.mapMember())
                .containsOnly(new SimpleEntry<>("keyOne", "valOne"),
                              new SimpleEntry<>("keyTwo", "valTwo"));
            assertThat(e.simpleStructMember().stringMember()).isEqualTo("foobar");
        }
    }

    @Test
    public void modeledException_UnmarshalledIntoModeledException() throws IOException {
        Map obj = new HashMap();
        obj.put("__type", "EmptyModeledException");
        stub404Response(PATH, toCbor(obj));
        assertThatThrownBy(() -> client.allTypes(AllTypesRequest.builder().build()))
            .isExactlyInstanceOf(EmptyModeledException.class);
    }

    @Test
    public void modeledException_HasExceptionMetadataSet() throws IOException {
        Map obj = new HashMap();
        obj.put("__type", "EmptyModeledException");
        obj.put("Message", "This is the service message");
        stubFor(post(urlEqualTo(PATH)).willReturn(
            aResponse()
                .withStatus(404)
                .withHeader("x-amzn-RequestId", "1234")
                .withBody(toCbor(obj))));
        try {
            client.allTypes();
        } catch (EmptyModeledException e) {
            AwsErrorDetails awsErrorDetails = e.awsErrorDetails();
            assertThat(awsErrorDetails.errorCode()).isEqualTo("EmptyModeledException");
            assertThat(awsErrorDetails.errorMessage()).isEqualTo("This is the service message");
            assertThat(awsErrorDetails.serviceName()).isEqualTo("ProtocolCbor");
            assertThat(awsErrorDetails.sdkHttpResponse()).isNotNull();
            assertThat(e.requestId()).isEqualTo("1234");
            assertThat(e.extendedRequestId()).isNull();
            assertThat(e.statusCode()).isEqualTo(404);
        }
    }

    @Test
    public void modeledException_HasExceptionMetadataIncludingExtendedRequestIdSet() throws IOException {
        Map obj = new HashMap();
        obj.put("__type", "EmptyModeledException");
        obj.put("Message", "This is the service message");

        stubFor(post(urlEqualTo(PATH)).willReturn(
            aResponse()
                .withStatus(404)
                .withHeader("x-amzn-RequestId", "1234")
                .withHeader("x-amz-id-2", "5678")
                .withBody(toCbor(obj))));
        try {
            client.allTypes();
        } catch (EmptyModeledException e) {
            AwsErrorDetails awsErrorDetails = e.awsErrorDetails();
            assertThat(awsErrorDetails.errorCode()).isEqualTo("EmptyModeledException");
            assertThat(awsErrorDetails.errorMessage()).isEqualTo("This is the service message");
            assertThat(awsErrorDetails.serviceName()).isEqualTo("ProtocolCbor");
            assertThat(awsErrorDetails.sdkHttpResponse()).isNotNull();
            assertThat(e.requestId()).isEqualTo("1234");
            assertThat(e.extendedRequestId()).isEqualTo("5678");
            assertThat(e.statusCode()).isEqualTo(404);
        }
    }

    @Test
    public void emptyErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH, "");
        assertThatThrownBy(() -> client.allTypes(AllTypesRequest.builder().build()))
            .isExactlyInstanceOf(ProtocolCborException.class);
    }

    @Test
    public void malformedErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH, "THIS ISN'T CBOR");
        assertThatThrownBy(() -> client.allTypes(AllTypesRequest.builder().build()))
            .isExactlyInstanceOf(ProtocolCborException.class);
    }

    @Test
    public void modeledException_responseCodeNotPresent_usesCodeFromMetadata() throws IOException {
        Map obj = new HashMap();
        obj.put("__type", "EmptyModeledException");
        stub404Response(PATH, toCbor(obj));

        ProtocolCborClient client = ProtocolCborClient.builder()
                          .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                          .region(Region.US_EAST_1)
                          .endpointOverride(URI.create("http://localhost:" + wireMock.port())).overrideConfiguration(o -> o.addExecutionInterceptor(new ExecutionInterceptor() {
                @Override
                public SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context, ExecutionAttributes executionAttributes) {
                    return context.httpResponse().toBuilder().statusCode(0).build();
                }
            }))
                          .build();

        assertThatThrownBy(() -> client.allTypes(AllTypesRequest.builder().build()))
            .isExactlyInstanceOf(EmptyModeledException.class)
            .satisfies(e -> {
                assertThat(((EmptyModeledException) e).statusCode()).isEqualTo(400);
            });
    }

    private void mapToCbor(Map<?, ?> map, CBORGenerator generator) throws IOException {

        generator.writeStartObject();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            generator.writeFieldName((String) entry.getKey());
            toCbor(entry.getValue(), generator);
        }
        generator.writeEndObject();
    }

    private void listToCbor(List list, CBORGenerator generator) throws IOException {
        generator.writeStartArray();
        for (Object e : list) {
            toCbor(e, generator);
        }
        generator.writeEndArray();
    }

    private void toCbor(Object val, CBORGenerator generator) throws IOException {
        if (val instanceof String) {
            generator.writeString((String) val);
        } else if (val instanceof Integer) {
            generator.writeNumber((Integer) val);
        } else if (val instanceof Long) {
            generator.writeNumber((Long) val);
        } else if (val instanceof Double) {
            generator.writeNumber((Double) val);
        } else if (val instanceof Float) {
            generator.writeNumber((Float) val);
        } else if (val instanceof Boolean) {
            generator.writeBoolean((Boolean) val);
        } else if (val instanceof Map) {
            mapToCbor((Map<String, Object>) val, generator);
        } else if (val instanceof List) {
            listToCbor((List) val, generator);
        } else if (val instanceof BigDecimal) {
            generator.writeNumber((BigDecimal) val);
        }
    }

    private byte[] toCbor(Object o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CBORGenerator generator = cborFactory.createGenerator(baos);
        toCbor(o, generator);
        generator.flush();
        generator.close();
        return baos.toByteArray();
    }
}
