package fr.shikkanime.services

import org.junit.jupiter.api.Test

class MediaImageTest {
    @Test
    fun getLongTimeoutImage() {
        MediaImage.getLongTimeoutImage("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/fcaab22f-cc08-463e-8cc8-bf4367fb1027/compose")
        MediaImage.getLongTimeoutImage("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/1d043ddd-5295-40bb-8e65-0aa3ec34b301/compose")
        MediaImage.getLongTimeoutImage("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/bf85d6d3-d182-4570-adff-1261b843c864/compose")

        MediaImage.getLongTimeoutImage("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/6b27e3d6-74e5-4ea1-a0f1-79ca316dd9b0/compose")
        MediaImage.getLongTimeoutImage("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/3cfa2215-a96d-4064-a2bd-1ebd5dbe1eec/compose")
        MediaImage.getLongTimeoutImage("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/707bc5c2-f9ef-4eeb-8c3e-3967e54a62bf/compose")
        MediaImage.getLongTimeoutImage("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/6b27e3d6-74e5-4ea1-a0f1-79ca316dd9b0/compose")
    }

    @Test
    fun getLongTimeoutImageMultiThreads() {
        val images = listOf(
            "https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/6b27e3d6-74e5-4ea1-a0f1-79ca316dd9b0/compose",
            "https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/3cfa2215-a96d-4064-a2bd-1ebd5dbe1eec/compose",
            "https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/707bc5c2-f9ef-4eeb-8c3e-3967e54a62bf/compose",
            "https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/6b27e3d6-74e5-4ea1-a0f1-79ca316dd9b0/compose"
        )

        images.parallelStream().forEach {
            MediaImage.getLongTimeoutImage(it)
        }
    }
}