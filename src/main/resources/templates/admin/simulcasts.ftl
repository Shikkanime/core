<#import "_navigation.ftl" as navigation />

<@navigation.display>
    <div class="d-flex mb-3">
        <a href="/admin/simulcasts/recalculate" class="btn btn-success ms-auto me-0">Recalculate</a>
    </div>

    <ul>
        <#list simulcasts as simulcast>
            <li>${simulcast.season} ${simulcast.year?c}</li>
        </#list>
    </ul>
</@navigation.display>