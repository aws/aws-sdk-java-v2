package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

@Generated("software.amazon.awssdk:codegen")
final class MapOfEnumToStringCopier {
    static Map<String, String> copy(Map<String, String> mapOfEnumToStringParam) {
        if (mapOfEnumToStringParam == null || mapOfEnumToStringParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<String, String> mapOfEnumToStringParamCopy = mapOfEnumToStringParam.entrySet().stream()
            .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
        return Collections.unmodifiableMap(mapOfEnumToStringParamCopy);
    }

    static Map<String, String> copyEnumToString(Map<EnumType, String> mapOfEnumToStringParam) {
        if (mapOfEnumToStringParam == null || mapOfEnumToStringParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<String, String> mapOfEnumToStringParamCopy = mapOfEnumToStringParam.entrySet().stream()
            .collect(HashMap::new, (m, e) -> m.put(e.getKey().toString(), e.getValue()), HashMap::putAll);
        return Collections.unmodifiableMap(mapOfEnumToStringParamCopy);
    }
}
