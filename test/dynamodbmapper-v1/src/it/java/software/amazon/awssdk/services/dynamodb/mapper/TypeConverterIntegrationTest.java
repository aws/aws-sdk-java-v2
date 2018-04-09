/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.junit.Assert.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMappingException;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTypeConverted;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTypeConverter;
import software.amazon.awssdk.services.dynamodb.pojos.AutoKeyAndVal;
import software.amazon.awssdk.services.dynamodb.pojos.Currency;

/**
 * Tests updating component attribute fields correctly.
 */
public class TypeConverterIntegrationTest extends AbstractKeyAndValIntegrationTestCase {

    /**
     * Test using {@code Currency}.
     */
    @Test
    public void testStringCurrency() {
        final KeyAndStringCurrency object = new KeyAndStringCurrency();
        object.setVal(new Currency(79.99D, "CAD"));
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test using {@code Currency}.
     */
    @Test
    public void testStringCurrencyNull() {
        final KeyAndStringCurrency object = new KeyAndStringCurrency();
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test using {@code Currency}.
     */
    @Test(expected = DynamoDbMappingException.class) //<- does not yet support lists/maps
    public void testCurrency() {
        final KeyAndCurrency object = new KeyAndCurrency();
        object.setVal(new Currency(69.99D, "CAD"));
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test using {@code Currency}.
     */
    @Test
    public void testStringSetCurrency() {
        final KeyAndStringSetCurrency object = new KeyAndStringSetCurrency();
        object.setVal(new HashSet<Currency>(Arrays.asList(new Currency(4.99D, "USD"), new Currency(5.99D, "USD"))));
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test using {@code Currency}.
     */
    @Test
    public void testStringSetCurrencyNull() {
        final KeyAndStringSetCurrency object = new KeyAndStringSetCurrency();
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test using {@code Currency}.
     */
    @Test
    public void testDoubleCurrency() {
        final KeyAndDoubleCurrency object = new KeyAndDoubleCurrency();
        object.setVal(new Currency(99.99D, "CAD"));

        final Currency currency = assertBeforeAndAfterChange(null, object);
        assertEquals(object.getVal().getAmount(), currency.getAmount());
        assertEquals("USD", currency.getUnit());
    }

    /**
     * Test using {@code Currency}.
     */
    @Test
    public void testDoubleCurrencyNull() {
        final KeyAndDoubleCurrency object = new KeyAndDoubleCurrency();
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test using {@code Currency}.
     */
    @Test
    public void testDoubleSetCurrency() {
        final KeyAndDoubleSetCurrency object = new KeyAndDoubleSetCurrency();
        object.setVal(new HashSet<Currency>(Arrays.asList(new Currency(28.99D, "USD"), new Currency(29.99D, "USD"))));
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test using {@code Currency}.
     */
    @Test
    public void testDoubleSetCurrencyNull() {
        final KeyAndDoubleSetCurrency object = new KeyAndDoubleSetCurrency();
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * An object with {@code Currency}.
     */
    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class KeyAndStringCurrency extends AutoKeyAndVal<Currency> {
        @CurrencyFormat(separator = "-")
        public Currency getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Currency val) {
            super.setVal(val);
        }

        @DynamoDbTypeConverted(converter = StringCurrencyConverter.class)
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.METHOD, ElementType.TYPE})
        public static @interface CurrencyFormat {
            String separator() default " ";
        }

        public static final class StringCurrencyConverter implements DynamoDbTypeConverter<String, Currency> {
            private final CurrencyFormat f;

            public StringCurrencyConverter(final Class<Currency> targetType, final CurrencyFormat f) {
                this.f = f;
            }

            @Override
            public String convert(final Currency object) {
                return new StringBuilder().append(object.getAmount()).append(f.separator()).append(object.getUnit()).toString();
            }

            @Override
            public Currency unconvert(final String object) {
                final String[] splits = object.split(f.separator());
                return new Currency(Double.valueOf(splits[0]), splits[1]);
            }
        }
    }

    /**
     * An object with {@code Currency}.
     */
    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class KeyAndCurrency extends AutoKeyAndVal<Currency> {
        @DynamoDbTypeConverted(converter = NoConvertCurrencyConverter.class)
        public Currency getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Currency val) {
            super.setVal(val);
        }

        public static final class NoConvertCurrencyConverter implements DynamoDbTypeConverter<Currency, Currency> {
            @Override
            public Currency convert(final Currency object) {
                return object;
            }

            @Override
            public Currency unconvert(final Currency object) {
                return object;
            }
        }
    }

    /**
     * An object with {@code Currency}.
     */
    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class KeyAndStringSetCurrency extends AutoKeyAndVal<Set<Currency>> {
        @CurrencyFormat(separator = "-")
        public Set<Currency> getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Set<Currency> val) {
            super.setVal(val);
        }

        @DynamoDbTypeConverted(converter = StringSetCurrencyConverter.class)
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.METHOD, ElementType.TYPE})
        public static @interface CurrencyFormat {
            String separator() default " ";
        }

        public static final class StringSetCurrencyConverter implements DynamoDbTypeConverter<Set<String>, Set<Currency>> {
            private final CurrencyFormat f;

            public StringSetCurrencyConverter(final Class<Currency> targetType, final CurrencyFormat f) {
                this.f = f;
            }

            @Override
            public Set<String> convert(final Set<Currency> object) {
                final Set<String> objects = new HashSet<String>();
                for (final Currency o : object) {
                    objects.add(new StringBuilder().append(o.getAmount()).append(f.separator()).append(o.getUnit()).toString());
                }
                return objects;
            }

            @Override
            public Set<Currency> unconvert(final Set<String> object) {
                final Set<Currency> objects = new HashSet<Currency>();
                for (final String o : object) {
                    final String[] splits = o.split(f.separator());
                    objects.add(new Currency(Double.valueOf(splits[0]), splits[1]));
                }
                return objects;
            }
        }
    }

    /**
     * An object with {@code Currency}.
     */
    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class KeyAndDoubleCurrency extends AutoKeyAndVal<Currency> {
        @CurrencyFormat
        public Currency getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Currency val) {
            super.setVal(val);
        }

        @DynamoDbTypeConverted(converter = DoubleCurrencyConverter.class)
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.METHOD, ElementType.TYPE})
        public static @interface CurrencyFormat {
            String separator() default " ";

            String unit() default "USD";
        }

        public static final class DoubleCurrencyConverter implements DynamoDbTypeConverter<Double, Currency> {
            private final CurrencyFormat f;

            public DoubleCurrencyConverter(final Class<Currency> targetType, final CurrencyFormat f) {
                this.f = f;
            }

            @Override
            public Double convert(final Currency object) {
                return object.getAmount();
            }

            @Override
            public Currency unconvert(final Double object) {
                return new Currency(object, f.unit());
            }
        }
    }

    /**
     * An object with {@code Currency}.
     */
    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class KeyAndDoubleSetCurrency extends AutoKeyAndVal<Set<Currency>> {
        @CurrencyFormat
        public Set<Currency> getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Set<Currency> val) {
            super.setVal(val);
        }

        @DynamoDbTypeConverted(converter = DoubleSetCurrencyConverter.class)
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.METHOD, ElementType.TYPE})
        public static @interface CurrencyFormat {
            String separator() default " ";

            String unit() default "USD";
        }

        public static final class DoubleSetCurrencyConverter implements DynamoDbTypeConverter<Set<Double>, Set<Currency>> {
            private final CurrencyFormat f;

            public DoubleSetCurrencyConverter(final Class<Currency> targetType, final CurrencyFormat f) {
                this.f = f;
            }

            @Override
            public Set<Double> convert(final Set<Currency> object) {
                final Set<Double> objects = new HashSet<Double>();
                for (final Currency o : object) {
                    objects.add(o.getAmount());
                }
                return objects;
            }

            @Override
            public Set<Currency> unconvert(final Set<Double> object) {
                final Set<Currency> objects = new HashSet<Currency>();
                for (final Double o : object) {
                    objects.add(new Currency(o, f.unit()));
                }
                return objects;
            }
        }
    }

}
