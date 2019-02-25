package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

@Generated("software.amazon.awssdk:codegen")
final class MapOfEnumToMapOfStringToEnumCopier {
    static Map<String, Map<String, String>> copy(Map<String, Map<String, String>> mapOfEnumToMapOfStringToEnumParam) {
        if (mapOfEnumToMapOfStringToEnumParam == null || mapOfEnumToMapOfStringToEnumParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<String, Map<String, String>> mapOfEnumToMapOfStringToEnumParamCopy = mapOfEnumToMapOfStringToEnumParam.entrySet()
                .stream()
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), MapOfStringToEnumCopier.copy(e.getValue())), HashMap::putAll);
        return Collections.unmodifiableMap(mapOfEnumToMapOfStringToEnumParamCopy);
    }

    static Map<String, Map<String, String>> copyEnumToString(
            Map<EnumType, Map<String, EnumType>> mapOfEnumToMapOfStringToEnumParam) {
        if (mapOfEnumToMapOfStringToEnumParam == null || mapOfEnumToMapOfStringToEnumParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<String, Map<String, String>> mapOfEnumToMapOfStringToEnumParamCopy = mapOfEnumToMapOfStringToEnumParam
                .entrySet()
                .stream()
                .collect(HashMap::new,
                        (m, e) -> m.put(e.getKey().toString(), MapOfStringToEnumCopier.copyEnumToString(e.getValue())),
                        HashMap::putAll);
        return Collections.unmodifiableMap(mapOfEnumToMapOfStringToEnumParamCopy);
    }

    static Map<EnumType, Map<String, EnumType>> copyStringToEnum(
            Map<String, Map<String, String>> mapOfEnumToMapOfStringToEnumParam) {
        if (mapOfEnumToMapOfStringToEnumParam == null || mapOfEnumToMapOfStringToEnumParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<EnumType, Map<String, EnumType>> mapOfEnumToMapOfStringToEnumParamCopy = mapOfEnumToMapOfStringToEnumParam.entrySet()
                .stream().collect(HashMap::new, (m, e) -> {
                    EnumType keyAsEnum = EnumType.fromValue(e.getKey());
                    if (keyAsEnum != EnumType.UNKNOWN_TO_SDK_VERSION) {
                        m.put(keyAsEnum, MapOfStringToEnumCopier.copyStringToEnum(e.getValue()));
                    }
                }, HashMap::putAll);
        return Collections.unmodifiableMap(mapOfEnumToMapOfStringToEnumParamCopy);
    }
}
