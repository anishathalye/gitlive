var express = require('express');
var compression = require('compression');
var rabbit = require('rabbit.js');
var connections = [];

var context = rabbit.createContext();
context.on('ready', function() {
    console.log('Connection ready');
    var sub = context.socket('SUB');
    sub.setEncoding('utf-8');
    sub.connect('gh-events', '', function() {
        console.log('Subscribed')
        sub.on('data', function(data) {
            consume(data);
        });
    });
});

app = express();
app.use(compression());
app.use(express.static(__dirname + '/public'));

app.get('/events', function(req, res) {
    if (req.headers.accept == 'text/event-stream') {
        res.writeHead(200, {
            'content-type': 'text/event-stream',
            'cache-control': 'no-cache',
            'connection': 'keep-alive'
        });
        connections.push(res);
        console.log('Connection added for ' + req.ip);

        req.on('close', function () {
            removeConnection(res);
        });
    } else {
        res.send(500, 'This path for EventSource subscription only...');
    }
});

app.listen(8000);

function consume(message) {
    broadcast(JSON.stringify(message));
}

function broadcast(data) {
    var id = (new Date()).toLocaleTimeString();
    connections.forEach(function (res) {
        writeEvent(res, id, data);
    });
}

function writeEvent(res, id, data) {
    res.write('id: ' + id + '\n');
    res.write('data: ' + data + '\n\n');
    res.flush();
}

function removeConnection(res) {
    var i = connections.indexOf(res);
    if (i !== -1) {
        connections.splice(i, 1);
        console.log('Connection closed.');
    }
}
