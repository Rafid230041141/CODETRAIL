package dev.codetrail.desktop.simulation.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/** Student text fields for the current simulation inputs; engines retain validation authority. */
public final class SimulationInputAdapter {
    public enum Kind { NUMBER, NUMBERS, TEXT, WORDS, ROWS, OPERATIONS, CHOICE }
    public record Field(String key, String label, String help, Kind kind, List<String> choices) {
        public boolean multiline() { return kind == Kind.ROWS || kind == Kind.OPERATIONS; }
    }
    private static final Set<String> OP_ARGUMENTS = Set.of("index", "value", "key", "word", "text", "left", "right", "delta");
    private static final Set<String> FIXED_DIRECTION = Set.of("PRIM_MST", "KRUSKAL_MST", "TOPOLOGICAL_SORT", "TARJAN_SCC", "BRIDGES_ARTICULATION", "MAX_FLOW");
    private final String type;
    private final ObjectNode defaults;
    private final List<Field> fields;

    public SimulationInputAdapter(SimulationMetadata metadata) {
        type = metadata.type();
        defaults = (ObjectNode) metadata.defaultInput();
        ArrayList<Field> result = new ArrayList<>();
        defaults.fieldNames().forEachRemaining(key -> {
            if (key.equals("algorithm") || key.equals("collision")
                    || key.equals("directed") && FIXED_DIRECTION.contains(type)
                    || defaults.has("operation") && OP_ARGUMENTS.contains(key)) return;
            result.add(describe(key));
        });
        fields = List.copyOf(result);
    }

    public List<Field> fields() { return fields; }

    public Map<String, String> example() { return display(defaults); }

    /** Published inputs use the same form, including a configured operation different from the default. */
    public Map<String, String> display(JsonNode input) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (Field field : fields) {
            JsonNode value = input.has(field.key()) ? input.get(field.key()) : defaults.get(field.key());
            if (field.key().equals("operation")) {
                if (input.has("operations")) values.put(field.key(), operationRows(input.get("operations")));
                else values.put(field.key(), operationText(input.has("operation") ? input : defaults));
            } else if (field.kind() == Kind.OPERATIONS) values.put(field.key(), operationRows(value));
            else if (field.kind() == Kind.ROWS) values.put(field.key(), rowsText(field.key(), value));
            else if (field.kind() == Kind.NUMBERS || field.kind() == Kind.WORDS) values.put(field.key(), join(value, " "));
            else if (field.key().equals("directed")) values.put(field.key(), input.path("directed").asBoolean(false) ? "One-way edges" : "Two-way edges");
            else if (field.key().equals("representation")) values.put(field.key(), value.asText().equals("adjacency-matrix") ? "Adjacency matrix" : "Adjacency list");
            else values.put(field.key(), value.asText());
        }
        return values;
    }

    public ObjectNode parse(Map<String, String> values) {
        ObjectNode input = defaults.deepCopy();
        int total = values.values().stream().mapToInt(value -> value == null ? 0 : value.length()).sum();
        if (total > 8_192) throw new IllegalArgumentException("Keep your input within 8,192 characters.");
        for (Field field : fields) {
            String value = values.getOrDefault(field.key(), "");
            if (value == null) value = "";
            switch (field.kind()) {
                case NUMBER -> input.set(field.key(), integer(value.trim(), field.label()));
                case TEXT -> input.put(field.key(), value);
                case NUMBERS, WORDS -> {
                    ArrayNode array = input.putArray(field.key());
                    String[] tokens = tokens(value);
                    for (int i = 0; i < tokens.length; i++) {
                        if (field.kind() == Kind.WORDS) array.add(tokens[i]);
                        else array.add(integer(tokens[i], field.label() + ", number " + (i + 1)));
                    }
                }
                case ROWS -> input.set(field.key(), parseRows(field, value));
                case OPERATIONS -> {
                    ArrayNode operations = JsonNodeFactory.instance.arrayNode();
                    String[] rows = lines(value);
                    for (int i = 0; i < rows.length; i++) operations.add(parseOperation(rows[i], i + 1));
                    if (field.key().equals("operation")) {
                        for (String argument : OP_ARGUMENTS) input.remove(argument);
                        input.remove("operation");
                        input.remove("operations");
                        if (operations.size() == 1) {
                            ObjectNode operation = (ObjectNode) operations.get(0);
                            input.put("operation", operation.remove("kind").asText());
                            input.setAll(operation);
                        } else if (type.equals("ARRAY") || type.equals("LINKED_LIST")) input.set("operations", operations);
                        else throw new IllegalArgumentException("Enter exactly one operation for this simulation.");
                    } else input.set(field.key(), operations);
                }
                case CHOICE -> {
                    if (!field.choices().contains(value)) throw new IllegalArgumentException("Choose " + field.label().toLowerCase(Locale.ROOT) + ".");
                    if (field.key().equals("directed")) input.put(field.key(), value.equals("One-way edges"));
                    else input.put(field.key(), value.equals("Adjacency matrix") ? "adjacency-matrix" : "adjacency-list");
                }
            }
        }
        return input;
    }

    private Field describe(String key) {
        String label = label(key);
        Kind kind;
        String help;
        List<String> choices = List.of();
        switch (key) {
            case "operation", "operations" -> { kind = Kind.OPERATIONS; help = operationHelp(); }
            case "array", "values", "machines" -> {
                kind = Kind.NUMBERS;
                help = "Separate whole numbers with spaces or commas. " + numberBounds(key);
            }
            case "words" -> { kind = Kind.WORDS; help = "Separate lowercase words with spaces or commas."; }
            case "text", "pattern" -> {
                kind = Kind.TEXT;
                help = key.equals("pattern") ? "1–16 lowercase letters a–z." :
                        (Set.of("SUFFIX_ARRAY", "SUFFIX_AUTOMATON").contains(type) ? "0–12" : "0–32") + " lowercase letters a–z. Blank is allowed.";
            }
            case "edges", "points", "matrix", "activities", "items", "entries", "queries" -> {
                kind = Kind.ROWS; help = rowHelp(key);
            }
            case "directed" -> { kind = Kind.CHOICE; help = "Choose whether each edge can be followed in one direction or both."; choices = List.of("Two-way edges", "One-way edges"); }
            case "representation" -> { kind = Kind.CHOICE; help = "Choose how to display the graph."; choices = List.of("Adjacency list", "Adjacency matrix"); }
            case "n", "vertices", "source", "sink", "target", "limit", "a", "b", "base", "exponent", "modulus", "capacity", "blockSize", "left", "right" -> {
                kind = Kind.NUMBER; help = scalarHelp(key);
            }
            default -> throw new IllegalArgumentException("This simulation's input form is not available yet.");
        }
        return new Field(key, label, help, kind, choices);
    }

    private String label(String key) {
        return switch (key) {
            case "array", "values" -> "Numbers";
            case "machines" -> "Time per item for each machine";
            case "target" -> type.equals("BINARY_SEARCH_ANSWER") ? "Items to produce" : "Number to find";
            case "n" -> switch (type) {
                case "RECURSION" -> "Number to find the factorial of";
                case "PASCAL_TRIANGLE" -> "Last row";
                case "BACKTRACKING_N_QUEENS" -> "Board size / Number of queens";
                default -> "Number of vertices";
            };
            case "vertices" -> "Number of elements";
            case "source" -> "Starting vertex";
            case "sink" -> "Destination vertex";
            case "directed" -> "Edge direction";
            case "representation" -> "Graph display";
            case "limit" -> "Upper limit (inclusive)";
            case "a" -> "First number";
            case "b" -> "Second number";
            case "capacity" -> type.equals("DYNAMIC_PROGRAMMING") ? "Knapsack capacity" : "Capacity";
            case "blockSize" -> "Numbers per block";
            case "left" -> "First index";
            case "right" -> "Last index (inclusive)";
            case "operation", "operations" -> "Operations";
            case "matrix" -> "Equations";
            case "entries" -> "Starting key and value pairs";
            case "queries" -> "Ranges to query";
            default -> Character.toUpperCase(key.charAt(0)) + key.substring(1);
        };
    }

    private String numberBounds(String key) {
        if (key.equals("machines")) return "1–24 positive machine times.";
        if (type.equals("DIVIDE_AND_CONQUER")) return "1–16 numbers, each from −999 to 999.";
        if (key.equals("array")) return "Up to 24 numbers, each from −999 to 999." + (type.equals("BINARY_SEARCH") ? " Use ascending order." : "");
        return "Positions start at index 0.";
    }

    private String scalarHelp(String key) {
        if (key.equals("n")) return switch (type) {
            case "RECURSION" -> "A whole number from 0 to 10.";
            case "PASCAL_TRIANGLE" -> "0–12. Build rows 0 through this row.";
            case "BACKTRACKING_N_QUEENS" -> "1–6. Use a square board with this many queens.";
            default -> (type.equals("GRAPH_REPRESENTATION") ? "1–8" : "1–10") + " vertices, numbered from 0.";
        };
        return switch (key) {
            case "vertices" -> "1–12 elements, numbered from 0.";
            case "source", "sink", "left", "right" -> "Indices start at 0; use an index in your input.";
            case "limit" -> "A whole number from 0 to 120.";
            case "a", "b" -> "A whole number from −1,000,000 to 1,000,000.";
            case "base" -> "A whole number from −1,000,000,000 to 1,000,000,000.";
            case "exponent" -> "A whole number from 0 to 1,000,000,000.";
            case "modulus" -> "A whole number from 1 to 1,000,000,007.";
            case "target" -> type.equals("BINARY_SEARCH_ANSWER") ? "1–1,000,000,000 items." : "A whole number from −999 to 999.";
            case "capacity" -> type.equals("DYNAMIC_PROGRAMMING") ? "A whole number from 0 to 20." : "Enter a whole-number capacity.";
            case "blockSize" -> "Enter a positive number of values per block.";
            default -> "Enter a whole number.";
        };
    }

    private String[] columns(String key) {
        return switch (key) {
            case "edges" -> {
                JsonNode first = defaults.path("edges").get(0);
                yield first != null && first.isObject() ? new String[]{"from", "to", type.equals("MAX_FLOW") ? "capacity" : "weight"} : new String[0];
            }
            case "activities" -> new String[]{"start", "finish"};
            case "items" -> new String[]{"weight", "value"};
            case "entries" -> new String[0];
            case "queries" -> defaults.path("queries").path(0).isObject() ? new String[]{"left", "right"} : new String[0];
            default -> new String[0];
        };
    }

    private String rowHelp(String key) {
        return switch (key) {
            case "edges" -> "One edge per line: From  To" + (columns(key).length == 3 ? type.equals("MAX_FLOW") ? "  Capacity" : "  Weight" : "")
                    + ". Up to 24 edges; vertex numbers start at 0. Separate columns with spaces or commas.";
            case "points" -> "One point per line: X  Y. Use 1–12 points; coordinates from −1,000,000 to 1,000,000.";
            case "matrix" -> "One equation per line: coefficients, then result. Example: 2 1 5 means 2x₁ + x₂ = 5. Use 1–4 rows and 1–4 variables; decimals are allowed.";
            case "activities" -> "One activity per line: Start  Finish. At most 12; finish must be later than start.";
            case "items" -> "One item per line: Weight  Value. At most 10; weight 1–999, value −999 to 999.";
            case "entries" -> "One pair per line: Key  Value. Separate whole numbers with spaces or commas.";
            case "queries" -> "One range per line: First index  Last index (inclusive). Indices start at 0.";
            default -> "One row per line. Separate numbers with spaces or commas.";
        };
    }

    private ArrayNode parseRows(Field field, String source) {
        ArrayNode rows = JsonNodeFactory.instance.arrayNode();
        String[] columns = columns(field.key());
        String[] lines = lines(source);
        int matrixWidth = -1;
        for (int i = 0; i < lines.length; i++) {
            String[] cells = tokens(lines[i]);
            String at = field.label() + ", row " + (i + 1);
            int width = field.key().equals("matrix") ? cells.length : columns.length == 0 ? 2 : columns.length;
            if (cells.length != width) throw new IllegalArgumentException(at + " needs " + width + " numbers. " + rowHelp(field.key()));
            if (field.key().equals("matrix")) {
                if (width < 2 || width > 5) throw new IllegalArgumentException(at + " needs 1–4 coefficients followed by the result.");
                if (matrixWidth != -1 && matrixWidth != width) throw new IllegalArgumentException(at + " needs " + matrixWidth + " numbers, like the first equation.");
                matrixWidth = width;
            }
            if (columns.length > 0) {
                ObjectNode row = rows.addObject();
                for (int c = 0; c < width; c++) row.set(columns[c], integer(cells[c], at + ", column " + (c + 1)));
                if (field.key().equals("activities") && row.path("start").asLong() >= row.path("finish").asLong())
                    throw new IllegalArgumentException("Activity " + (i + 1) + ": finish must be later than start.");
            } else {
                ArrayNode row = rows.addArray();
                for (int c = 0; c < width; c++) {
                    if (field.key().equals("matrix")) {
                        try {
                            double number = Double.parseDouble(cells[c]);
                            if (!Double.isFinite(number)) throw new NumberFormatException();
                            if (cells[c].matches("[+-]?\\d+")) row.add(integer(cells[c], at + ", column " + (c + 1)));
                            else row.add(number);
                        } catch (NumberFormatException ex) { throw new IllegalArgumentException(at + ", column " + (c + 1) + ": enter a finite number."); }
                    } else row.add(integer(cells[c], at + ", column " + (c + 1)));
                }
            }
        }
        return rows;
    }

    private String rowsText(String key, JsonNode rows) {
        StringJoiner result = new StringJoiner("\n");
        for (JsonNode row : rows) {
            if (row.isArray()) result.add(join(row, " "));
            else {
                StringJoiner values = new StringJoiner(" ");
                for (String column : columns(key)) values.add(row.path(column).asText());
                result.add(values.toString());
            }
        }
        return result.toString();
    }

    private static JsonNode integer(String value, String label) {
        try { return JsonNodeFactory.instance.numberNode(Long.parseLong(value)); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(label + ": enter a whole number (no brackets or quotes)."); }
    }

    private static String[] tokens(String source) { return source.isBlank() ? new String[0] : source.trim().split("[\\s,]+"); }
    private static String[] lines(String source) { return source.isBlank() ? new String[0] : source.strip().split("\\R"); }
    private static String join(JsonNode values, String separator) {
        StringJoiner text = new StringJoiner(separator);
        for (JsonNode value : values) text.add(value.asText());
        return text.toString();
    }

    private Map<String, String[]> operationArguments() {
        LinkedHashMap<String, String[]> commands = new LinkedHashMap<>();
        switch (type) {
            case "ARRAY", "LINKED_LIST" -> {
                commands.put("insert", new String[]{"index", "value"}); commands.put("delete", new String[]{"index"});
                if (type.equals("LINKED_LIST")) commands.put("reverse", new String[0]);
            }
            case "HEAP" -> { commands.put("insert", new String[]{"value"}); commands.put("extract", new String[0]); }
            case "BST", "AVL_TREE" -> {
                for (String command : List.of("insert", "search")) commands.put(command, new String[]{"value"});
                if (type.equals("BST")) commands.put("delete", new String[]{"value"});
                commands.put("inorder", new String[0]);
            }
            case "HASH_MAP" -> { commands.put("put", new String[]{"key", "value"}); commands.put("get", new String[]{"key"}); commands.put("remove", new String[]{"key"}); }
            case "TRIE" -> {
                for (String command : List.of("insert", "search", "prefix")) commands.put(command, new String[]{"word"});
                commands.put("match", new String[]{"text"});
            }
            case "STACK" -> { commands.put("push", new String[]{"value"}); commands.put("pop", new String[0]); }
            case "QUEUE" -> { commands.put("enqueue", new String[]{"value"}); commands.put("dequeue", new String[0]); }
            case "DSU" -> { commands.put("union", new String[]{"a", "b"}); commands.put("find", new String[]{"x"}); }
            case "SEGMENT_TREE" -> { commands.put("sum", new String[]{"left", "right"}); commands.put("minimum", new String[]{"left", "right"}); commands.put("set", new String[]{"index", "value"}); }
            case "FENWICK_TREE" -> { commands.put("add", new String[]{"index", "delta"}); commands.put("sum", new String[]{"index"}); }
            case "SPARSE_TABLE" -> commands.put("minimum", new String[]{"left", "right"});
            case "SEGMENT_TREE_LAZY" -> { commands.put("add", new String[]{"left", "right", "delta"}); commands.put("sum", new String[]{"left", "right"}); }
            case "SQRT_DECOMPOSITION", "ONLINE_RANGE_QUERY" -> { commands.put("set", new String[]{"index", "value"}); commands.put("sum", new String[]{"left", "right"}); }
            default -> throw new IllegalArgumentException("This simulation's operation form is not available yet.");
        }
        return commands;
    }

    private String operationHelp() {
        StringJoiner commands = new StringJoiner("; ");
        operationArguments().forEach((command, arguments) -> {
            StringJoiner words = new StringJoiner(" "); words.add(command);
            for (String argument : arguments) words.add(switch (argument) {
                case "left" -> "first-index"; case "right" -> "last-index"; case "delta" -> "amount";
                case "a" -> "first-element"; case "b" -> "second-element"; case "x" -> "element";
                default -> argument;
            });
            commands.add(words.toString());
        });
        return "One operation per line: " + commands + ". Replace the words after each command with your values. Indices start at 0.";
    }

    private String internalCommand(String command) {
        return switch (type) {
            case "SEGMENT_TREE", "SPARSE_TABLE" -> switch (command) { case "sum" -> "range-sum"; case "minimum" -> "range-min"; case "set" -> "point-set"; default -> command; };
            case "FENWICK_TREE" -> command.equals("add") ? "update" : "query";
            case "SEGMENT_TREE_LAZY" -> command.equals("add") ? "range-add" : "range-sum";
            case "SQRT_DECOMPOSITION", "ONLINE_RANGE_QUERY" -> command.equals("set") ? "update" : "query";
            default -> command;
        };
    }

    private String operationText(JsonNode operation) {
        String internal = operation.path(operation.has("operation") ? "operation" : "kind").asText();
        if ((type.equals("BST") || type.equals("AVL_TREE")) && (internal.equals("in-order") || internal.equals("in_order"))) internal = "inorder";
        if (type.equals("TRIE") && internal.equals("has")) internal = "search";
        for (Map.Entry<String, String[]> candidate : operationArguments().entrySet()) {
            if (!internalCommand(candidate.getKey()).equals(internal)) continue;
            StringJoiner result = new StringJoiner(" "); result.add(candidate.getKey());
            for (String argument : candidate.getValue()) {
                JsonNode value = operation.path(argument);
                if (argument.equals("value") && !operation.has("value") && (type.equals("BST") || type.equals("AVL_TREE"))) value = operation.path("key");
                result.add(value.asText());
            }
            return result.toString().stripTrailing();
        }
        throw new IllegalArgumentException("The example contains an unsupported operation.");
    }

    private String operationRows(JsonNode operations) {
        StringJoiner result = new StringJoiner("\n");
        for (JsonNode operation : operations) result.add(operationText(operation));
        return result.toString().stripTrailing();
    }

    private ObjectNode parseOperation(String source, int row) {
        String[] words = tokens(source);
        Map<String, String[]> commands = operationArguments();
        if (words.length == 0 || !commands.containsKey(words[0].toLowerCase(Locale.ROOT)))
            throw new IllegalArgumentException("Operation " + row + ": use " + String.join(", ", commands.keySet()) + ".");
        String command = words[0].toLowerCase(Locale.ROOT);
        String[] arguments = commands.get(command);
        boolean emptyMatch = type.equals("TRIE") && command.equals("match") && words.length == 1;
        if (!emptyMatch && words.length != arguments.length + 1) throw new IllegalArgumentException("Operation " + row + ": " + command + " needs " + arguments.length + " value(s). " + operationHelp());
        ObjectNode operation = JsonNodeFactory.instance.objectNode();
        operation.put("kind", internalCommand(command));
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].equals("word") || arguments[i].equals("text")) operation.put(arguments[i], emptyMatch ? "" : words[i + 1]);
            else operation.set(arguments[i], integer(words[i + 1], "Operation " + row + ", value " + (i + 1)));
        }
        return operation;
    }

    /** Preserve the engine's exact bounds while removing transport and implementation vocabulary. */
    public String friendlyError(String message) {
        if (message == null || message.isBlank()) return "Check your input and try again.";
        String result = message.replace(type + " ", "");
        if (result.contains("fields must be exactly") || result.contains("algorithm") || result.contains("JSON object"))
            return "This example could not be loaded. Use Reset example and try again.";
        result = result.replace("JSON array", "list").replace("lowercase ASCII", "lowercase a–z")
                .replace("in the inclusive range", "between").replace("bounded integers", "whole numbers")
                .replace("bounded integer", "whole number").replace("integer", "whole number");
        for (Field field : fields) {
            String prefix = "input " + field.key() + " ";
            if (result.startsWith(prefix)) result = field.label() + " " + result.substring(prefix.length());
            else if (result.startsWith(field.key() + " ")) result = field.label() + " " + result.substring(field.key().length() + 1);
        }
        for (String key : List.of("blockSize", "vertices", "operations", "machines", "source", "sink", "array", "values", "target", "left", "right"))
            result = result.replaceAll("\\b" + Pattern.quote(key) + "\\b", java.util.regex.Matcher.quoteReplacement(label(key).toLowerCase(Locale.ROOT)));
        result = result.replaceAll("\\b[A-Z]+(?:_[A-Z0-9]+)+\\b", "Simulation");
        return Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }
}
