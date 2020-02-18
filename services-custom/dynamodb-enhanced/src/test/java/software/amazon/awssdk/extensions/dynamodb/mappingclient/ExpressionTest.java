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

package software.amazon.awssdk.extensions.dynamodb.mappingclient;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

public class ExpressionTest {
    @Test
    public void coalesce_correctlyWrapsExpressions() {
        Expression expression1 = Expression.builder().expression("one").build();
        Expression expression2 = Expression.builder().expression("two").build();
        Expression expression3 = Expression.builder().expression("three").build();

        Expression coalescedExpression = Expression.coalesce(Expression.coalesce(expression1, expression2, " AND "),
                                                             expression3, " AND ");

        String expectedExpression = "((one) AND (two)) AND (three)";
        assertThat(coalescedExpression.expression(), is(expectedExpression));
    }

}
