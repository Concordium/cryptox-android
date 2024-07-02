package com.concordium.wallet.data.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class RssChannel
@JsonCreator
constructor(
    @JsonProperty("item")
    @JacksonXmlElementWrapper(useWrapping = false)
    val items: List<Item>
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Item
    @JsonCreator
    constructor(
        @JsonProperty("title")
        val title: String,
        @JsonProperty("link")
        val link: String,
        @JsonProperty("description")
        val description: String,
        @JsonProperty("thumbnail", namespace = "media")
        val thumbnail: Thumbnail?,
        @JsonProperty("pubDate")
        val pubDate: String,
    ) {
        class Thumbnail
        @JsonCreator
        constructor(
            @JacksonXmlProperty(localName = "url")
            val url: String,
        )
    }
}
