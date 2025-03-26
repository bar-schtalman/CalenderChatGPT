function appendMessage(sender, text) {
  const msgDiv = $("<div></div>").addClass("message " + sender).text(text);
  $("#chatWindow").append(msgDiv).append("<div class='clear'></div>");
  scrollToBottom();
}

function appendEvent(event) {
  const card = $("<div class='event-card'></div>");
  const summary = $("<div class='event-summary'></div>").text(event.summary);

  const date = event.date || "?";
  const time = event.time || "N/A - ?";

  const dateText = $("<span></span>").text(`📅 ${date}`);
  const timeText = $("<span></span>").text(`🕒 ${time}`);
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
