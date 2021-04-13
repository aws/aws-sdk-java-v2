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
final class ListOfMapOfStringToStructCopier {
    static List<Map<String, SimpleStruct>> copy(Collection<? extends Map<String, SimpleStruct>> listOfMapOfStringToStructParam) {
        if (listOfMapOfStringToStructParam == null || listOfMapOfStringToStructParam instanceof SdkAutoConstructList) {
            return DefaultSdkAutoConstructList.getInstance();
        }
        List<Map<String, SimpleStruct>> listOfMapOfStringToStructParamCopy = listOfMapOfStringToStructParam.stream()
                                                                                                           .map(MapOfStringToSimpleStructCopier::copy).collect(toList());
        return Collections.unmodifiableList(listOfMapOfStringToStructParamCopy);
    }

    static List<Map<String, SimpleStruct>> copyFromBuilder(
        Collection<? extends Map<String, ? extends SimpleStruct.Builder>> listOfMapOfStringToStructParam) {
        if (listOfMapOfStringToStructParam == null || listOfMapOfStringToStructParam instanceof DefaultSdkAutoConstructList) {
            return DefaultSdkAutoConstructList.getInstance();
        }
        return copy(listOfMapOfStringToStructParam.stream().map(MapOfStringToSimpleStructCopier::copyFromBuilder).collect(toList()));
    }
}
