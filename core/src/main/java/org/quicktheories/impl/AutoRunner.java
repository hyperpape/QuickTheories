package org.quicktheories.impl;

import org.quicktheories.api.Pair;
import org.quicktheories.core.Configuration;
import org.quicktheories.core.Gen;
import org.quicktheories.core.Strategy;
import org.quicktheories.generators.BooleansDSL;
import org.quicktheories.generators.IntegersDSL;
import org.quicktheories.generators.LongsDSL;
import org.quicktheories.generators.StringsDSL;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Runs an exploration of a method, automatically providing generators for its arguments.
 *
 * A precondition is that the method is either static or is defined on a class with a no-arg constructor.
 */
public class AutoRunner {

    private final String className;
    private final String methodName;
    private final List<String> argumentTypes;

    public AutoRunner(String className, String methodName, List<String> argumentTypes) {
        this.className = Objects.requireNonNull(className);
        this.methodName = Objects.requireNonNull(methodName);
        this.argumentTypes = argumentTypes;
    }

    public List<Pair<Object, Object>> run() throws Exception {
        Class<?> targetClass = Class.forName(className);
        List<Class<?>> argumentClasses = new ArrayList<>();
        for (String type : argumentTypes) {
            argumentClasses.add(Class.forName(type));
        }
        Method method = targetClass.getDeclaredMethod(methodName, argumentClasses.toArray(new Class[0]));
        method.setAccessible(true);
        Strategy state = Configuration.systemStrategy();
        final boolean isStatic = Modifier.isStatic(method.getModifiers());
        final Constructor<?> constructor = getConstructor(targetClass);
        if (!isStatic && constructor == null) {
            throw new IllegalArgumentException("Tried to run non-static method on class lacking no-arg constructor");
        }

        if (argumentTypes.size() == 1) {
            Gen<Object> gen = (Gen<Object>) mkGenerator(argumentTypes.get(0));
            TheoryRunner<Object, Object> runner = new TheoryRunner<Object, Object>(state, gen, identity(), Object::toString);
            Function<Object, Object> application = (s) -> {
                try {
                    if (isStatic) {
                        return method.invoke(null, s);
                    }
                    else {
                        return method.invoke(constructor.newInstance(), s);
                    }
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
            return runner.check((s) -> {
                application.apply(s);
                return true;
            }, application);
//        } else if (argumentClasses.size() == 2) {
//            TheoryBuilder2<?, ?> theoryBuilder = QuickTheory.qt().forAll(mkGenerator(argumentTypes.get(0)), mkGenerator(argumentTypes.get(1)));
//        } else if (argumentClasses.size() == 3) {
//            Gen<?> gen1 = mkGenerator(argumentTypes.get(0));
//            Gen<?> gen2 = mkGenerator(argumentTypes.get(1));
//            Gen<?> gen3 = mkGenerator(argumentTypes.get(2));
//            TheoryBuilder3<?, ?, ?> theoryBuilder = QuickTheory.qt().forAll(gen1, gen2, gen3);
//        } else if (argumentClasses.size() == 4) {
//            Gen<?> gen1 = mkGenerator(argumentTypes.get(0));
//            Gen<?> gen2 = mkGenerator(argumentTypes.get(1));
//            Gen<?> gen3 = mkGenerator(argumentTypes.get(2));
//            Gen<?> gen4 = mkGenerator(argumentTypes.get(3));
//            TheoryBuilder4<?, ?, ?, ?> theoryBuilder = QuickTheory.qt().forAll(gen1, gen2, gen3, gen4);
        } else {
            throw new IllegalArgumentException("Too many argument classes");
        }
    }

    private <T> Function<T,T> identity() {
        return (t) -> t;
    }

    private Constructor<?> getConstructor(Class<?> targetClass) {
        Constructor[] constructors = targetClass.getDeclaredConstructors();
        for (Constructor constructor : constructors) {
            if (constructor.getParameterCount() == 0) {
                return constructor;
            }
        }
        return null;
    }

    private Gen<?> mkGenerator(String typeString) {
        return mkSimpleGen(typeString);
    }

    private Gen<?> mkSimpleGen(String typeString) {
        switch (typeString) {
            case "java.lang.String":
                Gen<String> longStrings = new StringsDSL().allPossible().ofLengthBetween(0, 100);
                Gen<String> shortStrings = new StringsDSL().ascii().ofLengthBetween(0, 10);
                return shortStrings.mix(longStrings, 10);
            case "java.lang.Integer":
                return new IntegersDSL().all();
            case "java.lang.Long":
                return new LongsDSL().all();
            case "java.lang.Boolean":
                return new BooleansDSL().all();
            default:
                throw new IllegalArgumentException("Unrecognized type (" + typeString + ")");
        }
    }

}
