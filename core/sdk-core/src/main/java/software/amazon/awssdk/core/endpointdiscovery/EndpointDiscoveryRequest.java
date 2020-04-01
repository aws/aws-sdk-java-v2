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

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@SdkProtectedApi
public final class EndpointDiscoveryRequest
    implements ToCopyableBuilder<EndpointDiscoveryRequest.Builder, EndpointDiscoveryRequest> {

    private final Optional<String> operationName;
    private final Optional<Map<String, String>> identifiers;
    private final Optional<String> cacheKey;
    private final boolean required;
    private final URI defaultEndpoint;

    private EndpointDiscoveryRequest(BuilderImpl builder) {
        this.operationName = builder.operationName;
        this.identifiers = builder.identifiers;
        this.cacheKey = builder.cacheKey;
        this.required = builder.required;
        this.defaultEndpoint = builder.defaultEndpoint;
    }

    public Optional<String> operationName() {
        return operationName;
    }

    public Optional<Map<String, String>> identifiers() {
        return identifiers;
    }

    public Optional<String> cacheKey() {
        return cacheKey;
    }

    public boolean required() {
        return required;
    }

    public URI defaultEndpoint() {
        return defaultEndpoint;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /**
     * Builder interface for constructing a {@link EndpointDiscoveryRequest}.
     */
    public interface Builder extends CopyableBuilder<Builder, EndpointDiscoveryRequest> {

        /**
         * The name of the operation being used in the customer's request.
         *
         * @param operationName The name of the operation.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder operationName(String operationName);

        /**
         * Specifies a map containing a set identifiers mapped to the name of the field in the request.
         *
         * @param identifiers A map of identifiers for the request.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder identifiers(Map<String, String> identifiers);

        /**
         * The cache key to use for a given cache entry.
         *
         * @param cacheKey A cache key.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder cacheKey(String cacheKey);

        /**
         * Whether or not endpoint discovery is required for this request.
         *
         * @param required boolean specifying if endpoint discovery is required.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder required(boolean required);

        /**
         * The default endpoint for a request.
         * @param defaultEndpoint {@link URI} of the default endpoint
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder defaultEndpoint(URI defaultEndpoint);
    }

    static class BuilderImpl implements Builder {
        private Optional<String> operationName = Optional.empty();
        private Optional<Map<String, String>> identifiers = Optional.empty();
        private Optional<String> cacheKey = Optional.empty();
        private boolean required = false;
        private URI defaultEndpoint;

        private BuilderImpl() {

        }

        private BuilderImpl(EndpointDiscoveryRequest request) {
            this.operationName = request.operationName;
            this.identifiers = request.identifiers;
            this.cacheKey = request.cacheKey;
            this.required = request.required;
        }

        @Override
        public Builder operationName(String operationName) {
            this.operationName = Optional.ofNullable(operationName);
            return this;
        }

        @Override
        public Builder identifiers(Map<String, String> identifiers) {
            this.identifiers = Optional.ofNullable(identifiers);
            return this;
        }

        @Override
        public Builder cacheKey(String cacheKey) {
            this.cacheKey = Optional.ofNullable(cacheKey);
            return this;
        }

        @Override
        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        @Override
        public Builder defaultEndpoint(URI defaultEndpoint) {
            this.defaultEndpoint = defaultEndpoint;
            return this;
        }

        @Override
        public EndpointDiscoveryRequest build() {
            return new EndpointDiscoveryRequest(this);
        }
    }
}
