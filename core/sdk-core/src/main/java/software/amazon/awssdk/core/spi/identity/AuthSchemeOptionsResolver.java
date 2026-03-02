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

package software.amazon.awssdk.core.spi.identity;

import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;

/**
 * Callback interface for resolving auth scheme options from the request.
 * <p>
 * This allows auth scheme resolution to happen after interceptors have modified the request,
 * ensuring that any request modifications affecting auth scheme selection are respected.
 */
@FunctionalInterface
@SdkProtectedApi
public interface AuthSchemeOptionsResolver {
    /**
     * Resolves auth scheme options for the given request.
     *
     * @param request The request (after interceptors have modified it)
     * @return List of auth scheme options in priority order
     */
    List<AuthSchemeOption> resolve(SdkRequest request);
}
