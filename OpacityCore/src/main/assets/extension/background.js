// Listen for web requests and extract cookies
browser.webRequest.onHeadersReceived.addListener(
  function (details) {
    let cookies = details.responseHeaders.filter(
      (header) => header.name.toLowerCase() === "set-cookie"
    );
    // console.warn("Received cookies:", cookies);

    // Send cookies back to the app (GeckoView) via messaging
    browser.runtime.sendNativeMessage("gecko", { event: "cookies", cookies: cookies });
  },
  { urls: ["<all_urls>"] }, // Intercept all URLs
  ["responseHeaders"]
);

//browser.runtime.onMessage.addListener((message, sender, sendResponse) => {
//  console.log("Message received in background script:", message);
//  // Handle the message
//  sendResponse({response: "Message received"});
//  return true; // Keep the message channel open for asynchronous response
//});

// // Optionally, you can intercept outgoing requests and modify them
// browser.webRequest.onBeforeSendHeaders.addListener(
//   function (details) {
//     // You can modify request headers if needed
//     console.log("Request headers:", details.requestHeaders);
//     return { requestHeaders: details.requestHeaders };
//   },
//   { urls: ["<all_urls>"] }, // Intercept all URLs
//   ["blocking", "requestHeaders"]
// );
