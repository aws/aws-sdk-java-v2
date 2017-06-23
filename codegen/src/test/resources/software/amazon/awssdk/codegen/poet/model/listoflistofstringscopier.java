package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class ListOfListOfStringsCopier {
    static List<List<String>> copy(Collection<? extends Collection<String>> listOfListOfStringsParam) {
        if (listOfListOfStringsParam == null) {
            return null;
        }
        List<List<String>> listOfListOfStringsParamCopy = new ArrayList<>(listOfListOfStringsParam.size());
        for (Collection<String> e : listOfListOfStringsParam) {
            listOfListOfStringsParamCopy.add(ListOfStringsCopier.copy(e));
        }
        return listOfListOfStringsParamCopy;
    }
}

