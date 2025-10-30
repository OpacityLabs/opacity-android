browser.runtime.onMessage.addListener(function (message, sender, _) {
  if (message.type === "intercepted_request") {
    try {
      browser.runtime.sendNativeMessage("gecko", {
        event: "intercepted_request",
        request_type: message.data.request_type,
        data: message.data.data,
      });
    } catch (err) {
      // Silently handle errors to prevent crashes
    }
  }
});
