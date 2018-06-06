package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class ListOfListOfListOfStringsCopier {
    static List<List<List<String>>> copy(
            Collection<? extends Collection<? extends Collection<String>>> listOfListOfListOfStringsParam) {
        if (listOfListOfListOfStringsParam == null) {
            return null;
        }
        List<List<List<String>>> listOfListOfListOfStringsParamCopy = listOfListOfListOfStringsParam.stream()
                .map(ListOfListOfStringsCopier::copy).collect(toList());
        return Collections.unmodifiableList(listOfListOfListOfStringsParamCopy);
    }
}
