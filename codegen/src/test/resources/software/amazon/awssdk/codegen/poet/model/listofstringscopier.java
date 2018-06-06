package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructList;

@Generated("software.amazon.awssdk:codegen")
final class ListOfStringsCopier {
    static List<String> copy(Collection<String> listOfStringsParam) {
        if (listOfStringsParam == null || listOfStringsParam instanceof SdkAutoConstructList) {
            return DefaultSdkAutoConstructList.getInstance();
        }
        List<String> listOfStringsParamCopy = new ArrayList<>(listOfStringsParam);
        return Collections.unmodifiableList(listOfStringsParamCopy);
    }
}
