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

import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Resolves simple shape names from customization config (e.g., {@code "Error"})
 * to fully-qualified Smithy {@link ShapeId}s (e.g.,
 * {@code com.amazonaws.s3#Error})
 * using the service's namespace.
 */
public final class ShapeIdResolver {

    private ShapeIdResolver() {
    }

    /**
     * Extracts the simple shape name from a key that may be either a simple name
     * or a fully-qualified ShapeId (e.g.,
     * {@code "com.amazonaws.sqs#ListQueuesInput"}).
     * If the key contains {@code #}, returns the portion after it. Otherwise
     * returns
     * the key as-is.
     *
     * @param key the shape key (simple name or fully-qualified ShapeId)
     * @return the simple shape name
     */
    public static String toShapeName(String key) {
        int idx = key.indexOf('#');
        return idx >= 0 ? key.substring(idx + 1) : key;
    }

    /**
     * Resolves a shape name to a {@link ShapeId} within the service namespace.
     * If the name already contains {@code #} (i.e., it is already a fully-qualified
     * ShapeId), it is parsed directly and validated against the model. Otherwise,
     * constructs the candidate {@code namespace#simpleName} and verifies it exists
     * in the model.
     *
     * @param model   the Smithy model to search
     * @param service the service shape whose namespace is used
     * @param name    the shape name (simple or fully-qualified)
     * @return the resolved ShapeId
     * @throws IllegalStateException if the shape cannot be found in the model
     */
    public static ShapeId resolve(Model model, ServiceShape service, String name) {
        ShapeId candidate;
        if (name.contains("#")) {
            candidate = ShapeId.from(name);
        } else {
            String namespace = service.getId().getNamespace();
            candidate = ShapeId.from(namespace + "#" + name);
        }
        if (model.getShape(candidate).isPresent()) {
            return candidate;
        }
        throw new IllegalStateException(
                String.format("Cannot resolve shape '%s' in namespace '%s'. "
                        + "Shape not found in the Smithy model.", name,
                        service.getId().getNamespace()));
    }

    /**
     * Resolves a simple shape name, returning an empty {@link Optional} if the
     * shape does not exist in the model.
     *
     * @param model      the Smithy model to search
     * @param service    the service shape whose namespace is used
     * @param simpleName the simple (unqualified) shape name
     * @return an Optional containing the ShapeId if found, or empty
     */
    public static Optional<ShapeId> tryResolve(Model model, ServiceShape service, String simpleName) {
        String namespace = service.getId().getNamespace();
        ShapeId candidate = ShapeId.from(namespace + "#" + simpleName);
        return model.getShape(candidate).map(Shape::getId);
    }

    /**
     * Returns the namespace of the service (e.g., {@code "com.amazonaws.s3"}).
     *
     * @param service the service shape
     * @return the namespace string
     */
    public static String namespace(ServiceShape service) {
        return service.getId().getNamespace();
    }
}
