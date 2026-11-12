$version: "2"

namespace software.amazon.awssdk.codegen

/// Generates deprecated accessors under this name for a renamed member. Codegen does not support it on a member
/// that targets a list, a map, or an enum.
@trait(selector: ":is(structure, union) > member:not(:test(> :is(list, map, enum, [trait|enum])))")
@length(min: 1)
string deprecatedName

/// Generates an extra bean-style setter named "set" followed by this value.
@trait(selector: ":is(structure, union) > member")
@length(min: 1)
string alternateBeanPropertyName

/// Unmarshalling ignores a value that cannot be converted to the member's type.
@trait(selector: ":is(structure, union) > member")
structure ignoreDataTypeConversionFailures {}

/// The Java enum constant name for this enum value.
@trait(selector: "enum > member")
@length(min: 1)
string enumConstantName

/// A shape added for SDK use only. Members that target it are not marshalled.
@trait(selector: ":is(simpleType, list, map, structure, union)")
structure synthetic {}


/// Replaces the paginator that @paginated produces for this operation, for a paginator @paginated cannot express.
/// Each member means what the paginators-1.json field of the same name means, and names members by their Smithy
/// member names. With skip, no paginator is generated.
@trait(selector: "operation")
structure paginatorOverride {
    skip: Boolean
    inputToken: PaginatorMemberPaths
    outputToken: PaginatorMemberPaths
    resultKey: PaginatorMemberPaths
    moreResults: String
    limitKey: String
}

@private
list PaginatorMemberPaths {
    member: String
}

/// Java-only data for this operation's waiters, keyed by waiter name. Each entry gives maxAttempts, which Smithy
/// waiters do not have. With delay and acceptors, the entry replaces the @waitable waiter, for a waiters-2.json
/// waiter @waitable cannot express. With skip, no waiter is generated.
@trait(selector: "operation")
map waiterOverrides {
    key: String
    value: WaiterOverride
}

@private
structure WaiterOverride {
    skip: Boolean

    @range(min: 1)
    maxAttempts: Integer

    @range(min: 0)
    delay: Integer

    acceptors: WaiterOverrideAcceptors
}

@private
list WaiterOverrideAcceptors {
    member: WaiterOverrideAcceptor
}

/// A waiters-2.json acceptor.
@private
structure WaiterOverrideAcceptor {
    @required
    matcher: WaiterOverrideMatcher

    argument: String

    @required
    state: WaiterOverrideState

    /// A string, number, or boolean.
    @required
    expected: Document
}

@private
enum WaiterOverrideMatcher {
    STATUS = "status"
    ERROR = "error"
    PATH = "path"
    PATH_ALL = "pathAll"
    PATH_ANY = "pathAny"
}

@private
enum WaiterOverrideState {
    SUCCESS = "success"
    FAILURE = "failure"
    RETRY = "retry"
}
