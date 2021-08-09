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

package software.amazon.awssdk.protocols.ion;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.protocols.ion.internal.AwsStructuredIonFactory;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.DefaultJsonContentTypeResolver;
import software.amazon.awssdk.protocols.json.JsonContentTypeResolver;
import software.amazon.awssdk.protocols.json.StructuredJsonFactory;

/**
 * Protocol factory for AWS/Ion protocols. Supports both JSON RPC and REST JSON versions of Ion. Defaults
 * to Ion Binary but will use Ion Text if the system setting {@link SdkSystemSetting#BINARY_ION_ENABLED} is
 * set to false.
 */
@SdkProtectedApi
public final class AwsIonProtocolFactory extends BaseAwsJsonProtocolFactory {

    /**
     * Content type resolver implementation for Ion-enabled services.
     */
    private static final JsonContentTypeResolver ION_BINARY = new DefaultJsonContentTypeResolver("application/x-amz-ion-");

    /**
     * Content type resolver implementation for debugging Ion-enabled services.
     */
    private static final JsonContentTypeResolver ION_TEXT = new DefaultJsonContentTypeResolver("text/x-amz-ion-");

    private AwsIonProtocolFactory(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return Content type resolver implementation to use.
     */
    @Override
    protected JsonContentTypeResolver getContentTypeResolver() {
        return isIonBinaryEnabled() ? ION_BINARY : ION_TEXT;
    }

    /**
     * @return Instance of {@link StructuredJsonFactory} to use in creating handlers.
     */
    @Override
    protected StructuredJsonFactory getSdkFactory() {
        return isIonBinaryEnabled()
               ? AwsStructuredIonFactory.SDK_ION_BINARY_FACTORY
               : AwsStructuredIonFactory.SDK_ION_TEXT_FACTORY;
    }

    private boolean isIonBinaryEnabled() {
        return SdkSystemSetting.BINARY_ION_ENABLED.getBooleanValueOrThrow();
    }

    /**
     * Builder for {@link AwsJsonProtocolFactory}.
     */
    public static final class Builder extends BaseAwsJsonProtocolFactory.Builder<Builder> {

        private Builder() {
        }

        public AwsIonProtocolFactory build() {
            return new AwsIonProtocolFactory(this);
        }

    }
}
