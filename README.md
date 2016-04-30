
# SI-2712 fix plugin

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

For now you have to build the plugin yourself. Clone this project and run,
```bash
sbt '+ publishLocal'
```

Then in your project add the following to the sbt build file,

```scala
 addCompilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.0.0-SNAPSHOT" cross CrossVersion.full)

```

If you intended to use the `@unifyRightToLeft` annotation to enable right-to-left unify rule please add the following
as well

```scala
libraryDependencies += "com.milessabin" % "si2712fix-library" % "1.0.0-SNAPSHOT" cross CrossVersion.full
```
More context about right to left rule can be found in this [issue comment][right-left].


## Examples

An example project can be found at [milessabin/si2712fix-demo][demo]






[si2712]: https://issues.scala-lang.org/browse/SI-2712
[si2712pr]: https://github.com/scala/scala/pull/5102
[explain]: https://gist.github.com/djspiewak/7a81a395c461fd3a09a6941d4cd040f2
[demo]: https://github.com/milessabin/si2712fix-demo/tree/plugin-based
[right-left]: https://github.com/scala/scala/pull/5102#issuecomment-211140311


