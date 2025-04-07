let currentEventForGuest = null;

function openGuestModal(event) {
  console.log("Opening Guest Modal for event:", event);
  if (!event || !event.id) {
    alert("Invalid event selected. Please try again.");
    return;
  }
  currentEventForGuest = event;
  $('#guestEmails').val('');
  $('#guestModal').modal('show');
}

$('#saveGuestsBtn').on('click', () => {
  const guestEmails = $('#guestEmails').val().split(',')
    .map(e => e.trim())
    .filter(e => e);

  console.log("📝 Saving guests to event:", currentEventForGuest);

  if (!currentEventForGuest || !currentEventForGuest.id || guestEmails.length === 0) {
    alert("Please enter at least one valid email.");
    return;
  }

  $.ajax({
    url: `/api/events/${currentEventForGuest.id}/guests`,
    method: "PUT",
    contentType: "application/json",
    data: JSON.stringify(guestEmails),
    success: function (link) {
      $('#guestModal').modal('hide');
      appendMessage("ai", `👥 Guests added successfully. View it <a href="${link}" target="_blank">here</a>`);
    },
    error: function (xhr) {
      alert("Failed to add guests: " + xhr.responseText);
    }
  });
});

function renderGuestSection(event) {
  if (!event.guests || event.guests.length === 0) return "";

  const guests = Array.isArray(event.guests)
    ? event.guests
    : event.guests.split(",").map(email => email.trim());

  const guestList = guests.map(email => `<li>${email}</li>`).join("");

  return `
    <div class="guest-section mt-2">
      <button class="btn btn-secondary btn-sm toggle-guests">👥 Guests (${guests.length})</button>
      <ul class="guest-list pl-3 mt-2" style="display: none;">${guestList}</ul>
    </div>
  `;
}
// Toggle guest list visibility when clicking the guests button
$(document).on("click", ".toggle-guests", function() {
  $(this).next(".guest-list").slideToggle();
});

