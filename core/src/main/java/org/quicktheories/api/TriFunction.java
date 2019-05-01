package org.quicktheories.api;

@FunctionalInterface
public interface TriFunction<A, B, C, D> {

    /**
     * Applies this function to the given arguments.
     *
     * @param a the first function argument
     * @param b the second function argument
     * @param c the third function argument
     * @return the function result
     */
    D apply(A a, B b, C c);
}
