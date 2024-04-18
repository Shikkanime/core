function copyToClipboard(content) {
    const textarea = document.createElement("textarea");
    textarea.style.height = 0;
    document.body.appendChild(textarea);
    textarea.value = content;
    textarea.select();
    document.execCommand("copy");
    document.body.removeChild(textarea);
}

async function callApi(url, options = {}) {
    const {abortSignal, method = 'GET', body = null} = options;
    const headers = {'Content-Type': 'application/json'};
    const fetchOptions = {headers, signal: abortSignal, method};
    if (method !== 'GET') fetchOptions.body = JSON.stringify(body);

    return await fetch(url, fetchOptions)
        .then(response => response.json());
}