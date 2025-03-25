$(document).ready(function () {
  let currentEditingEvent = null;

  $("#chatForm").on("submit", function (e) {
    e.preventDefault();
    const message = $("#chatInput").val().trim();
    if (message === "") return;

    appendMessage("user", message);
    $("#chatInput").val("");

    $.ajax({
      url: "/chat",
      method: "GET",
      data: { prompt: message },
      success: function (response) {
        try {
          const parsed = JSON.parse(response);
          const isViewIntent = parsed.some(msg => msg.role === "event");

          parsed.forEach(msg => {
            if (msg.role === "event") {
              if (msg.created === true) {
                appendMessage("ai", `✅ Event '${msg.summary}' created for ${msg.date} at ${msg.time}`);
              }
              appendEvent(msg); // Render for both view & created
            } else {
              appendMessage(msg.role, msg.content);
            }
          });
        } catch (e) {
          appendMessage("ai", response);
        }
      },
      error: function () {
        appendMessage("ai", "Sorry, there was an error processing your request.");
      }
    });
  });

  function appendMessage(sender, text) {
    const msgDiv = $("<div></div>").addClass("message " + sender).text(text);
    $("#chatWindow").append(msgDiv).append("<div class='clear'></div>");
    $("#chatWindow").scrollTop($("#chatWindow")[0].scrollHeight);
  }

  function appendEvent(event) {
    const eventCard = $("<div></div>").addClass("event-card");
    const eventSummary = $("<div></div>").addClass("event-summary").text(event.summary);
    const eventDate = $("<div></div>").addClass("event-date").text("📅 " + event.date + " ⏰ " + (event.time || "N/A"));

    const deleteBtn = $("<button></button>")
      .addClass("delete-event")
      .text("❌")
      .on("click", function () {
        deleteEvent(event.calendarId, event.id, eventCard);
      });

    const editBtn = $("<button></button>")
      .addClass("edit-event")
      .text("✏️")
      .on("click", function () {
        currentEditingEvent = event;

        const startDate = event.date;
        const endDate = event.endDate || event.date;
        const [startTimeRaw, endTimeRaw] = (event.time || "").split("-").map(t => t.trim());

        $("#eventSummary").val(event.summary);
        $("#startDate").val(formatDateForInput(startDate));
        $("#startTime").val(formatTimeForInput(startTimeRaw));
        $("#endDate").val(formatDateForInput(endDate));
        $("#endTime").val(formatTimeForInput(endTimeRaw));

        $("#editModal").modal("show");
      });

    const buttonContainer = $("<div></div>").addClass("button-container").append(editBtn).append(deleteBtn);
    eventCard.append(eventSummary).append(eventDate).append(buttonContainer);
    $("#chatWindow").append(eventCard);
    $("#chatWindow").scrollTop($("#chatWindow")[0].scrollHeight);
  }

  function deleteEvent(calendarId, eventId, eventElement) {
    $.ajax({
      url: `/api/google-calendar/calendars/${calendarId}/events/${eventId}`,
      method: "DELETE",
      success: function () {
        eventElement.html("<div class='event-deleted'>DELETED</div>");
      },
      error: function (xhr) {
        alert("Error deleting event: " + xhr.responseText);
      }
    });
  }

  $("#saveEdit").click(function () {
    const summary = $("#eventSummary").val().trim();
    const startDate = $("#startDate").val();
    const startTime = $("#startTime").val();
    const endDate = $("#endDate").val();
    const endTime = $("#endTime").val();

    if (!summary || !startDate || !startTime || !endDate || !endTime) {
      alert("Please fill in all fields.");
      return;
    }

    const start = `${startDate}T${startTime}`;
    const end = `${endDate}T${endTime}`;

    const updatedEvent = {
      summary: summary,
      start: start,
      end: end,
      description: "",
      location: ""
    };

    $.ajax({
      url: `/api/google-calendar/calendars/${currentEditingEvent.calendarId}/events/${currentEditingEvent.id}`,
      method: "PUT",
      contentType: "application/json",
      data: JSON.stringify(updatedEvent),
      success: function () {
        alert("Event updated successfully!");
        $("#editModal").modal("hide");
      },
      error: function (xhr) {
        alert("Error updating event: " + xhr.responseText);
      }
    });
  });

  $("#authorizeButton").click(function () {
    window.location.href = '/authorize-google';
  });

  function formatDateForInput(dateStr) {
    const match = dateStr.match(/^(\d{2})[-.](\d{2})[-.](\d{4})$/);
    if (match) {
      const [, day, month, year] = match;
      return `${year}-${month}-${day}`;
    }

    const fallback = new Date(dateStr);
    if (!isNaN(fallback)) {
      return fallback.toISOString().split("T")[0];
    }

    return '';
  }

  function formatTimeForInput(timeStr) {
    if (!timeStr) return '';
    const ampmMatch = timeStr.match(/(\d{1,2}):(\d{2})\s*(AM|PM)/i);
    if (ampmMatch) {
      let hour = parseInt(ampmMatch[1]);
      const minute = ampmMatch[2];
      const period = ampmMatch[3].toUpperCase();
      if (period === "PM" && hour !== 12) hour += 12;
      if (period === "AM" && hour === 12) hour = 0;
      return `${String(hour).padStart(2, '0')}:${minute}`;
    }

    const twentyFourMatch = timeStr.match(/^(\d{2}):(\d{2})$/);
    if (twentyFourMatch) return timeStr;

    const temp = new Date(`1970-01-01T${timeStr}`);
    if (!isNaN(temp)) return temp.toTimeString().slice(0, 5);

    console.warn("Unable to parse time:", timeStr);
    return '';
  }
});
