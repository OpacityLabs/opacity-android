const script = document.createElement("script");
script.textContent = `
  (function() {
    const log = (request_type, data) => {
        try {
            window.postMessage({
                type: "intercepted_request",
                payload: {
                    request_type,
                    data
                }
            }, "*");
        } catch (e) {}
    };
    const originalFetch = window.fetch;
    window.fetch = async function(input, init) {
        try {
            const method = (init && init.method) || (typeof input === "string" ? "GET" : input.method || "GET");
            const url = typeof input === "string" ? input : input.url;
            let requestHeaders = init?.headers || {};
            if (requestHeaders instanceof Headers) {
                requestHeaders = Object.fromEntries(requestHeaders.entries());
            }
            log("fetch_request", {
                url,
                method,
                headers: requestHeaders,
                body: init?.body,
            });
            const response = await originalFetch.apply(this, arguments);
            const clonedResponse = response.clone();
            let responseHeaders = clonedResponse.headers || {};
            if (responseHeaders instanceof Headers) {
                responseHeaders = Object.fromEntries(responseHeaders.entries());
            }
            log("fetch_response", {
                url,
                method,
                headers: responseHeaders,
                body: await clonedResponse.text(),
                status: clonedResponse.status,
            });
            return response;
        } catch (err) {
            log("fetch_error", err.toString());
        }
    };
    const OriginalXHR = window.XMLHttpRequest;

    function PatchedXHR() {
        const xhr = new OriginalXHR();
        let _method = "";
        let _url = "";
        let headers = {};
        xhr.open = new Proxy(xhr.open, {
            apply(t, thisArg, args) {
                _method = args[0];
                _url = args[1];
                return Reflect.apply(t, thisArg, args);
            },
        });
        const setRequestHeader = xhr.setRequestHeader;
        xhr.setRequestHeader = function(name, value) {
            headers[name] = value;
            return setRequestHeader.apply(xhr, arguments);
        };
        xhr.send = new Proxy(xhr.send, {
            apply(t, thisArg, args) {
                log("xhr_request", {
                    method: _method,
                    url: _url,
                    headers,
                    body: args[0],
                });
                xhr.addEventListener("loadend", function() {
                    log("xhr_response", {
                        method: _method,
                        url: _url,
                        headers,
                        body: xhr.responseText || xhr.response,
                        status: xhr.status,
                    });
                });
                return Reflect.apply(t, thisArg, args);
            },
        });
        return xhr;
    }
    window.XMLHttpRequest = PatchedXHR;
})();
`;
(document.head || document.documentElement).appendChild(script);

window.addEventListener("message", (event) => {
  if (event.source !== window) return;
  if (event.data?.type === "intercepted_request") {
    try {
      browser.runtime.sendMessage({
        type: "intercepted_request",
        data: event.data.payload,
      });
    } catch (err) {}
  }
});
