package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class ListOfListOfStringsCopier {
    static List<List<String>> copy(Collection<? extends Collection<String>> listOfListOfStringsParam) {
        if (listOfListOfStringsParam == null) {
            return null;
        }
        List<List<String>> listOfListOfStringsParamCopy = listOfListOfStringsParam.stream()
                                                                                  .map(ListOfStringsCopier::copy)
                                                                                  .collect(toList());
        return Collections.unmodifiableList(listOfListOfStringsParamCopy);
    }
}

