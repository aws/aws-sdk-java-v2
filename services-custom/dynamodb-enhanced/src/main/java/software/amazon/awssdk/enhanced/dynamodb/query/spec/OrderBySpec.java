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

package software.amazon.awssdk.enhanced.dynamodb.query.spec;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.SortDirection;

/**
 * Specification for ordering result rows by an attribute or by an aggregate output name.
 */
@SdkInternalApi
public final class OrderBySpec {
    private final String attributeOrAggregateName;
    private final SortDirection direction;
    private final boolean byAggregate;

    private OrderBySpec(String attributeOrAggregateName, SortDirection direction, boolean byAggregate) {
        this.attributeOrAggregateName = Objects.requireNonNull(attributeOrAggregateName, "attributeOrAggregateName");
        this.direction = Objects.requireNonNull(direction, "direction");
        this.byAggregate = byAggregate;
    }

    /**
     * Order by a row attribute (e.g. "customerId", "name").
     */
    public static OrderBySpec byAttribute(String attribute, SortDirection direction) {
        return new OrderBySpec(attribute, direction, false);
    }

    /**
     * Order by an aggregate output name (e.g. "orderCount").
     */
    public static OrderBySpec byAggregate(String aggregateOutputName, SortDirection direction) {
        return new OrderBySpec(aggregateOutputName, direction, true);
    }

    public String attributeOrAggregateName() {
        return attributeOrAggregateName;
    }

    public SortDirection direction() {
        return direction;
    }

    public boolean isByAggregate() {
        return byAggregate;
    }
}
