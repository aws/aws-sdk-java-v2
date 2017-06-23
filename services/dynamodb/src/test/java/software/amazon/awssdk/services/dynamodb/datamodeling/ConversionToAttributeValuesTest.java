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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class ConversionToAttributeValuesTest {

    private DynamoDbMapperModelFactory models;
    private DynamoDbMapperConfig finalConfig;

    public static boolean equals(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 != null) {
            return o1.equals(o2);
        }
        return false;
    }

    public static int hash(Object... objs) {
        int hash = 7;
        for (int i = 0; i < objs.length; ++i) {
            hash = hash * 31 + objs[i].hashCode();
        }
        return hash;
    }

    @Before
    public void setUp() throws Exception {
        finalConfig = new DynamoDbMapperConfig.Builder()
                .withTypeConverterFactory(DynamoDbMapperConfig.DEFAULT.getTypeConverterFactory())
                .withConversionSchema(ConversionSchemas.V2)
                .build();
        this.models = StandardModelFactories.of(S3Link.Factory.of(null));
    }

    @Test
    public void converterFailsForSubProperty() throws Exception {
        DynamoDbMapperTableModel<ConverterData> tableModel = getTable(ConverterData.class);
        Map<String, AttributeValue> withSubData = tableModel.convert(new ConverterData());
        assertEquals("bar", tableModel.unconvert(withSubData).subDocument().getaData().value());
    }

    private <T> DynamoDbMapperTableModel<T> getTable(Class<T> clazz) {
        return this.models.getTableFactory(finalConfig).getTable(clazz);
    }

    @DynamoDbTable(tableName = "test")
    public static class ConverterData {

        @DynamoDbTypeConverted(converter = CustomDataConverter.class)
        CustomData customConverted;
        @DynamoDbHashKey
        private String key;
        private ConverterSubDocument subDocument;

        public ConverterData() {
            customConverted = new CustomData("foo");
            subDocument = new ConverterSubDocument();
            subDocument.setaData(new CustomData("bar"));
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public ConverterSubDocument subDocument() {
            return subDocument;
        }

        public void setSubDocument(ConverterSubDocument subProperty) {
            this.subDocument = subProperty;
        }

        public CustomData getCustomConverted() {
            return customConverted;
        }

        public void setCustomConverted(CustomData customConverted) {
            this.customConverted = customConverted;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ConverterData that = (ConverterData) o;
            return ConversionToAttributeValuesTest.equals(subDocument, that.subDocument);
        }

        @Override
        public int hashCode() {
            return ConversionToAttributeValuesTest.hash(subDocument);
        }

    }

    @DynamoDbDocument
    public static class ConverterSubDocument {

        @DynamoDbTypeConverted(converter = CustomDataConverter.class)
        private CustomData aData;

        public CustomData getaData() {
            return aData;
        }

        public void setaData(CustomData aData) {
            this.aData = aData;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ConverterSubDocument that = (ConverterSubDocument) o;
            return ConversionToAttributeValuesTest.equals(aData, that.aData);
        }

        @Override
        public int hashCode() {
            return ConversionToAttributeValuesTest.hash(aData);
        }
    }

    public static class CustomData {

        private final String value;

        public CustomData(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CustomData that = (CustomData) o;
            return ConversionToAttributeValuesTest.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return ConversionToAttributeValuesTest.hash(value);
        }
    }

    public static class CustomDataConverter implements DynamoDbTypeConverter<String, CustomData> {

        public String convert(CustomData object) {
            return object.value();
        }

        public CustomData unconvert(String object) {
            return new CustomData(object);
        }
    }
}
