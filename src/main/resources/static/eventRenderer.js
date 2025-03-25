function appendMessage(sender, text) {
  const msgDiv = $("<div></div>").addClass("message " + sender).text(text);
  $("#chatWindow").append(msgDiv).append("<div class='clear'></div>");
  scrollToBottom();
}

function appendEvent(event) {
  const card = $("<div class='event-card'></div>");
  const summary = $("<div class='event-summary'></div>").text(event.summary);

  // 🗓️ Add emoji icons with proper spacing
  const dateText = $("<span></span>").text(`📅 ${event.date}`);
  const timeText = $("<span></span>").text(`🕒 ${event.time || "N/A"}`);
  const date = $("<div class='event-date'></div>").append(dateText, " ", timeText);

  // ❌ Delete button with icon inside
const deleteBtn = $("<button class='delete-event'></button>")
  .html("❌")
  .on("click", () => {
    deleteEvent(
      event.calendarId,
      event.id,
      card,
      (el) => el.html("<div class='event-deleted'>DELETED</div>"),
      (xhr) => alert("Delete failed: " + xhr.responseText)
    );
  });

  // ✏️ Edit button with icon
  const editBtn = $("<button class='edit-event btn btn-primary btn-sm'></button>")
    .html("✏️")
    .on("click", () => openEditModal(event));

  const buttons = $("<div class='button-container'></div>").append(editBtn, deleteBtn);

  card.append(summary, date, buttons);
  $("#chatWindow").append(card);
  scrollToBottom();
}

