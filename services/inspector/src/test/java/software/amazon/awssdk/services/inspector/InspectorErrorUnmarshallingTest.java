/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.inspector;/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.inspector.model.AccessDeniedException;
import software.amazon.awssdk.services.inspector.model.ListRulesPackagesRequest;

public class InspectorErrorUnmarshallingTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private InspectorClient inspector;

    @Before
    public void setup() {
        StaticCredentialsProvider credsProvider = StaticCredentialsProvider.create(AwsCredentials.create("akid", "skid"));
        inspector = InspectorClient.builder()
                                   .credentialsProvider(credsProvider)
                                   .region(Region.US_EAST_1)
                                   .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                   .build();
    }

    /**
     * Some error shapes in Inspector define an errorCode member which clashes with the errorCode
     * defined in {@link SdkServiceException}. We've customized the name of the
     * modeled error code so both can be used by customers. This test asserts that both are
     * unmarshalled correctly.
     */
    @Test
    public void errorCodeAndInspectorErrorCodeUnmarshalledCorrectly() {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse().withStatus(400).withBody(
                "{\"__type\":\"AccessDeniedException\",\"errorCode\": \"ACCESS_DENIED_TO_RULES_PACKAGE\", " +
                "\"Message\":\"User: arn:aws:iam::1234:user/no-perms is not authorized to perform: inspector:ListRulesPackages\"}")));

        try {
            inspector.listRulesPackages(ListRulesPackagesRequest.builder().build());
        } catch (AccessDeniedException e) {
            assertEquals("AccessDeniedException", e.errorCode());
            assertEquals("ACCESS_DENIED_TO_RULES_PACKAGE", e.inspectorErrorCodeString());
        }
    }

}
