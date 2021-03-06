"use strict";

(function () {

  /*
   * On top of the click event, we must stop it for long presses
   * Since that will conflict with the context menu
   * Note that we only override it on conditions where the context menu
   * Will occur
   */
  var _phaseAClick, _phasePreventClick, clickTimeout, prevented;

  prevented = false;

  _phaseAClick = function _phaseAClick(e) {
    /*
     * Commonality; check for valid target
     */
    var element, url;
    element = e.target || e.srcElement;
    if (element.tagName !== "A") {
      element = element.parentNode;
    }
    // Notifications is two layers under
    if (element.tagName !== "A") {
      element = element.parentNode;
    }
    if (element.tagName === "A") {
      if (!prevented) {
        url = element.getAttribute("href");
        console.log("Click Intercept " + url);
        // if Phase is injected, check if loading the url through an overlay works
        if ((typeof Phase !== "undefined" && Phase !== null ? Phase.loadUrl(url) : void 0) === true) {
          e.stopPropagation();
          e.preventDefault();
        }
      } else {
        console.log("Click Intercept Prevented");
      }
    }
  };

  _phasePreventClick = function _phasePreventClick() {
    console.log("Click prevented");
    prevented = true;
  };

  document.addEventListener("click", _phaseAClick, true);

  clickTimeout = void 0;

  document.addEventListener("touchstart", function (e) {
    clickTimeout = setTimeout(_phasePreventClick, 400);
  }, true);

  document.addEventListener("touchend", function (e) {
    prevented = false;
    clearTimeout(clickTimeout);
  }, true);
}).call(undefined);