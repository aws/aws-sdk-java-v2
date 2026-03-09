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

package software.amazon.awssdk.benchmark.endpoint;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointUrl;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.endpoints.Ec2EndpointParams;
import software.amazon.awssdk.services.ec2.endpoints.Ec2EndpointProvider;
import software.amazon.awssdk.services.ec2.endpoints.internal.RulePartition;
import software.amazon.awssdk.services.ec2.endpoints.internal.RuleResult;
import software.amazon.awssdk.services.ec2.endpoints.internal.RulesFunctions;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.ImmutableMap;
import software.amazon.awssdk.utils.Validate;

/**
 * This benchmark measure the performance improvement of replacing URI.create with a custom EndpointUrl class.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Benchmark)
public class Ec2UriCreateEndpointResolutionBenchmark {


    @Param({
        "customEndpoint",
        "standardRegional",
        "dualstackFipsRegional"
    })
    private String testCase;

    private Ec2EndpointProvider oldProviderWithCreate;
    private Ec2EndpointProvider newProviderWithEndpointUrlParse;
    private Ec2EndpointProvider newProviderWithEndpointUrlComponents;

    private static final Map<String, Ec2EndpointParams> testCases;
    static {
        Map<String, Ec2EndpointParams> m = new HashMap<>();
        m.put("customEndpoint", Ec2EndpointParams.builder().region(Region.US_EAST_1).endpoint("http://localhost:8080").build());
        m.put("standardRegional", Ec2EndpointParams.builder().region(Region.US_EAST_1).build());
        m.put("dualstackFipsRegional", Ec2EndpointParams
            .builder().region(Region.US_EAST_1).useDualStack(true).useFips(true).build());
        testCases = m;
    }

    @Setup
    public void setup() {
        oldProviderWithCreate = new Ec2EndpointProviderOldUriCreate();
        newProviderWithEndpointUrlParse = new Ec2EndpointProviderNewEndpointUrlParse();
        newProviderWithEndpointUrlComponents = new Ec2EndpointProviderNewEndpointUrlComponents();
    }

    @Benchmark
    public Endpoint old_resolveWithUriCreate() throws ExecutionException, InterruptedException {
        return oldProviderWithCreate.resolveEndpoint(testCases.get(testCase)).get();
    }

    @Benchmark
    public Endpoint new_resolveWithEndpointUrlParse() throws ExecutionException, InterruptedException {
        return newProviderWithEndpointUrlParse.resolveEndpoint(testCases.get(testCase)).get();
    }

    @Benchmark
    public Endpoint new_resolveWithEndpointUrlComponents() throws ExecutionException, InterruptedException {
        return newProviderWithEndpointUrlComponents.resolveEndpoint(testCases.get(testCase)).get();
    }

    /**
     * Hard code the old DefaultEc2EndpointProvider that uses URI.create to preserve it for benchmarking here.
     */
    public static final class Ec2EndpointProviderOldUriCreate implements Ec2EndpointProvider {
        @Override
        public CompletableFuture<Endpoint> resolveEndpoint(Ec2EndpointParams params) {
            Validate.notNull(params.useDualStack(), "Parameter 'UseDualStack' must not be null");
            Validate.notNull(params.useFips(), "Parameter 'UseFIPS' must not be null");
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

        private static RuleResult endpointRule0(Ec2EndpointParams params, String region) {
            RuleResult result = endpointRule1(params);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule5(params, region);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid Configuration: Missing Region");
        }

        private static RuleResult endpointRule1(Ec2EndpointParams params) {
            if (params.endpoint() != null) {
                if (params.useFips()) {
                    return RuleResult.error("Invalid Configuration: FIPS and custom endpoint are not supported");
                }
                if (params.useDualStack()) {
                    return RuleResult.error("Invalid Configuration: Dualstack and custom endpoint are not supported");
                }
                return RuleResult.endpoint(Endpoint.builder().url(URI.create(params.endpoint())).build());
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule5(Ec2EndpointParams params, String region) {
            if (region != null) {
                return endpointRule6(params, region);
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule6(Ec2EndpointParams params, String region) {
            RulePartition partitionResult = RulesFunctions.awsPartition(region);
            if (partitionResult != null) {
                RuleResult result = endpointRule7(params, partitionResult, region);
                if (result.isResolved()) {
                    return result;
                }
                result = endpointRule11(params, partitionResult, region);
                if (result.isResolved()) {
                    return result;
                }
                result = endpointRule16(params, partitionResult, region);
                if (result.isResolved()) {
                    return result;
                }
                return RuleResult.endpoint(Endpoint.builder()
                                                   .url(URI.create("https://ec2." + region + "." + partitionResult.dnsSuffix())).build());
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule7(Ec2EndpointParams params, RulePartition partitionResult, String region) {
            if (params.useFips() && params.useDualStack()) {
                RuleResult result = endpointRule8(params, partitionResult, region);
                if (result.isResolved()) {
                    return result;
                }
                return RuleResult.error("FIPS and DualStack are enabled, but this partition does not support one or both");
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule8(Ec2EndpointParams params, RulePartition partitionResult, String region) {
            if (partitionResult.supportsFIPS() && partitionResult.supportsDualStack()) {
                return RuleResult.endpoint(Endpoint.builder()
                                                   .url(URI.create("https://ec2-fips." + region + "." + partitionResult.dualStackDnsSuffix())).build());
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule11(Ec2EndpointParams params, RulePartition partitionResult, String region) {
            if (params.useFips()) {
                RuleResult result = endpointRule12(params, partitionResult, region);
                if (result.isResolved()) {
                    return result;
                }
                return RuleResult.error("FIPS is enabled but this partition does not support FIPS");
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule12(Ec2EndpointParams params, RulePartition partitionResult, String region) {
            if (partitionResult.supportsFIPS()) {
                if ("aws-us-gov".equals(partitionResult.name())) {
                    return RuleResult
                        .endpoint(Endpoint.builder().url(URI.create("https://ec2." + region + ".amazonaws.com")).build());
                }
                return RuleResult.endpoint(Endpoint.builder()
                                                   .url(URI.create("https://ec2-fips." + region + "." + partitionResult.dnsSuffix())).build());
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule16(Ec2EndpointParams params, RulePartition partitionResult, String region) {
            if (params.useDualStack()) {
                RuleResult result = endpointRule17(params, partitionResult, region);
                if (result.isResolved()) {
                    return result;
                }
                return RuleResult.error("DualStack is enabled but this partition does not support DualStack");
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule17(Ec2EndpointParams params, RulePartition partitionResult, String region) {
            if (partitionResult.supportsDualStack()) {
                return RuleResult.endpoint(Endpoint.builder()
                                                   .url(URI.create("https://ec2." + region + "." + partitionResult.dualStackDnsSuffix())).build());
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

    /**
     * Hard code generated EC2 endpoint provider using the new EndpointUrl.parse
     */
    public static final class Ec2EndpointProviderNewEndpointUrlParse implements Ec2EndpointProvider {
        @Override
        public CompletableFuture<Endpoint> resolveEndpoint(Ec2EndpointParams params) {
            Validate.notNull(params.useDualStack(), "Parameter 'UseDualStack' must not be null");
            Validate.notNull(params.useFips(), "Parameter 'UseFIPS' must not be null");
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

        private static RuleResult endpointRule0(Ec2EndpointParams params, String region) {
            RuleResult result = endpointRule1(params);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule5(params, region);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid Configuration: Missing Region");
        }

        private static RuleResult endpointRule1(Ec2EndpointParams params) {
            if (params.endpoint() != null) {
                if (params.useFips()) {
                    return RuleResult.error("Invalid Configuration: FIPS and custom endpoint are not supported");
                }
                if (params.useDualStack()) {
                    return RuleResult.error("Invalid Configuration: Dualstack and custom endpoint are not supported");
                }
                return RuleResult.endpoint(Endpoint.builder().endpointUrl(EndpointUrl.parse(params.endpoint())).build());
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule5(Ec2EndpointParams params, String region) {
            if (region != null) {
                return endpointRule6(params, region);
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule6(Ec2EndpointParams params, String region) {
            RulePartition partitionResult = RulesFunctions.awsPartition(region);
            if (partitionResult != null) {
                RuleResult result = endpointRule7(params, partitionResult, region);
                if (result.isResolved()) {
                    return result;
                }
                result = endpointRule11(params, partitionResult, region);
                if (result.isResolved()) {
                    return result;
                }
                result = endpointRule16(params, partitionResult, region);
                if (result.isResolved()) {
                    return result;
                }
                return RuleResult.endpoint(Endpoint.builder()
                                                   .endpointUrl(EndpointUrl.parse("https://ec2." + region + "." + partitionResult.dnsSuffix())).build());
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule7(Ec2EndpointParams params, RulePartition partitionResult, String region) {
            if (params.useFips() && params.useDualStack()) {
                RuleResult result = endpointRule8(params, partitionResult, region);
                if (result.isResolved()) {
                    return result;
                }
                return RuleResult.error("FIPS and DualStack are enabled, but this partition does not support one or both");
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule8(Ec2EndpointParams params, RulePartition partitionResult, String region) {
            if (partitionResult.supportsFIPS() && partitionResult.supportsDualStack()) {
                return RuleResult.endpoint(Endpoint.builder()
                                                   .endpointUrl(EndpointUrl.parse("https://ec2-fips." + region + "." + partitionResult.dualStackDnsSuffix()))
                                                   .build());
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule11(Ec2EndpointParams params, RulePartition partitionResult, String region) {
            if (params.useFips()) {
                RuleResult result = endpointRule12(params, partitionResult, region);
                if (result.isResolved()) {
                    return result;
                }
                return RuleResult.error("FIPS is enabled but this partition does not support FIPS");
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule12(Ec2EndpointParams params, RulePartition partitionResult, String region) {
            if (partitionResult.supportsFIPS()) {
                if ("aws-us-gov".equals(partitionResult.name())) {
                    return RuleResult.endpoint(Endpoint.builder()
                                                       .endpointUrl(EndpointUrl.parse("https://ec2." + region + ".amazonaws.com")).build());
                }
                return RuleResult.endpoint(Endpoint.builder()
                                                   .endpointUrl(EndpointUrl.parse("https://ec2-fips." + region + "." + partitionResult.dnsSuffix())).build());
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule16(Ec2EndpointParams params, RulePartition partitionResult, String region) {
            if (params.useDualStack()) {
                RuleResult result = endpointRule17(params, partitionResult, region);
                if (result.isResolved()) {
                    return result;
                }
                return RuleResult.error("DualStack is enabled but this partition does not support DualStack");
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule17(Ec2EndpointParams params, RulePartition partitionResult, String region) {
            if (partitionResult.supportsDualStack()) {
                return RuleResult
                    .endpoint(Endpoint.builder()
                                      .endpointUrl(EndpointUrl.parse("https://ec2." + region + "." + partitionResult.dualStackDnsSuffix()))
                                      .build());
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

    /**
     * Hard coded generated EC2 endpoint provider using the new EndpointUrl.of with individual components.
     * Components are detected during codegen time.
     */
    public static final class Ec2EndpointProviderNewEndpointUrlComponents implements Ec2EndpointProvider {
        @Override
        public CompletableFuture<Endpoint> resolveEndpoint(Ec2EndpointParams params) {
            Validate.notNull(params.useDualStack(), "Parameter 'UseDualStack' must not be null");
            Validate.notNull(params.useFips(), "Parameter 'UseFIPS' must not be null");
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

        private static RuleResult endpointRule0(Ec2EndpointParams params, String region) {
            RuleResult result = endpointRule1(params);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule5(params, region);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error("Invalid Configuration: Missing Region");
        }

        private static RuleResult endpointRule1(Ec2EndpointParams params) {
            if (params.endpoint() != null) {
                if (params.useFips()) {
                    return RuleResult.error("Invalid Configuration: FIPS and custom endpoint are not supported");
                }
                if (params.useDualStack()) {
                    return RuleResult.error("Invalid Configuration: Dualstack and custom endpoint are not supported");
                }
                return RuleResult.endpoint(Endpoint.builder().endpointUrl(EndpointUrl.parse(params.endpoint())).build());
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule5(Ec2EndpointParams params, String region) {
            if (region != null) {
                return endpointRule6(params, region);
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule6(Ec2EndpointParams params, String region) {
            RulePartition partitionResult = RulesFunctions.awsPartition(region);
            if (partitionResult != null) {
                RuleResult result = endpointRule7(params, partitionResult, region);
                if (result.isResolved()) {
                    return result;
                }
                result = endpointRule11(params, partitionResult, region);
                if (result.isResolved()) {
                    return result;
                }
                result = endpointRule16(params, partitionResult, region);
                if (result.isResolved()) {
                    return result;
                }
                return RuleResult.endpoint(Endpoint.builder()
                                                   .endpointUrl(EndpointUrl.of("https", "ec2." + region + "." + partitionResult.dnsSuffix(), -1, "")).build());
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule7(Ec2EndpointParams params, RulePartition partitionResult, String region) {
            if (params.useFips() && params.useDualStack()) {
                RuleResult result = endpointRule8(params, partitionResult, region);
                if (result.isResolved()) {
                    return result;
                }
                return RuleResult.error("FIPS and DualStack are enabled, but this partition does not support one or both");
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule8(Ec2EndpointParams params, RulePartition partitionResult, String region) {
            if (partitionResult.supportsFIPS() && partitionResult.supportsDualStack()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .endpointUrl(
                                                   EndpointUrl.of("https", "ec2-fips." + region + "." + partitionResult.dualStackDnsSuffix(), -1, ""))
                                               .build());
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule11(Ec2EndpointParams params, RulePartition partitionResult, String region) {
            if (params.useFips()) {
                RuleResult result = endpointRule12(params, partitionResult, region);
                if (result.isResolved()) {
                    return result;
                }
                return RuleResult.error("FIPS is enabled but this partition does not support FIPS");
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule12(Ec2EndpointParams params, RulePartition partitionResult, String region) {
            if (partitionResult.supportsFIPS()) {
                if ("aws-us-gov".equals(partitionResult.name())) {
                    return RuleResult.endpoint(Endpoint.builder()
                                                       .endpointUrl(EndpointUrl.of("https", "ec2." + region + ".amazonaws.com", -1, "")).build());
                }
                return RuleResult.endpoint(Endpoint.builder()
                                                   .endpointUrl(EndpointUrl.of("https", "ec2-fips." + region + "." + partitionResult.dnsSuffix(), -1, ""))
                                                   .build());
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule16(Ec2EndpointParams params, RulePartition partitionResult, String region) {
            if (params.useDualStack()) {
                RuleResult result = endpointRule17(params, partitionResult, region);
                if (result.isResolved()) {
                    return result;
                }
                return RuleResult.error("DualStack is enabled but this partition does not support DualStack");
            }
            return RuleResult.carryOn();
        }

        private static RuleResult endpointRule17(Ec2EndpointParams params, RulePartition partitionResult, String region) {
            if (partitionResult.supportsDualStack()) {
                return RuleResult.endpoint(Endpoint.builder()
                                                   .endpointUrl(EndpointUrl.of("https", "ec2." + region + "." + partitionResult.dualStackDnsSuffix(), -1, ""))
                                                   .build());
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

}
