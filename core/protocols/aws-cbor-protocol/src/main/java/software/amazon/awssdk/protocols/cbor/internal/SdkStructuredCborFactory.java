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

package software.amazon.awssdk.protocols.cbor.internal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import java.util.function.BiFunction;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;

/**
 * Creates generators and protocol handlers for CBOR wire format.
 */
@SdkInternalApi
public abstract class SdkStructuredCborFactory {

    protected static final JsonFactory CBOR_FACTORY = new CBORFactory();

    protected static final CborGeneratorSupplier CBOR_GENERATOR_SUPPLIER = SdkCborGenerator::new;

    SdkStructuredCborFactory() {
    }

    @FunctionalInterface
    protected interface CborGeneratorSupplier extends BiFunction<JsonFactory, String, StructuredJsonGenerator> {
        @Override
        StructuredJsonGenerator apply(JsonFactory jsonFactory, String contentType);
    }
}
