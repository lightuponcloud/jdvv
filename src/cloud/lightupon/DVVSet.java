/*
 * Java implementation of Dotted Version Vectors, which
 * provides a container for a set of concurrent values (siblings) with causal
 * order information.
 * */
package cloud.lightupon;

import java.util.*;

public class DVVSet {
    /*
     * Constructs a new clock set without causal history,
     * and receives one value that goes to the anonymous list.
     */
    public Clock newDvv(String value) {
        List<String> values = new ArrayList();
        values.add(value);
        return new Clock(new ArrayList(), values);
    }

    /*
     * Same as new, but receives a list of values, instead of a single value.
     */
    public Clock newList(List values) {
        return new Clock(new ArrayList(), values);
    }

    public Clock newList(String value) {
        return newDvv(value);
    }

    /*
     * Constructs a new clock set with the causal history
     * of the given version vector / vector clock,
     * and receives one value that goes to the anonymous list.
     * The version vector SHOULD BE the output of join.
     */
    public Clock newWithHistory(List<List> vector, Object value) {
        // defense against non-order preserving serialization
        Collections.sort(vector, new DVVComparator());
        List entries = new ArrayList();
        for (int i = 0; i != vector.size(); i++) {
            List entry = new ArrayList();
            List item = (List) vector.get(i);
            entry.add(item.get(0));
            entry.add(item.get(1));
            entry.add(new ArrayList());
            entries.add(entry);
        }
        if (value instanceof List) {
            return new Clock(entries, (List) value);
        } else {
            return new Clock(entries, (String) value);
        }
    }

    /*
     * Same as newWithHistory, but receives a list of values, instead of a single value.
     */
    public Clock newListWithHistory(List vector, String value) {
        List values = new ArrayList();
        values.add(value);
        return newListWithHistory(vector, values);
    }

    public Clock newListWithHistory(List vector, List value) {
        return newWithHistory(vector, value);
    }

    private List foldl(final List<List> lst) {
        if (lst.isEmpty()) {
            return lst;
        }
        Collections.reverse(lst);
        List result = lst.stream().reduce(new ArrayList(), (x, y) -> _sync(x, y));
        return result;
    }

    /*
     * Synchronizes a list of clocks using _sync().
     * It discards (causally) outdated values, while merging all causal histories.
     */
    public List sync(Clock clock) {
        return foldl(clock.asList());
    }

    private List _sync(List clock1, List clock2) {
        if (clock1.isEmpty()) {
            return clock2;
        }
        if (clock2.isEmpty()) {
            return clock1;
        }
        List clock1Entries = (List) clock1.get(0);
        Object clock1Value = clock1.get(1);
        List clock2Entries = (List) clock2.get(0);
        Object clock2Value = clock2.get(1);

        Object value;
        if (less(clock1, clock2)) {
            value = clock2Value; // clock1 < clock2: return values2
        } else {
            if (less(clock2, clock1)) {
                value = clock1Value;
            } else {
                Set uniqueValues = new HashSet();
                if (clock1Value instanceof List) {
                    uniqueValues.addAll((List) clock1Value);
                } else {
                    uniqueValues.add(clock1Value);
                }
                if (clock2Value instanceof List) {
                    uniqueValues.addAll((List) clock2Value);
                } else {
                    uniqueValues.add(clock1Value);
                }
                value = new ArrayList();
                if (uniqueValues.size() > 0) {
                    ((List) value).addAll(uniqueValues);
                }
            }
        }
        List result = new ArrayList();
        List syncedClocks = _sync2(clock1Entries, clock2Entries);
        result.add(syncedClocks);
        result.add(value);
        return result;
    }

    private List _sync2(List<List> entries1, List<List> entries2) {
        if (entries1.isEmpty()) return entries2;
        if (entries2.isEmpty()) return entries1;

        // copy lists to avoid changing them
        List head1 = new ArrayList();
        head1.addAll(entries1.get(0));
        List head2 = new ArrayList();
        head2.addAll(entries2.get(0));

        DVVComparator comparator = new DVVComparator();
        if (comparator.compare(head2.get(0), head1.get(0)) > 0) {
            List result = new ArrayList();
            result.add(head1);
            List toAppend = _sync2(entries1.subList(1, entries1.size()), entries2);
            result.addAll(toAppend);
            return result;
        }
        if (comparator.compare(head1.get(0), head2.get(0)) > 0) {
            List result = new ArrayList();
            result.add(head2);
            List toAppend = _sync2(entries2.subList(1, entries2.size()), entries1);
            result.addAll(toAppend);
            return result;
        }

        Object theId = head1.get(0);
        int counter1 = (int) head1.get(1);
        List values1 = (List) head1.get(2);
        int counter2 = (int) head2.get(1);
        List values2 = (List) head2.get(2);
        List result = new ArrayList();
        List mergeResult = _merge(theId, counter1, values1, counter2, values2);
        result.add(mergeResult);
        List syncResult = _sync2(entries1.subList(1, entries1.size()), entries2.subList(1, entries2.size()));
        result.addAll(syncResult);
        return result;
    }

    /*
     * Returns [id(), counter(), values()]
     */
    private List _merge(Object theId, int counter1, List values1, int counter2, List values2) {
        int len1 = values1.size();
        int len2 = values2.size();
        List result = new ArrayList();
        result.add(theId);
        if (counter1 >= counter2) {
            if (counter1 - len1 >= counter2 - len2) {
                result.add(counter1);
                result.add(values1);
                return result;
            }
            result.add(counter1);
            int idx = counter1 - counter2 + len2;
            List slice = values1.subList(0, idx);
            result.add(slice);
            return result;
        }
        if (counter2 - len2 >= counter1 - len1) {
            result.add(counter2);
            result.add(values2);
            return result;
        }
        result.add(counter2);
        int idx = counter2 - counter1 + len1;
        List slice = values2.subList(0, idx);
        result.add(slice);
        return result;
    }

    /* Returns True if the first clock is causally older than
     * the second clock, thus values on the first clock are outdated.
     * Returns False otherwise.
     */
    public boolean less(List clock1, List clock2) {
        return greater((List) clock2.get(0), (List) clock1.get(0), false);
    }

    private boolean greater(List vector1, List vector2, boolean isStrict) {
        if (vector1.isEmpty() && vector2.isEmpty()) {
            return isStrict;
        }
        if (vector2.isEmpty()) return true;
        if (vector1.isEmpty()) return false;
        Object value1 = ((List) vector1.get(0)).get(0);
        Object value2 = ((List) vector2.get(0)).get(0);
        if (value1.equals(value2)) {
            int dotNum1 = (int) ((List) vector1.get(0)).get(1);
            int dotNum2 = (int) ((List) vector2.get(0)).get(1);
            if (dotNum1 == dotNum2) {
                return greater(vector1.subList(1, vector1.size()), vector2.subList(1, vector2.size()), isStrict);
            }
            if (dotNum1 > dotNum2) {
                return greater(vector1.subList(1, vector1.size()), vector2.subList(1, vector2.size()), isStrict);
            }
            if (dotNum1 < dotNum2) return false;
        }
        DVVComparator comparator = new DVVComparator();
        if (comparator.compare(((List) vector2.get(0)).get(0), ((List) vector1.get(0)).get(0)) > 0) {
            return greater(vector1.subList(1, vector1.size()), vector2, true);
        }
        return false;
    }

    /*
     * Return a version vector that represents the causal history.
     */
    public List join(Clock clock) {
        List values = clock.getEntries();
        List result = new ArrayList();
        for (int i = 0; i != values.size(); i++) {
            List value = (List) values.get(i);
            List record = new ArrayList();
            record.add(value.get(0));
            record.add(value.get(1));
            result.add(record);
        }
        return result;
    }

    /* Advances the causal history with the given id.
     * The new value is the *anonymous dot* of the clock.
     * The client clock SHOULD BE a direct result of new.
     */
    public Clock create(Clock clock, Object theId) {
        Object value = clock.getValue();
        List event;
        if (value instanceof List && ((List) value).size() > 0) {
            value = ((List) value).get(0);
            event = event(clock.getEntries(), theId, value);
        } else {
            event = event(clock.getEntries(), theId, value);
        }
        return new Clock(event, new ArrayList());
    }

    /* Advances the causal history of the
     * first clock with the given id, while synchronizing
     * with the second clock, thus the new clock is
     * causally newer than both clocks in the argument.
     * The new value is the *anonymous dot* of the clock.
     * The first clock SHOULD BE a direct result of new/2,
     * which is intended to be the client clock with
     * the new value in the *anonymous dot* while
     * the second clock is from the local server.
     */
    public Clock update(Clock clock1, Clock clock2, Object theId) {
        // Sync both clocks without the new value
        List c1 = new ArrayList();
        c1.add(clock1.getEntries());
        c1.add(new ArrayList());
        List syncedClock = _sync(c1, clock2.asList());

        Object clockValue = clock1.getValue();
        if (clockValue instanceof List) {
            clockValue = ((List) clockValue).get(0);
        }
        // We create a new event on the synced causal history,
        // with the id I and the new value.
        // The anonymous values that were synced still remain.
        List event = event((List) syncedClock.get(0), theId, clockValue);
        return new Clock(event, (List) syncedClock.get(1));
    }

    public List event(List vector, Object theId, Object value) {
        if (vector.isEmpty()) {
            List result = new ArrayList();
            List event = new ArrayList();
            event.add(theId);
            event.add(1);
            List values = new ArrayList();
            values.add(value);
            event.add(values);
            result.add(event);
            return result;
        }
        if (((List) vector.get(0)).size() > 0) {
            Object vectorId = ((List) vector.get(0)).get(0);
            if (theId.equals(vectorId)) {
                List values = new ArrayList();
                if (value instanceof List) {
                    values.addAll((List) value);
                    values.addAll((List) ((List) vector.get(0)).get(2));
                } else {
                    values.add(value);
                    values.addAll((List) ((List) vector.get(0)).get(2));
                }
                List result = new ArrayList();
                List bit = new ArrayList();
                bit.add(((List) vector.get(0)).get(0));
                bit.add((int) ((List) vector.get(0)).get(1) + 1); // incrementing counter
                bit.add(values);
                result.add(bit);
                result.addAll(vector.subList(1, vector.size()));
                return result;
            } else {
                Object nestedElement = ((List) vector.get(0)).get(0);
                DVVComparator comparator = new DVVComparator();
                if (comparator.compare(nestedElement, theId) > 0) {
                    List result = new ArrayList();
                    List bit = new ArrayList();
                    bit.add(theId);
                    bit.add(1);
                    List bitValue = new ArrayList();
                    bitValue.add(value);
                    bit.add(bitValue);
                    result.add(bit);
                    result.addAll(vector);
                    return result;
                }
            }
        }
        List result = new ArrayList();
        result.add(vector.get(0));
        List eventValue = event(vector.subList(1, vector.size()), theId, value);
        result.addAll(eventValue);
        return result;
    }

    /*
     * Returns the total number of values in this clock set.
     */
    public int size(Clock clock) {
        int result = 0;
        List entries = clock.getEntries();
        for (int i = 0; i != entries.size(); i++) {
            result += (int) entries.get(2);
        }
        Object value = clock.getValue();
        if (value instanceof List) {
            result += ((List) value).size();
        } else {
            if (value != null) result += 1;
        }
        return result;
    }

    /*
     * Returns all the ids used in this clock set.
     * */
    public List ids(List clock) {
        List result = new ArrayList<>();
        List entries = (List) clock.get(0);
        for (int i = 0; i != entries.size(); i++) {
            Object value = ((List) entries.get(i)).get(0);
            result.add(value);
        }
        return result;
    }

    /*
     * Returns all the values used in this clock set,
     * including the anonymous values.
     */
    public List values(List clock) {
        List lst = new ArrayList();
        List entries = (List) clock.get(0);
        for (int i = 0; i != entries.size(); i++) {
            List entry = (List) entries.get(i);
            List value = (List) entry.get(2);
            lst.add(value);
        }
        List flatList = new ArrayList();
        for (int i = 0; i != lst.size(); i++) {
            List subList = (List) lst.get(i);
            for (int j = 0; j != subList.size(); j++) {
                flatList.add(subList.get(j));
            }
        }
        List result = (List) clock.get(1);
        result.addAll(flatList);
        return result;
    }

    /*
     * Compares the equality of both clocks, regarding
     * only the causal histories, thus ignoring the values.
     */
    public boolean equal(Clock clock1, Clock clock2) {
        return equal(clock1.getEntries(), clock2.getEntries());
    }

    public boolean equal(List vector1, List vector2) {
        if (vector1.isEmpty() && vector2.isEmpty()) return true;
        if (!vector1.isEmpty() && !vector2.isEmpty()) {
            List value1 = (List) vector1.get(0);
            List value2 = (List) vector2.get(0);
            if (!value1.isEmpty() && !value2.isEmpty()) {
                if (value1.get(0) == value2.get(0) && value1.get(1) == value2.get(1)) {
                    int size1 = ((List) value1.get(2)).size();
                    int size2 = ((List) value2.get(2)).size();
                    if (size1 == size2) {
                        return equal(vector1.subList(1, vector1.size()), vector2.subList(1, vector2.size()));
                    }
                }
            }
        }
        return false;
    }
}
