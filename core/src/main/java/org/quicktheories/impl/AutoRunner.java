package org.quicktheories.impl;

import org.quicktheories.QuickTheory;
import org.quicktheories.api.*;
import org.quicktheories.core.Configuration;
import org.quicktheories.core.Gen;
import org.quicktheories.core.Strategy;
import org.quicktheories.dsl.TheoryBuilder2;
import org.quicktheories.dsl.TheoryBuilder3;
import org.quicktheories.dsl.TheoryBuilder4;
import org.quicktheories.generators.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Runs an exploration of a method, automatically providing generators for its arguments.
 *
 * For the time being, a precondition is that the method has 1-4 parameters of specified types and is either static or
 * is defined on a class with a no-arg constructor.
 */
public class AutoRunner {

    private static final List<String> RECOGNIZED_TYPES = Arrays.asList("java.lang.String", "java.lang.Integer",
            "java.lang.Float", "java.lang.Long", "java.lang.Double", "java.lang.Boolean");

    private final String className;
    private final String methodName;
    private final List<String> argumentTypes;

    public AutoRunner(String className, String methodName, List<String> argumentTypes) {
        this.className = Objects.requireNonNull(className);
        this.methodName = Objects.requireNonNull(methodName);
        this.argumentTypes = argumentTypes;
    }

    public static AutoRunner forClassMethod(String className, String methodName) {

        try {
            Class<?> cls = Class.forName(className);
            Method[] methods = cls.getMethods();
            Optional<Method> method = Arrays.stream(methods).
                    filter(m -> m.getName().equals(methodName)).
                    filter(m -> isUsableMethod(cls, m)).findFirst();
            AutoRunner runner = method.map(m -> {
                List<String> argumentTypes = Arrays.stream(m.getParameterTypes()).map(Class::getCanonicalName).collect(Collectors.toList());
                return new AutoRunner(className, m.getName(), argumentTypes);
            }).orElse(null);
            return runner;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isUsableMethod(Class<?> cls, Method method) {
        if (method.getParameterCount() == 0 || method.getParameterCount() > 4) {
            return false;
        }
        boolean staticOrConstructable = Modifier.isStatic(method.getModifiers());
        if (!staticOrConstructable) {
            Constructor<?>[] constructors = cls.getConstructors();
            staticOrConstructable = Arrays.stream(constructors).map(Constructor::getParameterCount).anyMatch(c -> c > 0);
            if (!staticOrConstructable) {
                return false;
            }
        }
        for (Parameter parameter : method.getParameters()) {
            Class<?> pClass = parameter.getType();
            if (!RECOGNIZED_TYPES.contains(pClass.getCanonicalName())) {
                return false;
            }
        }
        return true;
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
            return run1Arg(method, state, isStatic, constructor);
        } else if (argumentClasses.size() == 2) {
            return run2Args(method, isStatic, constructor);
        } else if (argumentClasses.size() == 3) {
            return run3Args(method, isStatic, constructor);
        } else if (argumentClasses.size() == 4) {
            return run4Args(method, isStatic, constructor);
        }
        else {
            throw new IllegalArgumentException("Too many argument classes");
        }
    }

    private List<Pair<Object, Object>> run1Arg(Method method, Strategy state, boolean isStatic, Constructor<?> constructor) {
        Gen<Object> gen = mkGenerator(argumentTypes.get(0));
        TheoryRunner<Object, Object> runner = new TheoryRunner<>(state, gen, identity(), Object::toString);
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
    }

    private List<Pair<Object, Object>> run2Args(Method method, boolean isStatic, Constructor<?> constructor) {
        Gen<Object> gen1 = mkGenerator(argumentTypes.get(0));
        Gen<Object> gen2 = mkGenerator(argumentTypes.get(1));
        TheoryBuilder2<Object, Object> theoryBuilder = QuickTheory.qt().forAll(gen1, gen2);
        BiFunction<Object, Object, Object> application = (s1, s2) -> {
            try {
                if (isStatic) {
                    return method.invoke(null, s1, s2);
                }
                else {
                    return method.invoke(constructor.newInstance(), s1, s2);
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        List<Pair<Pair<Object, Object>, Object>> valuePairs = theoryBuilder.check((s1, s2) -> {
            application.apply(s1, s2);
            return true;
        }, application);
        List<Pair<Object, Object>> results = new ArrayList<>();
        for (Pair<Pair<Object, Object>, Object> result : valuePairs) {
            results.add(Pair.of(result._1, result._2));
        }
        return results;
    }

    private List<Pair<Object, Object>> run3Args(Method method, boolean isStatic, Constructor<?> constructor) {
        Gen<Object> gen1 = mkGenerator(argumentTypes.get(0));
        Gen<Object> gen2 = mkGenerator(argumentTypes.get(1));
        Gen<Object> gen3 = mkGenerator(argumentTypes.get(2));
        TheoryBuilder3<Object, Object, Object> theoryBuilder = QuickTheory.qt().forAll(gen1, gen2, gen3);
        TriFunction<Object, Object, Object, Object> application = (s1, s2, s3) -> {
            try {
                if (isStatic) {
                    return method.invoke(null, s1, s2, s3);
                }
                else {
                    return method.invoke(constructor.newInstance(), s1, s2, s3);
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        List<Pair<Tuple3<Object, Object, Object>, Object>> valuePairs = theoryBuilder.check((s1, s2, s3) -> {
            application.apply(s1, s2, s3);
            return true;
        }, application);
        List<Pair<Object, Object>> results = new ArrayList<>();
        for (Pair<Tuple3<Object, Object, Object>, Object> result : valuePairs) {
            results.add(Pair.of(result._1, result._2));
        }
        return results;
    }

    private List<Pair<Object, Object>> run4Args(Method method, boolean isStatic, Constructor<?> constructor) {
        Gen<Object> gen1 = mkGenerator(argumentTypes.get(0));
        Gen<Object> gen2 = mkGenerator(argumentTypes.get(1));
        Gen<Object> gen3 = mkGenerator(argumentTypes.get(2));
        Gen<Object> gen4 = mkGenerator(argumentTypes.get(3));
        TheoryBuilder4<Object, Object, Object, Object> theoryBuilder = QuickTheory.qt().forAll(gen1, gen2, gen3, gen4);
        QuadFunction<Object, Object, Object, Object, Object> application = (s1, s2, s3, s4) -> {
            try {
                if (isStatic) {
                    return method.invoke(null, s1, s2, s3, s4);
                }
                else {
                    return method.invoke(constructor.newInstance(), s1, s2, s3, s4);
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        List<Pair<Tuple4<Object, Object, Object, Object>, Object>> valuePairs = theoryBuilder.check((s1, s2, s3, s4) -> {
            application.apply(s1, s2, s3, s4);
            return true;
        }, application);
        List<Pair<Object, Object>> results = new ArrayList<>();
        for (Pair<Tuple4<Object, Object, Object, Object>, Object> result : valuePairs) {
            results.add(Pair.of(result._1, result._2));
        }
        return results;
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

    private Gen<Object> mkGenerator(String typeString) {
        return mkSimpleGen(typeString);
    }

    private Gen<Object> mkSimpleGen(String typeString) {
        Gen<?> gen;
        switch (typeString) {
            case "java.lang.String":
                Gen<String> longStrings = new StringsDSL().allPossible().ofLengthBetween(0, 100);
                Gen<String> shortStrings = new StringsDSL().ascii().ofLengthBetween(0, 10);
                gen = shortStrings.mix(longStrings, 10);
                break;
            case "java.lang.Integer":
                gen = new IntegersDSL().all();
                break;
            case "java.lang.Long":
                gen = new LongsDSL().all();
                break;
            case "java.lang.Boolean":
                gen = new BooleansDSL().all();
                break;
            default:
                throw new IllegalArgumentException("Unrecognized type (" + typeString + ")");
        }
        return (Gen<Object>) gen;
    }

}
