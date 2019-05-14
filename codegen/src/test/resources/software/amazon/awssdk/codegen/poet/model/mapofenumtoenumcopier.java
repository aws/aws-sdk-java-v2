package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

@Generated("software.amazon.awssdk:codegen")
final class MapOfEnumToEnumCopier {
    static Map<String, String> copy(Map<String, String> mapOfEnumToEnumParam) {
        if (mapOfEnumToEnumParam == null || mapOfEnumToEnumParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<String, String> mapOfEnumToEnumParamCopy = mapOfEnumToEnumParam.entrySet().stream()
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
        return Collections.unmodifiableMap(mapOfEnumToEnumParamCopy);
    }

    static Map<String, String> copyEnumToString(Map<EnumType, EnumType> mapOfEnumToEnumParam) {
        if (mapOfEnumToEnumParam == null || mapOfEnumToEnumParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<String, String> mapOfEnumToEnumParamCopy = mapOfEnumToEnumParam.entrySet().stream()
                .collect(HashMap::new, (m, e) -> m.put(e.getKey().toString(), e.getValue().toString()), HashMap::putAll);
        return Collections.unmodifiableMap(mapOfEnumToEnumParamCopy);
    }

    static Map<EnumType, EnumType> copyStringToEnum(Map<String, String> mapOfEnumToEnumParam) {
        if (mapOfEnumToEnumParam == null || mapOfEnumToEnumParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<EnumType, EnumType> mapOfEnumToEnumParamCopy = mapOfEnumToEnumParam.entrySet().stream()
                .collect(HashMap::new, (m, e) -> {
                    EnumType keyAsEnum = EnumType.fromValue(e.getKey());
                    if (keyAsEnum != EnumType.UNKNOWN_TO_SDK_VERSION) {
                        m.put(keyAsEnum, EnumType.fromValue(e.getValue()));
                    }
                }, HashMap::putAll);
        return Collections.unmodifiableMap(mapOfEnumToEnumParamCopy);
    }
}
