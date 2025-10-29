browser.runtime.onMessage.addListener(function (message, sender, _) {
  if (message.type === "intercepted_request") {
    try {
      browser.runtime.sendNativeMessage("gecko", {
        event: "intercepted_request",
        data: message.data,
      });
    } catch (err) {
      // Silently handle errors to prevent crashes
    }
  }
});
