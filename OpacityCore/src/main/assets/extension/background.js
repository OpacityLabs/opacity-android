async function fetchAndSendCookiesForDomain(domain) {
  const cleanDomain = domain.replace(/^\./, "");

  let cookies = await browser.cookies.getAll({ domain: cleanDomain });
  if (!cookies || cookies.length === 0) {
    cookies = await browser.cookies.getAll({ domain: "." + cleanDomain });
  }

  if (!cookies || cookies.length === 0) {
    return;
  }

  try {
    const cookieStrings = cookies.map((cookie) => {
      let cookieStr = `${cookie.name}=${cookie.value}`;
      if (cookie.domain) {
        cookieStr += `; Domain=${cookie.domain}`;
      }
      if (cookie.path) {
        cookieStr += `; Path=${cookie.path}`;
      }
      if (cookie.secure) {
        cookieStr += `; Secure`;
      }
      if (cookie.httpOnly) {
        cookieStr += `; HttpOnly`;
      }

      return cookieStr;
    });

    browser.runtime.sendNativeMessage("gecko", {
      event: "cookies",
      cookies: cookieStrings.join("\n"),
      domain: domain,
    });
  } catch (err) {}
}

browser.webRequest.onHeadersReceived.addListener(
  function (details) {
    let url = details.url;
    let domain = new URL(url).hostname;

    let cookiesHeaders = details.responseHeaders.filter(
      (header) => header.name.toLowerCase() === "set-cookie",
    );

    if (cookiesHeaders.length === 0) {
      return;
    }

    let cookies = cookiesHeaders.map((h) => h.value).join("\n");

    if (!cookies) {
      return;
    }

    browser.runtime.sendNativeMessage("gecko", {
      event: "cookies",
      cookies: cookies,
      domain: domain,
    });

    setTimeout(() => fetchAndSendCookiesForDomain(domain), 125);
  },
  { urls: ["<all_urls>"] }, // Intercept all URLs
  ["responseHeaders"],
);

// Handle messages from content script
browser.runtime.onMessage.addListener(function (message, sender, _) {
  if (message.type === "cookie_operation") {
    try {
      const domain = message.domain || new URL(sender.tab.url).hostname;

      fetchAndSendCookiesForDomain(domain);
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
    const url = details.url;
    const domain = new URL(url).hostname;

    fetchAndSendCookiesForDomain(domain);

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

browser.webNavigation.onCompleted.addListener(function (details) {
  if (details.frameId === 0) {
    try {
      const domain = new URL(details.url).hostname;
      fetchAndSendCookiesForDomain(domain);
    } catch (err) {}
  }
});

// Listen for cookie changes and send updates to native
browser.cookies.onChanged.addListener(function (changeInfo) {
  const cookie = changeInfo.cookie;
  if (cookie && cookie.domain) {
    const domain = cookie.domain.replace(/^\./, "");
    fetchAndSendCookiesForDomain(domain);
  }
});
