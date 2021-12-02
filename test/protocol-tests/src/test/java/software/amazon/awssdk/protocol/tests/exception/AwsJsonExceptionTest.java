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
import java.net.URI;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocoljsonrpc.ProtocolJsonRpcClient;
import software.amazon.awssdk.services.protocoljsonrpc.model.AllTypesRequest;
import software.amazon.awssdk.services.protocoljsonrpc.model.EmptyModeledException;
import software.amazon.awssdk.services.protocoljsonrpc.model.ImplicitPayloadException;
import software.amazon.awssdk.services.protocoljsonrpc.model.ProtocolJsonRpcException;

/**
 * Exception related tests for AWS/JSON RPC.
 */
public class AwsJsonExceptionTest {
    private static final String PATH = "/";

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolJsonRpcClient client;

    @Before
    public void setupClient() {
        client = ProtocolJsonRpcClient.builder()
                                      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                      .region(Region.US_EAST_1)
                                      .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                      .build();
    }

    @Test
    public void unmodeledException_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH, "{\"__type\": \"SomeUnknownType\"}");
        assertThatThrownBy(() -> client.allTypes(AllTypesRequest.builder().build()))
            .isExactlyInstanceOf(ProtocolJsonRpcException.class);
    }

    @Test
    public void modeledExceptionWithImplicitPayloadMembers_UnmarshalledIntoModeledException() {
        stubFor(post(urlEqualTo(PATH)).willReturn(
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
    public void modeledException_UnmarshalledIntoModeledException() {
        stub404Response(PATH, "{\"__type\": \"EmptyModeledException\"}");
        assertThatThrownBy(() -> client.allTypes(AllTypesRequest.builder().build()))
            .isExactlyInstanceOf(EmptyModeledException.class);
    }

    @Test
    public void modeledException_HasExceptionMetadataSet() {
        stubFor(post(urlEqualTo(PATH)).willReturn(
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
            assertThat(awsErrorDetails.serviceName()).isEqualTo("ProtocolJsonRpc");
            assertThat(awsErrorDetails.sdkHttpResponse()).isNotNull();
            assertThat(e.requestId()).isEqualTo("1234");
            assertThat(e.extendedRequestId()).isNull();
            assertThat(e.statusCode()).isEqualTo(404);
        }
    }

    @Test
    public void modeledException_HasExceptionMetadataIncludingExtendedRequestIdSet() {
        stubFor(post(urlEqualTo(PATH)).willReturn(
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
            assertThat(awsErrorDetails.serviceName()).isEqualTo("ProtocolJsonRpc");
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
            .isExactlyInstanceOf(ProtocolJsonRpcException.class);
    }

    @Test
    public void malformedErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH, "THIS ISN'T JSON");
        assertThatThrownBy(() -> client.allTypes(AllTypesRequest.builder().build()))
            .isExactlyInstanceOf(ProtocolJsonRpcException.class);
    }
}
