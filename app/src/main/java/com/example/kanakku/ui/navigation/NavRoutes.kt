package com.example.kanakku.ui.navigation

/**
 * Navigation routes for the Kanakku app.
 *
 * Defines route constants for all screens in the application, including
 * main screens accessed via bottom navigation and secondary screens.
 *
 * Follows offline-first architecture with proper type-safe navigation.
 */
object NavRoutes {
    /**
     * Route for the category management screen.
     *
     * Shows all categories with search, filter, and CRUD operations.
     */
    const val CATEGORY_MANAGEMENT = "category_management"

    /**
     * Base route for the category editor screen.
     *
     * Use [categoryEditorRoute] to create routes with parameters.
     */
    const val CATEGORY_EDITOR = "category_editor"

    /**
     * Route pattern for category editor with optional categoryId argument.
     *
     * Format: "category_editor?categoryId={categoryId}"
     */
    const val CATEGORY_EDITOR_ROUTE = "$CATEGORY_EDITOR?categoryId={categoryId}"

    /**
     * Argument key for category ID in category editor route.
     */
    const val ARG_CATEGORY_ID = "categoryId"

    /**
     * Creates a category editor route with optional category ID.
     *
     * @param categoryId Optional category ID for editing existing category.
     *                   If null, creates a new category.
     * @return Navigation route string
     */
    fun categoryEditorRoute(categoryId: String? = null): String {
        return if (categoryId != null) {
            "$CATEGORY_EDITOR?categoryId=$categoryId"
        } else {
            CATEGORY_EDITOR
        }
    }
}
