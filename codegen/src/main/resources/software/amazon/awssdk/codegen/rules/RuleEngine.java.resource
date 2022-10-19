import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public interface RuleEngine {
    /**
     * Evaluate the given {@link EndpointRuleset} using the named values in {@code args} as input into the rule set.
     *
     * @param ruleset The rule set to evaluate.
     * @param args The arguments.
     * @return The computed value.
     */
    Value evaluate(EndpointRuleset ruleset, Map<Identifier, Value> args);

    static RuleEngine defaultEngine() {
        return new DefaultRuleEngine();
    }
}
