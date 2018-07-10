package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;

@Generated("software.amazon.awssdk:codegen")
final class MapOfEnumToStringCopier {
    static Map<String, String> copy(Map<String, String> mapOfEnumToStringParam) {
        if (mapOfEnumToStringParam == null) {
            return null;
        }
        Map<String, String> mapOfEnumToStringParamCopy = mapOfEnumToStringParam.entrySet().stream()
                                                                               .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        return Collections.unmodifiableMap(mapOfEnumToStringParamCopy);
    }

    static Map<String, String> copyEnumToString(Map<EnumType, String> mapOfEnumToStringParam) {
        if (mapOfEnumToStringParam == null) {
            return null;
        }
        Map<String, String> mapOfEnumToStringParamCopy = mapOfEnumToStringParam.entrySet().stream()
                                                                               .collect(toMap(e -> e.getKey().toString(), Map.Entry::getValue));
        return Collections.unmodifiableMap(mapOfEnumToStringParamCopy);
    }
}
