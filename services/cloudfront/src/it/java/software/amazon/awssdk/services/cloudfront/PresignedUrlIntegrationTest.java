/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.junit.Assert.assertEquals;
import static software.amazon.awssdk.test.util.DateUtils.yyMMddhhmmss;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.services.cloudfront.model.Aliases;
import software.amazon.awssdk.services.cloudfront.model.CacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.CacheBehaviors;
import software.amazon.awssdk.services.cloudfront.model.CloudFrontOriginAccessIdentityConfig;
import software.amazon.awssdk.services.cloudfront.model.CookiePreference;
import software.amazon.awssdk.services.cloudfront.model.CreateCloudFrontOriginAccessIdentityRequest;
import software.amazon.awssdk.services.cloudfront.model.CreateCloudFrontOriginAccessIdentityResponse;
import software.amazon.awssdk.services.cloudfront.model.CreateDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.CreateDistributionResponse;
import software.amazon.awssdk.services.cloudfront.model.DefaultCacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.DeleteDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.DistributionConfig;
import software.amazon.awssdk.services.cloudfront.model.ForwardedValues;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionConfigRequest;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionConfigResponse;
import software.amazon.awssdk.services.cloudfront.model.ItemSelection;
import software.amazon.awssdk.services.cloudfront.model.LoggingConfig;
import software.amazon.awssdk.services.cloudfront.model.Origin;
import software.amazon.awssdk.services.cloudfront.model.Origins;
import software.amazon.awssdk.services.cloudfront.model.PriceClass;
import software.amazon.awssdk.services.cloudfront.model.S3OriginConfig;
import software.amazon.awssdk.services.cloudfront.model.TrustedSigners;
import software.amazon.awssdk.services.cloudfront.model.UpdateDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.UpdateDistributionResponse;
import software.amazon.awssdk.services.cloudfront.util.SignerUtils;
import software.amazon.awssdk.services.cloudfront.util.SignerUtils.Protocol;
import software.amazon.awssdk.services.s3.model.AccessControlPolicy;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Grant;
import software.amazon.awssdk.services.s3.model.Grantee;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.Permission;
import software.amazon.awssdk.services.s3.model.PutObjectAclRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.Type;
import software.amazon.awssdk.sync.RequestBody;
import software.amazon.awssdk.test.util.RandomTempFile;
import software.amazon.awssdk.util.StringUtils;

/**
 * Tests pre-signed URLs
 */
@ReviewBeforeRelease("Tests a handwritten utility, would be good to integ test but right now they are broken" +
                     "and creating distributions is extremely slow")
@Ignore
public class PresignedUrlIntegrationTest extends IntegrationTestBase {

    private static final String PRIVATE_KEY_FILE = "pk-APKAJM22QV32R3I2XVIQ.pem";
    //    private static final String PRIVATE_KEY_FILE_DER = "pk-APKAJM22QV32R3I2XVIQ.der";
    private static final String PRIVATE_KEY_ID = "APKAJM22QV32R3I2XVIQ";
    private static final String DEFAULT_ROOT_OBJECT = "key.html";
    private static final String DISTRIBUTION_COMMENT = "comment";
    private static String callerReference = yyMMddhhmmss();
    private static final String bucketName = StringUtils.lowerCase(PresignedUrlIntegrationTest.class.getSimpleName())
                                             + "." + callerReference;
    private static String dnsName;
    private static String domainName;

    private static String distributionETag;
    private static String distributionId;

    @BeforeClass
    public static void initial() throws Exception {
        IntegrationTestBase.setUp();

        CreateCloudFrontOriginAccessIdentityResponse result =
            cloudfront.createCloudFrontOriginAccessIdentity(
                CreateCloudFrontOriginAccessIdentityRequest.builder().cloudFrontOriginAccessIdentityConfig(
                    CloudFrontOriginAccessIdentityConfig.builder()
                                                        .comment("new access identity")
                                                        .callerReference(callerReference).build()).build());

        String s3CanonicalUserId = result.cloudFrontOriginAccessIdentity()
                                         .s3CanonicalUserId();
        System.out.println("s3CanonicalUserId=" + s3CanonicalUserId);
        String accessId = result.cloudFrontOriginAccessIdentity().id();
        System.out.println("accessId=" + accessId);

        s3.createBucket(CreateBucketRequest.builder()
                                           .bucket(bucketName)
                                           .createBucketConfiguration(CreateBucketConfiguration.builder()
                                                                                               .locationConstraint("us-east-1")
                                                                                               .build())
                                           .build());

        dnsName = bucketName + ".s3.amazonaws.com";

        s3.putObject(PutObjectRequest.builder()
                                     .bucket(bucketName)
                                     .key("key")
                                     .build(),
                     RequestBody.of(new RandomTempFile("" + System.currentTimeMillis(), 1000L)));

        s3.putObjectAcl(PutObjectAclRequest.builder()
                                           .bucket(bucketName)
                                           .key("key")
                                           .acl(ObjectCannedACL.AuthenticatedRead)
                                           .accessControlPolicy(
                                               AccessControlPolicy.builder()
                                                                  .grants(Grant.builder()
                                                                               .grantee(Grantee.builder()
                                                                                               .type(Type.CanonicalUser)
                                                                                               .id(s3CanonicalUserId)
                                                                                               .build())
                                                                               .permission(Permission.READ)
                                                                               .build())
                                                                  .build())
                                           .build());

        // create a private distribution
        CreateDistributionRequest createDistributionRequest =
            CreateDistributionRequest.builder()
                                     .distributionConfig(
                                         DistributionConfig.builder()
                                                           .priceClass(PriceClass.PriceClass_100)
                                                           .defaultCacheBehavior(
                                                               DefaultCacheBehavior.builder()
                                                                                   .minTTL(100L)
                                                                                   .targetOriginId("1")
                                                                                   .viewerProtocolPolicy("allow-all")
                                                                                   .trustedSigners(
                                                                                       TrustedSigners.builder()
                                                                                                     .enabled(true)
                                                                                                     .quantity(1)
                                                                                                     .items("self")
                                                                                                     .build())
                                                                                   .forwardedValues(
                                                                                       ForwardedValues.builder()
                                                                                                      .cookies(CookiePreference.builder()
                                                                                                                               .forward(ItemSelection.None)
                                                                                                                               .build())
                                                                                                      .queryString(true)
                                                                                                      .build())
                                                                                   .build())
                                                           .aliases(
                                                               Aliases.builder()
                                                                      .quantity(0)
                                                                      .build())
                                                           .logging(
                                                               LoggingConfig.builder()
                                                                            .includeCookies(Boolean.FALSE)
                                                                            .enabled(false)
                                                                            .bucket(bucketName)
                                                                            .prefix("")
                                                                            .build())
                                                           .callerReference(callerReference)
                                                           .cacheBehaviors(
                                                               CacheBehaviors.builder()
                                                                             .quantity(1)
                                                                             .items(
                                                                                 CacheBehavior.builder()
                                                                                              .minTTL(100L)
                                                                                              .targetOriginId("1")
                                                                                              .viewerProtocolPolicy("allow-all")
                                                                                              .trustedSigners(
                                                                                                  TrustedSigners.builder()
                                                                                                                .enabled(true)
                                                                                                                .quantity(1)
                                                                                                                .items("self")
                                                                                                                .build())
                                                                                              .forwardedValues(
                                                                                                  ForwardedValues.builder()
                                                                                                                 .cookies(
                                                                                                                     CookiePreference.builder()
                                                                                                                                     .forward(ItemSelection.None)
                                                                                                                                     .build())
                                                                                                                 .queryString(true)
                                                                                                                 .build())
                                                                                              .pathPattern("*")
                                                                                              .build())
                                                                             .build())
                                                           .comment(DISTRIBUTION_COMMENT)
                                                           .defaultRootObject(DEFAULT_ROOT_OBJECT)
                                                           .enabled(true)
                                                           .origins(
                                                               Origins.builder()
                                                                      .items(
                                                                          Origin.builder()
                                                                                .domainName(dnsName)
                                                                                .id("1")
                                                                                .s3OriginConfig(
                                                                                    S3OriginConfig.builder()
                                                                                                  .originAccessIdentity(
                                                                                                      "origin-access-identity/cloudfront/"
                                                                                                      + accessId)
                                                                                                  .build())
                                                                                .build())
                                                                      .quantity(1)
                                                                      .build())
                                                           .build())
                                     .build();
        CreateDistributionResponse createDistributionResult = cloudfront.createDistribution(createDistributionRequest);

        domainName = createDistributionResult.distribution().domainName();
        distributionId = createDistributionResult.distribution().id();
        distributionETag = createDistributionResult.eTag();

        waitForDistributionToDeploy(distributionId);
    }

    @AfterClass
    public static void tearDown() throws Exception {

        // Disable the distribution
        GetDistributionConfigResponse distributionConfigResults =
            cloudfront.getDistributionConfig(GetDistributionConfigRequest.builder().id(distributionId).build());
        DistributionConfig distributionConfig = distributionConfigResults.distributionConfig().toBuilder().enabled(false).build();
        UpdateDistributionResponse updateDistributionResult = cloudfront
            .updateDistribution(UpdateDistributionRequest.builder().id(distributionId)
                                                         .ifMatch(distributionETag)
                                                         .distributionConfig(distributionConfig).build());
        distributionETag = updateDistributionResult.eTag();

        waitForDistributionToDeploy(distributionId);

        if (distributionId != null) {
            try {

                cloudfront.deleteDistribution(DeleteDistributionRequest.builder().id(distributionId).ifMatch(
                    distributionETag).build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        deleteBucketAndAllContents(bucketName);
    }

    @Test
    public void testUnsignedUri() throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(SignerUtils.generateResourcePath(Protocol.https, domainName, "key"));
        HttpResponse response = client.execute(httpGet);

        assertEquals(403, response.getStatusLine().getStatusCode());
    }
}