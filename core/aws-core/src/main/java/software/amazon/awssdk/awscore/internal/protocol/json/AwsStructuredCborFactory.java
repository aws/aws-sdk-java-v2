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

package software.amazon.awssdk.awscore.internal.protocol.json;

import com.fasterxml.jackson.core.JsonFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.protocol.json.SdkStructuredCborFactory;
import software.amazon.awssdk.core.protocol.json.StructuredJsonGenerator;

/**
 * Creates generators and protocol handlers for CBOR wire format.
 */
@SdkInternalApi
public final class AwsStructuredCborFactory extends SdkStructuredCborFactory {

    public static final AwsStructuredJsonFactory SDK_CBOR_FACTORY =
            new BaseAwsStructuredJsonFactory(CBOR_FACTORY, CBOR_SCALAR_UNMARSHALLERS) {
        @Override
        protected StructuredJsonGenerator createWriter(JsonFactory jsonFactory,
                                                       String contentType) {
            return CBOR_GENERATOR_SUPPLIER.apply(jsonFactory, contentType);
        }
    };

    protected AwsStructuredCborFactory() {
    }
}
