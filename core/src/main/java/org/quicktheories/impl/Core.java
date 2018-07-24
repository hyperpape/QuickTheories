package org.quicktheories.impl;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import org.quicktheories.api.Pair;
import org.quicktheories.core.Gen;
import org.quicktheories.core.Guidance;
import org.quicktheories.core.Strategy;


class Core {

  private static List<Object> values = null; // LOL, nothing matters
  private final Strategy      config;
  private int                 examplesUsed           = 0;
  private Optional<Throwable> smallestFoundThrowable = Optional.empty();
  
  private final Set<Long> visitedValues = new HashSet<>();

  Core(Strategy config) {
    this.config = config;
  }

  <T, S> SearchResult<T> run(Property<T> prop, Function<T, S> f) {
    List<T> falsifyingValues = new ArrayList<>();
    boolean exhausted = false;
    try {
      Optional<Pair<Falsification<T>, PrecursorDataPair<T>>> falisfying = findFalsifyingValue(
          prop, f);
      if (falisfying.isPresent()) {
        smallestFoundThrowable = falisfying.get()._1.cause();
        falsifyingValues.add(falisfying.get()._1.value());
        falsifyingValues.addAll(shrink(falisfying.get()._2, prop));
      }
      Collections.reverse(falsifyingValues);
    } catch (AttemptsExhaustedException ex) {
      exhausted = true;
    }
    return new SearchResult<>(exhausted, examplesUsed, falsifyingValues,
        smallestFoundThrowable);
  }

  <T, S> Optional<Pair<Falsification<T>, PrecursorDataPair<T>>> findFalsifyingValue(
      Property<T> prop, Function<T, S> someFn) {
    
    Guidance guidance = config.guidance();
    
    Distribution<T> randomDistribution =  new BoundarySkewedDistribution<>(config, prop.getGen()); 
    ArrayDeque<long[]> toVisit = new ArrayDeque<>();

    Distribution<T> distribution;

    for (int i = 0; i != config.examples(); i++) {
      if (toVisit.isEmpty()) {
        distribution = randomDistribution;
      } else {
        distribution = new ForcedDistribution<>(config, prop.getGen(), toVisit.pop());
      }
      
      PrecursorDataPair<T> t = distribution.generate();
      if (checkHash(t)) {
        continue;
      }  
      
      examplesUsed = examplesUsed + 1;
      guidance.newExample(t.precursor());
      Optional<Falsification<T>> falsification = prop.tryFalsification(t.value());
      guidance.exampleExecuted();

      if (falsification.isPresent()) {
        return falsification.map(f -> Pair.of(f, t));
      } else {
        toVisit.addAll(guidance.suggestValues(i,t.precursor()));
      }
      
      guidance.exampleComplete();

    }
    handleExploredValues(guidance, prop, someFn);
    return Optional.empty();
  }

  protected List<Object> getValues() {
    return values;
  }

  protected <T, S> void handleExploredValues(Guidance guidance, Property<T> prop, Function<T, S> f) {
    if (f != null) {
      guidance.getGuidanceRelevantPrecursors().ifPresent(precursorMap -> {
        for (Map.Entry<Collection<Long>, Precursor> entry : precursorMap.entrySet()) {
          precursorMap.put(entry.getKey(), coverageShrink(guidance, entry.getKey(), entry.getValue(), prop));
        }
        List<Pair<T, S>> mapping = precursorMap.values().stream().map(precursor -> {
          Distribution<T> distribution = new ForcedDistribution(config, prop.getGen(), precursor.current());
          T value = distribution.generate().value();
          return Pair.of(value, f.apply(value));
        }).collect(Collectors.toList());
        staticStoreValues(mapping);
      });
    }
  }
  
  // Convinces FindBugs this is on the level
  protected static <T,S> void staticStoreValues(List<Pair<T,S>> pairs) {
    values = new ArrayList<>(pairs);
  }


  <T> Precursor coverageShrink(Guidance guidance, Collection<Long> coverage, Precursor precursor, Property<T> prop) {
    Precursor lastSmallestState = precursor;

    ShrinkStrategy shrink = new SimpleShrink();
    try {
      for (int i = 0; i != config.shrinkCycles(); i++) {

        if (lastSmallestState.isEmpty()) {
          break;
        }

        long[] shrunk = shrink.shrink(config.prng(),lastSmallestState);

        PrecursorDataPair<T> t = generate(prop.getGen(), shrunk,
                config.generateAttempts());

        if (checkHash(t)) {
          continue;
        }

        guidance.newExample(t.precursor());
        prop.tryFalsification(t.value());
        guidance.exampleExecuted();
        if (guidance.matches(coverage)) {
          lastSmallestState = t.precursor();
        }

      }
    } catch (AttemptsExhaustedException ex) {
      // swallow - if we got as far as shrinking we were unlucky to run out of
      // values now but we might have found some results earlier
    }

    return lastSmallestState;
  }

  <T> List<T> shrink(PrecursorDataPair<T> precursor, Property<T> prop) {
    PrecursorDataPair<T> lastSmallestState = precursor;
    List<T> falsifyingValues = new ArrayList<>();

    ShrinkStrategy shrink = new SimpleShrink();
    try {
      for (int i = 0; i != config.shrinkCycles(); i++) {
        
        if (lastSmallestState.precursor().isEmpty()) {
          break;
        }
        
        long[] shrunk = shrink.shrink(config.prng(),lastSmallestState.precursor());
        
        PrecursorDataPair<T> t = generate(prop.getGen(), shrunk,
            config.generateAttempts());
        
        if (checkHash(t) || t.failedAssumptions() > lastSmallestState.failedAssumptions()) {
          continue;
        }

        Optional<Falsification<T>> maybeFalisfied = prop
            .tryFalsification(t.value());
        if (maybeFalisfied.isPresent()) {    
          lastSmallestState = t;
          falsifyingValues.add(t.value());
          smallestFoundThrowable = maybeFalisfied.get().cause();
        } 
      }
    } catch (AttemptsExhaustedException ex) {
      // swallow - if we got as far as shrinking we were unlucky to run out of
      // values now but we might have found some results earlier
    }

    return falsifyingValues;

  }

  private <T> boolean checkHash(PrecursorDataPair<T> t) {
    long hash = generateHash(t);
    if (visitedValues.contains(hash)) {
      return true;
    }
    visitedValues.add(hash);
    return false;
   }

  
  private <T> long generateHash(PrecursorDataPair<T> t) {   
    // CRC gives fairly good performance thanks to optimised code in JVM
    // but a higher collision rate than alternatives like murmer3 - using mainly
    // so we don't need to include a hash implementation
   CRC32 crc = new CRC32();
   crc.update(t.precursor().bytes());
   return crc.getValue();
  }

  private <T> PrecursorDataPair<T> generate(Gen<T> gen, long[] forced,
      int maxTries) {
    ShapedDataSource buffer = new ShapedDataSource(config.prng(), forced,
        maxTries);
    T t = gen.generate(buffer);
    return new PrecursorDataPair<>(buffer.capturedPrecursor(), buffer.failedAssumptions(), t);
  }

}
