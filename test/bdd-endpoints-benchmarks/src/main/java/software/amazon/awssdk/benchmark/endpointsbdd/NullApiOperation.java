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

package software.amazon.awssdk.benchmark.endpointsbdd;

import java.util.Collections;
import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Minimal stub {@link ApiOperation} used when building {@link software.amazon.smithy.java.endpoints.EndpointResolverParams}
 * for the smithy-java resolver. {@link software.amazon.smithy.java.endpoints.EndpointResolverParams}
 * requires a non-null {@code operation}, but since all endpoint parameters are supplied via
 * {@code ADDITIONAL_ENDPOINT_PARAMS} on the {@link software.amazon.smithy.java.context.Context},
 * the resolver never inspects the operation for context params. None of these methods are called.
 */
final class NullApiOperation implements ApiOperation<NullSerializableStruct, NullSerializableStruct> {

    static final NullApiOperation INSTANCE = new NullApiOperation();

    private static final Schema SCHEMA =
            Schema.createOperation(ShapeId.from("smithy.benchmark#NullOperation"));

    private static final ApiService SERVICE =
            () -> Schema.createService(ShapeId.from("smithy.benchmark#NullService"));

    private NullApiOperation() {
    }

    @Override
    public ShapeBuilder<NullSerializableStruct> inputBuilder() {
        throw new UnsupportedOperationException("NullApiOperation");
    }

    @Override
    public ShapeBuilder<NullSerializableStruct> outputBuilder() {
        throw new UnsupportedOperationException("NullApiOperation");
    }

    @Override
    public Schema schema() {
        return SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TypeRegistry.EMPTY;
    }

    @Override
    public List<ShapeId> effectiveAuthSchemes() {
        return Collections.emptyList();
    }

    @Override
    public List<Schema> errorSchemas() {
        return Collections.emptyList();
    }

    @Override
    public ApiService service() {
        return SERVICE;
    }

    @Override
    public String name() {
        return "NullOperation";
    }
}
