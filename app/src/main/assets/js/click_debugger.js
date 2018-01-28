'use strict';

(function () {
  // for desktop only
  var _phaseAContext;

  _phaseAContext = function _phaseAContext(e) {
    /*
     * Commonality; check for valid target
     */
    var element;
    element = e.target || e.currentTarget || e.srcElement;
    if (!element) {
      return;
    }
    console.log('Clicked element: ' + element.tagName + ' ' + element.className);
  };

  document.addEventListener('contextmenu', _phaseAContext, true);
}).call(undefined);