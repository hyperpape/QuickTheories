import org.quicktheories.api.Pair;
import org.quicktheories.core.Configuration;
import org.quicktheories.core.Gen;
import org.quicktheories.core.Strategy;
import org.quicktheories.generators.SourceDSL;
import org.quicktheories.impl.TheoryRunner;

import java.util.List;
import java.util.function.Function;

public class Test {

    public static void main(String[] args) {
        Test test = new Test();
        Function<String, String> fn = s -> String.valueOf(test.incorrectIsEmailValid(s));
        List<Pair<String, String>> values = explore(fn, SourceDSL.strings().basicLatinAlphabet().ofLengthBetween(0, 32));
        values.forEach(System.out::println);
    }

    /**
     * Explores the values of a given function
     * @param f the function to explore
     * @param generator a generator
     * @param <S> the
     * @return
     */
    public static <S> List<Pair<S, S>> explore(Function<S,S> f, Gen<S> generator) {
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
            if (s.charAt(i) < '.' || s.charAt(i) > 'z') {
                return false;
            }
        }
        return true;
    }

}
