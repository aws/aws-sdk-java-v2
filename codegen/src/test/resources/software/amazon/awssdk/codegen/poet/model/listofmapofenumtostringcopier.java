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
final class ListOfMapOfEnumToStringCopier {
    static List<Map<String, String>> copy(Collection<? extends Map<String, String>> listOfMapOfEnumToStringParam) {
        List<Map<String, String>> list;
        if (listOfMapOfEnumToStringParam == null || listOfMapOfEnumToStringParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<Map<String, String>> modifiableList = new ArrayList<>();
            listOfMapOfEnumToStringParam.forEach(entry -> {
                Map<String, String> map;
                if (entry == null || entry instanceof SdkAutoConstructMap) {
                    map = DefaultSdkAutoConstructMap.getInstance();
                } else {
                    Map<String, String> modifiableMap = new LinkedHashMap<>();
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

    static List<Map<String, String>> copyEnumToString(Collection<? extends Map<EnumType, String>> listOfMapOfEnumToStringParam) {
        List<Map<String, String>> list;
        if (listOfMapOfEnumToStringParam == null || listOfMapOfEnumToStringParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<Map<String, String>> modifiableList = new ArrayList<>();
            listOfMapOfEnumToStringParam.forEach(entry -> {
                Map<String, String> map;
                if (entry == null || entry instanceof SdkAutoConstructMap) {
                    map = DefaultSdkAutoConstructMap.getInstance();
                } else {
                    Map<String, String> modifiableMap = new LinkedHashMap<>();
                    entry.forEach((key, value) -> {
                        String result = key.toString();
                        modifiableMap.put(result, value);
                    });
                    map = Collections.unmodifiableMap(modifiableMap);
                }
                modifiableList.add(map);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }

    static List<Map<EnumType, String>> copyStringToEnum(Collection<? extends Map<String, String>> listOfMapOfEnumToStringParam) {
        List<Map<EnumType, String>> list;
        if (listOfMapOfEnumToStringParam == null || listOfMapOfEnumToStringParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<Map<EnumType, String>> modifiableList = new ArrayList<>();
            listOfMapOfEnumToStringParam.forEach(entry -> {
                Map<EnumType, String> map;
                if (entry == null || entry instanceof SdkAutoConstructMap) {
                    map = DefaultSdkAutoConstructMap.getInstance();
                } else {
                    Map<EnumType, String> modifiableMap = new LinkedHashMap<>();
                    entry.forEach((key, value) -> {
                        EnumType result = EnumType.fromValue(key);
                        if (result != EnumType.UNKNOWN_TO_SDK_VERSION) {
                            modifiableMap.put(result, value);
                        }
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
