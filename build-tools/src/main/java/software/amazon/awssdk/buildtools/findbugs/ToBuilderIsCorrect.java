/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.buildtools.findbugs;

import static java.util.Collections.emptyList;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

/**
 * A FindBugs/SpotBugs rule for verifying that implementations of
 * software.amazon.awssdk.utils.builder.ToCopyableBuilder and CopyableBuilder are correct.
 */
public class ToBuilderIsCorrect extends OpcodeStackDetector {
    private final BugReporter bugReporter;

    /**
     * All implementations of CopyableBuilder encountered in the module.
     */
    private final Map<String, JavaClass> builders = new HashMap<>();

    /**
     * All implementations of ToCopyableBuilder encountered in the module.
     */
    private final Map<String, JavaClass> buildables = new HashMap<>();

    /**
     * The fields in CopyableBuilder implementations that are expected to be copied.
     *
     * Keys: The builder FQCN, Values: the fields in the builder
     */
    private final Map<String, List<String>> builderFields = new HashMap<>();

    /**
     * The fields in CopyableBuilder implementations that are not expected to be copied. These are configured on the toBuilder
     * method using the @ToBuilderIgnoreField annotation.
     *
     * Keys: The builder FQCN, Values: the fields to ignore
     */
    private final Map<String, List<String>> ignoredFields = new HashMap<>();

    /**
     * The implementations of CopyableBuilder that are not validated. These are determined automatically, e.g. because the
     * class inherits its toBuilder implementation from a parent class.
     */
    private final Set<String> ignoredBuildables = new HashSet<>();

    /**
     * Which fields are modified in a builder's constructor. This is used when a buildable uses a builder's constructor to copy
     * over fields.
     *
     * Key 1: The builder FQCN, Key 2: The constructor signature, Values: the fields modified in the constructor
     */
    private final Map<String, Map<String, List<String>>> builderConstructorModifiedFields = new HashMap<>();

    /**
     * Which fields are modified in a buildable's toBuilder method. This is used when a buildable invokes methods on the
     * builder to copy over fields.
     *
     * Key 1: The buildable FQCN, Key 2: the invoked builder FQCN, Values: the fields modified in the
     * toBuilder
     */
    private final Map<String, Map<String, List<String>>> toBuilderModifiedFields = new HashMap<>();

    /**
     * Which constructors are invoked from a buildable's toBuilder method. This is used when a buildable uses a builder's
     * constructor to copy over fields.
     *
     * Key 1: The buildable FQCN, Key 2: the invoked builder FQCN, Values: the signature of the constructor invoked by the
     * toBuilder method.
     */
    private final Map<String, Map<String, String>> constructorsInvokedFromToBuilder = new HashMap<>();

    /**
     * Which builder constructors reference which other builder constructors. This is used when a buildable uses a builder's
     * constructor to copy over fields, and that constructor delegates some copying to another internal constructor.
     *
     * Key 1: The builder FQCN, Key 2: the invoking constructor signature, Value: the target constructor signature.
     */
    private final Map<String, Map<String, String>> crossConstructorReferences = new HashMap<>();

    /**
     * Which classes or interfaces a builder extends. This is used to determine what builder is being modified when it is
     * invoked virtually from a toBuilder implementation.
     *
     * Key: The builder FQCN, Value: the FQCN parent classes and interfaces (including transitive)
     */
    private final Map<String, Set<String>> builderParents = new HashMap<>();

    /**
     * Whether the current class being assessed is a builder.
     */
    private boolean isBuilder = false;

    /**
     * Whether the current class being assessed is a buildable.
     */
    private boolean isBuildable = false;

    /**
     * Whether the current method being assessed is a builder's constructor.
     */
    private boolean inBuilderConstructor = false;

    /**
     * Whether the current method being assessed is a buildable's toBuilder.
     */
    private boolean inBuildableToBuilder = false;

    public ToBuilderIsCorrect(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    /**
     * Determine whether we should visit the provided class. We only visit builders and buildables.
     */
    @Override
    public boolean shouldVisit(JavaClass obj) {
        isBuilder = false;
        isBuildable = false;
        inBuilderConstructor = false;
        inBuildableToBuilder = false;

        try {
            if (obj.isInterface()) {
                return false;
            }

            JavaClass[] parentInterfaces = obj.getAllInterfaces();
            Set<String> parents = Stream.concat(Arrays.stream(parentInterfaces), Arrays.stream(obj.getSuperClasses()))
                                        .map(JavaClass::getClassName)
                                        .collect(Collectors.toSet());
            for (JavaClass i : parentInterfaces) {
                if (i.getClassName().equals("software.amazon.awssdk.utils.builder.CopyableBuilder")) {
                    builders.put(obj.getClassName(), obj);
                    builderParents.put(obj.getClassName(), parents);
                    isBuilder = true;
                }

                if (i.getClassName().equals("software.amazon.awssdk.utils.builder.ToCopyableBuilder")) {
                    buildables.put(obj.getClassName(), obj);
                    isBuildable = true;
                }
            }

            if (isBuildable && isBuilder) {
                System.err.println(obj.getClassName() + " implements both CopyableBuilder and ToCopyableBuilder.");
                bugReporter.reportBug(new BugInstance(this, "BAD_TO_BUILDER", NORMAL_PRIORITY)
                                          .addClass(obj));
                visitAfter(obj);
                return false;
            }

            return isBuildable || isBuilder;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(Field obj) {
        if (isBuilder) {
            builderFields.computeIfAbsent(getDottedClassName(), c -> new ArrayList<>()).add(obj.getName());
        }
    }

    @Override
    public void visit(Method method) {
        if (isBuilder && method.getName().equals(Const.CONSTRUCTOR_NAME)) {
            // This is a builder constructor
            builderConstructorModifiedFields.computeIfAbsent(getDottedClassName(), n1 -> new HashMap<>())
                                            .computeIfAbsent(method.getSignature(), n -> new ArrayList<>());
            crossConstructorReferences.computeIfAbsent(getDottedClassName(), n1 -> new HashMap<>());
            inBuildableToBuilder = false;
            inBuilderConstructor = true;

            if (method.isPublic()) {
                System.err.println(getDottedClassName() + getMethodSig() + " must not be public.");
                bugReporter.reportBug(new BugInstance(this, "BAD_TO_BUILDER", NORMAL_PRIORITY)
                                          .addClassAndMethod(getXMethod()));
            }
        } else if (isBuildable && method.getName().equals("toBuilder") && method.getSignature().startsWith("()")) {
            // This is a buildable toBuilder
            constructorsInvokedFromToBuilder.computeIfAbsent(getDottedClassName(), n -> new HashMap<>());
            toBuilderModifiedFields.computeIfAbsent(getDottedClassName(), n -> new HashMap<>());
            inBuildableToBuilder = true;
            inBuilderConstructor = false;

            registerIgnoredFields();
        } else {
            inBuildableToBuilder = false;
            inBuilderConstructor = false;
        }
    }

    /**
     * Register ignored fields specified in the current method's ToBuilderIgnoreField annotation.
     */
    private void registerIgnoredFields() {
        for (AnnotationValue annotation : getXMethod().getAnnotations()) {
            if (annotation.getAnnotationClass()
                          .getDottedClassName()
                          .equals("software.amazon.awssdk.annotations.ToBuilderIgnoreField")) {
                Object value = annotation.getValue("value");
                for (Object ignoredField : (Object[]) value) {
                    ignoredFields.computeIfAbsent(getDottedClassName(), n -> new ArrayList<>())
                                 .add((String) ignoredField);
                }
            }
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (inBuilderConstructor) {
            sawOpCodeInBuilderConstructor(seen);
        } else if (inBuildableToBuilder) {
            sawOpCodeInBuildableToBuilder(seen);
        }
    }

    private void sawOpCodeInBuilderConstructor(int seen) {
        // In builder constructor
        if (seen == Const.PUTFIELD && !isEmptyAllocation()) {
            // Writing directly to a field, but not with an empty value that was just constructed (likely empty maps, lists,
            // etc.)
            addConstructorModifiedField(getXFieldOperand().getName());
        } else if (seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE) {
            XField field = getStack().getStackItem(1).getXField();
            if (field != null && field.getClassName().equals(getDottedClassName())) {
                // We're invoking a method on one of our fields. We just assume that counts as updating it (e.g. Map.putAll).
                addConstructorModifiedField(field.getName());
            }
        } else if (seen == Const.INVOKESPECIAL &&
                   getDottedClassName().equals(getDottedClassConstantOperand()) &&
                   getNameConstantOperand().equals(Const.CONSTRUCTOR_NAME)) {
            // Invoking another constructor on this builder
            crossConstructorReferences.get(getDottedClassName())
                                      .put(getXMethod().getSignature(), getSigConstantOperand());
        }
    }

    /**
     * Return true if the value on the top of the stack is a newly-allocated object, created from a zero-arg method. If so, we
     * assume it's just allocating something empty (e.g. new HashMap(), new ArrayList(), etc.)
     */
    private boolean isEmptyAllocation() {
        OpcodeStack.Item topOfStack = getStack().getStackItem(0);
        XMethod returnValueOf = topOfStack.getReturnValueOf();
        if (returnValueOf == null) {
            return false;
        }

        return topOfStack.isNewlyAllocated() && returnValueOf.getSignature().startsWith("()");
    }

    private void addConstructorModifiedField(String field) {
        builderConstructorModifiedFields.get(getDottedClassName())
                                        .get(getXMethod().getSignature())
                                        .add(field);
    }

    private void sawOpCodeInBuildableToBuilder(int seen) {
        // In toBuilder method
        if (seen == Const.INVOKESPECIAL && getNameConstantOperand().equals(Const.CONSTRUCTOR_NAME)) {
            // Invoking a constructor, possibly on a builder
            constructorsInvokedFromToBuilder.get(getDottedClassName())
                                            .put(getDottedClassConstantOperand(), getSigConstantOperand());
        } else if (seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE) {
            // Invoking a method, possibly on a builder
            toBuilderModifiedFields.get(getDottedClassName())
                                   .computeIfAbsent(getDottedClassConstantOperand(), n -> new ArrayList<>())
                                   .add(getNameConstantOperand());
        } else if (seen == Const.INVOKESPECIAL && getNameConstantOperand().equals("toBuilder")) {
            // Invoking another toBuilder. We make an assumption that it's doing the work instead of this buildable.
            ignoredBuildables.add(getDottedClassName());
        }
    }

    @Override
    public void report() {
        buildables.forEach((buildableClassName, buildableClass) -> {
            // Find how toBuilder invokes the builder
            if (ignoredBuildables.contains(buildableClassName)) {
                // Skip validating this buildable because it delegates its toBuilder call to another buildable.
            } else if (invokesBuilderViaMethods(buildableClassName)) {
                validateBuilderViaMethods(buildableClassName, buildableClass);
            } else if (invokesBuilderViaConstructor(buildableClassName)) {
                validateBuilderConstructor(buildableClassName, buildableClass);
            } else {
                System.err.println(buildableClassName + ".toBuilder() does not invoke a constructor or method on a builder. A "
                                   + "toBuilder() method must either invoke a builder constructor or call builder() and "
                                   + "configure the fields on the builder.");
                bugReporter.reportBug(new BugInstance(this, "BAD_TO_BUILDER", NORMAL_PRIORITY)
                                          .addClass(buildableClass));
            }
        });
    }

    private boolean invokesBuilderViaConstructor(String buildableClassName) {
        Map<String, String> constructors = constructorsInvokedFromToBuilder.get(buildableClassName);
        if (constructors == null) {
            return false;
        }

        return constructors.keySet().stream().anyMatch(builders::containsKey);
    }

    private boolean invokesBuilderViaMethods(String buildableClassName) {
        Map<String, List<String>> methods = toBuilderModifiedFields.get(buildableClassName);
        if (methods == null) {
            return false;
        }

        return methods.keySet()
                      .stream()
                      .anyMatch(builderClass -> builders.containsKey(builderClass) ||
                                                builderParents.values()
                                                              .stream()
                                                              .anyMatch(parents -> parents.contains(builderClass)));
    }

    private void validateBuilderConstructor(String buildableClassName, JavaClass buildableClass) {
        Map<String, String> invokedConstructors = constructorsInvokedFromToBuilder.get(buildableClassName);

        // Remove and ignore any non-builder constructors the buildable invoked.
        invokedConstructors.keySet().removeIf(builderClass -> !builders.containsKey(builderClass));
        if (invokedConstructors.size() > 1) {
            System.err.println(buildableClassName + ".toBuilder() invokes multiple builder constructors: " + invokedConstructors);
            bugReporter.reportBug(new BugInstance(this, "BAD_TO_BUILDER", NORMAL_PRIORITY)
                                      .addClass(buildableClass));
        }

        // Find the constructor implementation that we actually invoked, along with all of the constructors that were
        // transitively called
        Map.Entry<String, String> invokedConstructor = invokedConstructors.entrySet().iterator().next();
        String builder = invokedConstructor.getKey();
        String calledConstructor = invokedConstructor.getValue();
        List<String> allInvokedConstructors = getAllInvokedConstructors(builder, calledConstructor);

        if (!builders.containsKey(builder)) {
            System.err.println(buildableClassName + ".toBuilder() invokes the constructor of an unknown type: " + builder);
            bugReporter.reportBug(new BugInstance(this, "BAD_TO_BUILDER", NORMAL_PRIORITY)
                                      .addClass(buildableClass));
        }

        // Find which fields we modified in those constructors
        Set<String> builderFieldsForBuilder = new HashSet<>(builderFields.get(builder));
        Map<String, List<String>> allConstructors = builderConstructorModifiedFields.get(builder);

        allInvokedConstructors.forEach(constructorSignature -> {
            List<String> modifiedFields = allConstructors.get(constructorSignature);

            if (modifiedFields == null) {
                System.err.println(buildableClassName + ".toBuilder() invokes an unknown constructor: " + builder +
                                   constructorSignature);
                bugReporter.reportBug(new BugInstance(this, "BAD_TO_BUILDER", NORMAL_PRIORITY)
                                          .addClass(builders.get(builder)));
                return;
            }

            modifiedFields.forEach(builderFieldsForBuilder::remove);
        });

        // Subtract ignored fields
        ignoredFields.getOrDefault(buildableClassName, emptyList()).forEach(builderFieldsForBuilder::remove);

        // Anything left is unmodified
        if (!builderFieldsForBuilder.isEmpty()) {
            System.err.println(buildableClassName + ".toBuilder() does not update all of the builder's fields. "
                               + "Missing fields: " + builderFieldsForBuilder + ". This check does not currently "
                               + "consider transitive method calls. If this is a false positive, you can ignore this field by "
                               + "annotating the toBuilder method with @ToBuilderIgnoreField and specifying the fields to "
                               + "ignore.");
            bugReporter.reportBug(new BugInstance(this, "BAD_TO_BUILDER", NORMAL_PRIORITY)
                                      .addClass(builders.get(builder)));
        }
    }

    private List<String> getAllInvokedConstructors(String builder, String signature) {
        List<String> result = new ArrayList<>();

        while (signature != null) {
            result.add(signature);
            signature = crossConstructorReferences.get(builder).get(signature);
        }

        return result;
    }

    private void validateBuilderViaMethods(String buildableClassName, JavaClass buildableClass) {
        // Find which methods were invoked in the builder
        Map<String, List<String>> invokedMethodsByClass = toBuilderModifiedFields.get(buildableClassName);
        invokedMethodsByClass.forEach((invokedClass, invokedMethods) -> {

            // Create a best-guess on what buildable implementation we're actually working with (based on the parent interface
            // we might be working with)
            String concreteClass = resolveParentToConcrete(invokedClass);
            if (builders.containsKey(concreteClass)) {
                // We're invoking these methods on a known builder. Assume the method name matches the field name and validate
                // based on that.
                Set<String> builderFieldsForBuilder = new HashSet<>(builderFields.get(concreteClass));
                invokedMethods.forEach(builderFieldsForBuilder::remove);
                ignoredFields.getOrDefault(buildableClassName, emptyList()).forEach(builderFieldsForBuilder::remove);

                if (!builderFieldsForBuilder.isEmpty()) {
                    System.err.println(buildableClassName + ".toBuilder() does not update all of the builder's fields. "
                                       + "Missing fields: " + builderFieldsForBuilder + ". This check does not currently "
                                       + "consider transitive method calls.");
                    bugReporter.reportBug(new BugInstance(this, "BAD_TO_BUILDER", NORMAL_PRIORITY)
                                              .addClass(buildableClass));
                }
            }
        });
    }

    private String resolveParentToConcrete(String parent) {
        return builderParents.entrySet()
                             .stream()
                             .filter(parents -> parents.getValue().contains(parent))
                             .map(parents -> parents.getKey())
                             .findAny()
                             .orElse(parent);
    }
}
