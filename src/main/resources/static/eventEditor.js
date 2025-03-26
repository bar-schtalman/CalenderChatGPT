let currentEditingEvent = null;

function openEditModal(event) {
  currentEditingEvent = event;

  const start = event.start || "";
  const end = event.end || "";
  const [startDate, startTime] = start.split(" ");
  const [endDate, endTime] = end.split(" ");

  $("#eventSummary").val(event.summary || "");
  $("#startDate").val(formatDateForInput(startDate));
  $("#startTime").val(formatTimeForInput(startTime));
  $("#endDate").val(formatDateForInput(endDate || startDate));
  $("#endTime").val(formatTimeForInput(endTime));

  $("#editModal").modal("show");
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
      location.reload(); // Optional: refresh the UI
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
