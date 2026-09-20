package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.OneTrustConfig
import com.washingtonpost.android.config.data.datasources.utils.MapConfigParams

@JsonClass(generateAdapter = true)
data class RawOneTrustConfig(
    @Json(name = "storageLocation") val storageLocation: String? = null,
    @Json(name = "domainIdentifier") val domainIdentifier: String? = null,
    @Json(name = "stageDomainIdentifier") val stageDomainIdentifier: String? = null,
) {
    fun mapToDomain(params: MapConfigParams): OneTrustConfig {
        val defaultDomainIdentifier = if (params.configProvider.isDebugBuild) {
            "D6F9F0731C3B9DFA6C9E1F03B027448FC74EAAA0CD952CD4FE089A25C628EF61D704D6572D83B2BF80D48850A2E17E4C"
        } else {
            "D6F9F0731C3B9DFA6C9E1F03B027448FC74EAAA0CD952CD4FE089A25C628EF6152D802D9A71026BD1D06A2F8E78995B1"
        }
        return OneTrustConfig(
            storageLocation = storageLocation ?: "cdn.cookielaw.org",
            domainIdentifier = params.decrypt(domainIdentifier ?: defaultDomainIdentifier),
            stageDomainIdentifier = params.decrypt(
                stageDomainIdentifier
                    ?: "10716746C80DDF5D5179DAF7180FEF60EDE8CA7792893371A131EECF130AF0D9227FB1CABE90997468FE9E7422EAEA25"
            ),
        )
    }
}
