package com.concordium.wallet.data

import com.concordium.wallet.data.model.NewsfeedEntry
import java.util.Date

class NewsfeedRepository {
    suspend fun getEntries(): List<NewsfeedEntry> {
        return listOf(
            NewsfeedEntry.Article(
                title = "Concordium Becomes First Overseas Blockchain Platform to Join Japan Blockchain Association",
                description = "Concordium AG is proud to announce that it has been accepted as a full member of the Japan Blockchain Association. It now becomes the first overseas blockchain platform to join the Association, which comprises over 120 domestic and international companies and organizations.",
                thumbnailUrl = "https://cdn.prod.website-files.com/6521bb608cd53c12a5831b95/6521bb608cd53c12a5831f27_64f060f3fc95f9d20817868_hafidh-satyanto-boNRsEMxPsY-unsplash-min-min.jpeg",
                url = "https://concordium-4926ab-5553023-28e7dbd644046.webflow.io/article/concordium-becomes-first-overseas-blockchain-platform-to-join-japan-blockchain-association",
                date = Date(1624443102000L),
            ),
            NewsfeedEntry.Article(
                title = "Concordium and Swvl announce partnership for blockchain-based mass transit systems",
                description = "Concordium and Swvl announce partnership for blockchain-based mass transit systems.",
                thumbnailUrl = "https://cdn.prod.website-files.com/6521bb608cd53c12a5831b95/6521bb608cd53c12a5831f28_64f060f3fc95f9d20817836a_john-verhoestra-Yh2UPFrdYoU-unsplash-min.jpg",
                url = "https://concordium-4926ab-5553023-28e7dbd644046.webflow.io/article/concordium-and-swvl-announce-partnership-for-blockchain-based-mass-transit-systems",
                date = Date(1628676702000L),
            ),
            NewsfeedEntry.Article(
                title = "Concordium will launch a DeFi Lab focused on creating Regulated Decentralized Financial Products",
                description = "Concordium and Verum Capital AG are collaborating to launch the first lab focused on developing regulated decentralized finance products: the Concordium DeFi Lab.The lab will establish a funding vehicle and allocate for \$100 M in \$CCD in the form of grants and investments through its community endowment program. The Concordium DeFi Lab offers everyone a one-of-a-kind opportunity to participate in the future of regulated decentralized finance.",
                thumbnailUrl = "https://cdn.prod.website-files.com/6521bb608cd53c12a5831b95/6521bb608cd53c12a5831f2d_64f060f3fc95f9d20817838b_gift-habeshaw-ImFZSnfobKk-unsplash-min.jpg",
                url = "https://concordium-4926ab-5553023-28e7dbd644046.webflow.io/article/concordium-will-launch-a-defi-lab-focused-on-creating-regulated-decentralized-financial-products",
                date = Date(1638961902000L),
            ),
        )
    }
}
