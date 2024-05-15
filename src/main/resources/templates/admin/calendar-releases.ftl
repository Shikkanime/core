<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div class="toast-container position-fixed top-0 end-0 p-3">
        <div id="successToast" class="toast align-items-center text-bg-success border-0" role="alert"
             aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    Calendar updated successfully!
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"
                        aria-label="Close"></button>
            </div>
        </div>

        <div id="errorToast" class="toast align-items-center text-bg-danger border-0" role="alert" aria-live="assertive"
             aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    Error updating calendar!
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"
                        aria-label="Close"></button>
            </div>
        </div>
    </div>

    <div x-data="{
        days: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'],
        weekCalendar: [
            [
                {
                    releaseTime: '12:00',
                    platform: 'CRUN',
                    anime: 'Date A Live',
                    season: 4,
                    episodeType: 'EPISODE',
                    number: 6,
                    langType: 'SUBTITLES'
                }
            ]
        ],
        selectedDay: ''
    }">
        <div class="modal fade" id="addCalendarRelease" tabindex="-1" aria-labelledby="addCalendarReleaseLabel" aria-hidden="true" x-data="{
            release: {
                releaseTime: '',
                platform: '',
                anime: '',
                season: '',
                episodeType: '',
                number: '',
                langType: ''
            },
            copy: JSON.parse(JSON.stringify(release))
        }">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h1 class="modal-title fs-5" id="addCalendarReleaseLabel">Add a new release</h1>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row g-3">
                            <div class="col-md-6">
                                <label for="day" class="form-label">Day</label>
                                <input type="text" class="form-control" id="day" name="day" x-model="selectedDay" disabled>
                            </div>
                            <div class="col-md-6">
                                <label for="releaseDateTime" class="form-label">Release time</label>
                                <input type="time" class="form-control" id="releaseDateTime" name="releaseDateTime" x-model="release.releaseTime">
                            </div>
                            <div class="col-md-6">
                                <label for="platform" class="form-label">Platform</label>
                                <input type="text" class="form-control" id="platform" name="platform" x-model="release.platform">
                            </div>
                            <div class="col-md-6">
                                <label for="anime" class="form-label">Anime</label>
                                <input type="text" class="form-control" id="anime" name="anime" x-model="release.anime">
                            </div>
                            <div class="col-md-6">
                                <label for="season" class="form-label">Season</label>
                                <input type="number" class="form-control" id="season" name="season" x-model="release.season">
                            </div>
                            <div class="col-md-6">
                                <label for="episodeType" class="form-label">Episode type</label>
                                <input type="text" class="form-control" id="episodeType" name="episodeType" x-model="release.episodeType">
                            </div>
                            <div class="col-md-6">
                                <label for="number" class="form-label">Number</label>
                                <input type="number" class="form-control" id="number" name="number" x-model="release.number">
                            </div>
                            <div class="col-md-6">
                                <label for="langType" class="form-label">Lang type</label>
                                <input type="text" class="form-control" id="langType" name="langType" x-model="release.langType">
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>

                        <button class="btn btn-danger" @click="addInDay(release, weekCalendar, days, selectedDay)">
                            Add
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <div class="table-responsive">
            <table class="table">
                <thead>
                <tr>
                    <template x-for="day in days">
                        <th scope="col" class="text-center" style="width: 14.28%">
                            <div class="d-flex justify-content-center align-items-center">
                                <span class="me-2" x-text="day"></span>

                                <button class="btn btn-sm btn-outline-primary"
                                        @click="selectedDay = day"
                                        data-bs-toggle="modal" data-bs-target="#addCalendarRelease">
                                    <i class="bi bi-plus"></i>
                                </button>
                            </div>
                        </th>
                    </template>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <template x-for="calendar in weekCalendar">
                        <td class="border-start border-end border-light">
                            <#-- Order by release time -->
                            <template
                                    x-for="animeRelease in calendar.sort((a, b) => a.releaseTime.localeCompare(b.releaseTime))">
                                <template x-if="animeRelease">
                                    <div class="card mb-3 position-relative">
                                        <div class="mx-2">
                                            <div class="d-flex align-items-center py-1">
                                                <span class="text-muted" x-text="animeRelease.releaseTime"></span>
                                                <div class="vr mx-2"></div>
                                                <div class="d-block mt-2">
                                                    <span class="h6 text-truncate-2 mb-0 fw-bold"
                                                          x-text="animeRelease.anime"></span>
                                                    <p class="text-muted mt-0 mb-0"
                                                       x-text="'Season ' + animeRelease.season + ' ' + animeRelease.episodeType + ' ' + animeRelease.number"></p>
                                                    <p class="text-muted mt-0 mb-1" x-text="animeRelease.langType"></p>
                                                </div>
                                            </div>
                                        </div>

                                        <div class="position-absolute top-0 start-100 translate-middle">
                                            <button class="btn btn-sm btn-danger rounded-circle"
                                                    @click="weekCalendar[weekCalendar.indexOf(calendar)].splice(calendar.indexOf(animeRelease), 1)">
                                                <i class="bi bi-trash"></i>
                                            </button>
                                        </div>
                                    </div>
                                </template>
                            </template>
                        </td>
                    </template>
                </tr>
                </tbody>
            </table>
        </div>
    </div>

    <script>
        function addInDay(release, weekCalendar, days, day) {
            const indexOf = days.indexOf(day);

            if (indexOf === -1) {
                return;
            }

            if (weekCalendar[indexOf] === undefined) {
                weekCalendar[indexOf] = [];
            }

            const weekCalendarElement = weekCalendar[indexOf];

            // weekCalendarElement.push({
            //     // Random release time
            //     releaseTime: Math.floor(Math.random() * 24) + ':' + Math.floor(Math.random() * 60) + ':' + Math.floor(Math.random() * 60),
            //     platform: 'CRUN',
            //     anime: 'Date A Live',
            //     season: 4,
            //     episodeType: 'EPISODE',
            //     // Random number
            //     number: Math.floor(Math.random() * 100),
            //     langType: 'SUBTITLES'
            // })

            weekCalendarElement.push({...release});
        }
    </script>
</@navigation.display>