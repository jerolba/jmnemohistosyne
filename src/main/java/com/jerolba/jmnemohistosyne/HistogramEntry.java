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

public class HistogramEntry implements Comparable<HistogramEntry> {

    private String className;
    private long instances;
    private long size;

    public HistogramEntry(String className, long instances, long size) {
        this.instances = instances;
        this.size = size;
        this.className = className;
    }

    public long getInstances() {
        return instances;
    }

    public long getSize() {
        return size;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return className + "," + instances + "," + size;
    }

    @Override
    public int compareTo(HistogramEntry o) {
        return (int) (o.size - this.size);
    }

}