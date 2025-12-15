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

package software.amazon.awssdk.services.connect.endpoints.internal;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.services.connect.endpoints.ConnectEndpointParams;
import software.amazon.awssdk.services.connect.endpoints.ConnectEndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class BDDEndpointResolverCodegenDag2 implements ConnectEndpointProvider {
    private static final int REGION = 0;

    private static final int USE_DUAL_STACK = 1;

    private static final int USE_FIPS = 2;

    private static final int ENDPOINT = 3;

    private static final int PARTITION_RESULT = 4;
    
    private static class Registers {
        String region;
        Boolean useDualStack;
        Boolean useFips;
        String endpoint;
        RulePartition partition;
    }

    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(ConnectEndpointParams params) {
        try {
            Validate.notNull(params.useDualStack(), "Parameter 'UseDualStack' must not be null");
            Validate.notNull(params.useFips(), "Parameter 'UseFIPS' must not be null");
            Registers registers = new Registers();
            registers.region = params.region() == null ? null : params.region().id();
            registers.useDualStack = params.useDualStack();
            registers.useFips = params.useFips();
            registers.endpoint= params.endpoint();
            RuleResult result = n14(registers, false);
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

    private static boolean c0(Registers registers) {
        return ((registers.endpoint) != null);
    }

    private static boolean c1(Registers registers) {
        return (( registers.region) != null);
    }

    private static boolean c2(Registers registers) {
        return registers.useFips;
    }

    private static boolean c3(Registers registers) {
        registers.partition = RulesFunctions.awsPartition(registers.region);
        return registers.partition != null;
    }

    private static boolean c4(Registers registers) {
        return (registers.useDualStack);
    }

    private static boolean c5(Registers registers) {
        return registers.partition.supportsFIPS();
    }

    private static boolean c6(Registers registers) {
        return registers.partition.supportsDualStack();
    }

    private static boolean c7(Registers registers) {
        return ("aws-us-gov"
            .equals(registers.partition.name()));
    }

    private static RuleResult r0(Registers registers) {
        return RuleResult.error("Invalid Configuration: FIPS and custom endpoint are not supported");
    }

    private static RuleResult r1(Registers registers) {
        return RuleResult.error("Invalid Configuration: Dualstack and custom endpoint are not supported");
    }

    private static RuleResult r2(Registers registers) {
        return RuleResult.endpoint(Endpoint.builder().url(URI.create(registers.endpoint)).build());
    }

    private static RuleResult r3(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://acm-fips."
                                                       + registers.region
                                                       + "."
                                                       + registers.partition
                                                           .dualStackDnsSuffix())).build());
    }

    private static RuleResult r4(Registers registers) {
        return RuleResult.error("FIPS and DualStack are enabled, but this partition does not support one or both");
    }

    private static RuleResult r5(Registers registers) {
        return RuleResult.endpoint(Endpoint.builder()
                                           .url(URI.create("https://acm." + registers.region + ".amazonaws.com")).build());
    }

    private static RuleResult r6(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://acm-fips."
                                                       + registers.region
                                                       + "."
                                                       + registers.partition
                                                           .dnsSuffix())).build());
    }

    private static RuleResult r7(Registers registers) {
        return RuleResult.error("FIPS is enabled but this partition does not support FIPS");
    }

    private static RuleResult r8(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://acm."
                                                       + registers.region
                                                       + "."
                                                       + registers.partition
                                                           .dualStackDnsSuffix())).build());
    }

    private static RuleResult r9(Registers registers) {
        return RuleResult.error("DualStack is enabled but this partition does not support DualStack");
    }

    private static RuleResult r10(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://acm."
                                                       + registers.region
                                                       + "."
                                                       + (registers.partition.dnsSuffix()))).build());
    }

    private static RuleResult r11(Registers registers) {
        return RuleResult.error("Invalid Configuration: Missing Region");
    }

    private static RuleResult n1(Registers registers, boolean complemented) {
        if (complemented != c4(registers)) {
            return r1(registers);
        }
        return r2(registers);
    }

    private static RuleResult n2(Registers registers, boolean complemented) {
        if (complemented != c2(registers)) {
            return r0(registers);
        }
        return n1(registers, false);
    }

    private static RuleResult n3(Registers registers, boolean complemented) {
        if (complemented != c6(registers)) {
            return r3(registers);
        }
        return r4(registers);
    }

    private static RuleResult n4(Registers registers, boolean complemented) {
        if (complemented != c5(registers)) {
            return n3(registers, false);
        }
        return r4(registers);
    }

    private static RuleResult n5(Registers registers, boolean complemented) {
        if (complemented != c7(registers)) {
            return r5(registers);
        }
        return r6(registers);
    }

    private static RuleResult n6(Registers registers, boolean complemented) {
        if (complemented != c5(registers)) {
            return n5(registers, false);
        }
        return r7(registers);
    }

    private static RuleResult n7(Registers registers, boolean complemented) {
        if (complemented != c4(registers)) {
            return n4(registers, false);
        }
        return n6(registers, false);
    }

    private static RuleResult n8(Registers registers, boolean complemented) {
        if (complemented != c3(registers)) {
            return n7(registers, false);
        }
        return r11(registers);
    }

    private static RuleResult n9(Registers registers, boolean complemented) {
        if (complemented != c6(registers)) {
            return r8(registers);
        }
        return r9(registers);
    }

    private static RuleResult n10(Registers registers, boolean complemented) {
        if (complemented != c4(registers)) {
            return n9(registers, false);
        }
        return r10(registers);
    }

    private static RuleResult n11(Registers registers, boolean complemented) {
        if (complemented != c3(registers)) {
            return n10(registers, false);
        }
        return r11(registers);
    }

    private static RuleResult n12(Registers registers, boolean complemented) {
        if (complemented != c2(registers)) {
            return n8(registers, false);
        }
        return n11(registers, false);
    }

    private static RuleResult n13(Registers registers, boolean complemented) {
        if (complemented != c1(registers)) {
            return n12(registers, false);
        }
        return r11(registers);
    }

    private static RuleResult n14(Registers registers, boolean complemented) {
        if (complemented != c0(registers)) {
            return n2(registers, false);
        }
        return n13(registers, false);
    }
}
