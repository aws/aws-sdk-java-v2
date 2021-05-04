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
final class MapOfEnumToListOfEnumsCopier {
    static Map<String, List<String>> copy(Map<String, ? extends Collection<String>> mapOfEnumToListOfEnumsParam) {
        Map<String, List<String>> map;
        if (mapOfEnumToListOfEnumsParam == null || mapOfEnumToListOfEnumsParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, List<String>> modifiableMap = new LinkedHashMap<>();
            mapOfEnumToListOfEnumsParam.forEach((key, value) -> {
                List<String> list;
                if (value == null || value instanceof SdkAutoConstructList) {
                    list = DefaultSdkAutoConstructList.getInstance();
                } else {
                    List<String> modifiableList = new ArrayList<>();
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

    static Map<String, List<String>> copyEnumToString(Map<EnumType, ? extends Collection<EnumType>> mapOfEnumToListOfEnumsParam) {
        Map<String, List<String>> map;
        if (mapOfEnumToListOfEnumsParam == null || mapOfEnumToListOfEnumsParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<String, List<String>> modifiableMap = new LinkedHashMap<>();
            mapOfEnumToListOfEnumsParam.forEach((key, value) -> {
                String result = key.toString();
                List<String> list;
                if (value == null || value instanceof SdkAutoConstructList) {
                    list = DefaultSdkAutoConstructList.getInstance();
                } else {
                    List<String> modifiableList = new ArrayList<>();
                    value.forEach(entry -> {
                        String result1 = entry.toString();
                        modifiableList.add(result1);
                    });
                    list = Collections.unmodifiableList(modifiableList);
                }
                modifiableMap.put(result, list);
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }

    static Map<EnumType, List<EnumType>> copyStringToEnum(Map<String, ? extends Collection<String>> mapOfEnumToListOfEnumsParam) {
        Map<EnumType, List<EnumType>> map;
        if (mapOfEnumToListOfEnumsParam == null || mapOfEnumToListOfEnumsParam instanceof SdkAutoConstructMap) {
            map = DefaultSdkAutoConstructMap.getInstance();
        } else {
            Map<EnumType, List<EnumType>> modifiableMap = new LinkedHashMap<>();
            mapOfEnumToListOfEnumsParam.forEach((key, value) -> {
                EnumType result = EnumType.fromValue(key);
                List<EnumType> list;
                if (value == null || value instanceof SdkAutoConstructList) {
                    list = DefaultSdkAutoConstructList.getInstance();
                } else {
                    List<EnumType> modifiableList = new ArrayList<>();
                    value.forEach(entry -> {
                        EnumType result1 = EnumType.fromValue(entry);
                        modifiableList.add(result1);
                    });
                    list = Collections.unmodifiableList(modifiableList);
                }
                if (result != EnumType.UNKNOWN_TO_SDK_VERSION) {
                    modifiableMap.put(result, list);
                }
            });
            map = Collections.unmodifiableMap(modifiableMap);
        }
        return map;
    }
}
