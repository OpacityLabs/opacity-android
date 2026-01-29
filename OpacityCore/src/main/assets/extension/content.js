const originalCookie = Object.getOwnPropertyDescriptor(
  Document.prototype,
  "cookie",
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
      result = originalSetter.call(document, value);
    }
  } catch (error) {}

  if (!value) {
    return result;
  }

  sendCookieData(value);

  return result;
};

// apply descriptor with the new setter
Object.defineProperty(Document.prototype, "cookie", {
  configurable: originalCookie.configurable,
  enumerable: originalCookie.enumerable,
  get: originalCookie.get,
  set: cookieSetter,
});

try {
  const initialCookies = document.cookie;

  if (initialCookies) {
    sendCookieData(initialCookies);
  }
} catch (error) {}
