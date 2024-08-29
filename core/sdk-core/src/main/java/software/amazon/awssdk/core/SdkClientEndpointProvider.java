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

package software.amazon.awssdk.core;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Validate;

@SdkProtectedApi
public interface SdkClientEndpointProvider {
    static SdkClientEndpointProvider forOverrideEndpoint(URI uri) {
        Validate.paramNotNull(uri.getScheme(), "The URI scheme of endpointOverride");
        return new SdkClientEndpointProvider() {
            @Override
            public URI clientEndpoint() {
                return uri;
            }

            @Override
            public boolean isEndpointOverridden() {
                return true;
            }
        };
    }

    URI clientEndpoint();
    boolean isEndpointOverridden();
}
