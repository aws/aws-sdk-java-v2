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

package software.amazon.awssdk.services.query.endpoints.internal;

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
import software.amazon.awssdk.services.query.endpoints.QueryEndpointParams;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.uri.SdkUri;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultQueryEndpointProvider implements QueryEndpointProvider {
    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(QueryEndpointParams params) {
        Validate.notNull(params.region(), "Parameter 'region' must not be null");
        try {
            RuleResult result = endpointRule0(params, new LocalState(params.region()));
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

    private static RuleResult endpointRule0(QueryEndpointParams params, LocalState locals) {
        return endpointRule1(params, locals);
    }

    private static RuleResult endpointRule1(QueryEndpointParams params, LocalState locals) {
        RulePartition partitionResult = null;
        if ((partitionResult = RulesFunctions.awsPartition(locals.region())) != null) {
            locals = locals.toBuilder().partitionResult(partitionResult).build();
            RuleResult result = endpointRule2(params, locals);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule6(params, locals);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule11(params, locals);
            if (result.isResolved()) {
                return result;
            }
            return endpointRule12(params, locals);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule2(QueryEndpointParams params, LocalState locals) {
        if (params.endpointId() != null) {
            RuleResult result = endpointRule3(params, locals);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule4(params, locals);
            if (result.isResolved()) {
                return result;
            }
            return endpointRule5(params, locals);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule3(QueryEndpointParams params, LocalState locals) {
        if (params.useFipsEndpoint() != null && params.useFipsEndpoint()) {
            return RuleResult.error("FIPS endpoints not supported with multi-region endpoints");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule4(QueryEndpointParams params, LocalState locals) {
        if (params.useFipsEndpoint() == null && params.useDualStackEndpoint() != null && params.useDualStackEndpoint()) {
            return RuleResult
                .endpoint(Endpoint
                              .builder()
                              .url(SdkUri.getInstance().create("https://" + params.endpointId() + ".query."
                                              + locals.partitionResult().dualStackDnsSuffix()))
                              .putAttribute(
                                  AwsEndpointAttribute.AUTH_SCHEMES,
                                  Arrays.asList(SigV4aAuthScheme.builder().signingName("query")
                                                                .signingRegionSet(Arrays.asList("*")).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule5(QueryEndpointParams params, LocalState locals) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(SdkUri.getInstance().create("https://" + params.endpointId() + ".query." + locals.partitionResult().dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4aAuthScheme.builder().signingName("query").signingRegionSet(Arrays.asList("*"))
                                                                         .build())).build());
    }

    private static RuleResult endpointRule6(QueryEndpointParams params, LocalState locals) {
        if (RulesFunctions.isValidHostLabel(locals.region(), false)) {
            RuleResult result = endpointRule7(params, locals);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule8(params, locals);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule9(params, locals);
            if (result.isResolved()) {
                return result;
            }
            return endpointRule10(params, locals);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule7(QueryEndpointParams params, LocalState locals) {
        if (params.useFipsEndpoint() != null && params.useFipsEndpoint() && params.useDualStackEndpoint() == null) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(SdkUri.getInstance().create("https://query-fips." + locals.region() + "." + locals.partitionResult().dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().signingName("query").signingRegionSet(Arrays.asList("*"))
                                                                             .build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule8(QueryEndpointParams params, LocalState locals) {
        if (params.useDualStackEndpoint() != null && params.useDualStackEndpoint() && params.useFipsEndpoint() == null) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(SdkUri.getInstance().create("https://query." + locals.region() + "." + locals.partitionResult().dualStackDnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().signingName("query").signingRegionSet(Arrays.asList("*"))
                                                                             .build(), SigV4AuthScheme.builder().signingName("query").signingRegion(locals.region())
                                                                                                      .build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule9(QueryEndpointParams params, LocalState locals) {
        if (params.useDualStackEndpoint() != null && params.useFipsEndpoint() != null && params.useDualStackEndpoint()
            && params.useFipsEndpoint()) {
            return RuleResult
                .endpoint(Endpoint
                              .builder()
                              .url(SdkUri.getInstance().create("https://query-fips." + locals.region() + "."
                                              + locals.partitionResult().dualStackDnsSuffix()))
                              .putAttribute(
                                  AwsEndpointAttribute.AUTH_SCHEMES,
                                  Arrays.asList(SigV4aAuthScheme.builder().signingName("query")
                                                                .signingRegionSet(Arrays.asList("*")).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule10(QueryEndpointParams params, LocalState locals) {
        return RuleResult.endpoint(Endpoint.builder()
                                           .url(SdkUri.getInstance().create("https://query." + locals.region() + "." + locals.partitionResult().dnsSuffix())).build());
    }

    private static RuleResult endpointRule11(QueryEndpointParams params, LocalState locals) {
        return RuleResult.error(locals.region() + " is not a valid HTTP host-label");
    }

    private static RuleResult endpointRule12(QueryEndpointParams params, LocalState locals) {
        if (params.useFipsEndpoint() == null && params.useDualStackEndpoint() != null && params.useDualStackEndpoint()
            && params.arnList() != null) {
            String firstArn = null;
            RuleArn parsedArn = null;
            if ((firstArn = RulesFunctions.listAccess(params.arnList(), 0)) != null) {
                locals = locals.toBuilder().firstArn(firstArn).build();
            } else {
                return RuleResult.carryOn();
            }
            if ((parsedArn = RulesFunctions.awsParseArn(locals.firstArn())) != null) {
                locals = locals.toBuilder().parsedArn(parsedArn).build();
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(SdkUri.getInstance().create("https://" + params.endpointId() + ".query."
                                                               + locals.partitionResult().dualStackDnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4aAuthScheme.builder().signingName("query")
                                                                                 .signingRegionSet(Arrays.asList("*")).build())).build());
            }
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

    private static final class LocalState {
        private final String region;

        private final RulePartition partitionResult;

        private final String firstArn;

        private final RuleArn parsedArn;

        LocalState() {
            this.region = null;
            this.partitionResult = null;
            this.firstArn = null;
            this.parsedArn = null;
        }

        LocalState(Region region) {
            if (region != null) {
                this.region = region.id();
            } else {
                this.region = null;
            }
            this.partitionResult = null;
            this.firstArn = null;
            this.parsedArn = null;
        }

        LocalState(LocalStateBuilder builder) {
            this.region = builder.region;
            this.partitionResult = builder.partitionResult;
            this.firstArn = builder.firstArn;
            this.parsedArn = builder.parsedArn;
        }

        public String region() {
            return this.region;
        }

        public RulePartition partitionResult() {
            return this.partitionResult;
        }

        public String firstArn() {
            return this.firstArn;
        }

        public RuleArn parsedArn() {
            return this.parsedArn;
        }

        public LocalStateBuilder toBuilder() {
            return new LocalStateBuilder(this);
        }
    }

    private static final class LocalStateBuilder {
        private String region;

        private RulePartition partitionResult;

        private String firstArn;

        private RuleArn parsedArn;

        LocalStateBuilder() {
            this.region = null;
            this.partitionResult = null;
            this.firstArn = null;
            this.parsedArn = null;
        }

        LocalStateBuilder(LocalState locals) {
            this.region = locals.region;
            this.partitionResult = locals.partitionResult;
            this.firstArn = locals.firstArn;
            this.parsedArn = locals.parsedArn;
        }

        public LocalStateBuilder region(String value) {
            this.region = value;
            return this;
        }

        public LocalStateBuilder partitionResult(RulePartition value) {
            this.partitionResult = value;
            return this;
        }

        public LocalStateBuilder firstArn(String value) {
            this.firstArn = value;
            return this;
        }

        public LocalStateBuilder parsedArn(RuleArn value) {
            this.parsedArn = value;
            return this;
        }

        LocalState build() {
            return new LocalState(this);
        }
    }
}
