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
package com.jerolba.jmnemohistosyne;

import static java.util.stream.Collectors.joining;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Contains a histogram from a set of classes. Mantains insertion order and can
 * be accessed by class name
 *
 */
public class MemoryHistogram implements Iterable<HistogramEntry> {

    private final SimpleMap<String, HistogramEntry> map = new SimpleMap<>();

    public MemoryHistogram() {
    }

    private MemoryHistogram(Collection<HistogramEntry> all) {
        for (HistogramEntry entry : all) {
            this.add(entry);
        }
    }

    public void add(HistogramEntry it) {
        map.put(it.getClassName(), it);
    }

    public HistogramEntry get(String className) {
        return map.get(className);
    }

    public MemoryHistogram filter(Object... orCriterias) {
        List<HistogramEntry> ac = new ArrayList<>();
        for (Object criteria : orCriterias) {
            for (HistogramEntry entry : findCriteria(criteria)) {
                ac.add(entry);
            }
        }
        return sortedHistogram(ac);
    }

    private MemoryHistogram findCriteria(Object criteria) {
        if (criteria instanceof Class<?>) {
            return findClass(((Class<?>) criteria));
        }
        if (criteria instanceof Pattern) {
            return findPattern((Pattern) criteria);
        }
        if (criteria instanceof String) {
            return findString((String) criteria);
        }
        throw new UnsupportedOperationException(criteria.getClass().getName() + " type not supported");
    }

    private MemoryHistogram findClass(Class<?> criteria) {
        String className = criteria.getName();
        MemoryHistogram res = new MemoryHistogram();
        HistogramEntry entry = get(className);
        if (entry != null) {
            res.add(entry);
        }
        return res;
    }

    private MemoryHistogram findString(String str) {
        MemoryHistogram res = new MemoryHistogram();
        if (str.endsWith("*")) {
            String match = str.substring(0, str.length() - 1);
            for (HistogramEntry entry : map) {
                if (entry.getClassName().startsWith(match)) {
                    res.add(entry);
                }
            }
            return res;
        }
        HistogramEntry entry = get(str);
        if (entry != null) {
            res.add(entry);
        }
        return res;
    }

    private MemoryHistogram findPattern(Pattern pattern) {
        MemoryHistogram res = new MemoryHistogram();
        for (HistogramEntry entry : map) {
            Matcher matcher = pattern.matcher(entry.getClassName());
            if (matcher.find()) {
                res.add(entry);
            }
        }
        return res;
    }

    public long getTotalMemory() {
        long ac = 0;
        for (HistogramEntry e : map) {
            ac += e.getSize();
        }
        return ac;
    }

    public List<String> values() {
        List<String> values = new ArrayList<>();
        for (HistogramEntry histogramEntry : map) {
            values.add(histogramEntry.toString());
        }
        return values;
    }

    @Override
    public String toString() {
        return "class,instances,size\n" + values().stream().collect(joining("\n"));
    }

    @Override
    public Iterator<HistogramEntry> iterator() {
        return map.iterator();
    }

    public Stream<HistogramEntry> stream() {
        return map.stream();
    }

    /**
     * Creates a new MemoryHistogram with the difference between this instance and
     * the reference one. For each class calculates the difference of instances and
     * consumed bytes.
     */
    public MemoryHistogram diff(MemoryHistogram reference) {
        List<HistogramEntry> all = new ArrayList<>();
        for (HistogramEntry entry : map) {
            HistogramEntry refEntry = reference.map.get(entry.getClassName());
            if (refEntry != null) {
                long size = entry.getSize() - refEntry.getSize();
                if (size != 0) {
                    all.add(new HistogramEntry(entry.getClassName(), entry.getInstances() - refEntry.getInstances(),
                            size));
                }
            } else {
                all.add(entry);
            }
        }
        for (HistogramEntry ref : reference.map) {
            HistogramEntry thisEntry = map.get(ref.getClassName());
            if (thisEntry == null) {
                all.add(new HistogramEntry(ref.getClassName(), -ref.getInstances(), -ref.getSize()));
            }
        }
        return sortedHistogram(all);
    }

    private MemoryHistogram sortedHistogram(List<HistogramEntry> all) {
        Collections.sort(all);
        return new MemoryHistogram(all);
    }

    /**
     * Return the top N entries of the histogram.
     */
    public MemoryHistogram getTop(int top) {
        int cont = 0;
        MemoryHistogram ac = new MemoryHistogram();
        for (HistogramEntry e : this) {
            if (++cont > top) {
                return ac;
            }
            ac.add(e);
        }
        return ac;
    }

    /**
     * Map like implementation avoiding use of HashMap or LinkedHashMap and pollute
     * heap with instances of objects we want to measure. It's a linked list of
     * HistMapNode, which mantains insert order. Put and get has O(n) complexity.
     */
    public static class SimpleMap<K, V> implements Iterable<V> {

        private SimpleMapNode<K, V> root;
        private SimpleMapNode<K, V> last;

        public void put(K key, V value) {
            if (root == null) {
                root = new SimpleMapNode<>(key, value);
                last = root;
            } else {
                SimpleMapNode<K, V> find = find(key);
                if (find == null) {
                    SimpleMapNode<K, V> newOne = new SimpleMapNode<>(key, value);
                    last.addNext(newOne);
                    last = newOne;
                } else {
                    find.value = value;
                }
            }
        }

        public V get(K key) {
            SimpleMapNode<K, V> find = find(key);
            if (find != null) {
                return find.value;
            }
            return null;
        }

        public boolean containsKey(K key) {
            return find(key) != null;
        }

        private SimpleMapNode<K, V> find(K key) {
            SimpleMapNode<K, V> it = root;
            while (it != null) {
                if (it.key.equals(key)) {
                    return it;
                }
                it = it.next;
            }
            return null;
        }

        @Override
        public Iterator<V> iterator() {
            return new MemoryHistogramIterator();
        }

        public Stream<V> stream() {
            return StreamSupport.stream(spliterator(), false);
        }

        private class MemoryHistogramIterator implements Iterator<V> {

            SimpleMapNode<K, V> current = root;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public V next() {
                V value = current.value;
                current = current.next;
                return value;
            }

        }

    }

    private static class SimpleMapNode<K, V> {

        private K key;
        private V value;
        private SimpleMapNode<K, V> next;

        SimpleMapNode(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public void addNext(SimpleMapNode<K, V> next) {
            this.next = next;
        }

    }

}
