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

package software.amazon.awssdk.benchmark.enhanced.dynamodb;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.dynamodb.datamodeling.DynamoDBAttribute;
import software.amazon.awssdk.dynamodb.datamodeling.DynamoDBHashKey;
import software.amazon.awssdk.dynamodb.datamodeling.DynamoDBTable;

/**
 * Bean classes for the v2 DynamoDBMapper (dynamodb-mapper-v2).
 * Mirrors V1ItemFactory beans but uses software.amazon.awssdk.dynamodb.datamodeling annotations.
 */
public final class V2MapperItemFactory {

    @DynamoDBTable(tableName = "V2MapperTinyBean")
    public static class V2MapperTinyBean extends ItemFactory.TinyBean {

        public V2MapperTinyBean() {
        }

        public V2MapperTinyBean(String stringAttr) {
            super.setStringAttr(stringAttr);
        }

        @DynamoDBHashKey
        @Override
        public String getStringAttr() {
            return super.getStringAttr();
        }
    }

    @DynamoDBTable(tableName = "V2MapperSmallBean")
    public static class V2MapperSmallBean extends ItemFactory.SmallBean {
        private ByteBuffer binaryAttr;

        public V2MapperSmallBean() {
        }

        public V2MapperSmallBean(String stringAttr) {
            super.setStringAttr(stringAttr);
        }

        @DynamoDBHashKey
        @Override
        public String getStringAttr() {
            return super.getStringAttr();
        }

        @DynamoDBAttribute(attributeName = "binaryAttr")
        public ByteBuffer getBinaryAttrV2() {
            return binaryAttr;
        }

        public void setBinaryAttrV2(ByteBuffer binaryAttr) {
            this.binaryAttr = binaryAttr;
        }

        @DynamoDBAttribute
        @Override
        public List<String> getListAttr() {
            return super.getListAttr();
        }

        static V2MapperSmallBean fromSmallBean(ItemFactory.SmallBean sb) {
            V2MapperSmallBean b = new V2MapperSmallBean();
            b.setStringAttr(sb.getStringAttr());
            b.setBinaryAttrV2(sb.getBinaryAttr().asByteBuffer());
            b.setListAttr(sb.getListAttr());
            return b;
        }
    }

    @DynamoDBTable(tableName = "V2MapperHugeBean")
    public static class V2MapperHugeBean extends ItemFactory.HugeBean {
        private ByteBuffer binaryAttr;
        private Map<String, ByteBuffer> mapAttr1;
        private Map<String, List<ByteBuffer>> mapAttr2;
        private Map<String, List<Map<String, List<ByteBuffer>>>> mapAttr3;

        public V2MapperHugeBean() {
        }

        public V2MapperHugeBean(String stringAttr) {
            super.setStringAttr(stringAttr);
        }

        @DynamoDBHashKey
        @Override
        public String getStringAttr() {
            return super.getStringAttr();
        }

        @DynamoDBAttribute
        @Override
        public String getHashKey() {
            return super.getHashKey();
        }

        @DynamoDBAttribute(attributeName = "binaryAttr")
        public ByteBuffer getBinaryAttrV2() {
            return binaryAttr;
        }

        public void setBinaryAttrV2(ByteBuffer binaryAttr) {
            this.binaryAttr = binaryAttr;
        }

        @DynamoDBAttribute
        @Override
        public List<String> getListAttr() {
            return super.getListAttr();
        }

        @DynamoDBAttribute(attributeName = "mapAttr1")
        public Map<String, ByteBuffer> getMapAttr1V2() {
            return mapAttr1;
        }

        public void setMapAttr1V2(Map<String, ByteBuffer> mapAttr1) {
            this.mapAttr1 = mapAttr1;
        }

        @DynamoDBAttribute(attributeName = "mapAttr2")
        public Map<String, List<ByteBuffer>> getMapAttr2V2() {
            return mapAttr2;
        }

        public void setMapAttr2V2(Map<String, List<ByteBuffer>> mapAttr2) {
            this.mapAttr2 = mapAttr2;
        }

        @DynamoDBAttribute(attributeName = "mapAttr3")
        public Map<String, List<Map<String, List<ByteBuffer>>>> getMapAttr3V2() {
            return mapAttr3;
        }

        public void setMapAttr3V2(
                Map<String, List<Map<String, List<ByteBuffer>>>> mapAttr3) {
            this.mapAttr3 = mapAttr3;
        }

        static V2MapperHugeBean fromHugeBean(ItemFactory.HugeBean hb) {
            V2MapperHugeBean b = new V2MapperHugeBean();
            b.setHashKey(hb.getHashKey());
            b.setStringAttr(hb.getStringAttr());
            b.setBinaryAttrV2(hb.getBinaryAttr().asByteBuffer());
            b.setListAttr(hb.getListAttr());
            b.setMapAttr1V2(hb.getMapAttr1().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            e -> e.getValue().asByteBuffer())));
            b.setMapAttr2V2(hb.getMapAttr2().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            e -> e.getValue().stream()
                                    .map(SdkBytes::asByteBuffer)
                                    .collect(Collectors.toList()))));
            b.setMapAttr3V2(hb.getMapAttr3().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            e -> e.getValue().stream()
                                    .map(m -> m.entrySet().stream()
                                            .collect(Collectors.toMap(
                                                    Map.Entry::getKey,
                                                    ee -> ee.getValue().stream()
                                                            .map(SdkBytes::asByteBuffer)
                                                            .collect(Collectors.toList()))))
                                    .collect(Collectors.toList()))));
            return b;
        }
    }

    @DynamoDBTable(tableName = "V2MapperHugeBeanFlat")
    public static class V2MapperHugeBeanFlat extends ItemFactory.HugeBeanFlat {

        public V2MapperHugeBeanFlat() {
        }

        public V2MapperHugeBeanFlat(String stringAttr) {
            this.setStringAttr1(stringAttr);
        }

        @DynamoDBAttribute(attributeName = "stringAttr1")
        @DynamoDBHashKey
        @Override
        public String getStringAttr1() {
            return super.getStringAttr1();
        }

        @DynamoDBAttribute(attributeName = "stringAttr2")
        @Override
        public String getStringAttr2() {
            return super.getStringAttr2();
        }

        @DynamoDBAttribute(attributeName = "stringAttr3")
        @Override
        public String getStringAttr3() {
            return super.getStringAttr3();
        }

        @DynamoDBAttribute(attributeName = "stringAttr4")
        @Override
        public String getStringAttr4() {
            return super.getStringAttr4();
        }

        @DynamoDBAttribute(attributeName = "stringAttr5")
        @Override
        public String getStringAttr5() {
            return super.getStringAttr5();
        }

        @DynamoDBAttribute(attributeName = "stringAttr6")
        @Override
        public String getStringAttr6() {
            return super.getStringAttr6();
        }

        @DynamoDBAttribute(attributeName = "stringAttr7")
        @Override
        public String getStringAttr7() {
            return super.getStringAttr7();
        }

        @DynamoDBAttribute(attributeName = "stringAttr8")
        @Override
        public String getStringAttr8() {
            return super.getStringAttr8();
        }

        @DynamoDBAttribute(attributeName = "stringAttr9")
        @Override
        public String getStringAttr9() {
            return super.getStringAttr9();
        }

        @DynamoDBAttribute(attributeName = "stringAttr10")
        @Override
        public String getStringAttr10() {
            return super.getStringAttr10();
        }

        @DynamoDBAttribute(attributeName = "stringAttr11")
        @Override
        public String getStringAttr11() {
            return super.getStringAttr11();
        }

        @DynamoDBAttribute(attributeName = "stringAttr12")
        @Override
        public String getStringAttr12() {
            return super.getStringAttr12();
        }

        @DynamoDBAttribute(attributeName = "stringAttr13")
        @Override
        public String getStringAttr13() {
            return super.getStringAttr13();
        }

        @DynamoDBAttribute(attributeName = "stringAttr14")
        @Override
        public String getStringAttr14() {
            return super.getStringAttr14();
        }

        @DynamoDBAttribute(attributeName = "stringAttr15")
        @Override
        public String getStringAttr15() {
            return super.getStringAttr15();
        }

        @DynamoDBAttribute(attributeName = "stringAttr16")
        @Override
        public String getStringAttr16() {
            return super.getStringAttr16();
        }

        @DynamoDBAttribute(attributeName = "stringAttr17")
        @Override
        public String getStringAttr17() {
            return super.getStringAttr17();
        }

        @DynamoDBAttribute(attributeName = "stringAttr18")
        @Override
        public String getStringAttr18() {
            return super.getStringAttr18();
        }

        @DynamoDBAttribute(attributeName = "stringAttr19")
        @Override
        public String getStringAttr19() {
            return super.getStringAttr19();
        }

        @DynamoDBAttribute(attributeName = "stringAttr20")
        @Override
        public String getStringAttr20() {
            return super.getStringAttr20();
        }

        @DynamoDBAttribute(attributeName = "stringAttr21")
        @Override
        public String getStringAttr21() {
            return super.getStringAttr21();
        }

        @DynamoDBAttribute(attributeName = "stringAttr22")
        @Override
        public String getStringAttr22() {
            return super.getStringAttr22();
        }

        @DynamoDBAttribute(attributeName = "stringAttr23")
        @Override
        public String getStringAttr23() {
            return super.getStringAttr23();
        }

        @DynamoDBAttribute(attributeName = "stringAttr24")
        @Override
        public String getStringAttr24() {
            return super.getStringAttr24();
        }

        @DynamoDBAttribute(attributeName = "stringAttr25")
        @Override
        public String getStringAttr25() {
            return super.getStringAttr25();
        }

        @DynamoDBAttribute(attributeName = "stringAttr26")
        @Override
        public String getStringAttr26() {
            return super.getStringAttr26();
        }

        @DynamoDBAttribute(attributeName = "stringAttr27")
        @Override
        public String getStringAttr27() {
            return super.getStringAttr27();
        }

        @DynamoDBAttribute(attributeName = "stringAttr28")
        @Override
        public String getStringAttr28() {
            return super.getStringAttr28();
        }

        @DynamoDBAttribute(attributeName = "stringAttr29")
        @Override
        public String getStringAttr29() {
            return super.getStringAttr29();
        }

        @DynamoDBAttribute(attributeName = "stringAttr30")
        @Override
        public String getStringAttr30() {
            return super.getStringAttr30();
        }

        @DynamoDBAttribute(attributeName = "stringAttr31")
        @Override
        public String getStringAttr31() {
            return super.getStringAttr31();
        }

        @DynamoDBAttribute(attributeName = "stringAttr32")
        @Override
        public String getStringAttr32() {
            return super.getStringAttr32();
        }

        @DynamoDBAttribute(attributeName = "stringAttr33")
        @Override
        public String getStringAttr33() {
            return super.getStringAttr33();
        }

        @DynamoDBAttribute(attributeName = "stringAttr34")
        @Override
        public String getStringAttr34() {
            return super.getStringAttr34();
        }

        @DynamoDBAttribute(attributeName = "stringAttr35")
        @Override
        public String getStringAttr35() {
            return super.getStringAttr35();
        }

        @DynamoDBAttribute(attributeName = "stringAttr36")
        @Override
        public String getStringAttr36() {
            return super.getStringAttr36();
        }

        @DynamoDBAttribute(attributeName = "stringAttr37")
        @Override
        public String getStringAttr37() {
            return super.getStringAttr37();
        }

        @DynamoDBAttribute(attributeName = "stringAttr38")
        @Override
        public String getStringAttr38() {
            return super.getStringAttr38();
        }

        @DynamoDBAttribute(attributeName = "stringAttr39")
        @Override
        public String getStringAttr39() {
            return super.getStringAttr39();
        }

        @DynamoDBAttribute(attributeName = "stringAttr40")
        @Override
        public String getStringAttr40() {
            return super.getStringAttr40();
        }

        @DynamoDBAttribute(attributeName = "stringAttr41")
        @Override
        public String getStringAttr41() {
            return super.getStringAttr41();
        }

        @DynamoDBAttribute(attributeName = "stringAttr42")
        @Override
        public String getStringAttr42() {
            return super.getStringAttr42();
        }

        @DynamoDBAttribute(attributeName = "stringAttr43")
        @Override
        public String getStringAttr43() {
            return super.getStringAttr43();
        }

        @DynamoDBAttribute(attributeName = "stringAttr44")
        @Override
        public String getStringAttr44() {
            return super.getStringAttr44();
        }

        @DynamoDBAttribute(attributeName = "stringAttr45")
        @Override
        public String getStringAttr45() {
            return super.getStringAttr45();
        }

        @DynamoDBAttribute(attributeName = "stringAttr46")
        @Override
        public String getStringAttr46() {
            return super.getStringAttr46();
        }

        @DynamoDBAttribute(attributeName = "stringAttr47")
        @Override
        public String getStringAttr47() {
            return super.getStringAttr47();
        }

        @DynamoDBAttribute(attributeName = "stringAttr48")
        @Override
        public String getStringAttr48() {
            return super.getStringAttr48();
        }

        @DynamoDBAttribute(attributeName = "stringAttr49")
        @Override
        public String getStringAttr49() {
            return super.getStringAttr49();
        }

        @DynamoDBAttribute(attributeName = "stringAttr50")
        @Override
        public String getStringAttr50() {
            return super.getStringAttr50();
        }

        @DynamoDBAttribute(attributeName = "stringAttr51")
        @Override
        public String getStringAttr51() {
            return super.getStringAttr51();
        }

        @DynamoDBAttribute(attributeName = "stringAttr52")
        @Override
        public String getStringAttr52() {
            return super.getStringAttr52();
        }

        @DynamoDBAttribute(attributeName = "stringAttr53")
        @Override
        public String getStringAttr53() {
            return super.getStringAttr53();
        }

        @DynamoDBAttribute(attributeName = "stringAttr54")
        @Override
        public String getStringAttr54() {
            return super.getStringAttr54();
        }

        @DynamoDBAttribute(attributeName = "stringAttr55")
        @Override
        public String getStringAttr55() {
            return super.getStringAttr55();
        }

        @DynamoDBAttribute(attributeName = "stringAttr56")
        @Override
        public String getStringAttr56() {
            return super.getStringAttr56();
        }

        @DynamoDBAttribute(attributeName = "stringAttr57")
        @Override
        public String getStringAttr57() {
            return super.getStringAttr57();
        }

        @DynamoDBAttribute(attributeName = "stringAttr58")
        @Override
        public String getStringAttr58() {
            return super.getStringAttr58();
        }

        @DynamoDBAttribute(attributeName = "stringAttr59")
        @Override
        public String getStringAttr59() {
            return super.getStringAttr59();
        }

        @DynamoDBAttribute(attributeName = "stringAttr60")
        @Override
        public String getStringAttr60() {
            return super.getStringAttr60();
        }

        @DynamoDBAttribute(attributeName = "stringAttr61")
        @Override
        public String getStringAttr61() {
            return super.getStringAttr61();
        }

        @DynamoDBAttribute(attributeName = "stringAttr62")
        @Override
        public String getStringAttr62() {
            return super.getStringAttr62();
        }

        @DynamoDBAttribute(attributeName = "stringAttr63")
        @Override
        public String getStringAttr63() {
            return super.getStringAttr63();
        }

        public static V2MapperHugeBeanFlat fromHugeBeanFlat(
                ItemFactory.HugeBeanFlat b) {
            V2MapperHugeBeanFlat bean = new V2MapperHugeBeanFlat();
            for (int i = 1; i <= 63; ++i) {
                try {
                    Method setter = V2MapperHugeBeanFlat.class
                            .getMethod("setStringAttr" + i, String.class);
                    Method getter = ItemFactory.HugeBeanFlat.class
                            .getMethod("getStringAttr" + i);
                    setter.invoke(bean, getter.invoke(b));
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
            return bean;
        }
    }
}
