import java.io.IOException;

import pikaparser.grammar.Grammar;
import pikaparser.grammar.MetaGrammar;
import pikaparser.parser.Parser;
import pikaparser.parser.ParserInfo;

public class TestMetaGrammar {

    public static void main(String[] args) throws IOException {
        String zedGrammar;
        try (var stream = TestMetaGrammar.class.getClassLoader().getResource("zedGrammarSimple").openStream()) {
            zedGrammar = new String(stream.readAllBytes());
        }

        Parser metaParser = MetaGrammar.newParser(zedGrammar);

        ParserInfo.printParseResult(metaParser, "Grammar");

        Grammar grammar = MetaGrammar.parseGrammar(metaParser);

        System.out.println("Parsed grammar:");
        for (var clause : grammar.allClauses) {
            System.out.println("    " + clause.toStringWithRuleNamesAndLabels() + ";");
        }
    }

}
