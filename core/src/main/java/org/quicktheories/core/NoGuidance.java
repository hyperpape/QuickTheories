package org.quicktheories.core;

import org.quicktheories.impl.Precursor;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class NoGuidance implements Guidance {

  @Override
  public void exampleExecuted() {    
  }

  @Override
  public Collection<long[]> suggestValues(int i, Precursor t) {
    return Collections.emptyList();
  }

  @Override
  public void exampleComplete() {
  }

  @Override
  public Optional<Map<Collection<Long>, Precursor>> getGuidanceRelevantPrecursors() {
    return Optional.empty();
  }

  @Override
  public void newExample(Precursor precursor) {
 
  }

  @Override
  public boolean matches(Collection<Long> coverage) {
    return false;
  }

}
