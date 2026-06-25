package dev.takesome.helix.ui.binding;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import dev.takesome.helix.ui.binding.UiBindingSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Safe numeric/boolean mini-DSL for UI binding expressions.
 *
 * Supported primitives are numbers, data paths, parentheses, +, -, *, /, %,
 * comparisons, &&, ||, ! and pure numeric helper functions such as max, min,
 * clamp, abs, floor, ceil, round, sqrt and pow.
 */
public final class UiBindingExpressionEvaluator {
    public float number(String expression, UiBindingSource source, float fallback) {
        if (expression == null || expression.isBlank()) return fallback;
        try {
            Parser parser = new Parser(expression, source);
            float value = parser.parseNumberRoot();
            return Float.isFinite(value) ? value : fallback;
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    public boolean bool(String expression, UiBindingSource source, boolean fallback) {
        if (expression == null || expression.isBlank()) return fallback;
        try {
            Parser parser = new Parser(expression, source);
            return parser.parseBooleanRoot();
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    public void validate(String expression) {
        if (expression == null || expression.isBlank()) return;
        Parser parser = new Parser(expression, EmptySource.INSTANCE);
        parser.parseBooleanRoot();
    }

    private enum EmptySource implements UiBindingSource {
        INSTANCE;

        @Override
        public String text(String key) { return ""; }

        @Override
        public float number(String key) { return 0f; }

        @Override
        public boolean bool(String key) { return false; }
    }

    private static final class Parser {
        private final String expression;
        private final UiBindingSource source;
        private int index;

        private Parser(String expression, UiBindingSource source) {
            this.expression = emptyIfNull(expression);
            this.source = source == null ? EmptySource.INSTANCE : source;
        }

        private float parseNumberRoot() {
            float value = parseNumberExpression();
            skipWhitespace();
            if (!end()) throw error("Unexpected token");
            return value;
        }

        private boolean parseBooleanRoot() {
            boolean value = parseOr();
            skipWhitespace();
            if (!end()) throw error("Unexpected token");
            return value;
        }

        private boolean parseOr() {
            boolean value = parseAnd();
            while (true) {
                skipWhitespace();
                if (!match("||")) return value;
                value = value | parseAnd();
            }
        }

        private boolean parseAnd() {
            boolean value = parseNot();
            while (true) {
                skipWhitespace();
                if (!match("&&")) return value;
                value = value & parseNot();
            }
        }

        private boolean parseNot() {
            skipWhitespace();
            if (match("!")) return !parseNot();
            if (match("(")) {
                boolean value = parseOr();
                expect(")");
                return value;
            }
            return parseComparison();
        }

        private boolean parseComparison() {
            float left = parseNumberExpression();
            skipWhitespace();
            if (match(">=")) return left >= parseNumberExpression();
            if (match("<=")) return left <= parseNumberExpression();
            if (match("==")) return Math.abs(left - parseNumberExpression()) <= 0.00001f;
            if (match("!=")) return Math.abs(left - parseNumberExpression()) > 0.00001f;
            if (match(">")) return left > parseNumberExpression();
            if (match("<")) return left < parseNumberExpression();
            return Math.abs(left) > 0.00001f;
        }

        private float parseNumberExpression() {
            float value = parseTerm();
            while (true) {
                skipWhitespace();
                if (match("+")) value += parseTerm();
                else if (match("-")) value -= parseTerm();
                else return value;
            }
        }

        private float parseTerm() {
            float value = parseUnary();
            while (true) {
                skipWhitespace();
                if (match("*")) value *= parseUnary();
                else if (match("/")) {
                    float divisor = parseUnary();
                    value = Math.abs(divisor) <= 0.000001f ? 0f : value / divisor;
                } else if (match("%")) {
                    float divisor = parseUnary();
                    value = Math.abs(divisor) <= 0.000001f ? 0f : value % divisor;
                } else return value;
            }
        }

        private float parseUnary() {
            skipWhitespace();
            if (match("+")) return parseUnary();
            if (match("-")) return -parseUnary();
            return parsePrimary();
        }

        private float parsePrimary() {
            skipWhitespace();
            if (match("(")) {
                float value = parseNumberExpression();
                expect(")");
                return value;
            }
            if (digit(peek()) || peek() == '.') return parseNumberLiteral();
            String identifier = parseIdentifier();
            if (identifier.isBlank()) throw error("Expected number, path or function");
            skipWhitespace();
            if (match("(")) return parseFunction(identifier);
            return readNumber(identifier);
        }

        private float parseFunction(String rawName) {
            String name = rawName.toLowerCase(Locale.ROOT);
            List<Float> args = new ArrayList<>();
            skipWhitespace();
            if (!match(")")) {
                do {
                    args.add(parseNumberExpression());
                    skipWhitespace();
                } while (match(","));
                expect(")");
            }
            return switch (name) {
                case "max" -> args.stream().reduce(Float.NEGATIVE_INFINITY, Math::max);
                case "min" -> args.stream().reduce(Float.POSITIVE_INFINITY, Math::min);
                case "clamp" -> clamp(arg(args, 0, 0f), arg(args, 1, 0f), arg(args, 2, 1f));
                case "abs" -> Math.abs(arg(args, 0, 0f));
                case "floor" -> (float) Math.floor(arg(args, 0, 0f));
                case "ceil" -> (float) Math.ceil(arg(args, 0, 0f));
                case "round" -> Math.round(arg(args, 0, 0f));
                case "sqrt" -> (float) Math.sqrt(Math.max(0f, arg(args, 0, 0f)));
                case "pow" -> (float) Math.pow(arg(args, 0, 0f), arg(args, 1, 1f));
                default -> readNumber(rawName);
            };
        }

        private String parseIdentifier() {
            skipWhitespace();
            int start = index;
            while (!end()) {
                char c = expression.charAt(index);
                if (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '$') index++;
                else break;
            }
            return expression.substring(start, index).trim();
        }

        private float parseNumberLiteral() {
            int start = index;
            boolean dot = false;
            while (!end()) {
                char c = expression.charAt(index);
                if (digit(c)) index++;
                else if (c == '.' && !dot) {
                    dot = true;
                    index++;
                } else break;
            }
            return Float.parseFloat(expression.substring(start, index));
        }

        private float readNumber(String key) {
            if (key == null || key.isBlank()) return 0f;
            String normalized = key.trim();
            if ("true".equalsIgnoreCase(normalized)) return 1f;
            if ("false".equalsIgnoreCase(normalized)) return 0f;
            float numeric = 0f;
            try {
                numeric = source.number(normalized);
                if (Math.abs(numeric) > 0.00001f) return numeric;
            } catch (RuntimeException ignored) {
                numeric = 0f;
            }
            try {
                return source.bool(normalized) ? 1f : numeric;
            } catch (RuntimeException ignored) {
                return numeric;
            }
        }

        private void expect(String token) {
            if (!match(token)) throw error("Expected `" + token + "`");
        }

        private boolean match(String token) {
            skipWhitespace();
            if (!expression.startsWith(token, index)) return false;
            index += token.length();
            return true;
        }

        private void skipWhitespace() {
            while (!end() && Character.isWhitespace(expression.charAt(index))) index++;
        }

        private char peek() {
            return end() ? ' ' : expression.charAt(index);
        }

        private boolean end() {
            return index >= expression.length();
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at " + index + " in `" + expression + "`");
        }

        private static boolean digit(char c) {
            return c >= '0' && c <= '9';
        }

        private static float arg(List<Float> args, int index, float fallback) {
            return index >= 0 && index < args.size() ? args.get(index) : fallback;
        }

        private static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
