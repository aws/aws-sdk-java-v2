import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class TreeRule extends Rule {
    private final List<Rule> rules;

    protected TreeRule(Builder builder, List<Rule> rules) {
        super(builder);
        this.rules = rules;
    }

    @Override
    public <T> T accept(RuleValueVisitor<T> v) {
        return v.visitTreeRule(rules);
    }

    @Override
    public String toString() {
        return "TreeRule{" +
               "conditions=" + conditions +
               ", documentation='" + documentation + '\'' +
               ", rules=" + rules +
               '}';
    }
}
