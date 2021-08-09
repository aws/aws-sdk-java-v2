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
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.json.BaseAwsStructuredJsonFactory;
import software.amazon.awssdk.protocols.json.ErrorCodeParser;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;
import software.amazon.ion.system.IonBinaryWriterBuilder;
import software.amazon.ion.system.IonTextWriterBuilder;
import software.amazon.ion.system.IonWriterBuilder;

@SdkInternalApi
public final class AwsStructuredIonFactory extends SdkStructuredIonFactory {
    private static final IonWriterBuilder BINARY_WRITER_BUILDER = IonBinaryWriterBuilder.standard().immutable();
    private static final IonWriterBuilder TEXT_WRITER_BUILDER = IonTextWriterBuilder.standard().immutable();


    public static final BaseAwsStructuredJsonFactory SDK_ION_BINARY_FACTORY =
        new AwsIonFactory(JSON_FACTORY, BINARY_WRITER_BUILDER);

    public static final BaseAwsStructuredJsonFactory SDK_ION_TEXT_FACTORY = new AwsIonFactory(JSON_FACTORY, TEXT_WRITER_BUILDER);

    static class AwsIonFactory extends BaseAwsStructuredJsonFactory {
        private final JsonFactory jsonFactory;

        private final IonWriterBuilder builder;


        AwsIonFactory(JsonFactory jsonFactory, IonWriterBuilder builder) {
            super(jsonFactory);
            this.jsonFactory = jsonFactory;
            this.builder = builder;
        }

        @Override
        protected StructuredJsonGenerator createWriter(JsonFactory jsonFactory, String contentType) {
            return ION_GENERATOR_SUPPLIER.apply(builder, contentType);
        }

        @Override
        public JsonFactory getJsonFactory() {
            return jsonFactory;
        }

        @Override
        public ErrorCodeParser getErrorCodeParser(String customErrorCodeFieldName) {
            return new CompositeErrorCodeParser(
                new IonErrorCodeParser(ION_SYSTEM),
                super.getErrorCodeParser(customErrorCodeFieldName));
        }
    }
}
