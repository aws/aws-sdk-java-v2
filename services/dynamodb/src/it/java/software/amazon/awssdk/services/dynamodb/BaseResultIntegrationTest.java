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

package software.amazon.awssdk.services.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class BaseResultIntegrationTest extends AwsIntegrationTestBase {

    private DynamoDbClient dynamoDB;

    @Before
    public void setup() {
        dynamoDB = DynamoDbClient.builder()
                .region(Region.US_WEST_2)
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .build();
    }

    @Test
    @ReviewBeforeRelease("Response metadata has been broken by client/interface refactoring. Fix before release")
    public void responseMetadataInBaseResultIsSameAsMetadataCache() {
        ListTablesRequest request = ListTablesRequest.builder().build();
        ListTablesResponse result = dynamoDB.listTables(request);
        assertThat(result.sdkHttpResponse().headers()).isNotNull();
    }

    @Test
    @ReviewBeforeRelease("Response metadata has been broken by client/interface refactoring. Fix before release")
    public void httpMetadataInBaseResultIsValid() {
        ListTablesResponse result = dynamoDB.listTables(ListTablesRequest.builder().build());
        assertThat(result.sdkHttpResponse().statusCode()).isEqualTo(200);
        assertThat(result.sdkHttpResponse().headers()).containsKey("x-amz-crc32");
    }
}
