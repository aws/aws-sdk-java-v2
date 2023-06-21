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

package software.amazon.awssdk.services.codecatalyst.endpoints.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class EndpointRule extends Rule {
    private final EndpointResult endpoint;

    protected EndpointRule(Builder builder, EndpointResult endpoint) {
        super(builder);
        this.endpoint = endpoint;
    }

    public EndpointResult getEndpoint() {
        return endpoint;
    }

    @Override
    public <T> T accept(RuleValueVisitor<T> visitor) {
        return visitor.visitEndpointRule(this.getEndpoint());
    }

    @Override
    public String toString() {
        return "EndpointRule{" + "endpoint=" + endpoint + ", conditions=" + conditions + ", documentation='" + documentation
                + '\'' + '}';
    }
}
