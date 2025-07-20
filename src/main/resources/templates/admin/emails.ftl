
<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <form action="${baseUrl}/admin/emails" method="post" id="emailForm">
        <div class="d-flex justify-content-end">
            <button type="button" class="btn btn-primary mt-3" data-bs-toggle="modal" data-bs-target="#confirmSendModal">Send</button>
        </div>

        <div class="card mt-3">
            <div class="card-body">
                <div class="row g-3">
                    <div class="col-md-12">
                        <label for="subject" class="form-label">Subject</label>
                        <input type="text" class="form-control" id="subject" name="subject">
                    </div>
                    <div class="col-md-12">
                        <label for="body" class="form-label">Body</label>
                        <textarea id="body" name="body" class="form-control" rows="5" placeholder="Body"></textarea>
                    </div>
                </div>
            </div>
        </div>
    </form>

    <div class="modal fade" id="confirmSendModal" tabindex="-1" aria-labelledby="confirmSendModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h1 class="modal-title fs-5" id="confirmSendModalLabel">Confirm</h1>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    Are you sure you want to send this email to all members?
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="button" class="btn btn-primary" onclick="document.getElementById('emailForm').submit();">Confirm</button>
                </div>
            </div>
        </div>
    </div>
</@navigation.display>