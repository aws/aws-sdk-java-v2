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

package software.amazon.awssdk.protocols.rpcv2.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.json.BaseAwsStructuredJsonFactory;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;
import software.amazon.awssdk.thirdparty.jackson.dataformat.cbor.CBORFactory;
import software.amazon.awssdk.thirdparty.jackson.dataformat.cbor.CBORFactoryBuilder;
import software.amazon.awssdk.thirdparty.jackson.dataformat.cbor.CBORGenerator;

/**
 * Creates generators and protocol handlers for RPCv2 CBOR wire format.
 */
@SdkInternalApi
public final class SdkStructuredRpcV2CborFactory {

    private static final CBORFactory CBOR_FACTORY = new CBORFactoryBuilder(new CBORFactory())
        /* Allows integers to be represented using as little bytes as possible. This is the default, adding here for clarity */
        .enable(CBORGenerator.Feature.WRITE_MINIMAL_INTS)
        /* Allows doubles (8 bytes) to be represented as floats (4 bytes) when possible. */
        .enable(CBORGenerator.Feature.WRITE_MINIMAL_DOUBLES)
        .build();

    public static final BaseAwsStructuredJsonFactory SDK_CBOR_FACTORY =
        new BaseAwsStructuredJsonFactory(CBOR_FACTORY) {
            @Override
            protected StructuredJsonGenerator createWriter(JsonFactory jsonFactory,
                                                           String contentType) {
                return new SdkRpcV2CborGenerator(jsonFactory, contentType);
            }

            @Override
            public CBORFactory getJsonFactory() {
                return CBOR_FACTORY;
            }
        };

    private SdkStructuredRpcV2CborFactory() {
    }
}
