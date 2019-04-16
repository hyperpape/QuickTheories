package com.justinblank.quicklooktest;

import org.quicktheories.api.Pair;
import org.quicktheories.core.Configuration;
import org.quicktheories.core.Gen;
import org.quicktheories.core.Strategy;
import org.quicktheories.generators.SourceDSL;
import org.quicktheories.impl.AutoRunner;
import org.quicktheories.impl.TheoryRunner;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class Test {

    public static void main(String[] args) {
        try {
            testExploration();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }

    private static void testAutoRunner() throws Exception {
        AutoRunner runner = new AutoRunner("org.quicktheories.Test", "incorrectIsEmailValid", Arrays.asList("java.lang.String"));
        runner.run().forEach(value -> {
            String msg = "\"" + value.first() + "\" -> " + value.second();
            System.out.println(msg);
        });
    }

    private static void testExploration() {
        Function<String, Boolean> fn = Test::incorrectIsEmailValid;
        List<Pair<String, Boolean>> values = explore(fn, SourceDSL.strings().basicLatinAlphabet().ofLengthBetween(0, 32));
        values.forEach(value -> {
            String msg = "\"" + value.first() + "\" -> " + value.second();
            System.out.println(msg);
        });
    }

    /**
     * Explores the values of a given function
     * @param f the function to explore
     * @param generator a generator
     * @param <S> the
     * @return
     */
    public static <S, T> List<Pair<S, T>> explore(Function<S, T> f, Gen<S> generator) {
        Strategy state = Configuration.systemStrategy();
        Function<S, S> identity = Function.identity();
        TheoryRunner<S, S> runner = new TheoryRunner<>(state, generator, identity, S::toString);
        return runner.check((S s) -> truth(f.apply(s)), f);
    }

    static <T> boolean truth(T t) {
        return true;
    }
    /**
     * Does a very bad job of validating emails, but has a lot of branches!
     *
     * @param email
     * @return is this email correct?
     */
    public static boolean incorrectIsEmailValid(String email) {
        if (null == email) {
            return false;
        }
        int index = email.indexOf('@');
        if (index == -1) {
            return false;
        }
        else if (index == email.length() - 1) {
            return false;
        }
        String name = email.substring(0, index);
        if (name.length() == 0) {
            return false;
        }
        String domain = email.substring(index + 1);
        if (domain.length() == 0) {
            return false;
        }
        if (!domain.contains(".")) {
            return false;
        }
        return allValidChars(domain) && allValidChars(name);
    }

    private static boolean allValidChars(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 'A' || c > 'z') {
                if (c != '.') {
                    return false;
                }
            }
        }
        return true;
    }
}
