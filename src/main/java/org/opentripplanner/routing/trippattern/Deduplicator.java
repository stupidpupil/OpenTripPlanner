package org.opentripplanner.routing.trippattern;

import com.google.common.collect.Maps;
import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.model.base.ToStringBuilder;

/**
 * Does the same thing as String.intern, but for several different types. Java's String.intern uses
 * perm gen space and is broken anyway.
 */
public class Deduplicator implements Serializable {

  private static final long serialVersionUID = 20140524L;

  private final Map<BitSet, BitSet> canonicalBitSets = Maps.newHashMap();
  private final Map<IntArray, IntArray> canonicalIntArrays = Maps.newHashMap();
  private final Map<String, String> canonicalStrings = Maps.newHashMap();
  private final Map<StringArray, StringArray> canonicalStringArrays = Maps.newHashMap();
  private final Map<String2DArray, String2DArray> canonicalString2DArrays = Maps.newHashMap();
  private final Map<Class<?>, Map<?, ?>> canonicalObjects = new HashMap<>();
  private final Map<Class<?>, Map<List<?>, List<?>>> canonicalLists = new HashMap<>();

  private final Map<String, Integer> effectCounter = new HashMap<>();

  /** Free up any memory used by the deduplicator. */
  public void reset() {
    canonicalBitSets.clear();
    canonicalIntArrays.clear();
    canonicalStrings.clear();
    canonicalStringArrays.clear();
    canonicalString2DArrays.clear();
    canonicalObjects.clear();
    canonicalLists.clear();
  }

  @Nullable
  public BitSet deduplicateBitSet(BitSet original) {
    if (original == null) {
      return null;
    }
    BitSet canonical = canonicalBitSets.get(original);
    if (canonical == null) {
      canonical = original;
      canonicalBitSets.put(canonical, canonical);
    }
    incrementEffectCounter(BitSet.class);
    return canonical;
  }

  /** Used to deduplicate time and stop sequence arrays. The same times may occur in many trips. */
  @Nullable
  public int[] deduplicateIntArray(int[] original) {
    if (original == null) {
      return null;
    }
    IntArray intArray = new IntArray(original);
    IntArray canonical = canonicalIntArrays.get(intArray);
    if (canonical == null) {
      canonical = intArray;
      canonicalIntArrays.put(canonical, canonical);
    }
    incrementEffectCounter(IntArray.class);
    return canonical.array;
  }

  @Nullable
  public String deduplicateString(String original) {
    if (original == null) {
      return null;
    }
    String canonical = canonicalStrings.putIfAbsent(original, original);
    incrementEffectCounter(String.class);
    return canonical == null ? original : canonical;
  }

  @Nullable
  public String[] deduplicateStringArray(String[] original) {
    if (original == null) {
      return null;
    }
    StringArray canonical = canonicalStringArrays.get(new StringArray(original, false));
    if (canonical == null) {
      canonical = new StringArray(original, true);
      canonicalStringArrays.put(canonical, canonical);
    }
    incrementEffectCounter(StringArray.class);
    return canonical.array;
  }

  /**
   * Used to deduplicate list of via places for stop destinationDisplay. Stops may have the same via
   * arrays.
   */
  @Nullable
  public String[][] deduplicateString2DArray(String[][] original) {
    if (original == null) {
      return null;
    }
    String2DArray canonical = canonicalString2DArrays.get(new String2DArray(original, false));
    if (canonical == null) {
      canonical = new String2DArray(original, true);
      canonicalString2DArrays.put(canonical, canonical);
    }
    incrementEffectCounter(String2DArray.class);
    return canonical.array;
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public <T> T deduplicateObject(Class<T> cl, T original) {
    if (String.class == cl) {
      throw new IllegalArgumentException("Use #deduplicateString() instead.");
    }
    if (original == null) {
      return null;
    }
    Map<T, T> objects = (Map<T, T>) canonicalObjects.computeIfAbsent(cl, c -> new HashMap<T, T>());
    T canonical = objects.putIfAbsent(original, original);
    incrementEffectCounter(cl);
    return canonical == null ? original : canonical;
  }

  @Nullable
  public <T> List<T> deduplicateImmutableList(Class<T> clazz, List<T> original) {
    if (original == null) {
      return null;
    }

    Map<List<?>, List<?>> canonicalLists =
      this.canonicalLists.computeIfAbsent(clazz, key -> new HashMap<>());

    @SuppressWarnings("unchecked")
    List<T> canonical = (List<T>) canonicalLists.get(original);
    if (canonical == null) {
      // The list may contain nulls, hence the use of the old unmodifiable wrapper
      //noinspection FuseStreamOperations
      canonical =
        Collections.unmodifiableList(
          original.stream().map(it -> deduplicateObject(clazz, it)).collect(Collectors.toList())
        );
      canonicalLists.put(canonical, canonical);
    }

    incrementEffectCounter(listKey(clazz));
    return canonical;
  }

  /**
   * Returns a string with the size of each canonical collection.
   */
  @Override
  public String toString() {
    var builder = ToStringBuilder
      .of(Deduplicator.class)
      .addObj("BitSet", sizeAndCount(canonicalBitSets.size(), BitSet.class))
      .addObj("IntArray", sizeAndCount(canonicalIntArrays.size(), IntArray.class))
      .addObj("String", sizeAndCount(canonicalStrings.size(), String.class))
      .addObj("StringArray", sizeAndCount(canonicalStringArrays.size(), StringArray.class))
      .addObj("String2DArray", sizeAndCount(canonicalString2DArrays.size(), String2DArray.class));

    canonicalObjects.forEach((k, v) -> builder.addObj(k.getSimpleName(), sizeAndCount(v.size(), k))
    );
    canonicalLists.forEach((k, v) ->
      builder.addObj("List<" + k.getSimpleName() + ">", sizeAndCount(v.size(), listKey(k)))
    );

    return builder.toString();
  }

  /* private members */

  private static <T> String listKey(Class<T> elementType) {
    return "List<" + elementType.getName() + ">";
  }

  private void incrementEffectCounter(Class<?> clazz) {
    incrementEffectCounter(clazz.getName());
  }

  private String sizeAndCount(int size, Class<?> clazz) {
    return sizeAndCount(size, clazz.getName());
  }

  private void incrementEffectCounter(String key) {
    // Count the first element, start at 1
    effectCounter.compute(key, (k, v) -> v == null ? 1 : ++v);
  }

  private String sizeAndCount(int size, String key) {
    int count = effectCounter.getOrDefault(key, 0);
    return size + "(" + count + ")";
  }

  /* private classes */

  /** A wrapper for a primitive int array. This is insane but necessary in Java. */
  private static class IntArray implements Serializable {

    private static final long serialVersionUID = 20140524L;
    final int[] array;

    IntArray(int[] array) {
      this.array = array;
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(array);
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof IntArray)) {
        return false;
      }

      IntArray that = (IntArray) other;
      return Arrays.equals(array, that.array);
    }
  }

  /** A wrapper for a String array. Optionally, the individual Strings may be deduplicated too. */
  private class StringArray implements Serializable {

    private static final long serialVersionUID = 20140524L;
    final String[] array;

    StringArray(String[] array, boolean deduplicateStrings) {
      if (deduplicateStrings) {
        this.array = new String[array.length];
        for (int i = 0; i < array.length; i++) {
          this.array[i] = deduplicateString(array[i]);
        }
      } else {
        this.array = array;
      }
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(array);
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof StringArray)) {
        return false;
      }
      StringArray that = (StringArray) other;
      return Arrays.equals(array, that.array);
    }
  }

  /** A wrapper for 2D string array */
  private class String2DArray implements Serializable {

    private static final long serialVersionUID = 20140524L;
    final String[][] array;

    private String2DArray(String[][] array, boolean deduplicate) {
      if (deduplicate) {
        this.array = new String[array.length][];
        for (int i = 0; i < array.length; i++) {
          this.array[i] = deduplicateStringArray(array[i]);
        }
      } else {
        this.array = array;
      }
    }

    @Override
    public int hashCode() {
      return Arrays.deepHashCode(array);
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof String2DArray)) {
        return false;
      }
      String2DArray that = (String2DArray) other;
      return Arrays.deepEquals(array, that.array);
    }
  }
}
