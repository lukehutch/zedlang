import java.io.IOException;

import pikaparser.grammar.Grammar;
import pikaparser.grammar.MetaGrammar;
import pikaparser.parser.Parser;
import pikaparser.parser.ParserInfo;

public class TestMetaGrammar {

    public static void main(String[] args) throws IOException {
        String grammarStr;
        try (var stream = TestMetaGrammar.class.getClassLoader().getResource("zedGrammarSmall").openStream()) {
            grammarStr = new String(stream.readAllBytes());
        }

        Parser metaParser = new Parser(MetaGrammar.grammar, grammarStr);

        ParserInfo.printParseResult(metaParser, "Grammar");

        Grammar grammar = MetaGrammar.parseGrammar(metaParser);

        System.out.println("\nParsed grammar:");
        for (var clause : grammar.allClauses) {
            System.out.println("    " + clause.toStringWithRuleName());
        }
    }

}
