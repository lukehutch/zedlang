import java.io.IOException;

import pikaparser.parser.MetaGrammarSimple;
import pikaparser.parser.Parser;
import pikaparser.parser.ParserInfo;

public class Test {

    public static void main(String[] args) throws IOException {

//        String input;
//        try (var stream = Test.class.getClassLoader().getResource("testGrammar").openStream()) {
//            input = new String(stream.readAllBytes());
//        }
//
//        // String input = " L = P | Q+; Q = L P; P = 'x' | 'y'; ";
//
//        // String input = "L = P Q+;";
//
//        Parser parser = MetaGrammar.newParser(input);
//
//        // Parser parser = MetaGrammarSimple.newParser("x x+");
//
//        ParserInfo.printParseResult(parser);

                {
                    // String grammarStr = "a (b c (d e f))";
                    // String grammarStr = "a + b + c - d - e + f + g - h - i + j - k";
                    String grammarStr = "a + b + c * d - e - ((x + y) + (p + f) + g) + h - i / j / k";
                    Parser parser = MetaGrammarSimple.newParser(grammarStr);
        
                    ParserInfo.printParseResult(parser);
                }
    }

}
