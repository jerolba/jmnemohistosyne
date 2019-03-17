[![Maven Central](https://img.shields.io/maven-central/v/com.jerolba/jmnemohistosyne.svg)](https://maven-badges.herokuapp.com/maven-central/com.jerolba/jmnemohistosyne)
[![Build Status](https://circleci.com/gh/jerolba/jmnemohistosyne.svg?style=shield)](https://circleci.com/gh/jerolba/jmnemohistosyne) 
[![Download](https://api.bintray.com/packages/jerolba/maven/jmnemohistosyne/images/download.svg)](https://bintray.com/jerolba/maven/jmnemohistosyne/_latestVersion)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Codecov](https://codecov.io/gh/jerolba/jmnemohistosyne/branch/master/graph/badge.svg)](https://codecov.io/gh/jerolba/jmnemohistosyne/)


# JMnemohistosyne

JMnemohistosyne executes programmatically memory histogram of current process using available system `jcmd` command. Parses its output to get the number of instantiated classes and memory consumption of each class.

JMnemohistosyne uses the `jmd` command with the `GC.class_histogram` flag, that forces the execution of a full garbage collection. Remember that `jcmd` command **is only available in JDK**, not in JRE.

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

## Memory histogram difference in code execution

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
MemoryHistogram filterd = histogram.filter("Object[]", "java.util.HashMap*", ArrayList.class, Pattern.compile(".*Hibernate.*"));
```

All options are applied to the histogram and joined in a new `MemoryHistogram`. 

## Dependency

JMnemohistosyne is uploaded to Maven Central Repository and to use it, you need to add the following Maven dependency:

```xml
<dependency>
  <groupId>com.jerolba</groupId>
  <artifactId>jmnemohistosyne</artifactId>
  <version>0.2.3</version>
</dependency>
```

in Gralde:

`implementation 'com.jerolba:jmnemohistosyne:0.2.3'`

or download the single [jar](http://central.maven.org/maven2/com/jerolba/jmnemohistosyne/0.2.3/jmnemohistosyne-0.2.3.jar) from Maven Central Repository.

### JDK

JMnemohistosyne depends on the presence of the `jcmd` command in the path. Then, check that the JDK is installed and is accesible from command line.

Currently it is tested in the [CI system](https://circleci.com/gh/jerolba/jmnemohistosyne) against: Oracle JDK 8, OpenJDK 8 and OpendJDK 11. 

## Contribute
Feel free to dive in! [Open an issue](https://github.com/jerolba/jmnemohistosyne/issues/new) or submit PRs.

Any contributor and maintainer of this project follows the [Contributor Covenant Code of Conduct](https://github.com/jerolba/jmnemohistosyne/blob/master/CODE_OF_CONDUCT.md).

## License
[Apache 2](https://github.com/jerolba/jmnemohistosyne/blob/master/LICENSE.txt) © Jerónimo López