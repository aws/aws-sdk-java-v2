package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

@Generated("software.amazon.awssdk:codegen")
final class MapOfStringToEnumCopier {
    static Map<String, String> copy(Map<String, String> mapOfStringToEnumParam) {
        if (mapOfStringToEnumParam == null || mapOfStringToEnumParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<String, String> mapOfStringToEnumParamCopy = mapOfStringToEnumParam.entrySet().stream()
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
        return Collections.unmodifiableMap(mapOfStringToEnumParamCopy);
    }

    static Map<String, String> copyEnumToString(Map<String, EnumType> mapOfStringToEnumParam) {
        if (mapOfStringToEnumParam == null || mapOfStringToEnumParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<String, String> mapOfStringToEnumParamCopy = mapOfStringToEnumParam.entrySet().stream()
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().toString()), HashMap::putAll);
        return Collections.unmodifiableMap(mapOfStringToEnumParamCopy);
    }

    static Map<String, EnumType> copyStringToEnum(Map<String, String> mapOfStringToEnumParam) {
        if (mapOfStringToEnumParam == null || mapOfStringToEnumParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<String, EnumType> mapOfStringToEnumParamCopy = mapOfStringToEnumParam.entrySet().stream()
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), EnumType.fromValue(e.getValue())), HashMap::putAll);
        return Collections.unmodifiableMap(mapOfStringToEnumParamCopy);
    }
}
