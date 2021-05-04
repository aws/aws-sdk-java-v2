package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

@Generated("software.amazon.awssdk:codegen")
final class MapOfStringToEnumCopier {
    static Map<String, String> copy(Map<String, String> mapOfStringToEnumParam) {
        Map<String, String> map;
        if (mapOfStringToEnumParam == null || mapOfStringToEnumParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, String> modifiableMap = new LinkedHashMap<>();
            mapOfStringToEnumParam.forEach((key, value) -> {
                modifiableMap.put(key, value);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }

    static Map<String, String> copyEnumToString(Map<String, EnumType> mapOfStringToEnumParam) {
        Map<String, String> map;
        if (mapOfStringToEnumParam == null || mapOfStringToEnumParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, String> modifiableMap = new LinkedHashMap<>();
            mapOfStringToEnumParam.forEach((key, value) -> {
                String result = value.toString();
                modifiableMap.put(key, result);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }

    static Map<String, EnumType> copyStringToEnum(Map<String, String> mapOfStringToEnumParam) {
        Map<String, EnumType> map;
        if (mapOfStringToEnumParam == null || mapOfStringToEnumParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, EnumType> modifiableMap = new LinkedHashMap<>();
            mapOfStringToEnumParam.forEach((key, value) -> {
                EnumType result = EnumType.fromValue(value);
                modifiableMap.put(key, result);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }
}
