package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import software.amazon.awssdk.runtime.StandardMemberCopier;

@Generated("software.amazon.awssdk:codegen")
final class MapOfStringToIntegerListCopier {
    static Map<String, List<Integer>> copy(Map<String, ? extends Collection<Integer>> mapOfStringToIntegerListParam) {
        if (mapOfStringToIntegerListParam == null) {
            return null;
        }
        Map<String, List<Integer>> mapOfStringToIntegerListParamCopy = new HashMap<>(mapOfStringToIntegerListParam.size());
        for (Map.Entry<String, ? extends Collection<Integer>> e : mapOfStringToIntegerListParam.entrySet()) {
            mapOfStringToIntegerListParamCopy.put(StandardMemberCopier.copy(e.getKey()), ListOfIntegersCopier.copy(e.getValue()));
        }
        return mapOfStringToIntegerListParamCopy;
    }
}

