(function () {
  "use strict";
  const range = document.getElementById("volumeRange");
  const value = document.getElementById("volumeValue");
  if (range && value) {
    range.addEventListener("input", function () {
      value.textContent = this.value + "%";
    });
  }

  document.querySelectorAll("[data-jui-replay]").forEach(function (button) {
    button.addEventListener("click", function () {
      const preview = document.getElementById("motionPreview");
      if (!preview) return;
      const animationClasses = ["jui-animate-fade", "jui-animate-up", "jui-animate-down", "jui-animate-left", "jui-animate-right", "jui-animate-zoom", "jui-animate-bounce", "jui-animate-wiggle", "jui-animate-pulse", "jui-animate-shake", "jui-animate-flip", "jui-animate-heartbeat"];
      preview.classList.remove.apply(preview.classList, animationClasses);
      void preview.offsetWidth;
      preview.classList.add(button.dataset.juiReplay);
    });
  });
  document.querySelectorAll("[data-jui-font-preview]").forEach(function (button) {
    button.addEventListener("click", function () {
      const preview = document.getElementById("fontPreview");
      if (!preview) return;
      preview.classList.remove("jui-font-system", "jui-font-rounded", "jui-font-serif", "jui-font-mono");
      preview.classList.add("jui-font-" + button.dataset.juiFontPreview);
    });
  });

})();