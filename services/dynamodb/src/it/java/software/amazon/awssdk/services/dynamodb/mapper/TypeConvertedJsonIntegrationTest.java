/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.mapper;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTypeConvertedJson;
import software.amazon.awssdk.services.dynamodb.pojos.AutoKeyAndVal;
import software.amazon.awssdk.services.dynamodb.pojos.Currency;

/**
 * Integration tests for {@code DynamoDBTypeConvertedJson}.
 */
public class TypeConvertedJsonIntegrationTest extends AbstractKeyAndValIntegrationTestCase {

    /**
     * Test marshalling.
     */
    @Test
    public void testMarshalling() {
        final KeyAndCurrency object = new KeyAndCurrency();
        object.setVal(new Currency(12.95D, "USD"));
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test marshalling a list.
     */
    @Test
    public void testListMarshalling() {
        final KeyAndCurrencyList object = new KeyAndCurrencyList();
        object.setVal(new ArrayList<Currency>());
        object.getVal().add(new Currency(1.99D, "CAD"));
        object.getVal().add(new Currency(2.99D, "CAD"));

        final List<Currency> after = assertBeforeAndAfterChange(false, object);
        for (final Currency currency : after) {
            assertNotNull(currency.getAmount());
            assertNotNull(currency.getUnit());
        }
    }

    /**
     * An object with a complex type.
     */
    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class KeyAndCurrency extends AutoKeyAndVal<Currency> {
        @DynamoDbTypeConvertedJson
        public Currency getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Currency val) {
            super.setVal(val);
        }
    }

    /**
     * An object with a complex type.
     */
    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class KeyAndCurrencyList extends AutoKeyAndVal<List<Currency>> {
        @DynamoDbTypeConvertedJson(targetType = CurrencyListType.class)
        public List<Currency> getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final List<Currency> val) {
            super.setVal(val);
        }

        public static final class CurrencyListType extends ArrayList<Currency> {
        }
    }

}
