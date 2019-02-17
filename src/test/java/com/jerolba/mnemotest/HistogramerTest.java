/**
 * Copyright 2019 Jerónimo López Bezanilla
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jerolba.mnemotest;

import static com.jerolba.jmnemohistosyne.Histogramer.getDiff;
import static java.lang.Integer.toHexString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.jerolba.jmnemohistosyne.MemoryHistogram;

public class HistogramerTest {

    @Test
    public void simpleTest() {
        MemoryHistogram diff = getDiff(() -> {
            ArrayList<String> lst = new ArrayList<>();
            for (int i = 0; i < 20000; i++) {
                lst.add("" + i);
            }
            return lst;
        });
        MemoryHistogram domain = diff.filter(ArrayList.class, "Object[]", "String");
        assertTrue(domain.get("java.util.ArrayList").getInstances() >= 1);
        assertTrue(domain.get("String").getInstances() >= 20000);
        assertTrue(domain.get("Object[]").getSize() >= 20000 * 4);
        System.out.println(domain);
    }

    @Test
    public void compositeTest() {
        Random rnd = new Random(0);
        MemoryHistogram diff = getDiff(() -> {
            ArrayList<SomeStructure> root = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                OtherStructure v1 = new OtherStructure();
                v1.age = rnd.nextInt(99);
                v1.name = toHexString(rnd.nextInt(1000));
                OtherStructure v2 = new OtherStructure();
                v2.age = rnd.nextInt(32);
                v2.name = toHexString(rnd.nextInt(100));
                SomeStructure some = new SomeStructure();
                some.name = toHexString(rnd.nextInt(30000));
                some.value = v1;
                some.other = v2;
                root.add(some);
            }
            return root;
        });
        System.out.println(diff);
        assertEquals(100, diff.get("com.jerolba.mnemotest.HistogramerTest$SomeStructure").getInstances());
        assertEquals(200, diff.get("com.jerolba.mnemotest.HistogramerTest$OtherStructure").getInstances());
        assertTrue(diff.get("String").getInstances() >= 100 + 100 + 100);
        assertTrue(diff.get("java.util.ArrayList").getInstances() >= 1);
        assertTrue(diff.get("Object[]").getInstances() >= 1);
    }

    private static class SomeStructure {

        private String name;
        private OtherStructure value;
        private OtherStructure other;

    }

    private static class OtherStructure {
        private String name;
        private int age;
    }
}
