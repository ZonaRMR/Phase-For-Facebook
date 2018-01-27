"use strict";

(function () {
  // focus listener for textareas
  // since swipe to refresh is quite sensitive, we will disable it
  // when we detect a user typing
  // note that this extends passed having a keyboard opened,
  // as a user may still be reviewing his/her post
  // swiping should automatically be reset on refresh
  var _phaseBlur, _phaseFocus;

  _phaseFocus = function _phaseFocus(e) {
    var element;
    element = e.target || e.srcElement;
    console.log("Phase focus", element.tagName);
    if (element.tagName === "TEXTAREA") {
      if (typeof Phase !== "undefined" && Phase !== null) {
        Phase.disableSwipeRefresh(true);
      }
    }
  };

  _phaseBlur = function _phaseBlur(e) {
    var element;
    element = e.target || e.srcElement;
    console.log("Phase blur", element.tagName);
    if (typeof Phase !== "undefined" && Phase !== null) {
      Phase.disableSwipeRefresh(false);
    }
  };

  document.addEventListener("focus", _phaseFocus, true);

  document.addEventListener("blur", _phaseBlur, true);
}).call(undefined);