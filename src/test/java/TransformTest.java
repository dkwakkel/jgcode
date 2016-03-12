import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;

import dkwakkel.jgcode.GCodeBaseListener;
import dkwakkel.jgcode.GCodeLexer;
import dkwakkel.jgcode.GCodeParser;
import dkwakkel.jgcode.GCodeParser.EndOfLineContext;

public class TransformTest
{

	@Test
	public void test() throws Exception {
		GCodeLexer tableLexer = new GCodeLexer(new ANTLRFileStream(TransformTest.class.getResource("Program1.gcode").toURI().getPath()));
		CommonTokenStream tokenStream = new CommonTokenStream(tableLexer);
		Transformer transformer = new Transformer(tokenStream);

		ParseTree tree = new GCodeParser(tokenStream).program();
		ParseTreeWalker walker = new ParseTreeWalker();
		walker.walk(transformer, tree);

		System.out.println(transformer.rewriter.getText());
	}

	// See another example here: http://media.pragprog.com/titles/tpantlr2/code/lexmagic/ShiftVarComments.java
	private static class Transformer extends GCodeBaseListener
	{
		private final CommonTokenStream		tokenStream;
		private final TokenStreamRewriter	rewriter;

		public Transformer(CommonTokenStream tokenStream) {
			this.tokenStream = tokenStream;
			rewriter = new TokenStreamRewriter(tokenStream);
		}

		@Override
		public void enterEndOfLine(EndOfLineContext ctx) {
			String txt = "Some comment";
			String newComment = "( " + txt.trim() + " )";
			rewriter.insertBefore(ctx.start, newComment);
		}
	}
}
