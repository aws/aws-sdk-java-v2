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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Expression;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A meta-extension that allows multiple extensions to be chained in a specified order to act as a single composite
 * extension. The order in which extensions will be used depends on the operation, for write operations they will be
 * called in forward order, for read operations they will be called in reverse order. For example :-
 *
 * If you create a chain of three extensions:
 * ChainMapperExtension.of(extension1, extension2, extension3);
 *
 * When performing any kind of write operation (eg: PutItem, UpdateItem) the beforeWrite() method will be called in
 * forward order:
 *
 * {@literal extension1 -> extension2 -> extension3}
 *
 * So the output of extension1 will be passed into extension2, and then the output of extension2 into extension3 and
 * so on. For operations that read (eg: GetItem, UpdateItem) the afterRead() method will be called in reverse order:
 *
 * {@literal extension3 -> extension2 -> extension1}
 *
 * This is designed to create a layered pattern when dealing with multiple extensions. One thing to note is that
 * UpdateItem acts as both a write operation and a read operation so the chain will be called both ways within a
 * single operation.
 */
@SdkPublicApi
public class ChainMapperExtension implements MapperExtension {
    private final Deque<MapperExtension> extensionChain;

    private ChainMapperExtension(List<MapperExtension> mapperExtensions) {
        this.extensionChain = new ArrayDeque<>(mapperExtensions);
    }

    /**
     * Construct a new instance of {@link ChainMapperExtension}.
     * @param mapperExtensions A list of {@link MapperExtension} to chain together.
     * @return A constructed {@link ChainMapperExtension} object.
     */
    public static ChainMapperExtension create(MapperExtension... mapperExtensions) {
        return new ChainMapperExtension(Arrays.asList(mapperExtensions));
    }

    /**
     * Implementation of the {@link MapperExtension} interface that will call all the chained extensions in forward
     * order, passing the results of each one to the next and coalescing the results into a single modification.
     * Multiple conditional statements will be separated by the string " AND ". Expression values will be coalesced
     * unless they conflict in which case an exception will be thrown.
     *
     * @param item The {@link AttributeValue} map of the item to be written.
     * @param tableMetadata A {@link TableMetadata} object describing the structure of the modelled table.
     * @return A single {@link WriteModification} representing the coalesced results of all the chained extensions.
     */
    @Override
    public WriteModification beforeWrite(Map<String, AttributeValue> item,
                                         OperationContext operationContext,
                                         TableMetadata tableMetadata) {
        AtomicReference<Map<String, AttributeValue>> transformedItem = new AtomicReference<>();
        AtomicReference<Expression> conditionalExpression = new AtomicReference<>();

        this.extensionChain.forEach(extension -> {
            Map<String, AttributeValue> itemToTransform = transformedItem.get() == null ? item : transformedItem.get();
            WriteModification writeModification = extension.beforeWrite(itemToTransform,
                                                                        operationContext,
                                                                        tableMetadata);

            if (writeModification.transformedItem() != null) {
                transformedItem.set(writeModification.transformedItem());
            }

            if (writeModification.additionalConditionalExpression() != null) {
                if (conditionalExpression.get() == null) {
                    conditionalExpression.set(writeModification.additionalConditionalExpression());
                } else {
                    conditionalExpression.set(
                        Expression.coalesce(conditionalExpression.get(),
                                            writeModification.additionalConditionalExpression(),
                                            " AND "));
                }
            }
        });

        return WriteModification.builder()
                                .transformedItem(transformedItem.get())
                                .additionalConditionalExpression(conditionalExpression.get())
                                .build();
    }

    /**
     * Implementation of the {@link MapperExtension} interface that will call all the chained extensions in reverse
     * order, passing the results of each one to the next and coalescing the results into a single modification.
     *
     * @param item The {@link AttributeValue} map of the item that is being read.
     * @param tableMetadata A {@link TableMetadata} object describing the structure of the modelled table.
     * @return A single {@link ReadModification} representing the final transformation of all the chained extensions.
     */
    @Override
    public ReadModification afterRead(Map<String, AttributeValue> item,
                                      OperationContext operationContext,
                                      TableMetadata tableMetadata) {
        AtomicReference<Map<String, AttributeValue>> transformedItem = new AtomicReference<>();

        this.extensionChain.descendingIterator().forEachRemaining(extension -> {
            Map<String, AttributeValue> itemToTransform = transformedItem.get() == null ? item : transformedItem.get();
            ReadModification readModification = extension.afterRead(itemToTransform, operationContext, tableMetadata);

            if (readModification.transformedItem() != null) {
                transformedItem.set(readModification.transformedItem());
            }
        });

        return ReadModification.builder()
                               .transformedItem(transformedItem.get())
                               .build();
    }
}
