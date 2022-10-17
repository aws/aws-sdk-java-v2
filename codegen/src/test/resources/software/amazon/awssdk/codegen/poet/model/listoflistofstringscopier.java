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
final class ListOfListOfStringsCopier {
    static List<List<String>> copy(Collection<? extends Collection<String>> listOfListOfStringsParam) {
        List<List<String>> list;
        if (listOfListOfStringsParam == null || listOfListOfStringsParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<List<String>> modifiableList = new ArrayList<>();
            listOfListOfStringsParam.forEach(entry -> {
                List<String> list1;
                if (entry == null || entry instanceof SdkAutoConstructList) {
                    list1 = DefaultSdkAutoConstructList.getInstance();
                } else {
                    List<String> modifiableList1 = new ArrayList<>();
                    entry.forEach(entry1 -> {
                        modifiableList1.add(entry1);
                    });
                    list1 = Collections.unmodifiableList(modifiableList1);
                }
                modifiableList.add(list1);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }
}
