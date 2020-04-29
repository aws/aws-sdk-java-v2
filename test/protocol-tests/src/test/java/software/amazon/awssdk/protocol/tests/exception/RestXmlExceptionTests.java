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
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlClient;
import software.amazon.awssdk.services.protocolrestxml.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolrestxml.model.EmptyModeledException;
import software.amazon.awssdk.services.protocolrestxml.model.ImplicitPayloadException;
import software.amazon.awssdk.services.protocolrestxml.model.MultiLocationOperationRequest;
import software.amazon.awssdk.services.protocolrestxml.model.ProtocolRestXmlException;

public class RestXmlExceptionTests {

    private static final String ALL_TYPES_PATH = "/2016-03-11/allTypes";

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolRestXmlClient client;

    @Before
    public void setupClient() {
        client = ProtocolRestXmlClient.builder()
                                  .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                  .region(Region.US_EAST_1)
                                  .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                  .build();
    }

    @Test
    public void unmodeledException_UnmarshalledIntoBaseServiceException() {
        stub404Response(ALL_TYPES_PATH,
                        "<ErrorResponse><Error><Code>UnmodeledException</Code></Error></ErrorResponse>");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void modeledException_UnmarshalledIntoModeledException() {
        stub404Response(ALL_TYPES_PATH,
                        "<ErrorResponse><Error><Code>EmptyModeledException</Code></Error></ErrorResponse>");
        try {
            callAllTypes();
        } catch (EmptyModeledException e) {
            assertThat(e).isInstanceOf(ProtocolRestXmlException.class);
        }
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
        stubFor(post(urlEqualTo(ALL_TYPES_PATH)).willReturn(
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
        stubFor(post(urlEqualTo(ALL_TYPES_PATH)).willReturn(
            aResponse()
                .withStatus(404)
                .withBody(xml)));
        try {
            client.allTypes();
        } catch (EmptyModeledException e) {
            AwsErrorDetails awsErrorDetails = e.awsErrorDetails();
            assertThat(awsErrorDetails.errorCode()).isEqualTo("EmptyModeledException");
            assertThat(awsErrorDetails.errorMessage()).isEqualTo("This is the service message");
            assertThat(awsErrorDetails.serviceName()).isEqualTo("ProtocolRestXml");
            assertThat(awsErrorDetails.sdkHttpResponse()).isNotNull();
            assertThat(e.requestId()).isEqualTo("1234");
            assertThat(e.extendedRequestId()).isNull();
            assertThat(e.statusCode()).isEqualTo(404);
        }
    }

    @Test
    public void modeledException_HasExceptionMetadataIncludingExtendedRequestIdSet() {
        String xml = "<ErrorResponse>"
                     + "   <Error>"
                     + "      <Code>EmptyModeledException</Code>"
                     + "      <Message>This is the service message</Message>"
                     + "   </Error>"
                     + "   <RequestId>1234</RequestId>"
                     + "</ErrorResponse>";
        stubFor(post(urlEqualTo(ALL_TYPES_PATH)).willReturn(
            aResponse()
                .withStatus(404)
                .withHeader("x-amz-id-2", "5678")
                .withBody(xml)));
        try {
            client.allTypes();
        } catch (EmptyModeledException e) {
            AwsErrorDetails awsErrorDetails = e.awsErrorDetails();
            assertThat(awsErrorDetails.errorCode()).isEqualTo("EmptyModeledException");
            assertThat(awsErrorDetails.errorMessage()).isEqualTo("This is the service message");
            assertThat(awsErrorDetails.serviceName()).isEqualTo("ProtocolRestXml");
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
        stub404Response(ALL_TYPES_PATH, "THIS ISN'T XML");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void illegalArgumentException_nullPathParam() {
        assertThrowsNestedExceptions(() -> client.multiLocationOperation(MultiLocationOperationRequest.builder().build()),
                                     SdkClientException.class,
                                     IllegalArgumentException.class);
    }

    @Test
    public void illegalArgumentException_emptyPathParam() {
        assertThrowsNestedExceptions(() -> client.multiLocationOperation(MultiLocationOperationRequest.builder()
                                                                                                      .pathParam("")
                                                                                                      .build()),
                                     SdkClientException.class,
                                     IllegalArgumentException.class);
    }

    private void callAllTypes() {
        client.allTypes(AllTypesRequest.builder().build());
    }

    private void assertThrowsServiceBaseException(Runnable runnable) {
        assertThrowsException(runnable, ProtocolRestXmlException.class);
    }

    private void assertThrowsIllegalArgumentException(Runnable runnable) {
        assertThrowsException(runnable, IllegalArgumentException.class);
    }

    private void assertThrowsNullPointerException(Runnable runnable) {
        assertThrowsException(runnable, NullPointerException.class);
    }

    private void assertThrowsException(Runnable runnable, Class<? extends Exception> expectedException) {
        try {
            runnable.run();
        } catch (Exception e) {
            assertEquals(expectedException, e.getClass());
        }
    }

    private void assertThrowsNestedExceptions(Runnable runnable, Class<? extends Exception> parentException,
                                              Class<? extends Exception> nestedException) {
        try {
            runnable.run();
        } catch (Exception e) {
            assertEquals(parentException, e.getClass());
            assertEquals(nestedException, e.getCause().getClass());
        }
    }
}
