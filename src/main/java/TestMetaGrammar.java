import java.io.IOException;

import pikaparser.grammar.Grammar;
import pikaparser.grammar.MetaGrammar;
import pikaparser.parser.Parser;
import pikaparser.parser.ParserInfo;

public class TestMetaGrammar {

    public static void main(String[] args) throws IOException {
        String grammarStr;
        try (var stream = TestMetaGrammar.class.getClassLoader().getResource("zedGrammar").openStream()) {
            grammarStr = new String(stream.readAllBytes());
        }

        Parser metaParser = new Parser(MetaGrammar.grammar, grammarStr);

        ParserInfo.printParseResult(metaParser, "GRAMMAR", false);

        Grammar zedGrammar = MetaGrammar.parseGrammar(metaParser);

        System.out.println("\nParsed grammar:");
        for (var clause : zedGrammar.allClauses) {
            System.out.println("    " + clause.toStringWithRuleNames());
        }
        
        // Parser zedParser = new Parser(zedGrammar, zedSrc);
    }

}
