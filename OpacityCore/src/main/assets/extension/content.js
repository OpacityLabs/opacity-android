const COOKIE_POLL_INTERVAL_MS = 500;

let cookiePollIntervalId = null;
let lastCookieState = document.cookie;

const originalCookie = Object.getOwnPropertyDescriptor(
  Document.prototype,
  "cookie"
);

const originalSetter = originalCookie.set;

const sendCookieData = (cookieData) => {
  if (!browser || !browser.runtime || !browser.runtime.sendMessage) {
    return;
  }

  browser.runtime.sendMessage({
    type: "cookie_operation",
    cookies: cookieData,
    domain: window.location.hostname || "",
  });
};

const cookieSetter = (value) => {
  let result;

  // call the original setter
  try {
    if (typeof originalSetter === "function") {
      result = originalSetter.call(this, value);
    }
  } catch (error) {}

  if (!value) {
    return result;
  }

  const mainCookiePart = value.split(";")[0];
  const cookieParts = mainCookiePart.split("=");

  if (cookieParts.length >= 2) {
    const cookieName = cookieParts[0].trim();
    const cookieValue = cookieParts.slice(1).join("=").trim();

    // send to background script
    if (cookieName) {
      const cookieData = { [cookieName]: cookieValue };
      sendCookieData(cookieData);
    }
  }

  return result;
};

// apply our new descriptor with the new setter
Object.defineProperty(Document.prototype, "cookie", {
  configurable: originalCookie.configurable,
  enumerable: originalCookie.enumerable,
  get: originalCookie.get,
  set: cookieSetter,
});

// poll changes to document.cookies
const pollForCookies = () => {
  cookiePollIntervalId = setTimeout(() => {
    try {
      const currentCookieState = document.cookie;

      if (currentCookieState === lastCookieState) {
        pollForCookies();
        return;
      }

      const parsedCookies = (() => {
        const cookies = {};
        if (!currentCookieState) return cookies;

        currentCookieState.split(";").forEach((cookie) => {
          const parts = cookie.trim().split("=");
          if (parts.length >= 2) {
            const name = parts[0].trim();
            const value = parts.slice(1).join("=").trim();
            if (name) {
              cookies[name] = value;
            }
          }
        });

        return cookies;
      })();

      lastCookieState = currentCookieState;

      sendCookieData(parsedCookies);
      pollForCookies();
    } catch (error) {
      pollForCookies();
    }
  }, COOKIE_POLL_INTERVAL_MS);
};

pollForCookies();
