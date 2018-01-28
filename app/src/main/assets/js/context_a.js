"use strict";

(function () {
  // context menu for links
  // largely mimics click_a.js
  // we will also bind a listener here to notify the activity not to deal with viewpager scrolls
  var _phaseAContext, longClick;

  longClick = false;

  _phaseAContext = function _phaseAContext(e) {
    /*
     * Commonality; check for valid target
     */
    var element, image, imageUrl, img, imgUrl, text, url;
    if (typeof Phase !== "undefined" && Phase !== null) {
      Phase.longClick(true);
    }
    longClick = true;
    element = e.target || e.currentTarget || e.srcElement;
    if (!element) {
      return;
    }
    if (element.tagName !== "A") {
      element = element.parentNode;
    }
    //Notifications is two layers under
    if (element.tagName !== "A") {
      element = element.parentNode;
    }
    if (element.tagName === "A" && element.getAttribute("href") !== "#") {
      url = element.getAttribute("href");
      if (!url) {
        return;
      }
      text = element.parentNode.innerText;
      // check if image item exists, first in children and then in parent
      image = element.querySelector("[style*=\"background-image: url(\"]");
      if (!image) {
        image = element.parentNode.querySelector("[style*=\"background-image: url(\"]");
      }
      if (image) {
        imageUrl = window.getComputedStyle(image, null).backgroundImage.trim().slice(4, -1);
        console.log("Context image: " + imageUrl);
        if (typeof Phase !== "undefined" && Phase !== null) {
          Phase.loadImage(imageUrl, text);
        }
        e.stopPropagation();
        e.preventDefault();
        return;
      }
      // check if true img exists
      img = element.querySelector("img[src*=scontent]");
      if (img) {
        imgUrl = img.src;
        console.log("Context img " + imgUrl);
        if (typeof Phase !== "undefined" && Phase !== null) {
          Phase.loadImage(imgUrl, text);
        }
        e.stopPropagation();
        e.preventDefault();
        return;
      }
      console.log("Context Content " + url + " " + text);
      if (typeof Phase !== "undefined" && Phase !== null) {
        Phase.contextMenu(url, text);
      }
      e.stopPropagation();
      e.preventDefault();
    }
  };

  document.addEventListener("contextmenu", _phaseAContext, true);

  document.addEventListener("touchend", function (e) {
    if (longClick) {
      if (typeof Phase !== "undefined" && Phase !== null) {
        Phase.longClick(false);
      }
      longClick = false;
    }
  }, true);
}).call(undefined);