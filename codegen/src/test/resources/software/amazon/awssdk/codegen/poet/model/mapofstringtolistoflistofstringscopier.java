package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;

@Generated("software.amazon.awssdk:codegen")
final class MapOfStringToListOfListOfStringsCopier {
    static Map<String, List<List<String>>> copy(
            Map<String, ? extends Collection<? extends Collection<String>>> mapOfStringToListOfListOfStringsParam) {
        if (mapOfStringToListOfListOfStringsParam == null) {
            return null;
        }
        Map<String, List<List<String>>> mapOfStringToListOfListOfStringsParamCopy = mapOfStringToListOfListOfStringsParam
                .entrySet().stream().collect(toMap(Map.Entry::getKey, e -> ListOfListOfStringsCopier.copy(e.getValue())));
        return Collections.unmodifiableMap(mapOfStringToListOfListOfStringsParamCopy);
    }
}
