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
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.endpoints.authscheme.S3ExpressEndpointAuthScheme;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class RulesResolverWithCache implements S3EndpointProvider {
    private static final BDDResolverRuntimeDAGWithCache.UriFactory uriFactory = new BDDResolverRuntimeDAGWithCache.UriFactory();

    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(S3EndpointParams params) {
        Validate.notNull(params.useFips(), "Parameter 'UseFIPS' must not be null");
        Validate.notNull(params.useDualStack(), "Parameter 'UseDualStack' must not be null");
        Validate.notNull(params.forcePathStyle(), "Parameter 'ForcePathStyle' must not be null");
        Validate.notNull(params.accelerate(), "Parameter 'Accelerate' must not be null");
        Validate.notNull(params.useGlobalEndpoint(), "Parameter 'UseGlobalEndpoint' must not be null");
        Validate.notNull(params.disableMultiRegionAccessPoints(), "Parameter 'DisableMultiRegionAccessPoints' must not be null");
        try {
            Region region = params.region();
            String regionId = region == null ? null : region.id();
            RuleResult result = endpointRule0(params, regionId);
            if (result.canContinue()) {
                throw SdkClientException.create("Rule engine did not reach an error or endpoint result");
            }
            if (result.isError()) {
                String errorMsg = result.error();
                if (errorMsg.contains("Invalid ARN") && errorMsg.contains(":s3:::")) {
                    errorMsg += ". Use the bucket name instead of simple bucket ARNs in GetBucketLocationRequest.";
                }
                throw SdkClientException.create(errorMsg);
            }
            return CompletableFuture.completedFuture(result.endpoint());
        } catch (Exception error) {
            return CompletableFutureUtils.failedFuture(error);
        }
    }

    private static RuleResult endpointRule0(S3EndpointParams params, String region) {
        RuleResult result = endpointRule1(params, region);
        if (result.isResolved()) {
            return result;
        }
        return RuleResult.error("A region must be set when sending requests to S3.");
    }

    private static RuleResult endpointRule1(S3EndpointParams params, String region) {
        if (region != null) {
            if (params.accelerate() && params.useFips()) {
                return RuleResult.error("Accelerate cannot be used with FIPS");
            }
            if (params.useDualStack() && params.endpoint() != null) {
                return RuleResult.error("Cannot set dual-stack in combination with a custom endpoint.");
            }
            if (params.endpoint() != null && params.useFips()) {
                return RuleResult.error("A custom endpoint cannot be combined with FIPS");
            }
            if (params.endpoint() != null && params.accelerate()) {
                return RuleResult.error("A custom endpoint cannot be combined with S3 Accelerate");
            }
            if (params.useFips()) {
                RulePartition partitionResult = RulesFunctions.awsPartition(region);
                if (partitionResult != null) {
                    if ("aws-cn".equals(partitionResult.name())) {
                        return RuleResult.error("Partition does not support FIPS");
                    }
                }
            }
            RuleResult result = endpointRule7(params, region);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule86(params, region);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule158(params, region);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule165(params, region);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule179(params, region);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule350(params, region);
            if (result.isResolved()) {
                return result;
            }
            return endpointRule359(params, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule7(S3EndpointParams params, String region) {
        if (params.bucket() != null) {
            String bucketSuffix = RulesFunctions.substring(params.bucket(), 0, 6, true);
            if (bucketSuffix != null) {
                if ("--x-s3".equals(bucketSuffix)) {
                    if (params.accelerate()) {
                        return RuleResult.error("S3Express does not support S3 Accelerate.");
                    }
                    RuleResult result = endpointRule9(params, region);
                    if (result.isResolved()) {
                        return result;
                    }
                    result = endpointRule23(params, region);
                    if (result.isResolved()) {
                        return result;
                    }
                    result = endpointRule30(params, region);
                    if (result.isResolved()) {
                        return result;
                    }
                    return RuleResult.error("S3Express bucket name is not a valid virtual hostable name.");
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule9(S3EndpointParams params, String region) {
        if (params.endpoint() != null) {
            RuleUrl url = RulesFunctions.parseURL(params.endpoint());
            if (url != null) {
                RuleResult result = endpointRule10(params, url, region);
                if (result.isResolved()) {
                    return result;
                }
                result = endpointRule17(params, url, region);
                if (result.isResolved()) {
                    return result;
                }
                result = endpointRule20(params, url, region);
                if (result.isResolved()) {
                    return result;
                }
                return RuleResult.error("S3Express bucket name is not a valid virtual hostable name.");
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule10(S3EndpointParams params, RuleUrl url, String region) {
        if (params.disableS3ExpressSessionAuth() != null && params.disableS3ExpressSessionAuth()) {
            RuleResult result = endpointRule11(params, url, region);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule14(params, url, region);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("S3Express bucket name is not a valid virtual hostable name.");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule11(S3EndpointParams params, RuleUrl url, String region) {
        if (url.isIp()) {
            return endpointRule12(params, url, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule12(S3EndpointParams params, RuleUrl url, String region) {
        String uriEncodedBucket = RulesFunctions.uriEncode(params.bucket());
        if (uriEncodedBucket != null) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri(url.scheme() + "://" + url.authority() + "/" + uriEncodedBucket + url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule14(S3EndpointParams params, RuleUrl url, String region) {
        if (RulesFunctions.awsIsVirtualHostableS3Bucket(params.bucket(), false)) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri(url.scheme() + "://" + params.bucket() + "." + url.authority() + url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule17(S3EndpointParams params, RuleUrl url, String region) {
        if (url.isIp()) {
            return endpointRule18(params, url, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule18(S3EndpointParams params, RuleUrl url, String region) {
        String uriEncodedBucket = RulesFunctions.uriEncode(params.bucket());
        if (uriEncodedBucket != null) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri(url.scheme() + "://" + url.authority() + "/" + uriEncodedBucket + url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule20(S3EndpointParams params, RuleUrl url, String region) {
        if (RulesFunctions.awsIsVirtualHostableS3Bucket(params.bucket(), false)) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri(url.scheme() + "://" + params.bucket() + "." + url.authority() + url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule23(S3EndpointParams params, String region) {
        if (params.useS3ExpressControlEndpoint() != null && params.useS3ExpressControlEndpoint()) {
            return endpointRule24(params, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule24(S3EndpointParams params, String region) {
        RulePartition partitionResult = RulesFunctions.awsPartition(region);
        if (partitionResult != null) {
            return endpointRule25(params, region, partitionResult);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule25(S3EndpointParams params, String region, RulePartition partitionResult) {
        String uriEncodedBucket = RulesFunctions.uriEncode(params.bucket());
        if (uriEncodedBucket != null) {
            if (params.endpoint() == null) {
                if (params.useFips() && params.useDualStack()) {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(uriFactory.createUri("https://s3express-control-fips.dualstack." + region + "."
                                                                   + partitionResult.dnsSuffix() + "/" + uriEncodedBucket))
                                                   .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(region).build())).build());
                }
                if (params.useFips() && !params.useDualStack()) {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(uriFactory.createUri("https://s3express-control-fips." + region + "." + partitionResult.dnsSuffix() + "/"
                                                                   + uriEncodedBucket))
                                                   .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(region).build())).build());
                }
                if (!params.useFips() && params.useDualStack()) {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(uriFactory.createUri("https://s3express-control.dualstack." + region + "." + partitionResult.dnsSuffix()
                                                                   + "/" + uriEncodedBucket))
                                                   .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(region).build())).build());
                }
                if (!params.useFips() && !params.useDualStack()) {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(uriFactory.createUri("https://s3express-control." + region + "." + partitionResult.dnsSuffix() + "/"
                                                                   + uriEncodedBucket))
                                                   .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(region).build())).build());
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule30(S3EndpointParams params, String region) {
        if (RulesFunctions.awsIsVirtualHostableS3Bucket(params.bucket(), false)) {
            return endpointRule31(params, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule31(S3EndpointParams params, String region) {
        RulePartition partitionResult = RulesFunctions.awsPartition(region);
        if (partitionResult != null) {
            RuleResult result = endpointRule32(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule59(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule64(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule69(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule74(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule79(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Unrecognized S3Express bucket name format.");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule32(S3EndpointParams params, String region, RulePartition partitionResult) {
        if (params.disableS3ExpressSessionAuth() != null && params.disableS3ExpressSessionAuth()) {
            RuleResult result = endpointRule33(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule38(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule43(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule48(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule53(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Unrecognized S3Express bucket name format.");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule33(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 6, 14, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 14, 16, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule38(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 6, 15, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 15, 17, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule43(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 6, 19, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 19, 21, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule48(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 6, 20, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 20, 22, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule53(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 6, 26, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 26, 28, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule59(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 6, 14, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 14, 16, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule64(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 6, 15, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 15, 17, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule69(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 6, 19, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 19, 21, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule74(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 6, 20, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 20, 22, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule79(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 6, 26, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 26, 28, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule86(S3EndpointParams params, String region) {
        if (params.bucket() != null) {
            String accessPointSuffix = RulesFunctions.substring(params.bucket(), 0, 7, true);
            if (accessPointSuffix != null) {
                if ("--xa-s3".equals(accessPointSuffix)) {
                    if (params.accelerate()) {
                        return RuleResult.error("S3Express does not support S3 Accelerate.");
                    }
                    RuleResult result = endpointRule88(params, region);
                    if (result.isResolved()) {
                        return result;
                    }
                    result = endpointRule102(params, region);
                    if (result.isResolved()) {
                        return result;
                    }
                    return RuleResult.error("S3Express bucket name is not a valid virtual hostable name.");
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule88(S3EndpointParams params, String region) {
        if (params.endpoint() != null) {
            RuleUrl url = RulesFunctions.parseURL(params.endpoint());
            if (url != null) {
                RuleResult result = endpointRule89(params, url, region);
                if (result.isResolved()) {
                    return result;
                }
                result = endpointRule96(params, url, region);
                if (result.isResolved()) {
                    return result;
                }
                result = endpointRule99(params, url, region);
                if (result.isResolved()) {
                    return result;
                }
                return RuleResult.error("S3Express bucket name is not a valid virtual hostable name.");
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule89(S3EndpointParams params, RuleUrl url, String region) {
        if (params.disableS3ExpressSessionAuth() != null && params.disableS3ExpressSessionAuth()) {
            RuleResult result = endpointRule90(params, url, region);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule93(params, url, region);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("S3Express bucket name is not a valid virtual hostable name.");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule90(S3EndpointParams params, RuleUrl url, String region) {
        if (url.isIp()) {
            return endpointRule91(params, url, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule91(S3EndpointParams params, RuleUrl url, String region) {
        String uriEncodedBucket = RulesFunctions.uriEncode(params.bucket());
        if (uriEncodedBucket != null) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri(url.scheme() + "://" + url.authority() + "/" + uriEncodedBucket + url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule93(S3EndpointParams params, RuleUrl url, String region) {
        if (RulesFunctions.awsIsVirtualHostableS3Bucket(params.bucket(), false)) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri(url.scheme() + "://" + params.bucket() + "." + url.authority() + url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule96(S3EndpointParams params, RuleUrl url, String region) {
        if (url.isIp()) {
            return endpointRule97(params, url, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule97(S3EndpointParams params, RuleUrl url, String region) {
        String uriEncodedBucket = RulesFunctions.uriEncode(params.bucket());
        if (uriEncodedBucket != null) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri(url.scheme() + "://" + url.authority() + "/" + uriEncodedBucket + url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule99(S3EndpointParams params, RuleUrl url, String region) {
        if (RulesFunctions.awsIsVirtualHostableS3Bucket(params.bucket(), false)) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri(url.scheme() + "://" + params.bucket() + "." + url.authority() + url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule102(S3EndpointParams params, String region) {
        if (RulesFunctions.awsIsVirtualHostableS3Bucket(params.bucket(), false)) {
            return endpointRule103(params, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule103(S3EndpointParams params, String region) {
        RulePartition partitionResult = RulesFunctions.awsPartition(region);
        if (partitionResult != null) {
            RuleResult result = endpointRule104(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule131(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule136(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule141(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule146(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule151(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Unrecognized S3Express bucket name format.");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule104(S3EndpointParams params, String region, RulePartition partitionResult) {
        if (params.disableS3ExpressSessionAuth() != null && params.disableS3ExpressSessionAuth()) {
            RuleResult result = endpointRule105(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule110(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule115(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule120(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule125(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Unrecognized S3Express bucket name format.");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule105(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 7, 15, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 15, 17, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule110(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 7, 16, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 16, 18, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule115(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 7, 20, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 20, 22, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule120(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 7, 21, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 21, 23, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule125(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 7, 27, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 27, 29, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule131(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 7, 15, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 15, 17, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule136(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 7, 16, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 16, 18, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule141(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 7, 20, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 20, 22, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule146(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 7, 21, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 21, 23, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule151(S3EndpointParams params, String region, RulePartition partitionResult) {
        String s3ExpressAvailabilityZoneId = RulesFunctions.substring(params.bucket(), 7, 27, true);
        if (s3ExpressAvailabilityZoneId != null) {
            String s3ExpressAvailabilityZoneDelim = RulesFunctions.substring(params.bucket(), 27, 29, true);
            if (s3ExpressAvailabilityZoneDelim != null) {
                if ("--".equals(s3ExpressAvailabilityZoneDelim)) {
                    if (params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-fips-" + s3ExpressAvailabilityZoneId
                                                                       + "." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId
                                                                       + ".dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                    if (!params.useFips() && !params.useDualStack()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri("https://" + params.bucket() + ".s3express-" + s3ExpressAvailabilityZoneId + "."
                                                                       + region + "." + partitionResult.dnsSuffix()))
                                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                                    .signingName("s3express").signingRegion(region).build())).build());
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule158(S3EndpointParams params, String region) {
        if (params.bucket() == null && params.useS3ExpressControlEndpoint() != null && params.useS3ExpressControlEndpoint()) {
            return endpointRule159(params, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule159(S3EndpointParams params, String region) {
        RulePartition partitionResult = RulesFunctions.awsPartition(region);
        if (partitionResult != null) {
            if (params.endpoint() != null) {
                RuleUrl url = RulesFunctions.parseURL(params.endpoint());
                if (url != null) {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(uriFactory.createUri(url.scheme() + "://" + url.authority() + url.path()))
                                                   .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(region).build())).build());
                }
            }
            if (params.useFips() && params.useDualStack()) {
                return RuleResult
                    .endpoint(Endpoint
                                  .builder()
                                  .url(uriFactory.createUri("https://s3express-control-fips.dualstack." + region + "."
                                                  + partitionResult.dnsSuffix()))
                                  .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                  .putAttribute(
                                      AwsEndpointAttribute.AUTH_SCHEMES,
                                      Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                   .signingName("s3express").signingRegion(region).build())).build());
            }
            if (params.useFips() && !params.useDualStack()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3express-control-fips." + region + "." + partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(region).build())).build());
            }
            if (!params.useFips() && params.useDualStack()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3express-control.dualstack." + region + "." + partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(region).build())).build());
            }
            if (!params.useFips() && !params.useDualStack()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3express-control." + region + "." + partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(region).build())).build());
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule165(S3EndpointParams params, String region) {
        if (params.bucket() != null) {
            String hardwareType = RulesFunctions.substring(params.bucket(), 49, 50, true);
            if (hardwareType != null) {
                String regionPrefix = RulesFunctions.substring(params.bucket(), 8, 12, true);
                if (regionPrefix != null) {
                    String bucketAliasSuffix = RulesFunctions.substring(params.bucket(), 0, 7, true);
                    if (bucketAliasSuffix != null) {
                        String outpostId = RulesFunctions.substring(params.bucket(), 32, 49, true);
                        if (outpostId != null) {
                            RulePartition regionPartition = RulesFunctions.awsPartition(region);
                            if (regionPartition != null) {
                                if ("--op-s3".equals(bucketAliasSuffix)) {
                                    RuleResult result = endpointRule166(params, outpostId, hardwareType, regionPrefix, region,
                                                                        regionPartition);
                                    if (result.isResolved()) {
                                        return result;
                                    }
                                    return RuleResult
                                        .error("Invalid ARN: The outpost Id must only contain a-z, A-Z, 0-9 and `-`.");
                                }
                            }
                        }
                    }
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule166(S3EndpointParams params, String outpostId, String hardwareType,
                                              String regionPrefix, String region, RulePartition regionPartition) {
        if (RulesFunctions.isValidHostLabel(outpostId, false)) {
            RuleResult result = endpointRule167(params, hardwareType, regionPrefix, region, regionPartition);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule172(params, hardwareType, regionPrefix, outpostId, region, regionPartition);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Unrecognized hardware type: \"Expected hardware type o or e but got " + hardwareType + "\"");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule167(S3EndpointParams params, String hardwareType, String regionPrefix, String region,
                                              RulePartition regionPartition) {
        if ("e".equals(hardwareType)) {
            RuleResult result = endpointRule168(params, regionPrefix, region);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri("https://" + params.bucket() + ".ec2.s3-outposts." + region + "."
                                                           + regionPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                             .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                           .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(region).build()))
                                           .build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule168(S3EndpointParams params, String regionPrefix, String region) {
        if ("beta".equals(regionPrefix)) {
            if (params.endpoint() == null) {
                return RuleResult.error("Expected a endpoint to be specified but no endpoint was found");
            }
            if (params.endpoint() != null) {
                RuleUrl url = RulesFunctions.parseURL(params.endpoint());
                if (url != null) {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(uriFactory.createUri("https://" + params.bucket() + ".ec2." + url.authority()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(
                                                           SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                           .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                         .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(region)
                                                                                                                                         .build())).build());
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule172(S3EndpointParams params, String hardwareType, String regionPrefix,
                                              String outpostId, String region, RulePartition regionPartition) {
        if ("o".equals(hardwareType)) {
            RuleResult result = endpointRule173(params, regionPrefix, outpostId, region);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri("https://" + params.bucket() + ".op-" + outpostId + ".s3-outposts." + region + "."
                                                           + regionPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                             .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                           .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(region).build()))
                                           .build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule173(S3EndpointParams params, String regionPrefix, String outpostId, String region) {
        if ("beta".equals(regionPrefix)) {
            if (params.endpoint() == null) {
                return RuleResult.error("Expected a endpoint to be specified but no endpoint was found");
            }
            if (params.endpoint() != null) {
                RuleUrl url = RulesFunctions.parseURL(params.endpoint());
                if (url != null) {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(uriFactory.createUri("https://" + params.bucket() + ".op-" + outpostId + "." + url.authority()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(
                                                           SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                           .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                         .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(region)
                                                                                                                                         .build())).build());
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule179(S3EndpointParams params, String region) {
        if (params.bucket() != null) {
            if (params.endpoint() != null && RulesFunctions.parseURL(params.endpoint()) == null) {
                return RuleResult.error("Custom endpoint `" + params.endpoint() + "` was not a valid URI");
            }
            RuleResult result = endpointRule181(params, region);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule222(params, region);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule227(params, region);
            if (result.isResolved()) {
                return result;
            }
            String arnPrefix = RulesFunctions.substring(params.bucket(), 0, 4, false);
            if (arnPrefix != null) {
                if ("arn:".equals(arnPrefix) && RulesFunctions.awsParseArn(params.bucket()) == null) {
                    return RuleResult.error("Invalid ARN: `" + params.bucket() + "` was not a valid ARN");
                }
            }
            if (params.forcePathStyle() && RulesFunctions.awsParseArn(params.bucket()) != null) {
                return RuleResult.error("Path-style addressing cannot be used with ARN buckets");
            }
            return endpointRule324(params, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule181(S3EndpointParams params, String region) {
        if (!params.forcePathStyle() && RulesFunctions.awsIsVirtualHostableS3Bucket(params.bucket(), false)) {
            return endpointRule182(params, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule182(S3EndpointParams params, String region) {
        RulePartition partitionResult = RulesFunctions.awsPartition(region);
        if (partitionResult != null) {
            RuleResult result = endpointRule183(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid region: region was not a valid DNS name.");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule183(S3EndpointParams params, String region, RulePartition partitionResult) {
        if (RulesFunctions.isValidHostLabel(region, false)) {
            if (params.accelerate() && "aws-cn".equals(partitionResult.name())) {
                return RuleResult.error("S3 Accelerate cannot be used in this region");
            }
            if (params.useDualStack() && params.useFips() && !params.accelerate() && params.endpoint() == null
                && "aws-global".equals(region)) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://" + params.bucket() + ".s3-fips.dualstack.us-east-1."
                                                               + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            }
            RuleResult result = endpointRule186(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            if (params.useDualStack() && params.useFips() && !params.accelerate() && params.endpoint() == null
                && !("aws-global".equals(region)) && !params.useGlobalEndpoint()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://" + params.bucket() + ".s3-fips.dualstack." + region + "."
                                                               + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(region).build())).build());
            }
            if (!params.useDualStack() && params.useFips() && !params.accelerate() && params.endpoint() == null
                && "aws-global".equals(region)) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://" + params.bucket() + ".s3-fips.us-east-1." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            }
            result = endpointRule190(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            if (!params.useDualStack() && params.useFips() && !params.accelerate() && params.endpoint() == null
                && !("aws-global".equals(region)) && !params.useGlobalEndpoint()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://" + params.bucket() + ".s3-fips." + region + "." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(region).build())).build());
            }
            if (params.useDualStack() && !params.useFips() && params.accelerate() && params.endpoint() == null
                && "aws-global".equals(region)) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://" + params.bucket() + ".s3-accelerate.dualstack.us-east-1."
                                                               + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            }
            result = endpointRule194(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            if (params.useDualStack() && !params.useFips() && params.accelerate() && params.endpoint() == null
                && !("aws-global".equals(region)) && !params.useGlobalEndpoint()) {
                return RuleResult
                    .endpoint(Endpoint
                                  .builder()
                                  .url(uriFactory.createUri("https://" + params.bucket() + ".s3-accelerate.dualstack."
                                                  + partitionResult.dnsSuffix()))
                                  .putAttribute(
                                      AwsEndpointAttribute.AUTH_SCHEMES,
                                      Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                   .signingRegion(region).build())).build());
            }
            if (params.useDualStack() && !params.useFips() && !params.accelerate() && params.endpoint() == null
                && "aws-global".equals(region)) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://" + params.bucket() + ".s3.dualstack.us-east-1." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            }
            result = endpointRule198(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            if (params.useDualStack() && !params.useFips() && !params.accelerate() && params.endpoint() == null
                && !("aws-global".equals(region)) && !params.useGlobalEndpoint()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://" + params.bucket() + ".s3.dualstack." + region + "."
                                                               + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(region).build())).build());
            }
            if (!params.useDualStack() && !params.useFips() && !params.accelerate() && params.endpoint() != null) {
                RuleUrl url = RulesFunctions.parseURL(params.endpoint());
                if (url != null) {
                    if (url.isIp() && "aws-global".equals(region)) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri(url.scheme() + "://" + url.authority() + url.normalizedPath() + params.bucket()))
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                        .signingRegion("us-east-1").build())).build());
                    }
                }
            }
            if (!params.useDualStack() && !params.useFips() && !params.accelerate() && params.endpoint() != null) {
                RuleUrl url = RulesFunctions.parseURL(params.endpoint());
                if (url != null) {
                    if (!url.isIp() && "aws-global".equals(region)) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri(url.scheme() + "://" + params.bucket() + "." + url.authority() + url.path()))
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                        .signingRegion("us-east-1").build())).build());
                    }
                }
            }
            result = endpointRule203(params, region);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule206(params, region);
            if (result.isResolved()) {
                return result;
            }
            if (!params.useDualStack() && !params.useFips() && !params.accelerate() && params.endpoint() != null) {
                RuleUrl url = RulesFunctions.parseURL(params.endpoint());
                if (url != null) {
                    if (url.isIp() && !("aws-global".equals(region)) && !params.useGlobalEndpoint()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri(url.scheme() + "://" + url.authority() + url.normalizedPath() + params.bucket()))
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                        .signingRegion(region).build())).build());
                    }
                }
            }
            if (!params.useDualStack() && !params.useFips() && !params.accelerate() && params.endpoint() != null) {
                RuleUrl url = RulesFunctions.parseURL(params.endpoint());
                if (url != null) {
                    if (!url.isIp() && !("aws-global".equals(region)) && !params.useGlobalEndpoint()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri(url.scheme() + "://" + params.bucket() + "." + url.authority() + url.path()))
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                        .signingRegion(region).build())).build());
                    }
                }
            }
            if (!params.useDualStack() && !params.useFips() && params.accelerate() && params.endpoint() == null
                && "aws-global".equals(region)) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://" + params.bucket() + ".s3-accelerate." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            }
            result = endpointRule212(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            if (!params.useDualStack() && !params.useFips() && params.accelerate() && params.endpoint() == null
                && !("aws-global".equals(region)) && !params.useGlobalEndpoint()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://" + params.bucket() + ".s3-accelerate." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(region).build())).build());
            }
            if (!params.useDualStack() && !params.useFips() && !params.accelerate() && params.endpoint() == null
                && "aws-global".equals(region)) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://" + params.bucket() + ".s3." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            }
            result = endpointRule217(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            if (!params.useDualStack() && !params.useFips() && !params.accelerate() && params.endpoint() == null
                && !("aws-global".equals(region)) && !params.useGlobalEndpoint()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://" + params.bucket() + ".s3." + region + "." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(region).build())).build());
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule186(S3EndpointParams params, String region, RulePartition partitionResult) {
        if (params.useDualStack() && params.useFips() && !params.accelerate() && params.endpoint() == null
            && !("aws-global".equals(region)) && params.useGlobalEndpoint()) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri("https://" + params.bucket() + ".s3-fips.dualstack." + region + "."
                                                           + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule190(S3EndpointParams params, String region, RulePartition partitionResult) {
        if (!params.useDualStack() && params.useFips() && !params.accelerate() && params.endpoint() == null
            && !("aws-global".equals(region)) && params.useGlobalEndpoint()) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri("https://" + params.bucket() + ".s3-fips." + region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule194(S3EndpointParams params, String region, RulePartition partitionResult) {
        if (params.useDualStack() && !params.useFips() && params.accelerate() && params.endpoint() == null
            && !("aws-global".equals(region)) && params.useGlobalEndpoint()) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri("https://" + params.bucket() + ".s3-accelerate.dualstack." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule198(S3EndpointParams params, String region, RulePartition partitionResult) {
        if (params.useDualStack() && !params.useFips() && !params.accelerate() && params.endpoint() == null
            && !("aws-global".equals(region)) && params.useGlobalEndpoint()) {
            return RuleResult
                .endpoint(Endpoint
                              .builder()
                              .url(uriFactory.createUri("https://" + params.bucket() + ".s3.dualstack." + region + "."
                                              + partitionResult.dnsSuffix()))
                              .putAttribute(
                                  AwsEndpointAttribute.AUTH_SCHEMES,
                                  Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                               .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule203(S3EndpointParams params, String region) {
        if (!params.useDualStack() && !params.useFips() && !params.accelerate() && params.endpoint() != null) {
            RuleUrl url = RulesFunctions.parseURL(params.endpoint());
            if (url != null) {
                if (url.isIp() && !("aws-global".equals(region)) && params.useGlobalEndpoint()) {
                    if ("us-east-1".equals(region)) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri(url.scheme() + "://" + url.authority() + url.normalizedPath() + params.bucket()))
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                        .signingRegion(region).build())).build());
                    }
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(uriFactory.createUri(url.scheme() + "://" + url.authority() + url.normalizedPath() + params.bucket()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule206(S3EndpointParams params, String region) {
        if (!params.useDualStack() && !params.useFips() && !params.accelerate() && params.endpoint() != null) {
            RuleUrl url = RulesFunctions.parseURL(params.endpoint());
            if (url != null) {
                if (!url.isIp() && !("aws-global".equals(region)) && params.useGlobalEndpoint()) {
                    if ("us-east-1".equals(region)) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri(url.scheme() + "://" + params.bucket() + "." + url.authority() + url.path()))
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                        .signingRegion(region).build())).build());
                    }
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(uriFactory.createUri(url.scheme() + "://" + params.bucket() + "." + url.authority() + url.path()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule212(S3EndpointParams params, String region, RulePartition partitionResult) {
        if (!params.useDualStack() && !params.useFips() && params.accelerate() && params.endpoint() == null
            && !("aws-global".equals(region)) && params.useGlobalEndpoint()) {
            if ("us-east-1".equals(region)) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://" + params.bucket() + ".s3-accelerate." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(region).build())).build());
            }
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri("https://" + params.bucket() + ".s3-accelerate." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule217(S3EndpointParams params, String region, RulePartition partitionResult) {
        if (!params.useDualStack() && !params.useFips() && !params.accelerate() && params.endpoint() == null
            && !("aws-global".equals(region)) && params.useGlobalEndpoint()) {
            if ("us-east-1".equals(region)) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://" + params.bucket() + ".s3." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(region).build())).build());
            }
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri("https://" + params.bucket() + ".s3." + region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule222(S3EndpointParams params, String region) {
        if (params.endpoint() != null) {
            RuleUrl url = RulesFunctions.parseURL(params.endpoint());
            if (url != null) {
                if ("http".equals(url.scheme()) && RulesFunctions.awsIsVirtualHostableS3Bucket(params.bucket(), true)
                    && !params.forcePathStyle() && !params.useFips() && !params.useDualStack() && !params.accelerate()) {
                    return endpointRule223(params, region, url);
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule223(S3EndpointParams params, String region, RuleUrl url) {
        RulePartition partitionResult = RulesFunctions.awsPartition(region);
        if (partitionResult != null) {
            RuleResult result = endpointRule224(params, region, url);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid region: region was not a valid DNS name.");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule224(S3EndpointParams params, String region, RuleUrl url) {
        if (RulesFunctions.isValidHostLabel(region, false)) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri(url.scheme() + "://" + params.bucket() + "." + url.authority() + url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule227(S3EndpointParams params, String region) {
        if (!params.forcePathStyle()) {
            RuleArn bucketArn = RulesFunctions.awsParseArn(params.bucket());
            if (bucketArn != null) {
                RuleResult result = endpointRule228(params, bucketArn, region);
                if (result.isResolved()) {
                    return result;
                }
                return RuleResult.error("Invalid ARN: No ARN type specified");
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule228(S3EndpointParams params, RuleArn bucketArn, String region) {
        String arnType = RulesFunctions.listAccess(bucketArn.resourceId(), 0);
        if (arnType != null) {
            if (!("".equals(arnType))) {
                RuleResult result = endpointRule229(params, bucketArn, arnType, region);
                if (result.isResolved()) {
                    return result;
                }
                result = endpointRule256(params, arnType, bucketArn, region);
                if (result.isResolved()) {
                    return result;
                }
                result = endpointRule294(params, bucketArn, region);
                if (result.isResolved()) {
                    return result;
                }
                return RuleResult.error("Invalid ARN: Unrecognized format: " + params.bucket() + " (type: " + arnType + ")");
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule229(S3EndpointParams params, RuleArn bucketArn, String arnType, String region) {
        if ("s3-object-lambda".equals(bucketArn.service())) {
            RuleResult result = endpointRule230(params, arnType, bucketArn, region);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid ARN: Object Lambda ARNs only support `accesspoint` arn types, but found: `"
                                    + arnType + "`");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule230(S3EndpointParams params, String arnType, RuleArn bucketArn, String region) {
        if ("accesspoint".equals(arnType)) {
            RuleResult result = endpointRule231(params, bucketArn, region);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult
                .error("Invalid ARN: Expected a resource of the format `accesspoint:<accesspoint name>` but no name was provided");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule231(S3EndpointParams params, RuleArn bucketArn, String region) {
        String accessPointName = RulesFunctions.listAccess(bucketArn.resourceId(), 1);
        if (accessPointName != null) {
            if (!("".equals(accessPointName))) {
                if (params.useDualStack()) {
                    return RuleResult.error("S3 Object Lambda does not support Dual-stack");
                }
                if (params.accelerate()) {
                    return RuleResult.error("S3 Object Lambda does not support S3 Accelerate");
                }
                RuleResult result = endpointRule234(params, bucketArn, region, accessPointName);
                if (result.isResolved()) {
                    return result;
                }
                return RuleResult.error("Invalid ARN: bucket ARN is missing a region");
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule234(S3EndpointParams params, RuleArn bucketArn, String region, String accessPointName) {
        if (!("".equals(bucketArn.region()))) {
            if (params.disableAccessPoints() != null && params.disableAccessPoints()) {
                return RuleResult.error("Access points are not supported for this operation");
            }
            RuleResult result = endpointRule236(params, bucketArn, region, accessPointName);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid ARN: The ARN may only contain a single resource component after `accesspoint`.");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule236(S3EndpointParams params, RuleArn bucketArn, String region, String accessPointName) {
        if (RulesFunctions.listAccess(bucketArn.resourceId(), 2) == null) {
            if (params.useArnRegion() != null && !params.useArnRegion()
                && !(RulesFunctions.stringEquals(bucketArn.region(), region))) {
                return RuleResult.error("Invalid configuration: region from ARN `" + bucketArn.region()
                                        + "` does not match client region `" + region + "` and UseArnRegion is `false`");
            }
            return endpointRule238(params, bucketArn, region, accessPointName);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule238(S3EndpointParams params, RuleArn bucketArn, String region, String accessPointName) {
        RulePartition bucketPartition = RulesFunctions.awsPartition(bucketArn.region());
        if (bucketPartition != null) {
            return endpointRule239(params, region, bucketPartition, bucketArn, accessPointName);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule239(S3EndpointParams params, String region, RulePartition bucketPartition,
                                              RuleArn bucketArn, String accessPointName) {
        RulePartition partitionResult = RulesFunctions.awsPartition(region);
        if (partitionResult != null) {
            RuleResult result = endpointRule240(params, bucketPartition, partitionResult, bucketArn, accessPointName);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Client was configured for partition `" + partitionResult.name() + "` but ARN (`"
                                    + params.bucket() + "`) has `" + bucketPartition.name() + "`");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule240(S3EndpointParams params, RulePartition bucketPartition,
                                              RulePartition partitionResult, RuleArn bucketArn, String accessPointName) {
        if (RulesFunctions.stringEquals(bucketPartition.name(), partitionResult.name())) {
            RuleResult result = endpointRule241(params, bucketArn, accessPointName, bucketPartition);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid region in ARN: `" + bucketArn.region() + "` (invalid DNS name)");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule241(S3EndpointParams params, RuleArn bucketArn, String accessPointName,
                                              RulePartition bucketPartition) {
        if (RulesFunctions.isValidHostLabel(bucketArn.region(), true)) {
            if ("".equals(bucketArn.accountId())) {
                return RuleResult.error("Invalid ARN: Missing account id");
            }
            RuleResult result = endpointRule243(params, bucketArn, accessPointName, bucketPartition);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid ARN: The account id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                    + bucketArn.accountId() + "`");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule243(S3EndpointParams params, RuleArn bucketArn, String accessPointName,
                                              RulePartition bucketPartition) {
        if (RulesFunctions.isValidHostLabel(bucketArn.accountId(), false)) {
            RuleResult result = endpointRule244(params, accessPointName, bucketArn, bucketPartition);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid ARN: The access point name may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                    + accessPointName + "`");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule244(S3EndpointParams params, String accessPointName, RuleArn bucketArn,
                                              RulePartition bucketPartition) {
        if (RulesFunctions.isValidHostLabel(accessPointName, false)) {
            if (params.endpoint() != null) {
                RuleUrl url = RulesFunctions.parseURL(params.endpoint());
                if (url != null) {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(uriFactory.createUri(url.scheme() + "://" + accessPointName + "-" + bucketArn.accountId() + "."
                                                                   + url.authority() + url.path()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                    .signingName("s3-object-lambda").signingRegion(bucketArn.region()).build())).build());
                }
            }
            if (params.useFips()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://" + accessPointName + "-" + bucketArn.accountId() + ".s3-object-lambda-fips."
                                                               + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                .signingName("s3-object-lambda").signingRegion(bucketArn.region()).build())).build());
            }
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri("https://" + accessPointName + "-" + bucketArn.accountId() + ".s3-object-lambda."
                                                           + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                            .signingRegion(bucketArn.region()).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule256(S3EndpointParams params, String arnType, RuleArn bucketArn, String region) {
        if ("accesspoint".equals(arnType)) {
            RuleResult result = endpointRule257(params, bucketArn, arnType, region);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult
                .error("Invalid ARN: Expected a resource of the format `accesspoint:<accesspoint name>` but no name was provided");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule257(S3EndpointParams params, RuleArn bucketArn, String arnType, String region) {
        String accessPointName = RulesFunctions.listAccess(bucketArn.resourceId(), 1);
        if (accessPointName != null) {
            if (!("".equals(accessPointName))) {
                RuleResult result = endpointRule258(params, bucketArn, arnType, region, accessPointName);
                if (result.isResolved()) {
                    return result;
                }
                result = endpointRule283(params, accessPointName, region, bucketArn);
                if (result.isResolved()) {
                    return result;
                }
                return RuleResult.error("Invalid Access Point Name");
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule258(S3EndpointParams params, RuleArn bucketArn, String arnType, String region,
                                              String accessPointName) {
        if (!("".equals(bucketArn.region()))) {
            return endpointRule259(params, arnType, bucketArn, region, accessPointName);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule259(S3EndpointParams params, String arnType, RuleArn bucketArn, String region,
                                              String accessPointName) {
        if ("accesspoint".equals(arnType)) {
            return endpointRule260(params, bucketArn, region, accessPointName);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule260(S3EndpointParams params, RuleArn bucketArn, String region, String accessPointName) {
        if (!("".equals(bucketArn.region()))) {
            if (params.disableAccessPoints() != null && params.disableAccessPoints()) {
                return RuleResult.error("Access points are not supported for this operation");
            }
            RuleResult result = endpointRule262(params, bucketArn, region, accessPointName);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid ARN: The ARN may only contain a single resource component after `accesspoint`.");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule262(S3EndpointParams params, RuleArn bucketArn, String region, String accessPointName) {
        if (RulesFunctions.listAccess(bucketArn.resourceId(), 2) == null) {
            if (params.useArnRegion() != null && !params.useArnRegion()
                && !(RulesFunctions.stringEquals(bucketArn.region(), region))) {
                return RuleResult.error("Invalid configuration: region from ARN `" + bucketArn.region()
                                        + "` does not match client region `" + region + "` and UseArnRegion is `false`");
            }
            return endpointRule264(params, bucketArn, region, accessPointName);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule264(S3EndpointParams params, RuleArn bucketArn, String region, String accessPointName) {
        RulePartition bucketPartition = RulesFunctions.awsPartition(bucketArn.region());
        if (bucketPartition != null) {
            return endpointRule265(params, region, bucketPartition, bucketArn, accessPointName);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule265(S3EndpointParams params, String region, RulePartition bucketPartition,
                                              RuleArn bucketArn, String accessPointName) {
        RulePartition partitionResult = RulesFunctions.awsPartition(region);
        if (partitionResult != null) {
            RuleResult result = endpointRule266(params, bucketPartition, partitionResult, bucketArn, accessPointName);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Client was configured for partition `" + partitionResult.name() + "` but ARN (`"
                                    + params.bucket() + "`) has `" + bucketPartition.name() + "`");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule266(S3EndpointParams params, RulePartition bucketPartition,
                                              RulePartition partitionResult, RuleArn bucketArn, String accessPointName) {
        if (RulesFunctions.stringEquals(bucketPartition.name(), partitionResult.name())) {
            RuleResult result = endpointRule267(params, bucketArn, accessPointName, bucketPartition);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid region in ARN: `" + bucketArn.region() + "` (invalid DNS name)");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule267(S3EndpointParams params, RuleArn bucketArn, String accessPointName,
                                              RulePartition bucketPartition) {
        if (RulesFunctions.isValidHostLabel(bucketArn.region(), true)) {
            RuleResult result = endpointRule268(params, bucketArn, accessPointName, bucketPartition);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid ARN: The ARN was not for the S3 service, found: " + bucketArn.service());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule268(S3EndpointParams params, RuleArn bucketArn, String accessPointName,
                                              RulePartition bucketPartition) {
        if ("s3".equals(bucketArn.service())) {
            RuleResult result = endpointRule269(params, bucketArn, accessPointName, bucketPartition);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid ARN: The account id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                    + bucketArn.accountId() + "`");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule269(S3EndpointParams params, RuleArn bucketArn, String accessPointName,
                                              RulePartition bucketPartition) {
        if (RulesFunctions.isValidHostLabel(bucketArn.accountId(), false)) {
            RuleResult result = endpointRule270(params, accessPointName, bucketArn, bucketPartition);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid ARN: The access point name may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                    + accessPointName + "`");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule270(S3EndpointParams params, String accessPointName, RuleArn bucketArn,
                                              RulePartition bucketPartition) {
        if (RulesFunctions.isValidHostLabel(accessPointName, false)) {
            if (params.accelerate()) {
                return RuleResult.error("Access Points do not support S3 Accelerate");
            }
            if (params.useFips() && params.useDualStack()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://" + accessPointName + "-" + bucketArn.accountId()
                                                               + ".s3-accesspoint-fips.dualstack." + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(bucketArn.region()).build())).build());
            }
            if (params.useFips() && !params.useDualStack()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://" + accessPointName + "-" + bucketArn.accountId() + ".s3-accesspoint-fips."
                                                               + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(bucketArn.region()).build())).build());
            }
            if (!params.useFips() && params.useDualStack()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://" + accessPointName + "-" + bucketArn.accountId() + ".s3-accesspoint.dualstack."
                                                               + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(bucketArn.region()).build())).build());
            }
            if (!params.useFips() && !params.useDualStack() && params.endpoint() != null) {
                RuleUrl url = RulesFunctions.parseURL(params.endpoint());
                if (url != null) {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(uriFactory.createUri(url.scheme() + "://" + accessPointName + "-" + bucketArn.accountId() + "."
                                                                   + url.authority() + url.path()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(bucketArn.region()).build())).build());
                }
            }
            if (!params.useFips() && !params.useDualStack()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://" + accessPointName + "-" + bucketArn.accountId() + ".s3-accesspoint."
                                                               + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(bucketArn.region()).build())).build());
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule283(S3EndpointParams params, String accessPointName, String region, RuleArn bucketArn) {
        if (RulesFunctions.isValidHostLabel(accessPointName, true)) {
            if (params.useDualStack()) {
                return RuleResult.error("S3 MRAP does not support dual-stack");
            }
            if (params.useFips()) {
                return RuleResult.error("S3 MRAP does not support FIPS");
            }
            if (params.accelerate()) {
                return RuleResult.error("S3 MRAP does not support S3 Accelerate");
            }
            if (params.disableMultiRegionAccessPoints()) {
                return RuleResult.error("Invalid configuration: Multi-Region Access Point ARNs are disabled.");
            }
            return endpointRule288(params, region, bucketArn, accessPointName);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule288(S3EndpointParams params, String region, RuleArn bucketArn, String accessPointName) {
        RulePartition mrapPartition = RulesFunctions.awsPartition(region);
        if (mrapPartition != null) {
            RuleResult result = endpointRule289(params, mrapPartition, bucketArn, accessPointName);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Client was configured for partition `" + mrapPartition.name()
                                    + "` but bucket referred to partition `" + bucketArn.partition() + "`");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule289(S3EndpointParams params, RulePartition mrapPartition, RuleArn bucketArn,
                                              String accessPointName) {
        if (RulesFunctions.stringEquals(mrapPartition.name(), bucketArn.partition())) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri("https://" + accessPointName + ".accesspoint.s3-global." + mrapPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                             .signingRegionSet(Arrays.asList("*")).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule294(S3EndpointParams params, RuleArn bucketArn, String region) {
        if ("s3-outposts".equals(bucketArn.service())) {
            if (params.useDualStack()) {
                return RuleResult.error("S3 Outposts does not support Dual-stack");
            }
            if (params.useFips()) {
                return RuleResult.error("S3 Outposts does not support FIPS");
            }
            if (params.accelerate()) {
                return RuleResult.error("S3 Outposts does not support S3 Accelerate");
            }
            if (RulesFunctions.listAccess(bucketArn.resourceId(), 4) != null) {
                return RuleResult.error("Invalid Arn: Outpost Access Point ARN contains sub resources");
            }
            RuleResult result = endpointRule299(params, bucketArn, region);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid ARN: The Outpost Id was not set");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule299(S3EndpointParams params, RuleArn bucketArn, String region) {
        String outpostId = RulesFunctions.listAccess(bucketArn.resourceId(), 1);
        if (outpostId != null) {
            RuleResult result = endpointRule300(params, outpostId, bucketArn, region);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid ARN: The outpost Id may only contain a-z, A-Z, 0-9 and `-`. Found: `" + outpostId
                                    + "`");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule300(S3EndpointParams params, String outpostId, RuleArn bucketArn, String region) {
        if (RulesFunctions.isValidHostLabel(outpostId, false)) {
            if (params.useArnRegion() != null && !params.useArnRegion()
                && !(RulesFunctions.stringEquals(bucketArn.region(), region))) {
                return RuleResult.error("Invalid configuration: region from ARN `" + bucketArn.region()
                                        + "` does not match client region `" + region + "` and UseArnRegion is `false`");
            }
            return endpointRule302(params, bucketArn, region, outpostId);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule302(S3EndpointParams params, RuleArn bucketArn, String region, String outpostId) {
        RulePartition bucketPartition = RulesFunctions.awsPartition(bucketArn.region());
        if (bucketPartition != null) {
            return endpointRule303(params, region, bucketPartition, bucketArn, outpostId);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule303(S3EndpointParams params, String region, RulePartition bucketPartition,
                                              RuleArn bucketArn, String outpostId) {
        RulePartition partitionResult = RulesFunctions.awsPartition(region);
        if (partitionResult != null) {
            RuleResult result = endpointRule304(params, bucketPartition, partitionResult, bucketArn, outpostId);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Client was configured for partition `" + partitionResult.name() + "` but ARN (`"
                                    + params.bucket() + "`) has `" + bucketPartition.name() + "`");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule304(S3EndpointParams params, RulePartition bucketPartition,
                                              RulePartition partitionResult, RuleArn bucketArn, String outpostId) {
        if (RulesFunctions.stringEquals(bucketPartition.name(), partitionResult.name())) {
            RuleResult result = endpointRule305(params, bucketArn, outpostId, bucketPartition);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid region in ARN: `" + bucketArn.region() + "` (invalid DNS name)");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule305(S3EndpointParams params, RuleArn bucketArn, String outpostId,
                                              RulePartition bucketPartition) {
        if (RulesFunctions.isValidHostLabel(bucketArn.region(), true)) {
            RuleResult result = endpointRule306(params, bucketArn, outpostId, bucketPartition);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid ARN: The account id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                    + bucketArn.accountId() + "`");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule306(S3EndpointParams params, RuleArn bucketArn, String outpostId,
                                              RulePartition bucketPartition) {
        if (RulesFunctions.isValidHostLabel(bucketArn.accountId(), false)) {
            RuleResult result = endpointRule307(params, bucketArn, outpostId, bucketPartition);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid ARN: Expected a 4-component resource");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule307(S3EndpointParams params, RuleArn bucketArn, String outpostId,
                                              RulePartition bucketPartition) {
        String outpostType = RulesFunctions.listAccess(bucketArn.resourceId(), 2);
        if (outpostType != null) {
            RuleResult result = endpointRule308(params, bucketArn, outpostType, outpostId, bucketPartition);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid ARN: expected an access point name");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule308(S3EndpointParams params, RuleArn bucketArn, String outpostType, String outpostId,
                                              RulePartition bucketPartition) {
        String accessPointName = RulesFunctions.listAccess(bucketArn.resourceId(), 3);
        if (accessPointName != null) {
            RuleResult result = endpointRule309(params, outpostType, accessPointName, bucketArn, outpostId, bucketPartition);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Expected an outpost type `accesspoint`, found " + outpostType);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule309(S3EndpointParams params, String outpostType, String accessPointName,
                                              RuleArn bucketArn, String outpostId, RulePartition bucketPartition) {
        if ("accesspoint".equals(outpostType)) {
            if (params.endpoint() != null) {
                RuleUrl url = RulesFunctions.parseURL(params.endpoint());
                if (url != null) {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(uriFactory.createUri("https://" + accessPointName + "-" + bucketArn.accountId() + "." + outpostId + "."
                                                                   + url.authority()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(
                                                           SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                           .signingRegionSet(Arrays.asList("*")).build(),
                                                           SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                          .signingRegion(bucketArn.region()).build())).build());
                }
            }
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri("https://" + accessPointName + "-" + bucketArn.accountId() + "." + outpostId
                                                           + ".s3-outposts." + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                             .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                           .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(bucketArn.region())
                                                                                                                                           .build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule324(S3EndpointParams params, String region) {
        String uriEncodedBucket = RulesFunctions.uriEncode(params.bucket());
        if (uriEncodedBucket != null) {
            return endpointRule325(params, region, uriEncodedBucket);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule325(S3EndpointParams params, String region, String uriEncodedBucket) {
        RulePartition partitionResult = RulesFunctions.awsPartition(region);
        if (partitionResult != null) {
            RuleResult result = endpointRule326(params, region, partitionResult, uriEncodedBucket);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Path-style addressing cannot be used with S3 Accelerate");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule326(S3EndpointParams params, String region, RulePartition partitionResult,
                                              String uriEncodedBucket) {
        if (!params.accelerate()) {
            if (params.useDualStack() && params.endpoint() == null && params.useFips() && "aws-global".equals(region)) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3-fips.dualstack.us-east-1." + partitionResult.dnsSuffix() + "/"
                                                               + uriEncodedBucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            }
            RuleResult result = endpointRule328(params, region, partitionResult, uriEncodedBucket);
            if (result.isResolved()) {
                return result;
            }
            if (params.useDualStack() && params.endpoint() == null && params.useFips() && !("aws-global".equals(region))
                && !params.useGlobalEndpoint()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3-fips.dualstack." + region + "." + partitionResult.dnsSuffix() + "/"
                                                               + uriEncodedBucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(region).build())).build());
            }
            if (!params.useDualStack() && params.endpoint() == null && params.useFips() && "aws-global".equals(region)) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3-fips.us-east-1." + partitionResult.dnsSuffix() + "/" + uriEncodedBucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            }
            result = endpointRule332(params, region, partitionResult, uriEncodedBucket);
            if (result.isResolved()) {
                return result;
            }
            if (!params.useDualStack() && params.endpoint() == null && params.useFips() && !("aws-global".equals(region))
                && !params.useGlobalEndpoint()) {
                return RuleResult
                    .endpoint(Endpoint
                                  .builder()
                                  .url(uriFactory.createUri("https://s3-fips." + region + "." + partitionResult.dnsSuffix() + "/"
                                                  + uriEncodedBucket))
                                  .putAttribute(
                                      AwsEndpointAttribute.AUTH_SCHEMES,
                                      Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                   .signingRegion(region).build())).build());
            }
            if (params.useDualStack() && params.endpoint() == null && !params.useFips() && "aws-global".equals(region)) {
                return RuleResult
                    .endpoint(Endpoint
                                  .builder()
                                  .url(uriFactory.createUri("https://s3.dualstack.us-east-1." + partitionResult.dnsSuffix() + "/"
                                                  + uriEncodedBucket))
                                  .putAttribute(
                                      AwsEndpointAttribute.AUTH_SCHEMES,
                                      Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                   .signingRegion("us-east-1").build())).build());
            }
            result = endpointRule336(params, region, partitionResult, uriEncodedBucket);
            if (result.isResolved()) {
                return result;
            }
            if (params.useDualStack() && params.endpoint() == null && !params.useFips() && !("aws-global".equals(region))
                && !params.useGlobalEndpoint()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3.dualstack." + region + "." + partitionResult.dnsSuffix() + "/"
                                                               + uriEncodedBucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(region).build())).build());
            }
            if (!params.useDualStack() && params.endpoint() != null) {
                RuleUrl url = RulesFunctions.parseURL(params.endpoint());
                if (url != null) {
                    if (!params.useFips() && "aws-global".equals(region)) {
                        return RuleResult
                            .endpoint(Endpoint
                                          .builder()
                                          .url(uriFactory.createUri(url.scheme() + "://" + url.authority() + url.normalizedPath()
                                                          + uriEncodedBucket))
                                          .putAttribute(
                                              AwsEndpointAttribute.AUTH_SCHEMES,
                                              Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                           .signingName("s3").signingRegion("us-east-1").build())).build());
                    }
                }
            }
            result = endpointRule340(params, region, uriEncodedBucket);
            if (result.isResolved()) {
                return result;
            }
            if (!params.useDualStack() && params.endpoint() != null) {
                RuleUrl url = RulesFunctions.parseURL(params.endpoint());
                if (url != null) {
                    if (!params.useFips() && !("aws-global".equals(region)) && !params.useGlobalEndpoint()) {
                        return RuleResult
                            .endpoint(Endpoint
                                          .builder()
                                          .url(uriFactory.createUri(url.scheme() + "://" + url.authority() + url.normalizedPath()
                                                          + uriEncodedBucket))
                                          .putAttribute(
                                              AwsEndpointAttribute.AUTH_SCHEMES,
                                              Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                           .signingName("s3").signingRegion(region).build())).build());
                    }
                }
            }
            if (!params.useDualStack() && params.endpoint() == null && !params.useFips() && "aws-global".equals(region)) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3." + partitionResult.dnsSuffix() + "/" + uriEncodedBucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            }
            result = endpointRule345(params, region, partitionResult, uriEncodedBucket);
            if (result.isResolved()) {
                return result;
            }
            if (!params.useDualStack() && params.endpoint() == null && !params.useFips() && !("aws-global".equals(region))
                && !params.useGlobalEndpoint()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3." + region + "." + partitionResult.dnsSuffix() + "/" + uriEncodedBucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(region).build())).build());
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule328(S3EndpointParams params, String region, RulePartition partitionResult,
                                              String uriEncodedBucket) {
        if (params.useDualStack() && params.endpoint() == null && params.useFips() && !("aws-global".equals(region))
            && params.useGlobalEndpoint()) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri("https://s3-fips.dualstack." + region + "." + partitionResult.dnsSuffix() + "/"
                                                           + uriEncodedBucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule332(S3EndpointParams params, String region, RulePartition partitionResult,
                                              String uriEncodedBucket) {
        if (!params.useDualStack() && params.endpoint() == null && params.useFips() && !("aws-global".equals(region))
            && params.useGlobalEndpoint()) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri("https://s3-fips." + region + "." + partitionResult.dnsSuffix() + "/" + uriEncodedBucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule336(S3EndpointParams params, String region, RulePartition partitionResult,
                                              String uriEncodedBucket) {
        if (params.useDualStack() && params.endpoint() == null && !params.useFips() && !("aws-global".equals(region))
            && params.useGlobalEndpoint()) {
            return RuleResult
                .endpoint(Endpoint
                              .builder()
                              .url(uriFactory.createUri("https://s3.dualstack." + region + "." + partitionResult.dnsSuffix() + "/"
                                              + uriEncodedBucket))
                              .putAttribute(
                                  AwsEndpointAttribute.AUTH_SCHEMES,
                                  Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                               .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule340(S3EndpointParams params, String region, String uriEncodedBucket) {
        if (!params.useDualStack() && params.endpoint() != null) {
            RuleUrl url = RulesFunctions.parseURL(params.endpoint());
            if (url != null) {
                if (!params.useFips() && !("aws-global".equals(region)) && params.useGlobalEndpoint()) {
                    if ("us-east-1".equals(region)) {
                        return RuleResult
                            .endpoint(Endpoint
                                          .builder()
                                          .url(uriFactory.createUri(url.scheme() + "://" + url.authority() + url.normalizedPath()
                                                          + uriEncodedBucket))
                                          .putAttribute(
                                              AwsEndpointAttribute.AUTH_SCHEMES,
                                              Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                           .signingName("s3").signingRegion(region).build())).build());
                    }
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(uriFactory.createUri(url.scheme() + "://" + url.authority() + url.normalizedPath() + uriEncodedBucket))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule345(S3EndpointParams params, String region, RulePartition partitionResult,
                                              String uriEncodedBucket) {
        if (!params.useDualStack() && params.endpoint() == null && !params.useFips() && !("aws-global".equals(region))
            && params.useGlobalEndpoint()) {
            if ("us-east-1".equals(region)) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3." + partitionResult.dnsSuffix() + "/" + uriEncodedBucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(region).build())).build());
            }
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri("https://s3." + region + "." + partitionResult.dnsSuffix() + "/" + uriEncodedBucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule350(S3EndpointParams params, String region) {
        if (params.useObjectLambdaEndpoint() != null && params.useObjectLambdaEndpoint()) {
            return endpointRule351(params, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule351(S3EndpointParams params, String region) {
        RulePartition partitionResult = RulesFunctions.awsPartition(region);
        if (partitionResult != null) {
            RuleResult result = endpointRule352(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid region: region was not a valid DNS name.");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule352(S3EndpointParams params, String region, RulePartition partitionResult) {
        if (RulesFunctions.isValidHostLabel(region, true)) {
            if (params.useDualStack()) {
                return RuleResult.error("S3 Object Lambda does not support Dual-stack");
            }
            if (params.accelerate()) {
                return RuleResult.error("S3 Object Lambda does not support S3 Accelerate");
            }
            if (params.endpoint() != null) {
                RuleUrl url = RulesFunctions.parseURL(params.endpoint());
                if (url != null) {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(uriFactory.createUri(url.scheme() + "://" + url.authority() + url.path()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                    .signingName("s3-object-lambda").signingRegion(region).build())).build());
                }
            }
            if (params.useFips()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3-object-lambda-fips." + region + "." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                .signingName("s3-object-lambda").signingRegion(region).build())).build());
            }
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri("https://s3-object-lambda." + region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                            .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule359(S3EndpointParams params, String region) {
        if (params.bucket() == null) {
            return endpointRule360(params, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule360(S3EndpointParams params, String region) {
        RulePartition partitionResult = RulesFunctions.awsPartition(region);
        if (partitionResult != null) {
            RuleResult result = endpointRule361(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid region: region was not a valid DNS name.");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule361(S3EndpointParams params, String region, RulePartition partitionResult) {
        if (RulesFunctions.isValidHostLabel(region, true)) {
            if (params.useFips() && params.useDualStack() && params.endpoint() == null && "aws-global".equals(region)) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3-fips.dualstack.us-east-1." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            }
            RuleResult result = endpointRule363(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            if (params.useFips() && params.useDualStack() && params.endpoint() == null && !("aws-global".equals(region))
                && !params.useGlobalEndpoint()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3-fips.dualstack." + region + "." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(region).build())).build());
            }
            if (params.useFips() && !params.useDualStack() && params.endpoint() == null && "aws-global".equals(region)) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3-fips.us-east-1." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            }
            result = endpointRule367(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            if (params.useFips() && !params.useDualStack() && params.endpoint() == null && !("aws-global".equals(region))
                && !params.useGlobalEndpoint()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3-fips." + region + "." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(region).build())).build());
            }
            if (!params.useFips() && params.useDualStack() && params.endpoint() == null && "aws-global".equals(region)) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3.dualstack.us-east-1." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            }
            result = endpointRule371(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            if (!params.useFips() && params.useDualStack() && params.endpoint() == null && !("aws-global".equals(region))
                && !params.useGlobalEndpoint()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3.dualstack." + region + "." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(region).build())).build());
            }
            if (!params.useFips() && !params.useDualStack() && params.endpoint() != null) {
                RuleUrl url = RulesFunctions.parseURL(params.endpoint());
                if (url != null) {
                    if ("aws-global".equals(region)) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri(url.scheme() + "://" + url.authority() + url.path()))
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                        .signingRegion("us-east-1").build())).build());
                    }
                }
            }
            result = endpointRule375(params, region);
            if (result.isResolved()) {
                return result;
            }
            if (!params.useFips() && !params.useDualStack() && params.endpoint() != null) {
                RuleUrl url = RulesFunctions.parseURL(params.endpoint());
                if (url != null) {
                    if (!("aws-global".equals(region)) && !params.useGlobalEndpoint()) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri(url.scheme() + "://" + url.authority() + url.path()))
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                        .signingRegion(region).build())).build());
                    }
                }
            }
            if (!params.useFips() && !params.useDualStack() && params.endpoint() == null && "aws-global".equals(region)) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            }
            result = endpointRule380(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            if (!params.useFips() && !params.useDualStack() && params.endpoint() == null && !("aws-global".equals(region))
                && !params.useGlobalEndpoint()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3." + region + "." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(region).build())).build());
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule363(S3EndpointParams params, String region, RulePartition partitionResult) {
        if (params.useFips() && params.useDualStack() && params.endpoint() == null && !("aws-global".equals(region))
            && params.useGlobalEndpoint()) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri("https://s3-fips.dualstack." + region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule367(S3EndpointParams params, String region, RulePartition partitionResult) {
        if (params.useFips() && !params.useDualStack() && params.endpoint() == null && !("aws-global".equals(region))
            && params.useGlobalEndpoint()) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri("https://s3-fips." + region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule371(S3EndpointParams params, String region, RulePartition partitionResult) {
        if (!params.useFips() && params.useDualStack() && params.endpoint() == null && !("aws-global".equals(region))
            && params.useGlobalEndpoint()) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri("https://s3.dualstack." + region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule375(S3EndpointParams params, String region) {
        if (!params.useFips() && !params.useDualStack() && params.endpoint() != null) {
            RuleUrl url = RulesFunctions.parseURL(params.endpoint());
            if (url != null) {
                if (!("aws-global".equals(region)) && params.useGlobalEndpoint()) {
                    if ("us-east-1".equals(region)) {
                        return RuleResult.endpoint(Endpoint
                                                       .builder()
                                                       .url(uriFactory.createUri(url.scheme() + "://" + url.authority() + url.path()))
                                                       .putAttribute(
                                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                        .signingRegion(region).build())).build());
                    }
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(uriFactory.createUri(url.scheme() + "://" + url.authority() + url.path()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
            }
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule380(S3EndpointParams params, String region, RulePartition partitionResult) {
        if (!params.useFips() && !params.useDualStack() && params.endpoint() == null && !("aws-global".equals(region))
            && params.useGlobalEndpoint()) {
            if ("us-east-1".equals(region)) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(uriFactory.createUri("https://s3." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(region).build())).build());
            }
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(uriFactory.createUri("https://s3." + region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }
        return RuleResult.carryOn();
    }

    @Override
    public boolean equals(Object rhs) {
        return rhs != null && getClass().equals(rhs.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
