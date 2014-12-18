Javacx - Introduction
---------------------

Javacx is **not** a preprocessor : it is a *tool* to allow you to easily use preprocessors with Java.

Here is a quick example :

~~~~ {.shell}
javacx -javacx:cpp mystuff.java
~~~~

This compiles mystuff.java by first preprocessing the source ( and *any* source files the compiler accesses, listed on the command line or not ). It's all done behind the scenes and transparently. No temporary files are created.

If you've never used a preprocessor with Java or not wanted to, this may be an opportunity to try them out. Preprocessors are powerful tools for the programmer. For programmers like me who grew up on C, not having support for preprocessing is like loosing an old friend ( albeit a fussy old friend ).

The Java compiler has no support for preprocesing of source files. Like many programmers I find myself wanting to use macros ( particularly C style macros ) when I use Java. With the standard compiler command ( javac ) I cannot do that easily, so I wrote **javacx** to support preprocessing.

The problem is that when you compile Java source files the compiler searches for other source files it needs. To preprocess these you must copy the files to a build directory and preprocess them there. This can become tedious, especially if you like to use more than one preprocessor. It compicates teh build process and should not.

Javacx uses the JavaCompiler and related classes which were introduced to expose limited compiler functionality. Basicaly it intercepts sources files ( that is .java files ) when the java compiler finds them and processes them according to commands you give on the command line.

As it happens the standard C preprocessor is pretty well suited to Java, as the syntax of the two langauages is "close enough". I've never had any problems with it. For this reason one simple option is *-javacx:cpp* to use the C preprocessor with the options "-P -C -nostdinc -", which is to pass comments through, not add line numbers ( which would confuse the Java compiler ), ignore the standard C include paths when searching for an include file and to accept input from stdin. By default it sends output to stdout. All preprocessors used with javacx should take input from stadin and send output to stdout.

By default javacx does no preprocessing. With the *javax:cpp* option it uses the standard C preprocessor ( which of course you need to have installed ) and for more general use you can use -javacx:cmd <command>, Developers will be aware that they probably need to take action to avoid the cammand being split by the shell into seperate arguments ( e.g. use quotes around the command ).
