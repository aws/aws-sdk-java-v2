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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.MalformedURLException;
import java.net.URI;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
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
                                .credentialsProvider(dummyCreds())
                                .region(Region.US_WEST_2)
                                .build();
        defaultUtilities = defaultClient.utilities();

        asyncClient = S3AsyncClient.builder()
                                   .credentialsProvider(dummyCreds())
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
    public void test_EndpointOverrideOnClientWorks() {
        S3Utilities customizeUtilities = S3Client.builder()
                                                 .endpointOverride(URI.create("https://s3.custom.host"))
                                                 .build()
                                                 .utilities();
        assertThat(customizeUtilities.getUrl(GetUrlRequest.builder()
                                                          .bucket("foo-bucket")
                                                          .key("key-without-spaces")
                                                          .build())
                                     .toExternalForm())
            .isEqualTo("https://foo-bucket.s3.custom.host/key-without-spaces");
    }

    @Test
    public void testWithAccelerateAndDualStackEnabled() throws MalformedURLException {
        S3Utilities utilities = S3Client.builder()
                                        .credentialsProvider(dummyCreds())
                                        .region(Region.US_WEST_2)
                                        .serviceConfiguration(ACCELERATE_AND_DUALSTACK_ENABLED)
                                        .build()
                                        .utilities();

        assertThat(utilities.getUrl(requestWithSpecialCharacters())
                            .toExternalForm())
            .isEqualTo("https://foo-bucket.s3-accelerate.dualstack.amazonaws.com/key%20with%40spaces");
    }

    @Test
    public void testWithAccelerateAndDualStackViaClientEnabled() throws MalformedURLException {
        S3Utilities utilities = S3Client.builder()
                                        .credentialsProvider(dummyCreds())
                                        .region(Region.US_WEST_2)
                                        .serviceConfiguration(S3Configuration.builder()
                                                                             .accelerateModeEnabled(true)
                                                                             .build())
                                        .dualstackEnabled(true)
                                        .build()
                                        .utilities();

        assertThat(utilities.getUrl(requestWithSpecialCharacters())
                            .toExternalForm())
            .isEqualTo("https://foo-bucket.s3-accelerate.dualstack.amazonaws.com/key%20with%40spaces");
    }

    @Test
    public void testWithDualStackViaUtilitiesBuilderEnabled() throws MalformedURLException {
        S3Utilities utilities = S3Utilities.builder()
                                           .region(Region.US_WEST_2)
                                           .dualstackEnabled(true)
                                           .build();

        assertThat(utilities.getUrl(requestWithSpecialCharacters())
                            .toExternalForm())
            .isEqualTo("https://foo-bucket.s3.dualstack.us-west-2.amazonaws.com/key%20with%40spaces");
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

    @Test
    public void getUrlWithVersionId() {
        S3Utilities utilities = S3Utilities.builder().region(Region.US_WEST_2).build();

        assertThat(utilities.getUrl(b -> b.bucket("foo").key("bar").versionId("1"))
                            .toExternalForm())
            .isEqualTo("https://foo.s3.us-west-2.amazonaws.com/bar?versionId=1");

        assertThat(utilities.getUrl(b -> b.bucket("foo").key("bar").versionId("@1"))
                            .toExternalForm())
            .isEqualTo("https://foo.s3.us-west-2.amazonaws.com/bar?versionId=%401");
    }

    @Test
    public void parseS3Uri_rootUri_shouldParseCorrectly() {
        String uriString = "https://s3.amazonaws.com";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).isEmpty();
        assertThat(s3Uri.key()).isEmpty();
        assertThat(s3Uri.region()).isEmpty();
        assertThat(s3Uri.isPathStyle()).isTrue();
        assertThat(s3Uri.rawQueryParameters()).isEmpty();
    }

    @Test
    public void parseS3Uri_rootUriTrailingSlash_shouldParseCorrectly() {
        String uriString = "https://s3.amazonaws.com/";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).isEmpty();
        assertThat(s3Uri.key()).isEmpty();
        assertThat(s3Uri.region()).isEmpty();
        assertThat(s3Uri.isPathStyle()).isTrue();
        assertThat(s3Uri.rawQueryParameters()).isEmpty();
    }

    @Test
    public void parseS3Uri_pathStyleTrailingSlash_shouldParseCorrectly() {
        String uriString = "https://s3.us-east-1.amazonaws.com/myBucket/";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("myBucket");
        assertThat(s3Uri.key()).isEmpty();
        assertThat(s3Uri.region()).contains(Region.US_EAST_1);
        assertThat(s3Uri.isPathStyle()).isTrue();
        assertThat(s3Uri.rawQueryParameters()).isEmpty();
    }

    @Test
    public void parseS3Uri_pathStyleGlobalEndpoint_shouldParseCorrectly() {
        String uriString = "https://s3.amazonaws.com/myBucket/resources/image1.png";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("myBucket");
        assertThat(s3Uri.key()).contains("resources/image1.png");
        assertThat(s3Uri.region()).isEmpty();
        assertThat(s3Uri.isPathStyle()).isTrue();
        assertThat(s3Uri.rawQueryParameters()).isEmpty();
    }

    @Test
    public void parseS3Uri_virtualStyleGlobalEndpoint_shouldParseCorrectly() {
        String uriString = "https://myBucket.s3.amazonaws.com/resources/image1.png";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("myBucket");
        assertThat(s3Uri.key()).contains("resources/image1.png");
        assertThat(s3Uri.region()).isEmpty();
        assertThat(s3Uri.isPathStyle()).isFalse();
        assertThat(s3Uri.rawQueryParameters()).isEmpty();
    }

    @Test
    public void parseS3Uri_pathStyleWithDot_shouldParseCorrectly() {
        String uriString = "https://s3.eu-west-2.amazonaws.com/myBucket/resources/image1.png";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("myBucket");
        assertThat(s3Uri.key()).contains("resources/image1.png");
        assertThat(s3Uri.region()).contains(Region.EU_WEST_2);
        assertThat(s3Uri.isPathStyle()).isTrue();
        assertThat(s3Uri.rawQueryParameters()).isEmpty();
    }

    @Test
    public void parseS3Uri_pathStyleWithDash_shouldParseCorrectly() {
        String uriString = "https://s3-eu-west-2.amazonaws.com/myBucket/resources/image1.png";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("myBucket");
        assertThat(s3Uri.key()).contains("resources/image1.png");
        assertThat(s3Uri.region()).contains(Region.EU_WEST_2);
        assertThat(s3Uri.isPathStyle()).isTrue();
        assertThat(s3Uri.rawQueryParameters()).isEmpty();
    }

    @Test
    public void parseS3Uri_virtualHostedStyleWithDot_shouldParseCorrectly() {
        String uriString = "https://myBucket.s3.us-east-2.amazonaws.com/image.png";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("myBucket");
        assertThat(s3Uri.key()).contains("image.png");
        assertThat(s3Uri.region()).contains(Region.US_EAST_2);
        assertThat(s3Uri.isPathStyle()).isFalse();
        assertThat(s3Uri.rawQueryParameters()).isEmpty();
    }

    @Test
    public void parseS3Uri_virtualHostedStyleWithDash_shouldParseCorrectly() {
        String uriString = "https://myBucket.s3-us-east-2.amazonaws.com/image.png";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("myBucket");
        assertThat(s3Uri.key()).contains("image.png");
        assertThat(s3Uri.region()).contains(Region.US_EAST_2);
        assertThat(s3Uri.isPathStyle()).isFalse();
        assertThat(s3Uri.rawQueryParameters()).isEmpty();
    }

    @Test
    public void parseS3Uri_pathStyleWithQuery_shouldParseCorrectly() {
        String uriString = "https://s3.us-west-1.amazonaws.com/myBucket/doc.txt?versionId=abc123";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("myBucket");
        assertThat(s3Uri.key()).contains("doc.txt");
        assertThat(s3Uri.region()).contains(Region.US_WEST_1);
        assertThat(s3Uri.isPathStyle()).isTrue();
        assertThat(s3Uri.firstMatchingRawQueryParameter("versionId")).contains("abc123");
    }

    @Test
    public void parseS3Uri_pathStyleWithQueryMultipleValues_shouldParseCorrectly() {
        String uriString = "https://s3.us-west-1.amazonaws.com/myBucket/doc.txt?versionId=abc123&versionId=def456";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("myBucket");
        assertThat(s3Uri.key()).contains("doc.txt");
        assertThat(s3Uri.region()).contains(Region.US_WEST_1);
        assertThat(s3Uri.isPathStyle()).isTrue();
        assertThat(s3Uri.firstMatchingRawQueryParameter("versionId")).contains("abc123");
        assertThat(s3Uri.firstMatchingRawQueryParameters("versionId")).contains("def456");
    }

    @Test
    public void parseS3Uri_pathStyleWithMultipleQueries_shouldParseCorrectly() {
        String uriString = "https://s3.us-west-1.amazonaws.com/myBucket/doc.txt?versionId=abc123&partNumber=77";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("myBucket");
        assertThat(s3Uri.key()).contains("doc.txt");
        assertThat(s3Uri.region()).contains(Region.US_WEST_1);
        assertThat(s3Uri.isPathStyle()).isTrue();
        assertThat(s3Uri.firstMatchingRawQueryParameter("versionId")).contains("abc123");
        assertThat(s3Uri.firstMatchingRawQueryParameter("partNumber")).contains("77");
    }

    @Test
    public void parseS3Uri_pathStyleWithEncoding_shouldParseCorrectly() {
        String uriString = "https://s3.us-west-1.amazonaws.com/my%40Bucket/object%20key?versionId=%61%62%63%31%32%33";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("my@Bucket");
        assertThat(s3Uri.key()).contains("object key");
        assertThat(s3Uri.region()).contains(Region.US_WEST_1);
        assertThat(s3Uri.isPathStyle()).isTrue();
        assertThat(s3Uri.firstMatchingRawQueryParameter("versionId")).contains("abc123");
    }

    @Test
    public void parseS3Uri_virtualStyleWithQuery_shouldParseCorrectly() {
        String uriString= "https://myBucket.s3.us-west-1.amazonaws.com/doc.txt?versionId=abc123";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("myBucket");
        assertThat(s3Uri.key()).contains("doc.txt");
        assertThat(s3Uri.region()).contains(Region.US_WEST_1);
        assertThat(s3Uri.isPathStyle()).isFalse();
        assertThat(s3Uri.firstMatchingRawQueryParameter("versionId")).contains("abc123");
    }
    @Test
    public void parseS3Uri_virtualStyleWithQueryMultipleValues_shouldParseCorrectly() {
        String uriString = "https://myBucket.s3.us-west-1.amazonaws.com/doc.txt?versionId=abc123&versionId=def456";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("myBucket");
        assertThat(s3Uri.key()).contains("doc.txt");
        assertThat(s3Uri.region()).contains(Region.US_WEST_1);
        assertThat(s3Uri.isPathStyle()).isFalse();
        assertThat(s3Uri.firstMatchingRawQueryParameter("versionId")).contains("abc123");
        assertThat(s3Uri.firstMatchingRawQueryParameters("versionId")).contains("def456");
    }

    @Test
    public void parseS3Uri_virtualStyleWithMultipleQueries_shouldParseCorrectly() {
        String uriString = "https://myBucket.s3.us-west-1.amazonaws.com/doc.txt?versionId=abc123&partNumber=77";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("myBucket");
        assertThat(s3Uri.key()).contains("doc.txt");
        assertThat(s3Uri.region()).contains(Region.US_WEST_1);
        assertThat(s3Uri.isPathStyle()).isFalse();
        assertThat(s3Uri.firstMatchingRawQueryParameter("versionId")).contains("abc123");
        assertThat(s3Uri.firstMatchingRawQueryParameter("partNumber")).contains("77");
    }

    @Test
    public void parseS3Uri_virtualStyleWithEncoding_shouldParseCorrectly() {
        String uriString = "https://myBucket.s3.us-west-1.amazonaws.com/object%20key?versionId=%61%62%63%31%32%33";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("myBucket");
        assertThat(s3Uri.key()).contains("object key");
        assertThat(s3Uri.region()).contains(Region.US_WEST_1);
        assertThat(s3Uri.isPathStyle()).isFalse();
        assertThat(s3Uri.firstMatchingRawQueryParameter("versionId")).contains("abc123");
    }

    @Test
    public void parseS3Uri_cliStyleWithoutKey_shouldParseCorrectly() {
        String uriString = "s3://myBucket";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("myBucket");
        assertThat(s3Uri.key()).isEmpty();
        assertThat(s3Uri.region()).isEmpty();
        assertThat(s3Uri.isPathStyle()).isFalse();
    }

    @Test
    public void parseS3Uri_cliStyleWithoutKeyWithTrailingSlash_shouldParseCorrectly() {
        String uriString = "s3://myBucket/";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("myBucket");
        assertThat(s3Uri.key()).isEmpty();
        assertThat(s3Uri.region()).isEmpty();
        assertThat(s3Uri.isPathStyle()).isFalse();
    }

    @Test
    public void parseS3Uri_cliStyleWithKey_shouldParseCorrectly() {
        String uriString = "s3://myBucket/resources/key";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("myBucket");
        assertThat(s3Uri.key()).contains("resources/key");
        assertThat(s3Uri.region()).isEmpty();
        assertThat(s3Uri.isPathStyle()).isFalse();
    }

    @Test
    public void parseS3Uri_cliStyleWithEncoding_shouldParseCorrectly() {
        String uriString = "s3://my%40Bucket/object%20key";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.uri()).isEqualTo(uri);
        assertThat(s3Uri.bucket()).contains("my@Bucket");
        assertThat(s3Uri.key()).contains("object key");
        assertThat(s3Uri.region()).isEmpty();
        assertThat(s3Uri.isPathStyle()).isFalse();
    }

    @Test
    public void parseS3Uri_accessPointUri_shouldThrowProperErrorMessage() {
        String accessPointUriString = "myendpoint-123456789012.s3-accesspoint.us-east-1.amazonaws.com";
        URI accessPointUri = URI.create(accessPointUriString);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            defaultUtilities.parseUri(accessPointUri);
        });
        assertThat(exception.getMessage()).isEqualTo("AccessPoints URI parsing is not supported: "
                                                     + "myendpoint-123456789012.s3-accesspoint.us-east-1.amazonaws.com");
    }

    @Test
    public void parseS3Uri_accessPointUriWithFipsDualstack_shouldThrowProperErrorMessage() {
        String accessPointUriString = "myendpoint-123456789012.s3-accesspoint-fips.dualstack.us-gov-east-1.amazonaws.com";
        URI accessPointUri = URI.create(accessPointUriString);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            defaultUtilities.parseUri(accessPointUri);
        });
        assertThat(exception.getMessage()).isEqualTo("AccessPoints URI parsing is not supported: "
                                                     + "myendpoint-123456789012.s3-accesspoint-fips.dualstack.us-gov-east-1.amazonaws.com");
    }

    @Test
    public void parseS3Uri_outpostsUri_shouldThrowProperErrorMessage() {
        String outpostsUriString = "myaccesspoint-123456789012.op-01234567890123456.s3-outposts.us-west-2.amazonaws.com";
        URI outpostsUri = URI.create(outpostsUriString);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            defaultUtilities.parseUri(outpostsUri);
        });
        assertThat(exception.getMessage()).isEqualTo("Outposts URI parsing is not supported: "
                                                     + "myaccesspoint-123456789012.op-01234567890123456.s3-outposts.us-west-2.amazonaws.com");
    }

    @Test
    public void parseS3Uri_outpostsUriWithChinaPartition_shouldThrowProperErrorMessage() {
        String outpostsUriString = "myaccesspoint-123456789012.op-01234567890123456.s3-outposts.cn-north-1.amazonaws.com.cn";
        URI outpostsUri = URI.create(outpostsUriString);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            defaultUtilities.parseUri(outpostsUri);
        });
        assertThat(exception.getMessage()).isEqualTo("Outposts URI parsing is not supported: "
                                                     + "myaccesspoint-123456789012.op-01234567890123456.s3-outposts.cn-north-1.amazonaws.com.cn");
    }

    @Test
    public void parseS3Uri_withNonS3Uri_shouldThrowProperErrorMessage() {
        String nonS3UriString = "https://www.amazon.com/";
        URI nonS3Uri = URI.create(nonS3UriString);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            defaultUtilities.parseUri(nonS3Uri);
        });
        assertThat(exception.getMessage()).isEqualTo("Invalid S3 URI: hostname does not appear to be a valid S3 endpoint: "
                                                     + "https://www.amazon.com/");
    }

    @Test
    public void S3Uri_toString_printsCorrectOutput() {
        String uriString = "https://myBucket.s3.us-west-1.amazonaws.com/doc.txt?versionId=abc123&partNumber=77";
        URI uri = URI.create(uriString);

        S3Uri s3Uri = defaultUtilities.parseUri(uri);
        assertThat(s3Uri.toString()).isEqualTo("S3Uri(uri=https://myBucket.s3.us-west-1.amazonaws.com/doc.txt?"
                                               + "versionId=abc123&partNumber=77, bucket=myBucket, key=doc.txt, region=us-west-1,"
                                               + " isPathStyle=false, queryParams={versionId=[abc123], partNumber=[77]})");
    }

    @Test
    public void S3Uri_testEqualsAndHashCodeContract() {
        EqualsVerifier.forClass(S3Uri.class).verify();
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

    private static AwsCredentialsProvider dummyCreds() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
    }
}
