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
final class ListOfSimpleStructsCopier {
    static List<SimpleStruct> copy(Collection<? extends SimpleStruct> listOfSimpleStructsParam) {
        List<SimpleStruct> list;
        if (listOfSimpleStructsParam == null || listOfSimpleStructsParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<SimpleStruct> modifiableList = new ArrayList<>();
            listOfSimpleStructsParam.forEach(entry -> {
                modifiableList.add(entry);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }

    static List<SimpleStruct> copyFromBuilder(Collection<? extends SimpleStruct.Builder> listOfSimpleStructsParam) {
        List<SimpleStruct> list;
        if (listOfSimpleStructsParam == null || listOfSimpleStructsParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<SimpleStruct> modifiableList = new ArrayList<>();
            listOfSimpleStructsParam.forEach(entry -> {
                SimpleStruct member = entry.build();
                modifiableList.add(member);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }

    static List<SimpleStruct.Builder> copyToBuilder(Collection<? extends SimpleStruct> listOfSimpleStructsParam) {
        List<SimpleStruct.Builder> list;
        if (listOfSimpleStructsParam == null || listOfSimpleStructsParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<SimpleStruct.Builder> modifiableList = new ArrayList<>();
            listOfSimpleStructsParam.forEach(entry -> {
                SimpleStruct.Builder member = entry.toBuilder();
                modifiableList.add(member);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }
}
