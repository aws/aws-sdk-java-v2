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

package software.amazon.awssdk.codegen.smithy.customization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;

/**
 * Tests for {@link ShapeIdResolver}.
 *
 * <p><b>Property 3: ShapeId Resolution Correctness</b> — verify resolve returns correct
 * ShapeId for existing shapes, throws for missing shapes, tryResolve returns Optional.empty
 * for missing shapes.
 * <p><b>Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 31.1</b>
 */
class ShapeIdResolverTest {

    private static final String NAMESPACE = "com.example.myservice";
    private static final ShapeId SERVICE_ID = ShapeId.from(NAMESPACE + "#MyService");

    private ServiceShape service;
    private StringShape existingString;
    private StructureShape existingStruct;
    private Model model;

    @BeforeEach
    void setUp() {
        service = ServiceShape.builder()
                              .id(SERVICE_ID)
                              .version("2024-01-01")
                              .build();
        existingString = StringShape.builder()
                                    .id(ShapeId.from(NAMESPACE + "#MyString"))
                                    .build();
        existingStruct = StructureShape.builder()
                                       .id(ShapeId.from(NAMESPACE + "#MyStruct"))
                                       .build();
        model = Model.builder()
                     .addShape(service)
                     .addShape(existingString)
                     .addShape(existingStruct)
                     .build();
    }

    /**
     * resolve() with an existing shape returns the correct ShapeId with matching
     * namespace and name.
     * Validates: Requirement 4.1
     */
    @Test
    void resolve_existingShape_returnsCorrectShapeId() {
        ShapeId result = ShapeIdResolver.resolve(model, service, "MyString");

        assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
        assertThat(result.getName()).isEqualTo("MyString");
        assertThat(result).isEqualTo(ShapeId.from(NAMESPACE + "#MyString"));
    }

    /**
     * resolve() works for different shape types (structure shape).
     * Validates: Requirement 4.1
     */
    @Test
    void resolve_existingStructureShape_returnsCorrectShapeId() {
        ShapeId result = ShapeIdResolver.resolve(model, service, "MyStruct");

        assertThat(result.getNamespace()).isEqualTo(NAMESPACE);
        assertThat(result.getName()).isEqualTo("MyStruct");
    }

    /**
     * resolve() with a missing shape throws IllegalStateException with a message
     * containing the shape name and namespace.
     * Validates: Requirements 4.2, 31.1
     */
    @Test
    void resolve_missingShape_throwsIllegalStateException() {
        assertThatThrownBy(() -> ShapeIdResolver.resolve(model, service, "NonExistent"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NonExistent")
            .hasMessageContaining(NAMESPACE);
    }

    /**
     * tryResolve() with an existing shape returns Optional containing the ShapeId.
     * Validates: Requirement 4.3
     */
    @Test
    void tryResolve_existingShape_returnsOptionalContainingShapeId() {
        Optional<ShapeId> result = ShapeIdResolver.tryResolve(model, service, "MyString");

        assertThat(result).isPresent();
        assertThat(result.get().getNamespace()).isEqualTo(NAMESPACE);
        assertThat(result.get().getName()).isEqualTo("MyString");
    }

    /**
     * tryResolve() with a missing shape returns Optional.empty().
     * Validates: Requirement 4.4
     */
    @Test
    void tryResolve_missingShape_returnsEmptyOptional() {
        Optional<ShapeId> result = ShapeIdResolver.tryResolve(model, service, "NonExistent");

        assertThat(result).isEmpty();
    }

    /**
     * namespace() returns the service namespace string.
     * Validates: Requirement 4.5
     */
    @Test
    void namespace_returnsServiceNamespace() {
        String result = ShapeIdResolver.namespace(service);

        assertThat(result).isEqualTo(NAMESPACE);
    }
}
