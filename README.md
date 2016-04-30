# SI-2712-fix plugin

This is proof of concept fix to [SI-2712][si2712] implemented as a compiler plugin. There is also a [PR][si2712pr] to
fix this directly in scalac.

The implementation is based on a simple algorithm as suggested by Paul Chiusano in the comments on [SI-2712][si2712]:
Treat the type constructor as curried and partially applied, we treat a prefix as constants and solve for the suffix.
For the example in the ticket, unifying `M[A]` with `Int => Int`, this unifies as,

```Scala
M[t] = [t][Int => t]
A = Int
```

More detailed explanations can also be found at this gist [Explaining Miles's Magic][explain] by @djspiewak and the
readme of the [demo project][demo].

One place to discuss this fix is at [typelevel's gitter room](https://gitter.im/typelevel/general).

## Usage

Binary release artefacts are published to the [Sonatype OSS Repository Hosting service][sonatype] and synced to Maven
Central. To use the plugin in your project add the following to its sbt build file,

```scala
addCompilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.0.1" cross CrossVersion.full)

```

If you intended to use the `@unifyRightToLeft` annotation to enable right-to-left unify rule please add the following
as well

```scala
libraryDependencies += "com.milessabin" % "si2712fix-library" % "1.0.1" cross CrossVersion.full
```

More context about right to left rule can be found in this [issue comment][right-left].

## Examples

An example project can be found at [milessabin/si2712fix-demo][demo]

## Caveats

Please note that currently this plugin and the [Macro Paradise plugin][macroparadise] are incompatible because they
both hook into the Scala compiler via the same exclusive mechanism. The two projects are working on this problem and
hope to have a solution by the time of Scala Days ... watch this space!

## Participation

This project supports the [Typelevel][typelevel] [code of conduct][codeofconduct] and wants all of its
channels (Gitter, github, etc.) to be welcoming environments for everyone.

[typelevel]: http://typelevel.org/
[codeofconduct]: http://typelevel.org/conduct.html

## Building kittens

kittens is built with SBT 0.13.9 or later, and its master branch is built with Scala 2.11.7 by default.

## Contributors

+ Adelbert Chang <adelbertc@gmail.com> [@adelbertchang](https://twitter.com/adelbertchang)
+ Kailuo Wang <kailuo.wang@gmail.com> [@kailuowang](https://twitter.com/kailuowang)
+ Miles Sabin <miles@milessabin.com> [@milessabin](https://twitter.com/milessabin)
+ Your name here :-)

[si2712]: https://issues.scala-lang.org/browse/SI-2712
[si2712pr]: https://github.com/scala/scala/pull/5102
[explain]: https://gist.github.com/djspiewak/7a81a395c461fd3a09a6941d4cd040f2
[demo]: https://github.com/milessabin/si2712fix-demo/tree/plugin-based
[right-left]: https://github.com/scala/scala/pull/5102#issuecomment-211140311
[sonatype]: https://oss.sonatype.org/index.html#nexus-search;quick~si2712fix-plugin
[macroparadise]: http://docs.scala-lang.org/overviews/macros/paradise.html
