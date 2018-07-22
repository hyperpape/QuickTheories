Coverage based generation of example input-output pairs for methods. All the heavy lifting uses the QuickTheories library.

## Example

    Test test = new Test();
    Function<String, String> fn = s -> String.valueOf(test.incorrectIsEmailValid(s));
    List<Pair<String, String>> values = explore(fn, SourceDSL.strings().basicLatinAlphabet().ofLengthBetween(0, 32));
    values.forEach(System.out::println);

    // { input, output }}
    {B@., true}
    {1@.ptjUR(=(,ehOd, false}
    {1@., false}
    {, false}
    {R@ba>@k60b,jhLr}h^IOlfjGd, false}
    {%2O\:h|e15T^z@, false}
    {@ORf;X^@U7H$BO7(Z, false}

## Caveats

This is a dirty, dirty hack of the QuickTheories codebase to do something it wasn't really designed for.

The outputs are not shrunk in any way, and the coverage tool does not use symbolic analysis or related fancy ideas to
search for interesting inputs.

I did zero research on existing implementations of this idea, but I'd love to hear about them, as I think the core idea
could make for a nice tool when writing code.