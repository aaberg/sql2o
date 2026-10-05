# Converter exception handling: two proposals for discussion

Raised while covering `org.sql2o.converters` with tests. Both items are behaviour that callers can observe today,
so changing either is an API decision rather than a cleanup. Nothing has been changed; this document exists to get
a decision from the community first.

## The problem in one sentence

`ByteArrayConverter` refuses an unsupported type with a bare `RuntimeException` while every other converter in the
package throws `ConverterException`, and `InputStreamConverter` only catches the checked one, so the two failure modes
of the same call are different exception types.

## What happens today

`ByteArrayConverter.convert` has three outcomes:

| input | result |
| --- | --- |
| `null` | `null` |
| `Blob` or `byte[]` | the bytes |
| anything else | `RuntimeException("could not convert <type> to byte[]")` |

Note that the method signature declares `throws ConverterException`, and it does throw that for a `Blob` that cannot
be read. So one method reports two unrelated exception types for two different kinds of failure.

`InputStreamConverter.convert` delegates to `ByteArrayConverter` and catches `ConverterException` only:

```java
try {
    return new ByteArrayInputStream(new ByteArrayConverter().convert(val));
} catch (ConverterException e) {
    throw new ConverterException("Error converting Blob to InputSteam");
}
```

So a caller sees:

| input | `ByteArrayConverter` | `InputStreamConverter` |
| --- | --- | --- |
| unreadable `Blob` | `ConverterException` | `ConverterException` |
| unsupported type | `RuntimeException` | `RuntimeException`, unwrapped |

The message of the wrapped case also carries a typo, `InputSteam`, which makes grepping logs harder than it needs to
be.

Two smaller things in the same area, both currently only pinned by tests:

- `BooleanConverter` converts a `Number` through `intValue()`, so `0.5d` is `false` and `1.5d` is `true`.
- `Convert.registerConverter` has no counterpart, so a converter registered for a class is permanent for the life of
  the JVM. The test for the registry works around this by swapping the private map and putting the original back,
  which is worth knowing if anyone writes another test around it.

## Proposal 1: make `ByteArrayConverter` throw `ConverterException`

Throw `ConverterException` for an unsupported type instead of `RuntimeException`.

Pros:

- one method, one exception type, matching everything else in the package;
- `InputStreamConverter` then wraps both cases and callers only have to handle `ConverterException`.

Cons:

- `RuntimeException` is unchecked, so existing code that catches `RuntimeException` around this call keeps working,
  but code that *specifically* catches `RuntimeException` and inspects the message would need to change;
- `ConverterException` is checked, so a caller that currently compiles without a `catch` will not compile any more.
  That is the breaking part, and it is why this needs a decision rather than a patch.

## Proposal 2: widen the catch in `InputStreamConverter`

Catch `RuntimeException` as well and rethrow it as a `ConverterException`, keeping the checked exception the method
already declares.

Pros:

- `InputStreamConverter` becomes consistent on its own, without touching `ByteArrayConverter` or its callers;
- no signature change anywhere, so nothing downstream breaks.

Cons:

- the inconsistency in `ByteArrayConverter` stays, and the wrapper now hides a runtime failure behind a checked one,
  which can be its own surprise.

## Also worth a decision

- Whether the `InputSteam` typo in the message should be corrected in the same release that changes any of the above,
  since a message change is what downstream log greps would notice.
- Whether the `BooleanConverter` truncation of a `Number` through `intValue()` is intended. It is not reachable from a
  `BIT`/`BOOLEAN` column on the common databases, but it is reachable when a value is passed in by hand.

## A third one: Row does not wrap what the numeric converters throw

Found while covering `org.sql2o.data`. `Row.getObject(int, Class)` and `Row.getObject(String, Class)` both wrap
conversion failures like this:

```java
try {
    return (V) throwIfNull(clazz, quirks.converterOf(clazz)).convert(getObject(columnIndex));
} catch (ConverterException ex) {
    throw new Sql2oException("Error converting value", ex);
}
```

The catch only sees `ConverterException`, and the numeric converters do not throw that for text that is not a number:
`NumberConverter` hands the string to `Integer.parseInt` and friends, which throw the unchecked
`NumberFormatException`. So the caller of `row.getInteger(...)` gets one of two unrelated exception types depending on
the converter involved:

| what the converter did | what the caller gets |
| --- | --- |
| threw `ConverterException`, e.g. `BooleanConverter` on an unknown type | `Sql2oException("Error converting value")` |
| let a JDK parser fail, e.g. `IntegerConverter` on `"abc"` | `NumberFormatException`, unwrapped |

This is reachable from ordinary use: a `VARCHAR` column holding text read into an `int` property, or a driver handing
back a string where a number was expected.

Note that this is not something `NumberConverter` can fix on its own without a decision, because changing it to throw
`ConverterException` turns a currently-working unchecked failure into a checked one for anyone calling
`convert` directly.

Suggested direction, if the community wants one behaviour: have the converters declare what they throw for bad input,
and let `Row` be the single place that translates. Both halves need a decision, so neither is changed here.

Pinned today by `RowTest.aConverterThatRefusesTheValueIsReportedAsAConversionProblem` and
`RowTest.textThatIsNotANumberEscapesAsANumberFormatException`.

## How this is pinned today

So the behaviour is not changed silently, the following tests describe what happens now:

- `ByteArrayConverterTest.somethingThatIsNotBytesOrABlobIsRefused`
- `InputStreamConverterTest.anUnsupportedTypeEscapesUnwrapped`
- `InputStreamConverterTest.aBlobThatCannotBeReadIsReportedHere`
- `BooleanConverterTest.aNumberIsTrueUnlessItTruncatesToZero`

Whichever way the decision goes, those tests are the ones to update.

## Summary of what a caller can be handed today

For one call that reads a value out of a row, depending on the converter and the value:

- `Sql2oException("Error converting value")` when a converter refused the value,
- `NumberFormatException` when a JDK parser inside a numeric converter failed,
- `ConverterException` from `Row`'s own "no converter registered" guard, re-wrapped as `Sql2oException`,
- `RuntimeException` from `ByteArrayConverter` for an unsupported type, when the target is `byte[]` or `InputStream`.

Everything above is worth reducing to one type, but which type is the community's call.