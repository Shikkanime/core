<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div x-data="{ display: 'CPU' }">
        <div class="d-flex align-items-center mb-3 gap-3">
            <div class="row g-3 align-items-center">
                <div class="col-auto">
                    <select class="form-select" id="datemenu">
                        <option value="1" selected>Last hour</option>
                        <option value="24">Last 24 hours</option>
                        <option value="168">Last 7 days</option>
                    </select>
                </div>
            </div>
            <button class="btn" @click="display = 'CPU'" :class="{ 'btn-dark': display === 'CPU', 'btn-outline-dark': display !== 'CPU' }">CPU</button>
            <button class="btn" @click="display = 'Memory'" :class="{ 'btn-dark': display === 'Memory', 'btn-outline-dark': display !== 'Memory' }">Memory</button>
        </div>

        <div class="row g-4">
            <div class="col-md-6">
                <div class="card p-3">
                    <div style="height: 50vh">
                        <canvas id="cpuLoadChart" x-show="display === 'CPU'"></canvas>
                        <canvas id="memoryUsageChart" x-show="display === 'Memory'"></canvas>
                    </div>
                </div>
            </div>

            <div class="col-md-6">
                <div class="card p-3">
                    <div style="height: 50vh">
                        <canvas id="loginCountChart"></canvas>
                    </div>
                </div>
            </div>

            <div class="col-md-6">
                <div class="card p-3">
                    <div class="d-flex flex-column flex-md-row mb-3 mb-md-4 justify-content-center">
                        <h4 class="card-title">Simulcasts</h4>

                        <div class="ms-md-auto me-md-0">
                            <a id="simulcasts-invalidate" href="/admin/simulcasts-invalidate"
                               class="btn btn-danger">Invalidate</a>
                            <button id="simulcasts-show" class="btn btn-success">Show all</button>
                        </div>

                        <script>
                            document.getElementById('simulcasts-show').addEventListener('click', function () {
                                document.querySelectorAll('.list-group-item.d-none').forEach(function (element) {
                                    element.classList.remove('d-none');
                                });
                            });
                        </script>
                    </div>

                    <ul class="list-group list-group-numbered">
                        <#list simulcasts as simulcast>
                            <li class="list-group-item d-flex justify-content-between align-items-start <#if simulcast_index<5><#else>d-none</#if>">
                                <div class="ms-2 me-auto">
                                    <div class="fw-bold">${simulcast.season} ${simulcast.year?c}</div>
                                </div>

                                <span class="badge text-bg-primary rounded-pill">
                                    ${simulcast.animesCount}
                                </span>
                            </li>
                        </#list>
                    </ul>
                </div>
            </div>

            <div class="col-md-6">
                <div class="card p-3">
                    <div class="d-flex flex-column flex-md-row mb-3 mb-md-4 justify-content-center">
                        <h4 class="card-title">Images</h4>

                        <div class="ms-md-auto me-md-0">
                            <a id="images-invalidate" href="/admin/images-invalidate" class="btn btn-danger">Invalidate</a>
                            <a href="/admin/images-save" class="btn btn-success">Save</a>
                        </div>
                    </div>

                    <div class="row g-3">
                        <div class="col-md-4">
                            <label class="form-label" for="size">Size</label>
                            <input id="size" name="size" type="number" class="form-control disabled" value="${size?c}"
                                   disabled>
                        </div>

                        <div class="col-md-4">
                            <label class="form-label" for="originalSize">Original size</label>
                            <input id="originalSize" name="originalSize" type="text" class="form-control disabled"
                                   value="${originalSize}" disabled>
                        </div>

                        <div class="col-md-4">
                            <label class="form-label" for="compressedSize">Compressed size</label>
                            <input id="compressedSize" name="compressedSize" type="text" class="form-control disabled"
                                   value="${compressedSize}" disabled>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.7/dist/chart.umd.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/moment@^2"></script>
    <script src="https://cdn.jsdelivr.net/npm/chartjs-adapter-moment@^1"></script>

    <script src="/assets/js/dashboard_chart.js" crossorigin="anonymous"></script>
</@navigation.display>