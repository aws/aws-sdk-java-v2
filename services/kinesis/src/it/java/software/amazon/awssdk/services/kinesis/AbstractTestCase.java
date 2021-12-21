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

package software.amazon.awssdk.services.kinesis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import org.junit.BeforeClass;
import software.amazon.awssdk.awscore.util.AwsHostNameUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public class AbstractTestCase extends AwsTestBase {
    protected static KinesisClient client;
    protected static KinesisAsyncClient asyncClient;

    @BeforeClass
    public static void init() throws IOException {
        setUpCredentials();
        KinesisClientBuilder builder = KinesisClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN);
        setEndpoint(builder);
        client = builder.build();
        asyncClient = KinesisAsyncClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }

    private static void setEndpoint(KinesisClientBuilder builder) throws IOException {
        File endpointOverrides = new File(
                new File(System.getProperty("user.home")),
                ".aws/awsEndpointOverrides.properties"
        );

        if (endpointOverrides.exists()) {
            Properties properties = new Properties();
            properties.load(new FileInputStream(endpointOverrides));

            String endpoint = properties.getProperty("kinesis.endpoint");

            if (endpoint != null) {
                Region region = AwsHostNameUtils.parseSigningRegion(endpoint, "kinesis")
                                                .orElseThrow(() -> new IllegalArgumentException("Unknown region for endpoint. " +
                                                                                                endpoint));
                builder.region(region)
                       .endpointOverride(URI.create(endpoint));
            }
        }
    }
}
