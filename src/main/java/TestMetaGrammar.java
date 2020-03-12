import java.io.IOException;

import pikaparser.parser.MetaGrammar;
import pikaparser.parser.Parser;
import pikaparser.parser.ParserInfo;

public class TestMetaGrammar {

    public static void main(String[] args) throws IOException {
        String zedGrammar;
        try (var stream = TestMetaGrammar.class.getClassLoader().getResource("zedGrammarSimple").openStream()) {
            zedGrammar = new String(stream.readAllBytes());
        }

        Parser metaParser = MetaGrammar.newParser(zedGrammar);

        ParserInfo.printParseResult(metaParser);
    }

}
