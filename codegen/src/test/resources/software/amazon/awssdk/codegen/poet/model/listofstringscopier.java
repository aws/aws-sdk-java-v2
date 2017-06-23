package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class ListOfStringsCopier {
    static List<String> copy(Collection<String> listOfStringsParam) {
        if (listOfStringsParam == null) {
            return null;
        }
        List<String> listOfStringsParamCopy = new ArrayList<>(listOfStringsParam.size());
        for (String e : listOfStringsParam) {
            listOfStringsParamCopy.add(e);
        }
        return listOfStringsParamCopy;
    }
}

