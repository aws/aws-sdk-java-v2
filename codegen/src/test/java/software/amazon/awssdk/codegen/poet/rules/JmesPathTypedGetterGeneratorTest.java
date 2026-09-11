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

package software.amazon.awssdk.codegen.poet.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

class JmesPathTypedGetterGeneratorTest {

    @Test
    void sharedAllocatorAvoidsMethodParametersAndPreviousBindings() {
        IntermediateModel model = ClientTestModels.stringArrayServiceModels();
        JmesPathTypedGetterGenerator generator = new JmesPathTypedGetterGenerator(model);
        ShapeModel input = model.getOperation("ListOfObjectsOperation").getInputShape();
        NameAllocator names = generator.newNameAllocator();

        CodeBlock first = generator.lower(input, "Request.Value", "string", "firstParam", names);
        CodeBlock second = generator.lower(input, "Request.Value", "string", "secondParam", names);
        String generated = first.toString() + second.toString();

        assertTrue(generated.contains("request_ = request.request()"));
        assertTrue(generated.contains("request__ = request.request()"));
    }

    @Test
    void bareProjectionFallsBack() {
        IntermediateModel model = ClientTestModels.stringArrayServiceModels();
        JmesPathTypedGetterGenerator generator = new JmesPathTypedGetterGenerator(model);
        ShapeModel input = model.getOperation("TransactionOperation").getInputShape();

        assertThrows(UnsupportedOperationException.class,
                     () -> generator.lower(input, "TransactItems[*]", "stringarray", "stringArrayParam"));
    }

    @Test
    void collectionFlattenFallsBack() {
        IntermediateModel model = ClientTestModels.stringArrayServiceModels();
        JmesPathTypedGetterGenerator generator = new JmesPathTypedGetterGenerator(model);
        ShapeModel input = model.getOperation("ListOfObjectsOperation").getInputShape();

        assertThrows(UnsupportedOperationException.class,
                     () -> generator.lower(input, "nested.listOfObjects[*].aliases[]", "stringarray",
                                           "stringArrayParam"));
    }

    @Test
    void incompatibleTerminalConversionFallsBack() {
        IntermediateModel model = ClientTestModels.stringArrayServiceModels();
        JmesPathTypedGetterGenerator generator = new JmesPathTypedGetterGenerator(model);
        ShapeModel input = model.getOperation("ListOfObjectsOperation").getInputShape();

        assertThrows(UnsupportedOperationException.class,
                     () -> generator.lower(input, "Request.Value", "boolean", "booleanParam"));
    }

    @Test
    void idempotencyTokenFallsBack() {
        IntermediateModel model = ClientTestModels.stringArrayServiceModels();
        JmesPathTypedGetterGenerator generator = new JmesPathTypedGetterGenerator(model);
        ShapeModel input = model.getOperation("ListOfObjectsOperation").getInputShape();

        assertThrows(UnsupportedOperationException.class,
                     () -> generator.lower(input, "Request.Token", "string", "stringParam"));
    }

    @Test
    void customizedRuntimeDefaultFallsBack() {
        IntermediateModel model = ClientTestModels.stringArrayServiceModels();
        model.getCustomizationConfig().setModelMarshallerDefaultValueSupplier(
            Collections.singletonMap("Value", "example.DefaultSupplier"));
        JmesPathTypedGetterGenerator generator = new JmesPathTypedGetterGenerator(model);
        ShapeModel input = model.getOperation("ListOfObjectsOperation").getInputShape();

        assertThrows(UnsupportedOperationException.class,
                     () -> generator.lower(input, "Request.Value", "string", "stringParam"));
    }

    @Test
    void keysFiltersNullEntries() {
        IntermediateModel model = ClientTestModels.stringArrayServiceModels();
        JmesPathTypedGetterGenerator generator = new JmesPathTypedGetterGenerator(model);
        ShapeModel input = model.getOperation("MapKeysOperation").getInputShape();

        CodeBlock generated = generator.lower(input, "keys(RequestItems)", "stringarray", "stringArrayParam");

        assertTrue(generated.toString().contains("if (key != null)"));
    }

    @Test
    void keysLoopVariableDoesNotShadowResultLocal() {
        IntermediateModel model = ClientTestModels.stringArrayServiceModels();
        JmesPathTypedGetterGenerator generator = new JmesPathTypedGetterGenerator(model);
        ShapeModel input = model.getOperation("MapKeysOperation").getInputShape();

        CodeBlock generated = generator.lower(input, "keys(RequestItems)", "stringarray", "key");

        assertFalse(generated.toString().contains("for (java.lang.String key :"));
        assertFalse(generated.toString().contains("key.add(key)"));
    }

    @Test
    void keysLoopVariableParticipatesInSharedNamespace() {
        IntermediateModel model = ClientTestModels.stringArrayServiceModels();
        JmesPathTypedGetterGenerator generator = new JmesPathTypedGetterGenerator(model);
        ShapeModel input = model.getOperation("MapKeysOperation").getInputShape();
        NameAllocator names = generator.newNameAllocator();
        // Stand in for an earlier binding in the same method that already took "key".
        names.newName("key");

        CodeBlock generated = generator.lower(input, "keys(RequestItems)", "stringarray", "stringArrayParam", names);

        assertFalse(generated.toString().contains("for (java.lang.String key :"));
    }

    /**
     * {@code stringValues()} returns an immutable list when the projected prefix is null, so a projection behind a
     * nullable prefix must not hand the endpoint params an always-mutable list.
     */
    @Test
    void nullableProjectionPrefixSeedsAnImmutableEmptyList() {
        IntermediateModel model = ClientTestModels.stringArrayServiceModels();
        JmesPathTypedGetterGenerator generator = new JmesPathTypedGetterGenerator(model);
        ShapeModel input = model.getOperation("ListOfObjectsOperation").getInputShape();

        CodeBlock generated = generator.lower(input, "nested.listOfObjects[*].key", "stringarray", "stringArrayParam");

        assertTrue(generated.toString().contains("java.util.Collections.emptyList()"),
                   "expected the result to be seeded with an immutable empty list, but got: " + generated);
    }

    /**
     * A projection with no nullable prefix always reaches its loop, so it should keep allocating directly and stay
     * byte-identical to the previous output.
     */
    @Test
    void projectionWithoutNullablePrefixAllocatesDirectly() {
        IntermediateModel model = ClientTestModels.stringArrayServiceModels();
        JmesPathTypedGetterGenerator generator = new JmesPathTypedGetterGenerator(model);
        ShapeModel input = model.getOperation("TransactionOperation").getInputShape();

        CodeBlock generated = generator.lower(input, "TransactItems[*].Put.TableName", "stringarray",
                                              "stringArrayParam");

        assertFalse(generated.toString().contains("java.util.Collections.emptyList()"),
                    "unguarded projection should allocate directly, but got: " + generated);
    }
}
