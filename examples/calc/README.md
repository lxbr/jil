This project implements a trivial postfix calculator.

It was built with the following workflow:

1. Write a unit test in Clojure.
2. Implement the missing functionality in Java.
3. Load the Java source files.
4. Run tests. If successful goto 1. else goto 2.

**How did `JIL` help?**

* changes to the source code can be tested quickly at the REPL and via
  clojure.test
* recompilation is fast because the compiler runs in the REPL process
  and is not initialized more than once
* Java compiler errors and warnings are shown in the REPL
* less fear of using `java.util.HashMap` instead of custom class,
  since type safety is less important when you can prove your code
  to be correct at the REPL
* Clojure can be used to help development, e.g. via clojure.test,
  standard library, test.check, tools from 
  [clojure-goes-fast](https://github.com/clojure-goes-fast)
* one can implement a custom REPL on top of the default Clojure
  REPL to exercise the runtime behavior of a system 
  (see the `user` namespace)


