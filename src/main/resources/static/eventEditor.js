let currentEditingEvent = null;

function openEditModal(event) {
  currentEditingEvent = event;

  $.ajax({
    url: `/api/events/${event.id}`,
    method: "GET",
    success: function (data) {
      const start = data.start || "";
      const end = data.end || "";

      const [startDate, startTime] = start.split(" ");
      const [endDate, endTime] = end.split(" ");

      $("#eventSummary").val(data.summary || "");
      $("#startDate").val(formatDateForInput(startDate));
      $("#startTime").val(formatTimeForInput(startTime));
      $("#endDate").val(formatDateForInput(endDate || startDate));
      $("#endTime").val(formatTimeForInput(endTime));

      $("#editModal").modal("show");
    },
    error: function (xhr) {
      alert("❌ Failed to fetch event: " + xhr.responseText);
    }
  });
}

$("#saveEdit").click(() => {
  const summary = $("#eventSummary").val().trim();
  const start = `${$("#startDate").val()}T${$("#startTime").val()}`;
  const end = `${$("#endDate").val()}T${$("#endTime").val()}`;

  if (!(summary && start && end)) {
    alert("All fields are required");
    return;
  }

  const updatedEvent = {
    summary,
    description: "",
    location: "",
    start,
    end
  };

  $.ajax({
    url: `/api/events/update/${currentEditingEvent.id}`,
    method: "PUT",
    contentType: "application/json",
    data: JSON.stringify(updatedEvent),
    success: () => {
      $("#editModal").modal("hide");
      alert("✅ Event updated!");

      // Re-fetch event and update UI
      $.ajax({
        url: `/api/events/${currentEditingEvent.id}`,
        method: "GET",
        success: (updatedData) => {
          // Format fields for card display
          const startDateTime = new Date(updatedData.start);
          const endDateTime = new Date(updatedData.end);

          updatedData.date = startDateTime.toLocaleDateString("en-GB"); // dd/mm/yyyy
          updatedData.time = `${startDateTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - ${endDateTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;

          refreshEventInUI(updatedData);
        },
        error: (xhr) => {
          console.error("❌ Failed to fetch updated event:", xhr.responseText);
        }
      });
    },
    error: (xhr) => {
      alert("❌ Update failed: " + xhr.responseText);
    }
  });
});

function formatDateForInput(dateStr) {
  if (!dateStr) return '';
  const [day, month, year] = dateStr.split("-");
  return `${year}-${month}-${day}`;
}

function formatTimeForInput(timeStr) {
  return timeStr || '';
}
