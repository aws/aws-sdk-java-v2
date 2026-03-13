# Code Generation Guidelines

## Table of Contents
- [Overview](#overview)
- [Architecture Overview](#architecture-overview)
- [Poet Specification Development](#poet-specification-development)
- [Generator Task Organization](#generator-task-organization)
- [Generated Code Standards](#generated-code-standards)
- [Model Processing](#model-processing)
- [Customization and Extension](#customization-and-extension)
- [Code Examples and References](#code-examples-and-references)

## Overview

This document provides guidelines for developing and maintaining code generation components in the AWS SDK for Java v2. Code generation is a critical part of the SDK that transforms service models into Java client code using JavaPoet for programmatic Java code generation.

## Architecture Overview

The AWS SDK v2 code generation system is built around several key components:

### Core Components
- **CodeGenerator**: Main orchestrator that coordinates the generation process
- **IntermediateModel**: Processed service model used for code generation
- **Poet Specs**: JavaPoet-based specifications that define generated classes
- **Generator Tasks**: Parallel execution units that generate specific code artifacts

### JavaPoet Integration
The SDK uses Square's JavaPoet library for type-safe Java code generation:
- **ClassSpec**: Interface defining generated class specifications
- **TypeSpec**: JavaPoet's class/interface/enum builders
- **MethodSpec**: Method generation specifications
- **FieldSpec**: Field generation specifications

## Poet Specification Development

### Creating ClassSpec Implementations

When creating new code generators, implement the `ClassSpec` interface:

```java
public class MyGeneratorSpec implements ClassSpec {
    @Override
    public TypeSpec poetSpec() {
        return TypeSpec.classBuilder(className())
                      .addAnnotation(PoetUtils.generatedAnnotation())
                      .addModifiers(Modifier.PUBLIC)
                      .build();
    }

    @Override
    public ClassName className() {
        return ClassName.get("com.example", "GeneratedClass");
    }
}
```

#### ClassSpec Best Practices
- Always include the `@Generated` annotation using `PoetUtils.generatedAnnotation()`
- Use meaningful class names that reflect the generated component's purpose
- Implement `staticImports()` when static imports improve code readability
- Keep poet specifications focused on single responsibilities

### JavaPoet Code Generation Patterns

#### Type Safety
- Use `ClassName.get()` for referencing existing classes
- Use `ParameterizedTypeName.get()` for generic types
- Leverage `TypeVariableName` for generic type parameters
- Use `WildcardTypeName` for wildcard types

#### Code Block Construction
```java
CodeBlock.builder()
    .add("$T.<$T>builder($T.$L)\n", 
         SdkField.class, fieldType, 
         MarshallingType.class, marshallingType)
    .add(".memberName($S)\n", memberName)
    .add(".build()")
    .build();
```

#### Method Generation
```java
MethodSpec.methodBuilder("methodName")
    .addModifiers(Modifier.PUBLIC)
    .returns(returnType)
    .addParameter(paramType, "paramName")
    .addStatement("return $L", expression)
    .build();
```

### Poet Utilities

#### PoetUtils Helper Methods
- `generatedAnnotation()`: Standard @Generated annotation
- `createClassBuilder()`: Class builder with @Generated annotation
- `createInterfaceBuilder()`: Interface builder with @Generated annotation
- `addJavadoc()`: Safe JavaDoc addition with proper escaping
- `buildJavaFile()`: Convert ClassSpec to JavaFile with static imports

## Generator Task Organization

### Task Hierarchy
Code generation is organized into hierarchical tasks:
- **AwsGeneratorTasks**: Top-level coordinator
- **CommonGeneratorTasks**: Shared model and client generation
- **AsyncClientGeneratorTasks**: Async client-specific generation
- **PaginatorsGeneratorTasks**: Paginator generation
- **EventStreamGeneratorTasks**: Event stream support
- **WaitersGeneratorTasks**: Waiter generation

### Task Implementation Pattern
```java
public class MyGeneratorTask extends GeneratorTask {
    @Override
    public void compute() {
        // Generate code using JavaPoet
        ClassSpec spec = new MyClassSpec(model);
        new PoetGeneratorTask(outputDir, fileHeader, spec).compute();
    }
}
```

## Generated Code Standards

Generated code must adhere to all existing AWS SDK for Java v2 guidelines and standards. See the [Guidelines Index](README.md) for the complete list of applicable guidelines.

## Model Processing

### Intermediate Model Usage
The `IntermediateModel` provides processed service definitions:
- **ShapeModel**: Represents service data structures
- **MemberModel**: Represents structure members/fields
- **OperationModel**: Represents service operations
- **MetadataModel**: Service metadata and configuration

## Customization and Extension

### Service-Specific Customizations
- Use `CustomizationConfig` for service-specific behavior
- Document all customizations in service-specific files

### Extension Points
- Implement custom `ClassSpec` for new generators
- Extend existing generator tasks for additional functionality
- Use `PoetExtension` for model-to-class name mapping
- Leverage `NamingStrategy` for consistent naming

## Code Examples and References

### Existing ClassSpec Implementations
For practical examples of JavaPoet code generation patterns, refer to these existing implementations:

#### Model Generation
- **ShapeModelSpec**: `codegen/src/main/java/software/amazon/awssdk/codegen/poet/model/ShapeModelSpec.java`
  - Demonstrates field generation with SdkField metadata
  - Shows trait application for marshalling/unmarshalling
  - Implements static field initialization patterns

- **ModelBuilderSpecs**: `codegen/src/main/java/software/amazon/awssdk/codegen/poet/model/ModelBuilderSpecs.java`
  - Builder interface and implementation generation
  - Union type handling with enum generation
  - Fluent setter method patterns

#### Task Organization
- **AwsGeneratorTasks**: `codegen/src/main/java/software/amazon/awssdk/codegen/emitters/tasks/AwsGeneratorTasks.java`
  - Composite task pattern for organizing generation
  - Parallel execution coordination

- **PoetGeneratorTask**: `codegen/src/main/java/software/amazon/awssdk/codegen/emitters/PoetGeneratorTask.java`
  - Integration between ClassSpec and file output
  - Error handling during code generation

#### Utility Classes
- **PoetUtils**: `codegen/src/main/java/software/amazon/awssdk/codegen/poet/PoetUtils.java`
  - Common JavaPoet patterns and utilities
  - Type creation helpers and annotation management
  - JavaFile building with static imports

- **CodeGenerator**: `codegen/src/main/java/software/amazon/awssdk/codegen/CodeGenerator.java`
  - Main orchestration and validation workflow
  - Model processing and task execution patterns

### Validation and Testing Patterns

#### Fixture-Based Testing
Use static resource fixtures to validate generated code output, following the pattern in `AsyncClientClassTest`:

```java
// From AsyncClientClassTest.java
@Test
public void asyncClientClassRestJson() {
    AsyncClientClass asyncClientClass = createAsyncClientClass(restJsonServiceModels(), false);
    assertThat(asyncClientClass, generatesTo("test-json-async-client-class.java"));
}
```

**Testing Pattern Guidelines:**
- **Static Fixtures**: Store expected generated code in `src/test/resources/` as `.java` files
- **PoetMatchers**: Use `generatesTo()` matcher to compare generated code with fixtures
- **Fixture Organization**: Group fixtures by component type (client, model, etc.)
- **Multiple Scenarios**: Test different service models and configurations
- **Whitespace Normalization**: The matcher ignores whitespace differences for robust comparison

**Fixture File Structure:**
```
codegen/src/test/resources/software/amazon/awssdk/codegen/poet/client/
├── test-json-async-client-class.java          # Expected async client output
├── test-query-client-class.java               # Expected sync client output  
├── test-custompackage-async.java              # Custom package scenarios
```
