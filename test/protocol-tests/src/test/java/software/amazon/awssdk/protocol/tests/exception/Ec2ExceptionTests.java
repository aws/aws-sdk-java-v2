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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.protocol.tests.util.exception.ExceptionTestUtils.stub404Response;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolec2.ProtocolEc2Client;
import software.amazon.awssdk.services.protocolec2.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolec2.model.ProtocolEc2Exception;

public class Ec2ExceptionTests {
    private static final String PATH = "/";

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolEc2Client client;

    @Before
    public void setupClient() {
        client = ProtocolEc2Client.builder()
                                  .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                  .region(Region.US_EAST_1)
                                  .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                  .build();
    }

    @Test
    public void unmodeledException_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH,
                        "<Response><Errors><Error><Code>UnmodeledException</Code></Error></Errors></Response>");
        assertThatThrownBy(() -> client.allTypes(AllTypesRequest.builder().build()))
            .isExactlyInstanceOf(ProtocolEc2Exception.class);
    }

    @Test
    public void emptyErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH, "");
        assertThatThrownBy(() -> client.allTypes(AllTypesRequest.builder().build()))
            .isExactlyInstanceOf(ProtocolEc2Exception.class);
    }

    @Test
    public void malformedErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH, "THIS ISN'T XML");
        assertThatThrownBy(() -> client.allTypes(AllTypesRequest.builder().build()))
            .isExactlyInstanceOf(ProtocolEc2Exception.class);
    }
}
