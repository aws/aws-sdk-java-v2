package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

@Generated("software.amazon.awssdk:codegen")
final class MapOfStringToListOfListOfStringsCopier {
    static Map<String, List<List<String>>> copy(
        Map<String, ? extends Collection<? extends Collection<String>>> mapOfStringToListOfListOfStringsParam) {
        Map<String, List<List<String>>> map;
        if (mapOfStringToListOfListOfStringsParam == null || mapOfStringToListOfListOfStringsParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, List<List<String>>> modifiableMap = new LinkedHashMap<>();
            mapOfStringToListOfListOfStringsParam.forEach((key, value) -> {
                List<List<String>> list;
                if (value == null || value instanceof SdkAutoConstructList) {
                    list = DefaultSdkAutoConstructList.getInstance();
                } else {
                    List<List<String>> modifiableList = new ArrayList<>();
                    value.forEach(entry -> {
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
                modifiableMap.put(key, list);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }
}
