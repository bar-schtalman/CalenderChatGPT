function formatDateForInput(dateStr) {
  const match = dateStr.match(/^\d{2}[-.]\d{2}[-.]\d{4}$/);
  if (match) {
    const [day, month, year] = dateStr.split(/[.-]/);
    return `${year}-${month}-${day}`;
  }
  const fallback = new Date(dateStr);
  return !isNaN(fallback) ? fallback.toISOString().split("T")[0] : '';
}

function formatTimeForInput(timeStr) {
  if (!timeStr) return '';
  const ampmMatch = timeStr.match(/(\d{1,2}):(\d{2})\s*(AM|PM)/i);
  if (ampmMatch) {
    let [_, hour, minute, period] = ampmMatch;
    hour = parseInt(hour);
    if (period.toUpperCase() === 'PM' && hour !== 12) hour += 12;
    if (period.toUpperCase() === 'AM' && hour === 12) hour = 0;
    return `${String(hour).padStart(2, '0')}:${minute}`;
  }
  return timeStr.match(/^\d{2}:\d{2}$/) ? timeStr : '';
}

function scrollToBottom() {
  $("#chatWindow").scrollTop($("#chatWindow")[0].scrollHeight);
}