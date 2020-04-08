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

package software.amazon.awssdk.enhanced.dynamodb.internal.extensions;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.extensions.ReadModification;
import software.amazon.awssdk.enhanced.dynamodb.extensions.WriteModification;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A meta-extension that allows multiple extensions to be chained in a specified order to act as a single composite
 * extension. The order in which extensions will be used depends on the operation, for write operations they will be
 * called in forward order, for read operations they will be called in reverse order. For example :-
 *
 * <p>
 * If you create a chain of three extensions:
 * ChainMapperExtension.create(extension1, extension2, extension3);
 *
 * <p>
 * When performing any kind of write operation (eg: PutItem, UpdateItem) the beforeWrite() method will be called in
 * forward order:
 *
 * {@literal extension1 -> extension2 -> extension3}
 *
 * <p>
 * So the output of extension1 will be passed into extension2, and then the output of extension2 into extension3 and
 * so on. For operations that read (eg: GetItem, UpdateItem) the afterRead() method will be called in reverse order:
 *
 * {@literal extension3 -> extension2 -> extension1}
 *
 * <p>
 * This is designed to create a layered pattern when dealing with multiple extensions. One thing to note is that
 * UpdateItem acts as both a write operation and a read operation so the chain will be called both ways within a
 * single operation.
 */
@SdkInternalApi
public final class ChainExtension implements DynamoDbEnhancedClientExtension {
    private final Deque<DynamoDbEnhancedClientExtension> extensionChain;

    private ChainExtension(List<DynamoDbEnhancedClientExtension> extensions) {
        this.extensionChain = new ArrayDeque<>(extensions);
    }

    /**
     * Construct a new instance of {@link ChainExtension}.
     * @param extensions A list of {@link DynamoDbEnhancedClientExtension} to chain together.
     * @return A constructed {@link ChainExtension} object.
     */
    public static ChainExtension create(DynamoDbEnhancedClientExtension... extensions) {
        return new ChainExtension(Arrays.asList(extensions));
    }

    /**
     * Construct a new instance of {@link ChainExtension}.
     * @param extensions A list of {@link DynamoDbEnhancedClientExtension} to chain together.
     * @return A constructed {@link ChainExtension} object.
     */
    public static ChainExtension create(List<DynamoDbEnhancedClientExtension> extensions) {
        return new ChainExtension(extensions);
    }

    /**
     * Implementation of the {@link DynamoDbEnhancedClientExtension} interface that will call all the chained extensions
     * in forward order, passing the results of each one to the next and coalescing the results into a single modification.
     * Multiple conditional statements will be separated by the string " AND ". Expression values will be coalesced
     * unless they conflict in which case an exception will be thrown.
     *
     * @param context A {@link DynamoDbExtensionContext.BeforeWrite} context
     * @return A single {@link WriteModification} representing the coalesced results of all the chained extensions.
     */
    @Override
    public WriteModification beforeWrite(DynamoDbExtensionContext.BeforeWrite context) {
        Map<String, AttributeValue> transformedItem = null;
        Expression conditionalExpression = null;

        for (DynamoDbEnhancedClientExtension extension : this.extensionChain) {
            Map<String, AttributeValue> itemToTransform = transformedItem == null ? context.items() : transformedItem;

            DynamoDbExtensionContext.BeforeWrite beforeWrite =
                DefaultDynamoDbExtensionContext.builder()
                                               .items(itemToTransform)
                                               .operationContext(context.operationContext())
                                               .tableMetadata(context.tableMetadata())
                                               .build();

            WriteModification writeModification = extension.beforeWrite(beforeWrite);

            if (writeModification.transformedItem() != null) {
                transformedItem = writeModification.transformedItem();
            }

            if (writeModification.additionalConditionalExpression() != null) {
                if (conditionalExpression == null) {
                    conditionalExpression = writeModification.additionalConditionalExpression();
                } else {
                    conditionalExpression =
                        Expression.join(conditionalExpression,
                                        writeModification.additionalConditionalExpression(),
                                        " AND ");
                }
            }
        }

        return WriteModification.builder()
                                .transformedItem(transformedItem)
                                .additionalConditionalExpression(conditionalExpression)
                                .build();
    }

    /**
     * Implementation of the {@link DynamoDbEnhancedClientExtension} interface that will call all the chained extensions
     * in reverse order, passing the results of each one to the next and coalescing the results into a single modification.
     *
     * @param context A {@link DynamoDbExtensionContext.AfterRead} context
     * @return A single {@link ReadModification} representing the final transformation of all the chained extensions.
     */
    @Override
    public ReadModification afterRead(DynamoDbExtensionContext.AfterRead context) {
        Map<String, AttributeValue> transformedItem = null;

        Iterator<DynamoDbEnhancedClientExtension> iterator = extensionChain.descendingIterator();

        while (iterator.hasNext()) {
            Map<String, AttributeValue> itemToTransform =
                transformedItem == null ? context.items() : transformedItem;

            DynamoDbExtensionContext.AfterRead afterRead =
                DefaultDynamoDbExtensionContext.builder().items(itemToTransform)
                                               .operationContext(context.operationContext())
                                               .tableMetadata(context.tableMetadata())
                                               .build();

            ReadModification readModification = iterator.next().afterRead(afterRead);

            if (readModification.transformedItem() != null) {
                transformedItem = readModification.transformedItem();
            }
        }

        return ReadModification.builder()
                               .transformedItem(transformedItem)
                               .build();
    }
}
