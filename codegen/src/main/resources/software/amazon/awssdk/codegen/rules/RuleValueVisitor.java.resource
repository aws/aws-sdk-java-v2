import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Visitor for the right-hand side of rules (tree, error, endpoint)
 * @param <R> The return type of the visitor
 */
@SdkInternalApi
public interface RuleValueVisitor<R> {
    R visitTreeRule(List<Rule> rules);

    R visitErrorRule(Expr error);

    R visitEndpointRule(EndpointResult endpoint);
}
