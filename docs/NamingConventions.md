## Naming Conventions

This page describes the naming conventions, nouns and common terms 

- Abbreviations must follow the same conventions as any other word (eg. use `DynamoDbClient`, not `DynamoDBClient`)

- Use Singular Enum Name

  For enum classes or "pseudo-enums" classes(classes with public static fields), we should use singular name. (eg. use `SdkSystemSetting`, not `SdkSystemSettings`)
  
- Use of `Provider`, `Supplier` and `Factory` in the class name.
  - For general supplier classes (loading/creating resources of the same kind), prefer `Provide` unless the class extends from Java `Supplier` class. (eg. `AwsCredentialsProvider`)
  - For factories classes (creating resources of same or different kinds), prefer  `Factory`. (eg. `AwsJsonProtocolFactory`)
 

