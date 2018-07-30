package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;

@Generated("software.amazon.awssdk:codegen")
final class ListOfStringsCopier {
    static List<String> copy(Collection<String> listOfStringsParam) {
        if (listOfStringsParam == null) {
            return null;
        }
        List<String> listOfStringsParamCopy = new ArrayList<>(listOfStringsParam);
        return Collections.unmodifiableList(listOfStringsParamCopy);
    }
}
