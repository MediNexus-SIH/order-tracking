const express = require("express");
const http = require("http");
const socketIo = require("socket.io");
const path = require("path");
const cors = require("cors");

const app = express();
const server = http.createServer(app);
const io = socketIo(server);

// Use CORS middleware
app.use(cors());

// Parse JSON and URL-encoded data
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Serve static files from the "public" directory
app.use(express.static(path.join(__dirname, "public")));

let lastLocation = null;

// Endpoint to update location
app.post("/update-location", (req, res) => {
  console.log("Received location update:", req.body);
  const { latitude, longitude } = req.body;
  lastLocation = { latitude, longitude };
  io.emit("locationUpdate", lastLocation);
  res.sendStatus(200);
});

// Endpoint to get the last location
app.get("/last-location", (req, res) => {
  res.json(lastLocation);
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
