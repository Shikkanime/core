<#import "_layout.ftl" as layout />

<@layout.main>
    <div class="w-100 d-flex align-items-center justify-content-center">
        <div class="d-block card p-5">
            <div class="d-flex align-items-center justify-content-center text-center mb-5">
                <img src="/admin/assets/img/icon.jpg" width="32" height="32" alt="Logo" crossorigin="anonymous"
                     class="me-2 rounded-circle">
                <span class="fs-4">Shikkanime</span>
            </div>

            <form action="/admin/login" method="post">
                <div class="mb-3">
                    <label for="username" class="form-label">Username</label>
                    <input type="text" class="form-control" id="username" name="username" required>
                </div>

                <div class="mb-3">
                    <label for="password" class="form-label">Password</label>
                    <input type="password" class="form-control" id="password" name="password" required>
                </div>

                <#if error??>
                    <div class="alert alert-danger mb-3" role="alert">
                        ${error}
                    </div>
                </#if>

                <button type="submit" class="btn btn-primary w-100">Log in</button>
            </form>
        </div>
    </div>
</@layout.main>