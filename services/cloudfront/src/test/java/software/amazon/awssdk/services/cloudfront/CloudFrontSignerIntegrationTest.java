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
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignedUrl.getSignedURLWithCannedPolicy;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignedUrl.getSignedURLWithCustomPolicy;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.generateResourceUrl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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
import software.amazon.awssdk.services.cloudfront.model.AllowedMethods;
import software.amazon.awssdk.services.cloudfront.model.CacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.CacheBehaviors;
import software.amazon.awssdk.services.cloudfront.model.CachedMethods;
import software.amazon.awssdk.services.cloudfront.model.CloudFrontOriginAccessIdentityConfig;
import software.amazon.awssdk.services.cloudfront.model.CookiePreference;
import software.amazon.awssdk.services.cloudfront.model.CreateCloudFrontOriginAccessIdentityRequest;
import software.amazon.awssdk.services.cloudfront.model.CreateCloudFrontOriginAccessIdentityResponse;
import software.amazon.awssdk.services.cloudfront.model.CreateDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.CreateDistributionResponse;
import software.amazon.awssdk.services.cloudfront.model.CreateKeyGroupRequest;
import software.amazon.awssdk.services.cloudfront.model.CreatePublicKeyRequest;
import software.amazon.awssdk.services.cloudfront.model.CreatePublicKeyResponse;
import software.amazon.awssdk.services.cloudfront.model.CustomErrorResponses;
import software.amazon.awssdk.services.cloudfront.model.CustomHeaders;
import software.amazon.awssdk.services.cloudfront.model.DefaultCacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.DeleteCloudFrontOriginAccessIdentityRequest;
import software.amazon.awssdk.services.cloudfront.model.DeleteDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.DeleteKeyGroupRequest;
import software.amazon.awssdk.services.cloudfront.model.DeletePublicKeyRequest;
import software.amazon.awssdk.services.cloudfront.model.DistributionConfig;
import software.amazon.awssdk.services.cloudfront.model.ForwardedValues;
import software.amazon.awssdk.services.cloudfront.model.GeoRestriction;
import software.amazon.awssdk.services.cloudfront.model.GeoRestrictionType;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionConfigRequest;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionConfigResponse;
import software.amazon.awssdk.services.cloudfront.model.GetPublicKeyRequest;
import software.amazon.awssdk.services.cloudfront.model.Headers;
import software.amazon.awssdk.services.cloudfront.model.HttpVersion;
import software.amazon.awssdk.services.cloudfront.model.KeyGroup;
import software.amazon.awssdk.services.cloudfront.model.KeyGroupConfig;
import software.amazon.awssdk.services.cloudfront.model.LambdaFunctionAssociations;
import software.amazon.awssdk.services.cloudfront.model.LoggingConfig;
import software.amazon.awssdk.services.cloudfront.model.Method;
import software.amazon.awssdk.services.cloudfront.model.MinimumProtocolVersion;
import software.amazon.awssdk.services.cloudfront.model.Origin;
import software.amazon.awssdk.services.cloudfront.model.Origins;
import software.amazon.awssdk.services.cloudfront.model.PriceClass;
import software.amazon.awssdk.services.cloudfront.model.PublicKeyConfig;
import software.amazon.awssdk.services.cloudfront.model.QueryStringCacheKeys;
import software.amazon.awssdk.services.cloudfront.model.Restrictions;
import software.amazon.awssdk.services.cloudfront.model.S3OriginConfig;
import software.amazon.awssdk.services.cloudfront.model.TrustedKeyGroups;
import software.amazon.awssdk.services.cloudfront.model.UpdateDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.UpdateDistributionResponse;
import software.amazon.awssdk.services.cloudfront.model.ViewerCertificate;
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
    private static final String callerReference = "2022-10-21"; //Instant.now().toString().substring(0,10);
    private static final String bucketName = StringUtils.lowerCase(CloudFrontSignerIntegrationTest.class.getSimpleName())
                                             + "." + callerReference;
    private static final String s3ObjectKey = "s3ObjectKey";
    private static String dnsName = bucketName + ".s3.amazonaws.com";
    private static String publicKeyId = "KJXP7B90W0K47";
    private static String domainName = "dbvkchd2z8bpx.cloudfront.net";
    private static String distributionId = "E3GN6IWUWAFY2";
    private static KeyPair keyPair;
    private static File keyFile = new File("key.pem");
    private static String keyGroupId = "bff6c847-086e-4886-af89-28b3250b9e0d";
    private static String originAccessId = "E2TR2JP2I1U6QH";
    private static String distributionETag;

    @BeforeAll
    public static void initial() throws IOException, InterruptedException, NoSuchAlgorithmException {
        IntegrationTestBase.setUp();
        //initKeys();
        //setUpDistribution();
    }

    @Test
    void getSignedURLWithCannedPolicy_shouldWork() throws Exception {
        InputStream originalBucketContent = s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(s3ObjectKey).build());
        ZonedDateTime expirationDate = ZonedDateTime.of(2050, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        String signedUrl = getSignedURLWithCannedPolicy(Protocol.HTTPS, domainName, s3ObjectKey, keyFile,
                                                        publicKeyId, expirationDate);
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
    void getSignedURLWithCannedPolicy_withExpiredDate_shouldReturn403Response() throws Exception {
        ZonedDateTime expirationDate = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        String signedUrl = getSignedURLWithCannedPolicy(Protocol.HTTPS, domainName, s3ObjectKey, keyFile,
                                                        publicKeyId, expirationDate);
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
    void getSignedURLWithCustomPolicy_shouldWork() throws Exception {
        InputStream originalBucketContent = s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(s3ObjectKey).build());
        ZonedDateTime activeDate = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime expirationDate = ZonedDateTime.of(2050, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        String signedUrl = getSignedURLWithCustomPolicy(Protocol.HTTPS, domainName, s3ObjectKey, keyFile, publicKeyId, activeDate, expirationDate, null);
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
    void getSignedURLWithCustomPolicy_withFutureActiveDate_shouldReturn403Response() throws Exception {
        ZonedDateTime activeDate = ZonedDateTime.of(2040, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime expirationDate = ZonedDateTime.of(2050, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        String signedUrl = getSignedURLWithCustomPolicy(Protocol.HTTPS, domainName, s3ObjectKey, keyFile, publicKeyId, activeDate, expirationDate, null);
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
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(encodedPath);
        httpGet.addHeader("Cookie", cookies.getExpires().getKey() + "=" + cookies.getExpires().getValue());
        httpGet.addHeader("Cookie", cookies.getSignature().getKey() + "=" + cookies.getSignature().getValue());
        httpGet.addHeader("Cookie", cookies.getKeyPairId().getKey() + "=" + cookies.getKeyPairId().getValue());
        HttpResponse responseCookie = httpClient.execute(httpGet);
        int expectedStatus = 200;
        assertThat(responseCookie.getStatusLine().getStatusCode()).isEqualTo(expectedStatus);

        InputStream retrievedBucketContent = responseCookie.getEntity().getContent();
        assertThat(retrievedBucketContent).hasSameContentAs(originalBucketContent);
    }

    @Test
    void getCookiesForCannedPolicy_withExpiredDate_shouldReturn403Response() throws Exception {
        ZonedDateTime expirationDate = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        CookiesForCannedPolicy cookies = getCookiesForCannedPolicy(Protocol.HTTPS, domainName, s3ObjectKey, keyFile, publicKeyId, expirationDate);
        String encodedPath = generateResourceUrl(Protocol.HTTPS, domainName, s3ObjectKey);
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(encodedPath);
        httpGet.addHeader("Cookie", cookies.getExpires().getKey() + "=" + cookies.getExpires().getValue());
        httpGet.addHeader("Cookie", cookies.getSignature().getKey() + "=" + cookies.getSignature().getValue());
        httpGet.addHeader("Cookie", cookies.getKeyPairId().getKey() + "=" + cookies.getKeyPairId().getValue());
        HttpResponse responseCookie = httpClient.execute(httpGet);
        int expectedStatus = 403;
        assertThat(responseCookie.getStatusLine().getStatusCode()).isEqualTo(expectedStatus);
    }

    @Test
    void getCookiesForCustomPolicy_shouldWork() throws Exception {
        InputStream originalBucketContent = s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(s3ObjectKey).build());
        ZonedDateTime activeDate = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime expirationDate = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        CookiesForCustomPolicy cookies = getCookiesForCustomPolicy(Protocol.HTTPS, domainName, s3ObjectKey, keyFile, publicKeyId, activeDate, expirationDate, null);
        String encodedPath = generateResourceUrl(Protocol.HTTPS, domainName, s3ObjectKey);
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(encodedPath);
        httpGet.addHeader("Cookie", cookies.getPolicy().getKey() + "=" + cookies.getPolicy().getValue());
        httpGet.addHeader("Cookie", cookies.getSignature().getKey() + "=" + cookies.getSignature().getValue());
        httpGet.addHeader("Cookie", cookies.getKeyPairId().getKey() + "=" + cookies.getKeyPairId().getValue());
        HttpResponse responseCookie = httpClient.execute(httpGet);
        int expectedStatus = 200;
        assertThat(responseCookie.getStatusLine().getStatusCode()).isEqualTo(expectedStatus);

        InputStream retrievedBucketContent = responseCookie.getEntity().getContent();
        assertThat(retrievedBucketContent).hasSameContentAs(originalBucketContent);
    }

    @Test
    void getCookiesForCustomPolicy_withFutureActiveDate_shouldReturn403Response() throws Exception {
        ZonedDateTime activeDate = ZonedDateTime.of(2040, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime expirationDate = ZonedDateTime.of(2050, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        CookiesForCustomPolicy cookies = getCookiesForCustomPolicy(Protocol.HTTPS, domainName, s3ObjectKey, keyFile, publicKeyId, activeDate, expirationDate, null);
        String encodedPath = generateResourceUrl(Protocol.HTTPS, domainName, s3ObjectKey);
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(encodedPath);
        httpGet.addHeader("Cookie", cookies.getPolicy().getKey() + "=" + cookies.getPolicy().getValue());
        httpGet.addHeader("Cookie", cookies.getSignature().getKey() + "=" + cookies.getSignature().getValue());
        httpGet.addHeader("Cookie", cookies.getKeyPairId().getKey() + "=" + cookies.getKeyPairId().getValue());
        HttpResponse responseCookie = httpClient.execute(httpGet);
        int expectedStatus = 403;
        assertThat(responseCookie.getStatusLine().getStatusCode()).isEqualTo(expectedStatus);
    }

    /*@AfterAll
    public static void tearDown() throws InterruptedException {
        //unable to disable distribution through API
        disableDistribution();

        if (distributionId != null) {
            try {
                cloudFrontClient.deleteDistribution(DeleteDistributionRequest.builder().ifMatch(distributionETag).id(distributionId).build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        deleteBucketAndAllContents(bucketName);
        cloudFrontClient.deleteKeyGroup(DeleteKeyGroupRequest.builder().id(keyGroupId).build());
        String publicKeyETag = cloudFrontClient.getPublicKey(GetPublicKeyRequest.builder().id(publicKeyId).build()).eTag();
        cloudFrontClient.deletePublicKey(DeletePublicKeyRequest.builder().ifMatch(publicKeyETag).id(publicKeyId).build());
        cloudFrontClient.deleteCloudFrontOriginAccessIdentity(DeleteCloudFrontOriginAccessIdentityRequest.builder().id(originAccessId).build());
    }*/

    static void setUpDistribution() throws IOException, InterruptedException {
        //Create Origin Access Identity
        CreateCloudFrontOriginAccessIdentityResponse response = cloudFrontClient.createCloudFrontOriginAccessIdentity(
            CreateCloudFrontOriginAccessIdentityRequest.builder()
                                                       .cloudFrontOriginAccessIdentityConfig(CloudFrontOriginAccessIdentityConfig.builder()
                                                                                                                                 .callerReference(callerReference)
                                                                                                                                 .comment("SignerTestAccessIdentity")
                                                                                                                                 .build())
                                                       .build());
        originAccessId = response.cloudFrontOriginAccessIdentity().id();

        // Create Cloudfront trusted key group
        KeyGroup keyGroup =
            cloudFrontClient.createKeyGroup(CreateKeyGroupRequest.builder().keyGroupConfig(KeyGroupConfig.builder()
                                                                                                     .name("TestKeyGroup")
                                                                                                     .items(publicKeyId)
                                                                                                     .build()).build()).keyGroup();
        keyGroupId = keyGroup.id();

        // Create S3 Bucket
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        dnsName = bucketName + ".s3.amazonaws.com";

        //Upload temp file to bucket
        File content = new RandomTempFile("" + System.currentTimeMillis(), 1000L);
        s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(s3ObjectKey).build(), RequestBody.fromFile(content));

        //Distribution Config Parameters
        DefaultCacheBehavior defaultCacheBehavior = DefaultCacheBehavior.builder()
            .forwardedValues(ForwardedValues.builder()
                                            .queryString(false).cookies(CookiePreference.builder().forward("none").build())
                                            .headers(Headers.builder().quantity(0).build()).build()).minTTL(10000L).maxTTL(10000L).defaultTTL(10000L)
                                               .viewerProtocolPolicy(ViewerProtocolPolicy.ALLOW_ALL)
                                               .targetOriginId("1")
                                               .trustedKeyGroups(TrustedKeyGroups.builder().enabled(true).quantity(1).items(keyGroup.id()).build()).build();

        CacheBehavior cacheBehavior = CacheBehavior.builder()
                                                   .forwardedValues(ForwardedValues.builder()
                                                                                   .queryString(false).cookies(CookiePreference.builder().forward("none").build())
                                                                                   .headers(Headers.builder().quantity(0).build()).build()).minTTL(10000L).maxTTL(10000L).defaultTTL(10000L)
                                                 .targetOriginId("1")
                                                 .viewerProtocolPolicy(ViewerProtocolPolicy.ALLOW_ALL)
                                                 .trustedKeyGroups(TrustedKeyGroups.builder().enabled(true).quantity(1).items(keyGroup.id()).build())
                                                 .pathPattern("*").build();

        Origin origin = Origin.builder()
                              .domainName(dnsName)
                              .id("1")
                              .s3OriginConfig(S3OriginConfig.builder().originAccessIdentity("origin-access-identity/cloudfront/" + originAccessId).build()).build();

        // Create CloudFront Distribution
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

        // Add bucket policy for Origin Access Identity to read bucket object
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
        //Generate key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        keyPair = kpg.generateKeyPair();

        //Write private key to file
        keyFile = new File("key.pem");
        FileWriter writer = new FileWriter(keyFile);
        writer.write("-----BEGIN PRIVATE KEY-----\n");
        writer.write(encoder.encodeToString(keyPair.getPrivate().getEncoded()));
        writer.write("\n-----END PRIVATE KEY-----\n");
        writer.close();

        //Upload public key to Cloudfront
        String encodedKey = "-----BEGIN PUBLIC KEY-----\n" + encoder.encodeToString(keyPair.getPublic().getEncoded())
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

        Origin origin = Origin.builder()
            .originPath("")
            .customHeaders(CustomHeaders.builder().quantity(0).build())
                              .domainName(dnsName)
                              .id("1")
                              .s3OriginConfig(S3OriginConfig.builder().originAccessIdentity("origin-access-identity/cloudfront/" + originAccessId).build()).build();

        DefaultCacheBehavior defaultCacheBehavior = DefaultCacheBehavior.builder()
                                                                        .targetOriginId("1")
                                                                        .minTTL(10000L).maxTTL(10000L).defaultTTL(10000L)
                                                                        .forwardedValues(ForwardedValues.builder()
                                                                                                        .queryString(false).queryStringCacheKeys(QueryStringCacheKeys.builder().quantity(0).build()).cookies(CookiePreference.builder().forward("all").build())
                                                                                                        .headers(Headers.builder().quantity(0).build()).build())
                                                                        .viewerProtocolPolicy(ViewerProtocolPolicy.ALLOW_ALL)
                                                                        .allowedMethods(AllowedMethods.builder().quantity(2).items(Method.GET, Method.HEAD).cachedMethods(CachedMethods.builder().quantity(2).items(Method.HEAD, Method.GET).build()).build())
                                                                        .smoothStreaming(false)
                                                                        .compress(false)
                                                                        .lambdaFunctionAssociations(LambdaFunctionAssociations.builder().quantity(0).build())
                                                                        .fieldLevelEncryptionId("")
                                                                        .build();

        CacheBehavior cacheBehavior = CacheBehavior.builder()
                                                   .targetOriginId("1")
                                                   .minTTL(10000L).maxTTL(10000L).defaultTTL(10000L)
                                                   .forwardedValues(ForwardedValues.builder()
                                                                                   .queryString(false).queryStringCacheKeys(QueryStringCacheKeys.builder().quantity(0).build()).cookies(CookiePreference.builder().forward("all").build())
                                                                                   .headers(Headers.builder().quantity(0).build()).build())
                                                   .pathPattern("*")
                                                   .viewerProtocolPolicy(ViewerProtocolPolicy.ALLOW_ALL)
                                                   .allowedMethods(AllowedMethods.builder().quantity(2).items(Method.GET, Method.HEAD).cachedMethods(CachedMethods.builder().quantity(2).items(Method.HEAD, Method.GET).build()).build())
                                                   .smoothStreaming(false)
                                                   .compress(false)
                                                   .lambdaFunctionAssociations(LambdaFunctionAssociations.builder().quantity(0).build())
                                                   .fieldLevelEncryptionId("")
                                                   .build();

        DistributionConfig updatedConfig= DistributionConfig.builder()
                                                      .enabled(false)
                                                      .callerReference(callerReference)
                                                      .aliases(Aliases.builder().quantity(0).build())
                                                      .defaultRootObject("")
                                                      .origins(Origins.builder().quantity(1).items(origin).build())
                                                      .defaultCacheBehavior(defaultCacheBehavior)
                                                      .cacheBehaviors(CacheBehaviors.builder().quantity(1).items(cacheBehavior).build())
                                                      .customErrorResponses(CustomErrorResponses.builder().quantity(0).build())
                                                      .comment("Disable Distribution Before Deleting")
                                                      .logging(LoggingConfig.builder().includeCookies(false).enabled(false).bucket(bucketName).prefix("").build())
                                                      .priceClass(PriceClass.PRICE_CLASS_100)
                                                      .viewerCertificate(ViewerCertificate.builder().minimumProtocolVersion(MinimumProtocolVersion.SSL_V3).build())
                                                      .restrictions(Restrictions.builder().geoRestriction(GeoRestriction.builder().quantity(1).restrictionType(GeoRestrictionType.NONE).build()).build())
                                                      .webACLId("")
                                                      .httpVersion(HttpVersion.HTTP3).build();

        //CloudFrontException: Rate Exceeded Error
        UpdateDistributionResponse updateDistributionResponse =
            cloudFrontClient.updateDistribution(UpdateDistributionRequest.builder().id(distributionId).ifMatch(distributionETag).distributionConfig(updatedConfig).build());

        distributionETag = updateDistributionResponse.eTag();
        waitForDistributionToDeploy(distributionId);
    }

}
