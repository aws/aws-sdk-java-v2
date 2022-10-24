import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Pair;

@SdkInternalApi
public abstract class Fn extends Expr implements Into<Condition> {
    protected FnNode fnNode;

    public Fn(FnNode fnNode) {
        this.fnNode = fnNode;
    }

    /**
     * Convert this fn into a condition
     */
    public Condition condition() {
        return new Condition.Builder().fn(this).build();
    }

    public Condition condition(String result) {
        return new Condition.Builder().fn(this).result(result).build();
    }

    public abstract <T> T acceptFnVisitor(FnVisitor<T> visitor);

    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitFn(this);
    }

    /**
     * Returns the name of this function, eg. {@code isSet}, {@code parseUrl}
     * @return The name
     */
    public String getName() {
        return fnNode.getId();
    }

    /**
     * @return The arguments to this function
     */
    public List<Expr> getArgv() {
        return fnNode.getArgv();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        Fn fn = (Fn) o;

        return fnNode != null ? fnNode.equals(fn.fnNode) : fn.fnNode == null;
    }

    @Override
    public int hashCode() {
        return fnNode != null ? fnNode.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", fnNode.getId(),
                             fnNode.getArgv().stream().map(Expr::toString).collect(Collectors.joining(", ")));
    }

    protected Expr expectOneArg() {
        List<Expr> argv = this.fnNode.getArgv();
        if (argv.size() == 1) {
            return argv.get(0);
        } else {
            throw RuleError.builder()
                           .cause(SourceException.builder()
                                                 .message("expected 1 argument but found " + argv.size())
                                                 .build())
                           .build();
        }
    }

    protected Pair<Expr, Expr> expectTwoArgs() {
        List<Expr> argv = this.fnNode.getArgv();
        if (argv.size() == 2) {
            return Pair.of(argv.get(0), argv.get(1));
        } else {
            throw RuleError.builder()
                           .cause(SourceException.builder()
                                                 .message("expected 2 arguments but found " + argv.size())
                                                 .build())
                           .build();
        }

    }

    protected List<Expr> expectVariableArgs(int expectedNumberArgs) {
        List<Expr> argv = this.fnNode.getArgv();
        if (argv.size() == expectedNumberArgs) {
            return argv;
        } else {
            throw RuleError.builder()
                           .cause(SourceException.builder()
                                                 .message(String.format("expected %d arguments but found %d",
                                                                        expectedNumberArgs, argv.size()))
                                                 .build())
                           .build();
        }

    }

    @Override
    public Condition into() {
        return this.condition();
    }
}
