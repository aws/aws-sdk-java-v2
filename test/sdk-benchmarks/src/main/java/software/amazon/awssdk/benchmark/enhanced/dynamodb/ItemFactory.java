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

import com.amazonaws.util.ImmutableMapParameter;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import software.amazon.awssdk.core.SdkBytes;

abstract class ItemFactory<T> {
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";

    private static final Random RNG = new Random();

    public final Map<String, T> tiny() {
        return asItem(tinyBean());
    }

    public final Map<String, T> small() {
        return asItem(smallBean());
    }

    public final Map<String, T> huge() {
        return asItem(hugeBean());
    }

    public final Map<String, T> hugeFlat() {
        return asItem(hugeBeanFlat());
    }

    public final TinyBean tinyBean() {
        TinyBean b = new TinyBean();
        b.setStringAttr(randomS());
        return b;
    }

    public final SmallBean smallBean() {
        SmallBean b = new SmallBean();
        b.setStringAttr(randomS());
        b.setBinaryAttr(randomBytes());
        b.setListAttr(Arrays.asList(randomS(), randomS(), randomS()));
        return b;
    }

    public final HugeBean hugeBean() {
        HugeBean b = new HugeBean();
        b.setHashKey(randomS());
        b.setStringAttr(randomS());
        b.setBinaryAttr(randomBytes());
        b.setListAttr(IntStream.range(0, 32).mapToObj(i -> randomS()).collect(Collectors.toList()));

        Map<String, SdkBytes> mapAttr1 = new HashMap<>();
        mapAttr1.put("key1", randomBytes());
        mapAttr1.put("key2", randomBytes());
        mapAttr1.put("key3", randomBytes());

        b.setMapAttr1(mapAttr1);

        Map<String, List<SdkBytes>> mapAttr2 = new HashMap<>();
        mapAttr2.put("key1", Arrays.asList(randomBytes()));
        mapAttr2.put("key2", IntStream.range(0, 2).mapToObj(i -> randomBytes()).collect(Collectors.toList()));
        mapAttr2.put("key3", IntStream.range(0, 4).mapToObj(i -> randomBytes()).collect(Collectors.toList()));
        mapAttr2.put("key4", IntStream.range(0, 8).mapToObj(i -> randomBytes()).collect(Collectors.toList()));
        mapAttr2.put("key5", IntStream.range(0, 16).mapToObj(i -> randomBytes()).collect(Collectors.toList()));

        b.setMapAttr2(mapAttr2);

        ImmutableMapParameter.Builder<String, List<Map<String, List<SdkBytes>>>> mapAttr3Builder =
                ImmutableMapParameter.builder();

        List<Map<String, List<SdkBytes>>> value = Arrays.asList(
                ImmutableMapParameter.<String, List<SdkBytes>>builder()
                        .put("key1", IntStream.range(0, 2).mapToObj(i -> randomBytes()).collect(Collectors.toList()))
                        .build(),
                ImmutableMapParameter.<String, List<SdkBytes>>builder()
                        .put("key2", IntStream.range(0, 4).mapToObj(i -> randomBytes()).collect(Collectors.toList()))
                        .build(),
                ImmutableMapParameter.<String, List<SdkBytes>>builder()
                        .put("key3", IntStream.range(0, 8).mapToObj(i -> randomBytes()).collect(Collectors.toList()))
                        .build()
        );

        mapAttr3Builder.put("key1", value)
                       .put("key2", value)
                       .build();

        b.setMapAttr3(mapAttr3Builder.build());

        return b;
    }

    public HugeBeanFlat hugeBeanFlat() {
        HugeBeanFlat b = new HugeBeanFlat();
        Class<HugeBeanFlat> clazz = HugeBeanFlat.class;
        for (int i = 1; i <= 63; ++i) {
            try {
                Method setter = clazz.getMethod("setStringAttr" + i, String.class);
                setter.setAccessible(true);
                setter.invoke(b, randomS());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        return b;
    }

    protected abstract Map<String, T> asItem(TinyBean b);

    protected abstract Map<String, T> asItem(SmallBean b);

    protected abstract Map<String, T> asItem(HugeBean b);

    protected final Map<String, T> asItem(HugeBeanFlat b) {
        Map<String, T> item = new HashMap<>();
        Class<HugeBeanFlat> clazz = HugeBeanFlat.class;
        for (int i = 1; i <= 63; ++i) {
            try {
                Method getter = clazz.getMethod("getStringAttr" + i);
                getter.setAccessible(true);
                item.put("stringAttr" + i, av((String) getter.invoke(b)));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        return item;
    }

    protected abstract T av(String val);

    protected abstract T av(List<T> val);

    protected abstract T av(Map<String, T> val);

    protected abstract T av(SdkBytes val);

    private static String randomS(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; ++i) {
            sb.append(ALPHA.charAt(RNG.nextInt(ALPHA.length())));
        }
        return sb.toString();
    }

    private static String randomS() {
        return randomS(16);
    }

    private static ByteBuffer randomB(int len) {
        byte[] b = new byte[len];
        RNG.nextBytes(b);
        return ByteBuffer.wrap(b);
    }

    private static ByteBuffer randomB() {
        return randomB(16);
    }

    private static SdkBytes randomBytes() {
        return SdkBytes.fromByteBuffer(randomB());
    }

    public static class TinyBean {
        private String stringAttr;

        public String getStringAttr() {
            return stringAttr;
        }

        public void setStringAttr(String stringAttr) {
            this.stringAttr = stringAttr;
        }
    }

    public static class SmallBean {
        private String stringAttr;
        private SdkBytes binaryAttr;
        private List<String> listAttr;

        public String getStringAttr() {
            return stringAttr;
        }

        public void setStringAttr(String stringAttr) {
            this.stringAttr = stringAttr;
        }

        public SdkBytes getBinaryAttr() {
            return binaryAttr;
        }

        public void setBinaryAttr(SdkBytes binaryAttr) {
            this.binaryAttr = binaryAttr;
        }

        public List<String> getListAttr() {
            return listAttr;
        }

        public void setListAttr(List<String> listAttr) {
            this.listAttr = listAttr;
        }
    }

    public static class HugeBean {
        private String hashKey;
        private String stringAttr;
        private SdkBytes binaryAttr;
        private List<String> listAttr;

        private Map<String, SdkBytes> mapAttr1;
        private Map<String, List<SdkBytes>> mapAttr2;
        private Map<String, List<Map<String, List<SdkBytes>>>> mapAttr3;

        public String getHashKey() {
            return hashKey;
        }

        public void setHashKey(String hashKey) {
            this.hashKey = hashKey;
        }

        public String getStringAttr() {
            return stringAttr;
        }

        public void setStringAttr(String stringAttr) {
            this.stringAttr = stringAttr;
        }

        public SdkBytes getBinaryAttr() {
            return binaryAttr;
        }

        public void setBinaryAttr(SdkBytes binaryAttr) {
            this.binaryAttr = binaryAttr;
        }

        public List<String> getListAttr() {
            return listAttr;
        }

        public void setListAttr(List<String> listAttr) {
            this.listAttr = listAttr;
        }

        public Map<String, SdkBytes> getMapAttr1() {
            return mapAttr1;
        }

        public void setMapAttr1(Map<String, SdkBytes> mapAttr1) {
            this.mapAttr1 = mapAttr1;
        }

        public Map<String, List<SdkBytes>> getMapAttr2() {
            return mapAttr2;
        }

        public void setMapAttr2(Map<String, List<SdkBytes>> mapAttr2) {
            this.mapAttr2 = mapAttr2;
        }

        public Map<String, List<Map<String, List<SdkBytes>>>> getMapAttr3() {
            return mapAttr3;
        }

        public void setMapAttr3(Map<String, List<Map<String, List<SdkBytes>>>> mapAttr3) {
            this.mapAttr3 = mapAttr3;
        }
    }

    public static class HugeBeanFlat {
        private String stringAttr1;
        private String stringAttr2;
        private String stringAttr3;
        private String stringAttr4;
        private String stringAttr5;
        private String stringAttr6;
        private String stringAttr7;
        private String stringAttr8;
        private String stringAttr9;
        private String stringAttr10;
        private String stringAttr11;
        private String stringAttr12;
        private String stringAttr13;
        private String stringAttr14;
        private String stringAttr15;
        private String stringAttr16;
        private String stringAttr17;
        private String stringAttr18;
        private String stringAttr19;
        private String stringAttr20;
        private String stringAttr21;
        private String stringAttr22;
        private String stringAttr23;
        private String stringAttr24;
        private String stringAttr25;
        private String stringAttr26;
        private String stringAttr27;
        private String stringAttr28;
        private String stringAttr29;
        private String stringAttr30;
        private String stringAttr31;
        private String stringAttr32;
        private String stringAttr33;
        private String stringAttr34;
        private String stringAttr35;
        private String stringAttr36;
        private String stringAttr37;
        private String stringAttr38;
        private String stringAttr39;
        private String stringAttr40;
        private String stringAttr41;
        private String stringAttr42;
        private String stringAttr43;
        private String stringAttr44;
        private String stringAttr45;
        private String stringAttr46;
        private String stringAttr47;
        private String stringAttr48;
        private String stringAttr49;
        private String stringAttr50;
        private String stringAttr51;
        private String stringAttr52;
        private String stringAttr53;
        private String stringAttr54;
        private String stringAttr55;
        private String stringAttr56;
        private String stringAttr57;
        private String stringAttr58;
        private String stringAttr59;
        private String stringAttr60;
        private String stringAttr61;
        private String stringAttr62;
        private String stringAttr63;

        public String getStringAttr1() {
            return stringAttr1;
        }

        public void setStringAttr1(String stringAttr1) {
            this.stringAttr1 = stringAttr1;
        }

        public String getStringAttr2() {
            return stringAttr2;
        }

        public void setStringAttr2(String stringAttr2) {
            this.stringAttr2 = stringAttr2;
        }

        public String getStringAttr3() {
            return stringAttr3;
        }

        public void setStringAttr3(String stringAttr3) {
            this.stringAttr3 = stringAttr3;
        }

        public String getStringAttr4() {
            return stringAttr4;
        }

        public void setStringAttr4(String stringAttr4) {
            this.stringAttr4 = stringAttr4;
        }

        public String getStringAttr5() {
            return stringAttr5;
        }

        public void setStringAttr5(String stringAttr5) {
            this.stringAttr5 = stringAttr5;
        }

        public String getStringAttr6() {
            return stringAttr6;
        }

        public void setStringAttr6(String stringAttr6) {
            this.stringAttr6 = stringAttr6;
        }

        public String getStringAttr7() {
            return stringAttr7;
        }

        public void setStringAttr7(String stringAttr7) {
            this.stringAttr7 = stringAttr7;
        }

        public String getStringAttr8() {
            return stringAttr8;
        }

        public void setStringAttr8(String stringAttr8) {
            this.stringAttr8 = stringAttr8;
        }

        public String getStringAttr9() {
            return stringAttr9;
        }

        public void setStringAttr9(String stringAttr9) {
            this.stringAttr9 = stringAttr9;
        }

        public String getStringAttr10() {
            return stringAttr10;
        }

        public void setStringAttr10(String stringAttr10) {
            this.stringAttr10 = stringAttr10;
        }

        public String getStringAttr11() {
            return stringAttr11;
        }

        public void setStringAttr11(String stringAttr11) {
            this.stringAttr11 = stringAttr11;
        }

        public String getStringAttr12() {
            return stringAttr12;
        }

        public void setStringAttr12(String stringAttr12) {
            this.stringAttr12 = stringAttr12;
        }

        public String getStringAttr13() {
            return stringAttr13;
        }

        public void setStringAttr13(String stringAttr13) {
            this.stringAttr13 = stringAttr13;
        }

        public String getStringAttr14() {
            return stringAttr14;
        }

        public void setStringAttr14(String stringAttr14) {
            this.stringAttr14 = stringAttr14;
        }

        public String getStringAttr15() {
            return stringAttr15;
        }

        public void setStringAttr15(String stringAttr15) {
            this.stringAttr15 = stringAttr15;
        }

        public String getStringAttr16() {
            return stringAttr16;
        }

        public void setStringAttr16(String stringAttr16) {
            this.stringAttr16 = stringAttr16;
        }

        public String getStringAttr17() {
            return stringAttr17;
        }

        public void setStringAttr17(String stringAttr17) {
            this.stringAttr17 = stringAttr17;
        }

        public String getStringAttr18() {
            return stringAttr18;
        }

        public void setStringAttr18(String stringAttr18) {
            this.stringAttr18 = stringAttr18;
        }

        public String getStringAttr19() {
            return stringAttr19;
        }

        public void setStringAttr19(String stringAttr19) {
            this.stringAttr19 = stringAttr19;
        }

        public String getStringAttr20() {
            return stringAttr20;
        }

        public void setStringAttr20(String stringAttr20) {
            this.stringAttr20 = stringAttr20;
        }

        public String getStringAttr21() {
            return stringAttr21;
        }

        public void setStringAttr21(String stringAttr21) {
            this.stringAttr21 = stringAttr21;
        }

        public String getStringAttr22() {
            return stringAttr22;
        }

        public void setStringAttr22(String stringAttr22) {
            this.stringAttr22 = stringAttr22;
        }

        public String getStringAttr23() {
            return stringAttr23;
        }

        public void setStringAttr23(String stringAttr23) {
            this.stringAttr23 = stringAttr23;
        }

        public String getStringAttr24() {
            return stringAttr24;
        }

        public void setStringAttr24(String stringAttr24) {
            this.stringAttr24 = stringAttr24;
        }

        public String getStringAttr25() {
            return stringAttr25;
        }

        public void setStringAttr25(String stringAttr25) {
            this.stringAttr25 = stringAttr25;
        }

        public String getStringAttr26() {
            return stringAttr26;
        }

        public void setStringAttr26(String stringAttr26) {
            this.stringAttr26 = stringAttr26;
        }

        public String getStringAttr27() {
            return stringAttr27;
        }

        public void setStringAttr27(String stringAttr27) {
            this.stringAttr27 = stringAttr27;
        }

        public String getStringAttr28() {
            return stringAttr28;
        }

        public void setStringAttr28(String stringAttr28) {
            this.stringAttr28 = stringAttr28;
        }

        public String getStringAttr29() {
            return stringAttr29;
        }

        public void setStringAttr29(String stringAttr29) {
            this.stringAttr29 = stringAttr29;
        }

        public String getStringAttr30() {
            return stringAttr30;
        }

        public void setStringAttr30(String stringAttr30) {
            this.stringAttr30 = stringAttr30;
        }

        public String getStringAttr31() {
            return stringAttr31;
        }

        public void setStringAttr31(String stringAttr31) {
            this.stringAttr31 = stringAttr31;
        }

        public String getStringAttr32() {
            return stringAttr32;
        }

        public void setStringAttr32(String stringAttr32) {
            this.stringAttr32 = stringAttr32;
        }

        public String getStringAttr33() {
            return stringAttr33;
        }

        public void setStringAttr33(String stringAttr33) {
            this.stringAttr33 = stringAttr33;
        }

        public String getStringAttr34() {
            return stringAttr34;
        }

        public void setStringAttr34(String stringAttr34) {
            this.stringAttr34 = stringAttr34;
        }

        public String getStringAttr35() {
            return stringAttr35;
        }

        public void setStringAttr35(String stringAttr35) {
            this.stringAttr35 = stringAttr35;
        }

        public String getStringAttr36() {
            return stringAttr36;
        }

        public void setStringAttr36(String stringAttr36) {
            this.stringAttr36 = stringAttr36;
        }

        public String getStringAttr37() {
            return stringAttr37;
        }

        public void setStringAttr37(String stringAttr37) {
            this.stringAttr37 = stringAttr37;
        }

        public String getStringAttr38() {
            return stringAttr38;
        }

        public void setStringAttr38(String stringAttr38) {
            this.stringAttr38 = stringAttr38;
        }

        public String getStringAttr39() {
            return stringAttr39;
        }

        public void setStringAttr39(String stringAttr39) {
            this.stringAttr39 = stringAttr39;
        }

        public String getStringAttr40() {
            return stringAttr40;
        }

        public void setStringAttr40(String stringAttr40) {
            this.stringAttr40 = stringAttr40;
        }

        public String getStringAttr41() {
            return stringAttr41;
        }

        public void setStringAttr41(String stringAttr41) {
            this.stringAttr41 = stringAttr41;
        }

        public String getStringAttr42() {
            return stringAttr42;
        }

        public void setStringAttr42(String stringAttr42) {
            this.stringAttr42 = stringAttr42;
        }

        public String getStringAttr43() {
            return stringAttr43;
        }

        public void setStringAttr43(String stringAttr43) {
            this.stringAttr43 = stringAttr43;
        }

        public String getStringAttr44() {
            return stringAttr44;
        }

        public void setStringAttr44(String stringAttr44) {
            this.stringAttr44 = stringAttr44;
        }

        public String getStringAttr45() {
            return stringAttr45;
        }

        public void setStringAttr45(String stringAttr45) {
            this.stringAttr45 = stringAttr45;
        }

        public String getStringAttr46() {
            return stringAttr46;
        }

        public void setStringAttr46(String stringAttr46) {
            this.stringAttr46 = stringAttr46;
        }

        public String getStringAttr47() {
            return stringAttr47;
        }

        public void setStringAttr47(String stringAttr47) {
            this.stringAttr47 = stringAttr47;
        }

        public String getStringAttr48() {
            return stringAttr48;
        }

        public void setStringAttr48(String stringAttr48) {
            this.stringAttr48 = stringAttr48;
        }

        public String getStringAttr49() {
            return stringAttr49;
        }

        public void setStringAttr49(String stringAttr49) {
            this.stringAttr49 = stringAttr49;
        }

        public String getStringAttr50() {
            return stringAttr50;
        }

        public void setStringAttr50(String stringAttr50) {
            this.stringAttr50 = stringAttr50;
        }

        public String getStringAttr51() {
            return stringAttr51;
        }

        public void setStringAttr51(String stringAttr51) {
            this.stringAttr51 = stringAttr51;
        }

        public String getStringAttr52() {
            return stringAttr52;
        }

        public void setStringAttr52(String stringAttr52) {
            this.stringAttr52 = stringAttr52;
        }

        public String getStringAttr53() {
            return stringAttr53;
        }

        public void setStringAttr53(String stringAttr53) {
            this.stringAttr53 = stringAttr53;
        }

        public String getStringAttr54() {
            return stringAttr54;
        }

        public void setStringAttr54(String stringAttr54) {
            this.stringAttr54 = stringAttr54;
        }

        public String getStringAttr55() {
            return stringAttr55;
        }

        public void setStringAttr55(String stringAttr55) {
            this.stringAttr55 = stringAttr55;
        }

        public String getStringAttr56() {
            return stringAttr56;
        }

        public void setStringAttr56(String stringAttr56) {
            this.stringAttr56 = stringAttr56;
        }

        public String getStringAttr57() {
            return stringAttr57;
        }

        public void setStringAttr57(String stringAttr57) {
            this.stringAttr57 = stringAttr57;
        }

        public String getStringAttr58() {
            return stringAttr58;
        }

        public void setStringAttr58(String stringAttr58) {
            this.stringAttr58 = stringAttr58;
        }

        public String getStringAttr59() {
            return stringAttr59;
        }

        public void setStringAttr59(String stringAttr59) {
            this.stringAttr59 = stringAttr59;
        }

        public String getStringAttr60() {
            return stringAttr60;
        }

        public void setStringAttr60(String stringAttr60) {
            this.stringAttr60 = stringAttr60;
        }

        public String getStringAttr61() {
            return stringAttr61;
        }

        public void setStringAttr61(String stringAttr61) {
            this.stringAttr61 = stringAttr61;
        }

        public String getStringAttr62() {
            return stringAttr62;
        }

        public void setStringAttr62(String stringAttr62) {
            this.stringAttr62 = stringAttr62;
        }

        public String getStringAttr63() {
            return stringAttr63;
        }

        public void setStringAttr63(String stringAttr63) {
            this.stringAttr63 = stringAttr63;
        }
    }
}
