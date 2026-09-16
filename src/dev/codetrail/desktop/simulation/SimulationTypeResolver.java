package dev.codetrail.desktop.simulation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Locale;

/**
 * Resolves curriculum and user simulation types and adapts incoming JSON
 * configurations to ensure compatibility with registered engines.
 */
public final class SimulationTypeResolver {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SimulationTypeResolver() {
    }

    public static String resolve(String type, String configJson) {
        if (type == null || type.isBlank()) {
            return "ARRAY";
        }
        String t = type.trim().toUpperCase(Locale.ROOT);
        if (t.equals("SORTING")) {
            if (configJson != null && !configJson.isBlank()) {
                try {
                    JsonNode node = MAPPER.readTree(configJson);
                    String algo = node.path("algorithm").asText("MERGE").toUpperCase(Locale.ROOT);
                    if (algo.contains("QUICK")) return "QUICK_SORT";
                    if (algo.contains("HEAP")) return "HEAP_SORT";
                    if (algo.contains("COUNT")) return "COUNTING_SORT";
                    if (algo.contains("RADIX")) return "RADIX_SORT";
                    if (algo.contains("BUCKET")) return "BUCKET_SORT";
                    return "MERGE_SORT";
                } catch (Exception ignored) {
                }
            }
            return "MERGE_SORT";
        }
        return switch (t) {
            case "AVL" -> "AVL_TREE";
            case "SCC" -> "TARJAN_SCC";
            case "BRIDGES" -> "BRIDGES_ARTICULATION";
            case "KRUSKAL" -> "KRUSKAL_MST";
            case "PRIM" -> "PRIM_MST";
            case "DIVIDE_CONQUER" -> "DIVIDE_AND_CONQUER";
            case "GREEDY" -> "GREEDY_ACTIVITY_SELECTION";
            case "BACKTRACKING" -> "BACKTRACKING_N_QUEENS";
            case "STRING_MATCHING" -> "KMP";
            case "SUFFIX_STRUCTURE" -> "SUFFIX_ARRAY";
            case "ALGEBRA" -> "MODULAR_EXPONENTIATION";
            case "LINEAR_ALGEBRA" -> "GAUSSIAN_ELIMINATION";
            case "NUMBER_THEORY" -> "EXTENDED_GCD";
            case "COMBINATORICS" -> "PASCAL_TRIANGLE";
            case "GEOMETRY" -> "CONVEX_HULL";
            case "RANGE_QUERY" -> {
                if (configJson != null && configJson.contains("offline")) yield "MO_RANGE_QUERY";
                yield "ONLINE_RANGE_QUERY";
            }
            default -> SimulationEngineRegistry.defaultRegistry().registeredTypes().contains(t) ? t : "ARRAY";
        };
    }

    public static JsonNode normalizeInput(SimulationEngine engine, String configJson) {
        SimulationMetadata meta = engine.metadata();
        if (configJson == null || configJson.isBlank() || configJson.trim().equals("{}")) {
            return meta.defaultInput().deepCopy();
        }
        try {
            JsonNode parsed = MAPPER.readTree(configJson);
            if (!parsed.isObject()) {
                return meta.defaultInput().deepCopy();
            }
            ObjectNode obj = (ObjectNode) parsed;
            // If the engine expects "array" but parsed has "values", copy "values" -> "array"
            if (obj.has("values") && !obj.has("array") && meta.defaultInput().has("array")) {
                obj.set("array", obj.get("values"));
            }
            // If the engine expects "values" but parsed has "array", copy "array" -> "values"
            if (obj.has("array") && !obj.has("values") && meta.defaultInput().has("values")) {
                obj.set("values", obj.get("array"));
            }
            // Verify that generateSteps succeeds with this input:
            engine.generateSteps(obj);
            return obj;
        } catch (Exception ex) {
            // Gracefully fall back to engine's guaranteed valid default input
            return meta.defaultInput().deepCopy();
        }
    }
}
