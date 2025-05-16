// 🔐 Helper function to return auth header
function authHeader() {
  const token = localStorage.getItem("AUTH_TOKEN");
  console.log("JWT Token: ", token);
  return token ? { Authorization: `Bearer ${token}` } : {};
}

// 🧠 Send message to chat API
function sendChatMessage(message, onSuccess, onError) {
  const userId = sessionStorage.getItem("userId"); // <-- must be set after login

  if (!userId) {
    console.error("Missing userId in sessionStorage");
    return;
  }

  $.ajax({
    url: "/chat",
    method: "GET",
    headers: authHeader(),
    data: {
      prompt: message,
      userId: userId,
    },
    success: onSuccess,
    error: onError,
  });
}

// ❌ Delete calendar event
function deleteEvent(calendarId, eventId, element, onSuccess, onError) {
  $.ajax({
    url: `/api/google-calendar/calendars/${calendarId}/events/${eventId}`,
    method: "DELETE",
    headers: authHeader(),
    success: () => onSuccess(element),
    error: onError,
  });
}

// 🔁 Update calendar event
function updateEvent(calendarId, eventId, eventData, onSuccess, onError) {
  $.ajax({
    url: `/api/google-calendar/calendars/${calendarId}/events/${eventId}`,
    method: "PUT",
    headers: authHeader(),
    contentType: "application/json",
    data: JSON.stringify(eventData),
    success: onSuccess,
    error: onError,
  });
}

// 🙋 Fetch current authenticated user
fetch("/api/me", {
  headers: authHeader(),
})
  .then((res) => {
    if (!res.ok) {
      throw new Error("Unauthorized");
    }
    return res.json();
  })
  .then((user) => {
    sessionStorage.setItem("userId", user.id);
  })
  .catch(() => console.error("🔴 Couldn't fetch user session"));
