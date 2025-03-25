function sendChatMessage(message, onSuccess, onError) {
  $.ajax({
    url: "/chat",
    method: "GET",
    data: { prompt: message },
    success: onSuccess,
    error: onError
  });
}

function deleteEvent(calendarId, eventId, element, onSuccess, onError) {
  $.ajax({
    url: `/api/google-calendar/calendars/${calendarId}/events/${eventId}`,
    method: "DELETE",
    success: () => onSuccess(element),
    error: onError
  });
}

function updateEvent(calendarId, eventId, eventData, onSuccess, onError) {
  $.ajax({
    url: `/api/google-calendar/calendars/${calendarId}/events/${eventId}`,
    method: "PUT",
    contentType: "application/json",
    data: JSON.stringify(eventData),
    success: onSuccess,
    error: onError
  });
}
