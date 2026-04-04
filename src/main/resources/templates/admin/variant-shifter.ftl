<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <style>
        .search-container {
            position: relative;
        }

        .search-input {
            padding-left: 40px;
        }

        .search-icon {
            position: absolute;
            top: 50%;
            left: 15px;
            transform: translateY(-50%);
            color: #888;
        }
    </style>

    <div class="card mt-3 p-3" x-data="{
        searchAnimeName: '',
        animeSuggestions: [],
        showAnimeSuggestions: false,
        selectedAnime: null,
        platforms: [],
        async init() {
            try {
                const response = await fetch('/api/v1/platforms');
                this.platforms = await response.json();
            } catch (e) {
                console.error('Error fetching platforms:', e);
            }
        },
        async searchAnimes() {
            if (!this.searchAnimeName.trim()) {
                this.animeSuggestions = [];
                this.showAnimeSuggestions = false;
                this.selectedAnime = null;
                return;
            }

            try {
                const response = await fetch('/api/v1/animes?name=' + encodeURIComponent(this.searchAnimeName) + '&limit=5');
                const data = await response.json();
                this.animeSuggestions = data.data || [];
                this.showAnimeSuggestions = true;
            } catch (error) {
                console.error('Error fetching anime suggestions:', error);
                this.animeSuggestions = [];
                this.showAnimeSuggestions = false;
            }
        },
        selectAnime(anime) {
            this.selectedAnime = anime;
            this.searchAnimeName = anime.shortName;
            this.animeSuggestions = [];
            this.showAnimeSuggestions = false;
        }
    }">
        <h4 class="mb-4">Variant Shifter</h4>
        <p class="text-muted">
            Use this tool to bulk shift variant numbers for a specific platform (for example, if Netflix episodes have
            an offset compared to Crunchyroll).
            Corresponding variants will be moved to the target mapping, and if they are the only ones present, the
            source mapping will be deleted.
        </p>

        <form action="/admin/variant-shifter" method="post"
              @submit="if(!selectedAnime) { event.preventDefault(); alert('Please select an anime.'); }">
            <input type="hidden" name="animeUuid" :value="selectedAnime ? selectedAnime.uuid : ''">

            <div class="row mb-3">
                <div class="col-md-6">
                    <label class="form-label">Anime</label>
                    <div class="search-container position-relative">
                        <input type="text" class="form-control search-input"
                               x-model="searchAnimeName"
                               @input.debounce.300ms="searchAnimes()"
                               @focus="showAnimeSuggestions = true"
                               @keydown.escape="showAnimeSuggestions = false"
                               @blur="setTimeout(() => showAnimeSuggestions = false, 200)"
                               placeholder="Search Anime" required>
                        <i class="bi bi-search search-icon"></i>
                        <div x-show="showAnimeSuggestions && animeSuggestions.length > 0"
                             class="dropdown-menu show position-absolute w-100 mt-1" style="z-index: 1000;"
                             x-transition>
                            <template x-for="animeItem in animeSuggestions" :key="animeItem.uuid">
                                <a href="#" class="dropdown-item"
                                   @click.prevent="selectAnime(animeItem)"
                                   x-text="animeItem.shortName"
                                   style="display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                                </a>
                            </template>
                        </div>
                    </div>
                </div>
                <div class="col-md-6">
                    <label for="platform" class="form-label">Platform</label>
                    <select class="form-select" id="platform" name="platform" required>
                        <option value="" disabled selected>Select a platform</option>
                        <template x-for="platform in platforms" :key="platform.id">
                            <option :value="platform.id" x-text="platform.name"></option>
                        </template>
                    </select>
                </div>
            </div>

            <div class="row mb-3">
                <div class="col-md-4">
                    <label for="episodeType" class="form-label">Episode Type</label>
                    <select class="form-select" id="episodeType" name="episodeType" required>
                        <#list episodeTypes as type>
                            <option value="${type.name()}"
                                    <#if type.name() == "EPISODE">selected</#if>>${type.name()}</option>
                        </#list>
                    </select>
                </div>
                <div class="col-md-4">
                    <label for="season" class="form-label">Season</label>
                    <input type="number" class="form-control" id="season" name="season" min="1" value="1" required>
                </div>
                <div class="col-md-4">
                    <label for="shift" class="form-label">Shift (e.g., -1)</label>
                    <input type="number" class="form-control" id="shift" name="shift" placeholder="-1" required>
                </div>
            </div>

            <div class="row mb-4">
                <div class="col-md-6">
                    <label for="startNumber" class="form-label">Start Number (Source)</label>
                    <input type="number" class="form-control" id="startNumber" name="startNumber" min="1" required>
                </div>
                <div class="col-md-6">
                    <label for="endNumber" class="form-label">End Number (Source)</label>
                    <input type="number" class="form-control" id="endNumber" name="endNumber" min="1" required>
                </div>
            </div>

            <button type="submit" class="btn btn-warning w-100 fw-bold"
                    onclick="return confirm('Are you sure you want to shift these variants? This action will modify the database.');">
                Shift variants
            </button>
        </form>
    </div>
</@navigation.display>
