package com.wapo.view.menu

sealed class MenuItem(
        val id: String,
        val name: CharSequence,
        val type: MenuItemType
)

enum class MenuItemType {
    DEFAULT,
    AUTHOR
}

class FeaturedItem(
    id: String,
    name: CharSequence,
    val sectionIndex: Int,
    type: MenuItemType = MenuItemType.DEFAULT
): MenuItem(id, name, type) {
    var hasChildren = true
}

class AzItem(
    id: String,
    name: CharSequence,
    val sectionIndex: Int,
    type: MenuItemType = MenuItemType.DEFAULT
): MenuItem(id, name, type) {
    var isLastItem = false
}

class FooterItem: MenuItem("", "FOOTER", MenuItemType.DEFAULT)

class RecentListItem(val items: List<MenuItem>): MenuItem("", "Recent", MenuItemType.DEFAULT)

class RecentItem(
    id: String,
    name: CharSequence,
    type: MenuItemType = MenuItemType.DEFAULT
): MenuItem(id, name, type) {
    var isLastItem = false
}

class DividerItem(val leftMargin: Int = 0, val topMargin: Int = 0, val rightMargin: Int = 0,
                  val bottomMargin: Int = 0, val hideRuler: Boolean = false): MenuItem("", "Divider", MenuItemType.DEFAULT)