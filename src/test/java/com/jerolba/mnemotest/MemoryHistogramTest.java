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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.jerolba.jmnemohistosyne.HistogramEntry;
import com.jerolba.jmnemohistosyne.MemoryHistogram;

public class MemoryHistogramTest {

    public MemoryHistogram sut = new MemoryHistogram();

    private HistogramEntry fooEntry = new HistogramEntry("java.util.Foo", 100, 2400);
    private HistogramEntry barEntry = new HistogramEntry("java.util.Bar", 50, 1000);
    private HistogramEntry javaLangEntry = new HistogramEntry("java.lang.Baz", 10, 100);
    private HistogramEntry arrayListEntry = new HistogramEntry(ArrayList.class.getName(), 1, 48);

    @Test
    public void canAddAndFindEntry() {
        sut.add(fooEntry);
        assertNull(sut.get("java.util.Bar"));
        assertNotNull(sut.get("java.util.Foo"));
    }

    @Test
    public void doesntAllowDuplicates() {
        sut.add(fooEntry);
        sut.add(new HistogramEntry("java.util.Foo", 110, 2600));
        Iterator<HistogramEntry> it = sut.iterator();
        assertTrue(it.hasNext());
        it.next();
        assertFalse(it.hasNext());
    }

    @Nested
    class Filtering {

        @BeforeEach
        void beforeEachTest() {
            sut.add(fooEntry);
            sut.add(barEntry);
            sut.add(javaLangEntry);
            sut.add(arrayListEntry);
        }

        @Test
        public void canFilterByName() {
            MemoryHistogram filter = sut.filter("java.util.Foo");
            assertNotNull(filter.get("java.util.Foo"));
            assertNull(filter.get("java.util.Bar"));
        }

        @Test
        public void canFilterByNameWildcard() {
            MemoryHistogram filter = sut.filter("java.util.*");
            assertNotNull(filter.get("java.util.Foo"));
            assertNotNull(filter.get("java.util.Bar"));
            assertNull(filter.get("java.lang.Baz"));
        }

        @Test
        public void canFilterByClass() {
            MemoryHistogram filter = sut.filter(ArrayList.class);
            assertNotNull(filter.get(ArrayList.class.getName()));
            assertNull(filter.get("java.util.Foo"));
            assertNull(filter.get("java.util.Bar"));
        }

        @Test
        public void canFilterByPattern() {
            MemoryHistogram filter = sut.filter(Pattern.compile(".*Foo"));
            assertNotNull(filter.get("java.util.Foo"));
            assertNull(filter.get("java.util.Bar"));
        }

        @Test
        public void unsuportedCriteriaRaiseException() {
            assertThrows(UnsupportedOperationException.class, () -> {
                sut.filter(10);
            });
        }
    }

    @Test
    public void canCalculateTotalMemory() {
        sut.add(fooEntry);
        sut.add(barEntry);
        sut.add(javaLangEntry);
        assertEquals(2400 + 1000 + 100, sut.getTotalMemory());
        assertEquals(2400 + 1000, sut.filter("java.util.*").getTotalMemory());
    }

    @Test
    public void canIterateContent() {
        sut.add(fooEntry);
        sut.add(barEntry);
        Iterator<HistogramEntry> it = sut.iterator();
        assertTrue(it.hasNext());
        assertEquals("java.util.Foo", it.next().getClassName());
        assertTrue(it.hasNext());
        assertEquals("java.util.Bar", it.next().getClassName());
        assertFalse(it.hasNext());
    }

    @Test
    public void canBeStreamed() {
        sut.add(fooEntry);
        sut.add(barEntry);
        sut.add(javaLangEntry);
        assertEquals(2, sut.stream().filter(e -> e.getClassName().startsWith("java.util.")).count());
    }

    @Nested
    class Diff {

        private MemoryHistogram reference = new MemoryHistogram();

        @BeforeEach
        void beforeEachTest() {
            sut.add(fooEntry);
            sut.add(barEntry);
            sut.add(javaLangEntry);
            sut.add(arrayListEntry);

            reference.add(new HistogramEntry("java.util.Foo", 40, 960));
            reference.add(new HistogramEntry("java.util.Bar", 49, 980));
            reference.add(new HistogramEntry("java.lang.Baz", 1, 10));
            reference.add(new HistogramEntry("java.util.ArrayList", 1, 48));
        }

        @Test
        public void canDiffValues() {
            MemoryHistogram diff = sut.diff(reference);
            assertEquals(1440, diff.get("java.util.Foo").getSize());
            assertEquals(1, diff.get("java.util.Bar").getInstances());
            assertEquals(90, diff.get("java.lang.Baz").getSize());
            assertNull(diff.get("java.util.ArrayList"));
        }

        @Test
        void diffIsSortedBySize() {
            MemoryHistogram diff = sut.diff(reference);
            Iterator<HistogramEntry> iterator = diff.iterator();
            assertEquals("java.util.Foo", iterator.next().getClassName());
            assertEquals("java.lang.Baz", iterator.next().getClassName());
            assertEquals("java.util.Bar", iterator.next().getClassName());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void topFilterFirstItems() {
        sut.add(fooEntry);
        sut.add(barEntry);
        sut.add(javaLangEntry);
        sut.add(arrayListEntry);
        MemoryHistogram top = sut.getTop(3);
        Iterator<HistogramEntry> iterator = top.iterator();
        assertEquals("java.util.Foo", iterator.next().getClassName());
        assertEquals("java.util.Bar", iterator.next().getClassName());
        assertEquals("java.lang.Baz", iterator.next().getClassName());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void topFilterCanBeGreaterThanAvailableItems() {
        sut.add(fooEntry);
        sut.add(barEntry);
        sut.add(javaLangEntry);
        MemoryHistogram top = sut.getTop(10);
        Iterator<HistogramEntry> iterator = top.iterator();
        assertEquals("java.util.Foo", iterator.next().getClassName());
        assertEquals("java.util.Bar", iterator.next().getClassName());
        assertEquals("java.lang.Baz", iterator.next().getClassName());
        assertFalse(iterator.hasNext());
    }

}
