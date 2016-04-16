import java.io.StringWriter;
import java.util.List;

import org.antlr.v4.automata.ATNFactory;
import org.antlr.v4.automata.LexerATNFactory;
import org.antlr.v4.automata.ParserATNFactory;
import org.antlr.v4.codegen.CodeGenerator;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.semantics.SemanticPipeline;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.junit.Test;
import org.stringtemplate.v4.AutoIndentWriter;
import org.stringtemplate.v4.InstanceScope;
import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ST;

import dkwakkel.jgcode.GCodeBaseListener;
import dkwakkel.jgcode.GCodeLexer;
import dkwakkel.jgcode.GCodeParser;
import dkwakkel.jgcode.GCodeParser.CommentContext;
import dkwakkel.jgcode.GCodeParser.EndOfLineContext;
import dkwakkel.jgcode.GCodeParser.XContext;

public class TransformTest
{

	// @Test
	public void grammarTest() throws Exception {
		String s = "grammar T;\n" + "a : 'a' ;\nb :a* ;\n";
		Grammar g = new Grammar(s);

		if (g.ast != null && !g.ast.hasErrors) {
			SemanticPipeline sem = new SemanticPipeline(g);
			sem.process();

			ATNFactory factory = new ParserATNFactory(g);
			if (g.isLexer()) {
				factory = new LexerATNFactory((LexerGrammar) g);
			}
			g.atn = factory.createATN();

			CodeGenerator gen = new CodeGenerator(g);
			ST outputFileST = gen.generateParser();

			// STViz viz = outputFileST.inspect();
			// try {
			// viz.waitForClose();
			// }
			// catch (Exception e) {
			// e.printStackTrace();
			// }

			boolean debug = false;
			Interpreter interp = new Interpreter(outputFileST.groupThatCreatedThisInstance, outputFileST.impl.nativeGroup.errMgr, debug)
			{
				@Override
				protected int writePOJO(org.stringtemplate.v4.STWriter out, InstanceScope scope, Object o, String[] options)
						throws java.io.IOException {

					return super.writePOJO(out, scope, o, options);
				}
			};
			InstanceScope scope = new InstanceScope(null, outputFileST);
			StringWriter sw = new StringWriter();
			AutoIndentWriter out = new AutoIndentWriter(sw);
			interp.exec(out, scope);
			System.err.println(sw.toString());
		}
	}

	@Test
	public void test() throws Exception {
		Transformer transformer = createTransformer("Program1.gcode");

		System.out.println(transformer.rewriter.getText());
	}

	private Transformer createTransformer(String fileName) throws Exception {
		GCodeLexer tableLexer = new GCodeLexer(new ANTLRFileStream(TransformTest.class.getResource(fileName).toURI().getPath()));
		CommonTokenStream tokenStream = new CommonTokenStream(tableLexer);
		final Transformer transformer = new Transformer(tokenStream);

		final ParseTree tree = new GCodeParser(tokenStream).program();
		final ParseTreeWalker walker = new ParseTreeWalker();
		Thread t = new Thread()
		{
			@Override
			public void run() {
				walker.walk(transformer, tree);
			}
		};
		t.start();
		t.join();
		return transformer;
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
		public void exitComment(CommentContext ctx) {
      Token semi = ctx.getStop();
      int i = semi.getTokenIndex();
			List<Token> cmtChannel = tokenStream.getTokens(i, GCodeLexer.END_OF_LINE);
			if (cmtChannel != null) {
				Token cmt = cmtChannel.get(0);
				if (cmt != null) {
					rewriter.insertAfter(cmt.getStartIndex(), "BOE!");
				}
			}
		}

		@Override
		public void exitX(XContext ctx) {
			if (ctx.e().getText().equals("1.5")) {
				rewriter.replace(ctx.e().start, "2");
			}
		}

		@Override
		public void exitEndOfLine(final EndOfLineContext ctx) {
			int previousIndex = 0;
			for (Token t : tokenStream.get(previousIndex, ctx.stop.getStopIndex())) {
				rewriter.insertAfter(ctx.start, t);
			}
		}
	}
}
