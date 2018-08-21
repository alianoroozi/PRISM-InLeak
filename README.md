# About
**PRISM-InterLeak** is a tool to compute **information leakage** of concurrent probabilistic programs. It takes as input a program written in the [PRISM language](http://www.prismmodelchecker.org/manual/ThePRISMLanguage/Introduction) and analyzes the information leakage of the program. It computes various types of information leakage, including expected, minimum, maximum, bounded time, etc. 

The tool is built upon the [PRISM model checker](http://www.prismmodelchecker.org/). PRISM compiles a program written in the PRISM language, builds a discrete-time Markov chain model of the program and stores it using BDDs (Binary Decision Diagrams) and MTBDDs (Multi-Terminal Binary Decision Diagrams). PRISM-InterLeak uses these data structures to extract the set of reachable states and also create a sparse matrix containing the transitions. It then employs a depth-first path exploration algorithm to find all paths and traces of the program. For each trace, it computes the probability of the trace and the posterior entropy of the secret induced by the trace. Using these values, PRISM-InterLeak computes the *exact* values for each variant of the information leakage. 

A main difference of PRISM-InterLeak and other related leakage quantification tools is that PRISM-InterLeak takes into account *intermediate leakages*. This is suitable for *concurrent* programs, in which the attacker is able to observe intermediate values of publicly observable variables. 

# Installation
Compiling:
```console
cd prism-interleak
make
```


# Usage

```console
cd prism-interleak/bin
prism  -interleak [options] <model-file> [more-options]
```

Options:
========
```console
-min
```
Compute the expected leakage using min-entropy

```console
-shannon
```
Compute the expected leakage using Shannon entropy. The default is Shannon entropy

```console
-leakbounds
```
Compute maximum and minimum leakages, which are upper and lower leakage bounds for an attacker with a given prior knowledge about the secret input

```console
-bounded <n>
```  
Compute bounded time leakage, which is the amount of expected leakage at a given time (step)

```console
-initdist <file>
```
Specify the initial probability distribution of the secret input. If not specified, the uniform distribution is assumed

```console
-help | -h | -?
```
Display this help message

```console
-prismhelp 
```
Display PRISM help message

```console
-version 
```
Display PRISM-InterLeak and PRISM version info


# People
The people currently working on the tool are:

[Ali A. Noroozi](https://alianoroozi.github.io), currently a Ph.D. student at [University of Tabriz](http://tabrizu.ac.ir/en) and lead developer of the project,

Khayyam Salehi, currently a Ph.D. student at University of Tabriz and developer of the project,

[Jaber Karimpour](http://simap.tabrizu.ac.ir/cv/karimpour/?lang=en-gb), an associate professor at University of Tabriz and supervisor of the project,

[Ayaz Isazadeh](http://isazadeh.net/ayaz), a professor at University of Tabriz and supervisor of the project.

