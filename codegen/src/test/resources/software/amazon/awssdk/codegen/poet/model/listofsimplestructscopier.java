package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class ListOfSimpleStructsCopier {
    static List<SimpleStruct> copy(Collection<SimpleStruct> listOfSimpleStructsParam) {
        if (listOfSimpleStructsParam == null) {
            return null;
        }
        List<SimpleStruct> listOfSimpleStructsParamCopy = new ArrayList<>(listOfSimpleStructsParam.size());
        for (SimpleStruct e : listOfSimpleStructsParam) {
            listOfSimpleStructsParamCopy.add(e);
        }
        return listOfSimpleStructsParamCopy;
    }
}

