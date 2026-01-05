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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
// cost opt 5, using https://github.com/smithy-lang/smithy/commit/99d556a28c1fe79b840f61a39370eb3bdfd5bf07
// uses Evaluator class with cond/result functions
// result functions w/ switch dispatch, condition inline
// load binary bdd nodes from resource
// Optimized loop (no complimented nodes).
public final class BddCostOpt5Runtime6b implements S3EndpointProvider {
    private static final int[] BDD_DEFINITION;

    static {
        try (InputStream in = DefaultS3EndpointProvider.class.getResourceAsStream("/endpoints_bdd_f8772715.bin")) {
            if (in == null) {
                throw new IllegalStateException("Resource /endpoints_bdd_f8772715.bin not found");
            }
            BDD_DEFINITION = new int[1563];
            DataInputStream data = new DataInputStream(in);
            for (int i = 0; i < 1563; i++) {
                BDD_DEFINITION[i] = data.readInt();
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(S3EndpointParams params) {
        Evaluator evaluator = new Evaluator();
        evaluator.region = params.region() == null ? null : params.region().id();
        evaluator.bucket = params.bucket();
        evaluator.useFIPS = params.useFips();
        evaluator.useDualStack = params.useDualStack();
        evaluator.endpoint = params.endpoint();
        evaluator.forcePathStyle = params.forcePathStyle();
        evaluator.accelerate = params.accelerate();
        evaluator.useGlobalEndpoint = params.useGlobalEndpoint();
        evaluator.useObjectLambdaEndpoint = params.useObjectLambdaEndpoint();
        evaluator.key = params.key();
        evaluator.prefix = params.prefix();
        evaluator.copySource = params.copySource();
        evaluator.disableAccessPoints = params.disableAccessPoints();
        evaluator.disableMultiRegionAccessPoints = params.disableMultiRegionAccessPoints();
        evaluator.useArnRegion = params.useArnRegion();
        evaluator.useS3ExpressControlEndpoint = params.useS3ExpressControlEndpoint();
        evaluator.disableS3ExpressSessionAuth = params.disableS3ExpressSessionAuth();
        final int[] bdd = BDD_DEFINITION;
        int nodeRef = 521;
        while ((nodeRef > 1) && nodeRef < 100000000) {
            int base = (nodeRef - 1) * 3;
            int conditionResult = evaluator.cond(bdd[base]) ? 1 : 0;
            nodeRef = bdd[base + 2 - conditionResult];
        }
        if (nodeRef == -1 || nodeRef == 1) {
            return CompletableFutureUtils.failedFuture(SdkClientException
                                                           .create("Rule engine did not reach an error or endpoint result"));
        } else {
            RuleResult result = evaluator.result(nodeRef - 100000001);
            if (result.isError()) {
                String errorMsg = result.error();
                if (errorMsg.contains("Invalid ARN") && errorMsg.contains(":s3:::")) {
                    errorMsg += ". Use the bucket name instead of simple bucket ARNs in GetBucketLocationRequest.";
                }
                return CompletableFutureUtils.failedFuture(SdkClientException.create(errorMsg));
            }
            return CompletableFuture.completedFuture(result.endpoint());
        }
    }

    private static final class Evaluator {
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

        String _effective_std_region;

        RulePartition partitionResult;

        RuleUrl url;

        String accessPointSuffix;

        String regionPrefix;

        String hardwareType;

        String outpostId_ssa_2;

        String _s3e_ds;

        String _s3e_fips;

        String _s3e_auth;

        String s3expressAvailabilityZoneId;

        RuleArn bucketArn;

        String _effective_arn_region;

        String uri_encoded_bucket;

        String arnType;

        String accessPointName_ssa_1;

        RulePartition bucketPartition;

        String outpostId_ssa_1;

        String outpostType;

        String accessPointName_ssa_2;

        public final boolean cond(int i) {
            switch (i) {
                case 0: {
                    return (region != null);
                }
                case 1: {
                    _effective_std_region = RulesFunctions.ite("aws-global".equals(region), "us-east-1", region);
                    return _effective_std_region != null;
                }
                case 2: {
                    return (accelerate);
                }
                case 3: {
                    return (useFIPS);
                }
                case 4: {
                    return (RulesFunctions.coalesce(disableS3ExpressSessionAuth, false));
                }
                case 5: {
                    return (endpoint != null);
                }
                case 6: {
                    return (useDualStack);
                }
                case 7: {
                    return (bucket != null);
                }
                case 8: {
                    return ("--x-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 0, 6, true), "")));
                }
                case 9: {
                    return ("--xa-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 0, 7, true), "")));
                }
                case 10: {
                    partitionResult = RulesFunctions.awsPartition(region);
                    return partitionResult != null;
                }
                case 11: {
                    url = RulesFunctions.parseURL(endpoint);
                    return url != null;
                }
                case 12: {
                    accessPointSuffix = RulesFunctions.substring(bucket, 0, 7, true);
                    return accessPointSuffix != null;
                }
                case 13: {
                    return ("--op-s3".equals(accessPointSuffix));
                }
                case 14: {
                    regionPrefix = RulesFunctions.substring(bucket, 8, 12, true);
                    return regionPrefix != null;
                }
                case 15: {
                    hardwareType = RulesFunctions.substring(bucket, 49, 50, true);
                    return hardwareType != null;
                }
                case 16: {
                    outpostId_ssa_2 = RulesFunctions.substring(bucket, 32, 49, true);
                    return outpostId_ssa_2 != null;
                }
                case 17: {
                    return ("aws-cn".equals(partitionResult.name()));
                }
                case 18: {
                    _s3e_ds = RulesFunctions.ite(useDualStack, ".dualstack", "");
                    return _s3e_ds != null;
                }
                case 19: {
                    _s3e_fips = RulesFunctions.ite(useFIPS, "-fips", "");
                    return _s3e_fips != null;
                }
                case 20: {
                    return (forcePathStyle);
                }
                case 21: {
                    _s3e_auth = RulesFunctions.ite(RulesFunctions.coalesce(disableS3ExpressSessionAuth, false), "sigv4",
                                                   "sigv4-s3express");
                    return _s3e_auth != null;
                }
                case 22: {
                    return (RulesFunctions.awsIsVirtualHostableS3Bucket(bucket, false));
                }
                case 23: {
                    s3expressAvailabilityZoneId = RulesFunctions.listAccess(RulesFunctions.split(bucket, "--", 0), 1);
                    return s3expressAvailabilityZoneId != null;
                }
                case 24: {
                    return (RulesFunctions.isValidHostLabel(outpostId_ssa_2, false));
                }
                case 25: {
                    return (RulesFunctions.coalesce(useS3ExpressControlEndpoint, false));
                }
                case 26: {
                    return ("beta".equals(regionPrefix));
                }
                case 27: {
                    return (RulesFunctions.awsIsVirtualHostableS3Bucket(bucket, true));
                }
                case 28: {
                    return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 16, 18, true), "")));
                }
                case 29: {
                    return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 21, 23, true), "")));
                }
                case 30: {
                    return ("http".equals(url.scheme()));
                }
                case 31: {
                    return (RulesFunctions.isValidHostLabel(region, false));
                }
                case 32: {
                    bucketArn = RulesFunctions.awsParseArn(bucket);
                    return bucketArn != null;
                }
                case 33: {
                    return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 27, 29, true), "")));
                }
                case 34: {
                    _effective_arn_region = RulesFunctions.ite(RulesFunctions.coalesce(useArnRegion, true), bucketArn.region(),
                                                               region);
                    return _effective_arn_region != null;
                }
                case 35: {
                    return (url.isIp());
                }
                case 36: {
                    return ("arn:".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 0, 4, false), "")));
                }
                case 37: {
                    uri_encoded_bucket = RulesFunctions.uriEncode(bucket);
                    return uri_encoded_bucket != null;
                }
                case 38: {
                    return (RulesFunctions.coalesce(useObjectLambdaEndpoint, false));
                }
                case 39: {
                    arnType = RulesFunctions.listAccess(bucketArn.resourceId(), 0);
                    return arnType != null;
                }
                case 40: {
                    return ("".equals(arnType));
                }
                case 41: {
                    return ("accesspoint".equals(arnType));
                }
                case 42: {
                    accessPointName_ssa_1 = RulesFunctions.listAccess(bucketArn.resourceId(), 1);
                    return accessPointName_ssa_1 != null;
                }
                case 43: {
                    return ("".equals(accessPointName_ssa_1));
                }
                case 44: {
                    return ("s3-object-lambda".equals(bucketArn.service()));
                }
                case 45: {
                    return (RulesFunctions.isValidHostLabel(region, true));
                }
                case 46: {
                    return ("".equals(bucketArn.region()));
                }
                case 47: {
                    return ("e".equals(hardwareType));
                }
                case 48: {
                    return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 26, 28, true), "")));
                }
                case 49: {
                    return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 19, 21, true), "")));
                }
                case 50: {
                    return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 14, 16, true), "")));
                }
                case 51: {
                    return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 20, 22, true), "")));
                }
                case 52: {
                    return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 15, 17, true), "")));
                }
                case 53: {
                    return ("s3-outposts".equals(bucketArn.service()));
                }
                case 54: {
                    return (RulesFunctions.coalesce(disableAccessPoints, false));
                }
                case 55: {
                    bucketPartition = RulesFunctions.awsPartition(_effective_arn_region);
                    return bucketPartition != null;
                }
                case 56: {
                    return ("o".equals(hardwareType));
                }
                case 57: {
                    return (RulesFunctions.listAccess(bucketArn.resourceId(), 4) != null);
                }
                case 58: {
                    outpostId_ssa_1 = RulesFunctions.listAccess(bucketArn.resourceId(), 1);
                    return outpostId_ssa_1 != null;
                }
                case 59: {
                    return (RulesFunctions.coalesce(useArnRegion, true));
                }
                case 60: {
                    return (RulesFunctions.stringEquals(_effective_arn_region, bucketArn.region()));
                }
                case 61: {
                    return ("aws-global".equals(region));
                }
                case 62: {
                    return (useGlobalEndpoint);
                }
                case 63: {
                    return (disableMultiRegionAccessPoints);
                }
                case 64: {
                    return ("us-east-1".equals(region));
                }
                case 65: {
                    return (RulesFunctions.isValidHostLabel(outpostId_ssa_1, false));
                }
                case 66: {
                    outpostType = RulesFunctions.listAccess(bucketArn.resourceId(), 2);
                    return outpostType != null;
                }
                case 67: {
                    return (RulesFunctions.stringEquals(bucketPartition.name(), partitionResult.name()));
                }
                case 68: {
                    return (RulesFunctions.isValidHostLabel(_effective_arn_region, true));
                }
                case 69: {
                    return ("s3".equals(bucketArn.service()));
                }
                case 70: {
                    return ("".equals(bucketArn.accountId()));
                }
                case 71: {
                    return (RulesFunctions.isValidHostLabel(bucketArn.accountId(), false));
                }
                case 72: {
                    return (RulesFunctions.isValidHostLabel(accessPointName_ssa_1, false));
                }
                case 73: {
                    accessPointName_ssa_2 = RulesFunctions.listAccess(bucketArn.resourceId(), 3);
                    return accessPointName_ssa_2 != null;
                }
                case 74: {
                    return (RulesFunctions.isValidHostLabel(accessPointName_ssa_1, true));
                }
                case 75: {
                    return (RulesFunctions.stringEquals(bucketArn.partition(), partitionResult.name()));
                }
                case 76: {
                    return ("accesspoint".equals(outpostType));
                }
                default: {
                    throw new IllegalArgumentException("Unknown condition index");
                }
            }
        }

        public final RuleResult result(int i) {
            switch (i) {
                case 0: {
                    return result0();
                }
                case 1: {
                    return result1();
                }
                case 2: {
                    return result2();
                }
                case 3: {
                    return result3();
                }
                case 4: {
                    return result4();
                }
                case 5: {
                    return result5();
                }
                case 6: {
                    return result6();
                }
                case 7: {
                    return result7();
                }
                case 8: {
                    return result8();
                }
                case 9: {
                    return result9();
                }
                case 10: {
                    return result10();
                }
                case 11: {
                    return result11();
                }
                case 12: {
                    return result12();
                }
                case 13: {
                    return result13();
                }
                case 14: {
                    return result14();
                }
                case 15: {
                    return result15();
                }
                case 16: {
                    return result16();
                }
                case 17: {
                    return result17();
                }
                case 18: {
                    return result18();
                }
                case 19: {
                    return result19();
                }
                case 20: {
                    return result20();
                }
                case 21: {
                    return result21();
                }
                case 22: {
                    return result22();
                }
                case 23: {
                    return result23();
                }
                case 24: {
                    return result24();
                }
                case 25: {
                    return result25();
                }
                case 26: {
                    return result26();
                }
                case 27: {
                    return result27();
                }
                case 28: {
                    return result28();
                }
                case 29: {
                    return result29();
                }
                case 30: {
                    return result30();
                }
                case 31: {
                    return result31();
                }
                case 32: {
                    return result32();
                }
                case 33: {
                    return result33();
                }
                case 34: {
                    return result34();
                }
                case 35: {
                    return result35();
                }
                case 36: {
                    return result36();
                }
                case 37: {
                    return result37();
                }
                case 38: {
                    return result38();
                }
                case 39: {
                    return result39();
                }
                case 40: {
                    return result40();
                }
                case 41: {
                    return result41();
                }
                case 42: {
                    return result42();
                }
                case 43: {
                    return result43();
                }
                case 44: {
                    return result44();
                }
                case 45: {
                    return result45();
                }
                case 46: {
                    return result46();
                }
                case 47: {
                    return result47();
                }
                case 48: {
                    return result48();
                }
                case 49: {
                    return result49();
                }
                case 50: {
                    return result50();
                }
                case 51: {
                    return result51();
                }
                case 52: {
                    return result52();
                }
                case 53: {
                    return result53();
                }
                case 54: {
                    return result54();
                }
                case 55: {
                    return result55();
                }
                case 56: {
                    return result56();
                }
                case 57: {
                    return result57();
                }
                case 58: {
                    return result58();
                }
                case 59: {
                    return result59();
                }
                case 60: {
                    return result60();
                }
                case 61: {
                    return result61();
                }
                case 62: {
                    return result62();
                }
                case 63: {
                    return result63();
                }
                case 64: {
                    return result64();
                }
                case 65: {
                    return result65();
                }
                case 66: {
                    return result66();
                }
                case 67: {
                    return result67();
                }
                case 68: {
                    return result68();
                }
                case 69: {
                    return result69();
                }
                case 70: {
                    return result70();
                }
                case 71: {
                    return result71();
                }
                case 72: {
                    return result72();
                }
                case 73: {
                    return result73();
                }
                case 74: {
                    return result74();
                }
                case 75: {
                    return result75();
                }
                case 76: {
                    return result76();
                }
                case 77: {
                    return result77();
                }
                case 78: {
                    return result78();
                }
                case 79: {
                    return result79();
                }
                case 80: {
                    return result80();
                }
                case 81: {
                    return result81();
                }
                case 82: {
                    return result82();
                }
                case 83: {
                    return result83();
                }
                case 84: {
                    return result84();
                }
                case 85: {
                    return result85();
                }
                case 86: {
                    return result86();
                }
                case 87: {
                    return result87();
                }
                case 88: {
                    return result88();
                }
                case 89: {
                    return result89();
                }
                case 90: {
                    return result90();
                }
                case 91: {
                    return result91();
                }
                case 92: {
                    return result92();
                }
                case 93: {
                    return result93();
                }
                case 94: {
                    return result94();
                }
                case 95: {
                    return result95();
                }
                default: {
                    throw new IllegalArgumentException("Unknown condition index");
                }
            }
        }

        private final RuleResult result0() {
            return RuleResult.error("Accelerate cannot be used with FIPS");
        }

        private final RuleResult result1() {
            return RuleResult.error("Cannot set dual-stack in combination with a custom endpoint.");
        }

        private final RuleResult result2() {
            return RuleResult.error("A custom endpoint cannot be combined with FIPS");
        }

        private final RuleResult result3() {
            return RuleResult.error("A custom endpoint cannot be combined with S3 Accelerate");
        }

        private final RuleResult result4() {
            return RuleResult.error("Partition does not support FIPS");
        }

        private final RuleResult result5() {
            return RuleResult.error("S3Express does not support S3 Accelerate.");
        }

        private final RuleResult result6() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + url.authority() + "/" + uri_encoded_bucket + url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES,
                                                         Arrays.asList(DynamicAuthBuilder.builder().name(_s3e_auth).build())).build());
        }

        private final RuleResult result7() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + bucket + "." + url.authority() + url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES,
                                                         Arrays.asList(DynamicAuthBuilder.builder().name(_s3e_auth).build())).build());
        }

        private final RuleResult result8() {
            return RuleResult.error("S3Express bucket name is not a valid virtual hostable name.");
        }

        private final RuleResult result9() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3express-control" + _s3e_fips + _s3e_ds + "." + _effective_std_region + "."
                                                           + partitionResult.dnsSuffix() + "/" + uri_encoded_bucket))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result10() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3express" + _s3e_fips + "-" + s3expressAvailabilityZoneId + _s3e_ds
                                                           + "." + _effective_std_region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES,
                                                         Arrays.asList(DynamicAuthBuilder.builder().name(_s3e_auth).build())).build());
        }

        private final RuleResult result11() {
            return RuleResult.error("Unrecognized S3Express bucket name format.");
        }

        private final RuleResult result12() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + url.authority() + url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES,
                                                         Arrays.asList(DynamicAuthBuilder.builder().name(_s3e_auth).build())).build());
        }

        private final RuleResult result13() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3express-control" + _s3e_fips + _s3e_ds + "." + _effective_std_region + "."
                                                           + partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result14() {
            return RuleResult.error("Expected a endpoint to be specified but no endpoint was found");
        }

        private final RuleResult result15() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".ec2." + url.authority()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                             .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                           .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(_effective_std_region)
                                                                                                                                           .build())).build());
        }

        private final RuleResult result16() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".ec2.s3-outposts." + _effective_std_region + "."
                                                           + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                             .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                           .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(_effective_std_region)
                                                                                                                                           .build())).build());
        }

        private final RuleResult result17() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".op-" + outpostId_ssa_2 + "." + url.authority()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                             .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                           .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(_effective_std_region)
                                                                                                                                           .build())).build());
        }

        private final RuleResult result18() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".op-" + outpostId_ssa_2 + ".s3-outposts." + _effective_std_region
                                                           + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                             .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                           .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(_effective_std_region)
                                                                                                                                           .build())).build());
        }

        private final RuleResult result19() {
            return RuleResult.error("Unrecognized hardware type: \"Expected hardware type o or e but got " + hardwareType + "\"");
        }

        private final RuleResult result20() {
            return RuleResult.error("Invalid ARN: The outpost Id must only contain a-z, A-Z, 0-9 and `-`.");
        }

        private final RuleResult result21() {
            return RuleResult.error("Custom endpoint `" + endpoint + "` was not a valid URI");
        }

        private final RuleResult result22() {
            return RuleResult.error("S3 Accelerate cannot be used in this region");
        }

        private final RuleResult result23() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3-fips.dualstack." + _effective_std_region + "."
                                                           + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result24() {
            return RuleResult
                .endpoint(Endpoint
                              .builder()
                              .url(URI.create("https://" + bucket + ".s3-fips." + _effective_std_region + "."
                                              + partitionResult.dnsSuffix()))
                              .putAttribute(
                                  AwsEndpointAttribute.AUTH_SCHEMES,
                                  Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                               .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result25() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3-accelerate.dualstack." + _effective_std_region + "."
                                                           + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result26() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3-accelerate.dualstack." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result27() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3.dualstack." + _effective_std_region + "."
                                                           + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result28() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + url.authority() + url.normalizedPath() + bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result29() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + bucket + "." + url.authority() + url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result30() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3-accelerate." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result31() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result32() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3." + _effective_std_region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result33() {
            return RuleResult.error("Invalid region: region was not a valid DNS name.");
        }

        private final RuleResult result34() {
            return RuleResult.error("S3 Object Lambda does not support Dual-stack");
        }

        private final RuleResult result35() {
            return RuleResult.error("S3 Object Lambda does not support S3 Accelerate");
        }

        private final RuleResult result36() {
            return RuleResult.error("Access points are not supported for this operation");
        }

        private final RuleResult result37() {
            return RuleResult.error("Invalid configuration: region from ARN `" + bucketArn.region()
                                    + "` does not match client region `" + region + "` and UseArnRegion is `false`");
        }

        private final RuleResult result38() {
            return RuleResult.error("Invalid ARN: Missing account id");
        }

        private final RuleResult result39() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + accessPointName_ssa_1 + "-" + bucketArn.accountId() + "."
                                                           + url.authority() + url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                            .signingRegion(_effective_arn_region).build())).build());
        }

        private final RuleResult result40() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + accessPointName_ssa_1 + "-" + bucketArn.accountId() + ".s3-object-lambda-fips."
                                                           + _effective_arn_region + "." + bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                            .signingRegion(_effective_arn_region).build())).build());
        }

        private final RuleResult result41() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + accessPointName_ssa_1 + "-" + bucketArn.accountId() + ".s3-object-lambda."
                                                           + _effective_arn_region + "." + bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                            .signingRegion(_effective_arn_region).build())).build());
        }

        private final RuleResult result42() {
            return RuleResult.error("Invalid ARN: The access point name may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                    + accessPointName_ssa_1 + "`");
        }

        private final RuleResult result43() {
            return RuleResult.error("Invalid ARN: The account id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                    + bucketArn.accountId() + "`");
        }

        private final RuleResult result44() {
            return RuleResult.error("Invalid region in ARN: `" + bucketArn.region() + "` (invalid DNS name)");
        }

        private final RuleResult result45() {
            return RuleResult.error("Client was configured for partition `" + partitionResult.name() + "` but ARN (`" + bucket
                                    + "`) has `" + bucketPartition.name() + "`");
        }

        private final RuleResult result46() {
            return RuleResult.error("Invalid ARN: The ARN may only contain a single resource component after `accesspoint`.");
        }

        private final RuleResult result47() {
            return RuleResult.error("Invalid ARN: bucket ARN is missing a region");
        }

        private final RuleResult result48() {
            return RuleResult
                .error("Invalid ARN: Expected a resource of the format `accesspoint:<accesspoint name>` but no name was provided");
        }

        private final RuleResult result49() {
            return RuleResult.error("Invalid ARN: Object Lambda ARNs only support `accesspoint` arn types, but found: `"
                                    + arnType + "`");
        }

        private final RuleResult result50() {
            return RuleResult.error("Access Points do not support S3 Accelerate");
        }

        private final RuleResult result51() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + accessPointName_ssa_1 + "-" + bucketArn.accountId()
                                                           + ".s3-accesspoint-fips.dualstack." + _effective_arn_region + "." + bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_arn_region).build())).build());
        }

        private final RuleResult result52() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + accessPointName_ssa_1 + "-" + bucketArn.accountId() + ".s3-accesspoint-fips."
                                                           + _effective_arn_region + "." + bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_arn_region).build())).build());
        }

        private final RuleResult result53() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + accessPointName_ssa_1 + "-" + bucketArn.accountId()
                                                           + ".s3-accesspoint.dualstack." + _effective_arn_region + "." + bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_arn_region).build())).build());
        }

        private final RuleResult result54() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + accessPointName_ssa_1 + "-" + bucketArn.accountId() + "."
                                                           + url.authority() + url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_arn_region).build())).build());
        }

        private final RuleResult result55() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + accessPointName_ssa_1 + "-" + bucketArn.accountId() + ".s3-accesspoint."
                                                           + _effective_arn_region + "." + bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_arn_region).build())).build());
        }

        private final RuleResult result56() {
            return RuleResult.error("Invalid ARN: The ARN was not for the S3 service, found: " + bucketArn.service());
        }

        private final RuleResult result57() {
            return RuleResult.error("S3 MRAP does not support dual-stack");
        }

        private final RuleResult result58() {
            return RuleResult.error("S3 MRAP does not support FIPS");
        }

        private final RuleResult result59() {
            return RuleResult.error("S3 MRAP does not support S3 Accelerate");
        }

        private final RuleResult result60() {
            return RuleResult.error("Invalid configuration: Multi-Region Access Point ARNs are disabled.");
        }

        private final RuleResult result61() {
            return RuleResult
                .endpoint(Endpoint
                              .builder()
                              .url(URI.create("https://" + accessPointName_ssa_1 + ".accesspoint.s3-global."
                                              + partitionResult.dnsSuffix()))
                              .putAttribute(
                                  AwsEndpointAttribute.AUTH_SCHEMES,
                                  Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                .signingRegionSet(Arrays.asList("*")).build())).build());
        }

        private final RuleResult result62() {
            return RuleResult.error("Client was configured for partition `" + partitionResult.name()
                                    + "` but bucket referred to partition `" + bucketArn.partition() + "`");
        }

        private final RuleResult result63() {
            return RuleResult.error("Invalid Access Point Name");
        }

        private final RuleResult result64() {
            return RuleResult.error("S3 Outposts does not support Dual-stack");
        }

        private final RuleResult result65() {
            return RuleResult.error("S3 Outposts does not support FIPS");
        }

        private final RuleResult result66() {
            return RuleResult.error("S3 Outposts does not support S3 Accelerate");
        }

        private final RuleResult result67() {
            return RuleResult.error("Invalid Arn: Outpost Access Point ARN contains sub resources");
        }

        private final RuleResult result68() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + accessPointName_ssa_2 + "-" + bucketArn.accountId() + "." + outpostId_ssa_1
                                                           + "." + url.authority()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                             .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                           .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(_effective_arn_region)
                                                                                                                                           .build())).build());
        }

        private final RuleResult result69() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + accessPointName_ssa_2 + "-" + bucketArn.accountId() + "." + outpostId_ssa_1
                                                           + ".s3-outposts." + _effective_arn_region + "." + bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                             .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                           .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(_effective_arn_region)
                                                                                                                                           .build())).build());
        }

        private final RuleResult result70() {
            return RuleResult.error("Expected an outpost type `accesspoint`, found " + outpostType);
        }

        private final RuleResult result71() {
            return RuleResult.error("Invalid ARN: expected an access point name");
        }

        private final RuleResult result72() {
            return RuleResult.error("Invalid ARN: Expected a 4-component resource");
        }

        private final RuleResult result73() {
            return RuleResult.error("Invalid ARN: The outpost Id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                    + outpostId_ssa_1 + "`");
        }

        private final RuleResult result74() {
            return RuleResult.error("Invalid ARN: The Outpost Id was not set");
        }

        private final RuleResult result75() {
            return RuleResult.error("Invalid ARN: Unrecognized format: " + bucket + " (type: " + arnType + ")");
        }

        private final RuleResult result76() {
            return RuleResult.error("Invalid ARN: No ARN type specified");
        }

        private final RuleResult result77() {
            return RuleResult.error("Invalid ARN: `" + bucket + "` was not a valid ARN");
        }

        private final RuleResult result78() {
            return RuleResult.error("Path-style addressing cannot be used with ARN buckets");
        }

        private final RuleResult result79() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips.dualstack." + _effective_std_region + "." + partitionResult.dnsSuffix()
                                                           + "/" + uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result80() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips." + _effective_std_region + "." + partitionResult.dnsSuffix() + "/"
                                                           + uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result81() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3.dualstack." + _effective_std_region + "." + partitionResult.dnsSuffix() + "/"
                                                           + uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result82() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + url.authority() + url.normalizedPath() + uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result83() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3." + partitionResult.dnsSuffix() + "/" + uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result84() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3." + _effective_std_region + "." + partitionResult.dnsSuffix() + "/"
                                                           + uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result85() {
            return RuleResult.error("Path-style addressing cannot be used with S3 Accelerate");
        }

        private final RuleResult result86() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + url.authority() + url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result87() {
            return RuleResult
                .endpoint(Endpoint
                              .builder()
                              .url(URI.create("https://s3-object-lambda-fips." + _effective_std_region + "."
                                              + partitionResult.dnsSuffix()))
                              .putAttribute(
                                  AwsEndpointAttribute.AUTH_SCHEMES,
                                  Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                               .signingName("s3-object-lambda").signingRegion(_effective_std_region).build()))
                              .build());
        }

        private final RuleResult result88() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-object-lambda." + _effective_std_region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result89() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips.dualstack." + _effective_std_region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result90() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips." + _effective_std_region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result91() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3.dualstack." + _effective_std_region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result92() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + url.authority() + url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result93() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result94() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3." + _effective_std_region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(_effective_std_region).build())).build());
        }

        private final RuleResult result95() {
            return RuleResult.error("A region must be set when sending requests to S3.");
        }
    }

    public static class DynamicAuthBuilder {
        String name;

        private Map<String, String> properties = new HashMap<>();

        public static DynamicAuthBuilder builder() {
            return new DynamicAuthBuilder();
        }

        DynamicAuthBuilder name(String name) {
            this.name = name;
            return this;
        }

        DynamicAuthBuilder property(String key, String value) {
            properties.put(key, value);
            return this;
        }

        public EndpointAuthScheme build() {
            return null;
        }
    }
}
