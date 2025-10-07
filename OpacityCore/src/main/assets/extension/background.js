const cookieStore = {};

browser.webRequest.onHeadersReceived.addListener(
  function (details) {
    let url = details.url;
    let domain = new URL(url).hostname;

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

    browser.runtime.sendNativeMessage("gecko", {
      event: "cookies",
      cookies: cookies,
      domain: domain,
    });
  },
  { urls: ["<all_urls>"] }, // Intercept all URLs
  ["responseHeaders"]
);

// Handle messages from content script for document.cookies
browser.runtime.onMessage.addListener(function (message, sender, _) {
  if (message.type === "cookie_operation") {
    try {
      const domain = message.domain || new URL(sender.tab.url).hostname;

      if (!domain) {
        browser.runtime.sendNativeMessage("gecko", {
          event: "cookies",
          cookies: message.cookies,
          domain: message.domain,
        });

        return;
      }

      if (!cookieStore[domain]) {
        cookieStore[domain] = { cookies: {} };
      }

      Object.keys(message.cookies || {}).forEach((name) => {
        const value = message.cookies[name];

        cookieStore[domain].cookies[name] = value;
      });

      browser.runtime.sendNativeMessage("gecko", {
        event: "cookies",
        cookies: cookieStore[domain].cookies,
        domain: domain,
      });
    } catch (err) {
      browser.runtime.sendNativeMessage("gecko", {
        event: "cookies",
        cookies: message.cookies,
        domain: message.domain,
      });
    }
  }
});

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
