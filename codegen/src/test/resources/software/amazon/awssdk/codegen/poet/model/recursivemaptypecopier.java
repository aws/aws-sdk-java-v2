package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

@Generated("software.amazon.awssdk:codegen")
final class RecursiveMapTypeCopier {
    static Map<String, RecursiveStructType> copy(Map<String, ? extends RecursiveStructType> recursiveMapTypeParam) {
        Map<String, RecursiveStructType> map;
        if (recursiveMapTypeParam == null || recursiveMapTypeParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, RecursiveStructType> modifiableMap = new LinkedHashMap<>();
            recursiveMapTypeParam.forEach((key, value) -> {
                modifiableMap.put(key, value);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }

    static Map<String, RecursiveStructType> copyFromBuilder(
        Map<String, ? extends RecursiveStructType.Builder> recursiveMapTypeParam) {
        Map<String, RecursiveStructType> map;
        if (recursiveMapTypeParam == null || recursiveMapTypeParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, RecursiveStructType> modifiableMap = new LinkedHashMap<>();
            recursiveMapTypeParam.forEach((key, value) -> {
                RecursiveStructType member = value.build();
                modifiableMap.put(key, member);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }

    static Map<String, RecursiveStructType.Builder> copyToBuilder(Map<String, ? extends RecursiveStructType> recursiveMapTypeParam) {
        Map<String, RecursiveStructType.Builder> map;
        if (recursiveMapTypeParam == null || recursiveMapTypeParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, RecursiveStructType.Builder> modifiableMap = new LinkedHashMap<>();
            recursiveMapTypeParam.forEach((key, value) -> {
                RecursiveStructType.Builder member = value.toBuilder();
                modifiableMap.put(key, member);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }
}
