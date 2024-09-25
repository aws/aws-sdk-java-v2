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

package software.amazon.awssdk.protocols.cbor;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.protocols.cbor.internal.AwsStructuredCborFactory;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.DefaultJsonContentTypeResolver;
import software.amazon.awssdk.protocols.json.JsonContentTypeResolver;
import software.amazon.awssdk.protocols.json.StructuredJsonFactory;
import software.amazon.awssdk.protocols.json.internal.unmarshall.DefaultProtocolUnmarshallDependencies;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonProtocolUnmarshaller;
import software.amazon.awssdk.protocols.json.internal.unmarshall.ProtocolUnmarshallDependencies;
import software.amazon.awssdk.protocols.jsoncore.JsonValueNodeFactory;
import software.amazon.awssdk.utils.Lazy;

/**
 * Protocol factory for AWS/CBOR protocols. Supports both JSON RPC and REST JSON versions of CBOR. Defaults to
 * the CBOR wire format but can fallback to standard JSON if the {@link SdkSystemSetting#CBOR_ENABLED} is
 * set to false.
 */
@SdkProtectedApi
public final class AwsCborProtocolFactory extends BaseAwsJsonProtocolFactory {

    private static final Lazy<ProtocolUnmarshallDependencies> PROTOCOL_UNMARSHALL_DEPENDENCIES_LAZY =
        new Lazy<>(AwsCborProtocolFactory::newProtocolUnmarshallDependencies);

    /**
     * Content type resolver implementation for AWS_CBOR enabled services.
     */
    private static final JsonContentTypeResolver AWS_CBOR = new DefaultJsonContentTypeResolver("application/x-amz-cbor-");

    /**
     * Indicates whether CBOR is enabled for this factory. This is populated and build time and not longer checked against the
     * system setting
     */
    private final boolean isSdkSystemSettingCborEnabled;

    private AwsCborProtocolFactory(Builder builder) {
        super(builder);
        this.isSdkSystemSettingCborEnabled = builder.isSdkSystemSettingCborEnabled;
    }

    /**
     * @return Content type resolver implementation to use.
     */
    @Override
    protected JsonContentTypeResolver getContentTypeResolver() {
        if (isCborEnabled()) {
            return AWS_CBOR;
        }
        return AWS_JSON;
    }

    /**
     * @return Instance of {@link StructuredJsonFactory} to use in creating handlers.
     */
    @Override
    protected StructuredJsonFactory getSdkFactory() {
        if (isCborEnabled()) {
            return AwsStructuredCborFactory.SDK_CBOR_FACTORY;
        } else {
            return super.getSdkFactory();
        }
    }

    /**
     * CBOR uses epoch millis for timestamps rather than epoch seconds with millisecond decimal precision like JSON protocols.
     */
    @Override
    protected Map<MarshallLocation, TimestampFormatTrait.Format> getDefaultTimestampFormats() {
        // If Cbor is disabled, getting the default timestamp format from parent class
        if (!isCborEnabled()) {
            return super.getDefaultTimestampFormats();
        }

        Map<MarshallLocation, TimestampFormatTrait.Format> formats = new EnumMap<>(MarshallLocation.class);
        formats.put(MarshallLocation.HEADER, TimestampFormatTrait.Format.RFC_822);
        formats.put(MarshallLocation.PAYLOAD, TimestampFormatTrait.Format.UNIX_TIMESTAMP_MILLIS);
        return Collections.unmodifiableMap(formats);
    }

    private boolean isCborEnabled() {
        return isSdkSystemSettingCborEnabled;
    }

    private static boolean isSdkSystemSettingCborEnabled() {
        return SdkSystemSetting.CBOR_ENABLED.getBooleanValueOrThrow();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ProtocolUnmarshallDependencies defaultProtocolUnmarshallDependencies() {
        return PROTOCOL_UNMARSHALL_DEPENDENCIES_LAZY.getValue();
    }

    public static DefaultProtocolUnmarshallDependencies newProtocolUnmarshallDependencies() {
        return DefaultProtocolUnmarshallDependencies
            .builder()
            .jsonUnmarshallerRegistry(JsonProtocolUnmarshaller.timestampFormatRegistryFactory(defaultFormats()))
            .nodeValueFactory(JsonValueNodeFactory.DEFAULT)
            .timestampFormats(defaultFormats())
            .jsonFactory(AwsStructuredCborFactory.SDK_CBOR_FACTORY.getJsonFactory())
            .build();
    }

    private static Map<MarshallLocation, TimestampFormatTrait.Format> defaultFormats() {
        Map<MarshallLocation, TimestampFormatTrait.Format> formats = new EnumMap<>(MarshallLocation.class);
        formats.put(MarshallLocation.HEADER, TimestampFormatTrait.Format.RFC_822);
        formats.put(MarshallLocation.PAYLOAD, TimestampFormatTrait.Format.UNIX_TIMESTAMP_MILLIS);
        return Collections.unmodifiableMap(formats);

    }

    /**
     * Builder for {@link AwsJsonProtocolFactory}.
     */
    public static final class Builder extends BaseAwsJsonProtocolFactory.Builder<Builder> {

        private boolean isSdkSystemSettingCborEnabled = isSdkSystemSettingCborEnabled();

        private Builder() {
        }

        public AwsCborProtocolFactory build() {
            if (this.isSdkSystemSettingCborEnabled) {
                protocolUnmarshallDependencies(AwsCborProtocolFactory::defaultProtocolUnmarshallDependencies);
            } else {
                protocolUnmarshallDependencies(JsonProtocolUnmarshaller::defaultProtocolUnmarshallDependencies);
            }
            return new AwsCborProtocolFactory(this);
        }
    }
}
