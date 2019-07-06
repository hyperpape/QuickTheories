package org.quicktheories.impl;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AutoRunnerTest {

    @Test
    public void testRecognizesStaticMethods() throws Exception {
        AutoRunner runner = AutoRunner.forClassMethod("org.quicktheories.impl.NoArgConstructorClass", "oneArgStatic");
        assertNotNull(runner);
        assertNotNull(runner.run());

        runner = AutoRunner.forClassMethod("org.quicktheories.impl.NoArgConstructorClass", "twoArgsStatic");
        assertNotNull(runner);
        assertNotNull(runner.run());

        runner = AutoRunner.forClassMethod("org.quicktheories.impl.NoArgConstructorClass", "threeArgsStatic");
        assertNotNull(runner);
        assertNotNull(runner.run());

        runner = AutoRunner.forClassMethod("org.quicktheories.impl.NoArgConstructorClass", "fourArgsStatic");
        assertNotNull(runner);
        assertNotNull(runner.run());

        runner = AutoRunner.forClassMethod("org.quicktheories.impl.MultiArgConstructorClass", "twoArgsStatic");
        assertNotNull(runner);
        assertNotNull(runner.run());
    }

    @Test
    public void testIgnoresStaticMethodsWithIncorrectNumbersOfParameters() {
        AutoRunner runner = AutoRunner.forClassMethod("org.quicktheories.impl.NoArgConstructorClass", "noArgsStatic");
        assertNull(runner);

        runner = AutoRunner.forClassMethod("org.quicktheories.impl.NoArgConstructorClass", "fiveArgsStatic");
        assertNull(runner);
    }


    @Test
    public void testIgnoresMethodsWithIncorrectNumbersOfParameters() {
        AutoRunner runner = AutoRunner.forClassMethod("org.quicktheories.impl.NoArgConstructorClass", "noArgs");
        assertNull(runner);

        runner = AutoRunner.forClassMethod("org.quicktheories.impl.NoArgConstructorClass", "fiveArgsStatic");
        assertNull(runner);
    }


    @Test
    public void testIgnoresNonExistentMethods() {
        AutoRunner runner = AutoRunner.forClassMethod("org.quicktheories.impl.NoArgConstructorClass", "NOTHING");
        assertNull(runner);

        runner = AutoRunner.forClassMethod("org.quicktheories.impl.NoArgConstructorClass", "fiveArgs");
        assertNull(runner);
    }

    @Test
    public void testRecognizesMethodsOnClassWithNoArgConstructor() throws Exception {
        AutoRunner runner = AutoRunner.forClassMethod("org.quicktheories.impl.NoArgConstructorClass", "oneArg");
        assertNotNull(runner);
        assertNotNull(runner.run());

        runner = AutoRunner.forClassMethod("org.quicktheories.impl.NoArgConstructorClass", "fourArgs");
        assertNotNull(runner);
        assertNotNull(runner.run());
    }

    @Test
    public void testIgnoresMethodsOnClassWithMultiArgConstructor() throws Exception {
        AutoRunner runner = AutoRunner.forClassMethod("org.quicktheories.impl.MultiArgConstructorClass", "twoArgs");
        assertNull(runner);
    }
}
