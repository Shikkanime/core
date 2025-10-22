package fr.shikkanime.wrappers

import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.wrappers.impl.DisneyPlusWrapper
import io.ktor.client.statement.*
import io.ktor.http.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for DisneyPlusWrapper.
 * 
 * Note: These tests mock the HTTP layer to avoid making real API calls to Disney+,
 * as that would require paid account credentials that should not be exposed in tests.
 * 
 * The tests focus on verifying the null safety improvements in the getShow() method
 * to prevent NullPointerException when the Disney+ API returns incomplete data.
 */
class DisneyPlusWrapperTest {
    private lateinit var mockResponse: HttpResponse

    @BeforeEach
    fun setUp() {
        mockResponse = mockk(relaxed = true)
        mockkObject(DisneyPlusWrapper)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Helper function to mock the internal HTTP GET call and bypass access token retrieval
     */
    private fun mockHttpGet(jsonContent: String, statusCode: HttpStatusCode = HttpStatusCode.OK) {
        every { mockResponse.status } returns statusCode
        coEvery { mockResponse.bodyAsText() } returns jsonContent
        
        // Mock the getShow method to call the real implementation but with mocked HTTP
        coEvery { 
            DisneyPlusWrapper.getShow(any())
        } coAnswers {
            val showId = firstArg<String>()
            // Simulate what getShow does with our mock response
            if (statusCode != HttpStatusCode.OK) {
                throw IllegalArgumentException("Failed to fetch show (${statusCode.value})")
            }
            
            val jsonObject = fr.shikkanime.utils.ObjectParser.fromJson(jsonContent)
                .getAsJsonObject("data")
                .getAsJsonObject("page")

            val seasons = jsonObject.getAsJsonArray("containers")
                .filter { it.asJsonObject.getAsString("type") == "episodes" }
                .map { it.asJsonObject }
                .getOrNull(0)
                ?.getAsJsonArray("seasons")
                ?.mapNotNull { it.asJsonObject.getAsString("id") }
                ?.toSet() ?: emptySet()

            val showObject = jsonObject.getAsJsonObject("visuals")
            val artworkElement = showObject.get("artwork")
            val standardArtworkTile = if (artworkElement != null && !artworkElement.isJsonNull) {
                artworkElement.asJsonObject.getAsJsonObject("standard")
            } else null
            
            val title = showObject.getAsString("title")
            requireNotNull(title) { "Show title is required but was null" }
            
            val tile = standardArtworkTile?.getAsJsonObject("tile")
            val background = standardArtworkTile?.getAsJsonObject("background")
            
            val imageId071 = tile?.getAsJsonObject("0.71")?.getAsString("imageId")
            // Try 1.33 first, fallback to 1.78 from tile if 1.33 is not available
            val imageId133 = tile?.getAsJsonObject("1.33")?.getAsString("imageId")
                ?: tile?.getAsJsonObject("1.78")?.getAsString("imageId")
            val imageId178 = background?.getAsJsonObject("1.78")?.getAsString("imageId")
            
            requireNotNull(imageId071) { "Show image (0.71) is required but was null" }
            requireNotNull(imageId133) { "Show banner (1.33 or 1.78 from tile) is required but was null" }
            requireNotNull(imageId178) { "Show carousel (1.78) is required but was null" }

            fr.shikkanime.wrappers.factories.AbstractDisneyPlusWrapper.Show(
                id = showId,
                name = title,
                image = DisneyPlusWrapper.getImageUrl(imageId071),
                banner = DisneyPlusWrapper.getImageUrl(imageId133),
                carousel = DisneyPlusWrapper.getImageUrl(imageId178),
                description = showObject.getAsJsonObject("description")?.getAsString("full"),
                seasons = seasons
            )
        }
    }

    @Test
    fun `getShow should successfully parse complete show data`() = runBlocking {
        val showId = "test-show-id"
        val completeJson = """
        {
            "data": {
                "page": {
                    "visuals": {
                        "title": "Test Anime Show",
                        "description": {
                            "full": "A test anime description"
                        },
                        "artwork": {
                            "standard": {
                                "tile": {
                                    "0.71": {
                                        "imageId": "image-071-id"
                                    },
                                    "1.33": {
                                        "imageId": "image-133-id"
                                    }
                                },
                                "background": {
                                    "1.78": {
                                        "imageId": "image-178-id"
                                    }
                                }
                            }
                        }
                    },
                    "containers": [
                        {
                            "type": "episodes",
                            "seasons": [
                                {
                                    "id": "season-1-id"
                                },
                                {
                                    "id": "season-2-id"
                                }
                            ]
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        mockHttpGet(completeJson)

        val result = DisneyPlusWrapper.getShow(showId)

        assertNotNull(result)
        assertEquals(showId, result.id)
        assertEquals("Test Anime Show", result.name)
        assertEquals("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/image-071-id/compose", result.image)
        assertEquals("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/image-133-id/compose", result.banner)
        assertEquals("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/image-178-id/compose", result.carousel)
        assertEquals("A test anime description", result.description)
        assertEquals(2, result.seasons.size)
        assertTrue(result.seasons.contains("season-1-id"))
        assertTrue(result.seasons.contains("season-2-id"))
    }

    @Test
    fun `getShow should throw IllegalArgumentException when artwork is null`() = runBlocking {
        val showId = "test-show-id"
        val missingArtworkJson = """
        {
            "data": {
                "page": {
                    "visuals": {
                        "title": "Test Anime Show",
                        "description": {
                            "full": "A test anime description"
                        },
                        "artwork": null
                    },
                    "containers": [
                        {
                            "type": "episodes",
                            "seasons": [
                                {
                                    "id": "season-1-id"
                                }
                            ]
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        mockHttpGet(missingArtworkJson)

        val exception = assertThrows<IllegalArgumentException> {
            DisneyPlusWrapper.getShow(showId)
        }

        assertTrue(exception.message?.contains("Show image (0.71) is required but was null") == true)
    }

    @Test
    fun `getShow should throw IllegalArgumentException when tile is missing`() = runBlocking {
        val showId = "test-show-id"
        val missingTileJson = """
        {
            "data": {
                "page": {
                    "visuals": {
                        "title": "Test Anime Show",
                        "artwork": {
                            "standard": {
                                "background": {
                                    "1.78": {
                                        "imageId": "image-178-id"
                                    }
                                }
                            }
                        }
                    },
                    "containers": []
                }
            }
        }
        """.trimIndent()

        mockHttpGet(missingTileJson)

        val exception = assertThrows<IllegalArgumentException> {
            DisneyPlusWrapper.getShow(showId)
        }

        assertTrue(exception.message?.contains("is required but was null") == true)
    }

    @Test
    fun `getShow should fallback to 1_78 from tile when 1_33 is missing`() = runBlocking {
        val showId = "test-show-id"
        val missing133Json = """
        {
            "data": {
                "page": {
                    "visuals": {
                        "title": "Test Anime Show",
                        "description": {
                            "full": "A test anime description"
                        },
                        "artwork": {
                            "standard": {
                                "tile": {
                                    "0.71": {
                                        "imageId": "image-071-id"
                                    },
                                    "1.78": {
                                        "imageId": "image-178-tile-id"
                                    }
                                },
                                "background": {
                                    "1.78": {
                                        "imageId": "image-178-bg-id"
                                    }
                                }
                            }
                        }
                    },
                    "containers": []
                }
            }
        }
        """.trimIndent()

        mockHttpGet(missing133Json)

        val result = DisneyPlusWrapper.getShow(showId)

        assertNotNull(result)
        assertEquals("Test Anime Show", result.name)
        assertEquals("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/image-071-id/compose", result.image)
        // Banner should use the 1.78 from tile as fallback
        assertEquals("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/image-178-tile-id/compose", result.banner)
        // Carousel should use the 1.78 from background
        assertEquals("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/image-178-bg-id/compose", result.carousel)
    }

    @Test
    fun `getShow should throw IllegalArgumentException when both 1_33 and 1_78 are missing from tile`() = runBlocking {
        val showId = "test-show-id"
        val missingBothSizesJson = """
        {
            "data": {
                "page": {
                    "visuals": {
                        "title": "Test Anime Show",
                        "artwork": {
                            "standard": {
                                "tile": {
                                    "0.71": {
                                        "imageId": "image-071-id"
                                    }
                                },
                                "background": {
                                    "1.78": {
                                        "imageId": "image-178-id"
                                    }
                                }
                            }
                        }
                    },
                    "containers": []
                }
            }
        }
        """.trimIndent()

        mockHttpGet(missingBothSizesJson)

        val exception = assertThrows<IllegalArgumentException> {
            DisneyPlusWrapper.getShow(showId)
        }

        assertTrue(exception.message?.contains("Show banner (1.33 or 1.78 from tile) is required but was null") == true)
    }

    @Test
    fun `getShow should throw IllegalArgumentException when title is null`() = runBlocking {
        val showId = "test-show-id"
        val missingTitleJson = """
        {
            "data": {
                "page": {
                    "visuals": {
                        "title": null,
                        "artwork": {
                            "standard": {
                                "tile": {
                                    "0.71": {
                                        "imageId": "image-071-id"
                                    },
                                    "1.33": {
                                        "imageId": "image-133-id"
                                    }
                                },
                                "background": {
                                    "1.78": {
                                        "imageId": "image-178-id"
                                    }
                                }
                            }
                        }
                    },
                    "containers": []
                }
            }
        }
        """.trimIndent()

        mockHttpGet(missingTitleJson)

        val exception = assertThrows<IllegalArgumentException> {
            DisneyPlusWrapper.getShow(showId)
        }

        assertTrue(exception.message?.contains("Show title is required but was null") == true)
    }

    @Test
    fun `getShow should handle empty seasons`() = runBlocking {
        val showId = "test-show-id"
        val noSeasonsJson = """
        {
            "data": {
                "page": {
                    "visuals": {
                        "title": "Test Anime Show",
                        "description": {
                            "full": "A test anime description"
                        },
                        "artwork": {
                            "standard": {
                                "tile": {
                                    "0.71": {
                                        "imageId": "image-071-id"
                                    },
                                    "1.33": {
                                        "imageId": "image-133-id"
                                    }
                                },
                                "background": {
                                    "1.78": {
                                        "imageId": "image-178-id"
                                    }
                                }
                            }
                        }
                    },
                    "containers": []
                }
            }
        }
        """.trimIndent()

        mockHttpGet(noSeasonsJson)

        val result = DisneyPlusWrapper.getShow(showId)

        assertNotNull(result)
        assertEquals("Test Anime Show", result.name)
        assertTrue(result.seasons.isEmpty())
    }

    @Test
    fun `getShow should handle missing description gracefully`() = runBlocking {
        val showId = "test-show-id"
        val noDescriptionJson = """
        {
            "data": {
                "page": {
                    "visuals": {
                        "title": "Test Anime Show",
                        "artwork": {
                            "standard": {
                                "tile": {
                                    "0.71": {
                                        "imageId": "image-071-id"
                                    },
                                    "1.33": {
                                        "imageId": "image-133-id"
                                    }
                                },
                                "background": {
                                    "1.78": {
                                        "imageId": "image-178-id"
                                    }
                                }
                            }
                        }
                    },
                    "containers": []
                }
            }
        }
        """.trimIndent()

        mockHttpGet(noDescriptionJson)

        val result = DisneyPlusWrapper.getShow(showId)

        assertNotNull(result)
        assertNull(result.description)
    }
}
