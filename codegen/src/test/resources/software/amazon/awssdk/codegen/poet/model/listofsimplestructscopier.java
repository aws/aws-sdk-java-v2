package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class ListOfSimpleStructsCopier {
    static List<SimpleStruct> copy(Collection<SimpleStruct> listOfSimpleStructsParam) {
        if (listOfSimpleStructsParam == null) {
            return null;
        }
        List<SimpleStruct> listOfSimpleStructsParamCopy = listOfSimpleStructsParam.stream().collect(toList());
        return Collections.unmodifiableList(listOfSimpleStructsParamCopy);
    }

    static List<SimpleStruct> copyFromBuilder(Collection<? extends SimpleStruct.Builder> listOfSimpleStructsParam) {
        if (listOfSimpleStructsParam == null) {
            return null;
        }
        return copy(listOfSimpleStructsParam.stream().map(SimpleStruct.Builder::build).collect(toList()));
    }
}

