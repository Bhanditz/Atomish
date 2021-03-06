Atomish
=======

A small (in size, scope, and memory footprint), reasonably fast language for writing self contained executables.


Ur-Principle: separating and reifying readers, evallers, printers, runloops, universes, and mirrors.


Goals
-----

Able to compile programs as statically linked executables.
Able to compile programs as dynamically linked executables.
Able to compile to llvm IL, and then do a final compile and link on a target system.

Able to only pay for what you use, a la the ideal of C++. Several methods to achieve this:
- A sane, parametric module system, so you do not end up with a giant permgen, and you can specialise sanely.
- A metasyntactic evaluator, so you can swap the reader and evaluator functions in and out.
- An MMOP that lets you 'trim' the environment for subexpressions, to make late binding dynamic dispatch faster.
- Use of rewriting macros rather than pragmas for optimisation - ie ua{z+x\*y} -> a function that uses raw arithmetic on raw numbers rather than symbolic arithmetic on bignums
- Eventually, the use of twine, ropes and so forth for string manipulation, without needing cumbersome builders.
- Compiled functions with holes for *fast* templates.

No C or C++ anywhere (except in llvm, the rt\_\* libraries...)

Good enough FFI by way of bridges, alien cells, and alien evaluators.

This is all supposed to be a replacement for the places I use Ioke, but need something faster with a smaller memory footprint, like as a web app on a small VPS.

Inspired by Cola/Pepsi/Idst/Maru, Ioke, Common Lisp and Haskell.

Other influences: Perl 5&6 (Guts on the outside, TMTWTDI, Moose, 6Model), Io (Mirrors), OCaml (module system), Potion.

Minor influences: Atomy, Atomo, Slate, Erlang, Snobol, MetaLua.


Is it any good?
---------------

At the moment, no. The bootstrap pre-interpreter can run some basic pre-atomish code, but this is still mostly my pipe dreams.

Project Organisation
--------------------

Each subfolder has it's own README.md detailing relevant things.

The project is dual licensed under the GPLv3 and Death and Repudiation licences, which are available in the file LICENCE. Vendored files (in `bootstrap/vendor`) are available under their own licences, which are listed at the top of each file.

Installing instructions are in INSTALL.md

General project layout explanations are in LAYOUT.md

My assorted noodling and ill-considered thoughts at any given time about the project are in scratch.md

General syntax and semantics of the language Atomish are in atomish.md

