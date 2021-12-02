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

package software.amazon.awssdk.services.protocolrestjson;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.awscore.DefaultAwsResponseMetadata;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonResponseMetadata;

public class HashCodeEqualsTest {

    private static AllTypesRequest.Builder requestBuilder;
    private static AllTypesResponse.Builder responseBuilder;

    @BeforeClass
    public static void setUp() {
        requestBuilder =
            AllTypesRequest.builder()
                           .stringMember("foo")
                           .integerMember(123)
                           .booleanMember(true)
                           .floatMember(123.0f)
                           .doubleMember(123.9)
                           .longMember(123L)
                           .simpleList("so simple")
                           .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(1))
                                                        .putHeader("test", "test")
                                                        .apiCallAttemptTimeout(Duration.ofMillis(200))
                                                        .addApiName(ApiName.builder()
                                                                           .name("test")
                                                                           .version("version")
                                                                           .build())
                                                        .credentialsProvider(() -> AwsBasicCredentials.create("tests", "safda"))
                           );

        Map<String, String> metadata = new HashMap<>();
        AwsResponseMetadata responseMetadata = ProtocolRestJsonResponseMetadata.create(DefaultAwsResponseMetadata.create(metadata));

        responseBuilder = AllTypesResponse.builder()
                                          .stringMember("foo")
                                          .integerMember(123)
                                          .booleanMember(true)
                                          .floatMember(123.0f)
                                          .doubleMember(123.9)
                                          .longMember(123L)
                                          .simpleList("so simple");
    }

    @Test
    public void request_sameFields_shouldEqual() {
        AllTypesRequest request = requestBuilder.build();
        AllTypesRequest anotherRequest = requestBuilder.build();
        assertThat(request).isEqualTo(anotherRequest);
        assertThat(request.hashCode()).isEqualTo(anotherRequest.hashCode());
    }

    @Test
    public void request_differentOverrideConfiguration_shouldNotEqual_sdkFieldsShouldEqual() {
        AllTypesRequest anotherRequest = requestBuilder.build();
        AllTypesRequest request = requestBuilder.overrideConfiguration(b -> b.credentialsProvider(
            () -> AwsBasicCredentials.create("TEST", "test")
        )).build();

        assertThat(request).isNotEqualTo(anotherRequest);
        assertThat(request.equalsBySdkFields(anotherRequest)).isTrue();
        assertThat(request.hashCode()).isNotEqualTo(anotherRequest.hashCode());
    }

    @Test
    public void response_sameFields_shouldEqual() {
        AllTypesResponse response = responseBuilder.build();
        AllTypesResponse anotherResponse = responseBuilder.build();
        assertThat(response).isEqualTo(anotherResponse);

        assertThat(response.hashCode()).isEqualTo(anotherResponse.hashCode());
    }

    @Test
    public void response_differentResponseMetadata_shouldNotEqual_sdkFieldsShouldEqual() {
        AllTypesResponse anotherResponse = responseBuilder.build();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("foo", "bar");

        AwsResponseMetadata responseMetadata = ProtocolRestJsonResponseMetadata.create(DefaultAwsResponseMetadata.create(metadata));
        AllTypesResponse response = (AllTypesResponse) responseBuilder.responseMetadata(responseMetadata).build();

        assertThat(response.equalsBySdkFields(anotherResponse)).isTrue();
        assertThat(response).isNotEqualTo(anotherResponse);
        assertThat(response.hashCode()).isNotEqualTo(anotherResponse.hashCode());
    }
}
