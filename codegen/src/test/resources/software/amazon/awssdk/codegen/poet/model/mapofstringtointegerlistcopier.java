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
final class MapOfStringToIntegerListCopier {
    static Map<String, List<Integer>> copy(Map<String, ? extends Collection<Integer>> mapOfStringToIntegerListParam) {
        Map<String, List<Integer>> map;
        if (mapOfStringToIntegerListParam == null || mapOfStringToIntegerListParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, List<Integer>> modifiableMap = new LinkedHashMap<>();
            mapOfStringToIntegerListParam.forEach((key, value) -> {
                List<Integer> list;
                if (value == null || value instanceof SdkAutoConstructList) {
                    list = DefaultSdkAutoConstructList.getInstance();
                } else {
                    List<Integer> modifiableList = new ArrayList<>();
                    value.forEach(entry -> {
                        modifiableList.add(entry);
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
