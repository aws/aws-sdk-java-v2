package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

@Generated("software.amazon.awssdk:codegen")
final class MapOfEnumToEnumCopier {
    static Map<String, String> copy(Map<String, String> mapOfEnumToEnumParam) {
        Map<String, String> map;
        if (mapOfEnumToEnumParam == null || mapOfEnumToEnumParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, String> modifiableMap = new LinkedHashMap<>();
            mapOfEnumToEnumParam.forEach((key, value) -> {
                modifiableMap.put(key, value);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }

    static Map<String, String> copyEnumToString(Map<EnumType, EnumType> mapOfEnumToEnumParam) {
        Map<String, String> map;
        if (mapOfEnumToEnumParam == null || mapOfEnumToEnumParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, String> modifiableMap = new LinkedHashMap<>();
            mapOfEnumToEnumParam.forEach((key, value) -> {
                String result = key.toString();
                String result1 = value.toString();
                modifiableMap.put(result, result1);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }

    static Map<EnumType, EnumType> copyStringToEnum(Map<String, String> mapOfEnumToEnumParam) {
        Map<EnumType, EnumType> map;
        if (mapOfEnumToEnumParam == null || mapOfEnumToEnumParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<EnumType, EnumType> modifiableMap = new LinkedHashMap<>();
            mapOfEnumToEnumParam.forEach((key, value) -> {
                EnumType result = EnumType.fromValue(key);
                EnumType result1 = EnumType.fromValue(value);
                if (result != EnumType.UNKNOWN_TO_SDK_VERSION) {
                    modifiableMap.put(result, result1);
                }
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }
}
