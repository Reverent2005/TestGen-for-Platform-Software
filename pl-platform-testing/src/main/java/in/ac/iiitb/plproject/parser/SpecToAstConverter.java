package in.ac.iiitb.plproject.parser;

import in.ac.iiitb.plproject.parser.ast.*;
import in.ac.iiitb.plproject.ast.*;  // This imports NewGrammar classes including ResultExpr, OldExpr
import java.util.*;
import java.util.regex.*;


/**
 * Generic converter from .spec text files to JmlSpecAst
 * Works for any JML spec file without hardcoding
 */
public class SpecToAstConverter {
    
    public static JmlSpecAst convertSpecToAst(String specText) {
        List<JmlFunctionSpec> specs = new ArrayList<>();
        
        // Step 1: Extract individual spec blocks
        List<SpecBlock> blocks = extractSpecBlocks(specText);
        
        // Step 2: For each block, build its AST
        for (SpecBlock block : blocks) {
            JmlFunctionSpec spec = buildSpecAst(block);
            specs.add(spec);
        }
        
        return new JmlSpecAst(specs);
    }
    
    // ──────────────────────────────────────────────────────────
    // Step 1: Extract spec blocks
    // ──────────────────────────────────────────────────────────
    static List<SpecBlock> extractSpecBlocks(String specText) {
        List<SpecBlock> blocks = new ArrayList<>();
        
        Pattern specPattern = Pattern.compile(
            "spec\\s+(\\w+)\\s*\\{([^}]+)\\}",
            Pattern.DOTALL
        );
        Matcher m = specPattern.matcher(specText);
        
        while (m.find()) {
            String name = m.group(1);
            String body = m.group(2);
            blocks.add(new SpecBlock(name, body));
        }
        
        return blocks;
    }
    
    // ──────────────────────────────────────────────────────────
    // Step 2: Build AST for one spec block
    // ──────────────────────────────────────────────────────────
    static JmlFunctionSpec buildSpecAst(SpecBlock block) {
        String sigLine = extractClause(block.body, "signature");
        String reqLine = extractClause(block.body, "requires");
        String ensLine = extractClause(block.body, "ensures");
        
        FunctionSignature sig = parseSignature(sigLine);
        Expr precond = parseConditionToExpr(reqLine);
        Expr postcond = parseConditionToExpr(ensLine);
        
        return new JmlFunctionSpec(block.name, sig, precond, postcond);
    }
    
    // ──────────────────────────────────────────────────────────
    // Parse signature text → FunctionSignature object
    // ──────────────────────────────────────────────────────────
    static FunctionSignature parseSignature(String sig) {
        Pattern p = Pattern.compile("([\\w\\[\\]]+)\\s+(\\w+)\\s*\\(([^)]*)\\)");
        Matcher m = p.matcher(sig);
        
        if (m.find()) {
            String returnType = m.group(1);
            String methodName = m.group(2);
            List<Variable> params = parseParams(m.group(3));
            
            return new FunctionSignature(methodName, params, returnType);
        }
        return null;
    }
    
    static List<Variable> parseParams(String paramStr) {
        List<Variable> params = new ArrayList<>();
        if (paramStr.trim().isEmpty()) return params;
        
        for (String param : paramStr.split(",")) {
            String[] parts = param.trim().split("\\s+");
            if (parts.length == 2) {
                params.add(new Variable(parts[1], parts[0]));
            }
        }
        return params;
    }
    
    // ──────────────────────────────────────────────────────────
    // Parse condition text → Expr AST tree
    // ──────────────────────────────────────────────────────────
    static Expr parseConditionToExpr(String condText) {
        condText = condText.trim();
        
        // 1. Handle OR (lowest precedence)
        if (containsAtTopLevel(condText, "||")) {
            String[] parts = splitByTopLevel(condText, "||");
            Expr left = parseConditionToExpr(parts[0]);
            Expr right = parseConditionToExpr(parts[1]);
            return AstHelper.createBinaryExpr(left, right, "OR");
        }
        
        // 2. Handle AND
        if (containsAtTopLevel(condText, "&&")) {
            String[] parts = splitByTopLevel(condText, "&&");
            Expr left = parseConditionToExpr(parts[0]);
            Expr right = parseConditionToExpr(parts[1]);
            return AstHelper.createBinaryExpr(left, right, "AND");
        }
        
        // 3. Handle comparisons
        for (String op : new String[]{"<=", ">=", "==", "!=", "<", ">"}) {
            if (containsAtTopLevel(condText, op)) {
                String[] parts = splitByTopLevel(condText, op);
                Expr left = parseExprToExpr(parts[0]);
                Expr right = parseExprToExpr(parts[1]);
                return AstHelper.createBinaryExpr(left, right, opToOperator(op));
            }
        }
        
        // 4. Fallback: treat as expression
        return parseExprToExpr(condText);
    }
    
    // ──────────────────────────────────────────────────────────
    // Parse arithmetic expression text → Expr AST tree
    // ──────────────────────────────────────────────────────────
    static Expr parseExprToExpr(String exprText) {
        exprText = exprText.trim();
        
        // 1. Handle +/- (lowest precedence in arithmetic)
        for (String op : new String[]{"+", "-"}) {
            if (containsAtTopLevel(exprText, op) && !exprText.startsWith(op)) {
                String[] parts = splitByTopLevel(exprText, op);
                Expr left = parseExprToExpr(parts[0]);
                Expr right = parseExprToExpr(parts[1]);
                return AstHelper.createBinaryExpr(left, right, 
                    op.equals("+") ? "PLUS" : "MINUS");
            }
        }
        
        // 2. Handle */÷ (higher precedence)
        for (String op : new String[]{"*", "/"}) {
            if (containsAtTopLevel(exprText, op)) {
                String[] parts = splitByTopLevel(exprText, op);
                Expr left = parseExprToExpr(parts[0]);
                Expr right = parseExprToExpr(parts[1]);
                return AstHelper.createBinaryExpr(left, right, 
                    op.equals("*") ? "MULTIPLY" : "DIVIDE");
            }
        }
        
        // 3. Handle unary minus
        if (exprText.startsWith("-") && !exprText.startsWith("- ")) {
            Expr inner = parseExprToExpr(exprText.substring(1));
            return AstHelper.createUnaryExpr(inner, "MINUS");
        }
        
        // 4. Handle parentheses
        if (exprText.startsWith("(") && exprText.endsWith(")")) {
            return parseConditionToExpr(exprText.substring(1, exprText.length()-1));
        }
        
        // 5. Handle JML special tokens
        if (exprText.equals("\\result")) {
            return new ResultExpr();
        }
        if (exprText.startsWith("\\old(") && exprText.endsWith(")")) {
            Expr inner = parseExprToExpr(exprText.substring(5, exprText.length()-1));
            return new OldExpr(inner);
        }
        
        // 6. Handle literals
        if (exprText.matches("-?\\d+")) {
            return AstHelper.createIntegerLiteralExpr(Integer.parseInt(exprText));
        }
        if (exprText.matches("-?\\d+\\.\\d+")) {
            return AstHelper.createDoubleLiteralExpr(Double.parseDouble(exprText));
        }
        if (exprText.equals("true")) {
            return AstHelper.createBooleanLiteralExpr(true);
        }
        if (exprText.equals("false")) {
            return AstHelper.createBooleanLiteralExpr(false);
        }
        
        // 7. Default: variable name
        return AstHelper.createNameExpr(exprText);
    }
    
    // ──────────────────────────────────────────────────────────
    // Helper utilities
    // ──────────────────────────────────────────────────────────
    
    static String extractClause(String body, String clause) {
        Pattern p = Pattern.compile(clause + ":\\s*([^;]+);");
        Matcher m = p.matcher(body);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "";
    }
    
    static boolean containsAtTopLevel(String text, String op) {
        int parenDepth = 0;
        int idx = text.indexOf(op);
        while (idx != -1) {
            parenDepth = 0;
            for (int i = 0; i < idx; i++) {
                if (text.charAt(i) == '(') parenDepth++;
                else if (text.charAt(i) == ')') parenDepth--;
            }
            if (parenDepth == 0) return true;
            idx = text.indexOf(op, idx + 1);
        }
        return false;
    }
    
    static String[] splitByTopLevel(String text, String op) {
        int parenDepth = 0;
        int idx = text.indexOf(op);
        while (idx != -1) {
            parenDepth = 0;
            for (int i = 0; i < idx; i++) {
                if (text.charAt(i) == '(') parenDepth++;
                else if (text.charAt(i) == ')') parenDepth--;
            }
            if (parenDepth == 0) {
                return new String[]{
                    text.substring(0, idx).trim(),
                    text.substring(idx + op.length()).trim()
                };
            }
            idx = text.indexOf(op, idx + 1);
        }
        return new String[]{text};
    }
    
    static String opToOperator(String op) {
        switch (op) {
            case "==": return "EQUALS";
            case "!=": return "NOT_EQUALS";
            case "<": return "LESS_THAN";
            case ">": return "GREATER_THAN";
            case "<=": return "LESS_THAN_OR_EQUAL";
            case ">=": return "GREATER_THAN_OR_EQUAL";
            default: return op;
        }
    }
    
    static class SpecBlock {
        String name;
        String body;
        SpecBlock(String name, String body) {
            this.name = name;
            this.body = body;
        }
    }
}