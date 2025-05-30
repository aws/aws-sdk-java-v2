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

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointParams;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultQueryEndpointProvider implements QueryEndpointProvider {
    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(QueryEndpointParams params) {
        try {
            RuleResult result = endpointRule0(params, new LocalState());
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
        RuleResult result = endpointRule1(params, locals);
        if (result.isResolved()) {
            return result;
        }
        return endpointRule3(params, locals);
    }

    private static RuleResult endpointRule1(QueryEndpointParams params, LocalState locals) {
        if (params.endpoint() != null) {
            return endpointRule2(params, locals);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule2(QueryEndpointParams params, LocalState locals) {
        return RuleResult.endpoint(Endpoint.builder().url(URI.create(params.endpoint())).build());
    }

    private static RuleResult endpointRule3(QueryEndpointParams params, LocalState locals) {
        return RuleResult.error("Invalid Configuration: Missing Endpoint");
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
        LocalState() {
        }

        LocalState(LocalStateBuilder builder) {
        }

        public LocalStateBuilder toBuilder() {
            return new LocalStateBuilder(this);
        }
    }

    private static final class LocalStateBuilder {
        LocalStateBuilder() {
        }

        LocalStateBuilder(LocalState locals) {
        }

        LocalState build() {
            return new LocalState(this);
        }
    }
}
