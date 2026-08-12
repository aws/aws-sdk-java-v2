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

import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Minimal stub {@link SerializableStruct} used alongside {@link NullApiOperation} when building
 * {@link software.amazon.smithy.java.endpoints.EndpointResolverParams} for the smithy-java resolver.
 * {@link software.amazon.smithy.java.endpoints.EndpointResolverParams} requires a non-null
 * {@code inputValue}. Since all params are supplied via {@code ADDITIONAL_ENDPOINT_PARAMS}, the
 * resolver never reads members from this struct. None of these methods are called during resolution.
 */
final class NullSerializableStruct implements SerializableStruct {

    static final NullSerializableStruct INSTANCE = new NullSerializableStruct();

    private static final Schema SCHEMA =
            Schema.createOperation(ShapeId.from("smithy.benchmark#NullInput"));

    private NullSerializableStruct() {
    }

    @Override
    public Schema schema() {
        return SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        // no members — params come from ADDITIONAL_ENDPOINT_PARAMS
    }

    @Override
    public <T> T getMemberValue(Schema member) {
        return null;
    }
}
