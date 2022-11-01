package cloud.lightupon;

import java.util.ArrayList;
import java.util.List;


public class Clock {

    List entries;
    Object value;

    public Clock(List entries, List values) {
        this.entries = entries;
        this.value = values;
    }

    public Clock(List entries, String value) {
        this.entries = entries;
        this.value = new ArrayList();
        this.value = value;
    }

    public List getEntries() {
        return entries;
    }

    public Object getValue() {
        return value;
    }

    public boolean isEmpty() {
        boolean isValueEmpty = false;
        if (this.value instanceof List && ((List) value).size() == 0) {
            isValueEmpty = true;
        } else {
            if (this.value == null) isValueEmpty = true;
        }
        if (this.entries.isEmpty() && isValueEmpty) {
            return true;
        }
        return false;
    }

    public List asList() {
        List result = new ArrayList();
        result.add(this.entries);
        result.add(this.value);
        return result;
    }

    public boolean equals(Clock clock2) {
        return this.getEntries().equals(clock2.getEntries()) && this.getValue().equals(clock2.getValue());
    }

}
