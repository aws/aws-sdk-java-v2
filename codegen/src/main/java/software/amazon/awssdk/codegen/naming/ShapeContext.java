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
import software.amazon.awssdk.codegen.model.service.Shape;

/**
 * A read-only view of a service model shape, exposing the predicates
 * ({@link #isUnion()}, {@link #isException()}, {@link #isList()},
 * {@link #isMap()}, {@link #isOrContainsEnum()}) that {@link NamingStrategy}
 * consults when deriving Java names for members and accessors.
 *
 * <p>Adapters for specific model formats are provided as static factories;
 * see {@link #ofC2j(Shape, Map)}.
 */
public interface ShapeContext {

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
     * Adapts a C2J {@link Shape} into a {@link ShapeContext}. Uses the same
     * predicates {@link Utils} exposes today, so behavior is unchanged from
     * pre-refactor call sites.
     *
     * @param shape the C2J shape (must not be null when the caller intends to
     *              query any of the shape-relative predicates).
     * @param allShapes the service's full shape map, needed for the
     *                  {@code isOrContainsEnum} recursion into list/map targets.
     */
    static ShapeContext ofC2j(Shape shape, Map<String, Shape> allShapes) {
        return new ShapeContext() {
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
}
