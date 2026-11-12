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
    val filterLinks get() = Loc.s("links_filterLinks", "Filter Links")
    val filterCategory get() = Loc.s("links_filterCategory", "Category")
    val tag get() = Loc.s("links_tag", "Tag")

    // 20 Link Tags
    val tagCommon get() = Loc.s("links_tag_common", "Common")
    val tagPlaces get() = Loc.s("links_tag_places", "Places")
    val tagWantToGo get() = Loc.s("links_tag_want_to_go", "Want to Go")
    val tagShopping get() = Loc.s("links_tag_shopping", "Shopping")
    val tagRecipe get() = Loc.s("links_tag_recipe", "Recipe")
    val tagDocument get() = Loc.s("links_tag_document", "Document")
    val tagInfo get() = Loc.s("links_tag_info", "Information")
    val tagWork get() = Loc.s("links_tag_work", "Work")
    val tagSocial get() = Loc.s("links_tag_social", "Social")
    val tagVideo get() = Loc.s("links_tag_video", "Video")
    val tagArticle get() = Loc.s("links_tag_article", "Article")
    val tagMusic get() = Loc.s("links_tag_music", "Music & Audio")
    val tagEvent get() = Loc.s("links_tag_event", "Event & Ticket")
    val tagFinance get() = Loc.s("links_tag_finance", "Finance & Banking")
    val tagTravel get() = Loc.s("links_tag_travel", "Travel & Booking")
    val tagTech get() = Loc.s("links_tag_tech", "Tech & Tools")
    val tagHealth get() = Loc.s("links_tag_health", "Health & Fitness")
    val tagEducation get() = Loc.s("links_tag_education", "Education & Learning")
    val tagEntertainment get() = Loc.s("links_tag_entertainment", "Entertainment")
    val tagPortfolio get() = Loc.s("links_tag_portfolio", "Portfolio & Project")

    fun getLocalizedLinkTag(tagKey: String): String {
        return when (tagKey.trim().uppercase()) {
            "#COMMON#", "COMMON" -> tagCommon
            "#PLACES#", "PLACES" -> tagPlaces
            "#WANT_TO_GO#", "WANT_TO_GO" -> tagWantToGo
            "#SHOPPING#", "SHOPPING" -> tagShopping
            "#RECIPE#", "RECIPE" -> tagRecipe
            "#DOCUMENT#", "DOCUMENT" -> tagDocument
            "#INFO#", "INFO" -> tagInfo
            "#WORK#", "WORK" -> tagWork
            "#SOCIAL#", "SOCIAL" -> tagSocial
            "#VIDEO#", "VIDEO" -> tagVideo
            "#ARTICLE#", "ARTICLE" -> tagArticle
            "#MUSIC#", "MUSIC" -> tagMusic
            "#EVENT#", "EVENT" -> tagEvent
            "#FINANCE#", "FINANCE" -> tagFinance
            "#TRAVEL#", "TRAVEL" -> tagTravel
            "#TECH#", "TECH" -> tagTech
            "#HEALTH#", "HEALTH" -> tagHealth
            "#EDUCATION#", "EDUCATION" -> tagEducation
            "#ENTERTAINMENT#", "ENTERTAINMENT" -> tagEntertainment
            "#PORTFOLIO#", "PORTFOLIO" -> tagPortfolio
            else -> if (tagKey.isBlank()) tagCommon else if (tagKey.startsWith("#") && tagKey.endsWith("#")) tagKey.removeSurrounding("#") else tagKey
        }
    }

    val allStandardLinkTags: List<Pair<String, String>>
        get() = listOf(
            "#COMMON#" to tagCommon,
            "#PLACES#" to tagPlaces,
            "#WANT_TO_GO#" to tagWantToGo,
            "#SHOPPING#" to tagShopping,
            "#RECIPE#" to tagRecipe,
            "#DOCUMENT#" to tagDocument,
            "#INFO#" to tagInfo,
            "#WORK#" to tagWork,
            "#SOCIAL#" to tagSocial,
            "#VIDEO#" to tagVideo,
            "#ARTICLE#" to tagArticle,
            "#MUSIC#" to tagMusic,
            "#EVENT#" to tagEvent,
            "#FINANCE#" to tagFinance,
            "#TRAVEL#" to tagTravel,
            "#TECH#" to tagTech,
            "#HEALTH#" to tagHealth,
            "#EDUCATION#" to tagEducation,
            "#ENTERTAINMENT#" to tagEntertainment,
            "#PORTFOLIO#" to tagPortfolio
        )
}
