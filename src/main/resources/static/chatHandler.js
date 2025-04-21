// 🔐 Helper to inject Bearer token from localStorage
function authHeader() {
  const token = localStorage.getItem("AUTH_TOKEN");
  return token ? { 'Authorization': `Bearer ${token}` } : {};
}

$(document).ready(() => {
  // ✅ Store JWT from ?token=... into localStorage
  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get('token');
  if (token) {
    localStorage.setItem('AUTH_TOKEN', token);
    const newUrl = window.location.origin + window.location.pathname;
    window.history.replaceState({}, document.title, newUrl);
  }

  const browserTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  console.log("🕒 Detected browser time zone:", browserTimeZone);

  let selectedCalendarId = null;

  function loadCalendars() {
    $.ajax({
      url: '/api/google-calendar/calendars',
      method: 'GET',
      headers: authHeader(),
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

  function updateServerCalendar(calendarId) {
    $.ajax({
      url: '/api/google-calendar/calendars/select-calendar',
      method: 'POST',
      headers: authHeader(),
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

    $.ajax({
      url: "/chat/message",
      method: "POST",
      headers: {
        ...authHeader(),
        'Content-Type': 'application/json'
      },
      data: JSON.stringify(message),
      success: (response) => {
        try {
          const parsed = JSON.parse(response);

          parsed.forEach((msg) => {
            if (msg.role === "event") {
              appendEvent(msg);
            } else {
              appendMessage(msg.role, msg.content);
            }
          });
        } catch (e) {
          console.error("JSON parsing failed", e);
          appendMessage("ai", response);
        }
      },
      error: (xhr) => {
        console.error("❌ Chat API error:", xhr.responseText);
        appendMessage("ai", "❌ Error contacting server");
      }
    });
  });

  loadCalendars();
});
