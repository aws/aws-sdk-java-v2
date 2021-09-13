package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

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
final class ListOfMapOfStringToStructCopier {
    static List<Map<String, SimpleStruct>> copy(
        Collection<? extends Map<String, ? extends SimpleStruct>> listOfMapOfStringToStructParam) {
        List<Map<String, SimpleStruct>> list;
        if (listOfMapOfStringToStructParam == null || listOfMapOfStringToStructParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<Map<String, SimpleStruct>> modifiableList = new ArrayList<>();
            listOfMapOfStringToStructParam.forEach(entry -> {
                Map<String, SimpleStruct> map;
                if (entry == null || entry instanceof SdkAutoConstructMap) {
                    map = DefaultSdkAutoConstructMap.getInstance();
                } else {
                    Map<String, SimpleStruct> modifiableMap = new LinkedHashMap<>();
                    entry.forEach((key, value) -> {
                        modifiableMap.put(key, value);
                    });
                    map = Collections.unmodifiableMap(modifiableMap);
                }
                modifiableList.add(map);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }

    static List<Map<String, SimpleStruct>> copyFromBuilder(
        Collection<? extends Map<String, ? extends SimpleStruct.Builder>> listOfMapOfStringToStructParam) {
        List<Map<String, SimpleStruct>> list;
        if (listOfMapOfStringToStructParam == null || listOfMapOfStringToStructParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<Map<String, SimpleStruct>> modifiableList = new ArrayList<>();
            listOfMapOfStringToStructParam.forEach(entry -> {
                Map<String, SimpleStruct> map;
                if (entry == null || entry instanceof SdkAutoConstructMap) {
                    map = DefaultSdkAutoConstructMap.getInstance();
                } else {
                    Map<String, SimpleStruct> modifiableMap = new LinkedHashMap<>();
                    entry.forEach((key, value) -> {
                        SimpleStruct member = value.build();
                        modifiableMap.put(key, member);
                    });
                    map = Collections.unmodifiableMap(modifiableMap);
                }
                modifiableList.add(map);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }

    static List<Map<String, SimpleStruct.Builder>> copyToBuilder(
        Collection<? extends Map<String, ? extends SimpleStruct>> listOfMapOfStringToStructParam) {
        List<Map<String, SimpleStruct.Builder>> list;
        if (listOfMapOfStringToStructParam == null || listOfMapOfStringToStructParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<Map<String, SimpleStruct.Builder>> modifiableList = new ArrayList<>();
            listOfMapOfStringToStructParam.forEach(entry -> {
                Map<String, SimpleStruct.Builder> map;
                if (entry == null || entry instanceof SdkAutoConstructMap) {
                    map = DefaultSdkAutoConstructMap.getInstance();
                } else {
                    Map<String, SimpleStruct.Builder> modifiableMap = new LinkedHashMap<>();
                    entry.forEach((key, value) -> {
                        SimpleStruct.Builder member = value.toBuilder();
                        modifiableMap.put(key, member);
                    });
                    map = Collections.unmodifiableMap(modifiableMap);
                }
                modifiableList.add(map);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }
}
