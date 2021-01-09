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
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static software.amazon.awssdk.protocol.tests.util.exception.ExceptionTestUtils.stub404Response;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolrestjson.model.EmptyModeledException;
import software.amazon.awssdk.services.protocolrestjson.model.ExplicitPayloadAndHeadersException;
import software.amazon.awssdk.services.protocolrestjson.model.HeadOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.ImplicitPayloadException;
import software.amazon.awssdk.services.protocolrestjson.model.MultiLocationOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonException;

/**
 * Exception related tests for AWS REST JSON.
 */
public class RestJsonExceptionTests {

    private static final String ALL_TYPES_PATH = "/2016-03-11/allTypes";

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolRestJsonClient client;

    @Before
    public void setupClient() {
        client = ProtocolRestJsonClient.builder()
                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                       .build();
    }

    @Test
    public void unmodeledException_UnmarshalledIntoBaseServiceException() {
        stub404Response(ALL_TYPES_PATH, "{\"__type\": \"SomeUnknownType\"}");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void modeledException_UnmarshalledIntoModeledException() {
        stub404Response(ALL_TYPES_PATH, "{\"__type\": \"EmptyModeledException\"}");
        assertThrowsException(this::callAllTypes, EmptyModeledException.class);
    }

    @Test
    public void modeledExceptionWithMembers_UnmarshalledIntoModeledException() {
        stub404Response(ALL_TYPES_PATH, "");
        stubFor(post(urlEqualTo(ALL_TYPES_PATH)).willReturn(
            aResponse().withStatus(404)
                       .withHeader("x-amz-string", "foo")
                       .withHeader("x-amz-integer", "42")
                       .withHeader("x-amz-long", "9001")
                       .withHeader("x-amz-double", "1234.56")
                       .withHeader("x-amz-float", "789.10")
                       .withHeader("x-amz-timestamp", "Sun, 25 Jan 2015 08:00:00 GMT")
                       .withHeader("x-amz-boolean", "true")
                       .withBody("{\"__type\": \"ExplicitPayloadAndHeadersException\", \"StringMember\": \"foobar\"}")));
        try {
            callAllTypes();
        } catch (ExplicitPayloadAndHeadersException e) {
            assertEquals("foo", e.stringHeader());
            assertEquals(42, (int) e.integerHeader());
            assertEquals(9001, (long) e.longHeader());
            assertEquals(1234.56, e.doubleHeader(), 0.1);
            assertEquals(789.10, e.floatHeader(), 0.1);
            assertEquals(Instant.ofEpochMilli(1422172800000L), e.timestampHeader());
            assertEquals(true, e.booleanHeader());
            assertEquals("foobar", e.payloadMember().stringMember());
        }
    }

    @Test
    public void modeledExceptionWithImplicitPayloadMembers_UnmarshalledIntoModeledException() {
        stub404Response(ALL_TYPES_PATH, "");
        stubFor(post(urlEqualTo(ALL_TYPES_PATH)).willReturn(
            aResponse().withStatus(404)
                       .withBody("{\"__type\": \"ImplicitPayloadException\", "
                                 + "\"StringMember\": \"foo\","
                                 + "\"IntegerMember\": 42,"
                                 + "\"LongMember\": 9001,"
                                 + "\"DoubleMember\": 1234.56,"
                                 + "\"FloatMember\": 789.10,"
                                 + "\"TimestampMember\": 1398796238.123,"
                                 + "\"BooleanMember\": true,"
                                 + "\"BlobMember\": \"dGhlcmUh\","
                                 + "\"ListMember\": [\"valOne\", \"valTwo\"],"
                                 + "\"MapMember\": {\"keyOne\": \"valOne\", \"keyTwo\": \"valTwo\"},"
                                 + "\"SimpleStructMember\": {\"StringMember\": \"foobar\"}"
                                 + "}")));
        try {
            callAllTypes();
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
    public void modeledException_HasExceptionMetadataSet() {
        stubFor(post(urlEqualTo(ALL_TYPES_PATH)).willReturn(
            aResponse()
                .withStatus(404)
                .withHeader("x-amzn-RequestId", "1234")
                .withBody("{\"__type\": \"EmptyModeledException\", \"Message\": \"This is the service message\"}")));
        try {
            client.allTypes();
        } catch (EmptyModeledException e) {
            AwsErrorDetails awsErrorDetails = e.awsErrorDetails();
            assertThat(awsErrorDetails.errorCode()).isEqualTo("EmptyModeledException");
            assertThat(awsErrorDetails.errorMessage()).isEqualTo("This is the service message");
            assertThat(awsErrorDetails.serviceName()).isEqualTo("ProtocolRestJson");
            assertThat(awsErrorDetails.sdkHttpResponse()).isNotNull();
            assertThat(e.requestId()).isEqualTo("1234");
            assertThat(e.extendedRequestId()).isNull();
            assertThat(e.statusCode()).isEqualTo(404);
        }
    }

    @Test
    public void modeledException_HasExceptionMetadataIncludingExtendedRequestIdSet() {
        stubFor(post(urlEqualTo(ALL_TYPES_PATH)).willReturn(
            aResponse()
                .withStatus(404)
                .withHeader("x-amzn-RequestId", "1234")
                .withHeader("x-amz-id-2", "5678")
                .withBody("{\"__type\": \"EmptyModeledException\", \"Message\": \"This is the service message\"}")));
        try {
            client.allTypes();
        } catch (EmptyModeledException e) {
            AwsErrorDetails awsErrorDetails = e.awsErrorDetails();
            assertThat(awsErrorDetails.errorCode()).isEqualTo("EmptyModeledException");
            assertThat(awsErrorDetails.errorMessage()).isEqualTo("This is the service message");
            assertThat(awsErrorDetails.serviceName()).isEqualTo("ProtocolRestJson");
            assertThat(awsErrorDetails.sdkHttpResponse()).isNotNull();
            assertThat(e.requestId()).isEqualTo("1234");
            assertThat(e.extendedRequestId()).isEqualTo("5678");
            assertThat(e.statusCode()).isEqualTo(404);
        }
    }

    @Test
    public void emptyErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(ALL_TYPES_PATH, "");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void malformedErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(ALL_TYPES_PATH, "THIS ISN'T JSON");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void modeledExceptionInHeadRequest_UnmarshalledIntoModeledException() {
        stubFor(head(urlEqualTo("/2016-03-11/headOperation"))
                    .willReturn(aResponse()
                                    .withStatus(404)
                                    .withHeader("x-amzn-ErrorType", "EmptyModeledException")));
        assertThrowsException(() -> client.headOperation(HeadOperationRequest.builder().build()), EmptyModeledException.class);
    }

    @Test
    public void unmodeledExceptionInHeadRequest_UnmarshalledIntoModeledException() {
        stubFor(head(urlEqualTo("/2016-03-11/headOperation"))
                    .willReturn(aResponse()
                                    .withStatus(404)
                                    .withHeader("x-amzn-ErrorType", "SomeUnknownType")));
        assertThrowsServiceBaseException(() -> client.headOperation(HeadOperationRequest.builder().build()));
    }

    @Test
    public void nullPathParam_ThrowsSdkClientException() {
        assertThrowsSdkClientException(() -> client.multiLocationOperation(MultiLocationOperationRequest.builder().build()));
    }

    @Test
    public void emptyPathParam_ThrowsSdkClientException() {
        assertThrowsSdkClientException(() -> client.multiLocationOperation(MultiLocationOperationRequest.builder().pathParam("").build()));
    }


    private void callAllTypes() {
        client.allTypes(AllTypesRequest.builder().build());
    }

    private void assertThrowsServiceBaseException(Runnable runnable) {
        assertThrowsException(runnable, ProtocolRestJsonException.class);
    }

    private void assertThrowsSdkClientException(Runnable runnable) {
        assertThrowsException(runnable, SdkClientException.class);
    }

    private void assertThrowsException(Runnable runnable, Class<? extends Exception> expectedException) {
        try {
            runnable.run();
        } catch (Exception e) {
            assertEquals(expectedException, e.getClass());
        }
    }
}
