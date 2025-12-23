package com.adrianosilva.githubexplorer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubLicenseDto(
    @SerialName("key") val key: String,
    @SerialName("name") val name: String,
    @SerialName("spdx_id") val spdxId: String,
    @SerialName("url") val url: String?,
    @SerialName("node_id") val nodeId: String
)