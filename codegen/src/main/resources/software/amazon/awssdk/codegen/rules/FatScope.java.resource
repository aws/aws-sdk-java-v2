import java.util.HashMap;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class FatScope<T> {
    private final HashMap<Identifier, T> types;
    private final HashMap<Expr, T> facts;

    public FatScope(HashMap<Identifier, T> types, HashMap<Expr, T> facts) {
        this.types = types;
        this.facts = facts;
    }

    public FatScope() {
        this(new HashMap<>(), new HashMap<>());
    }

    public HashMap<Identifier, T> types() {
        return types;
    }

    public HashMap<Expr, T> facts() {
        return facts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FatScope<?> fatScope = (FatScope<?>) o;

        if (types != null ? !types.equals(fatScope.types) : fatScope.types != null) {
            return false;
        }
        return facts != null ? facts.equals(fatScope.facts) : fatScope.facts == null;
    }

    @Override
    public int hashCode() {
        int result = types != null ? types.hashCode() : 0;
        result = 31 * result + (facts != null ? facts.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FatScope[" +
               "types=" + types + ", " +
               "facts=" + facts + ']';
    }

}
