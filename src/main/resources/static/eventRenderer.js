function appendMessage(sender, text) {
  const msgDiv = $("<div></div>").addClass("message " + sender).text(text);
  $("#chatWindow").append(msgDiv).append("<div class='clear'></div>");
  scrollToBottom();
}

function appendEvent(event) {
  const card = $("<div class='event-card'></div>");
  const summary = $("<div class='event-summary'></div>").text(event.summary);
  const date = $("<div class='event-date'></div>").text(`📅 ${event.date} ⏰ ${event.time || "N/A"}`);

  const deleteBtn = $("<button class='delete-event'>❌</button>").on("click", () => {
    deleteEvent(event.calendarId, event.id, card, (el) => el.html("<div class='event-deleted'>DELETED</div>"),
      (xhr) => alert("Delete failed: " + xhr.responseText));
  });

  const editBtn = $("<button class='edit-event'>✏️</button>").on("click", () => openEditModal(event));

  const buttons = $("<div class='button-container'></div>").append(editBtn, deleteBtn);
  card.append(summary, date, buttons);
  $("#chatWindow").append(card);
  scrollToBottom();
}