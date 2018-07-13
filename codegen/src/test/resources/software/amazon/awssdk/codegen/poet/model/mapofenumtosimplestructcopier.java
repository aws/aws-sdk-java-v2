package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

@Generated("software.amazon.awssdk:codegen")
final class MapOfEnumToSimpleStructCopier {
    static Map<String, SimpleStruct> copy(Map<String, SimpleStruct> mapOfEnumToSimpleStructParam) {
        if (mapOfEnumToSimpleStructParam == null || mapOfEnumToSimpleStructParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
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

    static Map<String, SimpleStruct> copyEnumToString(Map<EnumType, SimpleStruct> mapOfEnumToSimpleStructParam) {
        if (mapOfEnumToSimpleStructParam == null || mapOfEnumToSimpleStructParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<String, SimpleStruct> mapOfEnumToSimpleStructParamCopy = mapOfEnumToSimpleStructParam.entrySet().stream()
                .collect(toMap(e -> e.getKey().toString(), Map.Entry::getValue));
        return Collections.unmodifiableMap(mapOfEnumToSimpleStructParamCopy);
    }
}
