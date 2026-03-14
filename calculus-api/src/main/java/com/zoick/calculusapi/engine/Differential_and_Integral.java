package com.zoick.calculusapi.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Differential_and_Integral {

    // ---public api---
    public String differentiate(String input) {
        String normalized = normalize(input);
        List<Token> tokens = tokenize(normalized);
        tokens = insertFunctionParens(tokens);
        tokens = implicitMultiplication(tokens);
        Parser p = new Parser(tokens);
        Expr expr = p.parseExpression();
        Expr d = Differentiator.diff(expr);
        Expr s = Simplifier.autoSimplify(d, true);
        return Printer.print(s);
    }

    public String differentiate(String input, int order) {
        String normalized = normalize(input);
        List<Token> tokens = tokenize(normalized);
        tokens = insertFunctionParens(tokens);
        tokens = implicitMultiplication(tokens);
        Parser p = new Parser(tokens);
        Expr expr = p.parseExpression();
        Expr d = Differentiator.diff(expr, order);
        Expr s = Simplifier.autoSimplify(d, true);
        return Printer.print(s);
    }

    public String integrate(String input) {
        String normalized = normalize(input);
        List<Token> tokens = tokenize(normalized);
        tokens = insertFunctionParens(tokens);
        tokens = implicitMultiplication(tokens);
        Parser p = new Parser(tokens);
        Expr expr = p.parseExpression();
        Expr i = Integrator.integrate(expr);
        Expr s = Simplifier.autoSimplify(i, true);
        return Printer.print(s);
    }

    public String integrateDefinite(String input, double a, double b) {
        String normalized = normalize(input);
        List<Token> tokens = tokenize(normalized);
        tokens = insertFunctionParens(tokens);
        tokens = implicitMultiplication(tokens);
        Parser p = new Parser(tokens);
        Expr expr = p.parseExpression();
        Expr antideriv = Integrator.integrate(expr);
        Expr simplified = Simplifier.autoSimplify(antideriv, true);
        double result = evaluateAt(simplified, b) - evaluateAt(simplified, a);
        return String.valueOf(result);
    }

    public String implicitDifferentiate(String equation) {
        String[] parts = equation.split("=");
        if (parts.length != 2) throw new IllegalArgumentException("Invalid equation");
        Expr left = parseExpressionFromInput(parts[0].trim());
        Expr right = parseExpressionFromInput(parts[1].trim());
        Expr leftDiff = Differentiator.diff(left);
        Expr rightDiff = Differentiator.diff(right);
        Expr equationDiff = new Sub(leftDiff, rightDiff);
        Map<Boolean, List<Expr>> terms = collectTermsWithDyDx(equationDiff);
        Expr dyTerms = buildAdd(terms.get(true));
        Expr otherTerms = new Neg(buildAdd(terms.get(false)));
        if (dyTerms instanceof Num num && num.v == 0) {
            throw new IllegalArgumentException("No dy/dx terms found");
        }
        Expr dyDx = new Div(otherTerms, dyTerms);
        Expr finalSimplified = Simplifier.autoSimplify(dyDx, true);
        return Printer.print(finalSimplified);
    }

    // ---normalization---
    private String normalize(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\s+", "").toLowerCase();
        return s;
    }

    // ---Tokenizer---
    enum TokenType {
        NUMBER, IDENT,
        PLUS, MINUS, STAR, SLASH, CARET,
        LPAREN, RPAREN, EOF
    }

    static class Token {
        final TokenType type;
        final String text;

        Token(TokenType t, String s) {
            type = t;
            text = s;
        }

        public String toString() {
            return type + ":" + text;
        }
    }

    private static final Pattern NUM = Pattern.compile("^[0-9]*\\.?[0-9]+([eE][+-]?[0-9]+)?$");
    private static final Set<String> FUNCTIONS = Set.of("sin", "cos", "tan", "ln", "exp", "sqrt", "sec", "csc", "cot");
    private static final Set<String> CONSTANTS = Set.of("e", "pi");

    private static List<Token> tokenize(String s) {
        List<Token> out = new ArrayList<>();
        for (int i = 0; i < s.length(); ) {
            char c = s.charAt(i);
            if (c == '+') {
                out.add(new Token(TokenType.PLUS, "+")); i++; continue;
            }
            if (c == '-') {
                out.add(new Token(TokenType.MINUS, "-")); i++; continue;
            }
            if (c == '*') {
                out.add(new Token(TokenType.STAR, "*")); i++; continue; }
            if (c == '/') { out.add(new Token(TokenType.SLASH, "/")); i++; continue;
            }
            if (c == '^') {
                out.add(new Token(TokenType.CARET, "^")); i++; continue;
            }
            if (c == '(') {
                out.add(new Token(TokenType.LPAREN, "(")); i++; continue;
            }
            if (c == ')') {
                out.add(new Token(TokenType.RPAREN, ")")); i++; continue;
            }
            if (Character.isDigit(c) || c == '.') {
                int j = i + 1;
                while (j < s.length() && "0123456789.eE+-".indexOf(s.charAt(j)) >= 0) {
                    if ((s.charAt(j) == '+' || s.charAt(j) == '-') &&
                            !(s.charAt(j - 1) == 'e' || s.charAt(j - 1) == 'E')) break;
                    j++;
                }
                String num = s.substring(i, j);
                try {
                    Double.parseDouble(num);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid number: " + num);
                }
                out.add(new Token(TokenType.NUMBER, num));
                i = j;
                continue;
            }
            if (Character.isLetter(c)) {
                int j = i + 1;
                while (j < s.length() && Character.isLetter(s.charAt(j))) j++;
                out.add(new Token(TokenType.IDENT, s.substring(i, j)));
                i = j;
                continue;
            }
            throw new IllegalArgumentException("Unexpected character '" + c + "' at position " + i);
        }
        out.add(new Token(TokenType.EOF, ""));
        return out;
    }

    private static List<Token> insertFunctionParens(List<Token> in) {
        List<Token> out = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            Token t = in.get(i);
            if (t.type == TokenType.IDENT && FUNCTIONS.contains(t.text)) {
                if (i + 1 < in.size() && in.get(i + 1).type == TokenType.CARET) {
                    if (i + 2 >= in.size()) throw new IllegalArgumentException("Missing exponent");
                    Token caret = in.get(i + 1);
                    int expStart = i + 2;
                    int expEnd = expStart + 1;
                    if (in.get(expStart).type == TokenType.LPAREN) {
                        int depth = 1;
                        expEnd = expStart + 1;
                        while (expEnd < in.size() && depth > 0) {
                            Token tk = in.get(expEnd);
                            if (tk.type == TokenType.LPAREN) depth++;
                            else if (tk.type == TokenType.RPAREN) depth--;
                            expEnd++;
                        }
                        if (depth > 0) throw new IllegalArgumentException("Unbalanced parentheses in exponent");
                    }
                    int j = expEnd;
                    out.add(new Token(TokenType.LPAREN, "("));
                    out.add(t);
                    out.add(new Token(TokenType.LPAREN, "("));
                    int depth = 0;
                    while (j < in.size()) {
                        Token tj = in.get(j);
                        if (tj.type == TokenType.EOF) break;
                        if (depth == 0 && (tj.type == TokenType.PLUS || tj.type == TokenType.MINUS || tj.type == TokenType.RPAREN)) break;
                        out.add(tj);
                        if (tj.type == TokenType.LPAREN) depth++;
                        else if (tj.type == TokenType.RPAREN) depth--;
                        j++;
                    }
                    out.add(new Token(TokenType.RPAREN, ")"));
                    out.add(new Token(TokenType.RPAREN, ")"));
                    out.add(caret);
                    for (int k = expStart; k < expEnd; k++) out.add(in.get(k));
                    i = j - 1;
                } else {
                    out.add(t);
                    if (i + 1 < in.size() && in.get(i + 1).type != TokenType.LPAREN) {
                        out.add(new Token(TokenType.LPAREN, "("));
                        int j = i + 1;
                        int depth = 0;
                        while (j < in.size()) {
                            Token tj = in.get(j);
                            if (tj.type == TokenType.EOF) break;
                            if (depth == 0 && (tj.type == TokenType.PLUS || tj.type == TokenType.MINUS || tj.type == TokenType.RPAREN)) break;
                            out.add(tj);
                            if (tj.type == TokenType.LPAREN) depth++;
                            else if (tj.type == TokenType.RPAREN) depth--;
                            j++;
                        }
                        out.add(new Token(TokenType.RPAREN, ")"));
                        i = j - 1;
                    }
                }
            } else {
                out.add(t);
            }
        }
        return out;
    }

    private List<Token> implicitMultiplication(List<Token> in) {
        if (in.size() <= 1) return in;
        ArrayList<Token> out = new ArrayList<>(in.size() * 2);
        for (int i = 0; i < in.size() - 1; i++) {
            Token a = in.get(i);
            Token b = in.get(i + 1);
            out.add(a);
            if (needsImplicitStar(a, b)) out.add(new Token(TokenType.STAR, "*"));
        }
        out.add(in.get(in.size() - 1));
        return out;
    }

    private boolean isAtomEnd(Token t) {
        return t.type == TokenType.NUMBER || t.type == TokenType.IDENT || t.type == TokenType.RPAREN;
    }

    private boolean isAtomStart(Token t) {
        return t.type == TokenType.NUMBER || t.type == TokenType.IDENT || t.type == TokenType.LPAREN;
    }

    private boolean isFunctionIndex(Token t) {
        return t.type == TokenType.IDENT && FUNCTIONS.contains(t.text);
    }

    private boolean needsImplicitStar(Token a, Token b) {
        if (isFunctionIndex(a) && b.type == TokenType.LPAREN) return false;
        if (a.type == TokenType.RPAREN && b.type == TokenType.LPAREN) return true;
        return isAtomEnd(a) && isAtomStart(b);
    }

    // ---AST Nodes---
    interface Expr {}

    static class Num implements Expr {
        final double v;
        Num(double v) { this.v = v; }
        @Override public boolean equals(Object o) { if (!(o instanceof Num other)) return false; return Double.compare(v, other.v) == 0; }
        @Override public int hashCode() { return Double.hashCode(v); }
    }

    static class Var implements Expr {
        final String name;
        Var(String name) { this.name = name; }
        @Override public boolean equals(Object o) { if (!(o instanceof Var other)) return false; return name.equals(other.name); }
        @Override public int hashCode() { return name.hashCode(); }
    }

    static class Add implements Expr {
        final Expr a, b;
        Add(Expr a, Expr b) { this.a = a; this.b = b; }
        @Override public boolean equals(Object o) { if (!(o instanceof Add other)) return false; return a.equals(other.a) && b.equals(other.b); }
        @Override public int hashCode() { return 31 * a.hashCode() + b.hashCode(); }
    }

    static class Sub implements Expr {
        final Expr a, b;
        Sub(Expr a, Expr b) { this.a = a; this.b = b; }
        @Override public boolean equals(Object o) { if (!(o instanceof Sub other)) return false; return a.equals(other.a) && b.equals(other.b); }
        @Override public int hashCode() { return 37 * a.hashCode() + b.hashCode(); }
    }

    static class Mul implements Expr {
        final Expr a, b;
        Mul(Expr a, Expr b) { this.a = a; this.b = b; }
        @Override public boolean equals(Object o) { if (!(o instanceof Mul other)) return false; return a.equals(other.a) && b.equals(other.b); }
        @Override public int hashCode() { return 41 * a.hashCode() + b.hashCode(); }
    }

    static class Div implements Expr {
        final Expr a, b;
        Div(Expr a, Expr b) { this.a = a; this.b = b; }
        @Override public boolean equals(Object o) { if (!(o instanceof Div other)) return false; return a.equals(other.a) && b.equals(other.b); }
        @Override public int hashCode() { return 43 * a.hashCode() + b.hashCode(); }
    }

    static class Pow implements Expr {
        final Expr base, exp;
        Pow(Expr base, Expr exp) { this.base = base; this.exp = exp; }
        @Override public boolean equals(Object o) { if (!(o instanceof Pow other)) return false; return base.equals(other.base) && exp.equals(other.exp); }
        @Override public int hashCode() { return 47 * base.hashCode() + exp.hashCode(); }
    }

    static class Neg implements Expr {
        final Expr e;
        Neg(Expr e) { this.e = e; }
        @Override public boolean equals(Object o) { if (!(o instanceof Neg other)) return false; return e.equals(other.e); }
        @Override public int hashCode() { return 53 * e.hashCode(); }
    }

    static class Func implements Expr {
        final String name;
        final Expr arg;
        Func(String name, Expr arg) { this.name = name; this.arg = arg; }
        @Override public boolean equals(Object o) { if (!(o instanceof Func other)) return false; return name.equals(other.name) && arg.equals(other.arg); }
        @Override public int hashCode() { return 59 * name.hashCode() + arg.hashCode(); }
    }

    static class DyDx implements Expr {
        @Override public boolean equals(Object o) { return o instanceof DyDx; }
        @Override public int hashCode() { return 61; }
    }

    // ---Parser---
    static class Parser {
        final List<Token> ts;
        int i = 0;

        Parser(List<Token> ts) { this.ts = ts; }

        Token peek() { return ts.get(i); }
        Token next() { return ts.get(i++); }

        boolean match(TokenType t) {
            if (peek().type == t) { i++; return true; }
            return false;
        }

        void expect(TokenType t) {
            if (!match(t)) throw new IllegalArgumentException("Expected " + t + " but found " + peek());
        }

        Expr parseExpression() {
            Expr e = parseAddSub();
            if (peek().type != TokenType.EOF)
                throw new IllegalArgumentException("Unexpected token " + peek());
            return e;
        }

        Expr parseAddSub() {
            Expr e = parseMulDiv();
            while (true) {
                if (match(TokenType.PLUS)) e = new Add(e, parseMulDiv());
                else if (match(TokenType.MINUS)) e = new Sub(e, parseMulDiv());
                else break;
            }
            return e;
        }

        Expr parseMulDiv() {
            Expr e = parsePow();
            while (true) {
                if (match(TokenType.STAR)) e = new Mul(e, parsePow());
                else if (match(TokenType.SLASH)) e = new Div(e, parsePow());
                else break;
            }
            return e;
        }

        Expr parsePow() {
            Expr base = parseUnary();
            if (match(TokenType.CARET)) return new Pow(base, parsePow());
            return base;
        }

        Expr parseUnary() {
            if (match(TokenType.PLUS)) return parseUnary();
            if (match(TokenType.MINUS)) return new Neg(parseUnary());
            return parsePrimary();
        }

        Expr parsePrimary() {
            Token t = peek();
            if (t.type == TokenType.NUMBER) { next(); return new Num(Double.parseDouble(t.text)); }
            if (t.type == TokenType.IDENT) {
                Token ident = next();
                if (FUNCTIONS.contains(ident.text)) {
                    if (match(TokenType.LPAREN)) {
                        Expr arg = parseAddSub();
                        expect(TokenType.RPAREN);
                        Expr f = new Func(ident.text, arg);
                        if (match(TokenType.CARET)) return new Pow(f, parsePow());
                        return f;
                    } else {
                        throw new IllegalArgumentException("Function needs parentheses near " + ident.text);
                    }
                } else {
                    return new Var(ident.text);
                }
            }
            if (match(TokenType.LPAREN)) {
                Expr e = parseAddSub();
                expect(TokenType.RPAREN);
                return e;
            }
            throw new IllegalArgumentException("Unexpected token in primary: " + t);
        }
    }

    // ---Differentiator---
    static class Differentiator {
        static Expr diff(Expr e) {
            if (e instanceof Num) return new Num(0);
            if (e instanceof Var v) {
                if ("x".equals(v.name)) return new Num(1);
                if ("y".equals(v.name)) return new DyDx();
                return new Num(0);
            }
            if (e instanceof Neg n) return new Neg(diff(n.e));
            if (e instanceof Add a) return new Add(diff(a.a), diff(a.b));
            if (e instanceof Sub s) return new Sub(diff(s.a), diff(s.b));
            if (e instanceof Mul m) return new Add(new Mul(diff(m.a), m.b), new Mul(m.a, diff(m.b)));
            if (e instanceof Div d) {
                Expr u = d.a, v = d.b;
                return new Div(new Sub(new Mul(diff(u), v), new Mul(u, diff(v))), new Pow(v, new Num(2)));
            }
            if (e instanceof Pow p) {
                if (p.exp instanceof Num num) {
                    double c = num.v;
                    Expr inner = diff(p.base);
                    if (inner instanceof Num n && n.v == 0) return new Num(0);
                    return new Mul(new Mul(new Num(c), new Pow(p.base, new Num(c - 1))), inner);
                }
                throw new IllegalArgumentException("Unsupported: exponent depends on x");
            }
            if (e instanceof Func f) {
                Expr u = f.arg;
                Expr du = diff(u);
                switch (f.name) {
                    case "sin"  -> {
                        return new Mul(new Func("cos", u), du);
                    }
                    case "cos"  -> {
                        return new Neg(new Mul(new Func("sin", u), du));
                    }
                    case "tan"  -> {
                        return new Mul(new Pow(new Func("sec", u), new Num(2)), du);
                    }
                    case "ln"   -> {
                        return new Mul(new Div(new Num(1), u), du);
                    }
                    case "exp"  -> {
                        return new Mul(new Func("exp", u), du);
                    }
                    case "sec"  -> {
                        return new Mul(new Mul(new Func("sec", u), new Func("tan", u)), du);
                    }
                    case "csc"  -> {
                        return new Neg(new Mul(new Mul(new Func("csc", u), new Func("cot", u)), du));
                    }
                    case "cot"  -> {
                        return new Neg(new Mul(new Pow(new Func("csc", u), new Num(2)), du));
                    }
                    case "sqrt" -> {
                        return new Mul(new Div(new Num(0.5), new Pow(u, new Num(0.5))), du);
                    }
                    default -> throw new IllegalArgumentException("Unsupported function: " + f.name);
                }
            }
            throw new IllegalArgumentException("Unsupported expression form: " + e.getClass().getSimpleName());
        }

        static Expr diff(Expr e, int order) {
            if (order <= 0) return e;
            return diff(diff(e), order - 1);
        }
    }

    // ---Integrator---
    static class Integrator {
        static Expr integrate(Expr e) {
            if (e instanceof Num n) return new Mul(n, new Var("x"));
            if (e instanceof Var v) {
                if ("x".equals(v.name)) return new Div(new Pow(v, new Num(2)), new Num(2));
                else return new Mul(v, new Var("x"));
            }
            if (e instanceof Neg n) return new Neg(integrate(n.e));
            if (e instanceof Add a) return new Add(integrate(a.a), integrate(a.b));
            if (e instanceof Sub s) return new Sub(integrate(s.a), integrate(s.b));
            if (e instanceof Mul m) {
                Expr powPart = null, expPart = null;
                if (m.a instanceof Pow pa && pa.base instanceof Var va && "x".equals(va.name) && m.b instanceof Func fb && "exp".equals(fb.name) && fb.arg instanceof Var vb && "x".equals(vb.name)) {
                    powPart = m.a; expPart = m.b;
                } else if (m.b instanceof Pow pb && pb.base instanceof Var vc && "x".equals(vc.name) && m.a instanceof Func fa && "exp".equals(fa.name) && fa.arg instanceof Var vd && "x".equals(vd.name)) {
                    powPart = m.b; expPart = m.a;
                }
                if (powPart instanceof Pow pow && pow.exp instanceof Num ne) {
                    return integrateByPartsXnExpX((int) ne.v);
                }
                Expr constPart = null, varPart = null;
                if (m.a instanceof Num || Differentiator.diff(m.a) instanceof Num nd && nd.v == 0) {
                    constPart = m.a; varPart = m.b;
                } else if (m.b instanceof Num || Differentiator.diff(m.b) instanceof Num nd2 && nd2.v == 0) {
                    constPart = m.b; varPart = m.a;
                }
                if (constPart != null) return new Mul(constPart, integrate(varPart));
                Expr subResult = trySubstitution(m);
                if (subResult != null) return subResult;
                return new Func("Integral", e);
            }
            if (e instanceof Div d) {
                if (d.a instanceof Num na && na.v == 1) {
                    if (d.b instanceof Add add && add.a instanceof Num n1 && n1.v == 1 && add.b instanceof Pow pw && pw.base instanceof Var vx && "x".equals(vx.name) && pw.exp instanceof Num n2 && n2.v == 2)
                        return new Func("arctan", new Var("x"));
                    if (d.b instanceof Func fs && "sqrt".equals(fs.name) && fs.arg instanceof Sub sub && sub.a instanceof Num n3 && n3.v == 1 && sub.b instanceof Pow pw2 && pw2.base instanceof Var vy && "x".equals(vy.name) && pw2.exp instanceof Num n4 && n4.v == 2)
                        return new Func("arcsin", new Var("x"));
                    if (d.b instanceof Var vz && "x".equals(vz.name))
                        return new Func("ln", new Var("x"));
                    if (d.b instanceof Func fs && "sqrt".equals(fs.name) && fs.arg instanceof Sub sub && sub.a instanceof Pow pa && pa.base instanceof Num && pa.exp instanceof Num ne && ne.v == 2 && sub.b instanceof Pow pb && pb.base instanceof Var vx && "x".equals(vx.name) && pb.exp instanceof Num ne2 && ne2.v == 2) {
                        double a = Math.sqrt(((Num) pa.base).v);
                        return new Func("arcsin", new Div(new Var("x"), new Num(a)));
                    }
                }
                Expr partialResult = tryPartialFractions(d);
                if (partialResult != null) return partialResult;
                return new Func("Integral", e);
            }
            if (e instanceof Pow p) {
                if (p.base instanceof Var vb && "x".equals(vb.name) && p.exp instanceof Num ne) {
                    double c = ne.v;
                    if (c == -1) return new Func("ln", p.base);
                    return new Div(new Pow(p.base, new Num(c + 1)), new Num(c + 1));
                }
                return new Func("Integral", e);
            }
            if (e instanceof Func f) {
                Expr u = f.arg;
                Expr du = Simplifier.advancedSimplify(Differentiator.diff(u));
                if (!(du instanceof Num numDu)) return new Func("Integral", e);
                if (numDu.v == 0) throw new IllegalArgumentException("Constant derivative in integration");
                Expr basicInteg;
                switch (f.name) {
                    case "sin"  -> basicInteg = new Neg(new Func("cos", u));
                    case "cos"  -> basicInteg = new Func("sin", u);
                    case "tan"  -> basicInteg = new Neg(new Func("ln", new Func("cos", u)));
                    case "ln"   -> basicInteg = new Sub(new Mul(u, new Func("ln", u)), u);
                    case "exp"  -> basicInteg = new Func("exp", u);
                    case "sec"  -> basicInteg = new Func("ln", new Add(new Func("sec", u), new Func("tan", u)));
                    case "csc"  -> basicInteg = new Neg(new Func("ln", new Add(new Func("csc", u), new Func("cot", u))));
                    case "cot"  -> basicInteg = new Func("ln", new Func("sin", u));
                    case "sqrt" -> basicInteg = new Mul(new Div(new Num(2), new Num(3)), new Pow(u, new Div(new Num(3), new Num(2))));
                    default -> {
                        return new Func("Integral", e);
                    }
                }
                return new Div(basicInteg, du);
            }
            return new Func("Integral", e);
        }

        private static Expr trySubstitution(Expr e) {
            if (!(e instanceof Mul)) return null;
            Mul m = (Mul) e;
            List<Expr> factors = flattenMul(m);
            List<Expr> candidates = collectSubexpressions(m);
            for (Expr u : candidates) {
                Expr du = Differentiator.diff(u);
                Expr duFactor = null;
                double coefficient = 1.0;
                for (Expr factor : factors) {
                    if (factor.equals(du)) { duFactor = factor; break; }
                    else if (factor instanceof Mul mul && mul.a instanceof Num num && mul.b.equals(du)) { duFactor = mul; coefficient = num.v; break; }
                    else if (factor instanceof Neg negDu && negDu.e.equals(du)) { coefficient = -1.0; duFactor = negDu; break; }
                }
                if (duFactor != null) {
                    Expr f = removeFactor(m, duFactor);
                    if (f instanceof Func func && func.arg.equals(u)) {
                        Expr basicInteg = integrateBasic(func);
                        if (!(basicInteg instanceof Func) || !((Func) basicInteg).name.equals("Integral")) {
                            Expr result = coefficient != 1.0 ? new Mul(new Num(coefficient), basicInteg) : basicInteg;
                            return substituteBack(result, u);
                        }
                    }
                }
            }
            return null;
        }

        private static List<Expr> flattenMul(Expr e) {
            List<Expr> factors = new ArrayList<>();
            if (e instanceof Mul m) { factors.addAll(flattenMul(m.a)); factors.addAll(flattenMul(m.b)); }
            else factors.add(e);
            return factors;
        }

        private static List<Expr> collectSubexpressions(Expr e) {
            List<Expr> candidates = new ArrayList<>();
            if (e instanceof Func f) { candidates.add(f.arg); candidates.addAll(collectSubexpressions(f.arg)); }
            else if (e instanceof Pow p) { candidates.add(p.base); candidates.addAll(collectSubexpressions(p.base)); candidates.addAll(collectSubexpressions(p.exp)); }
            else if (e instanceof Mul m) { candidates.addAll(collectSubexpressions(m.a)); candidates.addAll(collectSubexpressions(m.b)); }
            else if (e instanceof Add a) { candidates.addAll(collectSubexpressions(a.a)); candidates.addAll(collectSubexpressions(a.b)); }
            else if (e instanceof Sub s) { candidates.addAll(collectSubexpressions(s.a)); candidates.addAll(collectSubexpressions(s.b)); }
            return candidates;
        }

        private static Expr removeFactor(Expr e, Expr factor) {
            List<Expr> factors = flattenMul(e);
            factors.remove(factor);
            if (factors.isEmpty()) return new Num(1);
            Expr result = factors.get(0);
            for (int i = 1; i < factors.size(); i++) result = new Mul(result, factors.get(i));
            return result;
        }

        private static Expr integrateBasic(Func f) {
            switch (f.name) {
                case "sin" -> {
                    return new Neg(new Func("cos", f.arg));
                }
                case "cos" -> {
                    return new Func("sin", f.arg);
                }
                case "exp" -> {
                    return new Func("exp", f.arg);
                }
                case "ln"  -> {
                    return new Sub(new Mul(f.arg, new Func("ln", f.arg)), f.arg);
                }
                default    -> {
                    return new Func("Integral", f);
                }
            }
        }

        private static Expr substituteBack(Expr integral, Expr u) {
            if (integral instanceof Func f) return new Func(f.name, u);
            else if (integral instanceof Neg n && n.e instanceof Func f) return new Neg(new Func(f.name, u));
            else if (integral instanceof Mul m && m.a instanceof Num num) return new Mul(num, substituteBack(m.b, u));
            return integral;
        }

        private static Expr tryPartialFractions(Div d) {
            if (d.a instanceof Num na && na.v == 1 && d.b instanceof Sub sub
                    && sub.a instanceof Pow pa && pa.base instanceof Var && pa.exp instanceof Num ne && ne.v == 2
                    && sub.b instanceof Num n1 && n1.v == 1) {
                Expr term1 = new Mul(new Num(0.5), new Func("ln", new Sub(new Var("x"), new Num(1))));
                Expr term2 = new Mul(new Num(-0.5), new Func("ln", new Add(new Var("x"), new Num(1))));
                return new Add(term1, term2);
            }
            return null;
        }

        private static Expr integrateByPartsXnExpX(double n) {
            if (n != (int) n) throw new IllegalArgumentException("Non-integer exponent for x^n e^x");
            if (n == 0) return new Func("exp", new Var("x"));
            Expr u = new Pow(new Var("x"), new Num(n));
            Expr v = new Func("exp", new Var("x"));
            return new Sub(new Mul(u, v), new Mul(new Num(n), integrateByPartsXnExpX(n - 1)));
        }
    }

    // ---Simplifier---
    static class Simplifier {
        static Expr simplify(Expr e) {
            if (e instanceof Num n) return new Num(n.v);
            if (e instanceof Var v) return v;
            if (e instanceof Neg n) {
                Expr s = simplify(n.e);
                if (s instanceof Num nn) return new Num(-nn.v);
                if (s instanceof Neg nn) return simplify(nn.e);
                return new Neg(s);
            }
            if (e instanceof Add a) {
                Expr A = simplify(a.a), B = simplify(a.b);
                if (A instanceof Num na && B instanceof Num nb) return new Num(na.v + nb.v);
                if (isSin2PlusCos2(A, B) || isSin2PlusCos2(B, A)) return new Num(1);
                if (A instanceof Num na1 && na1.v == 0) return B;
                if (B instanceof Num nb1 && nb1.v == 0) return A;
                if (A.equals(B)) return new Mul(new Num(2), A);
                return new Add(A, B);
            }
            if (e instanceof Sub s) {
                Expr A = simplify(s.a), B = simplify(s.b);
                if (A instanceof Num na && B instanceof Num nb) return new Num(na.v - nb.v);
                if (B instanceof Num nb1 && nb1.v == 0) return A;
                if (A.equals(B)) return new Num(0);
                return new Sub(A, B);
            }
            if (e instanceof Mul m) {
                Expr A = simplify(m.a), B = simplify(m.b);
                if (A instanceof Num na && B instanceof Num nb) return new Num(na.v * nb.v);
                if (A instanceof Num na1 && na1.v == 0) return new Num(0);
                if (B instanceof Num nb1 && nb1.v == 0) return new Num(0);
                if (A instanceof Num na2 && na2.v == 1) return B;
                if (B instanceof Num nb2 && nb2.v == 1) return A;
                if (A.equals(B)) return new Pow(A, new Num(2));
                if (B instanceof DyDx) return A;
                return new Mul(A, B);
            }
            if (e instanceof Div d) {
                Expr A = simplify(d.a), B = simplify(d.b);
                if (A instanceof Num na && B instanceof Num nb) {
                    double val = na.v / nb.v;
                    if (Math.abs(val - Math.rint(val)) < 1e-12) return new Num(Math.rint(val));
                    int num = (int) na.v, den = (int) nb.v;
                    if (na.v == num && nb.v == den) {
                        int gcd = gcd(Math.abs(num), Math.abs(den));
                        if (gcd > 1) return new Div(new Num(num / gcd), new Num(den / gcd));
                    }
                    return new Num(val);
                }
                if (A instanceof Num na1 && na1.v == 0) return new Num(0);
                if (B instanceof Num nb1 && nb1.v == 1) return A;
                if (A.equals(B)) return new Num(1);
                return new Div(A, B);
            }
            if (e instanceof Pow p) {
                Expr base = simplify(p.base), exp = simplify(p.exp);
                if (exp instanceof Num ne) {
                    if (ne.v == 0) return new Num(1);
                    if (ne.v == 1) return base;
                }
                if (base instanceof Num nb && exp instanceof Num ne) return new Num(Math.pow(nb.v, ne.v));
                return new Pow(base, exp);
            }
            if (e instanceof Func f) {
                Expr arg = simplify(f.arg);
                if (f.name.equals("sin") && arg instanceof Num n && n.v == 0) return new Num(0);
                if (f.name.equals("cos") && arg instanceof Num n && n.v == 0) return new Num(1);
                if (f.name.equals("tan") && arg instanceof Num n && n.v == 0) return new Num(0);
                if (f.name.equals("ln") && arg instanceof Func g && g.name.equals("exp")) return g.arg;
                if (f.name.equals("exp") && arg instanceof Func g && g.name.equals("ln")) return g.arg;
                return new Func(f.name, arg);
            }
            return e;
        }

        private static boolean isSin2PlusCos2(Expr e1, Expr e2) {
            return e1 instanceof Pow p1 && p1.exp instanceof Num n1 && n1.v == 2 && p1.base instanceof Func f1 && "sin".equals(f1.name)
                    && e2 instanceof Pow p2 && p2.exp instanceof Num n2 && n2.v == 2 && p2.base instanceof Func f2 && "cos".equals(f2.name)
                    && f1.arg.equals(f2.arg);
        }

        private static int gcd(int a, int b) { return b == 0 ? a : gcd(b, a % b); }

        static Expr advancedSimplify(Expr e) {
            Expr s = simplify(e);
            if (s instanceof Sub sub) {
                Expr A = advancedSimplify(sub.a), B = advancedSimplify(sub.b);
                if (A instanceof Num na && B instanceof Num nb) return new Num(na.v - nb.v);
                return new Sub(A, B);
            }
            if (s instanceof Add a) {
                Expr A = advancedSimplify(a.a), B = advancedSimplify(a.b);
                if (A instanceof Num na && B instanceof Num nb) return new Num(na.v + nb.v);
                if (A instanceof Mul m && m.a instanceof Num num && num.v == 0) return advancedSimplify(B);
                if (B instanceof Mul m && m.a instanceof Num num && num.v == 0) return advancedSimplify(A);
                if (A instanceof Num na && B instanceof Add ab && ab.a instanceof Num nb) return advancedSimplify(new Add(new Num(na.v + nb.v), ab.b));
                return new Add(A, B);
            }
            if (s instanceof Mul m) {
                if (m.a instanceof Num na && m.b instanceof Mul mb && mb.a instanceof Num nb) return advancedSimplify(new Mul(new Num(na.v * nb.v), mb.b));
                if (m.b instanceof Add add) return advancedSimplify(new Add(new Mul(m.a, add.a), new Mul(m.a, add.b)));
            }
            if (s instanceof Pow p) {
                if (p.base instanceof Pow inner && p.exp instanceof Num nb && inner.exp instanceof Num na) return advancedSimplify(new Pow(inner.base, new Num(na.v * nb.v)));
            }
            if (s instanceof Div d) {
                if (d.a instanceof Div da) return advancedSimplify(new Div(da.a, new Mul(da.b, d.b)));
            }
            if (s instanceof Add) {
                List<Expr> flattened = flattenAdd(s);
                for (int i = 0; i < flattened.size(); i++) flattened.set(i, advancedSimplify(flattened.get(i)));
                return collectLikeTerms(flattened);
            }
            return s;
        }

        private static List<Expr> flattenAdd(Expr e) {
            List<Expr> terms = new ArrayList<>();
            if (e instanceof Add add) { terms.addAll(flattenAdd(add.a)); terms.addAll(flattenAdd(add.b)); }
            else terms.add(e);
            return terms;
        }

        private static Expr collectLikeTerms(List<Expr> terms) {
            Map<Expr, Double> coeffMap = new HashMap<>();
            double constSum = 0;
            for (Expr term : terms) {
                if (term instanceof Num num) { constSum += num.v; }
                else {
                    double coeff = 1.0;
                    Expr base = term;
                    if (term instanceof Mul mul && mul.a instanceof Num num) { coeff = num.v; base = mul.b; }
                    coeffMap.merge(base, coeff, Double::sum);
                }
            }
            List<Expr> newTerms = new ArrayList<>();
            if (constSum != 0) newTerms.add(new Num(constSum));
            for (Map.Entry<Expr, Double> entry : coeffMap.entrySet()) {
                double coeff = entry.getValue();
                if (coeff == 0) continue;
                Expr base = entry.getKey();
                if (coeff == 1) newTerms.add(base);
                else if (coeff == -1) newTerms.add(new Neg(base));
                else newTerms.add(new Mul(new Num(coeff), base));
            }
            if (newTerms.isEmpty()) return new Num(0);
            if (newTerms.size() == 1) return newTerms.get(0);
            Expr result = newTerms.get(0);
            for (int i = 1; i < newTerms.size(); i++) result = new Add(result, newTerms.get(i));
            return result;
        }

        static Expr autoSimplify(Expr e, boolean useAdvanced) {
            return useAdvanced ? advancedSimplify(e) : simplify(e);
        }
    }

    // ---Printer---
    static class Printer {
        static String print(Expr e) {
            if (e instanceof Num n) {
                if (Math.abs(n.v - Math.rint(n.v)) < 1e-12) return String.valueOf((long) Math.rint(n.v));
                return String.valueOf(n.v);
            }
            if (e instanceof Var v) return v.name;
            if (e instanceof Neg n) {
                Expr s = n.e;
                if (s instanceof Add a) return print(new Neg(a.a)) + "-" + print(a.b);
                if (s instanceof Sub sub) return print(new Neg(sub.a)) + "+" + print(sub.b);
                return "-" + wrap(n.e);
            }
            if (e instanceof Add a) return print(a.a) + " + " + print(a.b);
            if (e instanceof Sub s) return print(s.a) + " - " + print(s.b);
            if (e instanceof Mul m) {
                Expr left = m.a, right = m.b;
                if (right instanceof Num && !(left instanceof Num)) { left = m.b; right = m.a; }
                String leftStr = wrap(left);
                String rightStr = wrap(right);
                if (left instanceof Num num) {
                    if (num.v == 1) return rightStr;
                    if (num.v == -1) return "-" + rightStr;
                    if (right instanceof Var || right instanceof Func || (right instanceof Pow && ((Pow) right).base instanceof Var))
                        return leftStr + rightStr;
                }
                return leftStr + "*" + rightStr;
            }
            if (e instanceof DyDx) return "dy/dx";
            if (e instanceof Div d) return wrap(d.a) + "/" + wrap(d.b);
            if (e instanceof Pow p) return wrap(p.base) + "^" + wrapPowExp(p.exp);
            if (e instanceof Func f) {
                if ("Integral".equals(f.name)) return "Integral(" + print(f.arg) + ")";
                return f.name + "(" + print(f.arg) + ")";
            }
            return "?";
        }

        private static String wrap(Expr e) {
            if (e instanceof Num || e instanceof Var || e instanceof Func) return print(e);
            return "(" + print(e) + ")";
        }

        private static String wrapPowExp(Expr e) {
            if (e instanceof Num) return print(e);
            return "(" + print(e) + ")";
        }
    }

    // ---Numeric evaluator---
    private double evaluateAt(Expr e, double x) {
        if (e instanceof Num n) return n.v;
        if (e instanceof Var v && "x".equals(v.name)) return x;
        if (e instanceof Add a) return evaluateAt(a.a, x) + evaluateAt(a.b, x);
        if (e instanceof Sub s) return evaluateAt(s.a, x) - evaluateAt(s.b, x);
        if (e instanceof Mul m) return evaluateAt(m.a, x) * evaluateAt(m.b, x);
        if (e instanceof Div d) return evaluateAt(d.a, x) / evaluateAt(d.b, x);
        if (e instanceof Pow p) return Math.pow(evaluateAt(p.base, x), evaluateAt(p.exp, x));
        if (e instanceof Func f) {
            double arg = evaluateAt(f.arg, x);
            switch (f.name) {
                case "sin"    -> { return Math.sin(arg); }
                case "cos"    -> { return Math.cos(arg); }
                case "tan"    -> { return Math.tan(arg); }
                case "ln"     -> { return Math.log(arg); }
                case "exp"    -> { return Math.exp(arg); }
                case "sqrt"   -> { return Math.sqrt(arg); }
                case "sec"    -> { return 1 / Math.cos(arg); }
                case "csc"    -> { return 1 / Math.sin(arg); }
                case "cot"    -> { return 1 / Math.tan(arg); }
                case "arctan" -> { return Math.atan(arg); }
                case "arcsin" -> { return Math.asin(arg); }
                default -> throw new UnsupportedOperationException("Unsupported function: " + f.name);
            }
        }
        throw new UnsupportedOperationException("Cannot evaluate: " + e);
    }

    // ---Helper: parse input string into Expr---
    private Expr parseExpressionFromInput(String input) {
        String normalized = normalize(input);
        List<Token> tokens = tokenize(normalized);
        tokens = insertFunctionParens(tokens);
        tokens = implicitMultiplication(tokens);
        Parser p = new Parser(tokens);
        return p.parseExpression();
    }

    // ---Implicit differentiation helpers---
    private Map<Boolean, List<Expr>> collectTermsWithDyDx(Expr e) {
        Map<Boolean, List<Expr>> map = new HashMap<>();
        map.put(true, new ArrayList<>());
        map.put(false, new ArrayList<>());
        collectTermsWithDyDx(e, map);
        return map;
    }

    private void collectTermsWithDyDx(Expr e, Map<Boolean, List<Expr>> map) {
        if (e instanceof Add a) { collectTermsWithDyDx(a.a, map); collectTermsWithDyDx(a.b, map); }
        else if (e instanceof Sub s) { collectTermsWithDyDx(s.a, map); collectTermsWithDyDx(new Neg(s.b), map); }
        else if (hasDyDx(e)) map.get(true).add(e);
        else map.get(false).add(e);
    }

    private boolean hasDyDx(Expr e) {
        if (e instanceof DyDx) return true;
        if (e instanceof Add a) return hasDyDx(a.a) || hasDyDx(a.b);
        if (e instanceof Sub s) return hasDyDx(s.a) || hasDyDx(s.b);
        if (e instanceof Mul m) return hasDyDx(m.a) || hasDyDx(m.b);
        if (e instanceof Div d) return hasDyDx(d.a) || hasDyDx(d.b);
        if (e instanceof Neg n) return hasDyDx(n.e);
        if (e instanceof Func f) return hasDyDx(f.arg);
        if (e instanceof Pow p) return hasDyDx(p.base) || hasDyDx(p.exp);
        return false;
    }

    private Expr buildAdd(List<Expr> terms) {
        if (terms.isEmpty()) return new Num(0);
        if (terms.size() == 1) return terms.get(0);
        Expr result = terms.get(0);
        for (int i = 1; i < terms.size(); i++) result = new Add(result, terms.get(i));
        return result;
    }
}