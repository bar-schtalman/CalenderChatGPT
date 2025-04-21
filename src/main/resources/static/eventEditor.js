// 🔐 Include Authorization header
function authHeader() {
  const token = localStorage.getItem("AUTH_TOKEN");
  return token ? { 'Authorization': `Bearer ${token}` } : {};
}

let currentEditingEvent = null;

function openEditModal(event) {
  currentEditingEvent = event;

  $.ajax({
    url: `/api/events/${event.id}`,
    method: "GET",
    headers: authHeader(),
    success: function (data) {
      const [startDate, startTime] = data.start.split(" ");
      const [endDate, endTime] = data.end.split(" ");

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
    end,
    timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone
  };

  $.ajax({
    url: `/api/events/update/${currentEditingEvent.id}`,
    method: "PUT",
    headers: authHeader(),
    contentType: "application/json",
    data: JSON.stringify(updatedEvent),
    success: () => {
      $("#editModal").modal("hide");
      alert("✅ Event updated!");

      $.ajax({
        url: `/api/events/${currentEditingEvent.id}`,
        method: "GET",
        headers: authHeader(),
        success: (updatedData) => {
          // ⚡ Fix: parse string like "14-05-2025 16:00" manually
          const [startDateStr, startTimeStr] = updatedData.start.split(" ");
          const [endDateStr, endTimeStr] = updatedData.end.split(" ");

          updatedData.date = startDateStr;
          updatedData.time = `${startTimeStr} - ${endTimeStr}`;

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
