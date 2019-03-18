/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governings3
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;
import java.net.URI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;

public class S3UtilitiesTest {

    private static final URI US_EAST_1_URI = URI.create("https://s3.amazonaws.com");

    private static final S3Configuration ACCELERATE_AND_DUALSTACK_ENABLED = S3Configuration.builder()
                                                                                     .accelerateModeEnabled(true)
                                                                                     .dualstackEnabled(true)
                                                                                     .checksumValidationEnabled(true)
                                                                                     .build();
    private static final S3Configuration PATH_STYLE_CONFIG = S3Configuration.builder()
                                                                   .pathStyleAccessEnabled(true)
                                                                   .build();

    private static S3Client defaultClient;
    private static S3Utilities defaultUtilities;

    private static S3AsyncClient asyncClient;
    private static S3Utilities utilitiesFromAsyncClient;

    @BeforeClass
    public static void setup() {
        defaultClient = S3Client.builder()
                                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                .region(Region.US_WEST_2)
                                .build();
        defaultUtilities = defaultClient.utilities();

        asyncClient = S3AsyncClient.builder()
                                   .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                   .region(Region.AP_NORTHEAST_2)
                                   .build();
        utilitiesFromAsyncClient = asyncClient.utilities();
    }

    @AfterClass
    public static void cleanup() {
        defaultClient.close();
        asyncClient.close();
    }

    @Test
    public void test_utilities_createdThroughS3Client() throws MalformedURLException {
        assertThat(defaultUtilities.getUrl(requestWithoutSpaces())
                                   .toExternalForm())
            .isEqualTo("https://foo-bucket.s3.us-west-2.amazonaws.com/key-without-spaces");

        assertThat(defaultUtilities.getUrl(requestWithSpecialCharacters())
                                   .toExternalForm())
            .isEqualTo("https://foo-bucket.s3.us-west-2.amazonaws.com/key%20with%40spaces");
    }

    @Test
    public void test_utilities_withPathStyleAccessEnabled() throws MalformedURLException {
        S3Utilities pathStyleUtilities = S3Utilities.builder()
                                                    .region(Region.US_WEST_2)
                                                    .s3Configuration(PATH_STYLE_CONFIG)
                                                    .build();

        assertThat(pathStyleUtilities.getUrl(requestWithoutSpaces())
                                   .toExternalForm())
            .isEqualTo("https://s3.us-west-2.amazonaws.com/foo-bucket/key-without-spaces");

        assertThat(pathStyleUtilities.getUrl(requestWithSpecialCharacters())
                                   .toExternalForm())
            .isEqualTo("https://s3.us-west-2.amazonaws.com/foo-bucket/key%20with%40spaces");
    }

    @Test
    public void test_withUsEast1Region() throws MalformedURLException {
        S3Utilities usEastUtilities = S3Utilities.builder().region(Region.US_EAST_1).build();

        assertThat(usEastUtilities.getUrl(requestWithoutSpaces())
                                  .toExternalForm())
            .isEqualTo("https://foo-bucket.s3.amazonaws.com/key-without-spaces");

        assertThat(usEastUtilities.getUrl(requestWithSpecialCharacters()).toExternalForm())
            .isEqualTo("https://foo-bucket.s3.amazonaws.com/key%20with%40spaces");
    }

    @Test
    public void test_RegionOnRequestTakesPrecendence() throws MalformedURLException {
        S3Utilities utilities = S3Utilities.builder().region(Region.US_WEST_2).build();

        assertThat(utilities.getUrl(b -> b.bucket("foo-bucket")
                                          .key("key-without-spaces")
                                          .region(Region.US_EAST_1))
                                  .toExternalForm())
            .isEqualTo("https://foo-bucket.s3.amazonaws.com/key-without-spaces");
    }

    @Test
    public void test_EndpointOnRequestTakesPrecendence() throws MalformedURLException {
        assertThat(defaultUtilities.getUrl(GetUrlRequest.builder()
                                                        .bucket("foo-bucket")
                                                        .key("key-without-spaces")
                                                        .endpoint(US_EAST_1_URI)
                                                        .build())
                                   .toExternalForm())
            .isEqualTo("https://foo-bucket.s3.amazonaws.com/key-without-spaces");
    }

    @Test
    public void testWithAccelerateAndDualStackEnabled() throws MalformedURLException {
        S3Utilities utilities = S3Client.builder()
                                        .serviceConfiguration(ACCELERATE_AND_DUALSTACK_ENABLED)
                                        .build()
                                        .utilities();

        assertThat(utilities.getUrl(requestWithSpecialCharacters())
                            .toExternalForm())
            .isEqualTo("https://foo-bucket.s3-accelerate.dualstack.amazonaws.com/key%20with%40spaces");
    }

    @Test
    public void testAsync() throws MalformedURLException {
        assertThat(utilitiesFromAsyncClient.getUrl(requestWithoutSpaces())
                                           .toExternalForm())
            .isEqualTo("https://foo-bucket.s3.ap-northeast-2.amazonaws.com/key-without-spaces");

        assertThat(utilitiesFromAsyncClient.getUrl(requestWithSpecialCharacters())
                                           .toExternalForm())
            .isEqualTo("https://foo-bucket.s3.ap-northeast-2.amazonaws.com/key%20with%40spaces");
    }

    @Test (expected = NullPointerException.class)
    public void failIfRegionIsNotSetOnS3UtilitiesObject() throws MalformedURLException {
        S3Utilities.builder().build();
    }

    private static GetUrlRequest requestWithoutSpaces() {
        return GetUrlRequest.builder()
                            .bucket("foo-bucket")
                            .key("key-without-spaces")
                            .build();
    }

    private static GetUrlRequest requestWithSpecialCharacters() {
        return GetUrlRequest.builder()
                            .bucket("foo-bucket")
                            .key("key with@spaces")
                            .build();
    }
}
