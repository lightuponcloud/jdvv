package cloud.lightupon;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class DVVComparator implements Comparator {

    /*
     * Allows to compare lists with strings, as in Erlang.
     * ( list > string )
     */
    @Override
    public int compare(Object a, Object b) {
        if (a instanceof String && b instanceof String) {
            return (((String) a).compareTo(((String) b)));
        }
        if (a instanceof Number && b instanceof Number) {
            return (((String) a).compareTo(((String) b)));
        }
        if (a instanceof Collection && b instanceof Collection) {
            if (((Collection) a).size() > 0 && ((Collection) b).size() > 0) {
                Object va = ((Collection<?>) a).iterator().next();
                Object vb = ((Collection<?>) b).iterator().next();
                if (va instanceof Collection && vb instanceof Collection) {
                    int s1 = ((Collection) va).size();
                    int s2 = ((Collection) vb).size();
                    if (s1 > s2) {
                        return 1;
                    } else if (s1 == s2) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else if (va instanceof String && vb instanceof Collection) {
                    // string is less than list by Erlang logic
                    return -1;
                } else if (va instanceof Collection && vb instanceof String) {
                    return 1;
                } else if (va instanceof String && vb instanceof String) {
                    return ((String) va).compareTo((String) vb);
                }
            }
        }
        if (a instanceof String && b instanceof Collection) {
            // string is less than list by Erlang logic
            return -1;
        } else if (a instanceof Collection && b instanceof String) {
            return 1;
        } else if (a instanceof String && b instanceof String) {
            return ((String) a).compareTo((String) b);
        }
        return (((String) a).compareTo(((String) b)));
    }
}
