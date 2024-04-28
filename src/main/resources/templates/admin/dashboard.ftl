<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div x-data="{ display: 'CPU' }">
        <div class="container-fluid d-flex">
            <div class="row g-3 align-items-center mb-3">
                <div class="col-auto">
                    <label class="col-form-label" for="hours">Hours</label>
                </div>
                <div class="col-auto">
                    <select class="form-select" id="hours">
                        <option value="1" selected>1</option>
                        <option value="3">3</option>
                        <option value="6">6</option>
                        <option value="12">12</option>
                        <option value="24">24</option>
                        <option value="48">48</option>
                    </select>
                </div>
            </div>

            <div class="row g-3 align-items-center mb-3">
                <div class="col-auto">
                    <label class="col-form-label ms-2" for="display">Display</label>
                </div>
                <div class="col-auto">
                    <select class="form-select" id="display" x-model="display">
                        <option value="CPU" selected>CPU</option>
                        <option value="Memory">Memory</option>
                    </select>
                </div>
            </div>
        </div>

        <div class="row g-4">
            <div class="col-md-12">
                <div class="card p-3">
                    <div style="height: 50vh">
                        <canvas id="cpuLoadChart" x-show="display === 'CPU'"></canvas>
                        <canvas id="memoryUsageChart" x-show="display === 'Memory'"></canvas>
                    </div>
                </div>
            </div>

            <div class="col-md-6">
                <div class="card p-3">
                    <div class="d-flex mb-4">
                        <h4 class="card-title">Simulcasts</h4>
                        <a id="simulcasts-invalidate" href="/admin/simulcasts-invalidate"
                           class="btn btn-danger ms-auto me-0">Invalidate</a>
                    </div>

                    <ul class="list-group list-group-numbered">
                        <#list simulcasts as simulcast>
                            <li class="list-group-item d-flex justify-content-between align-items-start">
                                <div class="ms-2 me-auto">
                                    <div class="fw-bold">${simulcast.season} ${simulcast.year?c}</div>
                                </div>

                                <span class="badge text-bg-primary rounded-pill"
                                      id="simulcast-${simulcast.uuid}"></span>
                            </li>

                            <script>
                                document.addEventListener('DOMContentLoaded', function () {
                                    axios.get('/api/v1/animes?simulcast=${simulcast.uuid}')
                                        .then(response => {
                                            document.getElementById('simulcast-${simulcast.uuid}').innerText = response.data.total;
                                        });
                                });
                            </script>
                        </#list>
                    </ul>
                </div>
            </div>

            <div class="col-md-6">
                <div class="card p-3">
                    <div class="d-flex mb-4">
                        <h4 class="card-title">Images</h4>
                        <a id="images-invalidate" href="/admin/images-invalidate" class="btn btn-danger ms-auto me-0">Invalidate</a>
                        <a href="/admin/images-save" class="btn btn-success ms-2 me-0">Save</a>
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

    <script src="https://cdn.jsdelivr.net/npm/chart.js" crossorigin="anonymous"></script>
    <script src="/assets/js/home_chart.js" crossorigin="anonymous"></script>
</@navigation.display>