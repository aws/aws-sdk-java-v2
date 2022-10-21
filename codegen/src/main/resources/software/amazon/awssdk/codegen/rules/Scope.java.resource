import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class Scope<T> {
    private final Deque<FatScope<T>> scope;

    public Scope() {
        this.scope = new ArrayDeque<>();
        this.scope.push(new FatScope<T>());
    }

    public void push() {
        scope.push(new FatScope<>());
    }

    public void pop() {
        scope.pop();
    }

    public void insert(String name, T value) {
        this.insert(Identifier.of(name), value);
    }

    public void insert(Identifier name, T value) {
        this.scope.getFirst().types().put(name, value);
    }

    public void insertFact(Expr name, T value) {
        this.scope.getFirst().facts().put(name, value);
    }

    public <U> U inScope(Supplier<U> func) {
        this.push();
        try {
            return func.get();
        } finally {
            this.pop();
        }
    }

    @Override
    public String toString() {
        HashMap<Identifier, T> toPrint = new HashMap<>();
        for (FatScope<T> layer: scope) {
            toPrint.putAll(layer.types());
        }
        return toPrint.toString();
    }

    /**
     * Search the fact stack for an explicitly calculated value for [expr]
     * <p>
     * Currently, this is only impacted by the `isSet` function which will record
     * `T`, rather than {@code Option<T>} for its arguments
     *
     * @param expr The expression to evaluate
     * @return The value from the scope
     */
    public Optional<T> eval(Expr expr) {
        for (FatScope<T> layer : scope) {
            if (layer.facts().containsKey(expr)) {
                return Optional.of(layer.facts().get(expr));
            }
        }
        return Optional.empty();
    }

    public T expectValue(Identifier name) {
        for (FatScope<T> layer : scope) {
            if (layer.types().containsKey(name)) {
                return layer.types().get(name);
            }
        }
        throw new InnerParseError(String.format("No field named %s", name));
    }

    public Optional<T> getValue(Identifier name) {
        for (FatScope<T> layer : scope) {
            if (layer.types().containsKey(name)) {
                return Optional.of(layer.types().get(name));
            }
        }
        return Optional.empty();
    }
}
