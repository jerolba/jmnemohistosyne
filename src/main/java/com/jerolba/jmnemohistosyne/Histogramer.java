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

import static java.lang.Long.parseLong;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.jerolba.jmnemohistosyne.MemoryHistogram.SimpleMap;

public class Histogramer {

    private static final String PACKAGE_NAME = Histogramer.class.getPackage().getName() + ".";

    private static final SimpleMap<String, String> ALIASES = new SimpleMap<>();
    static {
        ALIASES.put("[J", "long[]");
        ALIASES.put("[I", "int[]");
        ALIASES.put("[B", "byte[]");
        ALIASES.put("[C", "char[]");
        ALIASES.put("[S", "short[]");
        ALIASES.put("[F", "float[]");
        ALIASES.put("[D", "double[]");
        ALIASES.put("[Z", "boolean[]");
    }

    /**
     * Calculates the memory consumed by runnable code as difference of obects that
     * exists before and after code execution. To avoid GC over measured objects
     * before Histogram execution, supplied code must return a reference to a root
     * instance which reference to all measurable instances.
     */
    public static <T> MemoryHistogram getDiff(Supplier<T> code) {
        Histogramer histogramer = new Histogramer();
        MemoryHistogram reference = histogramer.createHistogram();
        T value = code.get();
        MemoryHistogram current = histogramer.createHistogram();
        value.getClass();
        return current.diff(reference);
    }

    /**
     * Creates a memory histogram of the current process
     */
    public MemoryHistogram createHistogram() {
        List<String> commandOutput = runJcmd();
        MemoryHistogram histogram = new MemoryHistogram();
        int[] columns = locatecolumns(commandOutput);
        int cont = 0;
        while (!commandOutput.get(cont).startsWith("--")) {
            cont++;
        }
        commandOutput.stream().skip(cont + 1).forEach(str -> {
            String instances = str.substring(columns[0], columns[1]).trim();
            String bytes = str.substring(columns[2], columns[3]).trim();
            String className = str.substring(columns[3]).trim();
            if (!isThisLibraryCode(className) && !isTotalsLine(className)) {
                histogram.add(new HistogramEntry(translateName(className), parseLong(instances), parseLong(bytes)));
            }
        });
        return histogram;
    }

    private boolean isThisLibraryCode(String className) {
        return className.startsWith(PACKAGE_NAME);
    }

    private boolean isTotalsLine(String className) {
        return className.length() == 0;
    }

    /**
     * Given the output from jmap command, locate the character position of each
     * data column
     */
    private int[] locatecolumns(List<String> commandOutput) {
        String line = commandOutput.get(3);
        int idxDots = line.indexOf(":");
        int idxInst = endChar(line, idxDots + 1);
        int idxBytes = endChar(line, idxInst + 1);
        int[] res = new int[5];
        res[0] = idxDots + 1;
        res[1] = idxInst;
        res[2] = idxInst + 1;
        res[3] = idxBytes;
        res[4] = idxBytes + 1;
        return res;
    }

    /**
     * Locate the next end char of jmap header line
     */
    private int endChar(String line, int begin) {
        int it = begin;
        while (line.charAt(it) == ' ') {
            it++;
        }
        while (line.charAt(it) != ' ') {
            it++;
        }
        return it;
    }

    /**
     * Executes the jmap command with live parameter to guarantee GC execution. It's
     * expected to be in path.
     */
    private List<String> runJcmd() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            String PID = name.substring(0, name.indexOf("@"));
            Process p = Runtime.getRuntime().exec("jcmd " + PID + " GC.class_histogram");
            try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                return input.lines().collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Transforms class names to readable format: - Primivite arrays - Objects
     * arrays - java.lang objects
     *
     * @param className
     * @return
     */
    private String translateName(String className) {
        String name = removeJavaBase(className);
        if (ALIASES.containsKey(name)) {
            return ALIASES.get(name);
        }
        if (name.startsWith("[") && name.endsWith(";")) {
            int cont = 1;
            while (name.charAt(cont) == '[') {
                cont++;
            }
            name = name.substring(cont + 1, name.length() - 1);
            for (int i = 0; i < cont; i++) {
                name += "[]";
            }
        }
        return reduceName(name);
    }

    private final static String JAVA_LANG = "java.lang.";
    private final static String JAVA_BASE = " (java.base@";

    private String removeJavaBase(String className) {
        int idx = className.indexOf(JAVA_BASE);
        if (idx > 0) {
            return className.substring(0, idx);
        }
        return className;
    }

    private String reduceName(String className) {
        if (className.startsWith(JAVA_LANG)) {
            String reduced = className.substring(JAVA_LANG.length());
            if (reduced.indexOf(".") == -1 && reduced.indexOf("$") == -1) {
                return reduced;
            }
        }
        return className;
    }

}
