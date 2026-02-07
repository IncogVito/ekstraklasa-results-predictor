chrome.action.onClicked.addListener((tab) => {
    chrome.scripting.executeScript({
        target: { tabId: tab.id },
        func: () => {
            alert("Hello from Chrome extension!");
            console.log("Działa 🎉");
        }
    });
});