$(document).ready(() => {
  // ✅ Auth button
  $("#authorizeButton").click(() => {
    window.location.href = "/oauth2/authorization/google";
  });

  // ✅ Chat form submission
  $("#chatForm").off("submit").on("submit", function (e) {
    e.preventDefault(); // 🛑 Prevent full page reload

    const message = $("#chatInput").val().trim();
    if (!message) return;

    appendMessage("user", message);
    $("#chatInput").val("");

    sendChatMessage(
      message,
      (response) => {
        try {
          const parsed = JSON.parse(response);

          parsed.forEach((msg) => {
            if (msg.role === "event") {
              // 🗓️ VIEW EVENTS
              if (msg.intent === "VIEW" && msg.start && msg.end) {
                $.ajax({
                  url: "/api/events/view", // ✅ Fixed endpoint
                  method: "GET",
                  data: {
                    start: msg.start,
                    end: msg.end
                  },
                  success: function (events) {
                    if (!events || events.length === 0) {
                      appendMessage("ai", "📭 No events found for that time range.");
                    } else {
                      appendMessage("ai", `📅 Found ${events.length} event(s):`);
                      events.forEach((event) => appendEvent(event));
                    }
                  },
                  error: function (xhr) {
                    appendMessage("ai", "❌ Failed to fetch events: " + xhr.responseText);
                  }
                });

              // ✅ Created event message
              } else if (msg.created) {
                appendMessage("ai", `✅ '${msg.summary}' created at ${msg.date}, ${msg.time}`);
                appendEvent(msg);

              // 🔁 Fallback to appending the event if not matched
              } else {
                appendEvent(msg);
              }

            } else {
              appendMessage(msg.role, msg.content);
            }
          });

        } catch (e) {
          console.error("JSON parsing failed", e);
          appendMessage("ai", response);
        }
      },
      () => {
        appendMessage("ai", "❌ Error contacting server");
      }
    );
  });
});
