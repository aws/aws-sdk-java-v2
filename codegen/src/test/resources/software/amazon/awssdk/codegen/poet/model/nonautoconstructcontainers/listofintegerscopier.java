package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class ListOfIntegersCopier {
    static List<Integer> copy(Collection<Integer> listOfIntegersParam) {
        if (listOfIntegersParam == null) {
            return null;
        }
        List<Integer> listOfIntegersParamCopy = new ArrayList<>(listOfIntegersParam);
        return Collections.unmodifiableList(listOfIntegersParamCopy);
    }
}
