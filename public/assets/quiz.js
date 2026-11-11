/* Oathly web practice. No tracking, no cookies; your state choice stays in this browser. */
(function () {
  "use strict";
  var root = document.getElementById("quiz");
  if (!root) return;
  var mode = root.getAttribute("data-mode");
  var base = root.getAttribute("data-base");
  var stage = document.getElementById("quiz-stage");

  function el(tag, attrs, children) {
    var n = document.createElement(tag);
    Object.keys(attrs || {}).forEach(function (k) {
      if (k === "text") n.textContent = attrs[k];
      else if (k === "on") Object.keys(attrs.on).forEach(function (e) { n.addEventListener(e, attrs.on[e]); });
      else n.setAttribute(k, attrs[k]);
    });
    (children || []).forEach(function (c) { if (c) n.appendChild(c); });
    return n;
  }
  function show(nodes) {
    stage.textContent = "";
    nodes.forEach(function (n) { if (n) stage.appendChild(n); });
    var h = stage.querySelector("[data-focus]");
    if (h) { h.setAttribute("tabindex", "-1"); h.focus({ preventScroll: true }); }
    if (stage.getBoundingClientRect().top < 0) stage.scrollIntoView();
  }
  function shuffle(a) {
    a = a.slice();
    for (var i = a.length - 1; i > 0; i--) {
      var j = Math.floor(Math.random() * (i + 1)), t = a[i]; a[i] = a[j]; a[j] = t;
    }
    return a;
  }
  function getJSON(name) {
    return fetch(base + name).then(function (r) {
      if (!r.ok) throw new Error(name);
      return r.json();
    });
  }
  function store(key, value) {
    try {
      if (value === undefined) return localStorage.getItem(key);
      localStorage.setItem(key, value);
    } catch (e) { /* private mode */ }
    return null;
  }
  function list(items) {
    return el("ul", {}, items.map(function (t) { return el("li", { text: t }); }));
  }
  function loadError() {
    show([el("p", { text: "The questions could not be loaded. Check your connection and reload the page." })]);
  }
  function appNote(text) {
    return el("p", { class: "muted", text: text });
  }

  // ---------- U.S. mock interview: say the answer, reveal, mark yourself ----------
  function usStart() {
    var form = document.getElementById("quiz-setup");
    var version = form.querySelector("[name=version]");
    var senior = form.querySelector("[name=senior]");
    var state = form.querySelector("[name=state]");
    var saved = store("oathly.state");
    if (saved) state.value = saved;
    form.addEventListener("submit", function (e) {
      e.preventDefault();
      store("oathly.state", state.value);
      Promise.all([getJSON("us-" + version.value + ".json"), getJSON("places.json")]).then(function (d) {
        form.hidden = true;
        usRun(d[0], d[1][state.value] || null, senior.checked, function () { form.hidden = false; show([]); });
      }).catch(loadError);
    });
  }

  function usRun(bank, place, senior, restart) {
    var r = bank.rules;
    var asked = senior ? r.seniorAsked : r.asked;
    var passAt = senior ? r.seniorPassAt : r.passAt;
    var failAt = asked - passAt + 1;
    var pool = bank.questions.filter(function (q) { return !senior || q.senior; });
    var qs = shuffle(pool).slice(0, asked);
    var i = 0, right = 0, wrong = 0, missed = [];

    function answers(q) {
      if (!q.dyn || q.a.length) return q.a;
      if (place && place.answers[q.dyn]) return place.answers[q.dyn];
      return ["This answer depends on where you live. Choose your state above, or see the list of answers by state."];
    }
    function ask() {
      var q = qs[i];
      show([
        el("p", { class: "progress", text: "Question " + (i + 1) + " of up to " + asked + " · " + right + " right, " + wrong + " wrong" }),
        el("p", { class: "question", "data-focus": "", text: q.q }),
        el("p", { class: "muted", text: q.need > 1 ? "Say " + q.need + " answers out loud, then check." : "Say your answer out loud, then check." }),
        el("div", { class: "btns" }, [el("button", { class: "btn", type: "button", text: "Show the answer", on: { click: function () { reveal(q); } } })])
      ]);
    }
    function reveal(q) {
      var a = answers(q);
      var box = el("div", { class: "answer" }, [
        el("p", { class: "label", text: q.need > 1 ? "Answer (name " + q.need + ")" : a.length > 1 ? "Answer (any one of these)" : "Answer" }),
        list(a)
      ]);
      show([
        el("p", { class: "progress", text: "Question " + (i + 1) + " of up to " + asked }),
        el("p", { class: "question", "data-focus": "", text: q.q }),
        box,
        q.exp ? el("p", { text: q.exp }) : null,
        el("p", { class: "muted", text: "Did you say it correctly?" }),
        el("div", { class: "btns" }, [
          el("button", { class: "btn ok", type: "button", text: "I got it right", on: { click: function () { grade(q, true); } } }),
          el("button", { class: "btn bad", type: "button", text: "I got it wrong", on: { click: function () { grade(q, false); } } })
        ])
      ]);
    }
    function grade(q, ok) {
      if (ok) right++; else { wrong++; missed.push(q); }
      i++;
      if (right >= passAt || wrong >= failAt || i >= qs.length) finish(); else ask();
    }
    function finish() {
      var passed = right >= passAt;
      var nodes = [
        el("div", { class: "card result " + (passed ? "pass" : "fail") }, [
          el("h2", { "data-focus": "", text: passed ? "You passed!" : "Not this time" }),
          el("p", { text: right + " right and " + wrong + " wrong. " + (passed
            ? "The officer stops as soon as you reach " + passAt + " correct answers."
            : "The officer stops when you can no longer reach " + passAt + " correct answers.") })
        ])
      ];
      if (missed.length) {
        nodes.push(el("h3", { text: "Review what you missed" }));
        nodes.push(el("ul", { class: "qa" }, missed.map(function (q) {
          return el("li", {}, [el("p", { class: "q" }, [el("a", { href: q.url, text: q.n + ". " + q.q })]), list(answers(q))]);
        })));
      }
      nodes.push(el("div", { class: "btns" }, [
        el("button", { class: "btn", type: "button", text: "Try again", on: { click: restart } })
      ]));
      nodes.push(appNote("In the Oathly app the questions are read aloud, your spoken or typed answer is checked for you, and the questions you miss come back until you know them."));
      show(nodes);
    }
    ask();
  }

  // ---------- Australian practice test: 20 multiple-choice questions ----------
  function auStart() {
    var btn = document.getElementById("quiz-start");
    btn.addEventListener("click", function () {
      getJSON("au.json").then(function (bank) {
        btn.parentNode.hidden = true;
        auRun(bank, function () { btn.parentNode.hidden = false; show([]); });
      }).catch(loadError);
    });
  }

  function auRun(bank, restart) {
    var r = bank.rules;
    var values = shuffle(bank.questions.filter(function (q) { return q.values; })).slice(0, r.valuesQuestions);
    var other = shuffle(bank.questions.filter(function (q) { return !q.values; })).slice(0, r.questions - r.valuesQuestions);
    var qs = shuffle(values.concat(other));
    var passMark = Math.ceil(qs.length * r.passMarkPercent / 100);
    var i = 0, score = 0, valuesRight = 0, missed = [];

    function ask() {
      var q = qs[i];
      var order = shuffle([0, 1, 2]);
      var done = false;
      var after = el("div", {});
      var buttons = order.map(function (idx) {
        return el("button", { class: "choice", type: "button", text: q.options[idx], on: { click: function () { choose(idx); } } });
      });
      function choose(idx) {
        if (done) return;
        done = true;
        var ok = idx === q.answerIndex;
        if (ok) { score++; if (q.values) valuesRight++; } else missed.push(q);
        buttons.forEach(function (b, k) {
          b.disabled = true;
          if (order[k] === q.answerIndex) b.className = "choice right";
          else if (order[k] === idx) b.className = "choice wrong";
        });
        after.appendChild(el("p", { role: "status", text: (ok ? "Correct. " : "Not quite. ") + q.explanation }));
        after.appendChild(el("div", { class: "btns" }, [
          el("button", { class: "btn", type: "button", text: i + 1 < qs.length ? "Next question" : "See your result", on: { click: next } })
        ]));
        after.querySelector("button").focus();
      }
      show([
        el("p", { class: "progress", text: "Question " + (i + 1) + " of " + qs.length + (q.values ? " · Australian values question" : "") }),
        el("p", { class: "question", "data-focus": "", text: q.question }),
        el("div", {}, buttons),
        after
      ]);
    }
    function next() { i++; if (i >= qs.length) finish(); else ask(); }
    function finish() {
      var valuesOk = valuesRight === values.length;
      var passed = score >= passMark && valuesOk;
      var nodes = [
        el("div", { class: "card result " + (passed ? "pass" : "fail") }, [
          el("h2", { "data-focus": "", text: passed ? "You passed!" : "Not this time" }),
          el("p", { text: "You scored " + score + " of " + qs.length + " (you need " + passMark + "). Values questions: " + valuesRight + " of " + values.length + (valuesOk ? "." : " (all of them must be right to pass).") })
        ])
      ];
      if (missed.length) {
        nodes.push(el("h3", { text: "Review what you missed" }));
        nodes.push(el("ul", { class: "qa" }, missed.map(function (q) {
          return el("li", {}, [el("p", { class: "q", text: q.question }), el("p", { text: "Answer: " + q.options[q.answerIndex] }), el("p", { class: "why", text: q.explanation })]);
        })));
      }
      nodes.push(el("div", { class: "btns" }, [el("button", { class: "btn", type: "button", text: "Take another test", on: { click: restart } })]));
      nodes.push(appNote("The Oathly app keeps track of what you miss and brings those questions back until you know them."));
      show(nodes);
    }
    ask();
  }

  if (mode === "us") usStart();
  if (mode === "au") auStart();
})();
