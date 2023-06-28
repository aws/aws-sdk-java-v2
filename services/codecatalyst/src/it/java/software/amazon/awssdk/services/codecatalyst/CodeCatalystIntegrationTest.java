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

package software.amazon.awssdk.services.codecatalyst;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.token.credentials.ProfileTokenProvider;
import software.amazon.awssdk.services.codecatalyst.model.ListSpacesResponse;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class CodeCatalystIntegrationTest extends AwsIntegrationTestBase {

    private static CodeCatalystClient client;

    @BeforeClass
    public static void setUp() {
        client = CodeCatalystClient.builder()
                                   .tokenProvider(ProfileTokenProvider.create("codecatalyst"))
                                   .build();
    }

    @Test
    public void list() {
        ListSpacesResponse result = client.listSpaces(r -> {});
        Assert.assertTrue(result.items().size() >= 0);
    }
}
