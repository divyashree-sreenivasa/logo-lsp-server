package jetbrains.lsp.logo.analysis;

import jetbrains.lsp.logo.parser.LogoBaseVisitor;
import jetbrains.lsp.logo.parser.LogoParser;
import jetbrains.lsp.logo.parser.PositionConverter;
import org.antlr.v4.runtime.Token;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single-pass visitor that populates a SymbolTable from the parse tree.
 *
 * Collects every TO ... END block, recording:
 *  - the procedure's name and its source range
 *  - the full definition range (from TO token to END token): used to find enclosing procedure
 *  - each :param name and its source range on the TO line
 */
public class SymbolTableBuilder extends LogoBaseVisitor<Void> {

    private final SymbolTable symbolTable = new SymbolTable();

    public SymbolTable build(LogoParser.ProgramContext tree) {
        visit(tree);
        return symbolTable;
    }

    @Override
    public Void visitProcedureDef(LogoParser.ProcedureDefContext ctx) {
        Token nameToken = ctx.IDENT().getSymbol();
        Token toToken   = ctx.TO().getSymbol();
        Token endToken  = ctx.END().getSymbol();

        String name = nameToken.getText().toUpperCase();

        Range nameRange       = PositionConverter.toRange(nameToken);
        // definitionRange spans the whole block so enclosingProcedure() works correctly
        Range definitionRange = PositionConverter.toRange(toToken, endToken);

        List<String> params      = new ArrayList<>();
        Map<String, Range> paramRanges = new LinkedHashMap<>();

        for (LogoParser.ParamDeclContext paramCtx : ctx.paramDecl()) {
            Token colonToken     = paramCtx.COLON().getSymbol();
            Token paramNameToken = paramCtx.IDENT().getSymbol();
            String paramName     = paramNameToken.getText().toUpperCase();

            params.add(paramName);
            // Range covers both ':' and the name, matching what the user clicks on
            paramRanges.put(paramName, PositionConverter.toRange(colonToken, paramNameToken));
        }

        symbolTable.addProcedure(
                new ProcedureSymbol(name, nameRange, definitionRange, params, paramRanges));

        // Visit the body so nested definitions (if any) are also collected
        return visitChildren(ctx);
    }

    /**
     * Collects MAKE "varname declarations so go-to-declaration can resolve :varname
     * references that aren't procedure parameters.
     *
     * Grammar: makeCmd : MAKE QUOTED_STRING expr
     * The QUOTED_STRING token text is e.g. "NUM (leading quote, no closing quote).
     */
    /**
     * Collects FOR [i start end] loop variables.
     * Grammar: forCmd : FOR LBRACK IDENT expr expr expr? RBRACK LBRACK statement* RBRACK
     * RBRACK(0) closes the control list; RBRACK(1) closes the body.
     */
    @Override
    public Void visitForCmd(LogoParser.ForCmdContext ctx) {
        Token varToken      = ctx.IDENT().getSymbol();
        Token forToken      = ctx.FOR().getSymbol();
        Token bodyClose     = ctx.RBRACK(1).getSymbol(); // closing ] of the body block

        String varName  = varToken.getText().toUpperCase();
        Range nameRange  = PositionConverter.toRange(varToken);
        Range scopeRange = PositionConverter.toRange(forToken, bodyClose);

        symbolTable.addForLoopVariable(
                new SymbolTable.ForLoopVariable(varName, nameRange, scopeRange));

        return visitChildren(ctx);
    }

    @Override
    public Void visitMakeCmd(LogoParser.MakeCmdContext ctx) {
        Token quotedString = ctx.QUOTED_STRING().getSymbol();
        String raw = quotedString.getText(); // e.g. "NUM
        if (raw.length() > 1) {
            String varName = raw.substring(1).toUpperCase(); // strip leading "
            // Point to the quoted string token so the user lands on the declaration name
            Range nameRange = PositionConverter.toRange(quotedString);
            symbolTable.addMakeVariable(new SymbolTable.MakeVariable(varName, nameRange));
        }
        return visitChildren(ctx);
    }
}
