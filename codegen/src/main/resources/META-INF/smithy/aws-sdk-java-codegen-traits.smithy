$version: "2"

namespace software.amazon.awssdk.codegen

/// Generates deprecated accessors under this name for a renamed member.
@trait(selector: ":is(structure, union) > member")
string deprecatedName

/// Generates an extra bean-style setter named "set" followed by this value.
@trait(selector: ":is(structure, union) > member")
string alternateBeanPropertyName

/// Unmarshalling ignores a value that cannot be converted to the member's type.
@trait(selector: ":is(structure, union) > member")
structure ignoreDataTypeConversionFailures {}

/// The Java enum constant name for this enum value.
@trait(selector: "enum > member")
string enumConstantName

/// A shape added for SDK use only. Members that target it are not marshalled.
@trait(selector: "simpleType")
structure synthetic {}

