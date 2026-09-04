package com.jtool.android.core.localization.domains

import com.jtool.android.core.localization.Loc

object LinksLoc {
    val links get() = Loc.s("links_links", "Links")
    val searchLinks get() = Loc.s("links_searchLinks", "Search links...")
    val linkTitle get() = Loc.s("links_linkTitle", "Title")
    val linkUrl get() = Loc.s("links_linkUrl", "URL")
    val addLink get() = Loc.s("links_addLink", "Add Link")
    val invalidUrl get() = Loc.s("links_invalidUrl", "Invalid URL")
    val copy get() = Loc.s("links_copy", "Copy")
    val copied get() = Loc.s("links_copied", "Copied!")
    val urlHint get() = Loc.s("links_urlHint", "https://...")
    val confirmDeleteLinkTitle get() = Loc.s("links_confirmDeleteLinkTitle", "Delete Link?")
    val confirmDeleteLinkMsg get() = Loc.s("links_confirmDeleteLinkMsg", "Are you sure you want to delete this link?")
}
