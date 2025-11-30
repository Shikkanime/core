<#macro alpinePagination
pageVar="page"
limitVar="limit"
totalVar="pageable.total"
maxPageVar="maxPage"
setPageFunc="setPage">
    <div class="col-auto ms-auto text-muted d-flex align-items-center">
        <div>
            <span x-text="${totalVar} > 0 ? ((${pageVar} - 1) * ${limitVar}) + 1 : 0"></span>-<span x-text="Math.min(${pageVar} * ${limitVar}, ${totalVar})"></span>
            of
            <span x-text="${totalVar}"></span>
        </div>

        <div class="ms-2">
            <button type="button"
                    class="text-muted text-decoration-none btn btn-light"
                    @click="${setPageFunc}(Math.max(1, ${pageVar} - 1))"
                    :class="{disabled: ${pageVar} === 1}">
                <i class="bi bi-chevron-left"></i>
            </button>

            <button type="button"
                    class="text-muted text-decoration-none btn btn-light"
                    @click="${setPageFunc}(Math.min(${maxPageVar}, ${pageVar} + 1))"
                    :class="{disabled: ${pageVar} >= ${maxPageVar}}">
                <i class="bi bi-chevron-right"></i>
            </button>
        </div>
    </div>
</#macro>

<#macro pageSizeSelector
optionsList=[5, 10, 15, 30]
limitVar="limit"
onChange="setPage(1); applyFilters()">
    <div class="col-auto">
        <select class="form-select"
                x-model="${limitVar}"
                @change="${onChange}">
            <#list optionsList as optionValue>
                <option value="${optionValue}">
                    ${optionValue}
                </option>
            </#list>
        </select>
    </div>
</#macro>