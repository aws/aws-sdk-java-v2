package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructList;

@Generated("software.amazon.awssdk:codegen")
final class ListOfListOfStringsCopier {
    static List<List<String>> copy(Collection<? extends Collection<String>> listOfListOfStringsParam) {
        if (listOfListOfStringsParam == null || listOfListOfStringsParam instanceof SdkAutoConstructList) {
            return DefaultSdkAutoConstructList.getInstance();
        }
        List<List<String>> listOfListOfStringsParamCopy = listOfListOfStringsParam.stream().map(ListOfStringsCopier::copy)
                .collect(toList());
        return Collections.unmodifiableList(listOfListOfStringsParamCopy);
    }
}
