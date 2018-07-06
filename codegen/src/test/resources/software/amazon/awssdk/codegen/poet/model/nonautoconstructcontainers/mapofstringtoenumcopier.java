package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;

@Generated("software.amazon.awssdk:codegen")
final class MapOfStringToEnumCopier {
    static Map<String, String> copy(Map<String, String> mapOfStringToEnumParam) {
        if (mapOfStringToEnumParam == null) {
            return null;
        }
        Map<String, String> mapOfStringToEnumParamCopy = mapOfStringToEnumParam.entrySet().stream()
                                                                               .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        return Collections.unmodifiableMap(mapOfStringToEnumParamCopy);
    }

    static Map<String, String> copyEnumToString(Map<String, EnumType> mapOfStringToEnumParam) {
        if (mapOfStringToEnumParam == null) {
            return null;
        }
        Map<String, String> mapOfStringToEnumParamCopy = mapOfStringToEnumParam.entrySet().stream()
                                                                               .collect(toMap(Map.Entry::getKey, e -> e.getValue().toString()));
        return Collections.unmodifiableMap(mapOfStringToEnumParamCopy);
    }
}
