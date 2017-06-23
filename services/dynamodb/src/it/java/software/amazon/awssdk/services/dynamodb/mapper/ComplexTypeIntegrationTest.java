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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDBMapperIntegrationTestBase;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbHashKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTypeConverted;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTypeConvertedJson;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTypeConverter;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper;

/**
 * Tests of the configuration object
 */
public class ComplexTypeIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    // We don't start with the current system millis like other tests because
    // it's out of the range of some data types
    private static int start = 1;
    private static int byteStart = -127;

    @Test
    public void testComplexTypes() throws Exception {
        DynamoDbMapper util = new DynamoDbMapper(dynamo);

        ComplexClass obj = getUniqueObject();
        util.save(obj);
        ComplexClass loaded = util.load(ComplexClass.class, obj.getKey());
        assertEquals(obj, loaded);
    }

    private ComplexClass getUniqueObject() {
        ComplexClass obj = new ComplexClass();
        obj.setKey(String.valueOf(startKey++));
        obj.setBigDecimalAttribute(new BigDecimal(startKey++));
        obj.setBigIntegerAttribute(new BigInteger("" + startKey++));
        obj.setByteAttribute((byte) byteStart++);
        obj.setByteObjectAttribute(new Byte("" + byteStart++));
        obj.setDoubleAttribute(new Double("" + start++));
        obj.setDoubleObjectAttribute(new Double("" + start++));
        obj.setFloatAttribute(new Float("" + start++));
        obj.setFloatObjectAttribute(new Float("" + start++));
        obj.setIntAttribute(new Integer("" + start++));
        obj.setIntegerAttribute(new Integer("" + start++));
        obj.setLongAttribute(new Long("" + start++));
        obj.setLongObjectAttribute(new Long("" + start++));
        obj.setDateAttribute(new Date(startKey++));
        obj.setBooleanAttribute(start++ % 2 == 0);
        obj.setBooleanObjectAttribute(start++ % 2 == 0);
        obj.setExtraField("" + startKey++);
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(new Date(startKey++));
        obj.setCalendarAttribute(cal);
        obj.setComplexNestedType(new ComplexNestedType("" + start++, start++, new ComplexNestedType("" + start++,
                                                                                                    start++, null)));
        List<ComplexNestedType> complexTypes = new ArrayList<ComplexNestedType>();
        complexTypes.add(new ComplexNestedType("" + start++, start++,
                                               new ComplexNestedType("" + start++, start++, null)));
        complexTypes.add(new ComplexNestedType("" + start++, start++, new ComplexNestedType("" + start++, start++, null)));
        complexTypes.add(new ComplexNestedType("" + start++, start++, new ComplexNestedType("" + start++, start++, null)));
        obj.setComplexNestedTypeList(complexTypes);
        return obj;
    }

    /**
     * Tests using a complex type for a (string) key
     */
    @Test
    public void testComplexKey() throws Exception {
        ComplexKey obj = new ComplexKey();
        ComplexNestedType key = new ComplexNestedType();
        key.setIntValue(start++);
        key.setStringValue("" + start++);
        obj.setKey(key);
        obj.setOtherAttribute("" + start++);

        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);

        mapper.save(obj);
        ComplexKey loaded = mapper.load(ComplexKey.class, obj.getKey());
        assertEquals(obj, loaded);
    }

    public static final class ComplexNestedListTypeMarshaller implements DynamoDbTypeConverter<String, List<ComplexNestedType>> {
        @Override
        public String convert(final List<ComplexNestedType> object) {
            try {
                StringWriter writer = new StringWriter();
                JsonFactory jsonFactory = new MappingJsonFactory();
                JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(writer);
                jsonGenerator.writeObject(object);
                return writer.toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<ComplexNestedType> unconvert(String obj) {
            try {
                JsonFactory jsonFactory = new MappingJsonFactory();
                JsonParser jsonParser = jsonFactory.createJsonParser(new StringReader(obj));
                return jsonParser.readValueAs(new TypeReference<List<ComplexNestedType>>() {
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static final class ComplexClass extends NumberAttributeClass {

        private String extraField;
        private ComplexNestedType complexNestedType;
        private List<ComplexNestedType> complexNestedTypeList;

        @DynamoDbTypeConvertedJson
        public ComplexNestedType getComplexNestedType() {
            return complexNestedType;
        }

        public void setComplexNestedType(ComplexNestedType complexNestedType) {
            this.complexNestedType = complexNestedType;
        }

        @DynamoDbTypeConverted(converter = ComplexNestedListTypeMarshaller.class)
        public List<ComplexNestedType> getComplexNestedTypeList() {
            return complexNestedTypeList;
        }

        public void setComplexNestedTypeList(List<ComplexNestedType> complexNestedTypeList) {
            this.complexNestedTypeList = complexNestedTypeList;
        }

        public String getExtraField() {
            return extraField;
        }

        public void setExtraField(String extraField) {
            this.extraField = extraField;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((complexNestedType == null) ? 0 : complexNestedType.hashCode());
            result = prime * result + ((complexNestedTypeList == null) ? 0 : complexNestedTypeList.hashCode());
            result = prime * result + ((extraField == null) ? 0 : extraField.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ComplexClass other = (ComplexClass) obj;
            if (complexNestedType == null) {
                if (other.complexNestedType != null) {
                    return false;
                }
            } else if (!complexNestedType.equals(other.complexNestedType)) {
                return false;
            }
            if (complexNestedTypeList == null) {
                if (other.complexNestedTypeList != null) {
                    return false;
                }
            } else if (!complexNestedTypeList.equals(other.complexNestedTypeList)) {
                return false;
            }
            if (extraField == null) {
                if (other.extraField != null) {
                    return false;
                }
            } else if (!extraField.equals(other.extraField)) {
                return false;
            }
            return true;
        }

    }

    public static final class ComplexNestedType {

        private String stringValue;
        private Integer intValue;
        private ComplexNestedType nestedType;

        public ComplexNestedType() {
        }

        public ComplexNestedType(String stringValue, Integer intValue, ComplexNestedType nestedType) {
            super();
            this.stringValue = stringValue;
            this.intValue = intValue;
            this.nestedType = nestedType;
        }

        public String getStringValue() {
            return stringValue;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }

        public Integer getIntValue() {
            return intValue;
        }

        public void setIntValue(Integer intValue) {
            this.intValue = intValue;
        }

        public ComplexNestedType getNestedType() {
            return nestedType;
        }

        public void setNestedType(ComplexNestedType nestedType) {
            this.nestedType = nestedType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((intValue == null) ? 0 : intValue.hashCode());
            result = prime * result + ((nestedType == null) ? 0 : nestedType.hashCode());
            result = prime * result + ((stringValue == null) ? 0 : stringValue.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ComplexNestedType other = (ComplexNestedType) obj;
            if (intValue == null) {
                if (other.intValue != null) {
                    return false;
                }
            } else if (!intValue.equals(other.intValue)) {
                return false;
            }
            if (nestedType == null) {
                if (other.nestedType != null) {
                    return false;
                }
            } else if (!nestedType.equals(other.nestedType)) {
                return false;
            }
            if (stringValue == null) {
                if (other.stringValue != null) {
                    return false;
                }
            } else if (!stringValue.equals(other.stringValue)) {
                return false;
            }
            return true;
        }

    }

    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static final class ComplexKey {

        private ComplexNestedType key;
        private String otherAttribute;

        @DynamoDbHashKey
        @DynamoDbTypeConvertedJson
        public ComplexNestedType getKey() {
            return key;
        }

        public void setKey(ComplexNestedType key) {
            this.key = key;
        }

        public String getOtherAttribute() {
            return otherAttribute;
        }

        public void setOtherAttribute(String otherAttribute) {
            this.otherAttribute = otherAttribute;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((otherAttribute == null) ? 0 : otherAttribute.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ComplexKey other = (ComplexKey) obj;
            if (key == null) {
                if (other.key != null) {
                    return false;
                }
            } else if (!key.equals(other.key)) {
                return false;
            }
            if (otherAttribute == null) {
                if (other.otherAttribute != null) {
                    return false;
                }
            } else if (!otherAttribute.equals(other.otherAttribute)) {
                return false;
            }
            return true;
        }
    }

}
