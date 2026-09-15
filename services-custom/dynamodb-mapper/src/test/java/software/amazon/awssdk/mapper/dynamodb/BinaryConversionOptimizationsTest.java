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

package software.amazon.awssdk.mapper.dynamodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.mapper.dynamodb.internal.DynamoDBMapperModelFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class BinaryConversionOptimizationsTest {

    private static final DynamoDBMapperConfig DEFAULT_CONFIG = config(
        DynamoDBMapperConfig.ByteBufferReadBehavior.MUTABLE_COPY);
    private static final DynamoDBMapperConfig READ_ONLY_CONFIG = config(
        DynamoDBMapperConfig.ByteBufferReadBehavior.READ_ONLY);

    @Test
    public void byteBufferDefaultReturnsWritableDefensiveCopy() {
        DynamoDBMapperModelFactory factory = StandardModelFactories.of(S3Link.Factory.of(null));
        DynamoDBMapperTableModel<BinaryPojo> model = table(factory, DEFAULT_CONFIG);
        SdkBytes source = SdkBytes.fromByteArray(new byte[] {1, 2, 3});

        DynamoDBMapperFieldModel<BinaryPojo, ByteBuffer> field = model.field("binary");
        ByteBuffer result = field.unconvert(AttributeValue.createB(source));

        assertFalse(result.isReadOnly());
        result.put(0, (byte) 9);
        assertEquals(1, source.asByteBuffer().get(0));
    }

    @Test
    public void byteBufferReadOnlyOptInReturnsReadOnlyView() {
        DynamoDBMapperModelFactory factory = StandardModelFactories.of(S3Link.Factory.of(null));
        DynamoDBMapperTableModel<BinaryPojo> model = table(factory, READ_ONLY_CONFIG);
        SdkBytes source = SdkBytes.fromByteArray(new byte[] {1, 2, 3});

        DynamoDBMapperFieldModel<BinaryPojo, ByteBuffer> field = model.field("binary");
        ByteBuffer result = field.unconvert(AttributeValue.createB(source));

        assertTrue(result.isReadOnly());
        assertEquals(source.asByteBuffer(), result);
    }

    @Test
    public void byteBufferReadBehaviorUsesSeparateModelCaches() {
        DynamoDBMapperModelFactory factory = StandardModelFactories.of(S3Link.Factory.of(null));
        AttributeValue value = AttributeValue.createB(SdkBytes.fromUtf8String("data"));

        DynamoDBMapperFieldModel<BinaryPojo, ByteBuffer> writableField =
            table(factory, DEFAULT_CONFIG).field("binary");
        DynamoDBMapperFieldModel<BinaryPojo, ByteBuffer> readOnlyField =
            table(factory, READ_ONLY_CONFIG).field("binary");
        ByteBuffer writable = writableField.unconvert(value);
        ByteBuffer readOnly = readOnlyField.unconvert(value);

        assertFalse(writable.isReadOnly());
        assertTrue(readOnly.isReadOnly());
    }

    @Test
    public void byteBufferSetReadOnlyOptInReturnsReadOnlyViews() {
        DynamoDBMapperModelFactory factory = StandardModelFactories.of(S3Link.Factory.of(null));
        DynamoDBMapperTableModel<BinaryPojo> model = table(factory, READ_ONLY_CONFIG);
        SdkBytes source = SdkBytes.fromUtf8String("data");

        DynamoDBMapperFieldModel<BinaryPojo, Set<ByteBuffer>> field = model.field("binarySet");
        Set<ByteBuffer> result = field.unconvert(AttributeValue.createBs(Collections.singletonList(source)));

        assertTrue(result.iterator().next().isReadOnly());
    }

    @Test
    public void sdkBytesScalarRoundTripPreservesInstance() {
        DynamoDBMapperModelFactory factory = StandardModelFactories.of(S3Link.Factory.of(null));
        DynamoDBMapperFieldModel<BinaryPojo, SdkBytes> field = table(factory, DEFAULT_CONFIG).field("sdkBytes");
        SdkBytes source = SdkBytes.fromUtf8String("data");

        AttributeValue converted = field.convert(source);

        assertSame(source, converted.b());
        assertSame(source, field.unconvert(converted));
    }

    @Test
    public void sdkBytesSetRoundTripPreservesElementInstance() {
        DynamoDBMapperModelFactory factory = StandardModelFactories.of(S3Link.Factory.of(null));
        DynamoDBMapperFieldModel<BinaryPojo, Set<SdkBytes>> field = table(factory, DEFAULT_CONFIG).field("sdkBytesSet");
        SdkBytes source = SdkBytes.fromUtf8String("data");

        AttributeValue converted = field.convert(Collections.singleton(source));
        Set<SdkBytes> result = field.unconvert(converted);

        assertSame(source, converted.bs().get(0));
        assertSame(source, result.iterator().next());
    }

    @Test
    public void legacyItemConverterSupportsSdkBytes() throws Exception {
        ItemConverter converter = ConversionSchemas.V2.getConverter(new ConversionSchema.Dependencies());
        Method getter = BinaryPojo.class.getMethod("getSdkBytes");
        Method setter = BinaryPojo.class.getMethod("setSdkBytes", SdkBytes.class);
        SdkBytes source = SdkBytes.fromUtf8String("data");

        AttributeValue converted = converter.convert(getter, source);

        assertSame(source, converted.b());
        assertSame(source, converter.unconvert(getter, setter, converted));
    }

    private static DynamoDBMapperConfig config(DynamoDBMapperConfig.ByteBufferReadBehavior behavior) {
        return new DynamoDBMapperConfig.Builder()
            .withTypeConverterFactory(DynamoDBMapperConfig.DEFAULT.getTypeConverterFactory())
            .withConversionSchema(ConversionSchemas.V2)
            .withByteBufferReadBehavior(behavior)
            .build();
    }

    private static DynamoDBMapperTableModel<BinaryPojo> table(DynamoDBMapperModelFactory factory,
                                                               DynamoDBMapperConfig config) {
        return factory.getTableFactory(config).getTable(BinaryPojo.class);
    }

    @DynamoDBTable(tableName = "binary")
    public static class BinaryPojo {
        private String id;
        private ByteBuffer binary;
        private Set<ByteBuffer> binarySet;
        private SdkBytes sdkBytes;
        private Set<SdkBytes> sdkBytesSet;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDBAttribute
        public ByteBuffer getBinary() {
            return binary;
        }

        public void setBinary(ByteBuffer binary) {
            this.binary = binary;
        }

        @DynamoDBAttribute
        public Set<ByteBuffer> getBinarySet() {
            return binarySet;
        }

        public void setBinarySet(Set<ByteBuffer> binarySet) {
            this.binarySet = binarySet;
        }

        @DynamoDBAttribute
        public SdkBytes getSdkBytes() {
            return sdkBytes;
        }

        public void setSdkBytes(SdkBytes sdkBytes) {
            this.sdkBytes = sdkBytes;
        }

        @DynamoDBAttribute
        public Set<SdkBytes> getSdkBytesSet() {
            return sdkBytesSet;
        }

        public void setSdkBytesSet(Set<SdkBytes> sdkBytesSet) {
            this.sdkBytesSet = sdkBytesSet;
        }
    }
}
