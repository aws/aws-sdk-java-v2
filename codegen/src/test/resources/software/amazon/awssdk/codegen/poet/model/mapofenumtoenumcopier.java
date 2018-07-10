package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
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
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        return Collections.unmodifiableMap(mapOfEnumToEnumParamCopy);
    }

    static Map<String, String> copyEnumToString(Map<EnumType, EnumType> mapOfEnumToEnumParam) {
        if (mapOfEnumToEnumParam == null || mapOfEnumToEnumParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<String, String> mapOfEnumToEnumParamCopy = mapOfEnumToEnumParam.entrySet().stream()
                .collect(toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
        return Collections.unmodifiableMap(mapOfEnumToEnumParamCopy);
    }
}
