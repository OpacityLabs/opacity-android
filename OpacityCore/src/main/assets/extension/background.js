browser.webRequest.onHeadersReceived.addListener(
  function (details) {
    let cookiesHeaders = details.responseHeaders.filter(
      (header) => header.name.toLowerCase() === "set-cookie"
    );

    if (cookiesHeaders.length === 0) {
      return;
    }

    let cookies = cookiesHeaders[0].value;

    if (!cookies) {
      return;
    }

    // cookies is a single string, e.g. "sampleCookie=sampleValue; Path=\/\nsampleCookie2=sampleValue2; Path=\/"
    // We need to split and parse it to a single dictionary

    // Parse cookies
    let cookieDict = {};
    cookies.split("\n").forEach((cookie) => {
      let cookieParts = cookie.split(";")[0].split("=");
      let cookieName = cookieParts[0].trim();
      let cookieValue = cookieParts[1].trim();
      cookieDict[cookieName] = cookieValue;
    });

    // Send cookies back to the app (GeckoView) via messaging
    browser.runtime.sendNativeMessage("gecko", {
      event: "cookies",
      cookies: cookieDict,
    });
  },
  { urls: ["<all_urls>"] }, // Intercept all URLs
  ["responseHeaders"]
);

browser.webNavigation.onDOMContentLoaded.addListener(function (details) {
  if (details.frameId === 0) { // Ensure it's the top-level frame
    // Inject a script to fetch the outerHTML of the current document
    browser.tabs.executeScript(details.tabId, {
      code: "document.documentElement.outerHTML"
    }).then(result => {
      if (result && result.length > 0) {
        const htmlBody = result[0]; // The outerHTML of the page

        // Send the HTML content and cookies to the native app
        browser.runtime.sendNativeMessage("gecko", {
          event: "html_body",
          html: htmlBody
        });

      } else {
        browser.runtime.sendNativeMessage("gecko", {
          event: "No results when getting outerHTML",
        });
      }
    }).catch(err => {
      browser.runtime.sendNativeMessage("gecko", {
        event: "Execute script error: " + err.toString(),
      });
    })
  }
});