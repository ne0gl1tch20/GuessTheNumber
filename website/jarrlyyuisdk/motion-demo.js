/* Jarrlyy UI SDK · local motion demo wiring; no inline script needed under CSP. */
(function () {
  "use strict";
  function ready() {
    var motion = window.JarrlyyUIMotion;
    if (!window.JarrlyyUI || !motion) return;
    var score = document.getElementById("motionScore");
    var meter = document.getElementById("motionProgress");
    var label = document.getElementById("motionProgressLabel");
    var status = document.getElementById("motionStatus");
    var play = document.getElementById("motionReplay");
    var reward = document.getElementById("motionReward");
    function replay() {
      motion.count(score, 12450, { from: 0, duration: 720 });
      motion.progress(meter, 76);
      motion.count(label, 76, { from: 0, duration: 600, format: function (value) { return Math.round(value) + "%"; } });
      motion.init(document);
      status.textContent = "Animation played. Reduced-motion preferences are respected.";
    }
    if (play) play.addEventListener("click", replay);
    if (reward) reward.addEventListener("click", function () {
      motion.floatText(reward, "+XP");
      status.textContent = "Floating reward feedback triggered.";
    });
    replay();
  }
  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", ready, { once: true });
  else ready();
})();
