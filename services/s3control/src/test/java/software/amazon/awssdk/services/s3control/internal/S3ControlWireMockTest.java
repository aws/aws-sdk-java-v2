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

package software.amazon.awssdk.services.s3control.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.utils.builder.SdkBuilder;

public class S3ControlWireMockTest {

    @Rule
    public WireMockRule mockServer = new WireMockRule(new WireMockConfiguration().port(0).httpsPort(0));

    private S3ControlClient client;

    @Before
    public void setup() {
        client = S3ControlClient.builder()
                                   .region(Region.US_WEST_2)
                                   .credentialsProvider(() -> AwsBasicCredentials.create("test", "test"))
                                   .build();
    }

    @Test
    public void invalidAccountId_shouldThrowException() {
        assertThatThrownBy(() -> client.getPublicAccessBlock(b -> b.accountId("1234#"))).isInstanceOf(IllegalArgumentException.class)
                                                                                        .hasMessageContaining("must only contain alphanumeric characters and dashes");
    }

    @Test
    public void nullAccountId_shouldThrowException() {
        assertThatThrownBy(() -> client.getPublicAccessBlock(SdkBuilder::build)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("component is missing");
    }
}
