package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

@Generated("software.amazon.awssdk:codegen")
final class MapOfStringToSimpleStructCopier {
    static Map<String, SimpleStruct> copy(Map<String, ? extends SimpleStruct> mapOfStringToSimpleStructParam) {
        Map<String, SimpleStruct> map;
        if (mapOfStringToSimpleStructParam == null || mapOfStringToSimpleStructParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, SimpleStruct> modifiableMap = new LinkedHashMap<>();
            mapOfStringToSimpleStructParam.forEach((key, value) -> {
                modifiableMap.put(key, value);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }

    static Map<String, SimpleStruct> copyFromBuilder(Map<String, ? extends SimpleStruct.Builder> mapOfStringToSimpleStructParam) {
        Map<String, SimpleStruct> map;
        if (mapOfStringToSimpleStructParam == null || mapOfStringToSimpleStructParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, SimpleStruct> modifiableMap = new LinkedHashMap<>();
            mapOfStringToSimpleStructParam.forEach((key, value) -> {
                SimpleStruct member = value.build();
                modifiableMap.put(key, member);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }

    static Map<String, SimpleStruct.Builder> copyToBuilder(Map<String, ? extends SimpleStruct> mapOfStringToSimpleStructParam) {
        Map<String, SimpleStruct.Builder> map;
        if (mapOfStringToSimpleStructParam == null || mapOfStringToSimpleStructParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, SimpleStruct.Builder> modifiableMap = new LinkedHashMap<>();
            mapOfStringToSimpleStructParam.forEach((key, value) -> {
                SimpleStruct.Builder member = value.toBuilder();
                modifiableMap.put(key, member);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }
}
