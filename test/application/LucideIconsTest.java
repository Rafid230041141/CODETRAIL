package application;

import application.client.util.LucideIcons;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.List;

/**
 * LucideIconsTest verifies that all official Lucide vector assets referenced across
 * CodeTrail are present, non-null, and correctly mapped for both dark and light themes.
 */
public class LucideIconsTest {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("            CodeTrail Lucide Icons Verification Suite                           ");
        System.out.println("================================================================================");

        int passed = 0;
        int failed = 0;

        try {
            testStatusIcons();
            passed++;
            System.out.println("[PASS] Suite 1: Status Icons (check-circle-2, play-circle, circle, lock, circle-dashed)");
        } catch (Throwable t) {
            failed++;
            System.err.println("[FAIL] Suite 1: Status Icons failed: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            testCourseCardIcons();
            passed++;
            System.out.println("[PASS] Suite 2: Course Card Icons (binary, globe, smartphone, brain-circuit, bar-chart-3, gamepad-2, terminal)");
        } catch (Throwable t) {
            failed++;
            System.err.println("[FAIL] Suite 2: Course Card Icons failed: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            testCategoryAndTopicIcons();
            passed++;
            System.out.println("[PASS] Suite 3: Category & Topic Icons (arrow-up-down, search, share-2, git-branch, grid-3x3, link, layers-3, list-ordered, rows-3, network, code-2, braces)");
        } catch (Throwable t) {
            failed++;
            System.err.println("[FAIL] Suite 3: Category & Topic Icons failed: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            testDashboardAndHeaderIcons();
            passed++;
            System.out.println("[PASS] Suite 4: Dashboard & Header Icons (layout-dashboard, user, log-out, sun, moon, list-tree, award, flame)");
        } catch (Throwable t) {
            failed++;
            System.err.println("[FAIL] Suite 4: Dashboard & Header Icons failed: " + t.getMessage());
            t.printStackTrace();
        }

        try {
            testLearningBlockIcons();
            passed++;
            System.out.println("[PASS] Suite 5: Learning Block Icons (target, globe, sparkles, help-circle, activity, gamepad-2, bar-chart-3, grid-3x3, smartphone, binary)");
        } catch (Throwable t) {
            failed++;
            System.err.println("[FAIL] Suite 5: Learning Block Icons failed: " + t.getMessage());
            t.printStackTrace();
        }

        System.out.println("================================================================================");
        System.out.printf("Results: %d Passed, %d Failed%n", passed, failed);
        System.out.println("================================================================================");

        if (failed > 0) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }

    private static void testStatusIcons() {
        String[] statuses = {"completed", "current", "not_started", "locked", "skipped"};
        for (String status : statuses) {
            ImageView ivDark = LucideIcons.statusIcon(status, true);
            assertNotNull("Status icon dark for '" + status + "'", ivDark);
            assertNotNull("Status image dark for '" + status + "'", ivDark.getImage());

            ImageView ivLight = LucideIcons.statusIcon(status, false);
            assertNotNull("Status icon light for '" + status + "'", ivLight);
            assertNotNull("Status image light for '" + status + "'", ivLight.getImage());
        }
    }

    private static void testCourseCardIcons() {
        List<String> courseSlugs = List.of(
                "dsa", "competitive-programming",
                "web-development",
                "app-development",
                "ai-ml",
                "data-science",
                "game-dev",
                "languages"
        );

        for (String slug : courseSlugs) {
            ImageView iv = LucideIcons.courseIcon(slug, 18);
            assertNotNull("Course icon for '" + slug + "'", iv);
            assertNotNull("Course image for '" + slug + "'", iv.getImage());
        }
    }

    private static void testCategoryAndTopicIcons() {
        List<String> categories = List.of(
                "Sorting Algorithms",
                "Searching",
                "Graph Algorithms",
                "Tree Traversal",
                "Array Operations",
                "Linked List",
                "Stack & Queue",
                "Queue Applications",
                "Linear Data Structures",
                "Non-Linear Structures",
                "Data Structures",
                "Languages",
                "DSA Foundation"
        );

        for (String cat : categories) {
            for (boolean dark : List.of(true, false)) {
                ImageView iv = LucideIcons.categoryIcon(cat, 16, dark);
                assertNotNull("Category icon for '" + cat + "' (dark=" + dark + ")", iv);
                assertNotNull("Category image for '" + cat + "' (dark=" + dark + ")", iv.getImage());
            }
        }
    }

    private static void testDashboardAndHeaderIcons() {
        String[] headerIcons = {
                "layout-dashboard", "user", "log-out", "sun", "moon", "list-tree", "award", "flame", "trophy"
        };

        for (String name : headerIcons) {
            for (boolean dark : List.of(true, false)) {
                ImageView iv = LucideIcons.icon(name, 16, dark);
                assertNotNull("Header icon '" + name + "' (dark=" + dark + ")", iv);
                assertNotNull("Header image '" + name + "' (dark=" + dark + ")", iv.getImage());
            }
        }

        // Test colored variants
        Image goldAward = LucideIcons.getImage("award", "gold");
        assertNotNull("Award gold variant", goldAward);

        Image orangeFlame = LucideIcons.getImage("flame", "orange");
        assertNotNull("Flame orange variant", orangeFlame);

        Image brandBinary = LucideIcons.getImage("binary", "brand");
        assertNotNull("Binary brand variant", brandBinary);
    }

    private static void testLearningBlockIcons() {
        String[] blockIcons = {
                "target", "globe", "bug", "sparkles", "help-circle", "activity",
                "gamepad-2", "bar-chart-3", "grid-3x3", "smartphone", "binary"
        };

        for (String icon : blockIcons) {
            for (boolean dark : List.of(true, false)) {
                ImageView iv = LucideIcons.icon(icon, 18, dark);
                assertNotNull("Block icon '" + icon + "' (dark=" + dark + ")", iv);
                assertNotNull("Block image '" + icon + "' (dark=" + dark + ")", iv.getImage());
            }
        }
    }

    private static void assertNotNull(String msg, Object obj) {
        if (obj == null) {
            throw new AssertionError("Assertion failed: " + msg + " is null!");
        }
    }
}
