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

package software.amazon.awssdk.services.cloudfront;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignedCookie.getCookiesForCannedPolicy;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignedCookie.getCookiesForCustomPolicy;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignedUrl.getSignedUrlWithCannedPolicy;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignedUrl.getSignedUrlWithCustomPolicy;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.generateResourceUrl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.cloudfront.model.Aliases;
import software.amazon.awssdk.services.cloudfront.model.CacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.CacheBehaviors;
import software.amazon.awssdk.services.cloudfront.model.CloudFrontOriginAccessIdentityConfig;
import software.amazon.awssdk.services.cloudfront.model.CookiePreference;
import software.amazon.awssdk.services.cloudfront.model.CreateCloudFrontOriginAccessIdentityRequest;
import software.amazon.awssdk.services.cloudfront.model.CreateCloudFrontOriginAccessIdentityResponse;
import software.amazon.awssdk.services.cloudfront.model.CreateDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.CreateDistributionResponse;
import software.amazon.awssdk.services.cloudfront.model.CreateKeyGroupRequest;
import software.amazon.awssdk.services.cloudfront.model.CreatePublicKeyRequest;
import software.amazon.awssdk.services.cloudfront.model.CreatePublicKeyResponse;
import software.amazon.awssdk.services.cloudfront.model.DefaultCacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.DeleteCloudFrontOriginAccessIdentityRequest;
import software.amazon.awssdk.services.cloudfront.model.DeleteDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.DeleteKeyGroupRequest;
import software.amazon.awssdk.services.cloudfront.model.DeletePublicKeyRequest;
import software.amazon.awssdk.services.cloudfront.model.DistributionConfig;
import software.amazon.awssdk.services.cloudfront.model.ForwardedValues;
import software.amazon.awssdk.services.cloudfront.model.GetCloudFrontOriginAccessIdentityRequest;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionConfigRequest;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionConfigResponse;
import software.amazon.awssdk.services.cloudfront.model.GetKeyGroupRequest;
import software.amazon.awssdk.services.cloudfront.model.GetPublicKeyRequest;
import software.amazon.awssdk.services.cloudfront.model.Headers;
import software.amazon.awssdk.services.cloudfront.model.KeyGroup;
import software.amazon.awssdk.services.cloudfront.model.KeyGroupConfig;
import software.amazon.awssdk.services.cloudfront.model.LoggingConfig;
import software.amazon.awssdk.services.cloudfront.model.Origin;
import software.amazon.awssdk.services.cloudfront.model.Origins;
import software.amazon.awssdk.services.cloudfront.model.PriceClass;
import software.amazon.awssdk.services.cloudfront.model.PublicKeyConfig;
import software.amazon.awssdk.services.cloudfront.model.S3OriginConfig;
import software.amazon.awssdk.services.cloudfront.model.TrustedKeyGroups;
import software.amazon.awssdk.services.cloudfront.model.UpdateDistributionResponse;
import software.amazon.awssdk.services.cloudfront.model.ViewerProtocolPolicy;
import software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignedCookie.CookiesForCannedPolicy;
import software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignedCookie.CookiesForCustomPolicy;
import software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.Protocol;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.utils.StringUtils;

public class CloudFrontSignerIntegrationTest extends IntegrationTestBase {
    private static final Base64.Encoder encoder = Base64.getEncoder();
    private static final String callerReference = Instant.now().toString().substring(0,10);
    private static final String bucketName = StringUtils.lowerCase(CloudFrontSignerIntegrationTest.class.getSimpleName())
                                             + "." + callerReference;
    private static final String s3ObjectKey = "s3ObjectKey";
    private static String publicKeyId;
    private static String domainName;
    private static String distributionId;
    private static File keyFile;
    private static String keyGroupId;
    private static String originAccessId;
    private static String distributionETag;

    @BeforeAll
    public static void init() throws IOException, InterruptedException, NoSuchAlgorithmException {
        IntegrationTestBase.setUp();
        initKeys();
        setUpDistribution();
    }

    @AfterAll
    public static void tearDown() throws InterruptedException {
        disableDistribution();
        cloudFrontClient.deleteDistribution(DeleteDistributionRequest.builder().ifMatch(distributionETag).id(distributionId).build());
        deleteBucketAndAllContents(bucketName);
        String keyGroupETag = cloudFrontClient.getKeyGroup(GetKeyGroupRequest.builder().id(keyGroupId).build()).eTag();
        cloudFrontClient.deleteKeyGroup(DeleteKeyGroupRequest.builder().ifMatch(keyGroupETag).id(keyGroupId).build());
        String publicKeyETag = cloudFrontClient.getPublicKey(GetPublicKeyRequest.builder().id(publicKeyId).build()).eTag();
        cloudFrontClient.deletePublicKey(DeletePublicKeyRequest.builder().ifMatch(publicKeyETag).id(publicKeyId).build());
        String originAccessIdETag = cloudFrontClient.getCloudFrontOriginAccessIdentity(GetCloudFrontOriginAccessIdentityRequest.builder().id(originAccessId).build()).eTag();
        cloudFrontClient.deleteCloudFrontOriginAccessIdentity(DeleteCloudFrontOriginAccessIdentityRequest.builder().ifMatch(originAccessIdETag).id(originAccessId).build());
        keyFile.deleteOnExit();
    }

    @Test
    void unsignedUrl_shouldReturn403Response() throws Exception {
        String unsignedUrl = generateResourceUrl(Protocol.HTTPS, domainName, s3ObjectKey);
        SdkHttpClient client = ApacheHttpClient.create();
        HttpExecuteResponse response =
            client.prepareRequest(HttpExecuteRequest.builder()
                                                    .request(SdkHttpRequest.builder()
                                                                           .encodedPath(unsignedUrl)
                                                                           .host(domainName)
                                                                           .method(SdkHttpMethod.GET)
                                                                           .protocol("https")
                                                                           .build())
                                                    .build()).call();
        int expectedStatus = 403;
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatus);
    }

    @Test
    void getSignedUrlWithCannedPolicy_shouldWork() throws Exception {
        InputStream originalBucketContent = s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(s3ObjectKey).build());
        ZonedDateTime expirationDate = ZonedDateTime.of(2050, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        String signedUrl = getSignedUrlWithCannedPolicy(Protocol.HTTPS, domainName, s3ObjectKey, keyFile, publicKeyId, expirationDate);
        String encodedPath = signedUrl.substring(signedUrl.indexOf("s3ObjectKey"));
        SdkHttpClient client = ApacheHttpClient.create();
        HttpExecuteResponse response =
            client.prepareRequest(HttpExecuteRequest.builder()
                                                    .request(SdkHttpRequest.builder()
                                                                           .encodedPath(encodedPath)
                                                                           .host(domainName)
                                                                           .method(SdkHttpMethod.GET)
                                                                           .protocol("https")
                                                                           .build())
                                                    .build()).call();
        int expectedStatus = 200;
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatus);

        InputStream retrievedBucketContent = response.responseBody().get();
        assertThat(retrievedBucketContent).hasSameContentAs(originalBucketContent);
    }

    @Test
    void getSignedUrlWithCannedPolicy_withExpiredDate_shouldReturn403Response() throws Exception {
        ZonedDateTime expirationDate = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        String signedUrl = getSignedUrlWithCannedPolicy(Protocol.HTTPS, domainName, s3ObjectKey, keyFile, publicKeyId, expirationDate);
        String encodedPath = signedUrl.substring(signedUrl.indexOf("s3ObjectKey"));
        SdkHttpClient client = ApacheHttpClient.create();
        HttpExecuteResponse response =
            client.prepareRequest(HttpExecuteRequest.builder()
                                                    .request(SdkHttpRequest.builder()
                                                                           .encodedPath(encodedPath)
                                                                           .host(domainName)
                                                                           .method(SdkHttpMethod.GET)
                                                                           .protocol("https")
                                                                           .build())
                                                    .build()).call();
        int expectedStatus = 403;
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatus);
    }

    @Test
    void getSignedUrlWithCustomPolicy_shouldWork() throws Exception {
        InputStream originalBucketContent = s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(s3ObjectKey).build());
        ZonedDateTime activeDate = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime expirationDate = ZonedDateTime.of(2050, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        String signedUrl = getSignedUrlWithCustomPolicy(Protocol.HTTPS, domainName, s3ObjectKey, keyFile, publicKeyId, activeDate, expirationDate, null);
        String encodedPath = signedUrl.substring(signedUrl.indexOf("s3ObjectKey"));
        SdkHttpClient client = ApacheHttpClient.create();
        HttpExecuteResponse response =
            client.prepareRequest(HttpExecuteRequest.builder()
                                                    .request(SdkHttpRequest.builder()
                                                                           .encodedPath(encodedPath)
                                                                           .host(domainName)
                                                                           .method(SdkHttpMethod.GET)
                                                                           .protocol("https")
                                                                           .build())
                                                    .build()).call();
        int expectedStatus = 200;
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatus);

        InputStream retrievedBucketContent = response.responseBody().get();
        assertThat(retrievedBucketContent).hasSameContentAs(originalBucketContent);
    }

    @Test
    void getSignedUrlWithCustomPolicy_withFutureActiveDate_shouldReturn403Response() throws Exception {
        ZonedDateTime activeDate = ZonedDateTime.of(2040, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime expirationDate = ZonedDateTime.of(2050, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        String signedUrl = getSignedUrlWithCustomPolicy(Protocol.HTTPS, domainName, s3ObjectKey, keyFile, publicKeyId, activeDate, expirationDate, null);
        String encodedPath = signedUrl.substring(signedUrl.indexOf("s3ObjectKey"));
        SdkHttpClient client = ApacheHttpClient.create();
        HttpExecuteResponse response =
            client.prepareRequest(HttpExecuteRequest.builder()
                                                    .request(SdkHttpRequest.builder()
                                                                           .encodedPath(encodedPath)
                                                                           .host(domainName)
                                                                           .method(SdkHttpMethod.GET)
                                                                           .protocol("https")
                                                                           .build())
                                                    .build()).call();
        int expectedStatus = 403;
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatus);
    }

    @Test
    void getCookiesForCannedPolicy_shouldWork() throws Exception {
        InputStream originalBucketContent = s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(s3ObjectKey).build());
        ZonedDateTime expirationDate = ZonedDateTime.of(2050, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        CookiesForCannedPolicy cookies = getCookiesForCannedPolicy(Protocol.HTTPS, domainName, s3ObjectKey, keyFile, publicKeyId, expirationDate);
        String encodedPath = generateResourceUrl(Protocol.HTTPS, domainName, s3ObjectKey);

        SdkHttpClient client = ApacheHttpClient.create();
        HttpExecuteResponse response =
            client.prepareRequest(HttpExecuteRequest.builder()
                                                    .request(SdkHttpRequest.builder()
                                                                           .uri(URI.create(encodedPath))
                                                                           .appendHeader("Cookie",
                                                                                         cookies.getExpires().getKey() + "=" + cookies.getExpires().getValue())
                                                                           .appendHeader("Cookie",
                                                                                         cookies.getSignature().getKey() + "=" + cookies.getSignature().getValue())
                                                                           .appendHeader("Cookie",
                                                                                         cookies.getKeyPairId().getKey() + "=" + cookies.getKeyPairId().getValue())
                                                                           .method(SdkHttpMethod.GET)
                                                                           .build())
                                                    .build()).call();
        int expectedStatus = 200;
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatus);

        InputStream retrievedBucketContent = response.responseBody().get();
        assertThat(retrievedBucketContent).hasSameContentAs(originalBucketContent);
    }

    @Test
    void getCookiesForCannedPolicy_withExpiredDate_shouldReturn403Response() throws Exception {
        ZonedDateTime expirationDate = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        CookiesForCannedPolicy cookies = getCookiesForCannedPolicy(Protocol.HTTPS, domainName, s3ObjectKey, keyFile, publicKeyId, expirationDate);
        String encodedPath = generateResourceUrl(Protocol.HTTPS, domainName, s3ObjectKey);

        SdkHttpClient client = ApacheHttpClient.create();
        HttpExecuteResponse response =
            client.prepareRequest(HttpExecuteRequest.builder()
                                                    .request(SdkHttpRequest.builder()
                                                                           .uri(URI.create(encodedPath))
                                                                           .appendHeader("Cookie",
                                                                                         cookies.getExpires().getKey() + "=" + cookies.getExpires().getValue())
                                                                           .appendHeader("Cookie",
                                                                                         cookies.getSignature().getKey() + "=" + cookies.getSignature().getValue())
                                                                           .appendHeader("Cookie",
                                                                                         cookies.getKeyPairId().getKey() + "=" + cookies.getKeyPairId().getValue())
                                                                           .method(SdkHttpMethod.GET)
                                                                           .build())
                                                    .build()).call();
        int expectedStatus = 403;
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatus);
    }

    @Test
    void getCookiesForCustomPolicy_shouldWork() throws Exception {
        InputStream originalBucketContent = s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(s3ObjectKey).build());
        ZonedDateTime activeDate = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime expirationDate = ZonedDateTime.of(2050, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        CookiesForCustomPolicy cookies = getCookiesForCustomPolicy(Protocol.HTTPS, domainName, s3ObjectKey, keyFile, publicKeyId, activeDate, expirationDate, null);
        String encodedPath = generateResourceUrl(Protocol.HTTPS, domainName, s3ObjectKey);

        SdkHttpClient client = ApacheHttpClient.create();
        HttpExecuteResponse response =
            client.prepareRequest(HttpExecuteRequest.builder()
                                                    .request(SdkHttpRequest.builder()
                                                                           .uri(URI.create(encodedPath))
                                                                           .appendHeader("Cookie",
                                                                                         cookies.getPolicy().getKey() + "=" + cookies.getPolicy().getValue())
                                                                           .appendHeader("Cookie",
                                                                                         cookies.getSignature().getKey() + "=" + cookies.getSignature().getValue())
                                                                           .appendHeader("Cookie",
                                                                                         cookies.getKeyPairId().getKey() + "=" + cookies.getKeyPairId().getValue())
                                                                           .method(SdkHttpMethod.GET)
                                                                           .build())
                                                    .build()).call();
        int expectedStatus = 200;
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatus);

        InputStream retrievedBucketContent = response.responseBody().get();
        assertThat(retrievedBucketContent).hasSameContentAs(originalBucketContent);
    }

    @Test
    void getCookiesForCustomPolicy_withFutureActiveDate_shouldReturn403Response() throws Exception {
        ZonedDateTime activeDate = ZonedDateTime.of(2040, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime expirationDate = ZonedDateTime.of(2050, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        CookiesForCustomPolicy cookies = getCookiesForCustomPolicy(Protocol.HTTPS, domainName, s3ObjectKey, keyFile, publicKeyId, activeDate, expirationDate, null);
        String encodedPath = generateResourceUrl(Protocol.HTTPS, domainName, s3ObjectKey);

        SdkHttpClient client = ApacheHttpClient.create();
        HttpExecuteResponse response =
            client.prepareRequest(HttpExecuteRequest.builder()
                                                    .request(SdkHttpRequest.builder()
                                                                           .uri(URI.create(encodedPath))
                                                                           .appendHeader("Cookie",
                                                                                         cookies.getPolicy().getKey() + "=" + cookies.getPolicy().getValue())
                                                                           .appendHeader("Cookie",
                                                                                         cookies.getSignature().getKey() + "=" + cookies.getSignature().getValue())
                                                                           .appendHeader("Cookie",
                                                                                         cookies.getKeyPairId().getKey() + "=" + cookies.getKeyPairId().getValue())
                                                                           .method(SdkHttpMethod.GET)
                                                                           .build())
                                                    .build()).call();
        int expectedStatus = 403;
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatus);
    }

    static void setUpDistribution() throws IOException, InterruptedException {
        CreateCloudFrontOriginAccessIdentityResponse response = cloudFrontClient.createCloudFrontOriginAccessIdentity(
            CreateCloudFrontOriginAccessIdentityRequest.builder()
                                                       .cloudFrontOriginAccessIdentityConfig(CloudFrontOriginAccessIdentityConfig.builder()
                                                                                                                                 .callerReference(callerReference)
                                                                                                                                 .comment("SignerTestAccessIdentity")
                                                                                                                                 .build())
                                                       .build());
        originAccessId = response.cloudFrontOriginAccessIdentity().id();

        KeyGroup keyGroup =
            cloudFrontClient.createKeyGroup(CreateKeyGroupRequest.builder().keyGroupConfig(KeyGroupConfig.builder()
                                                                                                     .name("TestKeyGroup")
                                                                                                     .items(publicKeyId)
                                                                                                     .build()).build()).keyGroup();
        keyGroupId = keyGroup.id();

        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        File content = new RandomTempFile("testFile", 1000L);
        s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(s3ObjectKey).build(), RequestBody.fromFile(content));

        DefaultCacheBehavior defaultCacheBehavior = DefaultCacheBehavior.builder()
                                                                        .forwardedValues(ForwardedValues.builder()
                                                                                                        .queryString(false).cookies(CookiePreference.builder().forward("none").build())
                                                                                                        .headers(Headers.builder().quantity(0).build()).build()).minTTL(10000L).maxTTL(10000L).defaultTTL(10000L)
                                                                        .targetOriginId("1")
                                                                        .viewerProtocolPolicy(ViewerProtocolPolicy.ALLOW_ALL)
                                                                        .trustedKeyGroups(TrustedKeyGroups.builder().enabled(true).quantity(1).items(keyGroup.id()).build()).build();

        CacheBehavior cacheBehavior = CacheBehavior.builder()
                                                   .forwardedValues(ForwardedValues.builder()
                                                                                   .queryString(false).cookies(CookiePreference.builder().forward("none").build())
                                                                                   .headers(Headers.builder().quantity(0).build()).build()).minTTL(10000L).maxTTL(10000L).defaultTTL(10000L)
                                                   .targetOriginId("1")
                                                   .viewerProtocolPolicy(ViewerProtocolPolicy.ALLOW_ALL)
                                                   .trustedKeyGroups(TrustedKeyGroups.builder().enabled(true).quantity(1).items(keyGroup.id()).build()).pathPattern("*").build();

        Origin origin = Origin.builder()
                              .domainName(bucketName + ".s3.amazonaws.com")
                              .id("1")
                              .s3OriginConfig(S3OriginConfig.builder().originAccessIdentity("origin-access-identity/cloudfront/" + originAccessId).build()).build();

        DistributionConfig distributionConfiguration = DistributionConfig.builder()
                                                                         .priceClass(PriceClass.PRICE_CLASS_100)
                                                                         .defaultCacheBehavior(defaultCacheBehavior)
                                                                         .aliases(Aliases.builder().quantity(0).build())
                                                                         .logging(LoggingConfig.builder()
                                                                                               .includeCookies(false)
                                                                                               .enabled(false)
                                                                                               .bucket(bucketName)
                                                                                               .prefix("").build())
                                                                         .callerReference(callerReference)
                                                                         .cacheBehaviors(CacheBehaviors.builder()
                                                                                                       .quantity(1)
                                                                                                       .items(cacheBehavior).build())
                                                                         .comment("PresignerTestDistribution")
                                                                         .defaultRootObject("")
                                                                         .enabled(true)
                                                                         .origins(Origins.builder()
                                                                                         .quantity(1)
                                                                                         .items(origin).build()).build();

        CreateDistributionResponse createDistributionResponse =
            cloudFrontClient.createDistribution(CreateDistributionRequest.builder().distributionConfig(distributionConfiguration).build());

        domainName = createDistributionResponse.distribution().domainName();
        distributionId = createDistributionResponse.distribution().id();
        distributionETag = createDistributionResponse.eTag();

        waitForDistributionToDeploy(distributionId);

        String bucketPolicy = "{\n"
                              + "\"Version\":\"2012-10-17\",\n"
                              + "\"Id\":\"PolicyForCloudFrontPrivateContent\",\n"
                              + "\"Statement\":[\n"
                              + "{\n"
                              + "\"Effect\":\"Allow\",\n"
                              + "\"Principal\":{\n"
                              + "\"AWS\":\"arn:aws:iam::cloudfront:user/CloudFront Origin Access Identity " + originAccessId + "\"\n"
                              + "},\n"
                              + "\"Action\":\"s3:GetObject\",\n"
                              + "\"Resource\":\"arn:aws:s3:::" + bucketName + "/*\"\n"
                              + "}\n"
                              + "]\n"
                              + "}";

        s3Client.putBucketPolicy(PutBucketPolicyRequest.builder().bucket(bucketName).policy(bucketPolicy).build());
    }

    static void initKeys() throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();

        keyFile = new File("src/test/key.pem");
        FileWriter writer = new FileWriter(keyFile);
        writer.write("-----BEGIN PRIVATE KEY-----\n");
        writer.write(encoder.encodeToString(keyPair.getPrivate().getEncoded()));
        writer.write("\n-----END PRIVATE KEY-----\n");
        writer.close();

        String encodedKey = "-----BEGIN PUBLIC KEY-----\n"
                            + encoder.encodeToString(keyPair.getPublic().getEncoded())
                            + "\n-----END PUBLIC KEY-----\n";
        CreatePublicKeyResponse publicKeyResponse =
            cloudFrontClient.createPublicKey(CreatePublicKeyRequest.builder().publicKeyConfig(PublicKeyConfig.builder()
                                                                                                             .callerReference(callerReference)
                                                                                                             .name("testKey")
                                                                                                             .encodedKey(encodedKey).build()).build());
        publicKeyId = publicKeyResponse.publicKey().id();
    }

    static void disableDistribution() throws InterruptedException {
        GetDistributionConfigResponse distributionConfigResponse =
            cloudFrontClient.getDistributionConfig(GetDistributionConfigRequest.builder().id(distributionId).build());
        distributionETag = distributionConfigResponse.eTag();
        DistributionConfig originalConfig = distributionConfigResponse.distributionConfig();
        UpdateDistributionResponse updateDistributionResponse =
            cloudFrontClient.updateDistribution(r -> r.id(distributionId)
                                                      .ifMatch(distributionETag)
                                                      .distributionConfig(originalConfig.toBuilder()
                                                                                        .enabled(false)
                                                                                        .build()));
        distributionETag = updateDistributionResponse.eTag();
        waitForDistributionToDeploy(distributionId);
    }

}
