/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.protocol;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.http.HttpMethodName;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;

/**
 * Static information about an API operation used to marshall it correctly.
 */
@SdkProtectedApi
public final class OperationInfo {

    private final String requestUri;
    private final HttpMethodName httpMethodName;
    private final String operationIdentifier;
    private final String serviceName;
    private final boolean hasExplicitPayloadMember;
    private final boolean hasPayloadMembers;

    private OperationInfo(Builder builder) {
        this.requestUri = builder.requestUri;
        this.httpMethodName = builder.httpMethodName;
        this.operationIdentifier = builder.operationIdentifier;
        this.serviceName = builder.serviceName;
        this.hasExplicitPayloadMember = builder.hasExplicitPayloadMember;
        this.hasPayloadMembers = builder.hasPayloadMembers;
    }

    /**
     * @return Request URI for operation (may contain placeholders for members bound to the uri).
     */
    public String requestUri() {
        return requestUri;
    }

    /**
     * @return HTTP Method that should be used when sending the request.
     */
    public HttpMethodName httpMethodName() {
        return httpMethodName;
    }

    /**
     * @return Identifer for the operation/API being invoked. This is used for RPC based protocols that
     *     need to identify which action is being taken. For Query/EC2 protocol this is sent as the 'Action' query
     *     parameter, for JSON RPC this is sent as the 'X-Amz-Target' header.
     */
    public String operationIdentifier() {
        return operationIdentifier;
    }

    /**
     * @return Display name for the service. Available in the {@link SdkExecutionAttribute} object. This is usually the service
     * interface name but may be customized at generation time ('AmazonDynamoDBv2' for example).
     */
    public String serviceName() {
        return serviceName;
    }

    /**
     * @return True if the operation has a member that's explicitly marked as the payload. False otherwise. (Applicable only to
     *     RESTUL protocols).
     */
    public boolean hasExplicitPayloadMember() {
        return hasExplicitPayloadMember;
    }

    /**
     * @return True if the operation has members bound to the payload. Some requests (especially GET and DELETE) may not
     *     have any members bound to the payload. (Applicable only to RESTFUL protocols).
     */
    public boolean hasPayloadMembers() {
        return hasPayloadMembers;
    }

    /**
     * @return Builder instance to construct a {@link OperationInfo}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link OperationInfo}.
     */
    public static final class Builder {

        private String requestUri;
        private HttpMethodName httpMethodName;
        private String operationIdentifier;
        private String serviceName;
        private boolean hasExplicitPayloadMember;
        private boolean hasPayloadMembers;

        private Builder() {
        }

        public Builder requestUri(String requestUri) {
            this.requestUri = requestUri;
            return this;
        }

        public Builder httpMethodName(HttpMethodName httpMethodName) {
            this.httpMethodName = httpMethodName;
            return this;
        }

        public Builder operationIdentifier(String operationIdentifier) {
            this.operationIdentifier = operationIdentifier;
            return this;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder hasExplicitPayloadMember(boolean hasExplicitPayloadMember) {
            this.hasExplicitPayloadMember = hasExplicitPayloadMember;
            return this;
        }

        public Builder hasPayloadMembers(boolean hasPayloadMembers) {
            this.hasPayloadMembers = hasPayloadMembers;
            return this;
        }

        /**
         * @return An immutable {@link OperationInfo} object.
         */
        public OperationInfo build() {
            return new OperationInfo(this);
        }
    }
}
