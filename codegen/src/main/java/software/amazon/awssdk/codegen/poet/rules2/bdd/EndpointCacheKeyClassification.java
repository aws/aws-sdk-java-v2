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

package software.amazon.awssdk.codegen.poet.rules2.bdd;

/**
 * Classifies one endpoint parameter to decide how the generated BDD endpoint provider compares it when checking its
 * single-entry result cache.
 *
 * <p>Values are declared cheapest comparison first. {@code BddEndpointProviderSpec} emits the parameter checks in this
 * order and returns on the first mismatch, so the parameters most likely to differ between two requests are also the
 * ones reached last.
 *
 * <h2>Why a wrong classification cannot return a wrong endpoint</h2>
 *
 * <p>Every classification compares references first and either stops there or falls back to {@code equals}. Comparing
 * only references can report a mismatch for two equal values, which costs a re-resolution; it can never report a match
 * for two different values, because equal references imply the same object. So over-classifying a parameter as stable
 * costs hit rate, not correctness.
 *
 * <p>What does affect correctness is a parameter being left out of the comparison altogether. That is why
 * {@link EndpointProviderCacheIndex} classifies every parameter the BDD model declares and
 * {@code BddEndpointProviderSpec} fails codegen rather than skipping one it cannot classify.
 */
public enum EndpointCacheKeyClassification {
    /**
     * A {@code boolean} parameter. Compared with {@code ==} and no fallback, which is complete rather than merely fast:
     * autoboxing and {@code Boolean.valueOf} both hand back the {@code Boolean.TRUE}/{@code Boolean.FALSE} singletons,
     * so identity agrees with {@code equals} for every value a caller can produce short of the {@code Boolean}
     * constructor deprecated in Java 9.
     */
    BOOLEAN,

    /**
     * A string parameter sourced entirely from client configuration, where the same reference is handed to every
     * request: {@code AWS::Region} (interned by {@code Region.of}) and {@code clientContextParams} (read from the
     * client's {@code AttributeMap}). Compared with {@code ==} only.
     */
    CLIENT_STATIC_REF,

    /**
     * A string parameter bound to a {@code staticContextParams} literal. {@code EndpointResolverUtilsSpec} emits string
     * literals and hoists array values to {@code static final} fields, so the reference is fixed per operation.
     * Compared with {@code ==} only. List-valued static params are classified {@link #REQUEST_LIST} instead, since they
     * share the list comparison shape.
     */
    OPERATION_STATIC,

    /**
     * A string parameter that is logically fixed for the life of the client but whose reference stability depends on an
     * implementation detail a customer can replace: {@code SDK::Endpoint}, stable only because
     * {@code StaticClientEndpointProvider} computes the sanitized string once, and
     * {@code AWS::Auth::AccountIdEndpointMode}, stable only because {@code AccountIdEndpointMode.endpointModeValue}
     * returns an interned literal. A custom {@code ClientEndpointProvider} need not cache. Compared with {@code ==},
     * then {@code equals}.
     */
    SEMI_STABLE,

    /**
     * {@code AWS::Auth::AccountId}, read off the resolved identity. Stable while the credentials provider serves the
     * same cached identity, and a fresh reference after every refresh. Compared with {@code ==}, then {@code equals}.
     */
    IDENTITY_DERIVED,

    /**
     * A string parameter bound to a request member ({@code contextParam}) or extracted from the request by JMESPath
     * ({@code operationContextParams}). Generally a fresh reference per request; the identity check still pays for
     * itself when one API call resolves the endpoint more than once from the same request object. Compared with
     * {@code ==}, then {@code equals}.
     */
    REQUEST_DYNAMIC,

    /**
     * A {@code stringArray} parameter. Compared last, with a null-safe identity check, then size, then element-wise
     * identity or {@code equals}. Lists longer than {@link EndpointProviderCacheIndex#MAX_LIST_COMPARISON_SIZE} report
     * a miss without being walked, so the key check stays bounded no matter how large the request is.
     */
    REQUEST_LIST
}
