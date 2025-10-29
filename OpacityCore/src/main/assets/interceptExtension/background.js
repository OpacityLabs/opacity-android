browser.runtime.onMessage.addListener(function (message, sender, _) {
  if (message.type === "intercepted_request") {
    try {
      // Ensure data is properly stringified if it's an object
      const dataStr =
        typeof message.data === "string"
          ? message.data
          : JSON.stringify(message.data);

      browser.runtime.sendNativeMessage("gecko", {
        event: "intercepted_request",
        data: dataStr,
      });
    } catch (err) {
      // Silently handle errors to prevent crashes
    }
  }
});
