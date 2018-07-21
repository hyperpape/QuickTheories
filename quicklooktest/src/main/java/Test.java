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
        Function<String, String> fn = s -> test.testFn(s);
        List<String> values = explore(fn, SourceDSL.strings().ascii().ofLengthBetween(0, 32));
        values.forEach(System.out::println);
    }

    /**
     * Explores the values of a given function
     * @param f the function to explore
     * @param generator a generator
     * @param <S> the
     * @param <T>
     * @return
     */
    public static <S, T> List<T> explore(Function<S,T> f, Gen<S> generator) {
        Strategy state = Configuration.systemStrategy();
        Function<S, S> _ = Function.identity();
        TheoryRunner<S, S> runner = new TheoryRunner<>(state, generator, Function.identity(), S::toString);
        return runner.check((s) -> truth(f.apply(s)), f);
    }

    static <T> boolean truth(T t) {
        return true;
    }

    public static String testFn(String in) {
        if (null == in) {
            return in;
        }
        else if (in.contains("a")) {
            return in.substring(0, in.length() - "a".length());
        }
        return in;
    }

}
