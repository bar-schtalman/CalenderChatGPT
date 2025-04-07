// guestRenderer.js

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
    .filter(e => e.length > 0);

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
success: function (updatedEvent) {
  $('#guestModal').modal('hide');
  $(`#event-${updatedEvent.id}`).remove(); // Remove old event block
  appendEvent(updatedEvent);              // Render updated event with new guests
}
,
    error: function (xhr) {
      alert("Failed to add guests: " + xhr.responseText);
    }
  });
});

// Helper functions for autocomplete
function split(val) {
  return val.split(/,\s*/);
}

function extractLast(term) {
  return split(term).pop();
}

// jQuery UI Autocomplete Setup
$(document).ready(function () {
  $("#guestEmails")
    .on("keydown", function (event) {
      if (event.key === "Tab" && $(".ui-menu-item-wrapper:visible").length) {
        event.preventDefault();
      }
    })
    .autocomplete({
      minLength: 1,
      source: function (request, response) {
        const lastTerm = extractLast(request.term);
        if (!lastTerm) return;

        $.ajax({
          url: "/api/contacts/search",
          dataType: "json",
          data: { query: lastTerm },
          success: function (data) {
            console.log("📨 Contacts data returned from server:", data);
            response($.map(data, function (item) {
              return {
                label: `${item.name} <${item.email}>`,
                value: item.email
              };
            }));
          },
          error: function (xhr) {
            console.error("❌ Failed to fetch contacts:", xhr.responseText);
          }
        });
      },
      focus: function () {
        return false; // prevent autocomplete from overwriting whole field
      },
      select: function (event, ui) {
        let terms = split(this.value);
        terms.pop(); // remove current input
        terms.push(ui.item.value); // add selected contact
        terms.push(""); // add placeholder for next
        this.value = terms.join(", ");
        return false;
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

$(document).on("click", ".toggle-guests", function () {
  $(this).next(".guest-list").slideToggle();
});
