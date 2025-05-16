// 🔐 Auth helper
function authHeader() {
  const token = localStorage.getItem("AUTH_TOKEN");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

let currentEventForGuest = null;

function openGuestModal(event) {
  console.log("Opening Guest Modal for event:", event);
  if (!event || !event.id) {
    alert("Invalid event selected. Please try again.");
    return;
  }
  currentEventForGuest = event;
  $("#guestEmails").val("");
  $("#guestModal").modal("show");
}

// Save new guests
$("#saveGuestsBtn").on("click", () => {
  const guestEmails = $("#guestEmails")
    .val()
    .split(",")
    .map((e) => e.trim())
    .filter((e) => e);

  if (
    !currentEventForGuest ||
    !currentEventForGuest.id ||
    guestEmails.length === 0
  ) {
    alert("Please enter at least one valid email.");
    return;
  }

  $.ajax({
    url: `/api/events/${currentEventForGuest.id}/guests`,
    method: "PUT",
    headers: authHeader(),
    contentType: "application/json",
    data: JSON.stringify(guestEmails),
    success: function (updatedEvent) {
      $("#guestModal").modal("hide");
      $(`#event-${updatedEvent.id}`).replaceWith(
        window.renderEventCard(updatedEvent),
      );
    },
    error: function (xhr) {
      alert("Failed to add guests: " + xhr.responseText);
    },
  });
});

// Remove individual guest
$(document).on("click", ".remove-guest-btn", function () {
  const email = $(this).data("email");
  const eventId = $(this).data("event-id");

  if (!confirm(`Remove guest: ${email}?`)) return;

  $.ajax({
    url: `/api/events/${eventId}/guests/remove`,
    method: "PUT",
    headers: authHeader(),
    contentType: "application/json",
    data: JSON.stringify([email]),
    success: function (updatedEvent) {
      $(`#event-${updatedEvent.id}`).replaceWith(
        window.renderEventCard(updatedEvent),
      );
    },
    error: function (xhr) {
      alert("Failed to remove guest: " + xhr.responseText);
    },
  });
});

// Utility for splitting emails
function splitEmails(val) {
  return val.split(/,\s*/);
}

function extractLastEmailFragment(term) {
  return splitEmails(term).pop();
}

// Autocomplete contact suggestions
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
        const term = extractLastEmailFragment(request.term);
        $.ajax({
          url: "/api/contacts/search",
          dataType: "json",
          headers: authHeader(),
          data: { query: term },
          success: function (data) {
            console.log("📨 Contacts data returned from server:", data);
            response(
              $.map(data, function (item) {
                return {
                  label: `${item.name} <${item.email}>`,
                  value: item.email,
                };
              }),
            );
          },
          error: function (xhr) {
            console.error("❌ Failed to fetch contacts:", xhr.responseText);
          },
        });
      },
      focus: function () {
        return false;
      },
      select: function (event, ui) {
        let terms = splitEmails(this.value);
        terms.pop();
        terms.push(ui.item.value);
        terms.push("");
        this.value = terms.join(", ");
        return false;
      },
    });
});

// Render guest section in event card
function renderGuestSection(event) {
  if (!event.guests || event.guests.length === 0) return "";

  const guests = Array.isArray(event.guests)
    ? event.guests
    : event.guests.split(",").map((email) => email.trim());

  const guestList = guests
    .map(
      (email) => `
    <li class="guest-item d-flex justify-content-between align-items-center">
      ${email}
      <button class="btn btn-sm btn-danger ml-2 remove-guest-btn" data-email="${email}" data-event-id="${event.id}">&times;</button>
    </li>
  `,
    )
    .join("");

  return `
    <div class="guest-section mt-2">
      <button class="btn btn-secondary btn-sm toggle-guests">👥 Guests (${guests.length})</button>
      <ul class="guest-list pl-3 mt-2" style="display: none;">${guestList}</ul>
    </div>
  `;
}

// Toggle guest list visibility
$(document).on("click", ".toggle-guests", function () {
  $(this).next(".guest-list").slideToggle();
});
