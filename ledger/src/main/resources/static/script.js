/**
 * Sends a JSON payload to a specified endpoint.
 *
 * @param {string} endpoint The URL endpoint to send the JSON to.
 * @param {string} jsonPayload The JSON string to send in the request body.
 * @param {HTMLElement} responseArea The DOM element to display the response message.
 */
async function sendJson(endpoint, jsonPayload, responseArea) {
    try {
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsonPayload
        });

        // Check if the response was successful (status code 2xx)
        if (!response.ok) {
            // If not successful, try to read error message from response
            const errorText = await response.text();
            throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
        }

        const result = await response.text(); // assuming your controller returns plain text
        responseArea.textContent = result;
        responseArea.style.color = 'green'; // Set success color
    } catch (error) {
        responseArea.textContent = "An error occurred: " + error.message;
        responseArea.style.color = 'red'; // Set error color
    }
}

// Add an event listener to the form to call sendJson when submitted
document.addEventListener('DOMContentLoaded', () => {
    const form = document.querySelector('form');
    if (form) {
        form.addEventListener('submit', async (event) => {
            event.preventDefault(); // Prevent default form submission

            const jsonInput = document.getElementById('jsonInput').value;
            const responseArea = document.getElementById('responseMessage');
            const endpoint = form.dataset.endpoint; // Get the endpoint from a data attribute on the form

            if (endpoint) {
                await sendJson(endpoint, jsonInput, responseArea);
            } else {
                responseArea.textContent = "Error: Endpoint not defined for the form.";
                responseArea.style.color = 'red';
            }
        });
    }
});
