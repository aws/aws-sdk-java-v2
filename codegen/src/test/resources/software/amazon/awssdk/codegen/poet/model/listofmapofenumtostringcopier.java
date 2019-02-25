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
final class ListOfMapOfEnumToStringCopier {
    static List<Map<String, String>> copy(Collection<? extends Map<String, String>> listOfMapOfEnumToStringParam) {
        if (listOfMapOfEnumToStringParam == null || listOfMapOfEnumToStringParam instanceof SdkAutoConstructList) {
            return DefaultSdkAutoConstructList.getInstance();
        }
        List<Map<String, String>> listOfMapOfEnumToStringParamCopy = listOfMapOfEnumToStringParam.stream()
                .map(MapOfEnumToStringCopier::copy).collect(toList());
        return Collections.unmodifiableList(listOfMapOfEnumToStringParamCopy);
    }

    static List<Map<String, String>> copyEnumToString(Collection<? extends Map<EnumType, String>> listOfMapOfEnumToStringParam) {
        if (listOfMapOfEnumToStringParam == null || listOfMapOfEnumToStringParam instanceof SdkAutoConstructList) {
            return DefaultSdkAutoConstructList.getInstance();
        }
        List<Map<String, String>> listOfMapOfEnumToStringParamCopy = listOfMapOfEnumToStringParam.stream()
                .map(MapOfEnumToStringCopier::copyEnumToString).collect(toList());
        return Collections.unmodifiableList(listOfMapOfEnumToStringParamCopy);
    }

    static List<Map<EnumType, String>> copyStringToEnum(Collection<? extends Map<String, String>> listOfMapOfEnumToStringParam) {
        if (listOfMapOfEnumToStringParam == null || listOfMapOfEnumToStringParam instanceof SdkAutoConstructList) {
            return DefaultSdkAutoConstructList.getInstance();
        }
        List<Map<EnumType, String>> listOfMapOfEnumToStringParamCopy = listOfMapOfEnumToStringParam.stream()
                .map(MapOfEnumToStringCopier::copyStringToEnum).collect(toList());
        return Collections.unmodifiableList(listOfMapOfEnumToStringParamCopy);
    }
}
