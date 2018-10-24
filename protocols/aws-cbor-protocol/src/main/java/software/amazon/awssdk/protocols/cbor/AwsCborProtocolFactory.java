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

package software.amazon.awssdk.protocols.cbor;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.protocols.cbor.internal.AwsStructuredCborFactory;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.AwsStructuredJsonFactory;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.DefaultJsonContentTypeResolver;
import software.amazon.awssdk.protocols.json.JsonContentTypeResolver;

/**
 * Protocol factory for AWS/CBOR protocols. Supports both JSON RPC and REST JSON versions of CBOR. Defaults to
 * the CBOR wire format but can fallback to standard JSON if the {@link SdkSystemSetting#CBOR_ENABLED} is
 * set to false.
 */
@SdkProtectedApi
public final class AwsCborProtocolFactory extends BaseAwsJsonProtocolFactory {

    /**
     * Content type resolver implementation for AWS_CBOR enabled services.
     */
    private static final JsonContentTypeResolver AWS_CBOR = new DefaultJsonContentTypeResolver("application/x-amz-cbor-");

    private AwsCborProtocolFactory(Builder builder) {
        super(builder);
    }

    /**
     * @return Content type resolver implementation to use.
     */
    @Override
    protected JsonContentTypeResolver getContentTypeResolver() {
        if (isCborEnabled()) {
            return AWS_CBOR;
        } else {
            return AwsJsonProtocolFactory.AWS_JSON;
        }
    }

    /**
     * @return Instance of {@link AwsStructuredJsonFactory} to use in creating handlers.
     */
    @Override
    protected AwsStructuredJsonFactory getSdkFactory() {
        if (isCborEnabled()) {
            return AwsStructuredCborFactory.SDK_CBOR_FACTORY;
        } else {
            return super.getSdkFactory();
        }
    }

    private boolean isCborEnabled() {
        return SdkSystemSetting.CBOR_ENABLED.getBooleanValueOrThrow();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AwsJsonProtocolFactory}.
     */
    public static final class Builder extends BaseAwsJsonProtocolFactory.Builder<Builder> {

        private Builder() {
        }

        public AwsCborProtocolFactory build() {
            return new AwsCborProtocolFactory(this);
        }

    }
}
