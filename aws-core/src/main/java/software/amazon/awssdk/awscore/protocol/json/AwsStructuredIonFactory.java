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

package software.amazon.awssdk.awscore.protocol.json;

import com.fasterxml.jackson.core.JsonFactory;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.json.SdkStructuredIonFactory;
import software.amazon.awssdk.core.protocol.json.StructuredJsonGenerator;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;
import software.amazon.ion.system.IonBinaryWriterBuilder;
import software.amazon.ion.system.IonTextWriterBuilder;
import software.amazon.ion.system.IonWriterBuilder;

@SdkInternalApi
final class AwsStructuredIonFactory extends SdkStructuredIonFactory {
    private static final IonWriterBuilder BINARY_WRITER_BUILDER = IonBinaryWriterBuilder.standard().immutable();
    private static final IonWriterBuilder TEXT_WRITER_BUILDER = IonTextWriterBuilder.standard().immutable();


    static final AwsStructuredJsonFactory SDK_ION_BINARY_FACTORY = new AwsIonFactory(JSON_FACTORY, UNMARSHALLERS,
                                                                                     BINARY_WRITER_BUILDER);

    static final AwsStructuredJsonFactory SDK_ION_TEXT_FACTORY = new AwsIonFactory(JSON_FACTORY, UNMARSHALLERS,
                                                                                   TEXT_WRITER_BUILDER);

    static class AwsIonFactory extends BaseAwsStructuredJsonFactory {
        private final IonWriterBuilder builder;

        AwsIonFactory(JsonFactory jsonFactory, Map<Class<?>, Unmarshaller<?, JsonUnmarshallerContext>> unmarshallers,
                      IonWriterBuilder builder) {
            super(jsonFactory, unmarshallers);
            this.builder = builder;
        }

        @Override
        protected StructuredJsonGenerator createWriter(JsonFactory jsonFactory, String contentType) {
            return ION_GENERATOR_SUPPLIER.apply(builder, contentType);
        }

        @Override
        protected ErrorCodeParser getErrorCodeParser(String customErrorCodeFieldName) {
            return new CompositeErrorCodeParser(
                new IonErrorCodeParser(ION_SYSTEM),
                new JsonErrorCodeParser(customErrorCodeFieldName));
        }
    }
}
