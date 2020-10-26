package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.HashMap;
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
                                                                                                 .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
        return Collections.unmodifiableMap(mapOfEnumToSimpleStructParamCopy);
    }

    static Map<String, SimpleStruct> copyFromBuilder(Map<String, ? extends SimpleStruct.Builder> mapOfEnumToSimpleStructParam) {
        if (mapOfEnumToSimpleStructParam == null || mapOfEnumToSimpleStructParam instanceof DefaultSdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        return copy(mapOfEnumToSimpleStructParam.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().build())));
    }

    static Map<String, SimpleStruct> copyEnumToString(Map<EnumType, SimpleStruct> mapOfEnumToSimpleStructParam) {
        if (mapOfEnumToSimpleStructParam == null || mapOfEnumToSimpleStructParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<String, SimpleStruct> mapOfEnumToSimpleStructParamCopy = mapOfEnumToSimpleStructParam.entrySet().stream()
                                                                                                 .collect(HashMap::new, (m, e) -> m.put(e.getKey().toString(), e.getValue()), HashMap::putAll);
        return Collections.unmodifiableMap(mapOfEnumToSimpleStructParamCopy);
    }

    static Map<EnumType, SimpleStruct> copyStringToEnum(Map<String, SimpleStruct> mapOfEnumToSimpleStructParam) {
        if (mapOfEnumToSimpleStructParam == null || mapOfEnumToSimpleStructParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<EnumType, SimpleStruct> mapOfEnumToSimpleStructParamCopy = mapOfEnumToSimpleStructParam.entrySet().stream()
                                                                                                   .collect(HashMap::new, (m, e) -> {
                                                                                                       EnumType keyAsEnum = EnumType.fromValue(e.getKey());
                                                                                                       if (keyAsEnum != EnumType.UNKNOWN_TO_SDK_VERSION) {
                                                                                                           m.put(keyAsEnum, e.getValue());
                                                                                                       }
                                                                                                   }, HashMap::putAll);
        return Collections.unmodifiableMap(mapOfEnumToSimpleStructParamCopy);
    }
}
