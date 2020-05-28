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
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolquery.ProtocolQueryClient;
import software.amazon.awssdk.services.protocolquery.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolquery.model.EmptyModeledException;
import software.amazon.awssdk.services.protocolquery.model.ImplicitPayloadException;
import software.amazon.awssdk.services.protocolquery.model.ProtocolQueryException;

public class QueryExceptionTests {

    private static final String PATH = "/";

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolQueryClient client;

    @Before
    public void setupClient() {
        client = ProtocolQueryClient.builder()
                                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                    .region(Region.US_EAST_1)
                                    .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                    .build();
    }

    @Test
    public void unmodeledException_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH,
                        "<ErrorResponse><Error><Code>UnmodeledException</Code></Error></ErrorResponse>");
        assertThatThrownBy(() -> client.allTypes(AllTypesRequest.builder().build()))
            .isExactlyInstanceOf(ProtocolQueryException.class);
    }

    @Test
    public void unmodeledException_ErrorCodeSetOnServiceException() {
        stub404Response(PATH,
                        "<ErrorResponse><Error><Code>UnmodeledException</Code></Error></ErrorResponse>");
        AwsServiceException exception = captureServiceException(this::callAllTypes);
        assertThat(exception.awsErrorDetails().errorCode()).isEqualTo("UnmodeledException");
    }

    @Test
    public void unmodeledExceptionWithMessage_MessageSetOnServiceException() {
        stub404Response(PATH,
                        "<ErrorResponse><Error><Code>UnmodeledException</Code><Message>Something happened</Message></Error></ErrorResponse>");
        AwsServiceException exception = captureServiceException(this::callAllTypes);
        assertThat(exception.awsErrorDetails().errorMessage()).isEqualTo("Something happened");
    }

    @Test
    public void unmodeledException_StatusCodeSetOnServiceException() {
        stub404Response(PATH,
                        "<ErrorResponse><Error><Code>UnmodeledException</Code></Error></ErrorResponse>");
        SdkServiceException exception = captureServiceException(this::callAllTypes);
        assertThat(exception.statusCode()).isEqualTo(404);
    }

    @Test
    public void modeledException_UnmarshalledIntoModeledException() {
        stub404Response(PATH,
                        "<ErrorResponse><Error><Code>EmptyModeledException</Code></Error></ErrorResponse>");
        try {
            callAllTypes();
        } catch (EmptyModeledException e) {
            assertThat(e).isInstanceOf(ProtocolQueryException.class);
            assertThat(e.awsErrorDetails().errorCode()).isEqualTo("EmptyModeledException");
        }
    }

    @Test
    public void modeledExceptionWithMessage_MessageSetOnServiceExeption() {
        stub404Response(PATH,
                        "<ErrorResponse><Error><Code>EmptyModeledException</Code><Message>Something happened</Message></Error></ErrorResponse>");
        EmptyModeledException exception = captureModeledException(this::callAllTypes);
        assertThat(exception.awsErrorDetails().errorMessage()).isEqualTo("Something happened");
    }

    @Test
    public void modeledException_ErrorCodeSetOnServiceException() {
        stub404Response(PATH,
                        "<ErrorResponse><Error><Code>EmptyModeledException</Code></Error></ErrorResponse>");
        final EmptyModeledException exception = captureModeledException(this::callAllTypes);
        assertThat(exception.awsErrorDetails().errorCode()).isEqualTo("EmptyModeledException");
    }

    @Test
    public void modeledException_StatusCodeSetOnServiceException() {
        stub404Response(PATH,
                        "<ErrorResponse><Error><Code>EmptyModeledException</Code></Error></ErrorResponse>");
        final EmptyModeledException exception = captureModeledException(this::callAllTypes);
        assertThat(exception.statusCode()).isEqualTo(404);
    }

    @Test
    public void emptyErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH, "");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void malformedErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH, "THIS ISN'T XML");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void emptyErrorResponse_UnmarshallsIntoUnknownErrorType() {
        stub404Response(PATH, "");
        AwsServiceException exception = captureServiceException(this::callAllTypes);
        assertThat(exception.statusCode()).isEqualTo(404);
    }

    @Test
    public void malformedErrorResponse_UnmarshallsIntoUnknownErrorType() {
        stub404Response(PATH, "THIS ISN'T XML");
        AwsServiceException exception = captureServiceException(this::callAllTypes);
        assertThat(exception.statusCode()).isEqualTo(404);
    }

    @Test
    public void modeledExceptionWithImplicitPayloadMembers_UnmarshalledIntoModeledException() {
        String xml = "<ErrorResponse>"
                     + "   <Error>"
                     + "      <Code>ImplicitPayloadException</Code>"
                     + "      <Message>this is the service message</Message>"
                     + "      <StringMember>foo</StringMember>"
                     + "      <IntegerMember>42</IntegerMember>"
                     + "      <LongMember>9001</LongMember>"
                     + "      <DoubleMember>1234.56</DoubleMember>"
                     + "      <FloatMember>789.10</FloatMember>"
                     + "      <TimestampMember>2015-01-25T08:00:12Z</TimestampMember>"
                     + "      <BooleanMember>true</BooleanMember>"
                     + "      <BlobMember>dGhlcmUh</BlobMember>"
                     + "      <ListMember>"
                     + "         <member>valOne</member>"
                     + "         <member>valTwo</member>"
                     + "      </ListMember>"
                     + "      <MapMember>"
                     + "         <entry>"
                     + "            <key>keyOne</key>"
                     + "            <value>valOne</value>"
                     + "         </entry>"
                     + "         <entry>"
                     + "            <key>keyTwo</key>"
                     + "            <value>valTwo</value>"
                     + "         </entry>"
                     + "      </MapMember>"
                     + "      <SimpleStructMember>"
                     + "         <StringMember>foobar</StringMember>"
                     + "      </SimpleStructMember>"
                     + "   </Error>"
                     + "</ErrorResponse>";
        stubFor(post(urlEqualTo(PATH)).willReturn(
            aResponse().withStatus(404)
                       .withBody(xml)));
        try {
            client.allTypes();
        } catch (ImplicitPayloadException e) {
            assertThat(e.stringMember()).isEqualTo("foo");
            assertThat(e.integerMember()).isEqualTo(42);
            assertThat(e.longMember()).isEqualTo(9001);
            assertThat(e.doubleMember()).isEqualTo(1234.56);
            assertThat(e.floatMember()).isEqualTo(789.10f);
            assertThat(e.timestampMember()).isEqualTo(Instant.ofEpochMilli(1422172812000L));
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
        String xml = "<ErrorResponse>"
                     + "   <Error>"
                     + "      <Code>EmptyModeledException</Code>"
                     + "      <Message>This is the service message</Message>"
                     + "   </Error>"
                     + "   <RequestId>1234</RequestId>"
                     + "</ErrorResponse>";
        stubFor(post(urlEqualTo(PATH)).willReturn(
            aResponse()
                .withStatus(404)
                .withBody(xml)));
        try {
            client.allTypes();
        } catch (EmptyModeledException e) {
            AwsErrorDetails awsErrorDetails = e.awsErrorDetails();
            assertThat(awsErrorDetails.errorCode()).isEqualTo("EmptyModeledException");
            assertThat(awsErrorDetails.errorMessage()).isEqualTo("This is the service message");
            assertThat(awsErrorDetails.serviceName()).isEqualTo("ProtocolQuery");
            assertThat(awsErrorDetails.sdkHttpResponse()).isNotNull();
            assertThat(e.requestId()).isEqualTo("1234");
            assertThat(e.extendedRequestId()).isNull();
            assertThat(e.statusCode()).isEqualTo(404);
        }
    }

    @Test
    public void modeledException_RequestIDInXml_SetCorrectly() {
        String xml = "<ErrorResponse>"
                     + "   <Error>"
                     + "      <Code>EmptyModeledException</Code>"
                     + "      <Message>This is the service message</Message>"
                     + "   </Error>"
                     + "   <RequestID>1234</RequestID>"
                     + "</ErrorResponse>";
        stubFor(post(urlEqualTo(PATH)).willReturn(
            aResponse()
                .withStatus(404)
                .withBody(xml)));
        try {
            client.allTypes();
        } catch (EmptyModeledException e) {
            assertThat(e.requestId()).isEqualTo("1234");
            assertThat(e.extendedRequestId()).isNull();
        }
    }

    @Test
    public void requestIdInHeader_IsSetOnException() {
        stubFor(post(urlEqualTo(PATH)).willReturn(
            aResponse()
                .withStatus(404)
                .withHeader("x-amzn-RequestId", "1234")));
        try {
            client.allTypes();
        } catch (ProtocolQueryException e) {
            assertThat(e.requestId()).isEqualTo("1234");
            assertThat(e.extendedRequestId()).isNull();
        }
    }

    @Test
    public void requestIdAndExtendedRequestIdInHeader_IsSetOnException() {
        stubFor(post(urlEqualTo(PATH)).willReturn(
            aResponse()
                .withStatus(404)
                .withHeader("x-amzn-RequestId", "1234")
                .withHeader("x-amz-id-2", "5678")));
        try {
            client.allTypes();
        } catch (ProtocolQueryException e) {
            assertThat(e.requestId()).isEqualTo("1234");
            assertThat(e.extendedRequestId()).isEqualTo("5678");
        }
    }

    private void callAllTypes() {
        client.allTypes(AllTypesRequest.builder().build());
    }

    private void assertThrowsServiceBaseException(Runnable runnable) {
        assertThatThrownBy(runnable::run)
            .isExactlyInstanceOf(ProtocolQueryException.class);
    }

    private AwsServiceException captureServiceException(Runnable runnable) {
        try {
            runnable.run();
            return null;
        } catch (AwsServiceException exception) {
            return exception;
        }
    }

    private EmptyModeledException captureModeledException(Runnable runnable) {
        try {
            runnable.run();
            return null;
        } catch (EmptyModeledException exception) {
            return exception;
        }
    }
}
