$(document).ready(() => {
  const browserTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  console.log("🕒 Detected browser time zone:", browserTimeZone);

  let selectedCalendarId = null;

  function loadCalendars() {
    $.ajax({
      url: '/api/google-calendar/calendars',
      method: 'GET',
      success: function (data) {
        const calendarSelect = $('#calendarSelect');
        calendarSelect.empty();

        let defaultCalendar = null;

        if (data.length > 0) {
          data.forEach(function (calendar) {
            const option = $('<option></option>')
              .attr('value', calendar.id)
              .text(calendar.name);

            if (calendar.primary) {
              option.prop('selected', true);
              defaultCalendar = calendar.id;
            }

            calendarSelect.append(option);
          });

          if (!defaultCalendar && data[0]) {
            defaultCalendar = data[0].id;
            calendarSelect.val(defaultCalendar);
          }

          selectedCalendarId = defaultCalendar || data[0].id;
          updateServerCalendar(selectedCalendarId);
        } else {
          calendarSelect.append('<option>No calendars found</option>');
        }
      },
      error: function (xhr, status, error) {
        console.error("Error loading calendars:", status, error, xhr.responseText);
        alert("Failed to load calendars. Check the console for more details.");
      }
    });
  }

  loadCalendars();

  function updateServerCalendar(calendarId) {
    $.ajax({
      url: '/api/google-calendar/calendars/select-calendar',
      method: 'POST',
      data: { calendarId },
      success: function () {
        console.log("✅ Calendar ID updated on server: " + calendarId);
      },
      error: function () {
        alert("❌ Failed to update selected calendar on server");
      }
    });
  }

  $('#calendarSelect').on('change', function () {
    selectedCalendarId = $(this).val();
    console.log("Selected Calendar ID: " + selectedCalendarId);
    sessionStorage.setItem('selectedCalendarId', selectedCalendarId);
    updateServerCalendar(selectedCalendarId);
  });

  $("#chatForm").off("submit").on("submit", function (e) {
    e.preventDefault();

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
              appendEvent(msg); // ✅ Always just append event card
            } else {
              appendMessage(msg.role, msg.content); // 📅 "Events for X" message OR ai reply
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
