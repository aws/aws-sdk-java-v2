package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

@Generated("software.amazon.awssdk:codegen")
final class MapOfStringToStringCopier {
    static Map<String, String> copy(Map<String, String> mapOfStringToStringParam) {
        if (mapOfStringToStringParam == null || mapOfStringToStringParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<String, String> mapOfStringToStringParamCopy = mapOfStringToStringParam.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        return Collections.unmodifiableMap(mapOfStringToStringParamCopy);
    }
}
