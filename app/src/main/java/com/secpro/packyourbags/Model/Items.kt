package com.secpro.packyourbags.Model

import java.io.Serializable

data class Items(
    var ID: Int = 0,
    var itemname: String = "",
    var category: String = "",
    var addedby: String = "system",
    var checked: Boolean = false // ✅ Rename this for Firestore compatibility
) : Serializable
