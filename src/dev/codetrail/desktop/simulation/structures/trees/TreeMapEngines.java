package dev.codetrail.desktop.simulation.structures.trees;

import dev.codetrail.desktop.simulation.SimulationEngine;
import java.util.List;

/** Ordered factory for the bounded hash-map, tree, and trie providers. */
public final class TreeMapEngines {
    private TreeMapEngines() {
    }

    /** Preserve the curriculum order: Hash Map, BST, AVL Tree, then Trie. */
    public static List<SimulationEngine> all() {
        return List.of(
                new HashMapEngine(),
                new BstEngine(),
                new AvlTreeEngine(),
                new TrieEngine());
    }
}
