window.close = function () {
  window.postMessage({ type: "WINDOW_CLOSE_INTERCEPTED" }, "*");
};