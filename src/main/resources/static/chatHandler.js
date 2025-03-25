$(document).ready(() => {
  $("#authorizeButton").click(() => window.location.href = "/authorize-google");

  $("#chatForm").submit(e => {
    e.preventDefault();
    const message = $("#chatInput").val().trim();
    if (!message) return;

    appendMessage("user", message);
    $("#chatInput").val("");

    sendChatMessage(message, (response) => {
      try {
        const parsed = JSON.parse(response);
        parsed.forEach(msg => {
          if (msg.role === "event") {
            if (msg.created) {
              appendMessage("ai", `✅ '${msg.summary}' created at ${msg.date}, ${msg.time}`);
            }
            appendEvent(msg); // 👈 always render the event card
          } else {
            appendMessage(msg.role, msg.content);
          }
        });
      } catch (e) {
        appendMessage("ai", response);
      }
    }, () => {
      appendMessage("ai", "Error contacting server");
    });
  });
});
