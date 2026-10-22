(function () {
  "use strict";
  const range = document.getElementById("volumeRange");
  const value = document.getElementById("volumeValue");
  if (range && value) {
    range.addEventListener("input", function () {
      value.textContent = this.value + "%";
    });
  }
})();