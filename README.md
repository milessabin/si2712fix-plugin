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

**NOTE: This plugin should only be used by users still on Scala 2.10.x.**

Scala 2.10.6 users can add the plugin with the following SBT command:

```scala
addCompilerPlugin("com.milessabin" % "si2712fix-plugin_2.10.6" % "1.2.0")

// or
libraryDependencies += compilerPlugin("com.milessabin" % "si2712fix-plugin_2.10.6" % "1.2.0")
```

Scala 2.12.x users can get the fix by turning on the `-Ypartial-unification` compiler flag.

```scala
scalacOptions += "-Ypartial-unification"
```

Users on Scala 2.11.x should use [Typelevel Scala][tls] with the same flag, or wait for Scala 2.11.9 which
has the [backport][backport].

More context about right to left rule can be found in this [issue comment][right-left].

## Examples

An example project can be found at [milessabin/si2712fix-demo][demo]

## Projects that use this plugin

+ [asobu](https://github.com/iheartradio/asobu)
+ [eff-cats](https://github.com/atnos-org/eff-cats)
+ [eff-scalaz](https://github.com/atnos-org/eff-scalaz)
+ [kittens](https://github.com/milessabin/kittens)
+ [Matryoshka](https://github.com/slamdata/matryoshka)
+ [Quasar](https://github.com/quasar-analytics/quasar)

## Participation

This project supports the [Typelevel][typelevel] [code of conduct][codeofconduct] and wants all of its
channels (Gitter, github, etc.) to be welcoming environments for everyone.

## Building the plugin

This plugin is built with SBT 0.13.11 or later, and its master branch is built with Scala 2.11.8 and 2.10.6 by
default.

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
[typelevel]: http://typelevel.org/
[codeofconduct]: http://typelevel.org/conduct.html
[tls]: https://github.com/typelevel/scala
[backport]: https://github.com/scala/scala/pull/5343
