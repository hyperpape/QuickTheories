package org.quicktheories.coverage;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.ByteBuddyAgent.ProcessProvider;
import org.quicktheories.core.Guidance;
import org.quicktheories.core.PseudoRandom;
import org.quicktheories.impl.Precursor;
import sun.quicktheories.coverage.CodeCoverageStore;

import java.util.*;

public class CoverageGuidance implements Guidance {
  private static final int UNGUIDED_EXECUTIONS = 0;
    
  static {
    Installer in = new Installer(new ClassloaderByteArraySource(Thread.currentThread().getContextClassLoader()));
    ByteBuddyAgent.attach(in.createJar(), ProcessProvider.ForCurrentVm.INSTANCE);
  }

  private final Map<Collection<Long>, Precursor> coverageIncreasingPrecursors = new HashMap<>(); // TODO: per method?
  private final PseudoRandom prng; 
  private final Set<Long> visitedBranches = new HashSet<Long>();
  
  private Collection<Long> currentHits;
  
  CoverageGuidance(PseudoRandom prng) {
    this.prng = prng;
  }

  @Override
  public void newExample(Precursor newExample) {
    CodeCoverageStore.reset(); 
  }

  @Override
  public void exampleExecuted() {
    currentHits = CodeCoverageStore.getHits();  
  }

  @Override
  public Collection<long[]> suggestValues(int execution, Precursor precursor) {
    if (execution < UNGUIDED_EXECUTIONS) {
      return Collections.emptyList();
    }

    if (!visitedBranches.containsAll(currentHits)) {
      coverageIncreasingPrecursors.put(currentHits, precursor);
      List<long[]> nearBy = new ArrayList<long[]>();
      for (int i = 0; i != 20; i++) {
        nearBy.add(valueNear(precursor));
      }
      return nearBy;
    }
    return Collections.emptyList();
  }

  @Override
  public void exampleComplete() {
    visitedBranches.addAll(currentHits);
    currentHits = null;
  }

  private <T> long[] valueNear(Precursor t) {   
    long[] ls= t.current();
    int index = prng.nextInt(0, ls.length -1);
    ls[index] = prng.nextLong(t.min(index), t.max(index));
    return ls;
  }

  @Override
  public Optional<Map<Collection<Long>, Precursor>> getGuidanceRelevantPrecursors() {
    return Optional.of(coverageIncreasingPrecursors);
  }

  @Override
  public boolean matches(Collection<Long> coverage) {
    return currentHits.equals(coverage);
  }
}
