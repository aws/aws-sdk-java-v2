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

package software.amazon.awssdk.awscore.endpoints;

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.endpoints.EndpointAttributeKey;

/**
 * Known endpoint attributes added by endpoint rule sets for AWS services.
 */
@SdkProtectedApi
public final class AwsEndpointAttribute {

    /**
     * The auth schemes supported by the endpoint.
     */
    public static final EndpointAttributeKey<List<EndpointAuthScheme>> AUTH_SCHEMES =
        EndpointAttributeKey.forList("AuthSchemes");

    private AwsEndpointAttribute() {
    }

    public static List<EndpointAttributeKey<?>> values() {
        return Collections.singletonList(AUTH_SCHEMES);
    }
}
