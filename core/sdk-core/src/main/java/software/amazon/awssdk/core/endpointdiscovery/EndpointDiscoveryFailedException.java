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

package software.amazon.awssdk.core.endpointdiscovery;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.Validate;

/**
 * This exception is thrown when the SDK was unable to retrieve an endpoint from AWS. The cause describes what specific part of
 * the endpoint discovery process failed.
 */
@SdkPublicApi
public class EndpointDiscoveryFailedException extends SdkClientException {

    private static final long serialVersionUID = 1L;

    private EndpointDiscoveryFailedException(Builder b) {
        super(b);
        Validate.paramNotNull(b.cause(), "cause");
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public static EndpointDiscoveryFailedException create(Throwable cause) {
        return builder().message("Failed when retrieving a required endpoint from AWS.")
                        .cause(cause)
                        .build();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public interface Builder extends SdkClientException.Builder {
        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        EndpointDiscoveryFailedException build();
    }

    protected static final class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {

        protected BuilderImpl() {
        }

        protected BuilderImpl(EndpointDiscoveryFailedException ex) {
            super(ex);
        }

        @Override
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        @Override
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        @Override
        public EndpointDiscoveryFailedException build() {
            return new EndpointDiscoveryFailedException(this);
        }
    }
}
