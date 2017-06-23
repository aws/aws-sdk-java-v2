package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class RecursiveListTypeCopier {
    static List<RecursiveStructType> copy(Collection<RecursiveStructType> recursiveListTypeParam) {
        if (recursiveListTypeParam == null) {
            return null;
        }
        List<RecursiveStructType> recursiveListTypeParamCopy = new ArrayList<>(recursiveListTypeParam.size());
        for (RecursiveStructType e : recursiveListTypeParam) {
            recursiveListTypeParamCopy.add(e);
        }
        return recursiveListTypeParamCopy;
    }
}

