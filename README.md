[ ![Download](https://api.bintray.com/packages/jerolba/maven/jmnemohistosyne/images/download.svg) ](https://bintray.com/jerolba/maven/jmnemohistosyne/_latestVersion)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

# JMnemohistosyne

JMnemohistosyne executes memory histogram of current process using available system `jcmd` command. Parses its output to get the number of instantiated classes and memory consumption of each class.

JMnemohistosyne uses the `jmd` command with the `GC.class_histogram` flag, that forces the execution of a full garbage collection. Remember that `jcmd` command is only available in JDK, not in JRE.

Full garbage collection and memory histogram are expensive operations. It is not recommendable to run JMnemohistosyne in production.

Only live objects are profiled and it inspects all objects in heap, used by your own code or by the JVM.

## Current memory histogram

To know the current memory consumption you need to use Histogramer class:

```java
Histogramer histogramer = new Histogramer();
MemoryHistogram histogram = histogramer.createHistogram();
System.out.println(histogram);
```

All objects in memory are counted, created directly or indirectly by your code or by the JVM in its internal operations.

## Memory histogram difference in conde execution

You can profile newly created objects in a code section wrapping it in a labmda:

```java
MemoryHistogram diff = Histogramer.getDiff(() -> {
    HashMap<Integer, String> map = new HashMap<>();
    for (int i = 0; i < 10000; i++) {
        map.put(i, "" + i);
    }
    return map;
});
```

Histogramer will take a snapshot of memory histogram before executing lambda code. After executing your code it creates other histogram and calculates the difference between both histograms.
Profiled code needs to return a reference to an object which contains all the objects that needs to be measured to avoid its deallocation before the histogram is executed.

## Filtering

`MemoryHistogram` class is an iterable collection of `HistogramEntry` objects which contains: class name, number of instances and size of all instancess.

A `MemoryHistogram` can be filtered using the `filter` method with a varargs of:

- Class name including package: `java.util.HashMap`
- Class name with a final wildcard: `java.util.HashMap*`
- Class object: `HashMap.class`
- Regular expression patter over complete class name: `Pattern.compile(".*List")`

```java
MemoryHistogram filterd = histogram.filter("java.util.HashMap*", "Objec[]", Pattern.compile(".*Hibernate.*"));
```

All options are applied to the histogram and joined in a new `MemoryHistogram`. 

## Contribute
Feel free to dive in! [Open an issue](https://github.com/jerolba/jmnemohistosyne/issues/new) or submit PRs.

Any contributor and maintainer of this project follows the [Contributor Covenant Code of Conduct](https://github.com/jerolba/jmnemohistosyne/blob/master/CODE_OF_CONDUCT.md).

## License
[Apache 2](https://github.com/jerolba/jmnemohistosyne/blob/master/LICENSE.txt) © Jerónimo López