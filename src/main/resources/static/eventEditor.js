let currentEditingEvent = null;

function openEditModal(event) {
  currentEditingEvent = event;
  const [startTime, endTime] = (event.time || '').split("-").map(t => t.trim());
  $("#eventSummary").val(event.summary);
  $("#startDate").val(formatDateForInput(event.date));
  $("#startTime").val(formatTimeForInput(startTime));
  $("#endDate").val(formatDateForInput(event.endDate || event.date));
  $("#endTime").val(formatTimeForInput(endTime));
  $("#editModal").modal("show");
}

$("#saveEdit").click(() => {
  const summary = $("#eventSummary").val().trim();
  const start = `${$("#startDate").val()}T${$("#startTime").val()}`;
  const end = `${$("#endDate").val()}T${$("#endTime").val()}`;

  if (!(summary && start && end)) return alert("All fields required");

  updateEvent(currentEditingEvent.calendarId, currentEditingEvent.id, {
    summary, start, end, description: "", location: ""
  }, () => {
    alert("Event updated");
    $("#editModal").modal("hide");
  }, (xhr) => alert("Update error: " + xhr.responseText));
});