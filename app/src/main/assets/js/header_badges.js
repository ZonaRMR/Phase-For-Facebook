"use strict";

(function () {
  // bases the header contents if it exists
  var header;

  header = document.getElementById("mJewelNav");

  if (header !== null) {
    if (typeof Phase !== "undefined" && Phase !== null) {
      Phase.handleHeader(header.outerHTML);
    }
  }
}).call(undefined);