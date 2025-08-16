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

package software.amazon.awssdk.services.protocolrestjson.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

public class MemberCopierTest {
    private MockSyncHttpClient mockHttpClient;
    private ProtocolRestJsonClient client;

    @BeforeEach
    public void setupClient() {
        mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse200();

        client = ProtocolRestJsonClient.builder()
                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid",
                                                                                                                        "skid")))
                                       .region(Region.US_EAST_1)
                                       .httpClient(mockHttpClient)
                                       .build();
    }

    @Test
    public void enumListWithNulls_serializesWithoutNPE() {
        AllTypesRequest request = AllTypesRequest.builder()
                                                 .listOfEnums(Arrays.asList(EnumType.ENUM_VALUE1, null, EnumType.ENUM_VALUE2))
                                                 .build();

        assertDoesNotThrow(() -> client.allTypes(request));
    }

    @Test
    public void stringListWithNulls_serializesWithoutNPE() {
        AllTypesRequest request = AllTypesRequest.builder()
                                                 .simpleList(Arrays.asList("Foo", null, "Bar"))
                                                 .build();

        assertDoesNotThrow(() -> client.allTypes(request));
    }
}
