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

package software.amazon.awssdk.protocols.ion.internal;

import com.fasterxml.jackson.core.JsonFactory;
import java.util.function.BiFunction;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;
import software.amazon.ion.IonSystem;
import software.amazon.ion.system.IonSystemBuilder;
import software.amazon.ion.system.IonWriterBuilder;

@SdkInternalApi
abstract class SdkStructuredIonFactory {

    protected static final IonSystem ION_SYSTEM = IonSystemBuilder.standard().build();

    protected static final JsonFactory JSON_FACTORY = new IonFactory(ION_SYSTEM);

    protected static final IonGeneratorSupplier ION_GENERATOR_SUPPLIER = SdkIonGenerator::create;

    SdkStructuredIonFactory() {
    }

    @FunctionalInterface
    protected interface IonGeneratorSupplier extends BiFunction<IonWriterBuilder, String, StructuredJsonGenerator> {
        StructuredJsonGenerator apply(IonWriterBuilder writerBuilder, String contentType);
    }
}
