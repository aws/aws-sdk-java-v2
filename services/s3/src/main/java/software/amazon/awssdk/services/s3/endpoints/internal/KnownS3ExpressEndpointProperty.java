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

package software.amazon.awssdk.services.s3.endpoints.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.endpoints.EndpointAttributeKey;

@SdkInternalApi
public final class KnownS3ExpressEndpointProperty {

    /**
     * An attribute to represent a service component
     */
    public static final EndpointAttributeKey<String> BACKEND =
        new EndpointAttributeKey<>("Backend", String.class);

    private KnownS3ExpressEndpointProperty() {
    }
}
