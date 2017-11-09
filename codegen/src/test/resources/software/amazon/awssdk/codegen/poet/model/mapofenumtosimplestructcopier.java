package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.Map;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class MapOfEnumToSimpleStructCopier {
    static Map<String, SimpleStruct> copy(Map<String, SimpleStruct> mapOfEnumToSimpleStructParam) {
        if (mapOfEnumToSimpleStructParam == null) {
            return null;
        }
        Map<String, SimpleStruct> mapOfEnumToSimpleStructParamCopy = mapOfEnumToSimpleStructParam.entrySet().stream()
                                                                                                 .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        return Collections.unmodifiableMap(mapOfEnumToSimpleStructParamCopy);
    }

    static Map<String, SimpleStruct> copyFromBuilder(Map<String, ? extends SimpleStruct.Builder> mapOfEnumToSimpleStructParam) {
        if (mapOfEnumToSimpleStructParam == null) {
            return null;
        }
        return copy(mapOfEnumToSimpleStructParam.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().build())));
    }
}
