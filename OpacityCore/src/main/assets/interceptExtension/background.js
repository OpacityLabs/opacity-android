browser.runtime.onMessage.addListener(function (message, sender, _) {
  if (message.type === "intercepted_request") {
    browser.runtime.sendNativeMessage("gecko", {
      event: "intercepted_request",
      data: message.data,
    });
  }
});
