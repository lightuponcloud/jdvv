package cloud.lightupon;


import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        // Creating a new version

        DVVSet dvvSet = new DVVSet();
        Clock dot = dvvSet.create(dvvSet.newDvv("1611600920427"), "user_id_1");

        // Incrementing version
        List context = dvvSet.join(dot);
        Clock newDot = dvvSet.update(dvvSet.newWithHistory(context, "1616682865530"), dot, "user_id_2");
        List mergedHistory = dvvSet.sync(new Clock(dot.asList(), newDot.asList()));

        // Detecting conflicts
        List values = dvvSet.values(mergedHistory);
        if (values.size() > 1) {
            System.out.println("CONFLICT");
        } else {
            System.out.println("OK");
        }

        List context2 = dvvSet.join(dot);
        Clock newDot2 = dvvSet.update(dvvSet.newWithHistory(context2, "1616682865530"), dot, "user_id_2");
        dvvSet.sync(new Clock(dot.asList(), newDot2.asList()));
    }
}
