package org.quicktheories.impl;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.quicktheories.core.Gen;

class Property<T> {
  private final Predicate<T> test;
  private final Gen<T>       gen;

  Property(Predicate<T> test, Gen<T> gen) {
    this.test = test;
    this.gen = gen;
  }

  Predicate<T> getTest() {
    return test;
  }

  Gen<T> getGen() {
    return this.gen;
  }

  Optional<Falsification<T>> tryFalsification(T value) {
    try {
      if (!this.test.test(value)) {
        return Optional.of(Falsification.fromProperty(value));

      }
    } catch (final Throwable t) {
      return Optional.of(Falsification.fromException(t, value));
    }

    return Optional.empty();
  }

}
