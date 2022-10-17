package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

@Generated("software.amazon.awssdk:codegen")
final class MapOfEnumToMapOfStringToEnumCopier {
    static Map<String, Map<String, String>> copy(Map<String, ? extends Map<String, String>> mapOfEnumToMapOfStringToEnumParam) {
        Map<String, Map<String, String>> map;
        if (mapOfEnumToMapOfStringToEnumParam == null || mapOfEnumToMapOfStringToEnumParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, Map<String, String>> modifiableMap = new LinkedHashMap<>();
            mapOfEnumToMapOfStringToEnumParam.forEach((key, value) -> {
                Map<String, String> map1;
                if (value == null || value instanceof SdkAutoConstructMap) {
                    map1 = DefaultSdkAutoConstructMap.getInstance();
                } else {
                    Map<String, String> modifiableMap1 = new LinkedHashMap<>();
                    value.forEach((key1, value1) -> {
                        modifiableMap1.put(key1, value1);
                    });
                    map1 = Collections.unmodifiableMap(modifiableMap1);
                }
                modifiableMap.put(key, map1);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }

    static Map<String, Map<String, String>> copyEnumToString(
        Map<EnumType, ? extends Map<String, EnumType>> mapOfEnumToMapOfStringToEnumParam) {
        Map<String, Map<String, String>> map;
        if (mapOfEnumToMapOfStringToEnumParam == null || mapOfEnumToMapOfStringToEnumParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, Map<String, String>> modifiableMap = new LinkedHashMap<>();
            mapOfEnumToMapOfStringToEnumParam.forEach((key, value) -> {
                String result = key.toString();
                Map<String, String> map1;
                if (value == null || value instanceof SdkAutoConstructMap) {
                    map1 = DefaultSdkAutoConstructMap.getInstance();
                } else {
                    Map<String, String> modifiableMap1 = new LinkedHashMap<>();
                    value.forEach((key1, value1) -> {
                        String result1 = value1.toString();
                        modifiableMap1.put(key1, result1);
                    });
                    map1 = Collections.unmodifiableMap(modifiableMap1);
                }
                modifiableMap.put(result, map1);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }

    static Map<EnumType, Map<String, EnumType>> copyStringToEnum(
        Map<String, ? extends Map<String, String>> mapOfEnumToMapOfStringToEnumParam) {
        Map<EnumType, Map<String, EnumType>> map;
        if (mapOfEnumToMapOfStringToEnumParam == null || mapOfEnumToMapOfStringToEnumParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<EnumType, Map<String, EnumType>> modifiableMap = new LinkedHashMap<>();
            mapOfEnumToMapOfStringToEnumParam.forEach((key, value) -> {
                EnumType result = EnumType.fromValue(key);
                Map<String, EnumType> map1;
                if (value == null || value instanceof SdkAutoConstructMap) {
                    map1 = DefaultSdkAutoConstructMap.getInstance();
                } else {
                    Map<String, EnumType> modifiableMap1 = new LinkedHashMap<>();
                    value.forEach((key1, value1) -> {
                        EnumType result1 = EnumType.fromValue(value1);
                        modifiableMap1.put(key1, result1);
                    });
                    map1 = Collections.unmodifiableMap(modifiableMap1);
                }
                if (result != EnumType.UNKNOWN_TO_SDK_VERSION) {
                    modifiableMap.put(result, map1);
                }
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }
}
