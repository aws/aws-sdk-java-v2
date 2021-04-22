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
final class ListOfListOfListOfStringsCopier {
    static List<List<List<String>>> copy(
        Collection<? extends Collection<? extends Collection<String>>> listOfListOfListOfStringsParam) {
        List<List<List<String>>> list;
        if (listOfListOfListOfStringsParam == null || listOfListOfListOfStringsParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<List<List<String>>> modifiableList = new ArrayList<>();
            listOfListOfListOfStringsParam.forEach(entry -> {
                List<List<String>> list1;
                if (entry == null || entry instanceof SdkAutoConstructList) {
                    list1 = DefaultSdkAutoConstructList.getInstance();
                } else {
                    List<List<String>> modifiableList1 = new ArrayList<>();
                    entry.forEach(entry1 -> {
                        List<String> list2;
                        if (entry1 == null || entry1 instanceof SdkAutoConstructList) {
                            list2 = DefaultSdkAutoConstructList.getInstance();
                        } else {
                            List<String> modifiableList2 = new ArrayList<>();
                            entry1.forEach(entry2 -> {
                                modifiableList2.add(entry2);
                            });
                            list2 = Collections.unmodifiableList(modifiableList2);
                        }
                        modifiableList1.add(list2);
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
