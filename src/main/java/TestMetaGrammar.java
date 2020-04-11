import java.io.IOException;

import pikaparser.grammar.Grammar;
import pikaparser.grammar.MetaGrammar;
import pikaparser.parser.Parser;
import pikaparser.parser.ParserInfo;

public class TestMetaGrammar {

    public static void main(String[] args) throws IOException {
        System.out.println("Parsing Zed grammar");

        String grammarStr;
        try (var stream = TestMetaGrammar.class.getClassLoader() //
                // .getResource("zedGrammar") //
                .getResource("grammarSimple") //
                .openStream()) {
            grammarStr = new String(stream.readAllBytes());
        }

        Parser metaParser = new Parser(MetaGrammar.grammar);
        metaParser.parse(grammarStr);

        // ParserInfo.printParseResult(metaParser, "GRAMMAR", new String[] { "GRAMMAR", "RULE", "CLAUSE" },
        //         /* showAllMatches = */ false);

        ParserInfo.printSyntaxErrors(metaParser, new String[] { "GRAMMAR", "RULE", "CLAUSE" });

        Grammar zedGrammar = MetaGrammar.parseGrammar(metaParser);

        //        System.out.println("\nParsed grammar:");
        //        for (var clause : zedGrammar.allClauses) {
        //            System.out.println("    " + clause.toStringWithRuleNames());
        //        }

        System.out.println("Parsing Zed program");

        String srcStr;
        try (var stream = TestMetaGrammar.class.getClassLoader() //
                // .getResource("test.zed") //
                .getResource("test.simple") //
                .openStream()) {
            srcStr = new String(stream.readAllBytes());
        }

        Parser srcParser = new Parser(zedGrammar);
        srcParser.parse(srcStr);

        ParserInfo.printParseResult(srcParser, "Program",
                new String[] { "Program", "ImportStatement", "Assignment", "Expr" }, /* showAllMatches = */ false);
    }

}
