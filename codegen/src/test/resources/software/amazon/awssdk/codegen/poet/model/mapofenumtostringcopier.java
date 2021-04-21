package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

@Generated("software.amazon.awssdk:codegen")
final class MapOfEnumToStringCopier {
    static Map<String, String> copy(Map<String, String> mapOfEnumToStringParam) {
        Map<String, String> map;
        if (mapOfEnumToStringParam == null || mapOfEnumToStringParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, String> modifiableMap = new LinkedHashMap<>();
            mapOfEnumToStringParam.forEach((key, value) -> {
                modifiableMap.put(key, value);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }

    static Map<String, String> copyEnumToString(Map<EnumType, String> mapOfEnumToStringParam) {
        Map<String, String> map;
        if (mapOfEnumToStringParam == null || mapOfEnumToStringParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, String> modifiableMap = new LinkedHashMap<>();
            mapOfEnumToStringParam.forEach((key, value) -> {
                String result = key.toString();
                modifiableMap.put(result, value);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }

    static Map<EnumType, String> copyStringToEnum(Map<String, String> mapOfEnumToStringParam) {
        Map<EnumType, String> map;
        if (mapOfEnumToStringParam == null || mapOfEnumToStringParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<EnumType, String> modifiableMap = new LinkedHashMap<>();
            mapOfEnumToStringParam.forEach((key, value) -> {
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
