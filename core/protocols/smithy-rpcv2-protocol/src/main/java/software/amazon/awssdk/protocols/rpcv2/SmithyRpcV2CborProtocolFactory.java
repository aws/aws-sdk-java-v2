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

package software.amazon.awssdk.protocols.rpcv2;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonContentTypeResolver;
import software.amazon.awssdk.protocols.json.StructuredJsonFactory;
import software.amazon.awssdk.protocols.rpcv2.internal.SdkRpcV2CborUnmarshaller;
import software.amazon.awssdk.protocols.rpcv2.internal.SdkRpcV2CborValueNodeFactory;
import software.amazon.awssdk.protocols.rpcv2.internal.SdkStructuredRpcV2CborFactory;

/**
 * Protocol factory for RPCv2 CBOR protocol.
 */
@SdkProtectedApi
public final class SmithyRpcV2CborProtocolFactory extends BaseAwsJsonProtocolFactory {

    /**
     * Content type resolver implementation for RPC_V2_CBOR enabled services.
     */
    private static final JsonContentTypeResolver RPC_V2_CBOR = protocolMetadata -> "application/cbor";

    private SmithyRpcV2CborProtocolFactory(Builder builder) {
        super(builder);
    }

    /**
     * @return Content type resolver implementation to use.
     */
    @Override
    protected JsonContentTypeResolver getContentTypeResolver() {
        return RPC_V2_CBOR;
    }

    /**
     * @return Instance of {@link StructuredJsonFactory} to use in creating handlers.
     */
    @Override
    protected StructuredJsonFactory getSdkFactory() {
        return SdkStructuredRpcV2CborFactory.SDK_CBOR_FACTORY;
    }

    /**
     * Smithy RPCv2 uses epoch seconds with millisecond decimal precision.
     */
    @Override
    protected Map<MarshallLocation, TimestampFormatTrait.Format> getDefaultTimestampFormats() {
        return LazyHolder.DEFAULT_TIMESTAMP_FORMATS;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SmithyRpcV2CborProtocolFactory}.
     */
    public static final class Builder extends BaseAwsJsonProtocolFactory.Builder<Builder> {

        private Builder() {
            jsonValueNodeFactory(SdkRpcV2CborValueNodeFactory.INSTANCE);
            timestampFormatRegistryFactory(SdkRpcV2CborUnmarshaller::timestampFormatRegistryFactory);
        }

        public SmithyRpcV2CborProtocolFactory build() {
            return new SmithyRpcV2CborProtocolFactory(this);
        }
    }

    // Lazy initialization holder class idiom
    private static class LazyHolder {
        private static final Map<MarshallLocation, TimestampFormatTrait.Format> DEFAULT_TIMESTAMP_FORMATS =
            createDefaultTimestampFormats();

        private LazyHolder() {
        }

        static Map<MarshallLocation, TimestampFormatTrait.Format> createDefaultTimestampFormats() {
            Map<MarshallLocation, TimestampFormatTrait.Format> formats = new EnumMap<>(MarshallLocation.class);
            formats.put(MarshallLocation.PAYLOAD, TimestampFormatTrait.Format.UNIX_TIMESTAMP);
            return Collections.unmodifiableMap(formats);

        }
    }
}
