package test.cloud.lightupon;

import cloud.lightupon.Clock;
import cloud.lightupon.DVVSet;
import junit.framework.TestCase;

import java.util.*;

public class DVVSetTest extends TestCase {
    DVVSet dvvSet;

    protected void setUp() {
        dvvSet = new DVVSet();
    }

    public void testJoin() {
        Clock A = this.dvvSet.newDvv("v1");
        Clock A1 = this.dvvSet.create(A, "a");

        Clock B = dvvSet.newWithHistory(dvvSet.join(A1), "v2");
        Clock B1 = dvvSet.update(B, A1, "b");
        assertEquals(dvvSet.join(A), new ArrayList());

        List lst = new ArrayList();
        List nested = new ArrayList();
        nested.add("a");
        nested.add(1);
        lst.add(nested);
        assertEquals(dvvSet.join(A1), lst); // [["a", 1]]

        lst = new ArrayList();
        nested = new ArrayList();
        nested.add("a");
        nested.add(1);
        lst.add(nested);
        nested = new ArrayList();
        nested.add("b");
        nested.add(1);
        lst.add(nested);
        List stuff = dvvSet.join(B1);
        assertEquals(stuff, lst); // [["a", 1], ["b", 1]]
    }

    public void testUpdate() {
        Clock A0 = this.dvvSet.create(this.dvvSet.newDvv("v1"), "a");
        List v2 = new ArrayList();
        v2.add("v2");
        Clock A1 = this.dvvSet.update(this.dvvSet.newListWithHistory(this.dvvSet.join(A0), v2), A0, "a");
        List v3 = new ArrayList();
        v3.add("v3");
        Clock A2 = this.dvvSet.update(this.dvvSet.newListWithHistory(this.dvvSet.join(A1), v3), A0, "b");
        List v4 = new ArrayList();
        v4.add("v4");
        Clock A3 = this.dvvSet.update(this.dvvSet.newListWithHistory(this.dvvSet.join(A0), v4), A0, "b");
        List v5 = new ArrayList();
        v5.add("v5");
        Clock A4 = this.dvvSet.update(this.dvvSet.newListWithHistory(this.dvvSet.join(A0), v5), A0, "a");

        List expectedEntities0 = new ArrayList();
        List nested01 = new ArrayList();
        nested01.add("a");
        nested01.add(1);
        List nested02 = new ArrayList();
        nested02.add("v1");
        nested01.add(nested02);
        expectedEntities0.add(nested01);
        Clock expectedClock0 = new Clock(expectedEntities0, new ArrayList());
        assertTrue(expectedClock0.equals(A0)); // [[["a",1,["v1"]]],[]]

        List expectedEntities1 = new ArrayList();
        List nested11 = new ArrayList();
        nested11.add("a");
        nested11.add(2);
        List nested12 = new ArrayList();
        nested12.add("v2");
        nested11.add(nested12);
        expectedEntities1.add(nested11);
        Clock expectedClock1 = new Clock(expectedEntities1, new ArrayList());
        assertTrue(expectedClock1.equals(A1)); // [[["a",2,["v2"]]],[]]
    }

    public void testSync() {
        List nested00 = new ArrayList();
        List nested01 = new ArrayList();
        nested01.add("x");
        nested01.add(1);
        nested01.add(new ArrayList());
        nested00.add(nested01);
        Clock X = new Clock(nested00, new ArrayList()); // [[["x",1,[]]],[]]

        Clock A = this.dvvSet.create(this.dvvSet.newDvv("v1"), "a");
        List v2Lst = new ArrayList();
        v2Lst.add("v2");
        Clock Y = this.dvvSet.create(this.dvvSet.newList(v2Lst), "b");
        Clock A1 = this.dvvSet.create(this.dvvSet.newListWithHistory(this.dvvSet.join(A), v2Lst), "a");
        List v3Lst = new ArrayList();
        v3Lst.add("v3");
        Clock A3 = this.dvvSet.create(this.dvvSet.newListWithHistory(this.dvvSet.join(A1), v3Lst), "b");
        Clock A4 = this.dvvSet.create(this.dvvSet.newListWithHistory(this.dvvSet.join(A1), v3Lst), "c");

        List W = new ArrayList(); // [[["a",1,[]]],[]]
        List nested10 = new ArrayList();
        List nested11 = new ArrayList();
        nested11.add("a");
        nested11.add(1);
        nested11.add(new ArrayList());
        nested10.add(nested11);
        W.add(nested10);
        W.add(new ArrayList());

        List Z = new ArrayList(); // [[["a",2,["v2","v1"]]],[]]
        List nested20 = new ArrayList();
        List nested21 = new ArrayList();
        nested21.add("a");
        nested21.add(2);
        List nested22 = new ArrayList();
        nested22.add("v2");
        nested22.add("v1");
        nested21.add(nested22);
        nested20.add(nested21);
        Z.add(nested20);
        Z.add(new ArrayList());

        Clock clockWZ = new Clock(W, Z);
        Clock clockZW = new Clock(Z, W);
        List syncResultWZ = this.dvvSet.sync(clockWZ);
        List syncResultZW = this.dvvSet.sync(clockZW);
        assertEquals(syncResultWZ, syncResultZW);

        // test list of clocks synchronization

        Clock clockAA1 = new Clock(A.asList(), A1.asList());
        Clock clockA1A = new Clock(A1.asList(), A.asList());
        List syncResultAA1 = this.dvvSet.sync(clockAA1);
        List syncResultA1A = this.dvvSet.sync(clockA1A);
        assertEquals(syncResultAA1, syncResultA1A);

        Clock clockA4A3 = new Clock(A4.asList(), A3.asList());
        Clock clockA3A4 = new Clock(A3.asList(), A4.asList());
        List syncResultA4A3 = this.dvvSet.sync(clockA4A3);
        List syncResultA3A4 = this.dvvSet.sync(clockA3A4);
        assertEquals(syncResultA4A3, syncResultA3A4);

        List expectedValue0 = new ArrayList(); // [[["a",2,[]], ["b",1,["v3"]], ["c",1,["v3"]]],[]]
        List nested31 = new ArrayList();
        nested31.add("a");
        nested31.add(2);
        nested31.add(new ArrayList());

        List nested32 = new ArrayList();
        nested32.add("b");
        nested32.add(1);
        nested32.add(v3Lst);

        List nested33 = new ArrayList();
        nested33.add("c");
        nested33.add(1);
        nested33.add(v3Lst);

        List nested30 = new ArrayList();
        nested30.add(nested31);
        nested30.add(nested32);
        nested30.add(nested33);
        expectedValue0.add(nested30);
        expectedValue0.add(new ArrayList());

        assertEquals(syncResultA4A3, expectedValue0);

        List expectedValue1 = new ArrayList(); // [[["a",1,["v1"]],["x",1,[]]],[]]
        List nested40 = new ArrayList();
        List nested41 = new ArrayList();
        nested41.add("a");
        nested41.add(1);
        List nested42 = new ArrayList();
        nested42.add("v1");
        nested41.add(nested42);

        List nested43 = new ArrayList();
        nested43.add("x");
        nested43.add(1);
        nested43.add(new ArrayList());

        nested40.add(nested41);
        nested40.add(nested43);

        expectedValue1.add(nested40);
        expectedValue1.add(new ArrayList());

        Clock clockXA = new Clock(X.asList(), A.asList());
        List syncResultXA = this.dvvSet.sync(clockXA);
        assertEquals(syncResultXA, expectedValue1);

        Clock clockAX = new Clock(A.asList(), X.asList());
        List syncResultAX = this.dvvSet.sync(clockAX);
        assertEquals(syncResultXA, syncResultAX);

        List expectedValue2 = new ArrayList(); // [[["a",1,["v1"]],["b",1,["v2"]]],[]]
        List nested50 = new ArrayList();
        List nested51 = new ArrayList();
        List nested52 = new ArrayList();
        nested52.add("a");
        nested52.add(1);
        List v1Lst = new ArrayList();
        v1Lst.add("v1");
        nested52.add(v1Lst);
        nested51.addAll(nested52);

        List nested54 = new ArrayList();
        nested54.add("b");
        nested54.add(1);
        nested54.add(v2Lst);
        nested51.add(nested54);
        nested50.add(nested52);
        nested50.add(nested54);

        expectedValue2.add(nested50);
        expectedValue2.add(new ArrayList());

        Clock clockAY = new Clock(A.asList(), Y.asList());
        List syncResultAY = this.dvvSet.sync(clockAY);
        assertEquals(syncResultAY, expectedValue2);

        Clock clockYA = new Clock(Y.asList(), A.asList());
        List syncResultYA = this.dvvSet.sync(clockYA);
        assertEquals(syncResultAY, syncResultYA);

        // the following is the same check, just to make sure original values are not modified between calls
        List syncResultXACopy = this.dvvSet.sync(clockXA);
        List syncResultAXCopy = this.dvvSet.sync(clockAX);
        assertEquals(syncResultXACopy, syncResultAXCopy);
    }

    public void testSyncUpdate() {
        // Mary writes v1 w/o VV
        List v1Lst = new ArrayList();
        v1Lst.add("v1");
        Clock A0 = this.dvvSet.create(this.dvvSet.newList(v1Lst), "a");
        // Peter reads v1 with version vector (VV)
        List VV1 = this.dvvSet.join(A0);
        // Mary writes v2 w/o VV
        List v2Lst = new ArrayList();
        v2Lst.add("v2");
        Clock A1 = this.dvvSet.update(this.dvvSet.newList(v2Lst), A0, "a");
        // Peter writes v3 with VV from v1
        List v3Lst = new ArrayList();
        v3Lst.add("v3");
        Clock A2 = this.dvvSet.update(this.dvvSet.newListWithHistory(VV1, v3Lst), A1, "a");

        List expectedValue0 = new ArrayList();
        List nested0 = new ArrayList();
        nested0.add("a");
        nested0.add(1);
        expectedValue0.add(nested0);
        assertEquals(VV1, expectedValue0);

        List expectedValue1 = new ArrayList();
        List nested1 = new ArrayList();
        nested1.add("a");
        nested1.add(1);
        nested1.add(v1Lst);
        List nested2 = new ArrayList();
        nested2.add(nested1);
        expectedValue1.add(nested2);
        expectedValue1.add(new ArrayList());
        assertEquals(A0.asList(), expectedValue1);

        List expectedValue2 = new ArrayList();
        List nested3 = new ArrayList();
        nested3.add("a");
        nested3.add(2);
        List v2v1Lst = new ArrayList();
        v2v1Lst.add("v2");
        v2v1Lst.add("v1");
        nested3.add(v2v1Lst);
        List nested4 = new ArrayList();
        nested4.add(nested3);
        expectedValue2.add(nested4);
        expectedValue2.add(new ArrayList());
        assertEquals(A1.asList(), expectedValue2);

        // now A2 should only have v2 and v3, since v3 was causally newer than v1
        List expectedValue3 = new ArrayList();
        List nested5 = new ArrayList();
        nested5.add("a");
        nested5.add(3);
        List v3v2Lst = new ArrayList();
        v3v2Lst.add("v3");
        v3v2Lst.add("v2");
        nested5.add(v3v2Lst);
        List nested6 = new ArrayList();
        nested6.add(nested5);
        expectedValue3.add(nested6);
        expectedValue3.add(new ArrayList());
        assertEquals(A2.asList(), expectedValue3);
    }

    public void testEvent() {
        Clock E = this.dvvSet.create(this.dvvSet.newDvv("v1"), "a");
        List A = E.getEntries();

        List expectedValue0 = new ArrayList();
        List nested0 = new ArrayList();
        nested0.add("a");
        nested0.add(2);
        List v2v1Lst = new ArrayList();
        v2v1Lst.add("v2");
        v2v1Lst.add("v1");
        nested0.add(v2v1Lst);
        expectedValue0.add(nested0);

        List eventA = this.dvvSet.event(A, "a", "v2");
        assertEquals(eventA, expectedValue0);

        List expectedValue1 = new ArrayList();
        List nested1 = new ArrayList();
        nested1.add("a");
        nested1.add(1);
        List v1Lst = new ArrayList();
        v1Lst.add("v1");
        nested1.add(v1Lst);
        expectedValue1.add(nested1);

        List nested2 = new ArrayList();
        nested2.add("b");
        nested2.add(1);
        List v2Lst = new ArrayList();
        v2Lst.add("v2");
        nested2.add(v2Lst);
        expectedValue1.add(nested2);

        List eventB = this.dvvSet.event(A, "b", "v2");
        assertEquals(eventB, expectedValue1);
    }

    public void testLess() {
        List aLst = new ArrayList();
        aLst.add("a");
        Clock A = this.dvvSet.create(this.dvvSet.newList("v1"), aLst);
        List v2Lst = new ArrayList();
        v2Lst.add("v2");
        Clock B = this.dvvSet.create(this.dvvSet.newListWithHistory(this.dvvSet.join(A), v2Lst), "a");
        Clock B2 = this.dvvSet.create(this.dvvSet.newListWithHistory(this.dvvSet.join(A), v2Lst), "b");
        Clock B3 = this.dvvSet.create(this.dvvSet.newListWithHistory(this.dvvSet.join(A), v2Lst), "z");
        List v3Lst = new ArrayList();
        v3Lst.add("v3");
        Clock C = this.dvvSet.update(this.dvvSet.newListWithHistory(this.dvvSet.join(B), v3Lst), A, "c");
        List v4Lst = new ArrayList();
        v4Lst.add("v4");
        Clock D = this.dvvSet.update(this.dvvSet.newListWithHistory(this.dvvSet.join(C), v4Lst), B2, "d");

        assertTrue(this.dvvSet.less(A.asList(), B.asList()));
        assertTrue(this.dvvSet.less(A.asList(), C.asList()));
        assertTrue(this.dvvSet.less(B.asList(), C.asList()));
        assertTrue(this.dvvSet.less(B.asList(), D.asList()));
        assertTrue(this.dvvSet.less(B2.asList(), D.asList()));
        assertTrue(this.dvvSet.less(A.asList(), D.asList()));

        assertFalse(this.dvvSet.less(B2.asList(), C.asList()));

        assertFalse(this.dvvSet.less(B.asList(), B2.asList()));
        assertFalse(this.dvvSet.less(B2.asList(), B.asList()));
        assertFalse(this.dvvSet.less(A.asList(), A.asList()));
        assertFalse(this.dvvSet.less(C.asList(), C.asList()));
        assertFalse(this.dvvSet.less(D.asList(), B2.asList()));
        assertFalse(this.dvvSet.less(B3.asList(), D.asList()));
    }

    public void testEqual() {
        List nested01 = new ArrayList();
        nested01.add("a");
        nested01.add(4);
        List nested04 = new ArrayList();
        nested04.add("v5");
        nested04.add("v0");
        nested01.add(nested04);

        List nested02 = new ArrayList();
        nested02.add("b");
        nested02.add(0);
        nested02.add(new ArrayList());

        List v3Lst = new ArrayList();
        v3Lst.add("v3");
        List nested03 = new ArrayList();
        nested03.add("c");
        nested03.add(1);
        nested03.add(v3Lst);

        List nested00 = new ArrayList();
        nested00.add(nested01);
        nested00.add(nested02);
        nested00.add(nested03);

        List v0Lst = new ArrayList();
        v0Lst.add("v0");
        Clock A = new Clock(nested00, v0Lst); //  [[["a",4,["v5","v0"]],["b",0,[]],["c",1,["v3"]]], ["v0"]]

        List nested05 = new ArrayList();
        nested05.add("a");
        nested05.add(4);
        List nested06 = new ArrayList();
        nested06.add("v555");
        nested06.add("v0");
        nested05.add(nested06);

        List nested07 = new ArrayList();
        nested07.add("b");
        nested07.add(0);
        nested07.add(new ArrayList());

        List nested08 = new ArrayList();
        nested08.add("c");
        nested08.add(1);
        nested08.add(v3Lst);

        List nested09 = new ArrayList();
        nested09.add(nested05);
        nested09.add(nested07);
        nested09.add(nested08);

        Clock B = new Clock(nested09, new ArrayList()); // [[["a",4,["v555","v0"]], ["b",0,[]], ["c",1,["v3"]]], []]

        List nested10 = new ArrayList();
        nested10.add("a");
        nested10.add(4);
        List nested15 = new ArrayList();
        nested15.add("v5");
        nested15.add("v0");
        nested10.add(nested15);

        List nested11 = new ArrayList();
        nested11.add("b");
        nested11.add(0);
        nested11.add(new ArrayList());

        List nested17 = new ArrayList();
        nested17.add(nested10);
        nested17.add(nested11);

        List nested13 = new ArrayList();
        nested13.add("v6");
        nested13.add("v1");

        Clock C = new Clock(nested17, nested13); // [[["a",4,["v5","v0"]],["b",0,[]]], ["v6","v1"]]

        // compare only the causal history
        assertTrue(this.dvvSet.equal(A, B));
        assertTrue(this.dvvSet.equal(B, A));

        assertFalse(this.dvvSet.equal(A, C));
        assertFalse(this.dvvSet.equal(B, C));
    }

    public void testSize() {
        List v1List = new ArrayList();
        v1List.add("v1");
        assertEquals(1, this.dvvSet.size(this.dvvSet.newList(v1List)));
    }

    public void testValues() {
        List A = new ArrayList(); //  [[["a",4,["v0","v5"]],["b",0,[]],["c",1,["v3"]]], ["v1"]]
        List nested01 = new ArrayList();
        nested01.add("a");
        nested01.add(4);
        List nested04 = new ArrayList();
        nested04.add("v0");
        nested04.add("v5");
        nested01.add(nested04);

        List nested02 = new ArrayList();
        nested02.add("b");
        nested02.add(0);
        nested02.add(new ArrayList());

        List v3Lst = new ArrayList();
        v3Lst.add("v3");
        List nested03 = new ArrayList();
        nested03.add("c");
        nested03.add(1);
        nested03.add(v3Lst);

        List nested00 = new ArrayList();
        nested00.add(nested01);
        nested00.add(nested02);
        nested00.add(nested03);
        A.add(nested00);
        List v1Lst = new ArrayList();
        v1Lst.add("v1");
        A.add(v1Lst);

        List B = new ArrayList(); //  [[["a",4,["v0","v555"]], ["b",0,[]], ["c",1,["v3"]]], []]
        List nested05 = new ArrayList();
        nested05.add("a");
        nested05.add(4);
        List nested06 = new ArrayList();
        nested06.add("v0");
        nested06.add("v555");
        nested05.add(nested06);

        List nested07 = new ArrayList();
        nested07.add("b");
        nested07.add(0);
        nested07.add(new ArrayList());

        List nested08 = new ArrayList();
        nested08.add("c");
        nested08.add(1);
        nested08.add(v3Lst);

        List nested09 = new ArrayList();
        nested09.add(nested05);
        nested09.add(nested07);
        nested09.add(nested08);
        B.add(nested09);
        B.add(new ArrayList());

        List C = new ArrayList(); //  [[["a",4,[]],["b",0,[]]], ["v1","v6"]]

        List nested10 = new ArrayList();
        nested10.add("a");
        nested10.add(4);
        nested10.add(new ArrayList());

        List nested11 = new ArrayList();
        nested11.add("b");
        nested11.add(0);
        nested11.add(new ArrayList());

        List nested12 = new ArrayList();
        nested12.add(nested10);
        nested12.add(nested11);
        C.add(nested12);
        List nested13 = new ArrayList();
        nested13.add("v1");
        nested13.add("v6");
        C.add(nested13);

        List expectedAIds = new ArrayList();
        expectedAIds.add("a");
        expectedAIds.add("b");
        expectedAIds.add("c");
        assertEquals(dvvSet.ids(A), expectedAIds);
        assertEquals(dvvSet.ids(B), expectedAIds);

        List expectedCIds = new ArrayList();
        expectedCIds.add("a");
        expectedCIds.add("b");
        assertEquals(dvvSet.ids(C), expectedCIds);

        List expectedValuesA = new ArrayList();
        expectedValuesA.add("v0");
        expectedValuesA.add("v1");
        expectedValuesA.add("v3");
        expectedValuesA.add("v5");

        List sortedValuesA = this.dvvSet.values(A);
        Collections.sort(sortedValuesA, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return lhs.compareTo(rhs);
            }
        });
        assertEquals(sortedValuesA, expectedValuesA);

        List expectedValuesB = new ArrayList();
        expectedValuesB.add("v0");
        expectedValuesB.add("v3");
        expectedValuesB.add("v555");

        List sortedValuesB = this.dvvSet.values(B);
        Collections.sort(sortedValuesB);
        assertEquals(sortedValuesB, expectedValuesB);

        List expectedValuesC = new ArrayList();
        expectedValuesC.add("v1");
        expectedValuesC.add("v6");

        List sortedValuesC = this.dvvSet.values(C);
        Collections.sort(sortedValuesC);
        assertEquals(sortedValuesC, expectedValuesC);
    }

}
