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

package software.amazon.awssdk.crac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.internal.crac.CannedResponseHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonException;

/**
 * Tests a generated client configured with a {@link CannedResponseHttpClient}.
 */
class CannedResponseSyntheticCallTest {

    private static final byte[] RESPONSE_PAYLOAD = "{\"StringMember\":\"warmup\"}".getBytes(StandardCharsets.UTF_8);

    private static ProtocolRestJsonClient clientWith(CannedResponseHttpClient httpClient) {
        return ProtocolRestJsonClient.builder()
                                     .credentialsProvider(StaticCredentialsProvider.create(
                                         AwsBasicCredentials.create("akid", "skid")))
                                     .region(Region.US_EAST_1)
                                     .endpointOverride(URI.create("http://localhost"))
                                     .httpClient(httpClient)
                                     .build();
    }

    @Test
    void syntheticCall_throughCannedClient_completesAndUnmarshallsBody() {
        ProtocolRestJsonClient client = clientWith(CannedResponseHttpClient.builder()
                                                                           .responseBody(RESPONSE_PAYLOAD)
                                                                           .build());

        AllTypesResponse response = client.allTypes(r -> {
        });

        assertThat(response.stringMember()).isEqualTo("warmup");
    }

    @Test
    void syntheticCall_whenCannedStatusIsError_throwsServiceExceptionWithThatStatus() {
        ProtocolRestJsonClient client = clientWith(CannedResponseHttpClient.builder()
                                                                           .statusCode(500)
                                                                           .build());

        assertThatThrownBy(() -> client.allTypes(r -> {
        }))
            .isInstanceOf(ProtocolRestJsonException.class)
            .satisfies(e -> assertThat(((ProtocolRestJsonException) e).statusCode()).isEqualTo(500));
    }
}
