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

import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import com.amazonaws.util.ImmutableMapParameter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public final class V2ItemFactory extends ItemFactory<AttributeValue> {
    public static final TableSchema<ItemFactory.TinyBean> TINY_BEAN_TABLE_SCHEMA =
            TableSchema.builder(ItemFactory.TinyBean.class)
                    .newItemSupplier(ItemFactory.TinyBean::new)
                    .addAttribute(String.class, a -> a.name("stringAttr")
                            .getter(ItemFactory.TinyBean::getStringAttr)
                            .setter(ItemFactory.TinyBean::setStringAttr)
                            .tags(primaryPartitionKey()))
                    .build();

    public static final TableSchema<ItemFactory.SmallBean> SMALL_BEAN_TABLE_SCHEMA =
            TableSchema.builder(ItemFactory.SmallBean.class)
                    .newItemSupplier(ItemFactory.SmallBean::new)
                    .addAttribute(String.class, a -> a.name("stringAttr")
                            .getter(ItemFactory.SmallBean::getStringAttr)
                            .setter(ItemFactory.SmallBean::setStringAttr)
                            .tags(primaryPartitionKey()))
                    .addAttribute(SdkBytes.class, a -> a.name("binaryAttr")
                            .getter(ItemFactory.SmallBean::getBinaryAttr)
                            .setter(ItemFactory.SmallBean::setBinaryAttr))
                    .addAttribute(EnhancedType.listOf(String.class), a -> a.name("listAttr")
                            .getter(ItemFactory.SmallBean::getListAttr)
                            .setter(ItemFactory.SmallBean::setListAttr))
                    .build();

    public static final TableSchema<ItemFactory.HugeBean> HUGE_BEAN_TABLE_SCHEMA =
            TableSchema.builder(ItemFactory.HugeBean.class)
                    .newItemSupplier(ItemFactory.HugeBean::new)
                    .addAttribute(String.class, a -> a.name("stringAttr")
                            .getter(ItemFactory.HugeBean::getStringAttr)
                            .setter(ItemFactory.HugeBean::setStringAttr)
                            .tags(primaryPartitionKey()))
                    .addAttribute(String.class, a -> a.name("hashKey")
                            .getter(ItemFactory.HugeBean::getHashKey)
                            .setter(ItemFactory.HugeBean::setHashKey))
                    .addAttribute(SdkBytes.class, a -> a.name("binaryAttr")
                            .getter(ItemFactory.HugeBean::getBinaryAttr)
                            .setter(ItemFactory.HugeBean::setBinaryAttr))
                    .addAttribute(EnhancedType.listOf(String.class), a -> a.name("listAttr")
                            .getter(ItemFactory.HugeBean::getListAttr)
                            .setter(ItemFactory.HugeBean::setListAttr))
                    .addAttribute(new EnhancedType<Map<String, SdkBytes>>() {
                    }, a -> a.name("mapAttr1")
                            .getter(ItemFactory.HugeBean::getMapAttr1)
                            .setter(ItemFactory.HugeBean::setMapAttr1))
                    .addAttribute(new EnhancedType<Map<String, List<SdkBytes>>>() {
                    }, a -> a.name("mapAttr2")
                            .getter(ItemFactory.HugeBean::getMapAttr2)
                            .setter(ItemFactory.HugeBean::setMapAttr2))
                    .addAttribute(new EnhancedType<Map<String, List<Map<String, List<SdkBytes>>>>>() {
                    }, a -> a.name("mapAttr3")
                            .getter(ItemFactory.HugeBean::getMapAttr3)
                            .setter(ItemFactory.HugeBean::setMapAttr3))
                    .build();

    public static final TableSchema<ItemFactory.HugeBeanFlat> HUGE_BEAN_FLAT_TABLE_SCHEMA =
            TableSchema.builder(ItemFactory.HugeBeanFlat.class)
                    .newItemSupplier(ItemFactory.HugeBeanFlat::new)
                    .addAttribute(String.class, a -> a.name("stringAttr")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr1)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr1)
                            .tags(primaryPartitionKey()))
                    .addAttribute(String.class, a -> a.name("stringAttr2")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr2)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr2))
                    .addAttribute(String.class, a -> a.name("stringAttr3")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr3)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr3))
                    .addAttribute(String.class, a -> a.name("stringAttr4")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr4)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr4))
                    .addAttribute(String.class, a -> a.name("stringAttr5")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr5)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr5))
                    .addAttribute(String.class, a -> a.name("stringAttr6")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr6)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr6))
                    .addAttribute(String.class, a -> a.name("stringAttr7")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr7)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr7))
                    .addAttribute(String.class, a -> a.name("stringAttr8")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr8)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr8))
                    .addAttribute(String.class, a -> a.name("stringAttr9")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr9)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr9))
                    .addAttribute(String.class, a -> a.name("stringAttr10")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr10)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr10))
                    .addAttribute(String.class, a -> a.name("stringAttr11")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr11)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr11))
                    .addAttribute(String.class, a -> a.name("stringAttr12")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr12)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr12))
                    .addAttribute(String.class, a -> a.name("stringAttr13")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr13)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr13))
                    .addAttribute(String.class, a -> a.name("stringAttr14")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr14)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr14))
                    .addAttribute(String.class, a -> a.name("stringAttr15")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr15)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr15))
                    .addAttribute(String.class, a -> a.name("stringAttr16")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr16)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr16))
                    .addAttribute(String.class, a -> a.name("stringAttr17")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr17)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr17))
                    .addAttribute(String.class, a -> a.name("stringAttr18")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr18)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr18))
                    .addAttribute(String.class, a -> a.name("stringAttr19")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr19)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr19))
                    .addAttribute(String.class, a -> a.name("stringAttr20")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr20)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr20))
                    .addAttribute(String.class, a -> a.name("stringAttr21")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr21)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr21))
                    .addAttribute(String.class, a -> a.name("stringAttr22")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr22)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr22))
                    .addAttribute(String.class, a -> a.name("stringAttr23")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr23)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr23))
                    .addAttribute(String.class, a -> a.name("stringAttr24")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr24)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr24))
                    .addAttribute(String.class, a -> a.name("stringAttr25")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr25)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr25))
                    .addAttribute(String.class, a -> a.name("stringAttr26")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr26)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr26))
                    .addAttribute(String.class, a -> a.name("stringAttr27")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr27)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr27))
                    .addAttribute(String.class, a -> a.name("stringAttr28")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr28)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr28))
                    .addAttribute(String.class, a -> a.name("stringAttr29")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr29)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr29))
                    .addAttribute(String.class, a -> a.name("stringAttr30")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr30)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr30))
                    .addAttribute(String.class, a -> a.name("stringAttr31")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr31)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr31))
                    .addAttribute(String.class, a -> a.name("stringAttr32")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr32)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr32))
                    .addAttribute(String.class, a -> a.name("stringAttr33")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr33)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr33))
                    .addAttribute(String.class, a -> a.name("stringAttr34")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr34)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr34))
                    .addAttribute(String.class, a -> a.name("stringAttr35")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr35)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr35))
                    .addAttribute(String.class, a -> a.name("stringAttr36")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr36)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr36))
                    .addAttribute(String.class, a -> a.name("stringAttr37")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr37)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr37))
                    .addAttribute(String.class, a -> a.name("stringAttr38")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr38)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr38))
                    .addAttribute(String.class, a -> a.name("stringAttr39")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr39)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr39))
                    .addAttribute(String.class, a -> a.name("stringAttr40")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr40)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr40))
                    .addAttribute(String.class, a -> a.name("stringAttr41")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr41)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr41))
                    .addAttribute(String.class, a -> a.name("stringAttr42")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr42)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr42))
                    .addAttribute(String.class, a -> a.name("stringAttr43")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr43)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr43))
                    .addAttribute(String.class, a -> a.name("stringAttr44")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr44)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr44))
                    .addAttribute(String.class, a -> a.name("stringAttr45")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr45)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr45))
                    .addAttribute(String.class, a -> a.name("stringAttr46")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr46)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr46))
                    .addAttribute(String.class, a -> a.name("stringAttr47")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr47)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr47))
                    .addAttribute(String.class, a -> a.name("stringAttr48")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr48)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr48))
                    .addAttribute(String.class, a -> a.name("stringAttr49")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr49)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr49))
                    .addAttribute(String.class, a -> a.name("stringAttr50")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr50)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr50))
                    .addAttribute(String.class, a -> a.name("stringAttr51")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr51)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr51))
                    .addAttribute(String.class, a -> a.name("stringAttr52")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr52)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr52))
                    .addAttribute(String.class, a -> a.name("stringAttr53")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr53)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr53))
                    .addAttribute(String.class, a -> a.name("stringAttr54")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr54)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr54))
                    .addAttribute(String.class, a -> a.name("stringAttr55")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr55)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr55))
                    .addAttribute(String.class, a -> a.name("stringAttr56")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr56)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr56))
                    .addAttribute(String.class, a -> a.name("stringAttr57")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr57)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr57))
                    .addAttribute(String.class, a -> a.name("stringAttr58")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr58)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr58))
                    .addAttribute(String.class, a -> a.name("stringAttr59")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr59)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr59))
                    .addAttribute(String.class, a -> a.name("stringAttr60")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr60)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr60))
                    .addAttribute(String.class, a -> a.name("stringAttr61")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr61)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr61))
                    .addAttribute(String.class, a -> a.name("stringAttr62")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr62)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr62))
                    .addAttribute(String.class, a -> a.name("stringAttr63")
                            .getter(ItemFactory.HugeBeanFlat::getStringAttr63)
                            .setter(ItemFactory.HugeBeanFlat::setStringAttr63))
                    .build();


    protected Map<String, AttributeValue> asItem(TinyBean b) {
        ImmutableMapParameter.Builder<String, AttributeValue> builder = ImmutableMapParameter.builder();

        builder.put("stringAttr", av(b.getStringAttr()));

        return builder.build();
    }

    protected Map<String, AttributeValue> asItem(SmallBean b) {
        ImmutableMapParameter.Builder<String, AttributeValue> builder = ImmutableMapParameter.builder();

        builder.put("stringAttr", av(b.getStringAttr()));
        builder.put("binaryAttr", av(b.getBinaryAttr()));

        List<AttributeValue> listAttr = b.getListAttr().stream().map(this::av).collect(Collectors.toList());

        builder.put("listAttr", av(listAttr));

        return builder.build();
    }

    protected Map<String, AttributeValue> asItem(HugeBean b) {
        ImmutableMapParameter.Builder<String, AttributeValue> builder = ImmutableMapParameter.builder();

        builder.put("hashKey", av(b.getHashKey()));
        builder.put("stringAttr", av(b.getStringAttr()));
        builder.put("binaryAttr", av(b.getBinaryAttr()));

        List<AttributeValue> listAttr = b.getListAttr().stream().map(this::av).collect(Collectors.toList());

        builder.put("listAttr", av(listAttr));

        Map<String, AttributeValue> mapAttr1 = b.getMapAttr1().entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey,
                    e -> av(e.getValue())));

        builder.put("mapAttr1", av(mapAttr1));


        Map<String, AttributeValue> mapAttr2 = b.getMapAttr2().entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey,
                    e -> av(e.getValue().stream().map(this::av).collect(Collectors.toList()))));

        builder.put("mapAttr2", av(mapAttr2));

        Map<String, AttributeValue> mapAttr3 = b.getMapAttr3().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
            e -> {
                List<Map<String, List<SdkBytes>>> value = e.getValue();
                AttributeValue valueAv = av(value.stream().map(m -> av(m.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                            ee -> av(ee.getValue().stream().map(this::av).collect(Collectors.toList()))))))
                        .collect(Collectors.toList()));
                return valueAv;
            }));

        builder.put("mapAttr3", av(mapAttr3));

        return builder.build();
    }

    protected AttributeValue av(String val) {
        return AttributeValue.builder().s(val).build();
    }

    protected AttributeValue av(List<AttributeValue> val) {
        return AttributeValue.builder().l(val).build();
    }

    protected AttributeValue av(Map<String, AttributeValue> val) {
        return AttributeValue.builder().m(val).build();
    }

    protected AttributeValue av(SdkBytes val) {
        return AttributeValue.builder().b(val).build();
    }
}
