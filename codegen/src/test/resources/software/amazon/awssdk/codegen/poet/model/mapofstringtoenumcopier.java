package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.Map;
import javax.annotation.Generated;

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
}
