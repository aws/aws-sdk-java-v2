/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.s3.endpoints.internal;

import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.endpoints.authscheme.S3ExpressEndpointAuthScheme;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
// cost optimized round 2 bdd.  boolean and loop optimizations.  Method references  NO uriCreate optimizations
public final class BddCostOpt2Subgraphs implements S3EndpointProvider {

    private static boolean cond0(Registers registers) {
        return (registers.region != null);
    }

    private static boolean cond1(Registers registers) {
        return (registers.endpoint != null);
    }

    private static boolean cond2(Registers registers) {
        return (registers.bucket != null);
    }

    private static boolean cond3(Registers registers) {
        return (registers.useS3ExpressControlEndpoint != null);
    }

    private static boolean cond4(Registers registers) {
        return (registers.accelerate);
    }

    private static boolean cond5(Registers registers) {
        return (registers.useFIPS);
    }

    private static boolean cond6(Registers registers) {
        return ("aws-cn".equals(registers.partitionResult.name()));
    }

    private static boolean cond7(Registers registers) {
        return (registers.disableS3ExpressSessionAuth != null);
    }

    private static boolean cond8(Registers registers) {
        return (Boolean.FALSE != registers.useS3ExpressControlEndpoint);
    }

    private static boolean cond9(Registers registers) {
        return (Boolean.FALSE != registers.disableS3ExpressSessionAuth);
    }

    private static boolean cond10(Registers registers) {
        return (registers.forcePathStyle);
    }

    private static boolean cond11(Registers registers) {
        return (RulesFunctions.awsParseArn(registers.bucket) != null);
    }

    private static boolean cond12(Registers registers) {
        return (RulesFunctions.awsIsVirtualHostableS3Bucket(registers.bucket, false));
    }

    private static boolean cond13(Registers registers) {
        return (RulesFunctions.awsIsVirtualHostableS3Bucket(registers.bucket, true));
    }

    private static boolean cond14(Registers registers) {
        return ("http".equals(registers.url.scheme()));
    }

    private static boolean cond15(Registers registers) {
        return (registers.useObjectLambdaEndpoint != null);
    }

    private static boolean cond16(Registers registers) {
        return ("--op-s3".equals(registers.accessPointSuffix));
    }

    private static boolean cond17(Registers registers) {
        return (Boolean.FALSE != registers.useObjectLambdaEndpoint);
    }

    private static boolean cond18(Registers registers) {
        return ("beta".equals(registers.regionPrefix));
    }

    private static boolean cond19(Registers registers) {
        registers.partitionResult = RulesFunctions.awsPartition(registers.region);
        return registers.partitionResult != null;
    }

    private static boolean cond20(Registers registers) {
        return (RulesFunctions.awsParseArn(registers.bucket) != null);
    }

    private static boolean cond21(Registers registers) {
        registers.outpostId_ssa_2 = RulesFunctions.substring(registers.bucket, 32, 49, true);
        return registers.outpostId_ssa_2 != null;
    }

    private static boolean cond22(Registers registers) {
        registers.hardwareType = RulesFunctions.substring(registers.bucket, 49, 50, true);
        return registers.hardwareType != null;
    }

    private static boolean cond23(Registers registers) {
        registers.accessPointSuffix = RulesFunctions.substring(registers.bucket, 0, 7, true);
        return registers.accessPointSuffix != null;
    }

    private static boolean cond24(Registers registers) {
        registers.regionPrefix = RulesFunctions.substring(registers.bucket, 8, 12, true);
        return registers.regionPrefix != null;
    }

    private static boolean cond25(Registers registers) {
        return (registers.url.isIp());
    }

    private static boolean cond26(Registers registers) {
        return (registers.disableAccessPoints != null);
    }

    private static boolean cond27(Registers registers) {
        return (registers.useDualStack);
    }

    private static boolean cond28(Registers registers) {
        return ("e".equals(registers.hardwareType));
    }

    private static boolean cond29(Registers registers) {
        return ("o".equals(registers.hardwareType));
    }

    private static boolean cond30(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_6 = RulesFunctions.substring(registers.bucket, 7, 15, true);
        return registers.s3expressAvailabilityZoneId_ssa_6 != null;
    }

    private static boolean cond31(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_2 = RulesFunctions.substring(registers.bucket, 6, 15, true);
        return registers.s3expressAvailabilityZoneId_ssa_2 != null;
    }

    private static boolean cond32(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
        return registers.s3expressAvailabilityZoneId_ssa_7 != null;
    }

    private static boolean cond33(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.region, true));
    }

    private static boolean cond34(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.region, false));
    }

    private static boolean cond35(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.outpostId_ssa_2, false));
    }

    private static boolean cond36(Registers registers) {
        registers.bucketArn = RulesFunctions.awsParseArn(registers.bucket);
        return registers.bucketArn != null;
    }

    private static boolean cond37(Registers registers) {
        registers.uri_encoded_bucket = RulesFunctions.uriEncode(registers.bucket);
        return registers.uri_encoded_bucket != null;
    }

    private static boolean cond38(Registers registers) {
        registers.url = RulesFunctions.parseURL(registers.endpoint);
        return registers.url != null;
    }

    private static boolean cond39(Registers registers) {
        return (registers.useArnRegion != null);
    }

    private static boolean cond40(Registers registers) {
        return (Boolean.FALSE != registers.disableAccessPoints);
    }

    private static boolean cond41(Registers registers) {
        registers.arnType = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 0);
        return registers.arnType != null;
    }

    private static boolean cond42(Registers registers) {
        return (!registers.useArnRegion);
    }

    private static boolean cond43(Registers registers) {
        registers.outpostId_ssa_1 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 1);
        return registers.outpostId_ssa_1 != null;
    }

    private static boolean cond44(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
        return registers.s3expressAvailabilityZoneId_ssa_1 != null;
    }

    private static boolean cond45(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
        return registers.s3expressAvailabilityZoneId_ssa_8 != null;
    }

    private static boolean cond46(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
        return registers.s3expressAvailabilityZoneId_ssa_3 != null;
    }

    private static boolean cond47(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
        return registers.s3expressAvailabilityZoneId_ssa_4 != null;
    }

    private static boolean cond48(Registers registers) {
        return (RulesFunctions.listAccess(registers.bucketArn.resourceId(), 4) != null);
    }

    private static boolean cond49(Registers registers) {
        registers.accessPointName_ssa_1 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 1);
        return registers.accessPointName_ssa_1 != null;
    }

    private static boolean cond50(Registers registers) {
        return ("accesspoint".equals(registers.arnType));
    }

    private static boolean cond51(Registers registers) {
        return ("".equals(registers.arnType));
    }

    private static boolean cond52(Registers registers) {
        return ("".equals(registers.accessPointName_ssa_1));
    }

    private static boolean cond53(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
        return registers.s3expressAvailabilityZoneId_ssa_9 != null;
    }

    private static boolean cond54(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
        return registers.s3expressAvailabilityZoneId_ssa_5 != null;
    }

    private static boolean cond55(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
        return registers.s3expressAvailabilityZoneId_ssa_10 != null;
    }

    private static boolean cond56(Registers registers) {
        return (RulesFunctions.listAccess(registers.bucketArn.resourceId(), 2) != null);
    }

    private static boolean cond57(Registers registers) {
        return ("".equals(registers.bucketArn.region()));
    }

    private static boolean cond58(Registers registers) {
        return ("s3-object-lambda".equals(registers.bucketArn.service()));
    }

    private static boolean cond59(Registers registers) {
        return ("s3-outposts".equals(registers.bucketArn.service()));
    }

    private static boolean cond60(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.outpostId_ssa_1, false));
    }

    private static boolean cond61(Registers registers) {
        return (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region()));
    }

    private static boolean cond62(Registers registers) {
        return (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name()));
    }

    private static boolean cond63(Registers registers) {
        return (registers.disableMultiRegionAccessPoints);
    }

    private static boolean cond64(Registers registers) {
        return (registers.useGlobalEndpoint);
    }

    private static boolean cond65(Registers registers) {
        registers.outpostType = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 2);
        return registers.outpostType != null;
    }

    private static boolean cond66(Registers registers) {
        registers.accessPointName_ssa_2 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 3);
        return registers.accessPointName_ssa_2 != null;
    }

    private static boolean cond67(Registers registers) {
        return ("accesspoint".equals(registers.outpostType));
    }

    private static boolean cond68(Registers registers) {
        return ("aws-global".equals(registers.region));
    }

    private static boolean cond69(Registers registers) {
        return ("us-east-1".equals(registers.region));
    }

    private static boolean cond70(Registers registers) {
        return (!registers.url.isIp());
    }

    private static boolean cond71(Registers registers) {
        return ("".equals(registers.bucketArn.accountId()));
    }

    private static boolean cond72(Registers registers) {
        return ("s3".equals(registers.bucketArn.service()));
    }

    private static boolean cond73(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false));
    }

    private static boolean cond74(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, true));
    }

    private static boolean cond75(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true));
    }

    private static boolean cond76(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false));
    }

    private static boolean cond77(Registers registers) {
        return (RulesFunctions.stringEquals(registers.bucketArn.partition(), registers.partitionResult.name()));
    }

    private static boolean cond78(Registers registers) {
        registers.bucketPartition = RulesFunctions.awsPartition(registers.bucketArn.region());
        return registers.bucketPartition != null;
    }

    private static boolean cond79(Registers registers) {
        return ("--x-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 0, 6, true), "")));
    }

    private static boolean cond80(Registers registers) {
        return ("--xa-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 0, 7, true), "")));
    }

    private static boolean cond81(Registers registers) {
        return ("arn:".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 0, 4, false), "")));
    }

    private static boolean cond82(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), "")));
    }

    private static boolean cond83(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), "")));
    }

    private static boolean cond84(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), "")));
    }

    private static boolean cond85(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), "")));
    }

    private static boolean cond86(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), "")));
    }

    private static boolean cond87(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), "")));
    }

    private static boolean cond88(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), "")));
    }

    private static boolean cond89(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), "")));
    }

    private static boolean cond90(Registers registers) {
        return (RulesFunctions.parseURL(registers.endpoint) != null);
    }

    private static RuleResult result0(Registers registers) {
        return RuleResult.error("Accelerate cannot be used with FIPS");
    }

    private static RuleResult result1(Registers registers) {
        return RuleResult.error("Cannot set dual-stack in combination with a custom endpoint.");
    }

    private static RuleResult result2(Registers registers) {
        return RuleResult.error("A custom endpoint cannot be combined with FIPS");
    }

    private static RuleResult result3(Registers registers) {
        return RuleResult.error("A custom endpoint cannot be combined with S3 Accelerate");
    }

    private static RuleResult result4(Registers registers) {
        return RuleResult.error("Partition does not support FIPS");
    }

    private static RuleResult result5(Registers registers) {
        return RuleResult.error("S3Express does not support S3 Accelerate.");
    }

    private static RuleResult result6(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + "/" + registers.uri_encoded_bucket
                                                       + registers.url.path()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result7(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.bucket + "." + registers.url.authority()
                                                       + registers.url.path()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result8(Registers registers) {
        return RuleResult.error("S3Express bucket name is not a valid virtual hostable name.");
    }

    private static RuleResult result9(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + "/" + registers.uri_encoded_bucket
                                                       + registers.url.path()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result10(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.bucket + "." + registers.url.authority()
                                                       + registers.url.path()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result11(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3express-control-fips.dualstack." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result12(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3express-control-fips." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result13(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3express-control.dualstack." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result14(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3express-control." + registers.region + "." + registers.partitionResult.dnsSuffix()
                                                       + "/" + registers.uri_encoded_bucket))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result15(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_1
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result16(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_1
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result17(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_1
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result18(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_1 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result19(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_2
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result20(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_2
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result21(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_2
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result22(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_2 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result23(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_3
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result24(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_3
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result25(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_3
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result26(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_3 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result27(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_4
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result28(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_4
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result29(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_4
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result30(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_4 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result31(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_5
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result32(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_5
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result33(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_5
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result34(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_5 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result35(Registers registers) {
        return RuleResult.error("Unrecognized S3Express bucket name format.");
    }

    private static RuleResult result36(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_1
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result37(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_1
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result38(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_1
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result39(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_1 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result40(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_2
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result41(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_2
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result42(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_2
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result43(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_2 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result44(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_3
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result45(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_3
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result46(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_3
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result47(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_3 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result48(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_4
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result49(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_4
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result50(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_4
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result51(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_4 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result52(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_5
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result53(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_5
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result54(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_5
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result55(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_5 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result56(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_6
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result57(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_6
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result58(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_6
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result59(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_6 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result60(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_7
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result61(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_7
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result62(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_7
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result63(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_7 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result64(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_8
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result65(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_8
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result66(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_8
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result67(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_8 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result68(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_9
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result69(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_9
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result70(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_9
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result71(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_9 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result72(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result73(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result74(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result75(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result76(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_6
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result77(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_6
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result78(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_6
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result79(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_6 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result80(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_7
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result81(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_7
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result82(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_7
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result83(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_7 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result84(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_8
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result85(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_8
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result86(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_8
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result87(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_8 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result88(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_9
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result89(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_9
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result90(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_9
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result91(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_9 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result92(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result93(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result94(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result95(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result96(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result97(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3express-control-fips.dualstack." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result98(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3express-control-fips." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result99(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3express-control.dualstack." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result100(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3express-control." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result101(Registers registers) {
        return RuleResult.error("Expected a endpoint to be specified but no endpoint was found");
    }

    private static RuleResult result102(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".ec2." + registers.url.authority()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(
                                               SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                               .signingRegionSet(Arrays.asList("*")).build(),
                                               SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                              .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result103(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".ec2.s3-outposts." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(
                                               SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                               .signingRegionSet(Arrays.asList("*")).build(),
                                               SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                              .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result104(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".op-" + registers.outpostId_ssa_2 + "."
                                                       + registers.url.authority()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(
                                               SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                               .signingRegionSet(Arrays.asList("*")).build(),
                                               SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                              .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result105(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".op-" + registers.outpostId_ssa_2 + ".s3-outposts."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(
                                               SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                               .signingRegionSet(Arrays.asList("*")).build(),
                                               SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                              .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result106(Registers registers) {
        return RuleResult.error("Unrecognized hardware type: \"Expected hardware type o or e but got " + registers.hardwareType
                                + "\"");
    }

    private static RuleResult result107(Registers registers) {
        return RuleResult.error("Invalid ARN: The outpost Id must only contain a-z, A-Z, 0-9 and `-`.");
    }

    private static RuleResult result108(Registers registers) {
        return RuleResult.error("Custom endpoint `" + registers.endpoint + "` was not a valid URI");
    }

    private static RuleResult result109(Registers registers) {
        return RuleResult.error("S3 Accelerate cannot be used in this region");
    }

    private static RuleResult result110(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-fips.dualstack.us-east-1."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result111(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-fips.dualstack." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result112(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-fips.us-east-1." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result113(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-fips." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result114(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-accelerate.dualstack.us-east-1."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result115(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-accelerate.dualstack."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result116(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3.dualstack.us-east-1."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result117(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3.dualstack." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result118(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.normalizedPath()
                                                       + registers.bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result119(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.bucket + "." + registers.url.authority()
                                                       + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result120(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.normalizedPath()
                                                       + registers.bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result121(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.bucket + "." + registers.url.authority()
                                                       + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result122(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-accelerate." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result123(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-accelerate." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result124(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result125(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result126(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result127(Registers registers) {
        return RuleResult.error("Invalid region: region was not a valid DNS name.");
    }

    private static RuleResult result128(Registers registers) {
        return RuleResult.error("S3 Object Lambda does not support Dual-stack");
    }

    private static RuleResult result129(Registers registers) {
        return RuleResult.error("S3 Object Lambda does not support S3 Accelerate");
    }

    private static RuleResult result130(Registers registers) {
        return RuleResult.error("Access points are not supported for this operation");
    }

    private static RuleResult result131(Registers registers) {
        return RuleResult.error("Invalid configuration: region from ARN `" + registers.bucketArn.region()
                                + "` does not match client region `" + registers.region + "` and UseArnRegion is `false`");
    }

    private static RuleResult result132(Registers registers) {
        return RuleResult.error("Invalid ARN: Missing account id");
    }

    private static RuleResult result133(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.accessPointName_ssa_1 + "-"
                                                       + registers.bucketArn.accountId() + "." + registers.url.authority() + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult result134(Registers registers) {
        return RuleResult
            .endpoint(Endpoint
                          .builder()
                          .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                          + ".s3-object-lambda-fips." + registers.bucketArn.region() + "."
                                          + registers.bucketPartition.dnsSuffix()))
                          .putAttribute(
                              AwsEndpointAttribute.AUTH_SCHEMES,
                              Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                           .signingName("s3-object-lambda").signingRegion(registers.bucketArn.region()).build()))
                          .build());
    }

    private static RuleResult result135(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                       + ".s3-object-lambda." + registers.bucketArn.region() + "." + registers.bucketPartition.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult result136(Registers registers) {
        return RuleResult.error("Invalid ARN: The access point name may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                + registers.accessPointName_ssa_1 + "`");
    }

    private static RuleResult result137(Registers registers) {
        return RuleResult.error("Invalid ARN: The account id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                + registers.bucketArn.accountId() + "`");
    }

    private static RuleResult result138(Registers registers) {
        return RuleResult.error("Invalid region in ARN: `" + registers.bucketArn.region() + "` (invalid DNS name)");
    }

    private static RuleResult result139(Registers registers) {
        return RuleResult.error("Client was configured for partition `" + registers.partitionResult.name() + "` but ARN (`"
                                + registers.bucket + "`) has `" + registers.bucketPartition.name() + "`");
    }

    private static RuleResult result140(Registers registers) {
        return RuleResult.error("Invalid ARN: The ARN may only contain a single resource component after `accesspoint`.");
    }

    private static RuleResult result141(Registers registers) {
        return RuleResult.error("Invalid ARN: bucket ARN is missing a region");
    }

    private static RuleResult result142(Registers registers) {
        return RuleResult
            .error("Invalid ARN: Expected a resource of the format `accesspoint:<accesspoint name>` but no name was provided");
    }

    private static RuleResult result143(Registers registers) {
        return RuleResult.error("Invalid ARN: Object Lambda ARNs only support `accesspoint` arn types, but found: `"
                                + registers.arnType + "`");
    }

    private static RuleResult result144(Registers registers) {
        return RuleResult.error("Access Points do not support S3 Accelerate");
    }

    private static RuleResult result145(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                       + ".s3-accesspoint-fips.dualstack." + registers.bucketArn.region() + "."
                                                       + registers.bucketPartition.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult result146(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                       + ".s3-accesspoint-fips." + registers.bucketArn.region() + "." + registers.bucketPartition.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult result147(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                       + ".s3-accesspoint.dualstack." + registers.bucketArn.region() + "."
                                                       + registers.bucketPartition.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult result148(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.accessPointName_ssa_1 + "-"
                                                       + registers.bucketArn.accountId() + "." + registers.url.authority() + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult result149(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                       + ".s3-accesspoint." + registers.bucketArn.region() + "." + registers.bucketPartition.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult result150(Registers registers) {
        return RuleResult.error("Invalid ARN: The ARN was not for the S3 service, found: " + registers.bucketArn.service());
    }

    private static RuleResult result151(Registers registers) {
        return RuleResult.error("S3 MRAP does not support dual-stack");
    }

    private static RuleResult result152(Registers registers) {
        return RuleResult.error("S3 MRAP does not support FIPS");
    }

    private static RuleResult result153(Registers registers) {
        return RuleResult.error("S3 MRAP does not support S3 Accelerate");
    }

    private static RuleResult result154(Registers registers) {
        return RuleResult.error("Invalid configuration: Multi-Region Access Point ARNs are disabled.");
    }

    private static RuleResult result155(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_1 + ".accesspoint.s3-global."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                         .signingRegionSet(Arrays.asList("*")).build())).build());
    }

    private static RuleResult result156(Registers registers) {
        return RuleResult.error("Client was configured for partition `" + registers.partitionResult.name()
                                + "` but bucket referred to partition `" + registers.bucketArn.partition() + "`");
    }

    private static RuleResult result157(Registers registers) {
        return RuleResult.error("Invalid Access Point Name");
    }

    private static RuleResult result158(Registers registers) {
        return RuleResult.error("S3 Outposts does not support Dual-stack");
    }

    private static RuleResult result159(Registers registers) {
        return RuleResult.error("S3 Outposts does not support FIPS");
    }

    private static RuleResult result160(Registers registers) {
        return RuleResult.error("S3 Outposts does not support S3 Accelerate");
    }

    private static RuleResult result161(Registers registers) {
        return RuleResult.error("Invalid Arn: Outpost Access Point ARN contains sub resources");
    }

    private static RuleResult result162(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_2 + "-" + registers.bucketArn.accountId() + "."
                                                       + registers.outpostId_ssa_1 + "." + registers.url.authority()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(
                                               SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                               .signingRegionSet(Arrays.asList("*")).build(),
                                               SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                              .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult result163(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_2 + "-" + registers.bucketArn.accountId() + "."
                                                       + registers.outpostId_ssa_1 + ".s3-outposts." + registers.bucketArn.region() + "."
                                                       + registers.bucketPartition.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(
                                               SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                               .signingRegionSet(Arrays.asList("*")).build(),
                                               SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                              .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult result164(Registers registers) {
        return RuleResult.error("Expected an outpost type `accesspoint`, found " + registers.outpostType);
    }

    private static RuleResult result165(Registers registers) {
        return RuleResult.error("Invalid ARN: expected an access point name");
    }

    private static RuleResult result166(Registers registers) {
        return RuleResult.error("Invalid ARN: Expected a 4-component resource");
    }

    private static RuleResult result167(Registers registers) {
        return RuleResult.error("Invalid ARN: The outpost Id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                + registers.outpostId_ssa_1 + "`");
    }

    private static RuleResult result168(Registers registers) {
        return RuleResult.error("Invalid ARN: The Outpost Id was not set");
    }

    private static RuleResult result169(Registers registers) {
        return RuleResult.error("Invalid ARN: Unrecognized format: " + registers.bucket + " (type: " + registers.arnType + ")");
    }

    private static RuleResult result170(Registers registers) {
        return RuleResult.error("Invalid ARN: No ARN type specified");
    }

    private static RuleResult result171(Registers registers) {
        return RuleResult.error("Invalid ARN: `" + registers.bucket + "` was not a valid ARN");
    }

    private static RuleResult result172(Registers registers) {
        return RuleResult.error("Path-style addressing cannot be used with ARN buckets");
    }

    private static RuleResult result173(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips.dualstack.us-east-1." + registers.partitionResult.dnsSuffix() + "/"
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result174(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips.dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()
                                                       + "/" + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result175(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips.us-east-1." + registers.partitionResult.dnsSuffix() + "/"
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result176(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips." + registers.region + "." + registers.partitionResult.dnsSuffix() + "/"
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result177(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3.dualstack.us-east-1." + registers.partitionResult.dnsSuffix() + "/"
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result178(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3.dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix() + "/"
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result179(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.normalizedPath()
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result180(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.normalizedPath()
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result181(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result182(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result183(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3." + registers.region + "." + registers.partitionResult.dnsSuffix() + "/"
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result184(Registers registers) {
        return RuleResult.error("Path-style addressing cannot be used with S3 Accelerate");
    }

    private static RuleResult result185(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result186(Registers registers) {
        return RuleResult
            .endpoint(Endpoint
                          .builder()
                          .url(URI.create("https://s3-object-lambda-fips." + registers.region + "."
                                          + registers.partitionResult.dnsSuffix()))
                          .putAttribute(
                              AwsEndpointAttribute.AUTH_SCHEMES,
                              Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                           .signingName("s3-object-lambda").signingRegion(registers.region).build())).build());
    }

    private static RuleResult result187(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-object-lambda." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result188(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips.dualstack.us-east-1." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result189(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips.dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result190(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips.us-east-1." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result191(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result192(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3.dualstack.us-east-1." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result193(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3.dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result194(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result195(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result196(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result197(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result198(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result199(Registers registers) {
        return RuleResult.error("A region must be set when sending requests to S3.");
    }

    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(S3EndpointParams params) {
        Registers registers = new Registers();
        registers.region = params.region() == null ? null : params.region().id();
        registers.bucket = params.bucket();
        registers.useFIPS = params.useFips();
        registers.useDualStack = params.useDualStack();
        registers.endpoint = params.endpoint();
        registers.forcePathStyle = params.forcePathStyle();
        registers.accelerate = params.accelerate();
        registers.useGlobalEndpoint = params.useGlobalEndpoint();
        registers.useObjectLambdaEndpoint = params.useObjectLambdaEndpoint();
        registers.key = params.key();
        registers.prefix = params.prefix();
        registers.copySource = params.copySource();
        registers.disableAccessPoints = params.disableAccessPoints();
        registers.disableMultiRegionAccessPoints = params.disableMultiRegionAccessPoints();
        registers.useArnRegion = params.useArnRegion();
        registers.useS3ExpressControlEndpoint = params.useS3ExpressControlEndpoint();
        registers.disableS3ExpressSessionAuth = params.disableS3ExpressSessionAuth();

        RuleResult result = n0(registers);
        if (result.isError()) {
            String errorMsg = result.error();
            if (errorMsg.contains("Invalid ARN") && errorMsg.contains(":s3:::")) {
                errorMsg += ". Use the bucket name instead of simple bucket ARNs in GetBucketLocationRequest.";
            }
            return CompletableFutureUtils.failedFuture(SdkClientException.create(errorMsg));
        }
        return CompletableFuture.completedFuture(result.endpoint());
    }

    private static RuleResult n0(Registers registers) {
        return cond0(registers) ? n1(registers) : result199(registers);
    }
    private static RuleResult n1(Registers registers) {
        return cond4(registers) ? n826(registers) : n2(registers);
    }
    private static RuleResult n826(Registers registers) {
        return cond5(registers) ? result0(registers) : n827(registers);
    }
    private static RuleResult n827(Registers registers) {
        return cond1(registers) ? n1017(registers) : n828(registers);
    }
    private static RuleResult n1017(Registers registers) {
        return cond27(registers) ? result1(registers) : result3(registers);
    }
    private static RuleResult n828(Registers registers) {
        return cond2(registers) ? n841(registers) : n829(registers);
    }
    private static RuleResult n841(Registers registers) {
        return cond19(registers) ? n868(registers) : n842(registers);
    }
    private static RuleResult n868(Registers registers) {
        return cond6(registers) ? n888(registers) : n869(registers);
    }
    private static RuleResult n888(Registers registers) {
        return cond20(registers) ? n900(registers) : n889(registers);
    }
    private static RuleResult n900(Registers registers) {
        return cond79(registers) ? result5(registers) : n901(registers);
    }
    private static RuleResult n901(Registers registers) {
        return cond80(registers) ? result5(registers) : n902(registers);
    }
    private static RuleResult n902(Registers registers) {
        return cond10(registers) ? n994(registers) : n903(registers);
    }
    private static RuleResult n994(Registers registers) {
        return cond11(registers) ? n1005(registers) : n995(registers);
    }
    private static RuleResult n1005(Registers registers) {
        return cond21(registers) ? n1006(registers) : result172(registers);
    }
    private static RuleResult n1006(Registers registers) {
        return cond22(registers) ? n1007(registers) : result172(registers);
    }
    private static RuleResult n1007(Registers registers) {
        return cond23(registers) ? n1008(registers) : result172(registers);
    }
    private static RuleResult n1008(Registers registers) {
        return cond16(registers) ? n1009(registers) : result172(registers);
    }
    private static RuleResult n1009(Registers registers) {
        return cond24(registers) ? n1010(registers) : result172(registers);
    }
    private static RuleResult n1010(Registers registers) {
        // this is a subgraph: [1014, 1015, 1016, 1011, 1012, 1013]
        if (cond18(registers)) {
            if (cond35(registers)) {
                if (cond28(registers)) {
                    return result101(registers);
                } else {
                    if (cond29(registers)) {
                        return result101(registers);
                    } else {
                        return result106(registers);
                    }
                }
            } else {
                return result107(registers);
            }
        } else {
            if (cond35(registers)) {
                if (cond28(registers)) {
                    return result103(registers);
                } else {
                    if (cond29(registers)) {
                        return result105(registers);
                    } else {
                        return result106(registers);
                    }
                }
            } else {
                return result107(registers);
            }
        }
    }
    private static RuleResult n995(Registers registers) {
        return cond21(registers) ? n996(registers) : n1000(registers);
    }
    private static RuleResult n996(Registers registers) {
        return cond22(registers) ? n997(registers) : n1000(registers);
    }
    private static RuleResult n997(Registers registers) {
        return cond23(registers) ? n998(registers) : n1000(registers);
    }
    private static RuleResult n998(Registers registers) {
        return cond16(registers) ? n999(registers) : n1000(registers);
    }
    private static RuleResult n999(Registers registers) {
        return cond24(registers) ? n1010(registers) : n1000(registers);
    }
    private static RuleResult n1000(Registers registers) {
        return cond37(registers) ? result184(registers) : n1001(registers);
    }
    private static RuleResult n1001(Registers registers) {
        return cond15(registers) ? n1002(registers) : result199(registers);
    }
    private static RuleResult n1002(Registers registers) {
        return cond17(registers) ? n1003(registers) : result199(registers);
    }
    private static RuleResult n1003(Registers registers) {
        return cond33(registers) ? n1004(registers) : result127(registers);
    }
    private static RuleResult n1004(Registers registers) {
        return cond27(registers) ? result128(registers) : result129(registers);
    }
    private static RuleResult n903(Registers registers) {
        return cond12(registers) ? n988(registers) : n904(registers);
    }
    private static RuleResult n988(Registers registers) {
        return cond21(registers) ? n989(registers) : n993(registers);
    }
    private static RuleResult n989(Registers registers) {
        return cond22(registers) ? n990(registers) : n993(registers);
    }
    private static RuleResult n990(Registers registers) {
        return cond23(registers) ? n991(registers) : n993(registers);
    }
    private static RuleResult n991(Registers registers) {
        return cond16(registers) ? n992(registers) : n993(registers);
    }
    private static RuleResult n992(Registers registers) {
        return cond24(registers) ? n1010(registers) : n993(registers);
    }
    private static RuleResult n993(Registers registers) {
        return cond34(registers) ? result109(registers) : result127(registers);
    }
    private static RuleResult n904(Registers registers) {
        return cond36(registers) ? n905(registers) : n995(registers);
    }
    private static RuleResult n905(Registers registers) {
        return cond21(registers) ? n906(registers) : n910(registers);
    }
    private static RuleResult n906(Registers registers) {
        return cond22(registers) ? n907(registers) : n910(registers);
    }
    private static RuleResult n907(Registers registers) {
        return cond23(registers) ? n908(registers) : n910(registers);
    }
    private static RuleResult n908(Registers registers) {
        return cond16(registers) ? n909(registers) : n910(registers);
    }
    private static RuleResult n909(Registers registers) {
        return cond24(registers) ? n1010(registers) : n910(registers);
    }
    private static RuleResult n910(Registers registers) {
        return cond27(registers) ? n940(registers) : n911(registers);
    }
    private static RuleResult n940(Registers registers) {
        return cond26(registers) ? n941(registers) : n942(registers);
    }
    private static RuleResult n941(Registers registers) {
        return cond40(registers) ? n972(registers) : n942(registers);
    }
    private static RuleResult n972(Registers registers) {
        return cond41(registers) ? n973(registers) : result170(registers);
    }
    private static RuleResult n973(Registers registers) {
        return cond50(registers) ? n977(registers) : n974(registers);
    }
    private static RuleResult n977(Registers registers) {
        return cond51(registers) ? result170(registers) : n978(registers);
    }
    private static RuleResult n978(Registers registers) {
        return cond57(registers) ? n982(registers) : n979(registers);
    }
    private static RuleResult n982(Registers registers) {
        return cond58(registers) ? n986(registers) : n983(registers);
    }
    private static RuleResult n986(Registers registers) {
        return cond49(registers) ? n987(registers) : result142(registers);
    }
    private static RuleResult n987(Registers registers) {
        return cond52(registers) ? result142(registers) : result128(registers);
    }
    private static RuleResult n983(Registers registers) {
        return cond49(registers) ? n984(registers) : result142(registers);
    }
    private static RuleResult n984(Registers registers) {
        return cond52(registers) ? result142(registers) : n985(registers);
    }
    private static RuleResult n985(Registers registers) {
        return cond74(registers) ? result151(registers) : result157(registers);
    }
    private static RuleResult n979(Registers registers) {
        return cond58(registers) ? n986(registers) : n980(registers);
    }
    private static RuleResult n980(Registers registers) {
        return cond49(registers) ? n981(registers) : result142(registers);
    }
    private static RuleResult n981(Registers registers) {
        return cond52(registers) ? result142(registers) : result130(registers);
    }
    private static RuleResult n974(Registers registers) {
        return cond51(registers) ? result170(registers) : n975(registers);
    }
    private static RuleResult n975(Registers registers) {
        return cond58(registers) ? result143(registers) : n976(registers);
    }
    private static RuleResult n976(Registers registers) {
        return cond59(registers) ? result158(registers) : result169(registers);
    }
    private static RuleResult n942(Registers registers) {
        return cond56(registers) ? n965(registers) : n943(registers);
    }
    private static RuleResult n965(Registers registers) {
        return cond41(registers) ? n966(registers) : result170(registers);
    }
    private static RuleResult n966(Registers registers) {
        return cond50(registers) ? n967(registers) : n974(registers);
    }
    private static RuleResult n967(Registers registers) {
        return cond51(registers) ? result170(registers) : n968(registers);
    }
    private static RuleResult n968(Registers registers) {
        return cond57(registers) ? n982(registers) : n969(registers);
    }
    private static RuleResult n969(Registers registers) {
        return cond58(registers) ? n986(registers) : n970(registers);
    }
    private static RuleResult n970(Registers registers) {
        return cond49(registers) ? n971(registers) : result142(registers);
    }
    private static RuleResult n971(Registers registers) {
        return cond52(registers) ? result142(registers) : result140(registers);
    }
    private static RuleResult n943(Registers registers) {
        return cond41(registers) ? n944(registers) : result170(registers);
    }
    private static RuleResult n944(Registers registers) {
        return cond50(registers) ? n945(registers) : n974(registers);
    }
    private static RuleResult n945(Registers registers) {
        return cond51(registers) ? result170(registers) : n946(registers);
    }
    private static RuleResult n946(Registers registers) {
        return cond57(registers) ? n982(registers) : n947(registers);
    }
    private static RuleResult n947(Registers registers) {
        return cond78(registers) ? n954(registers) : n948(registers);
    }
    private static RuleResult n954(Registers registers) {
        return cond58(registers) ? n986(registers) : n955(registers);
    }
    private static RuleResult n955(Registers registers) {
        // this is a subgraph: [956, 957, 958, 959, 960, 961, 962, 963, 964]
        if (cond49(registers)) {
            if (cond52(registers)) {
                return result142(registers);
            } else {
                if (cond39(registers)) {
                    if (cond42(registers)) {
                        if (cond61(registers)) {
                            if (cond62(registers)) {
                                if (cond75(registers)) {
                                    if (cond72(registers)) {
                                        if (cond76(registers)) {
                                            if (cond73(registers)) {
                                                return result144(registers);
                                            } else {
                                                return result136(registers);
                                            }
                                        } else {
                                            return result137(registers);
                                        }
                                    } else {
                                        return result150(registers);
                                    }
                                } else {
                                    return result138(registers);
                                }
                            } else {
                                return result139(registers);
                            }
                        } else {
                            return result131(registers);
                        }
                    } else {
                        if (cond62(registers)) {
                            if (cond75(registers)) {
                                if (cond72(registers)) {
                                    if (cond76(registers)) {
                                        if (cond73(registers)) {
                                            return result144(registers);
                                        } else {
                                            return result136(registers);
                                        }
                                    } else {
                                        return result137(registers);
                                    }
                                } else {
                                    return result150(registers);
                                }
                            } else {
                                return result138(registers);
                            }
                        } else {
                            return result139(registers);
                        }
                    }
                } else {
                    if (cond62(registers)) {
                        if (cond75(registers)) {
                            if (cond72(registers)) {
                                if (cond76(registers)) {
                                    if (cond73(registers)) {
                                        return result144(registers);
                                    } else {
                                        return result136(registers);
                                    }
                                } else {
                                    return result137(registers);
                                }
                            } else {
                                return result150(registers);
                            }
                        } else {
                            return result138(registers);
                        }
                    } else {
                        return result139(registers);
                    }
                }
            }
        } else {
            return result142(registers);
        }
    }
    private static RuleResult n948(Registers registers) {
        return cond58(registers) ? n986(registers) : n949(registers);
    }
    private static RuleResult n949(Registers registers) {
        return cond49(registers) ? n950(registers) : result142(registers);
    }
    private static RuleResult n950(Registers registers) {
        return cond52(registers) ? result142(registers) : n951(registers);
    }
    private static RuleResult n951(Registers registers) {
        return cond39(registers) ? n952(registers) : result140(registers);
    }
    private static RuleResult n952(Registers registers) {
        return cond42(registers) ? n953(registers) : result140(registers);
    }
    private static RuleResult n953(Registers registers) {
        return cond61(registers) ? result140(registers) : result131(registers);
    }
    private static RuleResult n911(Registers registers) {
        return cond26(registers) ? n912(registers) : n913(registers);
    }
    private static RuleResult n912(Registers registers) {
        return cond40(registers) ? n926(registers) : n913(registers);
    }
    private static RuleResult n926(Registers registers) {
        return cond41(registers) ? n927(registers) : result170(registers);
    }
    private static RuleResult n927(Registers registers) {
        return cond50(registers) ? n931(registers) : n928(registers);
    }
    private static RuleResult n931(Registers registers) {
        return cond51(registers) ? result170(registers) : n932(registers);
    }
    private static RuleResult n932(Registers registers) {
        return cond57(registers) ? n934(registers) : n933(registers);
    }
    private static RuleResult n934(Registers registers) {
        return cond58(registers) ? n938(registers) : n935(registers);
    }
    private static RuleResult n938(Registers registers) {
        return cond49(registers) ? n939(registers) : result142(registers);
    }
    private static RuleResult n939(Registers registers) {
        return cond52(registers) ? result142(registers) : result129(registers);
    }
    private static RuleResult n935(Registers registers) {
        return cond49(registers) ? n936(registers) : result142(registers);
    }
    private static RuleResult n936(Registers registers) {
        return cond52(registers) ? result142(registers) : n937(registers);
    }
    private static RuleResult n937(Registers registers) {
        return cond74(registers) ? result153(registers) : result157(registers);
    }
    private static RuleResult n933(Registers registers) {
        return cond58(registers) ? n938(registers) : n980(registers);
    }
    private static RuleResult n928(Registers registers) {
        return cond51(registers) ? result170(registers) : n929(registers);
    }
    private static RuleResult n929(Registers registers) {
        return cond58(registers) ? result143(registers) : n930(registers);
    }
    private static RuleResult n930(Registers registers) {
        return cond59(registers) ? result160(registers) : result169(registers);
    }
    private static RuleResult n913(Registers registers) {
        return cond56(registers) ? n921(registers) : n914(registers);
    }
    private static RuleResult n921(Registers registers) {
        return cond41(registers) ? n922(registers) : result170(registers);
    }
    private static RuleResult n922(Registers registers) {
        return cond50(registers) ? n923(registers) : n928(registers);
    }
    private static RuleResult n923(Registers registers) {
        return cond51(registers) ? result170(registers) : n924(registers);
    }
    private static RuleResult n924(Registers registers) {
        return cond57(registers) ? n934(registers) : n925(registers);
    }
    private static RuleResult n925(Registers registers) {
        return cond58(registers) ? n938(registers) : n970(registers);
    }
    private static RuleResult n914(Registers registers) {
        return cond41(registers) ? n915(registers) : result170(registers);
    }
    private static RuleResult n915(Registers registers) {
        return cond50(registers) ? n916(registers) : n928(registers);
    }
    private static RuleResult n916(Registers registers) {
        return cond51(registers) ? result170(registers) : n917(registers);
    }
    private static RuleResult n917(Registers registers) {
        return cond57(registers) ? n934(registers) : n918(registers);
    }
    private static RuleResult n918(Registers registers) {
        return cond78(registers) ? n920(registers) : n919(registers);
    }
    private static RuleResult n920(Registers registers) {
        return cond58(registers) ? n938(registers) : n955(registers);
    }
    private static RuleResult n919(Registers registers) {
        return cond58(registers) ? n938(registers) : n949(registers);
    }
    private static RuleResult n889(Registers registers) {
        return cond79(registers) ? result5(registers) : n890(registers);
    }
    private static RuleResult n890(Registers registers) {
        return cond80(registers) ? result5(registers) : n891(registers);
    }
    private static RuleResult n891(Registers registers) {
        return cond81(registers) ? n892(registers) : n902(registers);
    }
    private static RuleResult n892(Registers registers) {
        return cond10(registers) ? n895(registers) : n893(registers);
    }
    private static RuleResult n895(Registers registers) {
        return cond21(registers) ? n896(registers) : result171(registers);
    }
    private static RuleResult n896(Registers registers) {
        return cond22(registers) ? n897(registers) : result171(registers);
    }
    private static RuleResult n897(Registers registers) {
        return cond23(registers) ? n898(registers) : result171(registers);
    }
    private static RuleResult n898(Registers registers) {
        return cond16(registers) ? n899(registers) : result171(registers);
    }
    private static RuleResult n899(Registers registers) {
        return cond24(registers) ? n1010(registers) : result171(registers);
    }
    private static RuleResult n893(Registers registers) {
        return cond12(registers) ? n988(registers) : n894(registers);
    }
    private static RuleResult n894(Registers registers) {
        return cond36(registers) ? n905(registers) : n895(registers);
    }
    private static RuleResult n869(Registers registers) {
        return cond20(registers) ? n875(registers) : n870(registers);
    }
    private static RuleResult n875(Registers registers) {
        return cond79(registers) ? result5(registers) : n876(registers);
    }
    private static RuleResult n876(Registers registers) {
        return cond80(registers) ? result5(registers) : n877(registers);
    }
    private static RuleResult n877(Registers registers) {
        return cond10(registers) ? n994(registers) : n878(registers);
    }
    private static RuleResult n878(Registers registers) {
        return cond12(registers) ? n879(registers) : n904(registers);
    }
    private static RuleResult n879(Registers registers) {
        return cond21(registers) ? n880(registers) : n884(registers);
    }
    private static RuleResult n880(Registers registers) {
        return cond22(registers) ? n881(registers) : n884(registers);
    }
    private static RuleResult n881(Registers registers) {
        return cond23(registers) ? n882(registers) : n884(registers);
    }
    private static RuleResult n882(Registers registers) {
        return cond16(registers) ? n883(registers) : n884(registers);
    }
    private static RuleResult n883(Registers registers) {
        return cond24(registers) ? n1010(registers) : n884(registers);
    }
    private static RuleResult n884(Registers registers) {
        return cond34(registers) ? n885(registers) : result127(registers);
    }
    private static RuleResult n885(Registers registers) {
        return cond27(registers) ? n887(registers) : n886(registers);
    }
    private static RuleResult n887(Registers registers) {
        return cond68(registers) ? result114(registers) : result115(registers);
    }
    private static RuleResult n886(Registers registers) {
        return cond68(registers) ? result122(registers) : result123(registers);
    }
    private static RuleResult n870(Registers registers) {
        return cond79(registers) ? result5(registers) : n871(registers);
    }
    private static RuleResult n871(Registers registers) {
        return cond80(registers) ? result5(registers) : n872(registers);
    }
    private static RuleResult n872(Registers registers) {
        return cond81(registers) ? n873(registers) : n877(registers);
    }
    private static RuleResult n873(Registers registers) {
        return cond10(registers) ? n895(registers) : n874(registers);
    }
    private static RuleResult n874(Registers registers) {
        return cond12(registers) ? n879(registers) : n894(registers);
    }
    private static RuleResult n842(Registers registers) {
        return cond20(registers) ? n848(registers) : n843(registers);
    }
    private static RuleResult n848(Registers registers) {
        return cond79(registers) ? result5(registers) : n849(registers);
    }
    private static RuleResult n849(Registers registers) {
        return cond80(registers) ? result5(registers) : n850(registers);
    }
    private static RuleResult n850(Registers registers) {
        return cond10(registers) ? n867(registers) : n851(registers);
    }
    private static RuleResult n867(Registers registers) {
        return cond11(registers) ? result172(registers) : result199(registers);
    }
    private static RuleResult n851(Registers registers) {
        return cond36(registers) ? n852(registers) : result199(registers);
    }
    private static RuleResult n852(Registers registers) {
        return cond27(registers) ? n860(registers) : n853(registers);
    }
    private static RuleResult n860(Registers registers) {
        return cond26(registers) ? n861(registers) : n862(registers);
    }
    private static RuleResult n861(Registers registers) {
        return cond40(registers) ? n972(registers) : n862(registers);
    }
    private static RuleResult n862(Registers registers) {
        return cond56(registers) ? n965(registers) : n863(registers);
    }
    private static RuleResult n863(Registers registers) {
        return cond41(registers) ? n864(registers) : result170(registers);
    }
    private static RuleResult n864(Registers registers) {
        return cond50(registers) ? n865(registers) : n974(registers);
    }
    private static RuleResult n865(Registers registers) {
        return cond51(registers) ? result170(registers) : n866(registers);
    }
    private static RuleResult n866(Registers registers) {
        return cond57(registers) ? n982(registers) : n948(registers);
    }
    private static RuleResult n853(Registers registers) {
        return cond26(registers) ? n854(registers) : n855(registers);
    }
    private static RuleResult n854(Registers registers) {
        return cond40(registers) ? n926(registers) : n855(registers);
    }
    private static RuleResult n855(Registers registers) {
        return cond56(registers) ? n921(registers) : n856(registers);
    }
    private static RuleResult n856(Registers registers) {
        return cond41(registers) ? n857(registers) : result170(registers);
    }
    private static RuleResult n857(Registers registers) {
        return cond50(registers) ? n858(registers) : n928(registers);
    }
    private static RuleResult n858(Registers registers) {
        return cond51(registers) ? result170(registers) : n859(registers);
    }
    private static RuleResult n859(Registers registers) {
        return cond57(registers) ? n934(registers) : n919(registers);
    }
    private static RuleResult n843(Registers registers) {
        return cond79(registers) ? result5(registers) : n844(registers);
    }
    private static RuleResult n844(Registers registers) {
        return cond80(registers) ? result5(registers) : n845(registers);
    }
    private static RuleResult n845(Registers registers) {
        return cond81(registers) ? n846(registers) : n850(registers);
    }
    private static RuleResult n846(Registers registers) {
        return cond10(registers) ? result171(registers) : n847(registers);
    }
    private static RuleResult n847(Registers registers) {
        return cond36(registers) ? n852(registers) : result171(registers);
    }
    private static RuleResult n829(Registers registers) {
        // this is a subgraph: [830, 831, 840, 832, 833, 1003, 1004, 834, 835, 839, 836, 837, 838]
        if (cond19(registers)) {
            if (cond3(registers)) {
                if (cond8(registers)) {
                    if (cond27(registers)) {
                        return result99(registers);
                    } else {
                        return result100(registers);
                    }
                } else {
                    if (cond15(registers)) {
                        if (cond17(registers)) {
                            if (cond33(registers)) {
                                if (cond27(registers)) {
                                    return result128(registers);
                                } else {
                                    return result129(registers);
                                }
                            } else {
                                return result127(registers);
                            }
                        } else {
                            if (cond33(registers)) {
                                if (cond27(registers)) {
                                    if (cond68(registers)) {
                                        return result192(registers);
                                    } else {
                                        return result193(registers);
                                    }
                                } else {
                                    if (cond68(registers)) {
                                        return result196(registers);
                                    } else {
                                        if (cond69(registers)) {
                                            if (cond64(registers)) {
                                                return result197(registers);
                                            } else {
                                                return result198(registers);
                                            }
                                        } else {
                                            return result198(registers);
                                        }
                                    }
                                }
                            } else {
                                return result127(registers);
                            }
                        }
                    } else {
                        if (cond33(registers)) {
                            if (cond27(registers)) {
                                if (cond68(registers)) {
                                    return result192(registers);
                                } else {
                                    return result193(registers);
                                }
                            } else {
                                if (cond68(registers)) {
                                    return result196(registers);
                                } else {
                                    if (cond69(registers)) {
                                        if (cond64(registers)) {
                                            return result197(registers);
                                        } else {
                                            return result198(registers);
                                        }
                                    } else {
                                        return result198(registers);
                                    }
                                }
                            }
                        } else {
                            return result127(registers);
                        }
                    }
                }
            } else {
                if (cond15(registers)) {
                    if (cond17(registers)) {
                        if (cond33(registers)) {
                            if (cond27(registers)) {
                                return result128(registers);
                            } else {
                                return result129(registers);
                            }
                        } else {
                            return result127(registers);
                        }
                    } else {
                        if (cond33(registers)) {
                            if (cond27(registers)) {
                                if (cond68(registers)) {
                                    return result192(registers);
                                } else {
                                    return result193(registers);
                                }
                            } else {
                                if (cond68(registers)) {
                                    return result196(registers);
                                } else {
                                    if (cond69(registers)) {
                                        if (cond64(registers)) {
                                            return result197(registers);
                                        } else {
                                            return result198(registers);
                                        }
                                    } else {
                                        return result198(registers);
                                    }
                                }
                            }
                        } else {
                            return result127(registers);
                        }
                    }
                } else {
                    if (cond33(registers)) {
                        if (cond27(registers)) {
                            if (cond68(registers)) {
                                return result192(registers);
                            } else {
                                return result193(registers);
                            }
                        } else {
                            if (cond68(registers)) {
                                return result196(registers);
                            } else {
                                if (cond69(registers)) {
                                    if (cond64(registers)) {
                                        return result197(registers);
                                    } else {
                                        return result198(registers);
                                    }
                                } else {
                                    return result198(registers);
                                }
                            }
                        }
                    } else {
                        return result127(registers);
                    }
                }
            }
        } else {
            return result199(registers);
        }
    }
    private static RuleResult n2(Registers registers) {
        return cond5(registers) ? n567(registers) : n3(registers);
    }
    private static RuleResult n567(Registers registers) {
        return cond1(registers) ? n825(registers) : n568(registers);
    }
    private static RuleResult n825(Registers registers) {
        return cond27(registers) ? result1(registers) : result2(registers);
    }
    private static RuleResult n568(Registers registers) {
        return cond2(registers) ? n580(registers) : n569(registers);
    }
    private static RuleResult n580(Registers registers) {
        return cond19(registers) ? n599(registers) : n581(registers);
    }
    private static RuleResult n599(Registers registers) {
        return cond6(registers) ? result4(registers) : n600(registers);
    }
    private static RuleResult n600(Registers registers) {
        return cond3(registers) ? n601(registers) : n602(registers);
    }
    private static RuleResult n601(Registers registers) {
        return cond8(registers) ? n612(registers) : n602(registers);
    }
    private static RuleResult n612(Registers registers) {
        return cond7(registers) ? n613(registers) : n614(registers);
    }
    private static RuleResult n613(Registers registers) {
        return cond9(registers) ? n668(registers) : n614(registers);
    }
    private static RuleResult n668(Registers registers) {
        return cond20(registers) ? n675(registers) : n669(registers);
    }
    private static RuleResult n675(Registers registers) {
        return cond79(registers) ? n796(registers) : n676(registers);
    }
    private static RuleResult n796(Registers registers) {
        return cond12(registers) ? n798(registers) : n797(registers);
    }
    private static RuleResult n798(Registers registers) {
        return cond37(registers) ? n824(registers) : n799(registers);
    }
    private static RuleResult n824(Registers registers) {
        return cond27(registers) ? result11(registers) : result12(registers);
    }
    private static RuleResult n799(Registers registers) {
        return cond27(registers) ? n812(registers) : n800(registers);
    }
    private static RuleResult n812(Registers registers) {
        // this is a subgraph: [813, 822, 823, 814, 815, 816, 817, 818, 819, 820, 821]
        if (cond82(registers)) {
            if (cond31(registers)) {
                if (cond84(registers)) {
                    if (cond44(registers)) {
                        return result15(registers);
                    } else {
                        return result19(registers);
                    }
                } else {
                    return result19(registers);
                }
            } else {
                if (cond84(registers)) {
                    if (cond44(registers)) {
                        return result15(registers);
                    } else {
                        if (cond85(registers)) {
                            if (cond46(registers)) {
                                return result23(registers);
                            } else {
                                if (cond47(registers)) {
                                    if (cond86(registers)) {
                                        return result27(registers);
                                    } else {
                                        if (cond54(registers)) {
                                            if (cond88(registers)) {
                                                return result31(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result31(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result27(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result31(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result31(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                } else {
                    if (cond85(registers)) {
                        if (cond46(registers)) {
                            return result23(registers);
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result27(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result31(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result31(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result27(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result31(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result31(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            }
        } else {
            if (cond84(registers)) {
                if (cond44(registers)) {
                    return result15(registers);
                } else {
                    if (cond85(registers)) {
                        if (cond46(registers)) {
                            return result23(registers);
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result27(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result31(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result31(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result27(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result31(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result31(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            } else {
                if (cond85(registers)) {
                    if (cond46(registers)) {
                        return result23(registers);
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result27(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result31(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result31(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                } else {
                    if (cond47(registers)) {
                        if (cond86(registers)) {
                            return result27(registers);
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result31(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    } else {
                        if (cond54(registers)) {
                            if (cond88(registers)) {
                                return result31(registers);
                            } else {
                                return result35(registers);
                            }
                        } else {
                            return result35(registers);
                        }
                    }
                }
            }
        }
    }
    private static RuleResult n800(Registers registers) {
        // this is a subgraph: [801, 810, 811, 802, 803, 804, 805, 806, 807, 808, 809]
        if (cond82(registers)) {
            if (cond31(registers)) {
                if (cond84(registers)) {
                    if (cond44(registers)) {
                        return result16(registers);
                    } else {
                        return result20(registers);
                    }
                } else {
                    return result20(registers);
                }
            } else {
                if (cond84(registers)) {
                    if (cond44(registers)) {
                        return result16(registers);
                    } else {
                        if (cond85(registers)) {
                            if (cond46(registers)) {
                                return result24(registers);
                            } else {
                                if (cond47(registers)) {
                                    if (cond86(registers)) {
                                        return result28(registers);
                                    } else {
                                        if (cond54(registers)) {
                                            if (cond88(registers)) {
                                                return result32(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result32(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result28(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result32(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result32(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                } else {
                    if (cond85(registers)) {
                        if (cond46(registers)) {
                            return result24(registers);
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result28(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result32(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result32(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result28(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result32(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result32(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            }
        } else {
            if (cond84(registers)) {
                if (cond44(registers)) {
                    return result16(registers);
                } else {
                    if (cond85(registers)) {
                        if (cond46(registers)) {
                            return result24(registers);
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result28(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result32(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result32(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result28(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result32(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result32(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            } else {
                if (cond85(registers)) {
                    if (cond46(registers)) {
                        return result24(registers);
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result28(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result32(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result32(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                } else {
                    if (cond47(registers)) {
                        if (cond86(registers)) {
                            return result28(registers);
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result32(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    } else {
                        if (cond54(registers)) {
                            if (cond88(registers)) {
                                return result32(registers);
                            } else {
                                return result35(registers);
                            }
                        } else {
                            return result35(registers);
                        }
                    }
                }
            }
        }
    }
    private static RuleResult n797(Registers registers) {
        return cond37(registers) ? n824(registers) : result8(registers);
    }
    private static RuleResult n676(Registers registers) {
        return cond80(registers) ? n774(registers) : n677(registers);
    }
    private static RuleResult n774(Registers registers) {
        return cond12(registers) ? n775(registers) : result8(registers);
    }
    private static RuleResult n775(Registers registers) {
        return cond27(registers) ? n786(registers) : n776(registers);
    }
    private static RuleResult n786(Registers registers) {
        // this is a subgraph: [787, 788, 789, 790, 791, 792, 793, 794, 795]
        if (cond30(registers)) {
            if (cond82(registers)) {
                return result56(registers);
            } else {
                if (cond83(registers)) {
                    if (cond32(registers)) {
                        return result60(registers);
                    } else {
                        if (cond45(registers)) {
                            if (cond86(registers)) {
                                return result64(registers);
                            } else {
                                if (cond87(registers)) {
                                    if (cond53(registers)) {
                                        return result68(registers);
                                    } else {
                                        if (cond55(registers)) {
                                            if (cond89(registers)) {
                                                return result72(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result72(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result68(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result72(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result72(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                } else {
                    if (cond45(registers)) {
                        if (cond86(registers)) {
                            return result64(registers);
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result68(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result72(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result72(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result68(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result72(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result72(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            }
        } else {
            if (cond83(registers)) {
                if (cond32(registers)) {
                    return result60(registers);
                } else {
                    if (cond45(registers)) {
                        if (cond86(registers)) {
                            return result64(registers);
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result68(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result72(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result72(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result68(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result72(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result72(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            } else {
                if (cond45(registers)) {
                    if (cond86(registers)) {
                        return result64(registers);
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result68(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result72(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result72(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                } else {
                    if (cond87(registers)) {
                        if (cond53(registers)) {
                            return result68(registers);
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result72(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    } else {
                        if (cond55(registers)) {
                            if (cond89(registers)) {
                                return result72(registers);
                            } else {
                                return result35(registers);
                            }
                        } else {
                            return result35(registers);
                        }
                    }
                }
            }
        }
    }
    private static RuleResult n776(Registers registers) {
        // this is a subgraph: [777, 778, 779, 780, 781, 782, 783, 784, 785]
        if (cond30(registers)) {
            if (cond82(registers)) {
                return result57(registers);
            } else {
                if (cond83(registers)) {
                    if (cond32(registers)) {
                        return result61(registers);
                    } else {
                        if (cond45(registers)) {
                            if (cond86(registers)) {
                                return result65(registers);
                            } else {
                                if (cond87(registers)) {
                                    if (cond53(registers)) {
                                        return result69(registers);
                                    } else {
                                        if (cond55(registers)) {
                                            if (cond89(registers)) {
                                                return result73(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result73(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result69(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result73(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result73(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                } else {
                    if (cond45(registers)) {
                        if (cond86(registers)) {
                            return result65(registers);
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result69(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result73(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result73(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result69(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result73(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result73(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            }
        } else {
            if (cond83(registers)) {
                if (cond32(registers)) {
                    return result61(registers);
                } else {
                    if (cond45(registers)) {
                        if (cond86(registers)) {
                            return result65(registers);
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result69(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result73(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result73(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result69(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result73(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result73(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            } else {
                if (cond45(registers)) {
                    if (cond86(registers)) {
                        return result65(registers);
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result69(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result73(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result73(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                } else {
                    if (cond87(registers)) {
                        if (cond53(registers)) {
                            return result69(registers);
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result73(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    } else {
                        if (cond55(registers)) {
                            if (cond89(registers)) {
                                return result73(registers);
                            } else {
                                return result35(registers);
                            }
                        } else {
                            return result35(registers);
                        }
                    }
                }
            }
        }
    }
    private static RuleResult n677(Registers registers) {
        return cond10(registers) ? n760(registers) : n678(registers);
    }
    private static RuleResult n760(Registers registers) {
        return cond11(registers) ? n1005(registers) : n761(registers);
    }
    private static RuleResult n761(Registers registers) {
        return cond21(registers) ? n762(registers) : n766(registers);
    }
    private static RuleResult n762(Registers registers) {
        return cond22(registers) ? n763(registers) : n766(registers);
    }
    private static RuleResult n763(Registers registers) {
        return cond23(registers) ? n764(registers) : n766(registers);
    }
    private static RuleResult n764(Registers registers) {
        return cond16(registers) ? n765(registers) : n766(registers);
    }
    private static RuleResult n765(Registers registers) {
        return cond24(registers) ? n1010(registers) : n766(registers);
    }
    private static RuleResult n766(Registers registers) {
        // this is a subgraph: [771, 773, 772, 767, 768, 769, 770]
        if (cond37(registers)) {
            if (cond27(registers)) {
                if (cond68(registers)) {
                    return result173(registers);
                } else {
                    return result174(registers);
                }
            } else {
                if (cond68(registers)) {
                    return result175(registers);
                } else {
                    return result176(registers);
                }
            }
        } else {
            if (cond15(registers)) {
                if (cond17(registers)) {
                    if (cond33(registers)) {
                        if (cond27(registers)) {
                            return result128(registers);
                        } else {
                            return result186(registers);
                        }
                    } else {
                        return result127(registers);
                    }
                } else {
                    return result199(registers);
                }
            } else {
                return result199(registers);
            }
        }
    }
    private static RuleResult n678(Registers registers) {
        return cond12(registers) ? n751(registers) : n679(registers);
    }
    private static RuleResult n751(Registers registers) {
        return cond21(registers) ? n752(registers) : n756(registers);
    }
    private static RuleResult n752(Registers registers) {
        return cond22(registers) ? n753(registers) : n756(registers);
    }
    private static RuleResult n753(Registers registers) {
        return cond23(registers) ? n754(registers) : n756(registers);
    }
    private static RuleResult n754(Registers registers) {
        return cond16(registers) ? n755(registers) : n756(registers);
    }
    private static RuleResult n755(Registers registers) {
        return cond24(registers) ? n1010(registers) : n756(registers);
    }
    private static RuleResult n756(Registers registers) {
        return cond34(registers) ? n757(registers) : result127(registers);
    }
    private static RuleResult n757(Registers registers) {
        return cond27(registers) ? n759(registers) : n758(registers);
    }
    private static RuleResult n759(Registers registers) {
        return cond68(registers) ? result110(registers) : result111(registers);
    }
    private static RuleResult n758(Registers registers) {
        return cond68(registers) ? result112(registers) : result113(registers);
    }
    private static RuleResult n679(Registers registers) {
        return cond36(registers) ? n680(registers) : n761(registers);
    }
    private static RuleResult n680(Registers registers) {
        return cond21(registers) ? n681(registers) : n685(registers);
    }
    private static RuleResult n681(Registers registers) {
        return cond22(registers) ? n682(registers) : n685(registers);
    }
    private static RuleResult n682(Registers registers) {
        return cond23(registers) ? n683(registers) : n685(registers);
    }
    private static RuleResult n683(Registers registers) {
        return cond16(registers) ? n684(registers) : n685(registers);
    }
    private static RuleResult n684(Registers registers) {
        return cond24(registers) ? n1010(registers) : n685(registers);
    }
    private static RuleResult n685(Registers registers) {
        return cond27(registers) ? n732(registers) : n686(registers);
    }
    private static RuleResult n732(Registers registers) {
        return cond26(registers) ? n733(registers) : n734(registers);
    }
    private static RuleResult n733(Registers registers) {
        return cond40(registers) ? n972(registers) : n734(registers);
    }
    private static RuleResult n734(Registers registers) {
        return cond56(registers) ? n965(registers) : n735(registers);
    }
    private static RuleResult n735(Registers registers) {
        return cond41(registers) ? n736(registers) : result170(registers);
    }
    private static RuleResult n736(Registers registers) {
        return cond50(registers) ? n737(registers) : n974(registers);
    }
    private static RuleResult n737(Registers registers) {
        return cond51(registers) ? result170(registers) : n738(registers);
    }
    private static RuleResult n738(Registers registers) {
        return cond57(registers) ? n982(registers) : n739(registers);
    }
    private static RuleResult n739(Registers registers) {
        return cond78(registers) ? n740(registers) : n948(registers);
    }
    private static RuleResult n740(Registers registers) {
        return cond58(registers) ? n986(registers) : n741(registers);
    }
    private static RuleResult n741(Registers registers) {
        // this is a subgraph: [742, 743, 744, 745, 746, 747, 748, 749, 750]
        if (cond49(registers)) {
            if (cond52(registers)) {
                return result142(registers);
            } else {
                if (cond39(registers)) {
                    if (cond42(registers)) {
                        if (cond61(registers)) {
                            if (cond62(registers)) {
                                if (cond75(registers)) {
                                    if (cond72(registers)) {
                                        if (cond76(registers)) {
                                            if (cond73(registers)) {
                                                return result145(registers);
                                            } else {
                                                return result136(registers);
                                            }
                                        } else {
                                            return result137(registers);
                                        }
                                    } else {
                                        return result150(registers);
                                    }
                                } else {
                                    return result138(registers);
                                }
                            } else {
                                return result139(registers);
                            }
                        } else {
                            return result131(registers);
                        }
                    } else {
                        if (cond62(registers)) {
                            if (cond75(registers)) {
                                if (cond72(registers)) {
                                    if (cond76(registers)) {
                                        if (cond73(registers)) {
                                            return result145(registers);
                                        } else {
                                            return result136(registers);
                                        }
                                    } else {
                                        return result137(registers);
                                    }
                                } else {
                                    return result150(registers);
                                }
                            } else {
                                return result138(registers);
                            }
                        } else {
                            return result139(registers);
                        }
                    }
                } else {
                    if (cond62(registers)) {
                        if (cond75(registers)) {
                            if (cond72(registers)) {
                                if (cond76(registers)) {
                                    if (cond73(registers)) {
                                        return result145(registers);
                                    } else {
                                        return result136(registers);
                                    }
                                } else {
                                    return result137(registers);
                                }
                            } else {
                                return result150(registers);
                            }
                        } else {
                            return result138(registers);
                        }
                    } else {
                        return result139(registers);
                    }
                }
            }
        } else {
            return result142(registers);
        }
    }
    private static RuleResult n686(Registers registers) {
        return cond26(registers) ? n687(registers) : n688(registers);
    }
    private static RuleResult n687(Registers registers) {
        return cond40(registers) ? n719(registers) : n688(registers);
    }
    private static RuleResult n719(Registers registers) {
        return cond41(registers) ? n720(registers) : result170(registers);
    }
    private static RuleResult n720(Registers registers) {
        return cond50(registers) ? n724(registers) : n721(registers);
    }
    private static RuleResult n724(Registers registers) {
        return cond51(registers) ? result170(registers) : n725(registers);
    }
    private static RuleResult n725(Registers registers) {
        return cond57(registers) ? n726(registers) : n980(registers);
    }
    private static RuleResult n726(Registers registers) {
        return cond58(registers) ? n730(registers) : n727(registers);
    }
    private static RuleResult n730(Registers registers) {
        return cond49(registers) ? n731(registers) : result142(registers);
    }
    private static RuleResult n731(Registers registers) {
        return cond52(registers) ? result142(registers) : result141(registers);
    }
    private static RuleResult n727(Registers registers) {
        return cond49(registers) ? n728(registers) : result142(registers);
    }
    private static RuleResult n728(Registers registers) {
        return cond52(registers) ? result142(registers) : n729(registers);
    }
    private static RuleResult n729(Registers registers) {
        return cond74(registers) ? result152(registers) : result157(registers);
    }
    private static RuleResult n721(Registers registers) {
        return cond51(registers) ? result170(registers) : n722(registers);
    }
    private static RuleResult n722(Registers registers) {
        return cond58(registers) ? result143(registers) : n723(registers);
    }
    private static RuleResult n723(Registers registers) {
        return cond59(registers) ? result159(registers) : result169(registers);
    }
    private static RuleResult n688(Registers registers) {
        return cond56(registers) ? n715(registers) : n689(registers);
    }
    private static RuleResult n715(Registers registers) {
        return cond41(registers) ? n716(registers) : result170(registers);
    }
    private static RuleResult n716(Registers registers) {
        return cond50(registers) ? n717(registers) : n721(registers);
    }
    private static RuleResult n717(Registers registers) {
        return cond51(registers) ? result170(registers) : n718(registers);
    }
    private static RuleResult n718(Registers registers) {
        return cond57(registers) ? n726(registers) : n970(registers);
    }
    private static RuleResult n689(Registers registers) {
        return cond41(registers) ? n690(registers) : result170(registers);
    }
    private static RuleResult n690(Registers registers) {
        return cond50(registers) ? n691(registers) : n721(registers);
    }
    private static RuleResult n691(Registers registers) {
        return cond51(registers) ? result170(registers) : n692(registers);
    }
    private static RuleResult n692(Registers registers) {
        return cond57(registers) ? n726(registers) : n693(registers);
    }
    private static RuleResult n693(Registers registers) {
        return cond78(registers) ? n694(registers) : n949(registers);
    }
    private static RuleResult n694(Registers registers) {
        return cond58(registers) ? n705(registers) : n695(registers);
    }
    private static RuleResult n705(Registers registers) {
        // this is a subgraph: [706, 707, 708, 709, 710, 711, 712, 713, 714]
        if (cond49(registers)) {
            if (cond52(registers)) {
                return result142(registers);
            } else {
                if (cond39(registers)) {
                    if (cond42(registers)) {
                        if (cond61(registers)) {
                            if (cond62(registers)) {
                                if (cond75(registers)) {
                                    if (cond71(registers)) {
                                        return result132(registers);
                                    } else {
                                        if (cond76(registers)) {
                                            if (cond73(registers)) {
                                                return result134(registers);
                                            } else {
                                                return result136(registers);
                                            }
                                        } else {
                                            return result137(registers);
                                        }
                                    }
                                } else {
                                    return result138(registers);
                                }
                            } else {
                                return result139(registers);
                            }
                        } else {
                            return result131(registers);
                        }
                    } else {
                        if (cond62(registers)) {
                            if (cond75(registers)) {
                                if (cond71(registers)) {
                                    return result132(registers);
                                } else {
                                    if (cond76(registers)) {
                                        if (cond73(registers)) {
                                            return result134(registers);
                                        } else {
                                            return result136(registers);
                                        }
                                    } else {
                                        return result137(registers);
                                    }
                                }
                            } else {
                                return result138(registers);
                            }
                        } else {
                            return result139(registers);
                        }
                    }
                } else {
                    if (cond62(registers)) {
                        if (cond75(registers)) {
                            if (cond71(registers)) {
                                return result132(registers);
                            } else {
                                if (cond76(registers)) {
                                    if (cond73(registers)) {
                                        return result134(registers);
                                    } else {
                                        return result136(registers);
                                    }
                                } else {
                                    return result137(registers);
                                }
                            }
                        } else {
                            return result138(registers);
                        }
                    } else {
                        return result139(registers);
                    }
                }
            }
        } else {
            return result142(registers);
        }
    }
    private static RuleResult n695(Registers registers) {
        // this is a subgraph: [696, 697, 698, 699, 700, 701, 702, 703, 704]
        if (cond49(registers)) {
            if (cond52(registers)) {
                return result142(registers);
            } else {
                if (cond39(registers)) {
                    if (cond42(registers)) {
                        if (cond61(registers)) {
                            if (cond62(registers)) {
                                if (cond75(registers)) {
                                    if (cond72(registers)) {
                                        if (cond76(registers)) {
                                            if (cond73(registers)) {
                                                return result146(registers);
                                            } else {
                                                return result136(registers);
                                            }
                                        } else {
                                            return result137(registers);
                                        }
                                    } else {
                                        return result150(registers);
                                    }
                                } else {
                                    return result138(registers);
                                }
                            } else {
                                return result139(registers);
                            }
                        } else {
                            return result131(registers);
                        }
                    } else {
                        if (cond62(registers)) {
                            if (cond75(registers)) {
                                if (cond72(registers)) {
                                    if (cond76(registers)) {
                                        if (cond73(registers)) {
                                            return result146(registers);
                                        } else {
                                            return result136(registers);
                                        }
                                    } else {
                                        return result137(registers);
                                    }
                                } else {
                                    return result150(registers);
                                }
                            } else {
                                return result138(registers);
                            }
                        } else {
                            return result139(registers);
                        }
                    }
                } else {
                    if (cond62(registers)) {
                        if (cond75(registers)) {
                            if (cond72(registers)) {
                                if (cond76(registers)) {
                                    if (cond73(registers)) {
                                        return result146(registers);
                                    } else {
                                        return result136(registers);
                                    }
                                } else {
                                    return result137(registers);
                                }
                            } else {
                                return result150(registers);
                            }
                        } else {
                            return result138(registers);
                        }
                    } else {
                        return result139(registers);
                    }
                }
            }
        } else {
            return result142(registers);
        }
    }
    private static RuleResult n669(Registers registers) {
        return cond79(registers) ? n796(registers) : n670(registers);
    }
    private static RuleResult n670(Registers registers) {
        return cond80(registers) ? n774(registers) : n671(registers);
    }
    private static RuleResult n671(Registers registers) {
        return cond81(registers) ? n672(registers) : n677(registers);
    }
    private static RuleResult n672(Registers registers) {
        return cond10(registers) ? n895(registers) : n673(registers);
    }
    private static RuleResult n673(Registers registers) {
        return cond12(registers) ? n751(registers) : n674(registers);
    }
    private static RuleResult n674(Registers registers) {
        return cond36(registers) ? n680(registers) : n895(registers);
    }
    private static RuleResult n614(Registers registers) {
        return cond20(registers) ? n617(registers) : n615(registers);
    }
    private static RuleResult n617(Registers registers) {
        return cond79(registers) ? n641(registers) : n618(registers);
    }
    private static RuleResult n641(Registers registers) {
        return cond12(registers) ? n642(registers) : n797(registers);
    }
    private static RuleResult n642(Registers registers) {
        return cond37(registers) ? n824(registers) : n643(registers);
    }
    private static RuleResult n643(Registers registers) {
        return cond27(registers) ? n656(registers) : n644(registers);
    }
    private static RuleResult n656(Registers registers) {
        // this is a subgraph: [657, 666, 667, 658, 659, 660, 661, 662, 663, 664, 665]
        if (cond82(registers)) {
            if (cond31(registers)) {
                if (cond84(registers)) {
                    if (cond44(registers)) {
                        return result36(registers);
                    } else {
                        return result40(registers);
                    }
                } else {
                    return result40(registers);
                }
            } else {
                if (cond84(registers)) {
                    if (cond44(registers)) {
                        return result36(registers);
                    } else {
                        if (cond85(registers)) {
                            if (cond46(registers)) {
                                return result44(registers);
                            } else {
                                if (cond47(registers)) {
                                    if (cond86(registers)) {
                                        return result48(registers);
                                    } else {
                                        if (cond54(registers)) {
                                            if (cond88(registers)) {
                                                return result52(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result52(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result48(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result52(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result52(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                } else {
                    if (cond85(registers)) {
                        if (cond46(registers)) {
                            return result44(registers);
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result48(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result52(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result52(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result48(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result52(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result52(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            }
        } else {
            if (cond84(registers)) {
                if (cond44(registers)) {
                    return result36(registers);
                } else {
                    if (cond85(registers)) {
                        if (cond46(registers)) {
                            return result44(registers);
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result48(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result52(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result52(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result48(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result52(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result52(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            } else {
                if (cond85(registers)) {
                    if (cond46(registers)) {
                        return result44(registers);
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result48(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result52(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result52(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                } else {
                    if (cond47(registers)) {
                        if (cond86(registers)) {
                            return result48(registers);
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result52(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    } else {
                        if (cond54(registers)) {
                            if (cond88(registers)) {
                                return result52(registers);
                            } else {
                                return result35(registers);
                            }
                        } else {
                            return result35(registers);
                        }
                    }
                }
            }
        }
    }
    private static RuleResult n644(Registers registers) {
        // this is a subgraph: [645, 654, 655, 646, 647, 648, 649, 650, 651, 652, 653]
        if (cond82(registers)) {
            if (cond31(registers)) {
                if (cond84(registers)) {
                    if (cond44(registers)) {
                        return result37(registers);
                    } else {
                        return result41(registers);
                    }
                } else {
                    return result41(registers);
                }
            } else {
                if (cond84(registers)) {
                    if (cond44(registers)) {
                        return result37(registers);
                    } else {
                        if (cond85(registers)) {
                            if (cond46(registers)) {
                                return result45(registers);
                            } else {
                                if (cond47(registers)) {
                                    if (cond86(registers)) {
                                        return result49(registers);
                                    } else {
                                        if (cond54(registers)) {
                                            if (cond88(registers)) {
                                                return result53(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result53(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result49(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result53(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result53(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                } else {
                    if (cond85(registers)) {
                        if (cond46(registers)) {
                            return result45(registers);
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result49(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result53(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result53(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result49(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result53(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result53(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            }
        } else {
            if (cond84(registers)) {
                if (cond44(registers)) {
                    return result37(registers);
                } else {
                    if (cond85(registers)) {
                        if (cond46(registers)) {
                            return result45(registers);
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result49(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result53(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result53(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result49(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result53(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result53(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            } else {
                if (cond85(registers)) {
                    if (cond46(registers)) {
                        return result45(registers);
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result49(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result53(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result53(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                } else {
                    if (cond47(registers)) {
                        if (cond86(registers)) {
                            return result49(registers);
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result53(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    } else {
                        if (cond54(registers)) {
                            if (cond88(registers)) {
                                return result53(registers);
                            } else {
                                return result35(registers);
                            }
                        } else {
                            return result35(registers);
                        }
                    }
                }
            }
        }
    }
    private static RuleResult n618(Registers registers) {
        return cond80(registers) ? n619(registers) : n677(registers);
    }
    private static RuleResult n619(Registers registers) {
        return cond12(registers) ? n620(registers) : result8(registers);
    }
    private static RuleResult n620(Registers registers) {
        return cond27(registers) ? n631(registers) : n621(registers);
    }
    private static RuleResult n631(Registers registers) {
        // this is a subgraph: [632, 633, 634, 635, 636, 637, 638, 639, 640]
        if (cond30(registers)) {
            if (cond82(registers)) {
                return result76(registers);
            } else {
                if (cond83(registers)) {
                    if (cond32(registers)) {
                        return result80(registers);
                    } else {
                        if (cond45(registers)) {
                            if (cond86(registers)) {
                                return result84(registers);
                            } else {
                                if (cond87(registers)) {
                                    if (cond53(registers)) {
                                        return result88(registers);
                                    } else {
                                        if (cond55(registers)) {
                                            if (cond89(registers)) {
                                                return result92(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result92(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result88(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result92(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result92(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                } else {
                    if (cond45(registers)) {
                        if (cond86(registers)) {
                            return result84(registers);
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result88(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result92(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result92(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result88(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result92(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result92(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            }
        } else {
            if (cond83(registers)) {
                if (cond32(registers)) {
                    return result80(registers);
                } else {
                    if (cond45(registers)) {
                        if (cond86(registers)) {
                            return result84(registers);
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result88(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result92(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result92(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result88(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result92(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result92(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            } else {
                if (cond45(registers)) {
                    if (cond86(registers)) {
                        return result84(registers);
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result88(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result92(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result92(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                } else {
                    if (cond87(registers)) {
                        if (cond53(registers)) {
                            return result88(registers);
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result92(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    } else {
                        if (cond55(registers)) {
                            if (cond89(registers)) {
                                return result92(registers);
                            } else {
                                return result35(registers);
                            }
                        } else {
                            return result35(registers);
                        }
                    }
                }
            }
        }
    }
    private static RuleResult n621(Registers registers) {
        // this is a subgraph: [622, 623, 624, 625, 626, 627, 628, 629, 630]
        if (cond30(registers)) {
            if (cond82(registers)) {
                return result77(registers);
            } else {
                if (cond83(registers)) {
                    if (cond32(registers)) {
                        return result81(registers);
                    } else {
                        if (cond45(registers)) {
                            if (cond86(registers)) {
                                return result85(registers);
                            } else {
                                if (cond87(registers)) {
                                    if (cond53(registers)) {
                                        return result89(registers);
                                    } else {
                                        if (cond55(registers)) {
                                            if (cond89(registers)) {
                                                return result93(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result93(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result89(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result93(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result93(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                } else {
                    if (cond45(registers)) {
                        if (cond86(registers)) {
                            return result85(registers);
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result89(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result93(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result93(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result89(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result93(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result93(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            }
        } else {
            if (cond83(registers)) {
                if (cond32(registers)) {
                    return result81(registers);
                } else {
                    if (cond45(registers)) {
                        if (cond86(registers)) {
                            return result85(registers);
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result89(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result93(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result93(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result89(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result93(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result93(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            } else {
                if (cond45(registers)) {
                    if (cond86(registers)) {
                        return result85(registers);
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result89(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result93(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result93(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                } else {
                    if (cond87(registers)) {
                        if (cond53(registers)) {
                            return result89(registers);
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result93(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    } else {
                        if (cond55(registers)) {
                            if (cond89(registers)) {
                                return result93(registers);
                            } else {
                                return result35(registers);
                            }
                        } else {
                            return result35(registers);
                        }
                    }
                }
            }
        }
    }
    private static RuleResult n615(Registers registers) {
        return cond79(registers) ? n641(registers) : n616(registers);
    }
    private static RuleResult n616(Registers registers) {
        return cond80(registers) ? n619(registers) : n671(registers);
    }
    private static RuleResult n602(Registers registers) {
        return cond7(registers) ? n603(registers) : n604(registers);
    }
    private static RuleResult n603(Registers registers) {
        return cond9(registers) ? n608(registers) : n604(registers);
    }
    private static RuleResult n608(Registers registers) {
        return cond20(registers) ? n610(registers) : n609(registers);
    }
    private static RuleResult n610(Registers registers) {
        return cond79(registers) ? n611(registers) : n676(registers);
    }
    private static RuleResult n611(Registers registers) {
        return cond12(registers) ? n799(registers) : result8(registers);
    }
    private static RuleResult n609(Registers registers) {
        return cond79(registers) ? n611(registers) : n670(registers);
    }
    private static RuleResult n604(Registers registers) {
        return cond20(registers) ? n606(registers) : n605(registers);
    }
    private static RuleResult n606(Registers registers) {
        return cond79(registers) ? n607(registers) : n618(registers);
    }
    private static RuleResult n607(Registers registers) {
        return cond12(registers) ? n643(registers) : result8(registers);
    }
    private static RuleResult n605(Registers registers) {
        return cond79(registers) ? n607(registers) : n616(registers);
    }
    private static RuleResult n581(Registers registers) {
        return cond20(registers) ? n587(registers) : n582(registers);
    }
    private static RuleResult n587(Registers registers) {
        return cond79(registers) ? result8(registers) : n588(registers);
    }
    private static RuleResult n588(Registers registers) {
        return cond80(registers) ? result8(registers) : n589(registers);
    }
    private static RuleResult n589(Registers registers) {
        return cond10(registers) ? n867(registers) : n590(registers);
    }
    private static RuleResult n590(Registers registers) {
        return cond36(registers) ? n591(registers) : result199(registers);
    }
    private static RuleResult n591(Registers registers) {
        return cond27(registers) ? n860(registers) : n592(registers);
    }
    private static RuleResult n592(Registers registers) {
        return cond26(registers) ? n593(registers) : n594(registers);
    }
    private static RuleResult n593(Registers registers) {
        return cond40(registers) ? n719(registers) : n594(registers);
    }
    private static RuleResult n594(Registers registers) {
        return cond56(registers) ? n715(registers) : n595(registers);
    }
    private static RuleResult n595(Registers registers) {
        return cond41(registers) ? n596(registers) : result170(registers);
    }
    private static RuleResult n596(Registers registers) {
        return cond50(registers) ? n597(registers) : n721(registers);
    }
    private static RuleResult n597(Registers registers) {
        return cond51(registers) ? result170(registers) : n598(registers);
    }
    private static RuleResult n598(Registers registers) {
        return cond57(registers) ? n726(registers) : n949(registers);
    }
    private static RuleResult n582(Registers registers) {
        return cond79(registers) ? result8(registers) : n583(registers);
    }
    private static RuleResult n583(Registers registers) {
        return cond80(registers) ? result8(registers) : n584(registers);
    }
    private static RuleResult n584(Registers registers) {
        return cond81(registers) ? n585(registers) : n589(registers);
    }
    private static RuleResult n585(Registers registers) {
        return cond10(registers) ? result171(registers) : n586(registers);
    }
    private static RuleResult n586(Registers registers) {
        return cond36(registers) ? n591(registers) : result171(registers);
    }
    private static RuleResult n569(Registers registers) {
        // this is a subgraph: [570, 571, 572, 579, 573, 574, 769, 770, 575, 576, 578, 577]
        if (cond19(registers)) {
            if (cond6(registers)) {
                return result4(registers);
            } else {
                if (cond3(registers)) {
                    if (cond8(registers)) {
                        if (cond27(registers)) {
                            return result97(registers);
                        } else {
                            return result98(registers);
                        }
                    } else {
                        if (cond15(registers)) {
                            if (cond17(registers)) {
                                if (cond33(registers)) {
                                    if (cond27(registers)) {
                                        return result128(registers);
                                    } else {
                                        return result186(registers);
                                    }
                                } else {
                                    return result127(registers);
                                }
                            } else {
                                if (cond33(registers)) {
                                    if (cond27(registers)) {
                                        if (cond68(registers)) {
                                            return result188(registers);
                                        } else {
                                            return result189(registers);
                                        }
                                    } else {
                                        if (cond68(registers)) {
                                            return result190(registers);
                                        } else {
                                            return result191(registers);
                                        }
                                    }
                                } else {
                                    return result127(registers);
                                }
                            }
                        } else {
                            if (cond33(registers)) {
                                if (cond27(registers)) {
                                    if (cond68(registers)) {
                                        return result188(registers);
                                    } else {
                                        return result189(registers);
                                    }
                                } else {
                                    if (cond68(registers)) {
                                        return result190(registers);
                                    } else {
                                        return result191(registers);
                                    }
                                }
                            } else {
                                return result127(registers);
                            }
                        }
                    }
                } else {
                    if (cond15(registers)) {
                        if (cond17(registers)) {
                            if (cond33(registers)) {
                                if (cond27(registers)) {
                                    return result128(registers);
                                } else {
                                    return result186(registers);
                                }
                            } else {
                                return result127(registers);
                            }
                        } else {
                            if (cond33(registers)) {
                                if (cond27(registers)) {
                                    if (cond68(registers)) {
                                        return result188(registers);
                                    } else {
                                        return result189(registers);
                                    }
                                } else {
                                    if (cond68(registers)) {
                                        return result190(registers);
                                    } else {
                                        return result191(registers);
                                    }
                                }
                            } else {
                                return result127(registers);
                            }
                        }
                    } else {
                        if (cond33(registers)) {
                            if (cond27(registers)) {
                                if (cond68(registers)) {
                                    return result188(registers);
                                } else {
                                    return result189(registers);
                                }
                            } else {
                                if (cond68(registers)) {
                                    return result190(registers);
                                } else {
                                    return result191(registers);
                                }
                            }
                        } else {
                            return result127(registers);
                        }
                    }
                }
            }
        } else {
            return result199(registers);
        }
    }
    private static RuleResult n3(Registers registers) {
        return cond1(registers) ? n161(registers) : n4(registers);
    }
    private static RuleResult n161(Registers registers) {
        return cond2(registers) ? n176(registers) : n162(registers);
    }
    private static RuleResult n176(Registers registers) {
        return cond19(registers) ? n229(registers) : n177(registers);
    }
    private static RuleResult n229(Registers registers) {
        return cond7(registers) ? n230(registers) : n231(registers);
    }
    private static RuleResult n230(Registers registers) {
        return cond9(registers) ? n271(registers) : n231(registers);
    }
    private static RuleResult n271(Registers registers) {
        return cond20(registers) ? n296(registers) : n272(registers);
    }
    private static RuleResult n296(Registers registers) {
        return cond79(registers) ? n544(registers) : n297(registers);
    }
    private static RuleResult n544(Registers registers) {
        return cond38(registers) ? n559(registers) : n545(registers);
    }
    private static RuleResult n559(Registers registers) {
        return cond12(registers) ? n563(registers) : n560(registers);
    }
    private static RuleResult n563(Registers registers) {
        return cond37(registers) ? n564(registers) : n565(registers);
    }
    private static RuleResult n564(Registers registers) {
        return cond25(registers) ? n566(registers) : n565(registers);
    }
    private static RuleResult n566(Registers registers) {
        return cond27(registers) ? result1(registers) : result6(registers);
    }
    private static RuleResult n565(Registers registers) {
        return cond27(registers) ? result1(registers) : result7(registers);
    }
    private static RuleResult n560(Registers registers) {
        return cond37(registers) ? n561(registers) : n562(registers);
    }
    private static RuleResult n561(Registers registers) {
        return cond25(registers) ? n566(registers) : n562(registers);
    }
    private static RuleResult n562(Registers registers) {
        return cond27(registers) ? result1(registers) : result8(registers);
    }
    private static RuleResult n545(Registers registers) {
        return cond12(registers) ? n546(registers) : n562(registers);
    }
    private static RuleResult n546(Registers registers) {
        // this is a subgraph: [547, 548, 557, 558, 549, 550, 551, 552, 553, 554, 555, 556]
        if (cond27(registers)) {
            return result1(registers);
        } else {
            if (cond82(registers)) {
                if (cond31(registers)) {
                    if (cond84(registers)) {
                        if (cond44(registers)) {
                            return result18(registers);
                        } else {
                            return result22(registers);
                        }
                    } else {
                        return result22(registers);
                    }
                } else {
                    if (cond84(registers)) {
                        if (cond44(registers)) {
                            return result18(registers);
                        } else {
                            if (cond85(registers)) {
                                if (cond46(registers)) {
                                    return result26(registers);
                                } else {
                                    if (cond47(registers)) {
                                        if (cond86(registers)) {
                                            return result30(registers);
                                        } else {
                                            if (cond54(registers)) {
                                                if (cond88(registers)) {
                                                    return result34(registers);
                                                } else {
                                                    return result35(registers);
                                                }
                                            } else {
                                                return result35(registers);
                                            }
                                        }
                                    } else {
                                        if (cond54(registers)) {
                                            if (cond88(registers)) {
                                                return result34(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                }
                            } else {
                                if (cond47(registers)) {
                                    if (cond86(registers)) {
                                        return result30(registers);
                                    } else {
                                        if (cond54(registers)) {
                                            if (cond88(registers)) {
                                                return result34(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result34(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        }
                    } else {
                        if (cond85(registers)) {
                            if (cond46(registers)) {
                                return result26(registers);
                            } else {
                                if (cond47(registers)) {
                                    if (cond86(registers)) {
                                        return result30(registers);
                                    } else {
                                        if (cond54(registers)) {
                                            if (cond88(registers)) {
                                                return result34(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result34(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result30(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result34(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result34(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                }
            } else {
                if (cond84(registers)) {
                    if (cond44(registers)) {
                        return result18(registers);
                    } else {
                        if (cond85(registers)) {
                            if (cond46(registers)) {
                                return result26(registers);
                            } else {
                                if (cond47(registers)) {
                                    if (cond86(registers)) {
                                        return result30(registers);
                                    } else {
                                        if (cond54(registers)) {
                                            if (cond88(registers)) {
                                                return result34(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result34(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result30(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result34(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result34(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                } else {
                    if (cond85(registers)) {
                        if (cond46(registers)) {
                            return result26(registers);
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result30(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result34(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result34(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result30(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result34(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result34(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            }
        }
    }
    private static RuleResult n297(Registers registers) {
        return cond80(registers) ? n531(registers) : n298(registers);
    }
    private static RuleResult n531(Registers registers) {
        return cond38(registers) ? n559(registers) : n532(registers);
    }
    private static RuleResult n532(Registers registers) {
        return cond12(registers) ? n533(registers) : n562(registers);
    }
    private static RuleResult n533(Registers registers) {
        // this is a subgraph: [534, 535, 536, 537, 538, 539, 540, 541, 542, 543]
        if (cond27(registers)) {
            return result1(registers);
        } else {
            if (cond30(registers)) {
                if (cond82(registers)) {
                    return result59(registers);
                } else {
                    if (cond83(registers)) {
                        if (cond32(registers)) {
                            return result63(registers);
                        } else {
                            if (cond45(registers)) {
                                if (cond86(registers)) {
                                    return result67(registers);
                                } else {
                                    if (cond87(registers)) {
                                        if (cond53(registers)) {
                                            return result71(registers);
                                        } else {
                                            if (cond55(registers)) {
                                                if (cond89(registers)) {
                                                    return result75(registers);
                                                } else {
                                                    return result35(registers);
                                                }
                                            } else {
                                                return result35(registers);
                                            }
                                        }
                                    } else {
                                        if (cond55(registers)) {
                                            if (cond89(registers)) {
                                                return result75(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                }
                            } else {
                                if (cond87(registers)) {
                                    if (cond53(registers)) {
                                        return result71(registers);
                                    } else {
                                        if (cond55(registers)) {
                                            if (cond89(registers)) {
                                                return result75(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result75(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        }
                    } else {
                        if (cond45(registers)) {
                            if (cond86(registers)) {
                                return result67(registers);
                            } else {
                                if (cond87(registers)) {
                                    if (cond53(registers)) {
                                        return result71(registers);
                                    } else {
                                        if (cond55(registers)) {
                                            if (cond89(registers)) {
                                                return result75(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result75(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result71(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result75(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result75(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                }
            } else {
                if (cond83(registers)) {
                    if (cond32(registers)) {
                        return result63(registers);
                    } else {
                        if (cond45(registers)) {
                            if (cond86(registers)) {
                                return result67(registers);
                            } else {
                                if (cond87(registers)) {
                                    if (cond53(registers)) {
                                        return result71(registers);
                                    } else {
                                        if (cond55(registers)) {
                                            if (cond89(registers)) {
                                                return result75(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result75(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result71(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result75(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result75(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                } else {
                    if (cond45(registers)) {
                        if (cond86(registers)) {
                            return result67(registers);
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result71(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result75(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result75(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result71(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result75(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result75(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            }
        }
    }
    private static RuleResult n298(Registers registers) {
        return cond90(registers) ? n311(registers) : n299(registers);
    }
    private static RuleResult n311(Registers registers) {
        return cond38(registers) ? n396(registers) : n312(registers);
    }
    private static RuleResult n396(Registers registers) {
        return cond10(registers) ? n504(registers) : n397(registers);
    }
    private static RuleResult n504(Registers registers) {
        return cond11(registers) ? n519(registers) : n505(registers);
    }
    private static RuleResult n519(Registers registers) {
        return cond21(registers) ? n520(registers) : n524(registers);
    }
    private static RuleResult n520(Registers registers) {
        return cond22(registers) ? n521(registers) : n524(registers);
    }
    private static RuleResult n521(Registers registers) {
        return cond23(registers) ? n522(registers) : n524(registers);
    }
    private static RuleResult n522(Registers registers) {
        return cond16(registers) ? n523(registers) : n524(registers);
    }
    private static RuleResult n523(Registers registers) {
        return cond24(registers) ? n525(registers) : n524(registers);
    }
    private static RuleResult n525(Registers registers) {
        return cond18(registers) ? n527(registers) : n526(registers);
    }
    private static RuleResult n527(Registers registers) {
        return cond27(registers) ? result1(registers) : n528(registers);
    }
    private static RuleResult n528(Registers registers) {
        return cond35(registers) ? n529(registers) : result107(registers);
    }
    private static RuleResult n529(Registers registers) {
        return cond28(registers) ? result102(registers) : n530(registers);
    }
    private static RuleResult n530(Registers registers) {
        return cond29(registers) ? result104(registers) : result106(registers);
    }
    private static RuleResult n526(Registers registers) {
        return cond27(registers) ? result1(registers) : n1011(registers);
    }
    private static RuleResult n1011(Registers registers) {
        return cond35(registers) ? n1012(registers) : result107(registers);
    }
    private static RuleResult n1012(Registers registers) {
        return cond28(registers) ? result103(registers) : n1013(registers);
    }
    private static RuleResult n1013(Registers registers) {
        return cond29(registers) ? result105(registers) : result106(registers);
    }
    private static RuleResult n524(Registers registers) {
        return cond27(registers) ? result1(registers) : result172(registers);
    }
    private static RuleResult n505(Registers registers) {
        return cond21(registers) ? n506(registers) : n510(registers);
    }
    private static RuleResult n506(Registers registers) {
        return cond22(registers) ? n507(registers) : n510(registers);
    }
    private static RuleResult n507(Registers registers) {
        return cond23(registers) ? n508(registers) : n510(registers);
    }
    private static RuleResult n508(Registers registers) {
        return cond16(registers) ? n509(registers) : n510(registers);
    }
    private static RuleResult n509(Registers registers) {
        return cond24(registers) ? n525(registers) : n510(registers);
    }
    private static RuleResult n510(Registers registers) {
        return cond37(registers) ? n517(registers) : n511(registers);
    }
    private static RuleResult n517(Registers registers) {
        return cond27(registers) ? result1(registers) : n518(registers);
    }
    private static RuleResult n518(Registers registers) {
        return cond68(registers) ? result179(registers) : result180(registers);
    }
    private static RuleResult n511(Registers registers) {
        return cond15(registers) ? n512(registers) : n513(registers);
    }
    private static RuleResult n512(Registers registers) {
        return cond17(registers) ? n514(registers) : n513(registers);
    }
    private static RuleResult n514(Registers registers) {
        return cond33(registers) ? n516(registers) : n515(registers);
    }
    private static RuleResult n516(Registers registers) {
        return cond27(registers) ? result1(registers) : result185(registers);
    }
    private static RuleResult n515(Registers registers) {
        return cond27(registers) ? result1(registers) : result127(registers);
    }
    private static RuleResult n513(Registers registers) {
        return cond27(registers) ? result1(registers) : result199(registers);
    }
    private static RuleResult n397(Registers registers) {
        return cond12(registers) ? n492(registers) : n398(registers);
    }
    private static RuleResult n492(Registers registers) {
        return cond21(registers) ? n493(registers) : n497(registers);
    }
    private static RuleResult n493(Registers registers) {
        return cond22(registers) ? n494(registers) : n497(registers);
    }
    private static RuleResult n494(Registers registers) {
        return cond23(registers) ? n495(registers) : n497(registers);
    }
    private static RuleResult n495(Registers registers) {
        return cond16(registers) ? n496(registers) : n497(registers);
    }
    private static RuleResult n496(Registers registers) {
        return cond24(registers) ? n525(registers) : n497(registers);
    }
    private static RuleResult n497(Registers registers) {
        return cond34(registers) ? n498(registers) : n515(registers);
    }
    private static RuleResult n498(Registers registers) {
        // this is a subgraph: [502, 503, 499, 500, 501]
        if (cond25(registers)) {
            if (cond27(registers)) {
                return result1(registers);
            } else {
                if (cond68(registers)) {
                    return result118(registers);
                } else {
                    return result120(registers);
                }
            }
        } else {
            if (cond27(registers)) {
                return result1(registers);
            } else {
                if (cond70(registers)) {
                    if (cond68(registers)) {
                        return result119(registers);
                    } else {
                        return result121(registers);
                    }
                } else {
                    return result127(registers);
                }
            }
        }
    }
    private static RuleResult n398(Registers registers) {
        return cond13(registers) ? n399(registers) : n400(registers);
    }
    private static RuleResult n399(Registers registers) {
        return cond14(registers) ? n485(registers) : n400(registers);
    }
    private static RuleResult n485(Registers registers) {
        return cond21(registers) ? n486(registers) : n490(registers);
    }
    private static RuleResult n486(Registers registers) {
        return cond22(registers) ? n487(registers) : n490(registers);
    }
    private static RuleResult n487(Registers registers) {
        return cond23(registers) ? n488(registers) : n490(registers);
    }
    private static RuleResult n488(Registers registers) {
        return cond16(registers) ? n489(registers) : n490(registers);
    }
    private static RuleResult n489(Registers registers) {
        return cond24(registers) ? n525(registers) : n490(registers);
    }
    private static RuleResult n490(Registers registers) {
        return cond34(registers) ? n491(registers) : n515(registers);
    }
    private static RuleResult n491(Registers registers) {
        return cond27(registers) ? result1(registers) : result121(registers);
    }
    private static RuleResult n400(Registers registers) {
        return cond36(registers) ? n401(registers) : n505(registers);
    }
    private static RuleResult n401(Registers registers) {
        return cond21(registers) ? n402(registers) : n406(registers);
    }
    private static RuleResult n402(Registers registers) {
        return cond22(registers) ? n403(registers) : n406(registers);
    }
    private static RuleResult n403(Registers registers) {
        return cond23(registers) ? n404(registers) : n406(registers);
    }
    private static RuleResult n404(Registers registers) {
        return cond16(registers) ? n405(registers) : n406(registers);
    }
    private static RuleResult n405(Registers registers) {
        return cond24(registers) ? n525(registers) : n406(registers);
    }
    private static RuleResult n406(Registers registers) {
        return cond27(registers) ? result1(registers) : n407(registers);
    }
    private static RuleResult n407(Registers registers) {
        return cond26(registers) ? n408(registers) : n409(registers);
    }
    private static RuleResult n408(Registers registers) {
        return cond40(registers) ? n446(registers) : n409(registers);
    }
    private static RuleResult n446(Registers registers) {
        return cond48(registers) ? n471(registers) : n447(registers);
    }
    private static RuleResult n471(Registers registers) {
        return cond41(registers) ? n472(registers) : result170(registers);
    }
    private static RuleResult n472(Registers registers) {
        return cond50(registers) ? n476(registers) : n473(registers);
    }
    private static RuleResult n476(Registers registers) {
        return cond51(registers) ? result170(registers) : n477(registers);
    }
    private static RuleResult n477(Registers registers) {
        return cond57(registers) ? n478(registers) : n980(registers);
    }
    private static RuleResult n478(Registers registers) {
        return cond58(registers) ? n730(registers) : n479(registers);
    }
    private static RuleResult n479(Registers registers) {
        // this is a subgraph: [480, 481, 484, 482, 483]
        if (cond49(registers)) {
            if (cond52(registers)) {
                return result142(registers);
            } else {
                if (cond63(registers)) {
                    if (cond74(registers)) {
                        return result154(registers);
                    } else {
                        return result157(registers);
                    }
                } else {
                    if (cond74(registers)) {
                        if (cond77(registers)) {
                            return result155(registers);
                        } else {
                            return result156(registers);
                        }
                    } else {
                        return result157(registers);
                    }
                }
            }
        } else {
            return result142(registers);
        }
    }
    private static RuleResult n473(Registers registers) {
        return cond51(registers) ? result170(registers) : n474(registers);
    }
    private static RuleResult n474(Registers registers) {
        return cond58(registers) ? result143(registers) : n475(registers);
    }
    private static RuleResult n475(Registers registers) {
        return cond59(registers) ? result161(registers) : result169(registers);
    }
    private static RuleResult n447(Registers registers) {
        return cond41(registers) ? n448(registers) : result170(registers);
    }
    private static RuleResult n448(Registers registers) {
        return cond50(registers) ? n476(registers) : n449(registers);
    }
    private static RuleResult n449(Registers registers) {
        return cond51(registers) ? result170(registers) : n450(registers);
    }
    private static RuleResult n450(Registers registers) {
        return cond78(registers) ? n458(registers) : n451(registers);
    }
    private static RuleResult n458(Registers registers) {
        // this is a subgraph: [459, 460, 461, 462, 463, 464, 465, 466, 467, 468, 469, 470]
        if (cond58(registers)) {
            return result143(registers);
        } else {
            if (cond59(registers)) {
                if (cond43(registers)) {
                    if (cond60(registers)) {
                        if (cond39(registers)) {
                            if (cond42(registers)) {
                                if (cond61(registers)) {
                                    if (cond62(registers)) {
                                        if (cond75(registers)) {
                                            if (cond76(registers)) {
                                                if (cond65(registers)) {
                                                    if (cond66(registers)) {
                                                        if (cond67(registers)) {
                                                            return result162(registers);
                                                        } else {
                                                            return result164(registers);
                                                        }
                                                    } else {
                                                        return result165(registers);
                                                    }
                                                } else {
                                                    return result166(registers);
                                                }
                                            } else {
                                                return result137(registers);
                                            }
                                        } else {
                                            return result138(registers);
                                        }
                                    } else {
                                        return result139(registers);
                                    }
                                } else {
                                    return result131(registers);
                                }
                            } else {
                                if (cond62(registers)) {
                                    if (cond75(registers)) {
                                        if (cond76(registers)) {
                                            if (cond65(registers)) {
                                                if (cond66(registers)) {
                                                    if (cond67(registers)) {
                                                        return result162(registers);
                                                    } else {
                                                        return result164(registers);
                                                    }
                                                } else {
                                                    return result165(registers);
                                                }
                                            } else {
                                                return result166(registers);
                                            }
                                        } else {
                                            return result137(registers);
                                        }
                                    } else {
                                        return result138(registers);
                                    }
                                } else {
                                    return result139(registers);
                                }
                            }
                        } else {
                            if (cond62(registers)) {
                                if (cond75(registers)) {
                                    if (cond76(registers)) {
                                        if (cond65(registers)) {
                                            if (cond66(registers)) {
                                                if (cond67(registers)) {
                                                    return result162(registers);
                                                } else {
                                                    return result164(registers);
                                                }
                                            } else {
                                                return result165(registers);
                                            }
                                        } else {
                                            return result166(registers);
                                        }
                                    } else {
                                        return result137(registers);
                                    }
                                } else {
                                    return result138(registers);
                                }
                            } else {
                                return result139(registers);
                            }
                        }
                    } else {
                        return result167(registers);
                    }
                } else {
                    return result168(registers);
                }
            } else {
                return result169(registers);
            }
        }
    }
    private static RuleResult n451(Registers registers) {
        // this is a subgraph: [452, 453, 454, 455, 456, 457]
        if (cond58(registers)) {
            return result143(registers);
        } else {
            if (cond59(registers)) {
                if (cond43(registers)) {
                    if (cond60(registers)) {
                        if (cond39(registers)) {
                            if (cond42(registers)) {
                                if (cond61(registers)) {
                                    return result167(registers);
                                } else {
                                    return result131(registers);
                                }
                            } else {
                                return result167(registers);
                            }
                        } else {
                            return result167(registers);
                        }
                    } else {
                        return result167(registers);
                    }
                } else {
                    return result168(registers);
                }
            } else {
                return result169(registers);
            }
        }
    }
    private static RuleResult n409(Registers registers) {
        return cond48(registers) ? n415(registers) : n410(registers);
    }
    private static RuleResult n415(Registers registers) {
        return cond56(registers) ? n442(registers) : n416(registers);
    }
    private static RuleResult n442(Registers registers) {
        return cond41(registers) ? n443(registers) : result170(registers);
    }
    private static RuleResult n443(Registers registers) {
        return cond50(registers) ? n444(registers) : n473(registers);
    }
    private static RuleResult n444(Registers registers) {
        return cond51(registers) ? result170(registers) : n445(registers);
    }
    private static RuleResult n445(Registers registers) {
        return cond57(registers) ? n478(registers) : n970(registers);
    }
    private static RuleResult n416(Registers registers) {
        return cond41(registers) ? n417(registers) : result170(registers);
    }
    private static RuleResult n417(Registers registers) {
        return cond50(registers) ? n418(registers) : n473(registers);
    }
    private static RuleResult n418(Registers registers) {
        return cond51(registers) ? result170(registers) : n419(registers);
    }
    private static RuleResult n419(Registers registers) {
        return cond57(registers) ? n478(registers) : n420(registers);
    }
    private static RuleResult n420(Registers registers) {
        return cond78(registers) ? n421(registers) : n949(registers);
    }
    private static RuleResult n421(Registers registers) {
        return cond58(registers) ? n432(registers) : n422(registers);
    }
    private static RuleResult n432(Registers registers) {
        // this is a subgraph: [433, 434, 435, 436, 437, 438, 439, 440, 441]
        if (cond49(registers)) {
            if (cond52(registers)) {
                return result142(registers);
            } else {
                if (cond39(registers)) {
                    if (cond42(registers)) {
                        if (cond61(registers)) {
                            if (cond62(registers)) {
                                if (cond75(registers)) {
                                    if (cond71(registers)) {
                                        return result132(registers);
                                    } else {
                                        if (cond76(registers)) {
                                            if (cond73(registers)) {
                                                return result133(registers);
                                            } else {
                                                return result136(registers);
                                            }
                                        } else {
                                            return result137(registers);
                                        }
                                    }
                                } else {
                                    return result138(registers);
                                }
                            } else {
                                return result139(registers);
                            }
                        } else {
                            return result131(registers);
                        }
                    } else {
                        if (cond62(registers)) {
                            if (cond75(registers)) {
                                if (cond71(registers)) {
                                    return result132(registers);
                                } else {
                                    if (cond76(registers)) {
                                        if (cond73(registers)) {
                                            return result133(registers);
                                        } else {
                                            return result136(registers);
                                        }
                                    } else {
                                        return result137(registers);
                                    }
                                }
                            } else {
                                return result138(registers);
                            }
                        } else {
                            return result139(registers);
                        }
                    }
                } else {
                    if (cond62(registers)) {
                        if (cond75(registers)) {
                            if (cond71(registers)) {
                                return result132(registers);
                            } else {
                                if (cond76(registers)) {
                                    if (cond73(registers)) {
                                        return result133(registers);
                                    } else {
                                        return result136(registers);
                                    }
                                } else {
                                    return result137(registers);
                                }
                            }
                        } else {
                            return result138(registers);
                        }
                    } else {
                        return result139(registers);
                    }
                }
            }
        } else {
            return result142(registers);
        }
    }
    private static RuleResult n422(Registers registers) {
        // this is a subgraph: [423, 424, 425, 426, 427, 428, 429, 430, 431]
        if (cond49(registers)) {
            if (cond52(registers)) {
                return result142(registers);
            } else {
                if (cond39(registers)) {
                    if (cond42(registers)) {
                        if (cond61(registers)) {
                            if (cond62(registers)) {
                                if (cond75(registers)) {
                                    if (cond72(registers)) {
                                        if (cond76(registers)) {
                                            if (cond73(registers)) {
                                                return result148(registers);
                                            } else {
                                                return result136(registers);
                                            }
                                        } else {
                                            return result137(registers);
                                        }
                                    } else {
                                        return result150(registers);
                                    }
                                } else {
                                    return result138(registers);
                                }
                            } else {
                                return result139(registers);
                            }
                        } else {
                            return result131(registers);
                        }
                    } else {
                        if (cond62(registers)) {
                            if (cond75(registers)) {
                                if (cond72(registers)) {
                                    if (cond76(registers)) {
                                        if (cond73(registers)) {
                                            return result148(registers);
                                        } else {
                                            return result136(registers);
                                        }
                                    } else {
                                        return result137(registers);
                                    }
                                } else {
                                    return result150(registers);
                                }
                            } else {
                                return result138(registers);
                            }
                        } else {
                            return result139(registers);
                        }
                    }
                } else {
                    if (cond62(registers)) {
                        if (cond75(registers)) {
                            if (cond72(registers)) {
                                if (cond76(registers)) {
                                    if (cond73(registers)) {
                                        return result148(registers);
                                    } else {
                                        return result136(registers);
                                    }
                                } else {
                                    return result137(registers);
                                }
                            } else {
                                return result150(registers);
                            }
                        } else {
                            return result138(registers);
                        }
                    } else {
                        return result139(registers);
                    }
                }
            }
        } else {
            return result142(registers);
        }
    }
    private static RuleResult n410(Registers registers) {
        return cond56(registers) ? n413(registers) : n411(registers);
    }
    private static RuleResult n413(Registers registers) {
        return cond41(registers) ? n414(registers) : result170(registers);
    }
    private static RuleResult n414(Registers registers) {
        return cond50(registers) ? n444(registers) : n449(registers);
    }
    private static RuleResult n411(Registers registers) {
        return cond41(registers) ? n412(registers) : result170(registers);
    }
    private static RuleResult n412(Registers registers) {
        return cond50(registers) ? n418(registers) : n449(registers);
    }
    private static RuleResult n312(Registers registers) {
        return cond10(registers) ? n379(registers) : n313(registers);
    }
    private static RuleResult n379(Registers registers) {
        return cond11(registers) ? n391(registers) : n380(registers);
    }
    private static RuleResult n391(Registers registers) {
        return cond21(registers) ? n392(registers) : n524(registers);
    }
    private static RuleResult n392(Registers registers) {
        return cond22(registers) ? n393(registers) : n524(registers);
    }
    private static RuleResult n393(Registers registers) {
        return cond23(registers) ? n394(registers) : n524(registers);
    }
    private static RuleResult n394(Registers registers) {
        return cond16(registers) ? n395(registers) : n524(registers);
    }
    private static RuleResult n395(Registers registers) {
        return cond24(registers) ? n526(registers) : n524(registers);
    }
    private static RuleResult n380(Registers registers) {
        return cond21(registers) ? n381(registers) : n385(registers);
    }
    private static RuleResult n381(Registers registers) {
        return cond22(registers) ? n382(registers) : n385(registers);
    }
    private static RuleResult n382(Registers registers) {
        return cond23(registers) ? n383(registers) : n385(registers);
    }
    private static RuleResult n383(Registers registers) {
        return cond16(registers) ? n384(registers) : n385(registers);
    }
    private static RuleResult n384(Registers registers) {
        return cond24(registers) ? n526(registers) : n385(registers);
    }
    private static RuleResult n385(Registers registers) {
        return cond37(registers) ? n390(registers) : n386(registers);
    }
    private static RuleResult n390(Registers registers) {
        return cond27(registers) ? result1(registers) : result184(registers);
    }
    private static RuleResult n386(Registers registers) {
        return cond15(registers) ? n387(registers) : n513(registers);
    }
    private static RuleResult n387(Registers registers) {
        return cond17(registers) ? n388(registers) : n513(registers);
    }
    private static RuleResult n388(Registers registers) {
        return cond33(registers) ? n389(registers) : n515(registers);
    }
    private static RuleResult n389(Registers registers) {
        return cond27(registers) ? result1(registers) : result187(registers);
    }
    private static RuleResult n313(Registers registers) {
        return cond12(registers) ? n374(registers) : n314(registers);
    }
    private static RuleResult n374(Registers registers) {
        return cond21(registers) ? n375(registers) : n515(registers);
    }
    private static RuleResult n375(Registers registers) {
        return cond22(registers) ? n376(registers) : n515(registers);
    }
    private static RuleResult n376(Registers registers) {
        return cond23(registers) ? n377(registers) : n515(registers);
    }
    private static RuleResult n377(Registers registers) {
        return cond16(registers) ? n378(registers) : n515(registers);
    }
    private static RuleResult n378(Registers registers) {
        return cond24(registers) ? n526(registers) : n515(registers);
    }
    private static RuleResult n314(Registers registers) {
        return cond36(registers) ? n315(registers) : n380(registers);
    }
    private static RuleResult n315(Registers registers) {
        return cond21(registers) ? n316(registers) : n320(registers);
    }
    private static RuleResult n316(Registers registers) {
        return cond22(registers) ? n317(registers) : n320(registers);
    }
    private static RuleResult n317(Registers registers) {
        return cond23(registers) ? n318(registers) : n320(registers);
    }
    private static RuleResult n318(Registers registers) {
        return cond16(registers) ? n319(registers) : n320(registers);
    }
    private static RuleResult n319(Registers registers) {
        return cond24(registers) ? n526(registers) : n320(registers);
    }
    private static RuleResult n320(Registers registers) {
        return cond27(registers) ? result1(registers) : n321(registers);
    }
    private static RuleResult n321(Registers registers) {
        return cond26(registers) ? n322(registers) : n323(registers);
    }
    private static RuleResult n322(Registers registers) {
        return cond40(registers) ? n356(registers) : n323(registers);
    }
    private static RuleResult n356(Registers registers) {
        return cond48(registers) ? n471(registers) : n357(registers);
    }
    private static RuleResult n357(Registers registers) {
        return cond41(registers) ? n358(registers) : result170(registers);
    }
    private static RuleResult n358(Registers registers) {
        return cond50(registers) ? n476(registers) : n359(registers);
    }
    private static RuleResult n359(Registers registers) {
        return cond51(registers) ? result170(registers) : n360(registers);
    }
    private static RuleResult n360(Registers registers) {
        return cond78(registers) ? n361(registers) : n451(registers);
    }
    private static RuleResult n361(Registers registers) {
        // this is a subgraph: [362, 363, 364, 365, 366, 367, 368, 369, 370, 371, 372, 373]
        if (cond58(registers)) {
            return result143(registers);
        } else {
            if (cond59(registers)) {
                if (cond43(registers)) {
                    if (cond60(registers)) {
                        if (cond39(registers)) {
                            if (cond42(registers)) {
                                if (cond61(registers)) {
                                    if (cond62(registers)) {
                                        if (cond75(registers)) {
                                            if (cond76(registers)) {
                                                if (cond65(registers)) {
                                                    if (cond66(registers)) {
                                                        if (cond67(registers)) {
                                                            return result163(registers);
                                                        } else {
                                                            return result164(registers);
                                                        }
                                                    } else {
                                                        return result165(registers);
                                                    }
                                                } else {
                                                    return result166(registers);
                                                }
                                            } else {
                                                return result137(registers);
                                            }
                                        } else {
                                            return result138(registers);
                                        }
                                    } else {
                                        return result139(registers);
                                    }
                                } else {
                                    return result131(registers);
                                }
                            } else {
                                if (cond62(registers)) {
                                    if (cond75(registers)) {
                                        if (cond76(registers)) {
                                            if (cond65(registers)) {
                                                if (cond66(registers)) {
                                                    if (cond67(registers)) {
                                                        return result163(registers);
                                                    } else {
                                                        return result164(registers);
                                                    }
                                                } else {
                                                    return result165(registers);
                                                }
                                            } else {
                                                return result166(registers);
                                            }
                                        } else {
                                            return result137(registers);
                                        }
                                    } else {
                                        return result138(registers);
                                    }
                                } else {
                                    return result139(registers);
                                }
                            }
                        } else {
                            if (cond62(registers)) {
                                if (cond75(registers)) {
                                    if (cond76(registers)) {
                                        if (cond65(registers)) {
                                            if (cond66(registers)) {
                                                if (cond67(registers)) {
                                                    return result163(registers);
                                                } else {
                                                    return result164(registers);
                                                }
                                            } else {
                                                return result165(registers);
                                            }
                                        } else {
                                            return result166(registers);
                                        }
                                    } else {
                                        return result137(registers);
                                    }
                                } else {
                                    return result138(registers);
                                }
                            } else {
                                return result139(registers);
                            }
                        }
                    } else {
                        return result167(registers);
                    }
                } else {
                    return result168(registers);
                }
            } else {
                return result169(registers);
            }
        }
    }
    private static RuleResult n323(Registers registers) {
        return cond48(registers) ? n329(registers) : n324(registers);
    }
    private static RuleResult n329(Registers registers) {
        return cond56(registers) ? n442(registers) : n330(registers);
    }
    private static RuleResult n330(Registers registers) {
        return cond41(registers) ? n331(registers) : result170(registers);
    }
    private static RuleResult n331(Registers registers) {
        return cond50(registers) ? n332(registers) : n473(registers);
    }
    private static RuleResult n332(Registers registers) {
        return cond51(registers) ? result170(registers) : n333(registers);
    }
    private static RuleResult n333(Registers registers) {
        return cond57(registers) ? n478(registers) : n334(registers);
    }
    private static RuleResult n334(Registers registers) {
        return cond78(registers) ? n335(registers) : n949(registers);
    }
    private static RuleResult n335(Registers registers) {
        return cond58(registers) ? n346(registers) : n336(registers);
    }
    private static RuleResult n346(Registers registers) {
        // this is a subgraph: [347, 348, 349, 350, 351, 352, 353, 354, 355]
        if (cond49(registers)) {
            if (cond52(registers)) {
                return result142(registers);
            } else {
                if (cond39(registers)) {
                    if (cond42(registers)) {
                        if (cond61(registers)) {
                            if (cond62(registers)) {
                                if (cond75(registers)) {
                                    if (cond71(registers)) {
                                        return result132(registers);
                                    } else {
                                        if (cond76(registers)) {
                                            if (cond73(registers)) {
                                                return result135(registers);
                                            } else {
                                                return result136(registers);
                                            }
                                        } else {
                                            return result137(registers);
                                        }
                                    }
                                } else {
                                    return result138(registers);
                                }
                            } else {
                                return result139(registers);
                            }
                        } else {
                            return result131(registers);
                        }
                    } else {
                        if (cond62(registers)) {
                            if (cond75(registers)) {
                                if (cond71(registers)) {
                                    return result132(registers);
                                } else {
                                    if (cond76(registers)) {
                                        if (cond73(registers)) {
                                            return result135(registers);
                                        } else {
                                            return result136(registers);
                                        }
                                    } else {
                                        return result137(registers);
                                    }
                                }
                            } else {
                                return result138(registers);
                            }
                        } else {
                            return result139(registers);
                        }
                    }
                } else {
                    if (cond62(registers)) {
                        if (cond75(registers)) {
                            if (cond71(registers)) {
                                return result132(registers);
                            } else {
                                if (cond76(registers)) {
                                    if (cond73(registers)) {
                                        return result135(registers);
                                    } else {
                                        return result136(registers);
                                    }
                                } else {
                                    return result137(registers);
                                }
                            }
                        } else {
                            return result138(registers);
                        }
                    } else {
                        return result139(registers);
                    }
                }
            }
        } else {
            return result142(registers);
        }
    }
    private static RuleResult n336(Registers registers) {
        // this is a subgraph: [337, 338, 339, 340, 341, 342, 343, 344, 345]
        if (cond49(registers)) {
            if (cond52(registers)) {
                return result142(registers);
            } else {
                if (cond39(registers)) {
                    if (cond42(registers)) {
                        if (cond61(registers)) {
                            if (cond62(registers)) {
                                if (cond75(registers)) {
                                    if (cond72(registers)) {
                                        if (cond76(registers)) {
                                            if (cond73(registers)) {
                                                return result149(registers);
                                            } else {
                                                return result136(registers);
                                            }
                                        } else {
                                            return result137(registers);
                                        }
                                    } else {
                                        return result150(registers);
                                    }
                                } else {
                                    return result138(registers);
                                }
                            } else {
                                return result139(registers);
                            }
                        } else {
                            return result131(registers);
                        }
                    } else {
                        if (cond62(registers)) {
                            if (cond75(registers)) {
                                if (cond72(registers)) {
                                    if (cond76(registers)) {
                                        if (cond73(registers)) {
                                            return result149(registers);
                                        } else {
                                            return result136(registers);
                                        }
                                    } else {
                                        return result137(registers);
                                    }
                                } else {
                                    return result150(registers);
                                }
                            } else {
                                return result138(registers);
                            }
                        } else {
                            return result139(registers);
                        }
                    }
                } else {
                    if (cond62(registers)) {
                        if (cond75(registers)) {
                            if (cond72(registers)) {
                                if (cond76(registers)) {
                                    if (cond73(registers)) {
                                        return result149(registers);
                                    } else {
                                        return result136(registers);
                                    }
                                } else {
                                    return result137(registers);
                                }
                            } else {
                                return result150(registers);
                            }
                        } else {
                            return result138(registers);
                        }
                    } else {
                        return result139(registers);
                    }
                }
            }
        } else {
            return result142(registers);
        }
    }
    private static RuleResult n324(Registers registers) {
        return cond56(registers) ? n327(registers) : n325(registers);
    }
    private static RuleResult n327(Registers registers) {
        return cond41(registers) ? n328(registers) : result170(registers);
    }
    private static RuleResult n328(Registers registers) {
        return cond50(registers) ? n444(registers) : n359(registers);
    }
    private static RuleResult n325(Registers registers) {
        return cond41(registers) ? n326(registers) : result170(registers);
    }
    private static RuleResult n326(Registers registers) {
        return cond50(registers) ? n332(registers) : n359(registers);
    }
    private static RuleResult n299(Registers registers) {
        return cond38(registers) ? n305(registers) : n300(registers);
    }
    private static RuleResult n305(Registers registers) {
        return cond21(registers) ? n306(registers) : n310(registers);
    }
    private static RuleResult n306(Registers registers) {
        return cond22(registers) ? n307(registers) : n310(registers);
    }
    private static RuleResult n307(Registers registers) {
        return cond23(registers) ? n308(registers) : n310(registers);
    }
    private static RuleResult n308(Registers registers) {
        return cond16(registers) ? n309(registers) : n310(registers);
    }
    private static RuleResult n309(Registers registers) {
        return cond24(registers) ? n525(registers) : n310(registers);
    }
    private static RuleResult n310(Registers registers) {
        return cond27(registers) ? result1(registers) : result108(registers);
    }
    private static RuleResult n300(Registers registers) {
        return cond21(registers) ? n301(registers) : n310(registers);
    }
    private static RuleResult n301(Registers registers) {
        return cond22(registers) ? n302(registers) : n310(registers);
    }
    private static RuleResult n302(Registers registers) {
        return cond23(registers) ? n303(registers) : n310(registers);
    }
    private static RuleResult n303(Registers registers) {
        return cond16(registers) ? n304(registers) : n310(registers);
    }
    private static RuleResult n304(Registers registers) {
        return cond24(registers) ? n526(registers) : n310(registers);
    }
    private static RuleResult n272(Registers registers) {
        return cond79(registers) ? n544(registers) : n273(registers);
    }
    private static RuleResult n273(Registers registers) {
        return cond80(registers) ? n531(registers) : n274(registers);
    }
    private static RuleResult n274(Registers registers) {
        return cond81(registers) ? n275(registers) : n298(registers);
    }
    private static RuleResult n275(Registers registers) {
        return cond90(registers) ? n276(registers) : n299(registers);
    }
    private static RuleResult n276(Registers registers) {
        return cond38(registers) ? n285(registers) : n277(registers);
    }
    private static RuleResult n285(Registers registers) {
        return cond10(registers) ? n290(registers) : n286(registers);
    }
    private static RuleResult n290(Registers registers) {
        return cond21(registers) ? n291(registers) : n295(registers);
    }
    private static RuleResult n291(Registers registers) {
        return cond22(registers) ? n292(registers) : n295(registers);
    }
    private static RuleResult n292(Registers registers) {
        return cond23(registers) ? n293(registers) : n295(registers);
    }
    private static RuleResult n293(Registers registers) {
        return cond16(registers) ? n294(registers) : n295(registers);
    }
    private static RuleResult n294(Registers registers) {
        return cond24(registers) ? n525(registers) : n295(registers);
    }
    private static RuleResult n295(Registers registers) {
        return cond27(registers) ? result1(registers) : result171(registers);
    }
    private static RuleResult n286(Registers registers) {
        return cond12(registers) ? n492(registers) : n287(registers);
    }
    private static RuleResult n287(Registers registers) {
        return cond13(registers) ? n288(registers) : n289(registers);
    }
    private static RuleResult n288(Registers registers) {
        return cond14(registers) ? n485(registers) : n289(registers);
    }
    private static RuleResult n289(Registers registers) {
        return cond36(registers) ? n401(registers) : n290(registers);
    }
    private static RuleResult n277(Registers registers) {
        return cond10(registers) ? n280(registers) : n278(registers);
    }
    private static RuleResult n280(Registers registers) {
        return cond21(registers) ? n281(registers) : n295(registers);
    }
    private static RuleResult n281(Registers registers) {
        return cond22(registers) ? n282(registers) : n295(registers);
    }
    private static RuleResult n282(Registers registers) {
        return cond23(registers) ? n283(registers) : n295(registers);
    }
    private static RuleResult n283(Registers registers) {
        return cond16(registers) ? n284(registers) : n295(registers);
    }
    private static RuleResult n284(Registers registers) {
        return cond24(registers) ? n526(registers) : n295(registers);
    }
    private static RuleResult n278(Registers registers) {
        return cond12(registers) ? n374(registers) : n279(registers);
    }
    private static RuleResult n279(Registers registers) {
        return cond36(registers) ? n315(registers) : n280(registers);
    }
    private static RuleResult n231(Registers registers) {
        return cond20(registers) ? n234(registers) : n232(registers);
    }
    private static RuleResult n234(Registers registers) {
        return cond79(registers) ? n249(registers) : n235(registers);
    }
    private static RuleResult n249(Registers registers) {
        return cond38(registers) ? n264(registers) : n250(registers);
    }
    private static RuleResult n264(Registers registers) {
        return cond12(registers) ? n267(registers) : n265(registers);
    }
    private static RuleResult n267(Registers registers) {
        return cond37(registers) ? n268(registers) : n269(registers);
    }
    private static RuleResult n268(Registers registers) {
        return cond25(registers) ? n270(registers) : n269(registers);
    }
    private static RuleResult n270(Registers registers) {
        return cond27(registers) ? result1(registers) : result9(registers);
    }
    private static RuleResult n269(Registers registers) {
        return cond27(registers) ? result1(registers) : result10(registers);
    }
    private static RuleResult n265(Registers registers) {
        return cond37(registers) ? n266(registers) : n562(registers);
    }
    private static RuleResult n266(Registers registers) {
        return cond25(registers) ? n270(registers) : n562(registers);
    }
    private static RuleResult n250(Registers registers) {
        return cond12(registers) ? n251(registers) : n562(registers);
    }
    private static RuleResult n251(Registers registers) {
        // this is a subgraph: [252, 253, 262, 263, 254, 255, 256, 257, 258, 259, 260, 261]
        if (cond27(registers)) {
            return result1(registers);
        } else {
            if (cond82(registers)) {
                if (cond31(registers)) {
                    if (cond84(registers)) {
                        if (cond44(registers)) {
                            return result39(registers);
                        } else {
                            return result43(registers);
                        }
                    } else {
                        return result43(registers);
                    }
                } else {
                    if (cond84(registers)) {
                        if (cond44(registers)) {
                            return result39(registers);
                        } else {
                            if (cond85(registers)) {
                                if (cond46(registers)) {
                                    return result47(registers);
                                } else {
                                    if (cond47(registers)) {
                                        if (cond86(registers)) {
                                            return result51(registers);
                                        } else {
                                            if (cond54(registers)) {
                                                if (cond88(registers)) {
                                                    return result55(registers);
                                                } else {
                                                    return result35(registers);
                                                }
                                            } else {
                                                return result35(registers);
                                            }
                                        }
                                    } else {
                                        if (cond54(registers)) {
                                            if (cond88(registers)) {
                                                return result55(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                }
                            } else {
                                if (cond47(registers)) {
                                    if (cond86(registers)) {
                                        return result51(registers);
                                    } else {
                                        if (cond54(registers)) {
                                            if (cond88(registers)) {
                                                return result55(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result55(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        }
                    } else {
                        if (cond85(registers)) {
                            if (cond46(registers)) {
                                return result47(registers);
                            } else {
                                if (cond47(registers)) {
                                    if (cond86(registers)) {
                                        return result51(registers);
                                    } else {
                                        if (cond54(registers)) {
                                            if (cond88(registers)) {
                                                return result55(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result55(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result51(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result55(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result55(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                }
            } else {
                if (cond84(registers)) {
                    if (cond44(registers)) {
                        return result39(registers);
                    } else {
                        if (cond85(registers)) {
                            if (cond46(registers)) {
                                return result47(registers);
                            } else {
                                if (cond47(registers)) {
                                    if (cond86(registers)) {
                                        return result51(registers);
                                    } else {
                                        if (cond54(registers)) {
                                            if (cond88(registers)) {
                                                return result55(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result55(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result51(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result55(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result55(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                } else {
                    if (cond85(registers)) {
                        if (cond46(registers)) {
                            return result47(registers);
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result51(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result55(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result55(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result51(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result55(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result55(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            }
        }
    }
    private static RuleResult n235(Registers registers) {
        return cond80(registers) ? n236(registers) : n298(registers);
    }
    private static RuleResult n236(Registers registers) {
        return cond38(registers) ? n264(registers) : n237(registers);
    }
    private static RuleResult n237(Registers registers) {
        return cond12(registers) ? n238(registers) : n562(registers);
    }
    private static RuleResult n238(Registers registers) {
        // this is a subgraph: [239, 240, 241, 242, 243, 244, 245, 246, 247, 248]
        if (cond27(registers)) {
            return result1(registers);
        } else {
            if (cond30(registers)) {
                if (cond82(registers)) {
                    return result79(registers);
                } else {
                    if (cond83(registers)) {
                        if (cond32(registers)) {
                            return result83(registers);
                        } else {
                            if (cond45(registers)) {
                                if (cond86(registers)) {
                                    return result87(registers);
                                } else {
                                    if (cond87(registers)) {
                                        if (cond53(registers)) {
                                            return result91(registers);
                                        } else {
                                            if (cond55(registers)) {
                                                if (cond89(registers)) {
                                                    return result95(registers);
                                                } else {
                                                    return result35(registers);
                                                }
                                            } else {
                                                return result35(registers);
                                            }
                                        }
                                    } else {
                                        if (cond55(registers)) {
                                            if (cond89(registers)) {
                                                return result95(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                }
                            } else {
                                if (cond87(registers)) {
                                    if (cond53(registers)) {
                                        return result91(registers);
                                    } else {
                                        if (cond55(registers)) {
                                            if (cond89(registers)) {
                                                return result95(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result95(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        }
                    } else {
                        if (cond45(registers)) {
                            if (cond86(registers)) {
                                return result87(registers);
                            } else {
                                if (cond87(registers)) {
                                    if (cond53(registers)) {
                                        return result91(registers);
                                    } else {
                                        if (cond55(registers)) {
                                            if (cond89(registers)) {
                                                return result95(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result95(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result91(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result95(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result95(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                }
            } else {
                if (cond83(registers)) {
                    if (cond32(registers)) {
                        return result83(registers);
                    } else {
                        if (cond45(registers)) {
                            if (cond86(registers)) {
                                return result87(registers);
                            } else {
                                if (cond87(registers)) {
                                    if (cond53(registers)) {
                                        return result91(registers);
                                    } else {
                                        if (cond55(registers)) {
                                            if (cond89(registers)) {
                                                return result95(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result95(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result91(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result95(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result95(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                } else {
                    if (cond45(registers)) {
                        if (cond86(registers)) {
                            return result87(registers);
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result91(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result95(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result95(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result91(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result95(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result95(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            }
        }
    }
    private static RuleResult n232(Registers registers) {
        return cond79(registers) ? n249(registers) : n233(registers);
    }
    private static RuleResult n233(Registers registers) {
        return cond80(registers) ? n236(registers) : n274(registers);
    }
    private static RuleResult n177(Registers registers) {
        return cond7(registers) ? n178(registers) : n179(registers);
    }
    private static RuleResult n178(Registers registers) {
        return cond9(registers) ? n185(registers) : n179(registers);
    }
    private static RuleResult n185(Registers registers) {
        return cond20(registers) ? n192(registers) : n186(registers);
    }
    private static RuleResult n192(Registers registers) {
        return cond79(registers) ? n228(registers) : n193(registers);
    }
    private static RuleResult n228(Registers registers) {
        return cond38(registers) ? n559(registers) : n562(registers);
    }
    private static RuleResult n193(Registers registers) {
        return cond80(registers) ? n228(registers) : n194(registers);
    }
    private static RuleResult n194(Registers registers) {
        return cond90(registers) ? n195(registers) : n310(registers);
    }
    private static RuleResult n195(Registers registers) {
        return cond10(registers) ? n227(registers) : n196(registers);
    }
    private static RuleResult n227(Registers registers) {
        return cond11(registers) ? n524(registers) : n513(registers);
    }
    private static RuleResult n196(Registers registers) {
        return cond36(registers) ? n197(registers) : n513(registers);
    }
    private static RuleResult n197(Registers registers) {
        return cond27(registers) ? result1(registers) : n198(registers);
    }
    private static RuleResult n198(Registers registers) {
        return cond26(registers) ? n199(registers) : n200(registers);
    }
    private static RuleResult n199(Registers registers) {
        return cond40(registers) ? n215(registers) : n200(registers);
    }
    private static RuleResult n215(Registers registers) {
        return cond48(registers) ? n219(registers) : n216(registers);
    }
    private static RuleResult n219(Registers registers) {
        return cond41(registers) ? n220(registers) : result170(registers);
    }
    private static RuleResult n220(Registers registers) {
        return cond50(registers) ? n221(registers) : n473(registers);
    }
    private static RuleResult n221(Registers registers) {
        return cond51(registers) ? result170(registers) : n222(registers);
    }
    private static RuleResult n222(Registers registers) {
        return cond57(registers) ? n223(registers) : n980(registers);
    }
    private static RuleResult n223(Registers registers) {
        return cond58(registers) ? n730(registers) : n224(registers);
    }
    private static RuleResult n224(Registers registers) {
        return cond49(registers) ? n225(registers) : result142(registers);
    }
    private static RuleResult n225(Registers registers) {
        return cond52(registers) ? result142(registers) : n226(registers);
    }
    private static RuleResult n226(Registers registers) {
        return cond63(registers) ? n484(registers) : result157(registers);
    }
    private static RuleResult n484(Registers registers) {
        return cond74(registers) ? result154(registers) : result157(registers);
    }
    private static RuleResult n216(Registers registers) {
        return cond41(registers) ? n217(registers) : result170(registers);
    }
    private static RuleResult n217(Registers registers) {
        return cond50(registers) ? n221(registers) : n218(registers);
    }
    private static RuleResult n218(Registers registers) {
        return cond51(registers) ? result170(registers) : n451(registers);
    }
    private static RuleResult n200(Registers registers) {
        return cond48(registers) ? n206(registers) : n201(registers);
    }
    private static RuleResult n206(Registers registers) {
        return cond56(registers) ? n211(registers) : n207(registers);
    }
    private static RuleResult n211(Registers registers) {
        return cond41(registers) ? n212(registers) : result170(registers);
    }
    private static RuleResult n212(Registers registers) {
        return cond50(registers) ? n213(registers) : n473(registers);
    }
    private static RuleResult n213(Registers registers) {
        return cond51(registers) ? result170(registers) : n214(registers);
    }
    private static RuleResult n214(Registers registers) {
        return cond57(registers) ? n223(registers) : n970(registers);
    }
    private static RuleResult n207(Registers registers) {
        return cond41(registers) ? n208(registers) : result170(registers);
    }
    private static RuleResult n208(Registers registers) {
        return cond50(registers) ? n209(registers) : n473(registers);
    }
    private static RuleResult n209(Registers registers) {
        return cond51(registers) ? result170(registers) : n210(registers);
    }
    private static RuleResult n210(Registers registers) {
        return cond57(registers) ? n223(registers) : n949(registers);
    }
    private static RuleResult n201(Registers registers) {
        return cond56(registers) ? n204(registers) : n202(registers);
    }
    private static RuleResult n204(Registers registers) {
        return cond41(registers) ? n205(registers) : result170(registers);
    }
    private static RuleResult n205(Registers registers) {
        return cond50(registers) ? n213(registers) : n218(registers);
    }
    private static RuleResult n202(Registers registers) {
        return cond41(registers) ? n203(registers) : result170(registers);
    }
    private static RuleResult n203(Registers registers) {
        return cond50(registers) ? n209(registers) : n218(registers);
    }
    private static RuleResult n186(Registers registers) {
        return cond79(registers) ? n228(registers) : n187(registers);
    }
    private static RuleResult n187(Registers registers) {
        return cond80(registers) ? n228(registers) : n188(registers);
    }
    private static RuleResult n188(Registers registers) {
        return cond81(registers) ? n189(registers) : n194(registers);
    }
    private static RuleResult n189(Registers registers) {
        return cond90(registers) ? n190(registers) : n310(registers);
    }
    private static RuleResult n190(Registers registers) {
        return cond10(registers) ? n295(registers) : n191(registers);
    }
    private static RuleResult n191(Registers registers) {
        return cond36(registers) ? n197(registers) : n295(registers);
    }
    private static RuleResult n179(Registers registers) {
        return cond20(registers) ? n182(registers) : n180(registers);
    }
    private static RuleResult n182(Registers registers) {
        return cond79(registers) ? n184(registers) : n183(registers);
    }
    private static RuleResult n184(Registers registers) {
        return cond38(registers) ? n264(registers) : n562(registers);
    }
    private static RuleResult n183(Registers registers) {
        return cond80(registers) ? n184(registers) : n194(registers);
    }
    private static RuleResult n180(Registers registers) {
        return cond79(registers) ? n184(registers) : n181(registers);
    }
    private static RuleResult n181(Registers registers) {
        return cond80(registers) ? n184(registers) : n188(registers);
    }
    private static RuleResult n162(Registers registers) {
        return cond19(registers) ? n163(registers) : n513(registers);
    }
    private static RuleResult n163(Registers registers) {
        return cond3(registers) ? n164(registers) : n165(registers);
    }
    private static RuleResult n164(Registers registers) {
        return cond8(registers) ? n173(registers) : n165(registers);
    }
    private static RuleResult n173(Registers registers) {
        return cond38(registers) ? n175(registers) : n174(registers);
    }
    private static RuleResult n175(Registers registers) {
        return cond27(registers) ? result1(registers) : result96(registers);
    }
    private static RuleResult n174(Registers registers) {
        return cond27(registers) ? result1(registers) : result100(registers);
    }
    private static RuleResult n165(Registers registers) {
        return cond38(registers) ? n168(registers) : n166(registers);
    }
    private static RuleResult n168(Registers registers) {
        return cond15(registers) ? n169(registers) : n170(registers);
    }
    private static RuleResult n169(Registers registers) {
        return cond17(registers) ? n514(registers) : n170(registers);
    }
    private static RuleResult n170(Registers registers) {
        return cond33(registers) ? n171(registers) : n515(registers);
    }
    private static RuleResult n171(Registers registers) {
        return cond27(registers) ? result1(registers) : n172(registers);
    }
    private static RuleResult n172(Registers registers) {
        return cond68(registers) ? result194(registers) : result195(registers);
    }
    private static RuleResult n166(Registers registers) {
        return cond15(registers) ? n167(registers) : n515(registers);
    }
    private static RuleResult n167(Registers registers) {
        return cond17(registers) ? n388(registers) : n515(registers);
    }
    private static RuleResult n4(Registers registers) {
        return cond2(registers) ? n10(registers) : n5(registers);
    }
    private static RuleResult n10(Registers registers) {
        return cond19(registers) ? n22(registers) : n11(registers);
    }
    private static RuleResult n22(Registers registers) {
        return cond3(registers) ? n23(registers) : n24(registers);
    }
    private static RuleResult n23(Registers registers) {
        return cond8(registers) ? n34(registers) : n24(registers);
    }
    private static RuleResult n34(Registers registers) {
        return cond7(registers) ? n35(registers) : n36(registers);
    }
    private static RuleResult n35(Registers registers) {
        return cond9(registers) ? n68(registers) : n36(registers);
    }
    private static RuleResult n68(Registers registers) {
        return cond20(registers) ? n75(registers) : n69(registers);
    }
    private static RuleResult n75(Registers registers) {
        return cond79(registers) ? n144(registers) : n76(registers);
    }
    private static RuleResult n144(Registers registers) {
        return cond12(registers) ? n146(registers) : n145(registers);
    }
    private static RuleResult n146(Registers registers) {
        return cond37(registers) ? n160(registers) : n147(registers);
    }
    private static RuleResult n160(Registers registers) {
        return cond27(registers) ? result13(registers) : result14(registers);
    }
    private static RuleResult n147(Registers registers) {
        return cond27(registers) ? n148(registers) : n547(registers);
    }
    private static RuleResult n148(Registers registers) {
        // this is a subgraph: [149, 158, 159, 150, 151, 152, 153, 154, 155, 156, 157]
        if (cond82(registers)) {
            if (cond31(registers)) {
                if (cond84(registers)) {
                    if (cond44(registers)) {
                        return result17(registers);
                    } else {
                        return result21(registers);
                    }
                } else {
                    return result21(registers);
                }
            } else {
                if (cond84(registers)) {
                    if (cond44(registers)) {
                        return result17(registers);
                    } else {
                        if (cond85(registers)) {
                            if (cond46(registers)) {
                                return result25(registers);
                            } else {
                                if (cond47(registers)) {
                                    if (cond86(registers)) {
                                        return result29(registers);
                                    } else {
                                        if (cond54(registers)) {
                                            if (cond88(registers)) {
                                                return result33(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result33(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result29(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result33(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result33(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                } else {
                    if (cond85(registers)) {
                        if (cond46(registers)) {
                            return result25(registers);
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result29(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result33(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result33(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result29(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result33(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result33(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            }
        } else {
            if (cond84(registers)) {
                if (cond44(registers)) {
                    return result17(registers);
                } else {
                    if (cond85(registers)) {
                        if (cond46(registers)) {
                            return result25(registers);
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result29(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result33(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result33(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result29(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result33(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result33(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            } else {
                if (cond85(registers)) {
                    if (cond46(registers)) {
                        return result25(registers);
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result29(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result33(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result33(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                } else {
                    if (cond47(registers)) {
                        if (cond86(registers)) {
                            return result29(registers);
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result33(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    } else {
                        if (cond54(registers)) {
                            if (cond88(registers)) {
                                return result33(registers);
                            } else {
                                return result35(registers);
                            }
                        } else {
                            return result35(registers);
                        }
                    }
                }
            }
        }
    }
    private static RuleResult n547(Registers registers) {
        return cond82(registers) ? n548(registers) : n549(registers);
    }
    private static RuleResult n548(Registers registers) {
        return cond31(registers) ? n557(registers) : n549(registers);
    }
    private static RuleResult n557(Registers registers) {
        return cond84(registers) ? n558(registers) : result22(registers);
    }
    private static RuleResult n558(Registers registers) {
        return cond44(registers) ? result18(registers) : result22(registers);
    }
    private static RuleResult n549(Registers registers) {
        return cond84(registers) ? n550(registers) : n551(registers);
    }
    private static RuleResult n550(Registers registers) {
        return cond44(registers) ? result18(registers) : n551(registers);
    }
    private static RuleResult n551(Registers registers) {
        return cond85(registers) ? n552(registers) : n553(registers);
    }
    private static RuleResult n552(Registers registers) {
        return cond46(registers) ? result26(registers) : n553(registers);
    }
    private static RuleResult n553(Registers registers) {
        return cond47(registers) ? n554(registers) : n555(registers);
    }
    private static RuleResult n554(Registers registers) {
        return cond86(registers) ? result30(registers) : n555(registers);
    }
    private static RuleResult n555(Registers registers) {
        return cond54(registers) ? n556(registers) : result35(registers);
    }
    private static RuleResult n556(Registers registers) {
        return cond88(registers) ? result34(registers) : result35(registers);
    }
    private static RuleResult n145(Registers registers) {
        return cond37(registers) ? n160(registers) : result8(registers);
    }
    private static RuleResult n76(Registers registers) {
        return cond80(registers) ? n132(registers) : n77(registers);
    }
    private static RuleResult n132(Registers registers) {
        return cond12(registers) ? n133(registers) : result8(registers);
    }
    private static RuleResult n133(Registers registers) {
        return cond27(registers) ? n134(registers) : n534(registers);
    }
    private static RuleResult n134(Registers registers) {
        // this is a subgraph: [135, 136, 137, 138, 139, 140, 141, 142, 143]
        if (cond30(registers)) {
            if (cond82(registers)) {
                return result58(registers);
            } else {
                if (cond83(registers)) {
                    if (cond32(registers)) {
                        return result62(registers);
                    } else {
                        if (cond45(registers)) {
                            if (cond86(registers)) {
                                return result66(registers);
                            } else {
                                if (cond87(registers)) {
                                    if (cond53(registers)) {
                                        return result70(registers);
                                    } else {
                                        if (cond55(registers)) {
                                            if (cond89(registers)) {
                                                return result74(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result74(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result70(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result74(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result74(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                } else {
                    if (cond45(registers)) {
                        if (cond86(registers)) {
                            return result66(registers);
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result70(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result74(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result74(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result70(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result74(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result74(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            }
        } else {
            if (cond83(registers)) {
                if (cond32(registers)) {
                    return result62(registers);
                } else {
                    if (cond45(registers)) {
                        if (cond86(registers)) {
                            return result66(registers);
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result70(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result74(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result74(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result70(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result74(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result74(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            } else {
                if (cond45(registers)) {
                    if (cond86(registers)) {
                        return result66(registers);
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result70(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result74(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result74(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                } else {
                    if (cond87(registers)) {
                        if (cond53(registers)) {
                            return result70(registers);
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result74(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    } else {
                        if (cond55(registers)) {
                            if (cond89(registers)) {
                                return result74(registers);
                            } else {
                                return result35(registers);
                            }
                        } else {
                            return result35(registers);
                        }
                    }
                }
            }
        }
    }
    private static RuleResult n534(Registers registers) {
        return cond30(registers) ? n535(registers) : n536(registers);
    }
    private static RuleResult n535(Registers registers) {
        return cond82(registers) ? result59(registers) : n536(registers);
    }
    private static RuleResult n536(Registers registers) {
        return cond83(registers) ? n537(registers) : n538(registers);
    }
    private static RuleResult n537(Registers registers) {
        return cond32(registers) ? result63(registers) : n538(registers);
    }
    private static RuleResult n538(Registers registers) {
        return cond45(registers) ? n539(registers) : n540(registers);
    }
    private static RuleResult n539(Registers registers) {
        return cond86(registers) ? result67(registers) : n540(registers);
    }
    private static RuleResult n540(Registers registers) {
        return cond87(registers) ? n541(registers) : n542(registers);
    }
    private static RuleResult n541(Registers registers) {
        return cond53(registers) ? result71(registers) : n542(registers);
    }
    private static RuleResult n542(Registers registers) {
        return cond55(registers) ? n543(registers) : result35(registers);
    }
    private static RuleResult n543(Registers registers) {
        return cond89(registers) ? result75(registers) : result35(registers);
    }
    private static RuleResult n77(Registers registers) {
        return cond10(registers) ? n116(registers) : n78(registers);
    }
    private static RuleResult n116(Registers registers) {
        return cond11(registers) ? n1005(registers) : n117(registers);
    }
    private static RuleResult n117(Registers registers) {
        return cond21(registers) ? n118(registers) : n122(registers);
    }
    private static RuleResult n118(Registers registers) {
        return cond22(registers) ? n119(registers) : n122(registers);
    }
    private static RuleResult n119(Registers registers) {
        return cond23(registers) ? n120(registers) : n122(registers);
    }
    private static RuleResult n120(Registers registers) {
        return cond16(registers) ? n121(registers) : n122(registers);
    }
    private static RuleResult n121(Registers registers) {
        return cond24(registers) ? n1010(registers) : n122(registers);
    }
    private static RuleResult n122(Registers registers) {
        // this is a subgraph: [127, 131, 128, 129, 130, 123, 124, 125, 126]
        if (cond37(registers)) {
            if (cond27(registers)) {
                if (cond68(registers)) {
                    return result177(registers);
                } else {
                    return result178(registers);
                }
            } else {
                if (cond68(registers)) {
                    return result181(registers);
                } else {
                    if (cond69(registers)) {
                        if (cond64(registers)) {
                            return result182(registers);
                        } else {
                            return result183(registers);
                        }
                    } else {
                        return result183(registers);
                    }
                }
            }
        } else {
            if (cond15(registers)) {
                if (cond17(registers)) {
                    if (cond33(registers)) {
                        if (cond27(registers)) {
                            return result128(registers);
                        } else {
                            return result187(registers);
                        }
                    } else {
                        return result127(registers);
                    }
                } else {
                    return result199(registers);
                }
            } else {
                return result199(registers);
            }
        }
    }
    private static RuleResult n78(Registers registers) {
        return cond12(registers) ? n105(registers) : n79(registers);
    }
    private static RuleResult n105(Registers registers) {
        return cond21(registers) ? n106(registers) : n110(registers);
    }
    private static RuleResult n106(Registers registers) {
        return cond22(registers) ? n107(registers) : n110(registers);
    }
    private static RuleResult n107(Registers registers) {
        return cond23(registers) ? n108(registers) : n110(registers);
    }
    private static RuleResult n108(Registers registers) {
        return cond16(registers) ? n109(registers) : n110(registers);
    }
    private static RuleResult n109(Registers registers) {
        return cond24(registers) ? n1010(registers) : n110(registers);
    }
    private static RuleResult n110(Registers registers) {
        // this is a subgraph: [111, 115, 112, 113, 114]
        if (cond34(registers)) {
            if (cond27(registers)) {
                if (cond68(registers)) {
                    return result116(registers);
                } else {
                    return result117(registers);
                }
            } else {
                if (cond68(registers)) {
                    return result124(registers);
                } else {
                    if (cond69(registers)) {
                        if (cond64(registers)) {
                            return result125(registers);
                        } else {
                            return result126(registers);
                        }
                    } else {
                        return result126(registers);
                    }
                }
            }
        } else {
            return result127(registers);
        }
    }
    private static RuleResult n79(Registers registers) {
        return cond36(registers) ? n80(registers) : n117(registers);
    }
    private static RuleResult n80(Registers registers) {
        return cond21(registers) ? n81(registers) : n85(registers);
    }
    private static RuleResult n81(Registers registers) {
        return cond22(registers) ? n82(registers) : n85(registers);
    }
    private static RuleResult n82(Registers registers) {
        return cond23(registers) ? n83(registers) : n85(registers);
    }
    private static RuleResult n83(Registers registers) {
        return cond16(registers) ? n84(registers) : n85(registers);
    }
    private static RuleResult n84(Registers registers) {
        return cond24(registers) ? n1010(registers) : n85(registers);
    }
    private static RuleResult n85(Registers registers) {
        return cond27(registers) ? n86(registers) : n321(registers);
    }
    private static RuleResult n86(Registers registers) {
        return cond26(registers) ? n87(registers) : n88(registers);
    }
    private static RuleResult n87(Registers registers) {
        return cond40(registers) ? n972(registers) : n88(registers);
    }
    private static RuleResult n88(Registers registers) {
        return cond56(registers) ? n965(registers) : n89(registers);
    }
    private static RuleResult n89(Registers registers) {
        return cond41(registers) ? n90(registers) : result170(registers);
    }
    private static RuleResult n90(Registers registers) {
        return cond50(registers) ? n91(registers) : n974(registers);
    }
    private static RuleResult n91(Registers registers) {
        return cond51(registers) ? result170(registers) : n92(registers);
    }
    private static RuleResult n92(Registers registers) {
        return cond57(registers) ? n982(registers) : n93(registers);
    }
    private static RuleResult n93(Registers registers) {
        return cond78(registers) ? n94(registers) : n948(registers);
    }
    private static RuleResult n94(Registers registers) {
        return cond58(registers) ? n986(registers) : n95(registers);
    }
    private static RuleResult n95(Registers registers) {
        // this is a subgraph: [96, 97, 98, 99, 100, 101, 102, 103, 104]
        if (cond49(registers)) {
            if (cond52(registers)) {
                return result142(registers);
            } else {
                if (cond39(registers)) {
                    if (cond42(registers)) {
                        if (cond61(registers)) {
                            if (cond62(registers)) {
                                if (cond75(registers)) {
                                    if (cond72(registers)) {
                                        if (cond76(registers)) {
                                            if (cond73(registers)) {
                                                return result147(registers);
                                            } else {
                                                return result136(registers);
                                            }
                                        } else {
                                            return result137(registers);
                                        }
                                    } else {
                                        return result150(registers);
                                    }
                                } else {
                                    return result138(registers);
                                }
                            } else {
                                return result139(registers);
                            }
                        } else {
                            return result131(registers);
                        }
                    } else {
                        if (cond62(registers)) {
                            if (cond75(registers)) {
                                if (cond72(registers)) {
                                    if (cond76(registers)) {
                                        if (cond73(registers)) {
                                            return result147(registers);
                                        } else {
                                            return result136(registers);
                                        }
                                    } else {
                                        return result137(registers);
                                    }
                                } else {
                                    return result150(registers);
                                }
                            } else {
                                return result138(registers);
                            }
                        } else {
                            return result139(registers);
                        }
                    }
                } else {
                    if (cond62(registers)) {
                        if (cond75(registers)) {
                            if (cond72(registers)) {
                                if (cond76(registers)) {
                                    if (cond73(registers)) {
                                        return result147(registers);
                                    } else {
                                        return result136(registers);
                                    }
                                } else {
                                    return result137(registers);
                                }
                            } else {
                                return result150(registers);
                            }
                        } else {
                            return result138(registers);
                        }
                    } else {
                        return result139(registers);
                    }
                }
            }
        } else {
            return result142(registers);
        }
    }
    private static RuleResult n69(Registers registers) {
        return cond79(registers) ? n144(registers) : n70(registers);
    }
    private static RuleResult n70(Registers registers) {
        return cond80(registers) ? n132(registers) : n71(registers);
    }
    private static RuleResult n71(Registers registers) {
        return cond81(registers) ? n72(registers) : n77(registers);
    }
    private static RuleResult n72(Registers registers) {
        return cond10(registers) ? n895(registers) : n73(registers);
    }
    private static RuleResult n73(Registers registers) {
        return cond12(registers) ? n105(registers) : n74(registers);
    }
    private static RuleResult n74(Registers registers) {
        return cond36(registers) ? n80(registers) : n895(registers);
    }
    private static RuleResult n36(Registers registers) {
        return cond20(registers) ? n39(registers) : n37(registers);
    }
    private static RuleResult n39(Registers registers) {
        return cond79(registers) ? n53(registers) : n40(registers);
    }
    private static RuleResult n53(Registers registers) {
        return cond12(registers) ? n54(registers) : n145(registers);
    }
    private static RuleResult n54(Registers registers) {
        return cond37(registers) ? n160(registers) : n55(registers);
    }
    private static RuleResult n55(Registers registers) {
        return cond27(registers) ? n56(registers) : n252(registers);
    }
    private static RuleResult n56(Registers registers) {
        // this is a subgraph: [57, 66, 67, 58, 59, 60, 61, 62, 63, 64, 65]
        if (cond82(registers)) {
            if (cond31(registers)) {
                if (cond84(registers)) {
                    if (cond44(registers)) {
                        return result38(registers);
                    } else {
                        return result42(registers);
                    }
                } else {
                    return result42(registers);
                }
            } else {
                if (cond84(registers)) {
                    if (cond44(registers)) {
                        return result38(registers);
                    } else {
                        if (cond85(registers)) {
                            if (cond46(registers)) {
                                return result46(registers);
                            } else {
                                if (cond47(registers)) {
                                    if (cond86(registers)) {
                                        return result50(registers);
                                    } else {
                                        if (cond54(registers)) {
                                            if (cond88(registers)) {
                                                return result54(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result54(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result50(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result54(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result54(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                } else {
                    if (cond85(registers)) {
                        if (cond46(registers)) {
                            return result46(registers);
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result50(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result54(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result54(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result50(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result54(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result54(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            }
        } else {
            if (cond84(registers)) {
                if (cond44(registers)) {
                    return result38(registers);
                } else {
                    if (cond85(registers)) {
                        if (cond46(registers)) {
                            return result46(registers);
                        } else {
                            if (cond47(registers)) {
                                if (cond86(registers)) {
                                    return result50(registers);
                                } else {
                                    if (cond54(registers)) {
                                        if (cond88(registers)) {
                                            return result54(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result54(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result50(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result54(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result54(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            } else {
                if (cond85(registers)) {
                    if (cond46(registers)) {
                        return result46(registers);
                    } else {
                        if (cond47(registers)) {
                            if (cond86(registers)) {
                                return result50(registers);
                            } else {
                                if (cond54(registers)) {
                                    if (cond88(registers)) {
                                        return result54(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result54(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                } else {
                    if (cond47(registers)) {
                        if (cond86(registers)) {
                            return result50(registers);
                        } else {
                            if (cond54(registers)) {
                                if (cond88(registers)) {
                                    return result54(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    } else {
                        if (cond54(registers)) {
                            if (cond88(registers)) {
                                return result54(registers);
                            } else {
                                return result35(registers);
                            }
                        } else {
                            return result35(registers);
                        }
                    }
                }
            }
        }
    }
    private static RuleResult n252(Registers registers) {
        return cond82(registers) ? n253(registers) : n254(registers);
    }
    private static RuleResult n253(Registers registers) {
        return cond31(registers) ? n262(registers) : n254(registers);
    }
    private static RuleResult n262(Registers registers) {
        return cond84(registers) ? n263(registers) : result43(registers);
    }
    private static RuleResult n263(Registers registers) {
        return cond44(registers) ? result39(registers) : result43(registers);
    }
    private static RuleResult n254(Registers registers) {
        return cond84(registers) ? n255(registers) : n256(registers);
    }
    private static RuleResult n255(Registers registers) {
        return cond44(registers) ? result39(registers) : n256(registers);
    }
    private static RuleResult n256(Registers registers) {
        return cond85(registers) ? n257(registers) : n258(registers);
    }
    private static RuleResult n257(Registers registers) {
        return cond46(registers) ? result47(registers) : n258(registers);
    }
    private static RuleResult n258(Registers registers) {
        return cond47(registers) ? n259(registers) : n260(registers);
    }
    private static RuleResult n259(Registers registers) {
        return cond86(registers) ? result51(registers) : n260(registers);
    }
    private static RuleResult n260(Registers registers) {
        return cond54(registers) ? n261(registers) : result35(registers);
    }
    private static RuleResult n261(Registers registers) {
        return cond88(registers) ? result55(registers) : result35(registers);
    }
    private static RuleResult n40(Registers registers) {
        return cond80(registers) ? n41(registers) : n77(registers);
    }
    private static RuleResult n41(Registers registers) {
        return cond12(registers) ? n42(registers) : result8(registers);
    }
    private static RuleResult n42(Registers registers) {
        return cond27(registers) ? n43(registers) : n239(registers);
    }
    private static RuleResult n43(Registers registers) {
        // this is a subgraph: [44, 45, 46, 47, 48, 49, 50, 51, 52]
        if (cond30(registers)) {
            if (cond82(registers)) {
                return result78(registers);
            } else {
                if (cond83(registers)) {
                    if (cond32(registers)) {
                        return result82(registers);
                    } else {
                        if (cond45(registers)) {
                            if (cond86(registers)) {
                                return result86(registers);
                            } else {
                                if (cond87(registers)) {
                                    if (cond53(registers)) {
                                        return result90(registers);
                                    } else {
                                        if (cond55(registers)) {
                                            if (cond89(registers)) {
                                                return result94(registers);
                                            } else {
                                                return result35(registers);
                                            }
                                        } else {
                                            return result35(registers);
                                        }
                                    }
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result94(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            }
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result90(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result94(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result94(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    }
                } else {
                    if (cond45(registers)) {
                        if (cond86(registers)) {
                            return result86(registers);
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result90(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result94(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result94(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result90(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result94(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result94(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            }
        } else {
            if (cond83(registers)) {
                if (cond32(registers)) {
                    return result82(registers);
                } else {
                    if (cond45(registers)) {
                        if (cond86(registers)) {
                            return result86(registers);
                        } else {
                            if (cond87(registers)) {
                                if (cond53(registers)) {
                                    return result90(registers);
                                } else {
                                    if (cond55(registers)) {
                                        if (cond89(registers)) {
                                            return result94(registers);
                                        } else {
                                            return result35(registers);
                                        }
                                    } else {
                                        return result35(registers);
                                    }
                                }
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result94(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        }
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result90(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result94(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result94(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                }
            } else {
                if (cond45(registers)) {
                    if (cond86(registers)) {
                        return result86(registers);
                    } else {
                        if (cond87(registers)) {
                            if (cond53(registers)) {
                                return result90(registers);
                            } else {
                                if (cond55(registers)) {
                                    if (cond89(registers)) {
                                        return result94(registers);
                                    } else {
                                        return result35(registers);
                                    }
                                } else {
                                    return result35(registers);
                                }
                            }
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result94(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    }
                } else {
                    if (cond87(registers)) {
                        if (cond53(registers)) {
                            return result90(registers);
                        } else {
                            if (cond55(registers)) {
                                if (cond89(registers)) {
                                    return result94(registers);
                                } else {
                                    return result35(registers);
                                }
                            } else {
                                return result35(registers);
                            }
                        }
                    } else {
                        if (cond55(registers)) {
                            if (cond89(registers)) {
                                return result94(registers);
                            } else {
                                return result35(registers);
                            }
                        } else {
                            return result35(registers);
                        }
                    }
                }
            }
        }
    }
    private static RuleResult n239(Registers registers) {
        return cond30(registers) ? n240(registers) : n241(registers);
    }
    private static RuleResult n240(Registers registers) {
        return cond82(registers) ? result79(registers) : n241(registers);
    }
    private static RuleResult n241(Registers registers) {
        return cond83(registers) ? n242(registers) : n243(registers);
    }
    private static RuleResult n242(Registers registers) {
        return cond32(registers) ? result83(registers) : n243(registers);
    }
    private static RuleResult n243(Registers registers) {
        return cond45(registers) ? n244(registers) : n245(registers);
    }
    private static RuleResult n244(Registers registers) {
        return cond86(registers) ? result87(registers) : n245(registers);
    }
    private static RuleResult n245(Registers registers) {
        return cond87(registers) ? n246(registers) : n247(registers);
    }
    private static RuleResult n246(Registers registers) {
        return cond53(registers) ? result91(registers) : n247(registers);
    }
    private static RuleResult n247(Registers registers) {
        return cond55(registers) ? n248(registers) : result35(registers);
    }
    private static RuleResult n248(Registers registers) {
        return cond89(registers) ? result95(registers) : result35(registers);
    }
    private static RuleResult n37(Registers registers) {
        return cond79(registers) ? n53(registers) : n38(registers);
    }
    private static RuleResult n38(Registers registers) {
        return cond80(registers) ? n41(registers) : n71(registers);
    }
    private static RuleResult n24(Registers registers) {
        return cond7(registers) ? n25(registers) : n26(registers);
    }
    private static RuleResult n25(Registers registers) {
        return cond9(registers) ? n30(registers) : n26(registers);
    }
    private static RuleResult n30(Registers registers) {
        return cond20(registers) ? n32(registers) : n31(registers);
    }
    private static RuleResult n32(Registers registers) {
        return cond79(registers) ? n33(registers) : n76(registers);
    }
    private static RuleResult n33(Registers registers) {
        return cond12(registers) ? n147(registers) : result8(registers);
    }
    private static RuleResult n31(Registers registers) {
        return cond79(registers) ? n33(registers) : n70(registers);
    }
    private static RuleResult n26(Registers registers) {
        return cond20(registers) ? n28(registers) : n27(registers);
    }
    private static RuleResult n28(Registers registers) {
        return cond79(registers) ? n29(registers) : n40(registers);
    }
    private static RuleResult n29(Registers registers) {
        return cond12(registers) ? n55(registers) : result8(registers);
    }
    private static RuleResult n27(Registers registers) {
        return cond79(registers) ? n29(registers) : n38(registers);
    }
    private static RuleResult n11(Registers registers) {
        return cond20(registers) ? n17(registers) : n12(registers);
    }
    private static RuleResult n17(Registers registers) {
        return cond79(registers) ? result8(registers) : n18(registers);
    }
    private static RuleResult n18(Registers registers) {
        return cond80(registers) ? result8(registers) : n19(registers);
    }
    private static RuleResult n19(Registers registers) {
        return cond10(registers) ? n867(registers) : n20(registers);
    }
    private static RuleResult n20(Registers registers) {
        return cond36(registers) ? n21(registers) : result199(registers);
    }
    private static RuleResult n21(Registers registers) {
        return cond27(registers) ? n860(registers) : n198(registers);
    }
    private static RuleResult n12(Registers registers) {
        return cond79(registers) ? result8(registers) : n13(registers);
    }
    private static RuleResult n13(Registers registers) {
        return cond80(registers) ? result8(registers) : n14(registers);
    }
    private static RuleResult n14(Registers registers) {
        return cond81(registers) ? n15(registers) : n19(registers);
    }
    private static RuleResult n15(Registers registers) {
        return cond10(registers) ? result171(registers) : n16(registers);
    }
    private static RuleResult n16(Registers registers) {
        return cond36(registers) ? n21(registers) : result171(registers);
    }
    private static RuleResult n5(Registers registers) {
        // this is a subgraph: [6, 7, 840, 8, 9, 125, 126, 834, 835, 839, 836, 837, 838]
        if (cond19(registers)) {
            if (cond3(registers)) {
                if (cond8(registers)) {
                    if (cond27(registers)) {
                        return result99(registers);
                    } else {
                        return result100(registers);
                    }
                } else {
                    if (cond15(registers)) {
                        if (cond17(registers)) {
                            if (cond33(registers)) {
                                if (cond27(registers)) {
                                    return result128(registers);
                                } else {
                                    return result187(registers);
                                }
                            } else {
                                return result127(registers);
                            }
                        } else {
                            if (cond33(registers)) {
                                if (cond27(registers)) {
                                    if (cond68(registers)) {
                                        return result192(registers);
                                    } else {
                                        return result193(registers);
                                    }
                                } else {
                                    if (cond68(registers)) {
                                        return result196(registers);
                                    } else {
                                        if (cond69(registers)) {
                                            if (cond64(registers)) {
                                                return result197(registers);
                                            } else {
                                                return result198(registers);
                                            }
                                        } else {
                                            return result198(registers);
                                        }
                                    }
                                }
                            } else {
                                return result127(registers);
                            }
                        }
                    } else {
                        if (cond33(registers)) {
                            if (cond27(registers)) {
                                if (cond68(registers)) {
                                    return result192(registers);
                                } else {
                                    return result193(registers);
                                }
                            } else {
                                if (cond68(registers)) {
                                    return result196(registers);
                                } else {
                                    if (cond69(registers)) {
                                        if (cond64(registers)) {
                                            return result197(registers);
                                        } else {
                                            return result198(registers);
                                        }
                                    } else {
                                        return result198(registers);
                                    }
                                }
                            }
                        } else {
                            return result127(registers);
                        }
                    }
                }
            } else {
                if (cond15(registers)) {
                    if (cond17(registers)) {
                        if (cond33(registers)) {
                            if (cond27(registers)) {
                                return result128(registers);
                            } else {
                                return result187(registers);
                            }
                        } else {
                            return result127(registers);
                        }
                    } else {
                        if (cond33(registers)) {
                            if (cond27(registers)) {
                                if (cond68(registers)) {
                                    return result192(registers);
                                } else {
                                    return result193(registers);
                                }
                            } else {
                                if (cond68(registers)) {
                                    return result196(registers);
                                } else {
                                    if (cond69(registers)) {
                                        if (cond64(registers)) {
                                            return result197(registers);
                                        } else {
                                            return result198(registers);
                                        }
                                    } else {
                                        return result198(registers);
                                    }
                                }
                            }
                        } else {
                            return result127(registers);
                        }
                    }
                } else {
                    if (cond33(registers)) {
                        if (cond27(registers)) {
                            if (cond68(registers)) {
                                return result192(registers);
                            } else {
                                return result193(registers);
                            }
                        } else {
                            if (cond68(registers)) {
                                return result196(registers);
                            } else {
                                if (cond69(registers)) {
                                    if (cond64(registers)) {
                                        return result197(registers);
                                    } else {
                                        return result198(registers);
                                    }
                                } else {
                                    return result198(registers);
                                }
                            }
                        }
                    } else {
                        return result127(registers);
                    }
                }
            }
        } else {
            return result199(registers);
        }
    }


    private static class Registers {
        String bucket;

        String region;

        boolean useFIPS;

        boolean useDualStack;

        String endpoint;

        boolean forcePathStyle;

        boolean accelerate;

        boolean useGlobalEndpoint;

        Boolean useObjectLambdaEndpoint;

        String key;

        String prefix;

        String copySource;

        Boolean disableAccessPoints;

        boolean disableMultiRegionAccessPoints;

        Boolean useArnRegion;

        Boolean useS3ExpressControlEndpoint;

        Boolean disableS3ExpressSessionAuth;

        RulePartition partitionResult;

        String outpostId_ssa_2;

        String hardwareType;

        String accessPointSuffix;

        String regionPrefix;

        String s3expressAvailabilityZoneId_ssa_6;

        String s3expressAvailabilityZoneId_ssa_2;

        String s3expressAvailabilityZoneId_ssa_7;

        RuleArn bucketArn;

        String uri_encoded_bucket;

        RuleUrl url;

        String arnType;

        String outpostId_ssa_1;

        String s3expressAvailabilityZoneId_ssa_1;

        String s3expressAvailabilityZoneId_ssa_8;

        String s3expressAvailabilityZoneId_ssa_3;

        String s3expressAvailabilityZoneId_ssa_4;

        String accessPointName_ssa_1;

        String s3expressAvailabilityZoneId_ssa_9;

        String s3expressAvailabilityZoneId_ssa_5;

        String s3expressAvailabilityZoneId_ssa_10;

        String outpostType;

        String accessPointName_ssa_2;

        RulePartition bucketPartition;
    }

    @FunctionalInterface
    interface ConditionFn {
        boolean test(Registers registers);
    }

    @FunctionalInterface
    interface ResultFn {
        RuleResult apply(Registers registers);
    }
}
