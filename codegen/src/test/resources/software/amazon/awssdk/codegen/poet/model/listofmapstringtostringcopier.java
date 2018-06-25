package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructList;

@Generated("software.amazon.awssdk:codegen")
final class ListOfMapStringToStringCopier {
    static List<Map<String, String>> copy(Collection<Map<String, String>> listOfMapStringToStringParam) {
        if (listOfMapStringToStringParam == null || listOfMapStringToStringParam instanceof SdkAutoConstructList) {
            return DefaultSdkAutoConstructList.getInstance();
        }
        List<Map<String, String>> listOfMapStringToStringParamCopy = listOfMapStringToStringParam.stream()
                .map(MapOfStringToStringCopier::copy).collect(toList());
        return Collections.unmodifiableList(listOfMapStringToStringParamCopy);
    }
}
