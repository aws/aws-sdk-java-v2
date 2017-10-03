package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class MapOfStringToIntegerListCopier {
    static Map<String, List<Integer>> copy(Map<String, ? extends Collection<Integer>> mapOfStringToIntegerListParam) {
        if (mapOfStringToIntegerListParam == null) {
            return null;
        }
        Map<String, List<Integer>> mapOfStringToIntegerListParamCopy = mapOfStringToIntegerListParam.entrySet().stream()
                                                                                                    .collect(toMap(Map.Entry::getKey, e -> ListOfIntegersCopier.copy(e.getValue())));
        return Collections.unmodifiableMap(mapOfStringToIntegerListParamCopy);
    }
}
