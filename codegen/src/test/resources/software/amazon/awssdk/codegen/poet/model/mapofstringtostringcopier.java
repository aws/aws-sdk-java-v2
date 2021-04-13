package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

@Generated("software.amazon.awssdk:codegen")
final class MapOfStringToStringCopier {
    static Map<String, String> copy(Map<String, String> mapOfStringToStringParam) {
        Map<String, String> map;
        if (mapOfStringToStringParam == null || mapOfStringToStringParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, String> modifiableMap = new LinkedHashMap<>();
            mapOfStringToStringParam.forEach((key, value) -> {
                modifiableMap.put(key, value);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }
}
