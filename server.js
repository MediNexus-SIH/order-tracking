const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const path = require('path');

const app = express();
const server = http.createServer(app);
const io = socketIo(server);

app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(express.static(path.join(__dirname, 'public')));

let lastLocation = null;

app.post('/update-location', (req, res) => {
    console.log('Received location update:', req.body);
    const { latitude, longitude } = req.body;
    lastLocation = { latitude, longitude };
    io.emit('locationUpdate', lastLocation);
    res.sendStatus(200);
});

app.get('/last-location', (req, res) => {
    res.json(lastLocation);
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});