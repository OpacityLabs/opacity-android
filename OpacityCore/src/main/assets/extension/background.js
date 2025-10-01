browser.webRequest.onHeadersReceived.addListener(
  function (details) {
    let url = details.url;
    let request_domain = new URL(url).hostname;
    let cookie_domain;

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
      let parts = cookie.split(";").map((p) => p.trim());

      let first = parts[0];
      let eqIndex = first.indexOf("=");
      if (eqIndex !== -1) {
        let name = first.slice(0, eqIndex);
        let value = first.slice(eqIndex + 1);
        cookieDict[name] = value;
      }

      parts.slice(1).forEach((attr) => {
        let [key, ...rest] = attr.split("=");
        let val = rest.join("=") || true;
        if (key.toLowerCase() === "domain") {
          /** RFC 6265
           * If the first character of the attribute-value string is %x2E ("."):
            Let cookie-domain be the attribute-value without the leading %x2E
            (".") character.
           */
          if (val.startsWith(".")) {
            val = val.substring(1);
          }
          cookie_domain = val;
        }
      });
    });

    // Send cookies back to the app (GeckoView) via messaging
    browser.runtime.sendNativeMessage("gecko", {
      event: "cookies",
      cookies: cookieDict,
      domain: cookie_domain || request_domain,
    });
  },
  { urls: ["<all_urls>"] }, // Intercept all URLs
  ["responseHeaders"]
);

browser.webNavigation.onDOMContentLoaded.addListener(function (details) {
  if (details.frameId === 0) {
    // Ensure it's the top-level frame
    // Inject a script to fetch the outerHTML of the current document
    browser.tabs
      .executeScript(details.tabId, {
        code: "document.documentElement.outerHTML",
      })
      .then((result) => {
        if (result && result.length > 0) {
          const htmlBody = result[0]; // The outerHTML of the page

          // Send the HTML content and cookies to the native app
          browser.runtime.sendNativeMessage("gecko", {
            event: "html_body",
            html: htmlBody,
          });
        } else {
          browser.runtime.sendNativeMessage("gecko", {
            event: "No results when getting outerHTML",
          });
        }
      })
      .catch((err) => {
        browser.runtime.sendNativeMessage("gecko", {
          event: "Execute script error: " + err.toString(),
        });
      });
  }
});
