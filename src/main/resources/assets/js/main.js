function copyToClipboard(content) {
    navigator.permissions.query({ name: "clipboard-write" }).then((result) => {
        if (result.state === "granted" || result.state === "prompt") {
            navigator.clipboard.writeText(content).then(function() {
                console.log("Copied to clipboard");
            }, function() {
                console.error("Failed to copy to clipboard");
            });
        }
    });
}