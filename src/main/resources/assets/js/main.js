function copyToClipboard(content) {
    const textarea = document.createElement("textarea");
    textarea.style.height = 0;
    document.body.appendChild(textarea);
    textarea.value = content;
    textarea.select();
    document.execCommand("copy");
    document.body.removeChild(textarea);
}

async function callApi(url) {
    return await fetch(url, {
        headers: {
            'Content-Type': 'application/json'
        }
    }).then(response => response.json())
        .catch(error => console.error(error));
}