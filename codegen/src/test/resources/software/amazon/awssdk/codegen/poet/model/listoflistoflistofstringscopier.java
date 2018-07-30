package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructList;

@Generated("software.amazon.awssdk:codegen")
final class ListOfListOfListOfStringsCopier {
    static List<List<List<String>>> copy(
            Collection<? extends Collection<? extends Collection<String>>> listOfListOfListOfStringsParam) {
        if (listOfListOfListOfStringsParam == null || listOfListOfListOfStringsParam instanceof SdkAutoConstructList) {
            return DefaultSdkAutoConstructList.getInstance();
        }
        List<List<List<String>>> listOfListOfListOfStringsParamCopy = listOfListOfListOfStringsParam.stream()
                .map(ListOfListOfStringsCopier::copy).collect(toList());
        return Collections.unmodifiableList(listOfListOfListOfStringsParamCopy);
    }
}
