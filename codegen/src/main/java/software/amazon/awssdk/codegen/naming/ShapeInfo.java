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

package software.amazon.awssdk.codegen.naming;

import java.util.Map;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.ErrorTrait;

/**
 * A read-only view of a service model shape, exposing the predicates
 * ({@link #isUnion()}, {@link #isException()}, {@link #isList()},
 * {@link #isMap()}, {@link #isOrContainsEnum()}) that {@link NamingStrategy}
 * consults when deriving Java names for members and accessors.
 *
 * <p>Adapters for specific model formats are provided as static factories;
 * see {@link #ofC2j(software.amazon.awssdk.codegen.model.service.Shape, Map)}
 * for C2J and {@link #ofSmithy(Shape, Model)} for Smithy.
 */
public interface ShapeInfo {

    /**
     * @return true if the shape is a union (member named {@code "type"} is reserved).
     */
    boolean isUnion();

    /**
     * @return true if the shape is an exception (exception-reserved method names apply).
     */
    boolean isException();

    /**
     * @return true if the shape is or transitively contains an enum shape. For
     *         list and map shapes, this recurses into member targets.
     */
    boolean isOrContainsEnum();

    /**
     * @return true if the shape is a list.
     */
    boolean isList();

    /**
     * @return true if the shape is a map.
     */
    boolean isMap();

    /**
     * Adapts a C2J {@link software.amazon.awssdk.codegen.model.service.Shape}
     * into a {@link ShapeInfo}. Uses the same predicates {@link Utils} exposes
     * today, so behavior is unchanged from pre-refactor call sites.
     *
     * @param shape the C2J shape (must not be null when the caller intends to
     *              query any of the shape-relative predicates).
     * @param allShapes the service's full shape map, needed for the
     *                  {@code isOrContainsEnum} recursion into list/map targets.
     */
    static ShapeInfo ofC2j(software.amazon.awssdk.codegen.model.service.Shape shape,
                           Map<String, software.amazon.awssdk.codegen.model.service.Shape> allShapes) {
        return new ShapeInfo() {
            @Override
            public boolean isUnion() {
                return shape.isUnion();
            }

            @Override
            public boolean isException() {
                return shape.isException();
            }

            @Override
            public boolean isOrContainsEnum() {
                return Utils.isOrContainsEnumShape(shape, allShapes);
            }

            @Override
            public boolean isList() {
                return Utils.isListShape(shape);
            }

            @Override
            public boolean isMap() {
                return Utils.isMapShape(shape);
            }
        };
    }

    /**
     * Adapts a Smithy {@link Shape} into a {@link ShapeInfo}. Predicates are
     * derived from the shape's type (union, list, map) and traits (exception
     * via {@link ErrorTrait}); enum recursion looks at Smithy 2.0
     * {@code EnumShape}/{@code IntEnumShape} members through list and map
     * targets using the provided {@code Model}.
     *
     * @param shape the Smithy shape (must not be null when the caller
     *              intends to query any of the shape-relative predicates).
     * @param model the loaded Smithy model, needed to resolve list element
     *              and map key/value targets for the enum-recursion check.
     */
    static ShapeInfo ofSmithy(Shape shape, Model model) {
        return new ShapeInfo() {
            @Override
            public boolean isUnion() {
                return shape.isUnionShape();
            }

            @Override
            public boolean isException() {
                return shape.hasTrait(ErrorTrait.class);
            }

            @Override
            public boolean isOrContainsEnum() {
                return isOrContainsEnumSmithy(shape, model);
            }

            @Override
            public boolean isList() {
                return shape.isListShape();
            }

            @Override
            public boolean isMap() {
                return shape.isMapShape();
            }
        };
    }

    /**
     * Recursively determines whether a Smithy shape is (or transitively
     * contains) a Smithy 2.0 enum shape.
     */
    static boolean isOrContainsEnumSmithy(Shape shape, Model model) {
        if (shape.isEnumShape() || shape.isIntEnumShape()) {
            return true;
        }
        if (shape.isListShape()) {
            Shape element = model.expectShape(shape.asListShape().get().getMember().getTarget());
            return isOrContainsEnumSmithy(element, model);
        }
        if (shape.isMapShape()) {
            MapShape map = shape.asMapShape().get();
            Shape key = model.expectShape(map.getKey().getTarget());
            Shape value = model.expectShape(map.getValue().getTarget());
            return isOrContainsEnumSmithy(key, model) || isOrContainsEnumSmithy(value, model);
        }
        return false;
    }
}
