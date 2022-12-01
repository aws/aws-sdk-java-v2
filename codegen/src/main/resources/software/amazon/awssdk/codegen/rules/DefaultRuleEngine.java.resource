import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class DefaultRuleEngine implements RuleEngine {
    private final RuleEvaluator evaluator = new RuleEvaluator();

    @Override
    public Value evaluate(EndpointRuleset ruleset, Map<Identifier, Value> args) {
        return evaluator.evaluateRuleset(ruleset, args);
    }
}
