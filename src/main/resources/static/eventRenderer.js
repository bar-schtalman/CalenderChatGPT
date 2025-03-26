// ✅ Message appending logic
function appendMessage(sender, text) {
  const msgDiv = $("<div></div>").addClass("message " + sender).text(text);
  $("#chatWindow").append(msgDiv).append("<div class='clear'></div>");
  scrollToBottom();
}

// ✅ Scroll chat window to bottom
function scrollToBottom() {
  const chatWindow = $("#chatWindow")[0];
  if (chatWindow) {
    chatWindow.scrollTop = chatWindow.scrollHeight;
  }
}

// ✅ Event card renderer
function appendEvent(event) {
  const card = $("<div class='event-card'></div>");
  const summary = $("<div class='event-summary'></div>").text(event.summary);

  // 🗓️ Extract date and time from backend format "dd-MM-yyyy HH:mm"
  const startParts = (event.start || "").split(" ");
  const endParts = (event.end || "").split(" ");

  const date = startParts[0] || "?";
  const startTime = startParts[1] || "N/A";
  const endTime = endParts[1] || "?";

  // Save for edit modal usage
  event.date = date;
  event.time = `${startTime} - ${endTime}`;
  event.endDate = endParts[0] || date;

  const dateText = $("<span></span>").text(`📅 ${date}`);
  const timeText = $("<span></span>").text(`🕒 ${startTime} - ${endTime}`);
  const dateRow = $("<div class='event-date'></div>").append(dateText, " ", timeText);

  const deleteBtn = $("<button class='delete-event'></button>")
    .html("❌")
    .on("click", () => {
      $.ajax({
        url: `/api/events/delete/${event.id}`,
        method: "DELETE",
        success: () => {
          card.html("<div class='event-deleted'>DELETED</div>");
        },
        error: (xhr) => {
          alert("Delete failed: " + xhr.responseText);
        }
      });
    });

  const editBtn = $("<button class='edit-event btn btn-primary btn-sm'></button>")
    .html("✏️")
    .on("click", () => openEditModal(event));

  const buttons = $("<div class='button-container'></div>").append(editBtn, deleteBtn);

  card.append(summary, dateRow, buttons);
  $("#chatWindow").append(card);
  scrollToBottom();
}
