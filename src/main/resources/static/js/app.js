/**
 * Progressive enhancements only (approved decision 6) — the app works
 * without this file; all state remains server-rendered.
 */
(function () {
  "use strict";

  // Invite link: copy to clipboard + Web Share (mockup §4).
  document.querySelectorAll(".js-copy-link").forEach(function (button) {
    button.addEventListener("click", function () {
      var link = document.querySelector("." + button.dataset.copyTarget);
      if (!link || !navigator.clipboard) {
        return;
      }
      navigator.clipboard.writeText(link.textContent.trim()).then(function () {
        var note = document.querySelector(".copy-note");
        if (note) {
          note.hidden = false;
        }
        button.classList.add("btn-copied");
      }).catch(function () {
        // Clipboard denied: the link is fully visible for manual copy.
      });
    });
  });

  document.querySelectorAll(".js-share-link").forEach(function (button) {
    if (!navigator.share) {
      button.hidden = true; // no native share → hide the ghost button
      return;
    }
    button.addEventListener("click", function () {
      navigator.share({ url: button.dataset.shareUrl }).catch(function () {
        // Share cancelled — nothing to do.
      });
    });
  });

  // Nationality combo: live flag preview next to the ISO code input.
  document.querySelectorAll("[data-country-combo]").forEach(function (combo) {
    var input = combo.querySelector(".combo-input");
    var flag = combo.querySelector("[data-country-flag]");
    if (!input || !flag) {
      return;
    }
    function flagEmoji(code) {
      if (!/^[A-Za-z]{2}$/.test(code)) {
        return null;
      }
      return String.fromCodePoint(
        0x1F1E6 + code.toUpperCase().charCodeAt(0) - 65,
        0x1F1E6 + code.toUpperCase().charCodeAt(1) - 65);
    }
    function update() {
      var emoji = flagEmoji(input.value);
      if (emoji) {
        flag.textContent = emoji;
        flag.classList.remove("combo-flag-empty");
      } else {
        flag.textContent = "";
        flag.classList.add("combo-flag-empty");
      }
    }
    input.addEventListener("input", update);
    update();
  });

  // Manual theme switch (mockup §11): swap the data-theme attribute + cookie
  // in place — no reload. Without JS each segment is a plain form POST.
  document.querySelectorAll(".ts-opt").forEach(function (segment) {
    segment.addEventListener("click", function (event) {
      var style = segment.value; // auto | light | dark
      if (style !== "auto" && style !== "light" && style !== "dark") {
        return; // let the no-JS form POST handle unexpected values
      }
      event.preventDefault();
      var root = document.documentElement;
      if (style === "auto") {
        root.removeAttribute("data-theme");
        document.cookie = "theme=auto;path=/;max-age=31536000;samesite=lax";
      } else {
        root.setAttribute("data-theme", style);
        document.cookie = "theme=" + style + ";path=/;max-age=31536000;samesite=lax";
      }
      document.querySelectorAll(".ts-opt").forEach(function (other) {
        other.setAttribute("aria-checked", String(other.value === style));
      });
    });
  });
})();
