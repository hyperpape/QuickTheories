Coverage based generation of example input-output pairs for methods. All the heavy lifting uses the QuickTheories library.

## Example

    Test test = new Test();
    Function<String, String> fn = s -> String.valueOf(test.incorrectIsEmailValid(s));
    List<Pair<String, String>> values = explore(fn, SourceDSL.strings().basicLatinAlphabet().ofLengthBetween(0, 32));
    values.forEach(System.out::println);

    //  input -> output
    "!!!!!!!!!!!!!!!!!!!!!!!!!!!@A." -> false
    "@!" -> false
    "!!!!!!!!!@!" -> false
    "" -> false
    "!@A!!!!!!!!." -> false
    "A@." -> true
    "!!!!!!!!!!!!!!!!!!!!!!!!!@" -> false

This example explores the functionality of a very bad email parser, which thinks that '.' is a valid domain name.

## Caveats

This is a dirty, dirty hack of the QuickTheories codebase to do something it wasn't really designed for.

The coverage tool does not use symbolic analysis or related fancy ideas to search for interesting inputs.
It searches shallowly enough that it does not always find an input that passes the "email parser" in the test case.

I did zero research on existing implementations of this idea, but I'd love to hear about them, as I think the core idea
could make for a nice tool when writing code.