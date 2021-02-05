package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

@Generated("software.amazon.awssdk:codegen")
final class RecursiveMapTypeCopier {
    static Map<String, RecursiveStructType> copy(Map<String, RecursiveStructType> recursiveMapTypeParam) {
        if (recursiveMapTypeParam == null || recursiveMapTypeParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<String, RecursiveStructType> recursiveMapTypeParamCopy = recursiveMapTypeParam.entrySet().stream()
                                                                                          .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
        return Collections.unmodifiableMap(recursiveMapTypeParamCopy);
    }

    static Map<String, RecursiveStructType> copyFromBuilder(
        Map<String, ? extends RecursiveStructType.Builder> recursiveMapTypeParam) {
        if (recursiveMapTypeParam == null || recursiveMapTypeParam instanceof DefaultSdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        return copy(recursiveMapTypeParam.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().build())));
    }
}
