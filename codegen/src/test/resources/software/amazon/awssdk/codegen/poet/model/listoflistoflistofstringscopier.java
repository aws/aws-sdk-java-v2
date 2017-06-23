package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class ListOfListOfListOfStringsCopier {
    static List<List<List<String>>> copy(
            Collection<? extends Collection<? extends Collection<String>>> listOfListOfListOfStringsParam) {
        if (listOfListOfListOfStringsParam == null) {
            return null;
        }
        List<List<List<String>>> listOfListOfListOfStringsParamCopy = new ArrayList<>(listOfListOfListOfStringsParam.size());
        for (Collection<? extends Collection<String>> e : listOfListOfListOfStringsParam) {
            listOfListOfListOfStringsParamCopy.add(ListOfListOfStringsCopier.copy(e));
        }
        return listOfListOfListOfStringsParamCopy;
    }
}

