# JIL

Java class compilation and loading from the REPL.

## The Problem

In Clojure we often have code and a REPL side by side, evaluating forms or files
frequently. In Java we are still stuck with the edit, compile, run cycle which
makes it harder to iterate quickly. When we need to drop down to Java for either
performance or extensive use of mutability it is hard to integrate it into our
otherwise dynamic development process.

How can the process of writing Java source code benefit from a REPL?

## Wishful Thinking

What if...

* we could load Java source files just like Clojure source files, 
  individually and on the fly?
* we could try some Java code at the REPL, change it, load it, try again, 
  just like in Clojure?
* we could mix Java and Clojure more easily in the same project?
* we could develop pure Java projects at the REPL?

Development is about failing fast. The faster you fail, the faster you write
something that works. And nothing fails as fast as bad code at the REPL.

## Inspiration

This library is inspired by [virgil](https://github.com/ztellman/virgil).
It differs from `virgil` in the following ways:

* no filesystem watcher that compiles changed source code files automatically
* source code files must be compiled explicitly via `load-{file,files,dir}` functions
* the workaround for issue https://github.com/clojure-emacs/cider-nrepl/issues/463
  is not applied because of worse compilation performance and the fact that the author
  does not use `cider`; if you do, please refer to the aforementioned issue
* no integration with `leiningen` or `boot`

## Some Advice

**Watch out, Classloader caveats apply!**

Class loading is far from trivial. When you experience a problem that looks rather exotic,
it most likely is due to a class linking issue. To avoid that, please follow these rules:

* when recompiling one class, every class that depends on it needs to be recompiled as well
* organize your Java code the way you would organize it in Clojure
  * prefer static methods to instance methods
  * prefer stateless data to stateful objects
  * minimize coupling
* try to use java.util.* classes instead of custom data holder classes
* push side effects to the edges of your system
* use interfaces for public and private APIs, classes calling classes is more brittle
  than classes calling interfaces

The intended use case for `JIL` is either calling the `load-*` functions at the REPL
or integrating those functions via key bindings into your Editor/IDE. Tools that
alter the classpath at runtime can cause problems with class compilation and loading.
Therefore, the more lightweight your development environment is, the better.

`JIL` has been developed on Java 1.8 and may or may not work with newer versions.

## License

This project is licensed under the terms of the Eclipse Public License 1.0 (EPL).

This project uses the ASM bytecode engineering library which is distributed
with the following notice:

```
ASM: a very small and fast Java bytecode manipulation framework
Copyright (c) 2000-2011 INRIA, France Telecom
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the copyright holders nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
THE POSSIBILITY OF SUCH DAMAGE.
```
