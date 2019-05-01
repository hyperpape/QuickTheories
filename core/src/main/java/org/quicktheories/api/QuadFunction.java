package org.quicktheories.api;

@FunctionalInterface
public interface QuadFunction<A, B, C, D, E> {

    /**
     * Applies this function to the given arguments.
     *
     * @param a the first function argument
     * @param b the second function argument
     * @param c the third function argument
     * @param d the fourth function argument
     * @return the function result
     */
    E apply(A a, B b, C c, D d);
}
