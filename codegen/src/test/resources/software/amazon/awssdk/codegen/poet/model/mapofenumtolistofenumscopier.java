package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

@Generated("software.amazon.awssdk:codegen")
final class MapOfEnumToListOfEnumsCopier {
    static Map<String, List<String>> copy(Map<String, ? extends Collection<String>> mapOfEnumToListOfEnumsParam) {
        if (mapOfEnumToListOfEnumsParam == null || mapOfEnumToListOfEnumsParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<String, List<String>> mapOfEnumToListOfEnumsParamCopy = mapOfEnumToListOfEnumsParam.entrySet().stream()
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), ListOfEnumsCopier.copy(e.getValue())), HashMap::putAll);
        return Collections.unmodifiableMap(mapOfEnumToListOfEnumsParamCopy);
    }

    static Map<String, List<String>> copyEnumToString(Map<EnumType, ? extends Collection<EnumType>> mapOfEnumToListOfEnumsParam) {
        if (mapOfEnumToListOfEnumsParam == null || mapOfEnumToListOfEnumsParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<String, List<String>> mapOfEnumToListOfEnumsParamCopy = mapOfEnumToListOfEnumsParam
                .entrySet()
                .stream()
                .collect(HashMap::new, (m, e) -> m.put(e.getKey().toString(), ListOfEnumsCopier.copyEnumToString(e.getValue())),
                        HashMap::putAll);
        return Collections.unmodifiableMap(mapOfEnumToListOfEnumsParamCopy);
    }

    static Map<EnumType, List<EnumType>> copyStringToEnum(Map<String, ? extends Collection<String>> mapOfEnumToListOfEnumsParam) {
        if (mapOfEnumToListOfEnumsParam == null || mapOfEnumToListOfEnumsParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<EnumType, List<EnumType>> mapOfEnumToListOfEnumsParamCopy = mapOfEnumToListOfEnumsParam.entrySet().stream()
                .collect(HashMap::new, (m, e) -> {
                    EnumType keyAsEnum = EnumType.fromValue(e.getKey());
                    if (keyAsEnum != EnumType.UNKNOWN_TO_SDK_VERSION) {
                        m.put(keyAsEnum, ListOfEnumsCopier.copyStringToEnum(e.getValue()));
                    }
                }, HashMap::putAll);
        return Collections.unmodifiableMap(mapOfEnumToListOfEnumsParamCopy);
    }
}

