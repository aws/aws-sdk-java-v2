package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import software.amazon.awssdk.runtime.StandardMemberCopier;

@Generated("software.amazon.awssdk:codegen")
final class MapOfStringToListOfListOfStringsCopier {
    static Map<String, List<List<String>>> copy(
            Map<String, ? extends Collection<? extends Collection<String>>> mapOfStringToListOfListOfStringsParam) {
        if (mapOfStringToListOfListOfStringsParam == null) {
            return null;
        }
        Map<String, List<List<String>>> mapOfStringToListOfListOfStringsParamCopy = new HashMap<>(
                mapOfStringToListOfListOfStringsParam.size());
        for (Map.Entry<String, ? extends Collection<? extends Collection<String>>> e : mapOfStringToListOfListOfStringsParam
                .entrySet()) {
            mapOfStringToListOfListOfStringsParamCopy.put(StandardMemberCopier.copy(e.getKey()),
                    ListOfListOfStringsCopier.copy(e.getValue()));
        }
        return mapOfStringToListOfListOfStringsParamCopy;
    }
}

