function sendChatMessage(message, onSuccess, onError) {
  const userId = sessionStorage.getItem("userId"); // <-- must be set after login

  if (!userId) {
    console.error("Missing userId in sessionStorage");
    return;
  }

  $.ajax({
    url: "/chat",
    method: "GET",
    data: {
      prompt: message,
      userId: userId
    },
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

fetch('/api/me')
  .then(res => res.json())
  .then(user => {
    sessionStorage.setItem('userId', user.id);
  })
  .catch(() => console.error("🔴 Couldn't fetch user session"));

