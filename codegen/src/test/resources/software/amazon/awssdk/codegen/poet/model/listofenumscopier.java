package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructList;

@Generated("software.amazon.awssdk:codegen")
final class ListOfEnumsCopier {
    static List<String> copy(Collection<String> listOfEnumsParam) {
        if (listOfEnumsParam == null || listOfEnumsParam instanceof SdkAutoConstructList) {
            return DefaultSdkAutoConstructList.getInstance();
        }
        List<String> listOfEnumsParamCopy = new ArrayList<>(listOfEnumsParam);
        return Collections.unmodifiableList(listOfEnumsParamCopy);
    }

    static List<String> copyEnumToString(Collection<EnumType> listOfEnumsParam) {
        if (listOfEnumsParam == null || listOfEnumsParam instanceof SdkAutoConstructList) {
            return DefaultSdkAutoConstructList.getInstance();
        }
        List<String> listOfEnumsParamCopy = listOfEnumsParam.stream().map(Object::toString).collect(toList());
        return Collections.unmodifiableList(listOfEnumsParamCopy);
    }

    static List<EnumType> copyStringToEnum(Collection<String> listOfEnumsParam) {
        if (listOfEnumsParam == null || listOfEnumsParam instanceof SdkAutoConstructList) {
            return DefaultSdkAutoConstructList.getInstance();
        }
        List<EnumType> listOfEnumsParamCopy = listOfEnumsParam.stream().map(EnumType::fromValue).collect(toList());
        return Collections.unmodifiableList(listOfEnumsParamCopy);
    }
}
