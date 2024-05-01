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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.testutils.service.http.MockHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

public class PathTraversalTest {
    private static S3Client client;
    private static MockSyncHttpClient httpClient;

    @BeforeClass
    public static void setup() {
        httpClient = new MockSyncHttpClient();
        client = S3Client.builder()
                         .region(Region.US_WEST_2)
                         .credentialsProvider(AnonymousCredentialsProvider.create())
                         .httpClient(httpClient)
                         .build();
    }

    @Test
    public void clientPreservesLeadingDotSegmentInUriLabel() {
        httpClient.stubNextResponse200();
        client.getObjectAsBytes(r -> r.bucket("mybucket").key("../key.txt"));
        assertThat(httpClient.getLastRequest().encodedPath()).isEqualTo("/../key.txt");
    }

    @Test
    public void clientPreservesEmbeddedDotSegmentInUriLabel() {
        httpClient.stubNextResponse200();
        client.getObjectAsBytes(r -> r.bucket("mybucket").key("foo/../key.txt"));
        assertThat(httpClient.getLastRequest().encodedPath()).isEqualTo("/foo/../key.txt");
    }
}
