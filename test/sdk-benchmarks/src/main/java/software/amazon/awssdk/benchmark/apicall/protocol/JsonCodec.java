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

package software.amazon.awssdk.benchmark.apicall.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.StructuredJsonFactory;
import software.amazon.awssdk.protocols.json.internal.marshall.JsonProtocolMarshallerBuilder;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonProtocolUnmarshaller;
import software.amazon.awssdk.protocols.json.internal.unmarshall.ProtocolUnmarshallDependencies;
import software.amazon.awssdk.protocols.rpcv2.SmithyRpcV2CborProtocolFactory;
import software.amazon.awssdk.utils.IoUtils;

/**
 * A codec to marshall/unmarshall shapes using a JSON protocol.
 */
public final class JsonCodec {

    private static final SdkClientConfiguration EMPTY_CLIENT_CONFIGURATION = SdkClientConfiguration.builder()
                                                                                                   .build();
    private static final OperationInfo EMPTY_OPERATION_INFO = OperationInfo.builder()
                                                                           .httpMethod(SdkHttpMethod.POST)
                                                                           .hasImplicitPayloadMembers(true)
                                                                           .build();


    /**
     * Returns the bytes as a SdkPojo instance.
     */
    public SdkPojo unmarshall(AwsJsonProtocol protocol, SdkPojo pojo, byte[] bytes) {
        try {
            ProtocolBehavior behavior = ProtocolBehavior.from(protocol);
            JsonProtocolUnmarshaller unmarshaller =
                JsonProtocolUnmarshaller
                    .builder()
                    .enableFastUnmarshalling(true)
                    .protocolUnmarshallDependencies(behavior.protocolUnmarshallDependencies())
                    .build();
            SdkHttpFullResponse response = SdkHttpFullResponse
                .builder()
                .statusCode(200)
                .putHeader("Content-Type", behavior.contentType())
                .content(AbortableInputStream.create(new ByteArrayInputStream(bytes)))
                .build();
            return unmarshaller.unmarshall(pojo, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the SdkPojo instance bytes marshalling.
     */
    public byte[] marshall(AwsJsonProtocol protocol, SdkPojo pojo) {
        try {
            ProtocolBehavior behavior = ProtocolBehavior.from(protocol);
            ProtocolMarshaller<SdkHttpFullRequest> marshaller =
                JsonProtocolMarshallerBuilder.create()
                                             .endpoint(new URI("http://localhost/"))
                                             .jsonGenerator(behavior.structuredJsonFactory().createWriter(behavior.contentType()))
                                             .contentType(behavior.contentType())
                                             .operationInfo(behavior.operationInfo())
                                             .sendExplicitNullForPayload(false)
                                             .protocolMetadata(behavior.protocolMetadata())
                                             .build();
            SdkHttpFullRequest req = marshaller.marshall(pojo);
            if (req.contentStreamProvider().isPresent()) {
                return IoUtils.toByteArray(req.contentStreamProvider().get().newStream());
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    static Supplier<StructuredJsonFactory> getStructuredJsonFactory(BaseAwsJsonProtocolFactory factory) {
        return () -> {
            try {
                Method method = BaseAwsJsonProtocolFactory.class.getDeclaredMethod("getSdkFactory");
                method.setAccessible(true);
                return (StructuredJsonFactory) method.invoke(factory);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    static Map<MarshallLocation, TimestampFormatTrait.Format> timestampFormats() {
        Map<MarshallLocation, TimestampFormatTrait.Format> formats = new EnumMap<>(MarshallLocation.class);
        formats.put(MarshallLocation.HEADER, TimestampFormatTrait.Format.RFC_822);
        formats.put(MarshallLocation.PAYLOAD, TimestampFormatTrait.Format.UNIX_TIMESTAMP);
        return Collections.unmodifiableMap(formats);
    }

    enum ProtocolBehavior {
        SMITHY_RPC_V2_CBOR(AwsJsonProtocol.SMITHY_RPC_V2_CBOR) {
            AwsJsonProtocolMetadata metadata = AwsJsonProtocolMetadata.builder()
                                                                      .protocol(AwsJsonProtocol.SMITHY_RPC_V2_CBOR)
                                                                      .build();
            BaseAwsJsonProtocolFactory factory = SmithyRpcV2CborProtocolFactory.builder()
                                                                               .protocol(AwsJsonProtocol.SMITHY_RPC_V2_CBOR)
                                                                               .clientConfiguration(EMPTY_CLIENT_CONFIGURATION)
                                                                               .build();
            Supplier<StructuredJsonFactory> structuredJsonFactory = getStructuredJsonFactory(factory);

            ProtocolUnmarshallDependencies rpcv2Dependencies = SmithyRpcV2CborProtocolFactory
                .defaultProtocolUnmarshallDependencies();

            @Override
            public AwsJsonProtocolMetadata protocolMetadata() {
                return metadata;
            }

            @Override
            public StructuredJsonFactory structuredJsonFactory() {
                return structuredJsonFactory.get();
            }

            @Override
            public ProtocolUnmarshallDependencies protocolUnmarshallDependencies() {
                return rpcv2Dependencies;
            }


            @Override
            public String contentType() {
                return "application/cbor";
            }

        },
        AWS_JSON(AwsJsonProtocol.AWS_JSON) {
            AwsJsonProtocolMetadata metadata = AwsJsonProtocolMetadata.builder()
                                                                      .protocol(AwsJsonProtocol.AWS_JSON)
                                                                      .contentType("application/json")
                                                                      .build();
            BaseAwsJsonProtocolFactory factory = AwsJsonProtocolFactory.builder()
                                                                       .protocol(AwsJsonProtocol.AWS_JSON)
                                                                       .clientConfiguration(EMPTY_CLIENT_CONFIGURATION)
                                                                       .build();

            Supplier<StructuredJsonFactory> structuredJsonFactory = getStructuredJsonFactory(factory);

            @Override
            public AwsJsonProtocolMetadata protocolMetadata() {
                return metadata;
            }

            @Override
            public StructuredJsonFactory structuredJsonFactory() {
                return structuredJsonFactory.get();
            }
        },
        ;

        private final AwsJsonProtocol protocol;
        private final ProtocolUnmarshallDependencies dpendencies = JsonProtocolUnmarshaller
            .defaultProtocolUnmarshallDependencies();

        ProtocolBehavior(AwsJsonProtocol protocol) {
            this.protocol = protocol;
        }

        public AwsJsonProtocolMetadata protocolMetadata() {
            throw new UnsupportedOperationException();
        }

        public StructuredJsonFactory structuredJsonFactory() {
            throw new UnsupportedOperationException();
        }

        public ProtocolUnmarshallDependencies protocolUnmarshallDependencies() {
            return dpendencies;
        }

        public OperationInfo operationInfo() {
            return EMPTY_OPERATION_INFO;
        }

        public String contentType() {
            return "application/json";
        }

        public static ProtocolBehavior from(AwsJsonProtocol protocol) {
            switch (protocol) {
                case SMITHY_RPC_V2_CBOR:
                    return SMITHY_RPC_V2_CBOR;
                case AWS_JSON:
                    return AWS_JSON;
                default:
                    throw new IllegalArgumentException("only SMITHY_RPC_V2_CBOR and AWS_JSON are supported");
            }
        }
    }
}