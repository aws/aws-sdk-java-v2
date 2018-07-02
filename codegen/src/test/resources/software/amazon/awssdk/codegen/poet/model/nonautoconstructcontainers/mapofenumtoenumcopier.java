package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;

@Generated("software.amazon.awssdk:codegen")
final class MapOfEnumToEnumCopier {
    static Map<String, String> copy(Map<String, String> mapOfEnumToEnumParam) {
        if (mapOfEnumToEnumParam == null) {
            return null;
        }
        Map<String, String> mapOfEnumToEnumParamCopy = mapOfEnumToEnumParam.entrySet().stream()
                                                                           .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        return Collections.unmodifiableMap(mapOfEnumToEnumParamCopy);
    }

    static Map<String, String> copyEnumToString(Map<EnumType, EnumType> mapOfEnumToEnumParam) {
        if (mapOfEnumToEnumParam == null) {
            return null;
        }
        Map<String, String> mapOfEnumToEnumParamCopy = mapOfEnumToEnumParam.entrySet().stream()
                                                                           .collect(toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
        return Collections.unmodifiableMap(mapOfEnumToEnumParamCopy);
    }
}
