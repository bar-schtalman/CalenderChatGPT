// 🔐 Include Authorization header
function authHeader() {
  const token = localStorage.getItem("AUTH_TOKEN");
  return token ? { 'Authorization': `Bearer ${token}` } : {};
}

function appendMessage(sender, text) {
  const msgDiv = $("<div></div>").addClass("message " + sender).html(text);
  $("#chatWindow").append(msgDiv).append("<div class='clear'></div>");
  scrollToBottom();
}

function appendEvent(event) {
  const card = $(`<div class='event-card' id='event-${event.id}'></div>`);
  const summary = $("<div class='event-summary'></div>").text(event.summary);

  const date = event.date || "?";
  const time = event.time || "N/A - ?";

  const dateText = $("<span class='event-date-text'></span>").text(`📅 ${date}`);
  const timeText = $("<span class='event-time-text'></span>").text(`🕒 ${time}`);
  const dateRow = $("<div class='event-date'></div>").append(dateText, " ", timeText);

  const deleteBtn = $("<button class='delete-event'></button>")
    .html("❌")
    .on("click", () => {
      $.ajax({
        url: `/api/events/delete/${event.id}`,
        method: "DELETE",
        headers: authHeader(),
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

  const guestBtn = $("<button class='guest-event btn btn-info btn-sm'></button>")
    .html("➕")
    .on("click", () => {
      console.log("Opening guest modal for event:", event.id);
      openGuestModal(event);
    });

  const buttons = $("<div class='button-container'></div>").append(editBtn, deleteBtn, guestBtn);
  const guestSection = renderGuestSection(event);

  card.append(summary, dateRow, buttons, guestSection);
  $("#chatWindow").append(card);
  scrollToBottom();
}

function refreshEventInUI(event) {
  const $card = $(`#event-${event.id}`);
  if ($card.length === 0) return;

  const summary = $("<div class='event-summary'></div>").text(event.summary);
  const date = event.date || "?";
  const time = event.time || "N/A - ?";

  const dateText = $("<span class='event-date-text'></span>").text(`📅 ${date}`);
  const timeText = $("<span class='event-time-text'></span>").text(`🕒 ${time}`);
  const dateRow = $("<div class='event-date'></div>").append(dateText, " ", timeText);

  const deleteBtn = $("<button class='delete-event'></button>")
    .html("❌")
    .on("click", () => {
      $.ajax({
        url: `/api/events/delete/${event.id}`,
        method: "DELETE",
        headers: authHeader(), // 👈 added this for JWT
        success: () => {
          $card.html("<div class='event-deleted'>DELETED</div>");
        },
        error: (xhr) => {
          alert("Delete failed: " + xhr.responseText);
        }
      });
    });

  const editBtn = $("<button class='edit-event btn btn-primary btn-sm'></button>")
    .html("✏️")
    .on("click", () => openEditModal(event));

  const guestBtn = $("<button class='guest-event btn btn-info btn-sm'></button>")
    .html("➕")
    .on("click", () => openGuestModal(event));

  const buttons = $("<div class='button-container'></div>").append(editBtn, deleteBtn, guestBtn);
  const guestSection = renderGuestSection(event);

  $card.empty().append(summary, dateRow, buttons, guestSection);
}


// Make appendEvent globally accessible as renderEventCard
window.renderEventCard = appendEvent;
