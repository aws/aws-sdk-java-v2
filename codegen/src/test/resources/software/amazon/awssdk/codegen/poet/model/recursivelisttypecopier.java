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
final class RecursiveListTypeCopier {
    static List<RecursiveStructType> copy(Collection<? extends RecursiveStructType> recursiveListTypeParam) {
        List<RecursiveStructType> list;
        if (recursiveListTypeParam == null || recursiveListTypeParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<RecursiveStructType> modifiableList = new ArrayList<>();
            recursiveListTypeParam.forEach(entry -> {
                modifiableList.add(entry);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }

    static List<RecursiveStructType> copyFromBuilder(Collection<? extends RecursiveStructType.Builder> recursiveListTypeParam) {
        List<RecursiveStructType> list;
        if (recursiveListTypeParam == null || recursiveListTypeParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<RecursiveStructType> modifiableList = new ArrayList<>();
            recursiveListTypeParam.forEach(entry -> {
                RecursiveStructType member = entry.build();
                modifiableList.add(member);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }

    static List<RecursiveStructType.Builder> copyToBuilder(Collection<? extends RecursiveStructType> recursiveListTypeParam) {
        List<RecursiveStructType.Builder> list;
        if (recursiveListTypeParam == null || recursiveListTypeParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<RecursiveStructType.Builder> modifiableList = new ArrayList<>();
            recursiveListTypeParam.forEach(entry -> {
                RecursiveStructType.Builder member = entry.toBuilder();
                modifiableList.add(member);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }
}
