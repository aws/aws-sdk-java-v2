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
final class MapOfStringToIntegerListCopier {
    static Map<String, List<Integer>> copy(Map<String, ? extends Collection<Integer>> mapOfStringToIntegerListParam) {
        if (mapOfStringToIntegerListParam == null || mapOfStringToIntegerListParam instanceof SdkAutoConstructMap) {
            return DefaultSdkAutoConstructMap.getInstance();
        }
        Map<String, List<Integer>> mapOfStringToIntegerListParamCopy = mapOfStringToIntegerListParam.entrySet().stream()
            .collect(HashMap::new, (m, e) -> m.put(e.getKey(), ListOfIntegersCopier.copy(e.getValue())), HashMap::putAll);
        return Collections.unmodifiableMap(mapOfStringToIntegerListParamCopy);
    }
}
