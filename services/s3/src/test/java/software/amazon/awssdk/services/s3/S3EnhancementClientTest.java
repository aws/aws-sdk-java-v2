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
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.protocols.xml.AwsS3ProtocolFactory;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;

public class S3EnhancementClientTest {

    private static final URI US_EAST_1_URI = URI.create("https://s3.amazonaws.com");
    private static final URI US_WEST_2_URI = URI.create( "https://s3.us-west-2.amazonaws.com");

    private static S3Configuration DEFAULT_CONFIG = S3Configuration.builder().build();
    private static S3Configuration PATH_STYLE_CONFIG = S3Configuration.builder()
                                                                   .pathStyleAccessEnabled(true)
                                                                   .build();

    private static S3EnhancementClient client;
    private static S3EnhancementClient pathStyleClient;
    private static S3EnhancementClient usEastClient;

    @BeforeClass
    public static void setup() {
        client = defaultClient();
        pathStyleClient = defaultPathStyleClient();
        usEastClient = usEastOneClient();
    }

    @Test
    public void keyWithoutSpaces() throws MalformedURLException {
        URL url = client.getUrl(requestWithoutSpaces())
                        .getUrl();

        assertThat(url.toExternalForm()).isEqualTo("https://foo-bucket.s3.us-west-2.amazonaws.com/key-without-spaces");
    }

    @Test
    public void keyWithSpecialCharacters() throws MalformedURLException {
        URL url = client.getUrl(requestWithSpecialCharacters())
                        .getUrl();

        assertThat(url.toExternalForm()).isEqualTo("https://foo-bucket.s3.us-west-2.amazonaws.com/key%20with%40spaces");
    }

    @Test
    public void keyWithoutSpaces_pathStyleAccess() throws MalformedURLException {
        URL url = pathStyleClient.getUrl(requestWithoutSpaces())
                        .getUrl();

        assertThat(url.toExternalForm()).isEqualTo("https://s3.us-west-2.amazonaws.com/foo-bucket/key-without-spaces");
    }

    @Test
    public void keyWithSpecialCharacters_pathStyleAccess() throws MalformedURLException {
        URL url = pathStyleClient.getUrl(requestWithSpecialCharacters())
                                 .getUrl();

        assertThat(url.toExternalForm()).isEqualTo("https://s3.us-west-2.amazonaws.com/foo-bucket/key%20with%40spaces");
    }

    @Test
    public void keyWithoutSpaces_usEastClient() throws MalformedURLException {
        URL url = usEastClient.getUrl(requestWithoutSpaces())
                              .getUrl();

        assertThat(url.toExternalForm()).isEqualTo("https://foo-bucket.s3.amazonaws.com/key-without-spaces");
    }

    @Test
    public void keyWithSpecialCharacters_usEastClient() throws MalformedURLException {
        URL url = usEastClient.getUrl(requestWithSpecialCharacters())
                              .getUrl();

        assertThat(url.toExternalForm()).isEqualTo("https://foo-bucket.s3.amazonaws.com/key%20with%40spaces");
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

    private static S3EnhancementClient defaultClient() {
        SdkClientConfiguration sdkClientConfiguration = clientConfig(DEFAULT_CONFIG,
                                                                     US_WEST_2_URI);
        return DefaultS3EnhancementClient.builder()
                                         .protocolFactory(initProtocolFactory(sdkClientConfiguration))
                                         .sdkClientConfiguration(sdkClientConfiguration)
                                         .build();
    }

    private static S3EnhancementClient defaultPathStyleClient() {
        SdkClientConfiguration sdkClientConfiguration = clientConfig(PATH_STYLE_CONFIG,
                                                                     US_WEST_2_URI);
        return DefaultS3EnhancementClient.builder()
                                         .protocolFactory(initProtocolFactory(sdkClientConfiguration))
                                         .sdkClientConfiguration(sdkClientConfiguration)
                                         .build();
    }

    private static S3EnhancementClient usEastOneClient() {
        SdkClientConfiguration sdkClientConfiguration = clientConfig(DEFAULT_CONFIG,
                                                                     US_EAST_1_URI);
        return DefaultS3EnhancementClient.builder()
                                         .protocolFactory(initProtocolFactory(sdkClientConfiguration))
                                         .sdkClientConfiguration(sdkClientConfiguration)
                                         .build();
    }

    private static SdkClientConfiguration clientConfig(S3Configuration s3Configuration,
                                                       URI endpoint) {
        return SdkClientConfiguration.builder()
                                     .option(SdkClientOption.SERVICE_CONFIGURATION, s3Configuration)
                                     .option(SdkClientOption.ENDPOINT, endpoint)
                                     .build();
    }

    private static AwsS3ProtocolFactory initProtocolFactory(SdkClientConfiguration sdkClientConfiguration) {
        return AwsS3ProtocolFactory.builder()
                                   .clientConfiguration(sdkClientConfiguration)
                                   .build();
    }
}
