package com.concordium.wallet.data.backend.news

import com.concordium.wallet.data.model.RssChannel
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "rss")
@JsonIgnoreProperties(ignoreUnknown = true)
class RssFeedResponse
@JsonCreator
constructor(
    @JsonProperty("channel")
    val channel: RssChannel,
)
