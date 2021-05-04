package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

@Generated("software.amazon.awssdk:codegen")
final class MapOfEnumToSimpleStructCopier {
    static Map<String, SimpleStruct> copy(Map<String, ? extends SimpleStruct> mapOfEnumToSimpleStructParam) {
        Map<String, SimpleStruct> map;
        if (mapOfEnumToSimpleStructParam == null || mapOfEnumToSimpleStructParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, SimpleStruct> modifiableMap = new LinkedHashMap<>();
            mapOfEnumToSimpleStructParam.forEach((key, value) -> {
                modifiableMap.put(key, value);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }

    static Map<String, SimpleStruct> copyFromBuilder(Map<String, ? extends SimpleStruct.Builder> mapOfEnumToSimpleStructParam) {
        Map<String, SimpleStruct> map;
        if (mapOfEnumToSimpleStructParam == null || mapOfEnumToSimpleStructParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, SimpleStruct> modifiableMap = new LinkedHashMap<>();
            mapOfEnumToSimpleStructParam.forEach((key, value) -> {
                SimpleStruct member = value.build();
                modifiableMap.put(key, member);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }

    static Map<String, SimpleStruct.Builder> copyToBuilder(Map<String, ? extends SimpleStruct> mapOfEnumToSimpleStructParam) {
        Map<String, SimpleStruct.Builder> map;
        if (mapOfEnumToSimpleStructParam == null || mapOfEnumToSimpleStructParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, SimpleStruct.Builder> modifiableMap = new LinkedHashMap<>();
            mapOfEnumToSimpleStructParam.forEach((key, value) -> {
                SimpleStruct.Builder member = value.toBuilder();
                modifiableMap.put(key, member);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }

    static Map<String, SimpleStruct> copyEnumToString(Map<EnumType, ? extends SimpleStruct> mapOfEnumToSimpleStructParam) {
        Map<String, SimpleStruct> map;
        if (mapOfEnumToSimpleStructParam == null || mapOfEnumToSimpleStructParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, SimpleStruct> modifiableMap = new LinkedHashMap<>();
            mapOfEnumToSimpleStructParam.forEach((key, value) -> {
                String result = key.toString();
                modifiableMap.put(result, value);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }

    static Map<EnumType, SimpleStruct> copyStringToEnum(Map<String, ? extends SimpleStruct> mapOfEnumToSimpleStructParam) {
        Map<EnumType, SimpleStruct> map;
        if (mapOfEnumToSimpleStructParam == null || mapOfEnumToSimpleStructParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<EnumType, SimpleStruct> modifiableMap = new LinkedHashMap<>();
            mapOfEnumToSimpleStructParam.forEach((key, value) -> {
                EnumType result = EnumType.fromValue(key);
                if (result != EnumType.UNKNOWN_TO_SDK_VERSION) {
                    modifiableMap.put(result, value);
                }
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }
}
