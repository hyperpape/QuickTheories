package org.quicktheories.core;

import org.quicktheories.impl.Precursor;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Provides hints for values to visit during search.
 */
public interface Guidance {

  void newExample(Precursor precursor);

  void exampleExecuted();

  Collection<long[]> suggestValues(int i, Precursor t);

  void exampleComplete();

  Optional<Map<Collection<Long>, Precursor>> getGuidanceRelevantPrecursors();

  boolean matches(Collection<Long> coverage);
}
