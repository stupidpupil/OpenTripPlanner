/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import static org.opentripplanner.util.AssertUtils.assertHasValue;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

public final class FeedScopedId implements Serializable, Comparable<FeedScopedId> {

  /**
   * One Bus Away uses the underscore as a scope separator between Agency and ID. In OTP we use feed
   * IDs instead of agency IDs as scope, and they are separated with a colon when represented
   * together in String form.
   */
  private static final char ID_SEPARATOR = ':';

  @Serial
  private static final long serialVersionUID = 1L;

  private final String feedId;

  private final String id;

  public FeedScopedId(@NotNull String feedId, @NotNull String id) {
    assertHasValue(feedId);
    assertHasValue(id);

    this.feedId = feedId;
    this.id = id;
  }

  /**
   * Return a new FeedId if the id is not {@code null}, an empty string or contains whitespace
   * only.
   */
  @Nullable
  public static FeedScopedId ofNullable(@NotNull String feedId, @Nullable String id) {
    return id == null || id.isBlank() ? null : new FeedScopedId(feedId, id);
  }

  /**
   * Given an id of the form "feedId:entityId", parses into a {@link FeedScopedId} id object.
   *
   * @param value id of the form "feedId:entityId"
   * @return an id object
   * @throws IllegalArgumentException if the id cannot be parsed
   */
  public static FeedScopedId parseId(String value) throws IllegalArgumentException {
    if (value == null || value.isEmpty()) {
      return null;
    }
    int index = value.indexOf(ID_SEPARATOR);
    if (index == -1) {
      throw new IllegalArgumentException("invalid feed-scoped-id: " + value);
    } else {
      return new FeedScopedId(value.substring(0, index), value.substring(index + 1));
    }
  }

  public static boolean isValidString(String value) throws IllegalArgumentException {
    return value != null && value.indexOf(ID_SEPARATOR) > -1;
  }

  /**
   * Concatenate feedId and id into a string.
   */
  public static String concatenateId(String feedId, String id) {
    return feedId + ID_SEPARATOR + id;
  }

  /**
   * Parses a string consisting of concatenated FeedScopedIds to a Set
   */
  public static Set<FeedScopedId> parseSetOfIds(String s) {
    return Arrays.stream(s.split(",")).map(FeedScopedId::parseId).collect(Collectors.toSet());
  }

  /**
   * Parses a string consisting of concatenated FeedScopedIds to a List
   */
  public static List<FeedScopedId> parseListOfIds(String s) {
    return Arrays.stream(s.split(",")).map(FeedScopedId::parseId).collect(Collectors.toList());
  }

  public String getFeedId() {
    return feedId;
  }

  public String getId() {
    return id;
  }

  public int compareTo(FeedScopedId o) {
    int c = this.feedId.compareTo(o.feedId);
    if (c == 0) {
      c = this.id.compareTo(o.id);
    }
    return c;
  }

  @Override
  public int hashCode() {
    return feedId.hashCode() ^ id.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof FeedScopedId other)) {
      return false;
    }

    return feedId.equals(other.feedId) && id.equals(other.id);
  }

  /**
   * DO NOT CHANGE THIS - It is used in Serialization of the id.
   */
  @Override
  public String toString() {
    return concatenateId(feedId, id);
  }
}
